package com.dakotagroupstaff.ui.operasional.assignment

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.dakotagroupstaff.data.Result
import com.dakotagroupstaff.data.local.pref.SessionManager
import com.dakotagroupstaff.data.remote.response.LetterOfAssignmentData
import com.dakotagroupstaff.databinding.ActivityAssignmentBinding
import com.dakotagroupstaff.databinding.DialogQrCodeBinding
import com.dakotagroupstaff.databinding.DialogSelectAssignmentBinding
import com.dakotagroupstaff.databinding.DialogOperationalCostBinding
import com.dakotagroupstaff.util.ErrorMessageHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class AssignmentActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAssignmentBinding
    private val viewModel: AssignmentViewModel by viewModel()
    private val sessionManager: SessionManager by inject()
    
    private var currentAssignmentId: String? = null
    private var currentPt: String? = null
    private var currentNip: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityAssignmentBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            setupToolbar()
            setupSession()
            setupObservers()
            setupListeners()
            
            // Load initial data
            loadData()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Surat Tugas"
        }
    }
    
    private fun setupSession() {
        currentPt = sessionManager.getPt()
        currentNip = sessionManager.getNip()
        
        if (currentPt == null || currentNip == null) {
            Toast.makeText(this, "Session tidak valid", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
    }
    
    private fun setupObservers() {
        // Observe agent locations for mapping agent codes to names
        viewModel.agentLocations.observe(this) { result ->
            // Agent locations loaded silently in background
            // Used for mapping startAgen code to agent name
            if (result is Result.Error) {
                android.util.Log.w("AssignmentActivity", "Failed to load agent locations: ${result.message}")
            }
        }
        
        // Observe assignment data
        viewModel.assignment.observe(this) { result ->
            when (result) {
                is Result.Loading -> {
                    showLoading()
                }
                is Result.Success -> {
                    showAssignmentData(result.data)
                }
                is Result.Error -> {
                    val friendlyMessage = ErrorMessageHelper.parseErrorMessage(result.message)
                    showEmptyState(friendlyMessage)
                }
            }
        }
        
        // Observe refreshing state
        viewModel.isRefreshing.observe(this) { isRefreshing ->
            // This will be used by fragments
        }
        
        // Observe save result
        viewModel.saveResult.observe(this) { result ->
            when (result) {
                is Result.Loading -> {
                    // Show loading in dialog
                }
                is Result.Success -> {
                    Toast.makeText(this, ErrorMessageHelper.getAssignmentCostSaveSuccess(), Toast.LENGTH_SHORT).show()
                    viewModel.resetSaveResult()
                }
                is Result.Error -> {
                    Toast.makeText(this, ErrorMessageHelper.getAssignmentCostSaveError(), Toast.LENGTH_SHORT).show()
                    viewModel.resetSaveResult()
                }
            }
        }
    }
    
    /**
     * Show loading state
     */
    private fun showLoading() {
        binding.progressBar.isVisible = true
        binding.layoutEmptyState.isVisible = false
        binding.cardAssignmentInfo.isVisible = false
        binding.cardAdditionalInfo.isVisible = false
        binding.cardDriverInfo.isVisible = false
        binding.cardNotes.isVisible = false
    }
    
    /**
     * Show empty state with message
     */
    private fun showEmptyState(message: String) {
        binding.progressBar.isVisible = false
        binding.layoutEmptyState.isVisible = true
        binding.cardAssignmentInfo.isVisible = false
        binding.cardAdditionalInfo.isVisible = false
        binding.cardDriverInfo.isVisible = false
        binding.cardNotes.isVisible = false
        
        // Empty state layout already has user-friendly static text:
        // "Tidak ada surat tugas" and "Anda belum memiliki surat tugas aktif saat ini"
        // No need to set dynamic message
    }
    
    /**
     * Show assignment data
     */
    private fun showAssignmentData(data: LetterOfAssignmentData) {
        binding.progressBar.isVisible = false
        binding.layoutEmptyState.isVisible = false
        binding.cardAssignmentInfo.isVisible = true
        binding.cardAdditionalInfo.isVisible = true
        binding.cardDriverInfo.isVisible = true
        binding.cardNotes.isVisible = true
        
        currentAssignmentId = data.sID
        
        // Display assignment info
        binding.tvAssignmentId.text = data.sID
        binding.tvDate.text = formatDate(data.tglBerangkat)
        
        val driverText = buildString {
            append("Supir: ${data.supir1Nama}")
            if (data.supir2Nama.isNotEmpty()) {
                append(" & ${data.supir2Nama}")
            }
        }
        binding.tvDriver.text = driverText
        
        binding.tvVehicle.text = data.noKendaraan
        binding.tvRoute.text = data.keterangan
        
        val kmText = if (data.trKm.isEmpty()) {
            "KM: Belum ada data"
        } else {
            "KM: ${data.trKm}"
        }
        binding.tvKM.text = kmText
        
        // Update status chip
        val statusText = if (data.trStatus.isEmpty()) "Aktif" else data.trStatus
        binding.chipStatus.text = statusText
        binding.chipStatus.setChipBackgroundColorResource(
            if (statusText.contains("Aktif", ignoreCase = true)) {
                com.dakotagroupstaff.R.color.success
            } else {
                com.dakotagroupstaff.R.color.warning
            }
        )
        
        // Display additional info
        binding.tvDepartureDate.text = formatDateTime(data.tglBerangkat)
        binding.tvReturnDate.text = formatDateTime(data.tglKembali)
        
        // Display start agent with name from API
        val agentText = if (data.startAgen == 0) {
            "-"
        } else {
            // Try to get agent name from cache
            val agentName = viewModel.getAgentNameByCode(data.startAgen)
            if (agentName != null) {
                "$agentName (Agen ${data.startAgen})"
            } else {
                "Agen ${data.startAgen}"
            }
        }
        binding.tvStartAgent.text = agentText
        
        binding.tvBranch.text = if (data.trCabang.isEmpty()) "-" else data.trCabang
        binding.tvLastEvent.text = if (data.trLastEvent.isEmpty()) "-" else data.trLastEvent
        
        // Display driver information
        binding.tvDriver1Nip.text = data.supir1NIP
        binding.tvDriver1Name.text = data.supir1Nama
        
        // Show/hide driver 2 info
        if (data.supir2Nama.isNotEmpty() && data.supir2NIP.isNotEmpty()) {
            binding.layoutDriver2.isVisible = true
            binding.tvDriver2Nip.text = data.supir2NIP
            binding.tvDriver2Name.text = data.supir2Nama
        } else {
            binding.layoutDriver2.isVisible = false
        }
        
        // Display notes (using keterangan as notes)
        if (data.keterangan.isNotEmpty()) {
            binding.tvNotes.isVisible = true
            binding.tvNoNotes.isVisible = false
            binding.tvNotes.text = data.keterangan
        } else {
            binding.tvNotes.isVisible = false
            binding.tvNoNotes.isVisible = true
        }
        
        // Load operational costs
        currentAssignmentId?.let { sID ->
            currentPt?.let { pt ->
                viewModel.getDownPaymentCosts(pt, sID)
                viewModel.getAdditionalOperationalCosts(pt, sID)
                viewModel.checkOperationalCostApproval(pt, sID)
            }
        }
    }
    
    private fun setupListeners() {
        // Refresh button on empty state
        binding.btnRefreshEmpty.setOnClickListener {
            loadData()
        }
        
        // Select Assignment button
        binding.btnSelectAssignment.setOnClickListener {
            showSelectAssignmentDialog()
        }
        
        // Operational Cost button
        binding.btnOperationalCost.setOnClickListener {
            showOperationalCostDialog()
        }
        
        // QR Code button
        binding.btnShowQR.setOnClickListener {
            showQRCodeDialog()
        }
    }
    
    private fun loadData() {
        currentPt?.let { pt ->
            // Load agent locations first for mapping agent codes to names
            viewModel.getAgentLocations(pt)
            
            currentNip?.let { nip ->
                viewModel.getLetterOfAssignment(pt, nip)
            }
        }
    }
    
    /**
     * Show dialog to select an assignment from the list
     */
    private fun showSelectAssignmentDialog() {
        android.util.Log.d("AssignmentActivity", "showSelectAssignmentDialog called")
        try {
            android.util.Log.d("AssignmentActivity", "Creating dialog binding...")
            val dialogBinding = DialogSelectAssignmentBinding.inflate(layoutInflater)
            
            android.util.Log.d("AssignmentActivity", "Creating MaterialAlertDialog...")
            val dialog = MaterialAlertDialogBuilder(this)
                .setView(dialogBinding.root)
                .setCancelable(true)
                .create()
            
            android.util.Log.d("AssignmentActivity", "Setting up RecyclerView adapter...")
            // Setup RecyclerView
            val adapter = AssignmentListAdapter { assignmentId ->
                android.util.Log.d("AssignmentActivity", "Assignment selected: $assignmentId")
                // Load selected assignment
                currentPt?.let { pt ->
                    currentNip?.let { nip ->
                        // Get full assignment details by sID
                        viewModel.getLetterOfAssignmentBySID(pt, nip, assignmentId)
                        dialog.dismiss()
                    }
                }
            }
            
            android.util.Log.d("AssignmentActivity", "Configuring RecyclerView...")
            dialogBinding.rvAssignments.apply {
                layoutManager = LinearLayoutManager(this@AssignmentActivity)
                this.adapter = adapter
            }
            
            android.util.Log.d("AssignmentActivity", "Loading assignment list... pt=$currentPt, nip=$currentNip")
            // Load assignment list immediately
            currentPt?.let { pt ->
                currentNip?.let { nip ->
                    viewModel.getAssignmentList(pt, nip)
                } ?: run {
                    android.util.Log.e("AssignmentActivity", "NIP is null!")
                    Toast.makeText(this, "NIP tidak ditemukan", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                android.util.Log.e("AssignmentActivity", "PT is null!")
                Toast.makeText(this, "PT tidak ditemukan", Toast.LENGTH_SHORT).show()
            }
            
            android.util.Log.d("AssignmentActivity", "Setting up observer...")
            // Observe assignment list - remove old observers first
            viewModel.assignmentList.removeObservers(this)
            viewModel.assignmentList.observe(this) { result ->
                android.util.Log.d("AssignmentActivity", "Assignment list result: $result")
                // Check if dialog is still showing to prevent crash
                if (!dialog.isShowing) {
                    android.util.Log.w("AssignmentActivity", "Dialog not showing, skipping UI update")
                    return@observe
                }
                
                when (result) {
                    is Result.Loading -> {
                        android.util.Log.d("AssignmentActivity", "Loading...")
                        dialogBinding.progressBar.isVisible = true
                        dialogBinding.rvAssignments.isVisible = false
                        dialogBinding.tvEmptyMessage.isVisible = false
                    }
                    is Result.Success -> {
                        android.util.Log.d("AssignmentActivity", "Success! Data size: ${result.data.size}")
                        dialogBinding.progressBar.isVisible = false
                        if (result.data.isEmpty()) {
                            dialogBinding.rvAssignments.isVisible = false
                            dialogBinding.tvEmptyMessage.isVisible = true
                        } else {
                            dialogBinding.rvAssignments.isVisible = true
                            dialogBinding.tvEmptyMessage.isVisible = false
                            adapter.submitList(result.data)
                        }
                    }
                    is Result.Error -> {
                        android.util.Log.e("AssignmentActivity", "Error: ${result.message}")
                        dialogBinding.progressBar.isVisible = false
                        dialogBinding.rvAssignments.isVisible = false
                        dialogBinding.tvEmptyMessage.isVisible = true
                        val friendlyMessage = ErrorMessageHelper.parseErrorMessage(result.message)
                        dialogBinding.tvEmptyMessage.text = friendlyMessage
                    }
                }
            }
            
            android.util.Log.d("AssignmentActivity", "Setting up close button...")
            dialogBinding.btnClose.setOnClickListener {
                android.util.Log.d("AssignmentActivity", "Close button clicked")
                dialog.dismiss()
            }
            
            android.util.Log.d("AssignmentActivity", "Showing dialog...")
            dialog.show()
            android.util.Log.d("AssignmentActivity", "Dialog shown successfully")
        } catch (e: Exception) {
            android.util.Log.e("AssignmentActivity", "Exception in showSelectAssignmentDialog", e)
            Toast.makeText(this, "Gagal membuka dialog: ${e.message}\n${e.stackTraceToString()}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
    
    /**
     * Show dialog for Uang Muka and Biaya Operasional
     */
    private fun showOperationalCostDialog() {
        if (currentAssignmentId == null) {
            Toast.makeText(this, "Pilih surat tugas terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }
        
        try {
            val dialogBinding = DialogOperationalCostBinding.inflate(layoutInflater)
            val dialog = MaterialAlertDialogBuilder(this)
                .setView(dialogBinding.root)
                .setCancelable(true)
                .create()
            
            // Setup ViewPager with fragments
            val viewPagerAdapter = AssignmentPagerAdapter(this)
            dialogBinding.viewPager.adapter = viewPagerAdapter
            
            // Connect TabLayout with ViewPager2
            TabLayoutMediator(dialogBinding.tabLayout, dialogBinding.viewPager) { tab, position ->
                tab.text = when (position) {
                    0 -> "Uang Muka"
                    1 -> "Biaya Tambahan"
                    else -> ""
                }
            }.attach()
            
            dialogBinding.btnClose.setOnClickListener {
                dialog.dismiss()
            }
            
            dialog.show()
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal membuka dialog: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
    
    private fun showQRCodeDialog() {
        currentAssignmentId?.let { sID ->
            try {
                // Get assignment data from ViewModel
                val assignmentData = (viewModel.assignment.value as? Result.Success)?.data
                
                if (assignmentData == null) {
                    Toast.makeText(this, "Data surat tugas tidak tersedia", Toast.LENGTH_SHORT).show()
                    return
                }
                
                // Create QR Code data (JSON format)
                val qrData = buildString {
                    append("{")
                    append("\"assignmentId\":\"${assignmentData.sID}\",")
                    append("\"driver\":\"${assignmentData.supir1Nama}\",")
                    append("\"vehicle\":\"${assignmentData.noKendaraan}\",")
                    append("\"date\":\"${assignmentData.tglBerangkat}\",")
                    append("\"route\":\"${assignmentData.keterangan}\",")
                    append("\"nip\":\"${currentNip}\",")
                    append("\"pt\":\"${currentPt}\"")
                    append("}")
                }
                
                // Generate QR Code bitmap
                val qrCodeBitmap = generateQRCode(qrData)
                
                // Show dialog with QR Code
                val dialogBinding = DialogQrCodeBinding.inflate(layoutInflater)
                dialogBinding.ivQRCode.setImageBitmap(qrCodeBitmap)
                dialogBinding.tvQRContent.text = "SID: $sID"
                
                val dialog = MaterialAlertDialogBuilder(this)
                    .setView(dialogBinding.root)
                    .create()
                
                dialogBinding.btnClose.setOnClickListener {
                    dialog.dismiss()
                }
                
                dialog.show()
                
            } catch (e: Exception) {
                Toast.makeText(this, "Gagal membuat QR Code: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        } ?: run {
            Toast.makeText(this, "Assignment ID tidak tersedia", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Generate QR Code bitmap from text data
     * @param data Text data to encode in QR Code
     * @return Bitmap of the QR Code
     */
    private fun generateQRCode(data: String): Bitmap {
        val size = 512 // QR Code size in pixels
        val qrCodeWriter = QRCodeWriter()
        val bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, size, size)
        
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        
        return bitmap
    }
    
    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
            val date = inputFormat.parse(dateString)
            date?.let { outputFormat.format(it) } ?: dateString
        } catch (e: Exception) {
            dateString
        }
    }
    
    private fun formatDateTime(dateString: String): String {
        return try {
            // Try to parse ISO 8601 format first (e.g., 2024-01-15T08:00:00.000Z)
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val outputFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID"))
            val date = inputFormat.parse(dateString)
            date?.let { outputFormat.format(it) } ?: dateString
        } catch (e: Exception) {
            try {
                // Fallback to simple date format
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
                val date = inputFormat.parse(dateString)
                date?.let { outputFormat.format(it) } ?: dateString
            } catch (e2: Exception) {
                dateString
            }
        }
    }
    
    fun formatCurrency(amount: String): String {
        return try {
            val numericAmount = amount.replace("[^0-9]".toRegex(), "").toLongOrNull() ?: 0L
            val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
            formatter.format(numericAmount).replace("Rp", "Rp ")
        } catch (e: Exception) {
            "Rp $amount"
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
