package com.dakotagroupstaff.ui.operasional.loper

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.ToneGenerator
import android.media.AudioManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.dakotagroupstaff.R
import com.dakotagroupstaff.databinding.ActivityBarcodeScannerBinding
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import kotlinx.coroutines.launch

class BarcodeScannerActivity : AppCompatActivity() {

private lateinit var binding: ActivityBarcodeScannerBinding
    private lateinit var viewModel: LoperViewModel
    private var isScanning = true
    private var isFlashOn = false

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

        // Get ViewModel from LoperActivity's factory
        viewModel = ViewModelProvider(this)[LoperViewModel::class.java]

        setupUI()
        checkCameraPermission()
    }

    private fun setupUI() {
        // Close button
        binding.btnClose.setOnClickListener {
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
                        if (result.data.found) {
                            // Success - green flash + beep
                            showSuccess(result.data.value)
                        } else {
                            // Not found
                            showError(result.data.message ?: "Barcode tidak ditemukan")
                        }
                    }
                    is com.dakotagroupstaff.data.Result.Error -> {
                        showError(result.message)
                    }
                }
            }
        }
    }

    private fun showSuccess(value: String) {
        // Green flash animation
        binding.scannerOverlay.showSuccess()

        // Play beep sound
        playBeepSound()

        // Show success message
        binding.tvScanStatus.text = "Scan berhasil!"
        binding.tvScanStatus.setTextColor(getColor(android.R.color.holo_green_dark))

        // Return result
        val resultIntent = Intent().apply {
            putExtra("SCANNED_VALUE", value)
            putExtra("SUCCESS", true)
        }
        setResult(RESULT_OK, resultIntent)

        // Close after delay
        binding.root.postDelayed({
            finish()
        }, 1000)
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
