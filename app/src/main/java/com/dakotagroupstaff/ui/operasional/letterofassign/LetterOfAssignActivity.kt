package com.dakotagroupstaff.ui.operasional.letterofassign

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.dakotagroupstaff.data.local.preferences.UserPreferences
import com.dakotagroupstaff.databinding.ActivityLetterOfAssignBinding
import com.dakotagroupstaff.data.Result
import com.dakotagroupstaff.util.SecurityChecker
import com.google.android.gms.location.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.system.exitProcess

/**
 * Letter of Assign Activity for PT DBS and PT DLB only
 * Features:
 * - GPS tracking every 15 minutes
 * - Checkpoint check-in with photo and odometer
 * - Tab view for Bongkar (unload) and Muat (load)
 * - Route management with main and backup routes
 * - Auto-refresh data every 60 seconds
 * - KM update functionality
 * - Next point information
 */
class LetterOfAssignActivity : AppCompatActivity() {
    
    companion object {
        private const val BARCODE_SCAN_REQUEST = 1001
    }
    
    private lateinit var binding: ActivityLetterOfAssignBinding
    private val viewModel: LetterOfAssignViewModel by viewModel()
    private val userPreferences: UserPreferences by inject()
    
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var pagerAdapter: LetterOfAssignPagerAdapter
    private lateinit var photoCaptureHelper: com.dakotagroupstaff.util.PhotoCaptureHelper
    
    private val checkpointAdapter = CheckpointAdapter { checkpoint ->
        showCheckinDialog(checkpoint)
    }
    
    private var currentNip: String? = null
    private var currentPt: String? = null
    private var currentAgenID: String? = null
    private var currentKendID: String? = null
    
    // Auto-refresh handler
    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            reloadData()
            refreshHandler.postDelayed(this, 60000) // 60 seconds
        }
    }
    
    // Permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        
        if (fineLocationGranted && coarseLocationGranted && cameraGranted) {
            startLocationTracking()
        } else {
            Toast.makeText(
                this,
                "Izin lokasi dan kamera diperlukan untuk fitur surat tugas",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLetterOfAssignBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize PhotoCaptureHelper
        photoCaptureHelper = com.dakotagroupstaff.util.PhotoCaptureHelper(this)
        
        setupToolbar()
        setupRecyclerView()
        setupViewPager()
        initLocationClient()
        checkPermissions()
        loadUserData()
        observeViewModel()
        setupClickListeners()
        
        // Start auto-refresh
        refreshHandler.postDelayed(refreshRunnable, 60000)
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Surat Tugas"
        }
    }
    
    private fun setupRecyclerView() {
        binding.rvCheckpoints.apply {
            layoutManager = LinearLayoutManager(this@LetterOfAssignActivity)
            adapter = checkpointAdapter
        }
    }
    
    private fun setupViewPager() {
        pagerAdapter = LetterOfAssignPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
        
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Bongkar"
                1 -> "Muat"
                else -> ""
            }
        }.attach()
    }
    
    private fun setupClickListeners() {
        binding.btnCompleteAssignment.setOnClickListener {
            showCompleteConfirmationDialog()
        }
        
        binding.btnUpdateKM.setOnClickListener {
            showUpdateKMDialog()
        }
        
        // QR Code button
        binding.btnQRCode.setOnClickListener {
            showQRCodeDialog()
        }
        
        // Barcode Scan button
        binding.btnScanBarcode.setOnClickListener {
            startBarcodeScanner()
        }
        
        // Alternative Route button
        binding.btnAlternativeRoute.setOnClickListener {
            showAlternativeRouteDialog()
        }
        
        // Less Items button
        binding.btnLessItems.setOnClickListener {
            showLessItemsDialog()
        }
    }
    
    private fun showCompleteConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Konfirmasi")
            .setMessage("Apakah Anda yakin ingin menyelesaikan surat tugas ini?")
            .setPositiveButton("Ya") { _, _ ->
                viewModel.completeAssignment()
                Toast.makeText(this, "Surat tugas diselesaikan", Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNegativeButton("Batal", null)
            .show()
    }
    
    private fun showUpdateKMDialog() {
        val kendID = currentKendID ?: run {
            Toast.makeText(this, "Data kendaraan tidak tersedia", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Create input dialog
        val input = android.widget.EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        input.hint = "Masukkan KM saat ini"
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Update Kilometer")
            .setMessage("Masukkan pembacaan kilometer kendaraan saat ini")
            .setView(input)
            .setPositiveButton("Update") { _, _ ->
                val km = input.text.toString()
                if (km.isNotEmpty()) {
                    val sID = viewModel.currentSID.value ?: ""
                    val agenId = currentAgenID ?: ""
                    // Update KM through ViewModel
                    viewModel.updateKM(agenId, "1", "1", km)
                    Toast.makeText(this, "KM diupdate: $km", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "KM tidak boleh kosong", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }
    
    private fun loadUserData() {
        lifecycleScope.launch {
            currentNip = userPreferences.getNip().first()
            currentPt = userPreferences.getPt().first()
            
            currentNip?.let { nip ->
                currentPt?.let { pt ->
                    viewModel.loadLetterOfAssign(nip, pt)
                }
            }
        }
    }
    
    private fun reloadData() {
        currentNip?.let { nip ->
            currentPt?.let { pt ->
                viewModel.loadLetterOfAssign(nip, pt)
            }
        }
    }
    
    private fun observeViewModel() {
        viewModel.letterOfAssign.observe(this) { result ->
            when (result) {
                is Result.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is Result.Success -> {
                    binding.progressBar.visibility = View.GONE
                    
                    if (result.data.isNotEmpty()) {
                        val firstItem = result.data[0]
                        
                        // Display assignment info
                        binding.tvAssignmentId.text = firstItem.sID
                        binding.tvKeterangan.text = firstItem.keterangan
                        binding.tvVehicle.text = "Kendaraan: ${firstItem.noKendaraan}"
                        binding.tvDrivers.text = "Sopir: ${firstItem.supir1Nama}" + 
                            if (firstItem.supir2Nama.isNotEmpty()) " & ${firstItem.supir2Nama}" else ""
                        
                        // Save current IDs
                        currentAgenID = firstItem.startAgen
                        currentKendID = firstItem.noKendaraan
                        
                        // Get KM data
                        currentKendID?.let { kendID ->
                            viewModel.getKMData(kendID)
                        }
                        
                        // Display checkpoints
                        checkpointAdapter.submitList(result.data)
                        
                        // Load Bongkar and Muat data
                        currentAgenID?.let { agenID ->
                            viewModel.getUnloadingData(agenID)
                            viewModel.getLoadingData(agenID)
                        }
                        
                        // Get next point
                        viewModel.getNextPoint()
                    } else {
                        Toast.makeText(this, "Tidak ada surat tugas aktif", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
                is Result.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Error: ${result.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        viewModel.checkpointResult.observe(this) { result ->
            when (result) {
                is Result.Success -> {
                    Toast.makeText(this, "Check-in berhasil!", Toast.LENGTH_SHORT).show()
                    reloadData()
                }
                is Result.Error -> {
                    Toast.makeText(this, "Check-in gagal: ${result.message}", Toast.LENGTH_SHORT).show()
                }
                is Result.Loading -> {
                    // Show loading if needed
                }
            }
        }
        
        viewModel.loadingData.observe(this) { result ->
            when (result) {
                is Result.Success -> {
                    pagerAdapter.getMuatFragment().setData(result.data)
                }
                is Result.Error -> {
                    // Handle error
                }
                is Result.Loading -> {
                    // Show loading if needed
                }
            }
        }
        
        viewModel.unloadingData.observe(this) { result ->
            when (result) {
                is Result.Success -> {
                    pagerAdapter.getBongkarFragment().setData(result.data)
                }
                is Result.Error -> {
                    // Handle error
                }
                is Result.Loading -> {
                    // Show loading if needed
                }
            }
        }
        
        viewModel.nextPointData.observe(this) { result ->
            when (result) {
                is Result.Success -> {
                    binding.cardNextPoint.visibility = View.VISIBLE
                    binding.tvNextPointName.text = result.data.nextPointNama
                    binding.tvNextPointStats.text = "BTT: ${result.data.jmlBTT}, Colly: ${result.data.jmlColly}, Berat: ${result.data.totalBerat}"
                }
                is Result.Error -> {
                    binding.cardNextPoint.visibility = View.GONE
                }
                is Result.Loading -> {
                    // Show loading if needed
                }
            }
        }
        
        viewModel.kmData.observe(this) { result ->
            when (result) {
                is Result.Success -> {
                    binding.tvCurrentKM.text = "KM: ${result.data}"
                }
                is Result.Error -> {
                    binding.tvCurrentKM.text = "KM: -"
                }
                is Result.Loading -> {
                    // Show loading if needed
                }
            }
        }
    }
    
    private fun showCheckinDialog(checkpoint: com.dakotagroupstaff.data.remote.response.LetterOfAssignDetail) {
        // Show confirmation dialog first
        MaterialAlertDialogBuilder(this)
            .setTitle("Check-in Checkpoint")
            .setMessage("Ambil foto selfie untuk check-in di ${checkpoint.trCabang}?")
            .setPositiveButton("Ambil Foto") { _, _ ->
                // Capture photo first
                photoCaptureHelper.takePhoto { photoBase64 ->
                    if (photoBase64.isNotEmpty()) {
                        // Upload photo
                        viewModel.uploadCheckpointPhoto(photoBase64)
                        
                        // Observe upload result
                        viewModel.uploadPhotoResult.observe(this) { result ->
                            when (result) {
                                is Result.Success -> {
                                    val photoUrl = result.data
                                    // After photo uploaded, submit checkpoint
                                    submitCheckpointWithPhoto(checkpoint, photoUrl)
                                }
                                is Result.Error -> {
                                    Toast.makeText(this, "Gagal upload foto: ${result.message}", Toast.LENGTH_SHORT).show()
                                }
                                else -> {}
                            }
                        }
                    } else {
                        Toast.makeText(this, "Gagal mengambil foto", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }
    
    private fun submitCheckpointWithPhoto(checkpoint: com.dakotagroupstaff.data.remote.response.LetterOfAssignDetail, photoUrl: String) {
        lifecycleScope.launch {
            if (ActivityCompat.checkSelfPermission(
                    this@LetterOfAssignActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this@LetterOfAssignActivity,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(this@LetterOfAssignActivity, "Izin lokasi diperlukan", Toast.LENGTH_SHORT).show()
                return@launch
            }
            
            val currentLocation = fusedLocationClient.lastLocation
            currentLocation.addOnSuccessListener { location ->
                location?.let {
                    // CRITICAL SECURITY CHECK: Detect Fake GPS
                    if (SecurityChecker.isMockLocation(it)) {
                        showFakeGpsDialog()
                        return@addOnSuccessListener
                    }
                    
                    currentNip?.let { nip ->
                        viewModel.submitCheckpoint(
                            agenID = checkpoint.trKdCabang,
                            km = "0",
                            lat = it.latitude.toString(),
                            lon = it.longitude.toString(),
                            urlpic = photoUrl,
                            nip = nip,
                            urut = checkpoint.trUrut
                        )
                        Toast.makeText(this@LetterOfAssignActivity, "Check-in berhasil", Toast.LENGTH_SHORT).show()
                    }
                } ?: run {
                    Toast.makeText(this@LetterOfAssignActivity, "Tidak dapat mendapatkan lokasi", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA
        )
        
        val hasAllPermissions = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        
        if (hasAllPermissions) {
            startLocationTracking()
        } else {
            requestPermissionLauncher.launch(permissions)
        }
    }
    
    private fun initLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    // CRITICAL SECURITY CHECK: Detect Fake GPS
                    if (SecurityChecker.isMockLocation(location)) {
                        showFakeGpsDialog()
                        return
                    }
                    
                    // Update location to server
                    viewModel.updateLocation(location.latitude, location.longitude)
                    
                    // Update last update time
                    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    binding.tvLastUpdate.text = "Terakhir update: ${dateFormat.format(Date())}"
                }
            }
        }
    }
    
    private fun startLocationTracking() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                900000L // 15 minutes in milliseconds
            ).build()
            
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                mainLooper
            )
        }
    }
    
    // ========== NEW FEATURES IMPLEMENTATION ==========
    
    /**
     * Show QR Code dialog for current assignment
     */
    private fun showQRCodeDialog() {
        try {
            val sID = viewModel.currentSID.value ?: run {
                Toast.makeText(this, "Data surat tugas belum tersedia", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Generate QR data
            val qrData = buildString {
                append("{")
                append("\"sID\":\"$sID\",")
                append("\"nip\":\"${currentNip ?: ""}\",")
                append("\"pt\":\"${currentPt ?: ""}\"")
                append("}")
            }
            
            // Generate QR Code
            val qrBitmap = com.dakotagroupstaff.util.QRCodeGenerator.generateQRCode(qrData)
            
            if (qrBitmap != null) {
                val dialogBinding = com.dakotagroupstaff.databinding.DialogQrCodeBinding.inflate(layoutInflater)
                dialogBinding.ivQRCode.setImageBitmap(qrBitmap)
                dialogBinding.tvQRContent.text = "SID: $sID"
                
                val dialog = MaterialAlertDialogBuilder(this)
                    .setView(dialogBinding.root)
                    .create()
                
                dialogBinding.btnClose.setOnClickListener {
                    dialog.dismiss()
                }
                
                dialog.show()
            } else {
                Toast.makeText(this, "Gagal membuat QR Code", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
    
    /**
     * Start barcode scanner for BTT tracking
     */
    private fun startBarcodeScanner() {
        try {
            // Launch ZXing scanner
            val intent = android.content.Intent(this, com.journeyapps.barcodescanner.CaptureActivity::class.java)
            intent.putExtra("SCAN_MODE", "QR_CODE_MODE,PRODUCT_MODE")
            intent.putExtra("PROMPT_MESSAGE", "Scan BTT Barcode")
            startActivityForResult(intent, BARCODE_SCAN_REQUEST)
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
    
    /**
     * Handle barcode scan result
     */
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == BARCODE_SCAN_REQUEST) {
            if (resultCode == RESULT_OK && data != null) {
                val contents = data.getStringExtra("SCAN_RESULT")
                if (contents != null) {
                    // Show scanned BTT
                    MaterialAlertDialogBuilder(this)
                        .setTitle("BTT Ter-scan")
                        .setMessage("Barcode: $contents")
                        .setPositiveButton("OK") { _, _ ->
                            // TODO: Process scanned BTT - update scan status
                            Toast.makeText(this, "BTT $contents berhasil di-scan", Toast.LENGTH_SHORT).show()
                        }
                        .show()
                } else {
                    Toast.makeText(this, "Scan dibatalkan", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Scan dibatalkan", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Show alternative route dialog
     */
    private fun showAlternativeRouteDialog() {
        // Load alternative routes
        viewModel.getAlternativeRoute()
        
        // Observe result
        viewModel.alternativeRoute.observe(this) { result ->
            when (result) {
                is Result.Loading -> {
                    // Show loading
                }
                is Result.Success -> {
                    if (result.data.isEmpty()) {
                        Toast.makeText(this, "Tidak ada rute cadangan", Toast.LENGTH_SHORT).show()
                        return@observe
                    }
                    
                    // Show dialog
                    val dialogBinding = com.dakotagroupstaff.databinding.DialogAlternativeRouteBinding.inflate(layoutInflater)
                    val adapter = AlternativeRouteAdapter { route ->
                        // Handle route selection
                        Toast.makeText(this, "Rute dipilih: ${route.agenNamaCad}", Toast.LENGTH_SHORT).show()
                    }
                    
                    dialogBinding.rvAlternativeRoute.apply {
                        layoutManager = LinearLayoutManager(this@LetterOfAssignActivity)
                        this.adapter = adapter
                    }
                    adapter.submitList(result.data)
                    
                    val dialog = MaterialAlertDialogBuilder(this)
                        .setView(dialogBinding.root)
                        .create()
                    
                    dialogBinding.btnCancel.setOnClickListener {
                        dialog.dismiss()
                    }
                    
                    dialog.show()
                }
                is Result.Error -> {
                    Toast.makeText(this, "Error: ${result.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    /**
     * Show less items (barang kurang) dialog
     */
    private fun showLessItemsDialog() {
        val nip = currentNip ?: run {
            Toast.makeText(this, "Data NIP tidak tersedia", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Load less items
        viewModel.getLessItems(nip)
        
        // Observe result
        viewModel.lessItems.observe(this) { result ->
            when (result) {
                is Result.Loading -> {
                    // Show loading
                }
                is Result.Success -> {
                    if (result.data.isEmpty()) {
                        Toast.makeText(this, "Tidak ada barang kurang", Toast.LENGTH_SHORT).show()
                        return@observe
                    }
                    
                    // Show dialog
                    val dialogBinding = com.dakotagroupstaff.databinding.DialogLessItemsBinding.inflate(layoutInflater)
                    val adapter = LessItemsAdapter()
                    
                    dialogBinding.rvLessItems.apply {
                        layoutManager = LinearLayoutManager(this@LetterOfAssignActivity)
                        this.adapter = adapter
                    }
                    adapter.submitList(result.data)
                    
                    val dialog = MaterialAlertDialogBuilder(this)
                        .setView(dialogBinding.root)
                        .create()
                    
                    dialogBinding.btnClose.setOnClickListener {
                        dialog.dismiss()
                    }
                    
                    dialog.show()
                }
                is Result.Error -> {
                    Toast.makeText(this, "Error: ${result.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Stop location updates
        fusedLocationClient.removeLocationUpdates(locationCallback)
        // Stop auto-refresh
        refreshHandler.removeCallbacks(refreshRunnable)
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
    
    /**
     * Show Fake GPS detection dialog and force exit app
     */
    private fun showFakeGpsDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("FAKE GPS Terdeteksi")
            .setMessage("Anda terdeteksi menggunakan FAKE GPS. Silahkan matikan FAKE GPS-nya lalu gunakan aplikasi Dakota Group Staff kembali.")
            .setCancelable(false)
            .setPositiveButton("Baik, Saya Mengerti") { _, _ ->
                finishAffinity()
                exitProcess(0)
            }
            .show()
    }
}
