package com.dakotagroupstaff.ui.operasional.loper

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.dakotagroupstaff.R
import com.dakotagroupstaff.data.local.preferences.UserPreferences
import com.dakotagroupstaff.data.local.preferences.dataStore
import com.dakotagroupstaff.data.local.room.AppDatabase
import com.dakotagroupstaff.data.remote.retrofit.ApiConfig
import com.dakotagroupstaff.data.repository.DeliveryRepository
import com.dakotagroupstaff.data.repository.LoperRepository
import com.dakotagroupstaff.databinding.ActivityBarcodeScannerBinding
import com.dakotagroupstaff.data.Result
import com.dakotagroupstaff.ui.operasional.loper.LoperViewModel
import com.dakotagroupstaff.utils.ViewModelFactory
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BarcodeScannerActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "BarcodeScannerActivity"
        const val REQUEST_SCAN_BARCODE = 100
        const val EXTRA_EXPECTED_BTT = "EXPECTED_BTT"
        const val EXTRA_NO_LOPER = "NO_LOPER"
        const val EXTRA_TOTAL_KOLI = "TOTAL_KOLI"
    }

    private lateinit var binding: ActivityBarcodeScannerBinding
    private var isScanning = true
    private var isFlashOn = false
    
    private val viewModel: LoperViewModel by viewModels {
        val userPref = UserPreferences.getInstance(dataStore)
        val apiService = ApiConfig.getApiService(userPreferences = userPref)
        val database = AppDatabase.getDatabase(this)
        val deliveryRepository = DeliveryRepository(apiService, userPref, database.deliveryListDao())
        val loperRepository = LoperRepository.getInstance(apiService, userPref)
        ViewModelFactory.getInstance(
            this,
            deliveryRepository = deliveryRepository,
            loperRepository = loperRepository
        )
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startScanning()
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private var expectedBttNo = ""
    private var noLoper = "" // NEW

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBarcodeScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get data from intent using constants
        expectedBttNo = intent.getStringExtra(EXTRA_EXPECTED_BTT) ?: ""
        noLoper = intent.getStringExtra(EXTRA_NO_LOPER) ?: ""
        
        val totalKoli = intent.getIntExtra(EXTRA_TOTAL_KOLI, 0)
        lifecycleScope.launch {
            val prefs = UserPreferences.getInstance(dataStore)
            
            // NEW: Check if current BTT matches expected BTT
            val currentBtt = prefs.getCurrentBttNumber()
            if (currentBtt.isNotEmpty() && currentBtt != expectedBttNo) {
                 // Different BTT - warning handled by onBarcodeScanned logic? 
                 // Or we should clear here? The user might have switched item in strict mode.
                 // Ideally if Intent has specific BTT, we should enforce it.
                 // But let's leave existing logic unless requested.
            }

            prefs.setCurrentBttTotalKoli(totalKoli)
            updateCounterDisplay()
        }
        
        setupUI()
        checkCameraPermission()
        setupObservers() // NEW
    }
    
    
    private fun setupObservers() {
        // Observer for Check Barcode
        lifecycleScope.launch {
            viewModel.checkBarcodeResult.collect { result ->
                if (result == null) return@collect
                
                when (result) {
                    is com.dakotagroupstaff.data.Result.Loading -> {
                        binding.tvScanStatus.text = "Memvalidasi..."
                    }
                    is com.dakotagroupstaff.data.Result.Success -> {
                        val data = result.data
                        val bttId = data.bttId ?: ""
                        val scannedValue = data.value
                        val koliId = data.koliId ?: scannedValue // Use value if koliId null
                        val totalKoli = data.totalKoli ?: 0
                        
                        // Check warning message from API (e.g. "Semua koli ... sudah discan")
                        // The API returns 200 OK with found: true and message if full.
                        // But CheckBarcodeResponse.kt might need 'message' field mapping if it's in data?
                        // Actually Result.Success doesn't have message usually, unless customized.
                        // But wait, the API returns { found: true, message: "..." }.
                        // CheckBarcodeData needs 'message' field? 
                        // Let's assume for now we proceed to validation.
                        
                        handleBarcodeValidation(bttId, scannedValue, koliId, totalKoli, data.message)
                    }
                    is com.dakotagroupstaff.data.Result.Error -> {
                        showError(result.message)
                    }
                }
            }
        }

        // Observer for Result Barcode BTT (Save)
        lifecycleScope.launch {
             viewModel.resultBarcodeBTTResult.collect { result ->
                 if (result == null) return@collect
                 
                 when(result) {
                     is com.dakotagroupstaff.data.Result.Success -> {
                         Toast.makeText(this@BarcodeScannerActivity, "BTT Complete & Saved!", Toast.LENGTH_SHORT).show()
                         viewModel.resetResultBarcodeBTT()
                     }
                     is com.dakotagroupstaff.data.Result.Error -> {
                         Toast.makeText(this@BarcodeScannerActivity, "Save Result: ${result.message}", Toast.LENGTH_LONG).show()
                         viewModel.resetResultBarcodeBTT()
                     }
                     else -> {}
                 }
             }
        }
    }

    private fun handleBarcodeValidation(
        bttId: String,
        scannedValue: String?,
        koliId: String?,
        totalKoli: Int,
        message: String?
    ) {
        lifecycleScope.launch {
            val prefs = UserPreferences.getInstance(dataStore)
            val currentBtt = prefs.getCurrentBttNumber()
            
            // First scan of a new BTT
            if (currentBtt.isEmpty() || currentBtt != (bttId)) {
                // If switching BTT, just show toast - don't clear previous data
                // Each BTT keeps its own scan data in DataStore
                if (currentBtt.isNotEmpty() && currentBtt != bttId) {
                   Toast.makeText(this@BarcodeScannerActivity, "Switching to new BTT: $bttId", Toast.LENGTH_SHORT).show()
                }
                
                prefs.setCurrentBttNumber(bttId)
                prefs.setCurrentBttTotalKoli(totalKoli)
            }
            
            val koliIdToStore = koliId ?: scannedValue ?: ""
            if (koliIdToStore.isNotEmpty()) {
                val alreadyScanned = prefs.isKoliScannedForBtt(bttId, koliIdToStore)
                if (alreadyScanned) {
                    showError("Item already scanned!")
                } else {
                    saveScanAndUpdateCounterWithAutoSubmit(koliIdToStore, scannedValue ?: "")
                }
            } else {
                showError("Invalid Koli ID")
            }
        }
    }

    private fun setupUI() {
        // Close button
        binding.btnClose.setOnClickListener {
            finish()
        }
        
        // Simpan button - Now submits to API
        binding.btnSimpan.setOnClickListener {
            submitToApiAndFinish()
        }

        // Flashlight toggle
        binding.btnFlash.setOnClickListener {
            isFlashOn = !isFlashOn
            binding.barcodeScanner.setTorchOn()
            updateFlashIcon()
        }
    }

    private fun updateFlashIcon() {
        val iconRes = if (isFlashOn) {
            R.drawable.ic_flash_on
        } else {
            R.drawable.ic_flash_off
        }
        binding.btnFlash.setImageResource(iconRes)
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startScanning()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun startScanning() {
        binding.barcodeScanner.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult) {
                if (isScanning && result.text != null) {
                    isScanning = false
                    onBarcodeScanned(result.text)
                }
            }

            override fun possibleResultPoints(resultPoints: List<ResultPoint>) {
                // Optional: draw result points
            }
        })
        binding.barcodeScanner.resume()
    }

    private fun onBarcodeScanned(barcode: String) {
        // Validate barcode with API using Flow
        lifecycleScope.launch {
            viewModel.checkBarcode(barcode).collect { result ->
                when (result) {
                    is com.dakotagroupstaff.data.Result.Loading -> {
                        // Show loading indicator
                        binding.tvScanStatus.text = "Memvalidasi..."
                    }
                    is com.dakotagroupstaff.data.Result.Success -> {
                        val bttId = result.data.bttId ?: ""
                        val scannedValue = result.data.value
                        
                        if (bttId.isNotEmpty()) {
                            // Parse BTT number from barcode
                            val scannedBttNumber = parseBttNumberFromBarcode(barcode)
                            
                            if (scannedBttNumber != null) {
                                lifecycleScope.launch {
                                    val prefs = UserPreferences.getInstance(dataStore)
                                    val currentBtt = prefs.getCurrentBttNumber()
                                    
                                    when {
                                        currentBtt.isEmpty() -> {
                                            // First scan - set current BTT
                                            val totalKoli = result.data.totalKoli ?: 0
                                            val koliIdToStore = result.data.koliId ?: scannedValue
                                            
                                            prefs.setCurrentBttNumber(scannedBttNumber)
                                            // Update total koli if available
                                            if (totalKoli > 0) {
                                                prefs.setCurrentBttTotalKoli(totalKoli)
                                            }
                                            
                                            saveScanAndUpdateCounterWithAutoSubmit(koliIdToStore, scannedValue)
                                        }
                                        currentBtt == scannedBttNumber -> {
                                            // Same BTT - continue normally
                                            val koliIdToStore = result.data.koliId ?: scannedValue
                                            saveScanAndUpdateCounterWithAutoSubmit(koliIdToStore, scannedValue)
                                        }
                                        else -> {
                                            // DIFFERENT BTT - show warning dialog
                                            val totalKoli = result.data.totalKoli ?: 0
                                            showBttMismatchDialog(scannedBttNumber, currentBtt, bttId, scannedValue, totalKoli, result.data.koliId ?: scannedValue)
                                        }
                                    }
                                }
                            } else {
                                // Invalid barcode format
                                showError("Format barcode tidak valid")
                                isScanning = true
                            }
                        } else {
                            // Not found or bttId is empty
                            showError(result.data.message ?: "Barcode tidak ditemukan")
                            // Allow scanning again
                            isScanning = true
                        }
                    }
                    is com.dakotagroupstaff.data.Result.Error -> {
                        showError(result.message)
                        // Allow scanning again
                        isScanning = true
                    }
                }
            }
        }
    }
    
    private suspend fun updateCounterDisplay() {
        val prefs = UserPreferences.getInstance(dataStore)
        val currentBtt = prefs.getCurrentBttNumber()
        val scannedCount = prefs.getScannedKoliCountForBtt(currentBtt)
        val totalKoli = prefs.getCurrentBttTotalKoli()
        
        binding.tvBttCounter.apply {
            text = "$scannedCount / $totalKoli"
            visibility = View.VISIBLE
        }
        binding.btnSimpan.visibility = View.VISIBLE
    }

    private fun showSuccessFeedback(value: String, message: String = "Scan berhasil! BTT disimpan.") {
        // Green flash animation
        binding.scannerOverlay.showSuccess()

        // Play beep sound
        playBeepSound()

        // Show success message briefly
        binding.tvScanStatus.text = message
        binding.tvScanStatus.setTextColor(getColor(android.R.color.holo_green_dark))
        
        // Reset status text after delay
        binding.root.postDelayed({
            binding.tvScanStatus.text = "Arahkan kamera ke barcode"
            binding.tvScanStatus.setTextColor(getColor(android.R.color.white))
        }, 1500)
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        binding.tvScanStatus.text = message
        binding.tvScanStatus.setTextColor(getColor(android.R.color.holo_red_dark))

        // Allow scanning again after delay
        binding.root.postDelayed({
            isScanning = true
            binding.tvScanStatus.text = "Arahkan kamera ke barcode"
            binding.tvScanStatus.setTextColor(getColor(android.R.color.white))
        }, 2000)
    }

    private fun playBeepSound() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
            toneGen.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.barcodeScanner.resume()
    }

    override fun onPause() {
        super.onPause()
        binding.barcodeScanner.pause()
    }

    override fun onDestroy() {
        // toneGenerator?.release() // Assuming toneGenerator is a member variable
        // toneGenerator = null
        super.onDestroy()
    }
    
    // === BTT Validation Helper Functions ===
    
    /**
     * Parse BTT number from barcode
     * Format: [16 chars BTT][4 chars Koli]
     * Example: 001122025C0000010001
     */
    private fun parseBttNumberFromBarcode(barcode: String): String? {
        return if (barcode.length >= 16) {
            barcode.substring(0, 16)
        } else {
            null
        }
    }
    
    /**
     * Save scan and update counter - extracted for reuse
     * Uses per-BTT storage in DataStore
     */
    private suspend fun saveScanAndUpdateCounter(koliId: String, value: String) {
        val prefs = UserPreferences.getInstance(dataStore)
        val currentBtt = prefs.getCurrentBttNumber()
        val currentTotal = prefs.getCurrentBttTotalKoli()
        
        // Check if already scanned for this BTT
        if (prefs.isKoliScannedForBtt(currentBtt, koliId)) {
             showSuccessFeedback(value, "Barcode sudah discan")
             isScanning = true
             return
        }
        
        // Check cap limit
        val scannedCount = prefs.getScannedKoliCountForBtt(currentBtt)
        if (currentTotal > 0 && scannedCount >= currentTotal) {
             showSuccessFeedback(value, "Semua koli sudah discan")
             isScanning = true
             return
        }
        
        // Save using per-BTT function
        prefs.addScannedKoliForBtt(currentBtt, koliId)
        updateCounterDisplay()
        showSuccessFeedback(value)
        isScanning = true
    }
    
    /**
     * Show BTT mismatch warning dialog
     */
    private fun showBttMismatchDialog(
        scannedBtt: String,
        currentBtt: String,
        newBttId: String,
        newValue: String,
        newTotalKoli: Int,
        newKoliId: String
    ) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("⚠️ Peringatan")
           .setMessage(buildWarningMessage(scannedBtt, currentBtt))
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
                // Allow scanning again
                isScanning = true
            }
            .setPositiveButton("Oke, Saya Mengerti") { dialog, _ ->
                // Clear DataStore and start new BTT
                lifecycleScope.launch {
                    clearAndStartNewBtt(scannedBtt, newBttId, newValue, newTotalKoli, newKoliId)
                }
                dialog.dismiss()
            }
            .setCancelable(false)
            .create()
        
        dialog.show()
    }
    
    /**
     * Build warning message with red text for important warning
     */
    private fun buildWarningMessage(scannedBtt: String, currentBtt: String): SpannableString {
        val message = """
Ini bukan barang dari BTT ini ($currentBtt)!
Apakah anda tetap ingin menggunakan barcode BTT ini?

Data scan untuk BTT $currentBtt akan tetap tersimpan dan dapat diproses nanti.
        """.trimIndent()
        
        val spannable = SpannableString(message)
        
        // Find position of warning text
        val warningStart = message.indexOf("Data scan")
        if (warningStart >= 0) {
            // Apply red color to warning text
            spannable.setSpan(
                ForegroundColorSpan(android.graphics.Color.RED),
                warningStart,
                message.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        
        return spannable
    }
    
    /**
     * Start with new BTT without clearing previous BTT data
     * Uses per-BTT storage - each BTT keeps its own scan data
     */
    private suspend fun clearAndStartNewBtt(scannedBtt: String, bttId: String, value: String, newTotalKoli: Int, koliId: String) {
        val prefs = UserPreferences.getInstance(dataStore)
        
        // Note: We don't clear previous BTT data anymore
        // Each BTT keeps its own scan data in DataStore
        
        // Set new current BTT
        prefs.setCurrentBttNumber(scannedBtt)
        
        // Save first scan of new BTT
        if (newTotalKoli > 0) {
            prefs.setCurrentBttTotalKoli(newTotalKoli)
        }
        
        // Save using per-BTT function
        prefs.addScannedKoliForBtt(scannedBtt, koliId)
        
        // Update UI
        updateCounterDisplay()
        showSuccessFeedback(value)
        
        // Allow scanning again
        isScanning = true
    }

    /**
     * Save scanned koli to DataStore and navigate to BTT Detail
     * Note: API submission is done later in LoperDetailActivity
     */
    private fun submitToApiAndFinish() {
        lifecycleScope.launch {
            val prefs = UserPreferences.getInstance(dataStore)
            val currentBtt = prefs.getCurrentBttNumber()
            val scannedCount = prefs.getScannedKoliCountForBtt(currentBtt)
            val totalKoli = prefs.getCurrentBttTotalKoli()
            
            // Check if no koli has been scanned
            if (scannedCount == 0) {
                AlertDialog.Builder(this@BarcodeScannerActivity)
                    .setMessage("Tidak bisa menyimpan karena tidak ada kode barcode koli pada btt ini yang terscan!")
                    .setPositiveButton("Oke") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setCancelable(false)
                    .show()
                return@launch
            }
            
            // Show toast with scan result
            val toastMessage = if (scannedCount >= totalKoli && totalKoli > 0) {
                "Semua koli ($scannedCount) berhasil discan"
            } else {
                "$scannedCount dari $totalKoli koli berhasil discan"
            }
            Toast.makeText(this@BarcodeScannerActivity, toastMessage, Toast.LENGTH_LONG).show()
            
            // Show loading dialog briefly
            showLoadingDialog("Mohon Tunggu...")
            
            // Data is already saved in DataStore per-BTT
            // No need to submit to API here - that will be done in LoperDetailActivity
            
            // Hide loading dialog
            hideLoadingDialog()
            
            // Navigate to BTT Detail Activity
            navigateToBttDetail(currentBtt)
        }
    }
    

    private suspend fun checkAndAutoSubmitIfComplete() {
        val prefs = UserPreferences.getInstance(dataStore)
        val currentBtt = prefs.getCurrentBttNumber()
        val scannedCount = prefs.getScannedKoliCountForBtt(currentBtt)
        val totalKoli = prefs.getCurrentBttTotalKoli()
        
        if (totalKoli > 0 && scannedCount >= totalKoli) {
            // All koli scanned - auto submit after short delay
            binding.root.postDelayed({
                submitToApiAndFinish()
            }, 1000) // 1 second delay to show completion
        }
    }
    
    /**
     * Show loading dialog with progress
     */
    private var loadingDialog: AlertDialog? = null
    
    private fun showLoadingDialog(message: String) {
        runOnUiThread {
            loadingDialog = AlertDialog.Builder(this)
                .setTitle("Mohon Tunggu")
                .setMessage(message)
                .setCancelable(false)
                .create()
            loadingDialog?.show()
        }
    }
    
    private fun hideLoadingDialog() {
        runOnUiThread {
            loadingDialog?.dismiss()
            loadingDialog = null
        }
    }
    
    /**
     * Finish activity with result
     */
    private fun finishWithResult() {
        val resultIntent = Intent().apply {
            putExtra("SUCCESS", true)
            putExtra("ACTION", "NAVIGATE_TO_DETAIL")
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }
    
    /**
     * Navigate to BTT Detail Activity
     */
    private fun navigateToBttDetail(bttNumber: String) {
        // Return to previous activity with success flag
        // The previous activity (BttListFragment) will handle navigation to detail
        val resultIntent = Intent().apply {
            putExtra("SUCCESS", true)
            putExtra("ACTION", "NAVIGATE_TO_DETAIL")
            putExtra("BTT_NUMBER", bttNumber)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }
    
    /**
     * Update saveScanAndUpdateCounter to check for auto-submit
     * Uses per-BTT storage in DataStore
     */
    private suspend fun saveScanAndUpdateCounterWithAutoSubmit(koliId: String, value: String) {
        val prefs = UserPreferences.getInstance(dataStore)
        val currentBtt = prefs.getCurrentBttNumber()
        val currentTotal = prefs.getCurrentBttTotalKoli()
        
        // Check if already scanned for this BTT
        if (prefs.isKoliScannedForBtt(currentBtt, koliId)) {
             showSuccessFeedback(value, "Barcode sudah discan")
             isScanning = true
             return
        }
        
        // Check cap limit
        val scannedCount = prefs.getScannedKoliCountForBtt(currentBtt)
        if (currentTotal > 0 && scannedCount >= currentTotal) {
             showSuccessFeedback(value, "Semua koli sudah discan")
             isScanning = true
             return
        }
        
        // Save using per-BTT function
        prefs.addScannedKoliForBtt(currentBtt, koliId)
        updateCounterDisplay()
        showSuccessFeedback(value)
        isScanning = true
        
        // Check if all koli are scanned and auto-submit
        checkAndAutoSubmitIfComplete()
    }
}
