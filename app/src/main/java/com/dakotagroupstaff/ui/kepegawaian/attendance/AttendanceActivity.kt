package com.dakotagroupstaff.ui.kepegawaian.attendance

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.dakotagroupstaff.R
import com.dakotagroupstaff.data.Result
import com.dakotagroupstaff.data.local.preferences.UserPreferences
import com.dakotagroupstaff.databinding.ActivityAttendanceBinding
import com.dakotagroupstaff.util.ErrorMessageHelper
import com.dakotagroupstaff.util.SecurityChecker
import com.google.android.gms.location.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.system.exitProcess

class AttendanceActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAttendanceBinding
    private val viewModel: AttendanceViewModel by viewModel()
    private val userPreferences: UserPreferences by inject()
    
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    
    private var currentNip: String? = null
    private var currentPt: String? = null
    private var isCheckingIn: Boolean = true // Track if user is checking in or out
    
    // Location permission launcher
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                // Permission granted, get location
                getCurrentLocation()
            }
            else -> {
                // Permission denied
                Toast.makeText(
                    this,
                    "Izin lokasi diperlukan untuk absensi",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAttendanceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupLocationClient()
        setupObservers()
        setupListeners()
        
        // Load user session and agent locations
        loadUserSession()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    // CRITICAL SECURITY CHECK: Detect Fake GPS
                    if (SecurityChecker.isMockLocation(location)) {
                        showFakeGpsDialog()
                        return
                    }
                    
                    currentPt?.let { pt ->
                        viewModel.updateUserLocation(
                            location.latitude,
                            location.longitude,
                            pt
                        )
                    }
                }
            }
        }
    }
    
    private fun setupObservers() {
        // Observe attendance history loading state
        viewModel.attendanceHistory.observe(this) { result ->
            when (result) {
                is Result.Loading -> {
                    binding.swipeRefreshLayout.isRefreshing = true
                }
                is Result.Success -> {
                    binding.swipeRefreshLayout.isRefreshing = false
                    // Data sudah di-cache, siap untuk dikirim ke AttendanceHistoryActivity
                }
                is Result.Error -> {
                    binding.swipeRefreshLayout.isRefreshing = false
                    // Tetap bisa buka history dengan cache data
                }
                null -> {
                    binding.swipeRefreshLayout.isRefreshing = false
                }
            }
        }
        
        // Observe location checking state
        viewModel.isCheckingLocation.observe(this) { isChecking ->
            binding.layoutLocationLoading.visibility = if (isChecking) View.VISIBLE else View.GONE
            binding.layoutLocationInfo.visibility = if (isChecking) View.GONE else View.VISIBLE
        }
        
        // Observe nearest agent
        viewModel.nearestAgent.observe(this) { result ->
            result?.let { (agent, distance) ->
                binding.tvNearestAgent.text = agent.namaAgen
                binding.tvDistanceInfo.text = "Jarak: ${distance.toInt()}m dari cabang"
                
                // Update range status
                val rangeMeters = agent.range.toDoubleOrNull() ?: 100.0
                val isWithinRange = distance <= rangeMeters
                
                if (isWithinRange) {
                    binding.cardRangeStatus.setCardBackgroundColor(
                        ContextCompat.getColor(this, android.R.color.holo_green_dark)
                    )
                    binding.tvRangeStatus.text = "✓ Anda berada dalam jangkauan"
                    
                    // Enable attendance buttons
                    binding.btnCheckIn.isEnabled = true
                    binding.btnCheckOut.isEnabled = true
                } else {
                    binding.cardRangeStatus.setCardBackgroundColor(
                        ContextCompat.getColor(this, android.R.color.holo_red_dark)
                    )
                    binding.tvRangeStatus.text = "✗ Anda berada di luar jangkauan (max ${rangeMeters.toInt()}m)"
                    
                    // Disable attendance buttons
                    binding.btnCheckIn.isEnabled = false
                    binding.btnCheckOut.isEnabled = false
                }
            }
        }
        
        // Observe submit result
        viewModel.submitResult.observe(this) { result ->
            when (result) {
                is Result.Loading -> {
                    binding.loadingOverlay.visibility = View.VISIBLE
                }
                is Result.Success -> {
                    binding.loadingOverlay.visibility = View.GONE
                    val successMessage = ErrorMessageHelper.getAttendanceSuccessMessage(isCheckingIn)
                    Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show()
                    
                    // Reload attendance history
                    currentNip?.let { nip ->
                        currentPt?.let { pt ->
                            viewModel.loadAttendanceHistory(pt, nip)
                        }
                    }
                    
                    viewModel.resetSubmitResult()
                }
                is Result.Error -> {
                    binding.loadingOverlay.visibility = View.GONE
                    val errorMessage = ErrorMessageHelper.getAttendanceErrorMessage(isCheckingIn)
                    showErrorDialog(errorMessage)
                    viewModel.resetSubmitResult()
                }
                null -> {
                    binding.loadingOverlay.visibility = View.GONE
                }
            }
        }
        
        // Observe error messages
        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }
    
    private fun setupListeners() {
        // Pull to refresh - Refresh attendance history from API
        binding.swipeRefreshLayout.setOnRefreshListener {
            currentNip?.let { nip ->
                currentPt?.let { pt ->
                    // Force refresh dari API
                    viewModel.loadAttendanceHistory(pt, nip)
                }
            } ?: run {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
        
        // Refresh location button
        binding.btnRefreshLocation.setOnClickListener {
            currentPt?.let { pt ->
                checkLocationPermissionAndGet()
            }
        }
        
        // Check in button
        binding.btnCheckIn.setOnClickListener {
            isCheckingIn = true
            showAttendanceConfirmation("Masuk") {

                submitAttendance("M")
            }
        }
        
        // Check out button
        binding.btnCheckOut.setOnClickListener {
            isCheckingIn = false
            showAttendanceConfirmation("Pulang") {
                submitAttendance("K")
            }
        }
        
        // Attendance history button
        binding.btnAttendanceHistory.setOnClickListener {
            // Data attendance history sudah di-load di cache, langsung buka activity
            val intent = Intent(this, AttendanceHistoryActivity::class.java)
            // Kirim NIP dan PT untuk load data dari cache
            intent.putExtra("NIP", currentNip)
            intent.putExtra("PT", currentPt)
            startActivity(intent)
        }
    }
    
    private fun loadUserSession() {
        lifecycleScope.launch {
            val session = userPreferences.getSession().first()
            
            currentNip = session.nip
            currentPt = session.pt
            
            // Load agent locations
            currentPt?.let { pt ->
                viewModel.loadAgentLocations(pt)
                
                // **Load attendance history dari cache saat masuk halaman**
                currentNip?.let { nip ->
                    viewModel.loadAttendanceHistory(pt, nip)
                }
                
                // Get current location
                checkLocationPermissionAndGet()
            }
        }
    }
    
    private fun checkLocationPermissionAndGet() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                getCurrentLocation()
            }
            else -> {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }
    
    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        
        binding.layoutLocationLoading.visibility = View.VISIBLE
        binding.layoutLocationInfo.visibility = View.GONE
        
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L
        ).apply {
            setMinUpdateIntervalMillis(2000L)
            setMaxUpdates(1)
        }.build()
        
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
        
        // Add timeout handler to prevent infinite loading
        binding.root.postDelayed({
            if (binding.layoutLocationLoading.visibility == View.VISIBLE) {
                binding.layoutLocationLoading.visibility = View.GONE
                binding.layoutLocationInfo.visibility = View.VISIBLE
                Toast.makeText(
                    this,
                    "Gagal mendapatkan lokasi. Mohon coba lagi atau periksa GPS Anda",
                    Toast.LENGTH_LONG
                ).show()
            }
        }, 15000) // 15 seconds timeout
    }
    
    private fun submitAttendance(schedule: String) {
        lifecycleScope.launch {
            val session = userPreferences.getSession().first()
            
            viewModel.submitAttendance(
                pt = session.pt,
                nip = session.nip,
                schedule = schedule,
                deviceId = session.imei,
                serialNumber = session.simId
            )
        }
    }
    
    private fun showAttendanceConfirmation(type: String, onConfirm: () -> Unit) {
        val agentName = viewModel.nearestAgent.value?.first?.namaAgen ?: "Unknown"
        val distance = viewModel.nearestAgent.value?.second?.toInt() ?: 0
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Konfirmasi Absen $type")
            .setMessage("Anda akan absen $type di:\n\n$agentName\nJarak: ${distance}m\n\nLanjutkan?")
            .setPositiveButton("Ya") { dialog, _ ->
                onConfirm()
                dialog.dismiss()
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    
    private fun showErrorDialog(message: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
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
