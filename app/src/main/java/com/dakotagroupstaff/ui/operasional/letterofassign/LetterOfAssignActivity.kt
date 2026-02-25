package com.dakotagroupstaff.ui.operasional.letterofassign

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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
import com.google.android.gms.location.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Letter of Assign Activity for PT DBS and PT DLB only
 * Features:
 * - GPS tracking every 15 minutes
 * - Checkpoint check-in with photo and odometer
 * - Tab view for Bongkar (unload) and Muat (load)
 * - Route management with main and backup routes
 */
class LetterOfAssignActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLetterOfAssignBinding
    private val viewModel: LetterOfAssignViewModel by viewModel()
    private val userPreferences: UserPreferences by inject()
    
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    
    private val checkpointAdapter = CheckpointAdapter { checkpoint ->
        // Handle checkpoint check-in
        showCheckinDialog(checkpoint)
    }
    
    private var currentNip: String? = null
    private var currentPt: String? = null
    
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
        
        setupToolbar()
        setupRecyclerView()
        checkPermissions()
        initLocationClient()
        loadUserData()
        observeViewModel()
        setupClickListeners()
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
    
    private fun setupClickListeners() {
        binding.btnCompleteAssignment.setOnClickListener {
            // TODO: Show confirmation dialog before completing
            viewModel.completeAssignment()
            Toast.makeText(this, "Surat tugas diselesaikan", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    private fun loadUserData() {
        lifecycleScope.launch {
            currentNip = userPreferences.getNip().first()
            currentPt = userPreferences.getPt().first()
            
            // Load letter of assign data
            currentNip?.let { nip ->
                currentPt?.let { pt ->
                    viewModel.loadLetterOfAssign(nip, pt)
                }
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
                        
                        // Display checkpoints
                        checkpointAdapter.submitList(result.data)
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
                    // Reload data
                    currentNip?.let { nip ->
                        currentPt?.let { pt ->
                            viewModel.loadLetterOfAssign(nip, pt)
                        }
                    }
                }
                is Result.Error -> {
                    Toast.makeText(this, "Check-in gagal: ${result.message}", Toast.LENGTH_SHORT).show()
                }
                is Result.Loading -> {
                    // Show loading if needed
                }
            }
        }
    }
    
    private fun showCheckinDialog(checkpoint: com.dakotagroupstaff.data.remote.response.LetterOfAssignDetail) {
        // TODO: Implement check-in dialog with camera for selfie and KM input
        // For now, just submit with dummy data
        
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
                    currentNip?.let { nip ->
                        viewModel.submitCheckpoint(
                            agenID = checkpoint.trKdCabang,
                            km = "0", // TODO: Get from dialog input
                            lat = it.latitude.toString(),
                            lon = it.longitude.toString(),
                            urlpic = "", // TODO: Upload photo and get URL
                            nip = nip,
                            urut = checkpoint.trUrut
                        )
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
                    // Update location to server
                    viewModel.updateLocation(location.latitude, location.longitude)
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
    
    override fun onDestroy() {
        super.onDestroy()
        // Stop location updates
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
