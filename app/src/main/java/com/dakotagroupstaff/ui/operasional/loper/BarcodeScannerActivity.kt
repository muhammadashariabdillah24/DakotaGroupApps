package com.dakotagroupstaff.ui.operasional.loper

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.view.View
import android.widget.Toast
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
                        if (bttId.isNotEmpty()) {
                            lifecycleScope.launch {
                                // Save to DataStore
                                val prefs = UserPreferences.getInstance(dataStore)
                                prefs.addScannedBttId(bttId)
                                
                                // Update counter display
                                updateCounterDisplay()
                                
                               // Show success feedback
                                showSuccessFeedback(result.data.value)
                                
                                // Allow continuous scanning
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

    private fun showSuccessFeedback(value: String) {
        // Green flash animation
        binding.scannerOverlay.showSuccess()

        // Play beep sound
        playBeepSound()

        // Show success message briefly
        binding.tvScanStatus.text = "Scan berhasil! BTT disimpan."
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

    companion object {
        const val REQUEST_SCAN_BARCODE = 100
    }
}
