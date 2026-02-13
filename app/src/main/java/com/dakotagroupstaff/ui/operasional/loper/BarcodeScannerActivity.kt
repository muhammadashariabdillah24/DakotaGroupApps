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
import com.dakotagroupstaff.utils.ViewModelFactory
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class BarcodeScannerActivity : AppCompatActivity() {

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBarcodeScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get total koli from intent
        val totalKoli = intent.getIntExtra("TOTAL_KOLI", 0)
        lifecycleScope.launch {
            val prefs = UserPreferences.getInstance(dataStore)
            prefs.setCurrentBttTotalKoli(totalKoli)
            updateCounterDisplay()
        }

        setupUI()
        checkCameraPermission()
    }

    private fun setupUI() {
        // Close button
        binding.btnClose.setOnClickListener {
            finish()
        }
        
        // Simpan button
        binding.btnSimpan.setOnClickListener {
            // Return to fragment with navigate action
            val resultIntent = Intent().apply {
                putExtra("SUCCESS", true)
                putExtra("ACTION", "NAVIGATE_TO_DETAIL")
            }
            setResult(RESULT_OK, resultIntent)
            finish()
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
                                            
                                            saveScanAndUpdateCounter(koliIdToStore, scannedValue)
                                        }
                                        currentBtt == scannedBttNumber -> {
                                            // Same BTT - continue normally
                                            val koliIdToStore = result.data.koliId ?: scannedValue
                                            saveScanAndUpdateCounter(koliIdToStore, scannedValue)
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
        val scannedIds = prefs.getScannedBttIds()
        val totalKoli = prefs.getCurrentBttTotalKoli()
        
        binding.tvBttCounter.apply {
            text = "${scannedIds.size} / $totalKoli"
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
     */
    private suspend fun saveScanAndUpdateCounter(koliId: String, value: String) {
        val prefs = UserPreferences.getInstance(dataStore)
        val currentScanned = prefs.getScannedBttIds()
        val currentTotal = prefs.getCurrentBttTotalKoli()
        
        // Check if already scanned
        if (currentScanned.contains(koliId)) {
             showSuccessFeedback(value, "Barcode sudah discan")
             isScanning = true
             return
        }
        
        // Check cap limit
        if (currentTotal > 0 && currentScanned.size >= currentTotal) {
             showSuccessFeedback(value, "Semua koli sudah discan")
             isScanning = true
             return
        }
        
        prefs.addScannedBttId(koliId)
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

Jika anda menekan tombol Oke, Saya Mengerti maka semua hasil scan barcode koli dari BTT ini ($currentBtt) akan dihapus.
        """.trimIndent()
        
        val spannable = SpannableString(message)
        
        // Find position of warning text
        val warningStart = message.indexOf("Jika anda")
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
     * Clear previous BTT data and start with new BTT
     */
    private suspend fun clearAndStartNewBtt(scannedBtt: String, bttId: String, value: String, newTotalKoli: Int, koliId: String) {
        val prefs = UserPreferences.getInstance(dataStore)
        
        // Clear all previous data
        prefs.clearScannedBttIds()
        
        // Set new current BTT
        prefs.setCurrentBttNumber(scannedBtt)
        
        // Save first scan of new BTT
        if (newTotalKoli > 0) {
            prefs.setCurrentBttTotalKoli(newTotalKoli)
        }
        
        prefs.addScannedBttId(koliId)
        
        // Update UI
        updateCounterDisplay()
        showSuccessFeedback(value)
        
        // Allow scanning again
        isScanning = true
    }

    companion object {
        const val REQUEST_SCAN_BARCODE = 100
    }
}
