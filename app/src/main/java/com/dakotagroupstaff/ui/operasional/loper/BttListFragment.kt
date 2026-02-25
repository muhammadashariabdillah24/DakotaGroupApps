package com.dakotagroupstaff.ui.operasional.loper

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.dakotagroupstaff.data.local.preferences.UserPreferences
import com.dakotagroupstaff.data.local.preferences.dataStore
import com.dakotagroupstaff.data.local.room.AppDatabase
import com.dakotagroupstaff.data.remote.response.DeliveryItem
import com.dakotagroupstaff.data.remote.retrofit.ApiConfig
import com.dakotagroupstaff.data.repository.DeliveryRepository
import com.dakotagroupstaff.databinding.FragmentBttListBinding
import com.dakotagroupstaff.utils.NetworkUtils
import com.dakotagroupstaff.utils.ViewModelFactory
import com.dakotagroupstaff.worker.BatchSubmitBttWorker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BttListFragment : Fragment() {

    private var _binding: FragmentBttListBinding? = null
    private val binding get() = _binding!!

    private lateinit var deliveryAdapter: DeliveryAdapter
    private var pendingDeliveryItem: DeliveryItem? = null
    
    private val scannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val action = result.data?.getStringExtra("ACTION")
            
            if (action == "NAVIGATE_TO_DETAIL") {
                // User pressed Simpan button - navigate to detail
                pendingDeliveryItem?.let { navigateToDetail(it) }
            }
            // If no action or other cases, just dismiss scanner
        }
        pendingDeliveryItem = null
    }

    private val viewModel: LoperViewModel by activityViewModels {
        val userPref = UserPreferences.getInstance(requireContext().dataStore)
        val apiService = ApiConfig.getApiService(userPreferences = userPref)
        val database = AppDatabase.getDatabase(requireContext())
        val deliveryRepository = DeliveryRepository(apiService, userPref, database.deliveryListDao())
        val loperRepository = com.dakotagroupstaff.data.repository.LoperRepository.getInstance(apiService, userPref)
        ViewModelFactory.getInstance(
            requireContext(), 
            deliveryRepository = deliveryRepository,
            loperRepository = loperRepository
        )
    }

    private val tabType: BttTabType
        get() {
            val name = arguments?.getString(ARG_TAB_TYPE) ?: BttTabType.PENDING.name
            return runCatching { BttTabType.valueOf(name) }.getOrDefault(BttTabType.PENDING)
        } 

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBttListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSwipeRefresh()
        setupObservers()
        setupProcessAllButton()
    }

    private fun setupRecyclerView() {
        deliveryAdapter = DeliveryAdapter(
            onItemClick = { deliveryItem ->
                // Show scanner modal for PENDING tab, direct navigation for SENT tab
                if (tabType == BttTabType.PENDING) {
                    showBarcodeScanner(deliveryItem)
                } else {
                    navigateToDetail(deliveryItem)
                }
            },
            onItemLongClick = if (tabType == BttTabType.SENT) {
                { deliveryItem ->
                    showDeleteConfirmationDialog(deliveryItem)
                }
            } else null,
            isFromSentTab = (tabType == BttTabType.SENT)
        )
        binding.rvDeliveryList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            adapter = deliveryAdapter
        }
    }
    
    private fun showDeleteConfirmationDialog(deliveryItem: DeliveryItem) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setMessage("Apakah anda ingin menghapus BTT ini dari Daftar BTT Terkirim?")
            .setPositiveButton("Iya, Hapus") { _, _ ->
                lifecycleScope.launch {
                    viewModel.removeSent(deliveryItem.noBtt)
                    Toast.makeText(requireContext(), "BTT dipindahkan ke Daftar BTT Tertunda", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            if (tabType == BttTabType.SENT) {
                // SENT tab uses local Room data only, no API reload
                binding.swipeRefresh.isRefreshing = false
            } else {
                (activity as? LoperActivity)?.reloadDelivery()
            }
        }
    }

    private fun setupObservers() {
        // Observe API delivery list only for tabs that use it (PENDING, OVERDUE)
        if (tabType != BttTabType.SENT) {
            viewModel.deliveryList.observe(viewLifecycleOwner) { result ->
                when (result) {
                    is com.dakotagroupstaff.data.Result.Loading -> {
                        showLoading()
                    }
                    is com.dakotagroupstaff.data.Result.Success -> {
                        val deliveryData = result.data.data.orEmpty()
                        when (tabType) {
                            BttTabType.PENDING -> {
                                lifecycleScope.launch {
                                    showPendingDeliveries(deliveryData)
                                }
                            }
                            BttTabType.SENT -> {
                                // No-op: SENT tab uses local Room data only
                            }
                            BttTabType.OVERDUE -> showOverdueDeliveries(deliveryData)
                        }
                    }
                    is com.dakotagroupstaff.data.Result.Error -> {
                        showEmptyState()
                        Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        
        // Observe sent deliveries from local storage for SENT tab
        if (tabType == BttTabType.SENT) {
            viewModel.sentDeliveries.observe(viewLifecycleOwner) { sentList ->
                showSentDeliveries(sentList)
            }
        }
    }

    private fun showLoading() {
        // Use default SwipeRefreshLayout loading indicator
        if (!binding.swipeRefresh.isRefreshing) {
            binding.swipeRefresh.isRefreshing = true
        }
        binding.layoutEmptyState.isVisible = false
    }

    private fun showEmptyState() {
        binding.swipeRefresh.isRefreshing = false
        binding.layoutEmptyState.isVisible = true
        binding.rvDeliveryList.isVisible = false
    }

    private suspend fun showPendingDeliveries(data: List<DeliveryItem>) {
        binding.swipeRefresh.isRefreshing = false

        // Filter out BTT that are already sent (in local storage)
        val pendingList = data.filter { item ->
            !viewModel.isDeliverySent(item.noBtt)
        }

        if (pendingList.isEmpty()) {
            binding.layoutEmptyState.isVisible = true
            binding.rvDeliveryList.isVisible = false
        } else {
            binding.layoutEmptyState.isVisible = false
            binding.rvDeliveryList.isVisible = true
            deliveryAdapter.submitList(pendingList)
        }
    }
    
    private fun showSentDeliveries(sentList: List<com.dakotagroupstaff.data.local.entity.DeliveryListEntity>) {
        binding.swipeRefresh.isRefreshing = false

        if (sentList.isEmpty()) {
            binding.layoutEmptyState.isVisible = true
            binding.rvDeliveryList.isVisible = false
        } else {
            binding.layoutEmptyState.isVisible = false
            binding.rvDeliveryList.isVisible = true
            
            // Convert DeliveryListEntity to DeliveryItem for adapter
            val deliveryItems = sentList.map { entity ->
                DeliveryItem(
                    noLoper = entity.noLoper,
                    nipSupir = "",
                    tanggalLoper = "",
                    noBtt = entity.noBtt,
                    penerima = entity.penerima,
                    alamat = entity.alamat,
                    propinsi = "",
                    kota = "",
                    kecamatan = "",
                    kelurahan = "",
                    kodepos = "",
                    telpPenerima = "",
                    namaPengirim = "",
                    telpPengirim = "",
                    jumlahKoli = entity.jumlahKoli,
                    berat = 0,
                    beratVolume = 0,
                    service = ""
                )
            }
            deliveryAdapter.submitList(deliveryItems)
        }
    }
    
    private fun showOverdueDeliveries(data: List<DeliveryItem>) {
        binding.swipeRefresh.isRefreshing = false

        // TODO: Implement overdue logic based on SLA/due date
        // For now, show empty
        binding.layoutEmptyState.isVisible = true
        binding.rvDeliveryList.isVisible = false
    }

    
    
    private fun navigateToDetail(deliveryItem: DeliveryItem) {
        val intent = Intent(requireContext(), LoperDetailActivity::class.java).apply {
            putExtra(LoperDetailActivity.EXTRA_DELIVERY_ITEM, deliveryItem)
            putExtra(LoperDetailActivity.EXTRA_IS_FROM_SENT_TAB, tabType == BttTabType.SENT)
        }
        startActivity(intent)
    }
    
    private fun showBarcodeScanner(deliveryItem: DeliveryItem) {
        lifecycleScope.launch {
            val prefs = UserPreferences.getInstance(requireContext().dataStore)
            val currentBtt = prefs.getCurrentBttNumber()
            val scannedCount = prefs.getScannedKoliCountForBtt(currentBtt)
            
            // Check if there's existing scan data for a different BTT
            if (currentBtt.isNotEmpty() && currentBtt != deliveryItem.noBtt && scannedCount > 0) {
                // Show warning dialog
                showDifferentBttWarningDialog(currentBtt, deliveryItem)
            } else {
                // Open scanner directly - load existing scan data if any
                launchBarcodeScanner(deliveryItem)
            }
        }
    }
    
    /**
     * Show warning dialog when user selects different BTT while having scan data
     */
    private fun showDifferentBttWarningDialog(currentBtt: String, deliveryItem: DeliveryItem) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("⚠️ Peringatan")
            .setMessage(
                """
Apakah anda yakin ingin melakukan scan kode barcode koli BTT ${deliveryItem.noBtt} padahal saat ini anda sedang melakukan scan kode barcode koli BTT $currentBtt.

Data scan untuk kedua BTT akan tetap tersimpan dan dapat diproses nanti.
                """.trimIndent()
            )
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
                // User cancelled - don't open scanner
            }
            .setPositiveButton("Oke") { dialog, _ ->
                dialog.dismiss()
                // Open scanner for new BTT without clearing previous BTT data
                lifecycleScope.launch {
                    launchBarcodeScanner(deliveryItem)
                }
            }
            .setCancelable(false)
            .show()
    }
    
    /**
     * Launch barcode scanner activity
     * Loads existing scan data for the BTT if available
     */
    private fun launchBarcodeScanner(deliveryItem: DeliveryItem) {
        lifecycleScope.launch {
            val prefs = UserPreferences.getInstance(requireContext().dataStore)
            
            // Check if this BTT already has scanned koli data
            val existingKoliCount = prefs.getScannedKoliCountForBtt(deliveryItem.noBtt)
            
            if (existingKoliCount > 0) {
                // BTT already has scan data - load it
                prefs.setCurrentBttNumber(deliveryItem.noBtt)
                prefs.setCurrentBttTotalKoli(deliveryItem.jumlahKoli)
                // Data is already stored per-BTT, no need to do anything else
            } else {
                // New BTT - set current BTT
                prefs.setCurrentBttNumber(deliveryItem.noBtt)
                prefs.setCurrentBttTotalKoli(deliveryItem.jumlahKoli)
            }
            
            pendingDeliveryItem = deliveryItem
            val intent = Intent(requireContext(), BarcodeScannerActivity::class.java).apply {
                putExtra(BarcodeScannerActivity.EXTRA_TOTAL_KOLI, deliveryItem.jumlahKoli)
                putExtra(BarcodeScannerActivity.EXTRA_EXPECTED_BTT, deliveryItem.noBtt)
                putExtra(BarcodeScannerActivity.EXTRA_NO_LOPER, deliveryItem.noLoper)
            }
            scannerLauncher.launch(intent)
        }
    }
    
    private fun setupProcessAllButton() {
        // Show "Proses Semua" button only for SENT tab
        if (tabType == BttTabType.SENT) {
            binding.btnProcessAll.isVisible = true
            binding.btnProcessAll.setOnClickListener {
                startBatchSubmission()
            }
        } else {
            binding.btnProcessAll.isVisible = false
        }
    }
    
    private fun startBatchSubmission() {
        lifecycleScope.launch {
            // Check if there are any items to submit
            // Using a safer way to get the list, or just let the worker handle it
            val sentList = viewModel.sentDeliveries.value
            if (sentList == null) {
                // If null, it means it's still loading or empty, let's wait a bit or show empty msg
                Toast.makeText(requireContext(), "Memuat data...", Toast.LENGTH_SHORT).show()
                return@launch
            }
            
            if (sentList.isEmpty()) {
                Toast.makeText(requireContext(), "Tidak ada BTT untuk diproses", Toast.LENGTH_SHORT).show()
                return@launch
            }
            
            // Check network availability
            if (!NetworkUtils.isNetworkAvailable(requireContext())) {
                Toast.makeText(requireContext(), "Tidak ada koneksi internet", Toast.LENGTH_SHORT).show()
                return@launch
            }
            
            // Create and enqueue WorkManager request
            val workRequest = OneTimeWorkRequestBuilder<BatchSubmitBttWorker>().build()
            val workManager = WorkManager.getInstance(requireContext())
            workManager.enqueue(workRequest)
            
            // Observe work status
            workManager.getWorkInfoByIdLiveData(workRequest.id).observe(viewLifecycleOwner) { workInfo ->
                when (workInfo?.state) {
                    WorkInfo.State.RUNNING -> {
                        // Work is running, notification handles progress
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        // Success means the worker finished its task (even if some BTTs failed internally)
                        Toast.makeText(requireContext(), "Proses pengiriman selesai. Cek notifikasi untuk detail.", Toast.LENGTH_LONG).show()
                    }
                    WorkInfo.State.FAILED -> {
                        // Failure means the process crashed or couldn't start
                        Toast.makeText(requireContext(), "Proses terhenti. Periksa koneksi atau coba lagi.", Toast.LENGTH_LONG).show()
                    }
                    else -> {}
                }
            }
            
            Toast.makeText(requireContext(), "Memulai pengiriman BTT di background...", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TAB_TYPE = "arg_tab_type"

        fun newInstance(tabType: BttTabType): BttListFragment {
            return BttListFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TAB_TYPE, tabType.name)
                }
            }
        }
    }
}
