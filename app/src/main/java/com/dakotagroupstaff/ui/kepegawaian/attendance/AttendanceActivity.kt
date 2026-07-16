package com.dakotagroupstaff.ui.kepegawaian.attendance

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.location.LocationManager
import android.provider.Settings
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.dakotagroupstaff.ui.base.BaseActivity
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
import com.dakotagroupstaff.data.local.entity.AgentLocationEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.system.exitProcess

class AttendanceActivity : BaseActivity() {
    
    // Kode jabatan yang TIDAK diizinkan menggunakan fitur WFH
    private val WFH_EXCLUDED_JAB_CODES = setOf(
        "0030", "0031", "0039", "0040", "0036", "0024", "0019",
        "0015", "0016", "0014", "0011", "0010", "0009", "0004",
        "0001", "0028", "0027",
        // Format tanpa leading zero (fallback)
        "30", "31", "39", "40", "36", "24", "19",
        "15", "16", "14", "11", "10", "9", "4",
        "1", "28", "27"
    )

    private lateinit var binding: ActivityAttendanceBinding
    private val viewModel: AttendanceViewModel by viewModel()
    private val userPreferences: UserPreferences by inject()
    
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    
    private var currentNip: String? = null
    private var currentPt: String? = null
    private var isCheckingIn: Boolean = true // Track if user is checking in or out
    private var isRefreshingLocation: Boolean = false // Track source of location request
    private var selectedWfhAgent: AgentLocationEntity? = null // Store selected agent for WFH
    
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
                // Permission denied — cek apakah bisa tampilkan rationale lagi
                val canShowRationale = shouldShowRequestPermissionRationale(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
                if (canShowRationale) {
                    // Ditolak sekali tapi belum permanen, tampilkan dialog rationale lagi
                    showLocationPermissionRationaleDialog()
                } else {
                    // Ditolak permanen ("Jangan tanya lagi"), arahkan ke Settings
                    showLocationPermissionDeniedDialog()
                }
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
        
        // Observe nearest agent (WFO only)
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
                    
                    // Enable WFO attendance buttons only
                    binding.btnCheckIn.isEnabled = true
                    binding.btnCheckOut.isEnabled = true
                } else {
                    binding.cardRangeStatus.setCardBackgroundColor(
                        ContextCompat.getColor(this, android.R.color.holo_red_dark)
                    )
                    binding.tvRangeStatus.text = "✗ Anda berada di luar jangkauan (max ${rangeMeters.toInt()}m)"
                    
                    // Disable WFO attendance buttons only — WFH tidak terpengaruh jarak
                    binding.btnCheckIn.isEnabled = false
                    binding.btnCheckOut.isEnabled = false
                }
                
                // WFH buttons state is independent of GPS range — only depends on agent selection
                updateWfhButtonsState()
            }
        }

        // Observe user location — update GPS coordinates display in real-time
        viewModel.userLocation.observe(this) { location ->
            location?.let { (lat, lon) ->
                val latStr = String.format("%.6f", lat)
                val lonStr = String.format("%.6f", lon)
                binding.tvGpsCoordinates.text = "Lat: $latStr  |  Lon: $lonStr"
                // Refresh WFH button state (GPS is now available)
                updateWfhButtonsState()
            } ?: run {
                binding.tvGpsCoordinates.text = "Menunggu sinyal GPS..."
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
        
        // Observe error messages — gunakan dialog agar user tidak melewatkan pesan error
        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                showErrorDialog(it)
                viewModel.clearError()
            }
        }
    }
    
    private fun setupListeners() {
        // Pull to refresh - Refresh attendance history from API
        binding.swipeRefreshLayout.setOnRefreshListener {
            currentNip?.let { nip ->
                currentPt?.let { pt ->
                    // Periksa izin lokasi terlebih dahulu sebelum refresh
                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        // Belum ada izin lokasi, hentikan refresh spinner dan tampilkan dialog
                        binding.swipeRefreshLayout.isRefreshing = false
                        isRefreshingLocation = false
                        checkLocationPermissionAndGet()
                    } else {
                        // Izin sudah ada, refresh history
                        viewModel.loadAttendanceHistory(pt, nip)
                    }
                }
            } ?: run {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
        
        // Refresh location button
        binding.btnRefreshLocation.setOnClickListener {
            isRefreshingLocation = true
            // 1. Cek izin permission lokasi terlebih dahulu
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Izin belum diberikan → tampilkan dialog rationale
                checkLocationPermissionAndGet()
                return@setOnClickListener
            }
            // 2. Izin sudah ada, cek apakah GPS/layanan lokasi perangkat aktif
            if (!isLocationEnabled()) {
                showLocationServicesDialog()
                return@setOnClickListener
            }
            // 3. Semua OK → ambil lokasi
            getCurrentLocation()
        }
        
        // Select Agent WFH button
        binding.btnSelectAgentWfh.setOnClickListener {
            showSelectAgentDialog()
        }

        // Check in button (WFO)
        binding.btnCheckIn.setOnClickListener {
            isCheckingIn = true
            showAttendanceConfirmation("Masuk", isWfh = false) {
                submitAttendance("M")
            }
        }
        
        // Check out button (WFO)
        binding.btnCheckOut.setOnClickListener {
            isCheckingIn = false
            showAttendanceConfirmation("Pulang", isWfh = false) {
                submitAttendance("K")
            }
        }
        
        // Check in button (WFH)
        binding.btnCheckInWfh.setOnClickListener {
            if (selectedWfhAgent == null) {
                Toast.makeText(this, "Silakan pilih agen/cabang terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            isCheckingIn = true
            showAttendanceConfirmation("Masuk (WFH)", isWfh = true) {
                submitAttendance("HM", selectedWfhAgent?.md5Code)
            }
        }
        
        // Check out button (WFH)
        binding.btnCheckOutWfh.setOnClickListener {
            if (selectedWfhAgent == null) {
                Toast.makeText(this, "Silakan pilih agen/cabang terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            isCheckingIn = false
            showAttendanceConfirmation("Keluar (WFH)", isWfh = true) {
                submitAttendance("HK", selectedWfhAgent?.md5Code)
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
        var isFirstEmission = true
        lifecycleScope.launch {
            userPreferences.getSession().collect { session ->
                currentNip = session.nip
                currentPt = session.pt

                // Kontrol visibilitas WFH berdasarkan kode jabatan
                val jabCodeClean = session.jabCode.trim()
                val isExcluded = WFH_EXCLUDED_JAB_CODES.contains(jabCodeClean) ||
                                 WFH_EXCLUDED_JAB_CODES.contains(jabCodeClean.padStart(4, '0'))

                // WFH disembunyikan jika jabCode termasuk dalam daftar yang dikecualikan.
                // Jika jabCode masih kosong (bio belum di-fetch), WFH ditampilkan sementara.
                binding.layoutWfhSection.visibility = if (isExcluded) View.GONE else View.VISIBLE

                // Inisialisasi one-time: hanya dijalankan pada emisi pertama
                if (isFirstEmission) {
                    isFirstEmission = false
                    val pt = session.pt
                    val nip = session.nip
                    viewModel.loadAgentLocations(pt)
                    viewModel.loadAttendanceHistory(pt, nip)
                    checkLocationPermissionAndGet()
                }
            }
        }
    }
    
    /**
     * Memeriksa apakah GPS atau Network Location provider aktif di perangkat.
     * Berbeda dari permission — ini adalah status tombol lokasi di Settings perangkat.
     */
    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
    
    /**
     * Dialog yang muncul ketika GPS/layanan lokasi perangkat tidak aktif.
     * Mengarahkan pengguna ke Settings Lokasi (bukan Settings app).
     */
    private fun showLocationServicesDialog() {
        MaterialAlertDialogBuilder(this)
            .setIcon(R.drawable.ic_location)
            .setTitle("Lokasi Perangkat Tidak Aktif")
            .setMessage(
                "GPS atau layanan lokasi perangkat Anda sedang tidak aktif.\n\n" +
                "Fitur absensi memerlukan lokasi aktif untuk:\n" +
                "• Mendeteksi posisi Anda saat ini\n" +
                "• Memvalidasi jarak ke kantor/cabang\n\n" +
                "Aktifkan lokasi perangkat terlebih dahulu."
            )
            .setCancelable(false)
            .setPositiveButton("Aktifkan Lokasi") { dialog, _ ->
                dialog.dismiss()
                // Buka halaman pengaturan lokasi perangkat
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("Nanti Saja") { dialog, _ ->
                dialog.dismiss()
                isRefreshingLocation = false
            }
            .show()
    }
    
    private fun checkLocationPermissionAndGet() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Izin sudah ada, cek selanjutnya apakah GPS aktif
                if (!isLocationEnabled()) {
                    showLocationServicesDialog()
                } else {
                    getCurrentLocation()
                }
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                // Pernah ditolak sebelumnya, tampilkan dialog rationale terlebih dahulu
                showLocationPermissionRationaleDialog()
            }
            else -> {
                // Baru pertama kali request atau belum pernah ditolak
                showLocationPermissionRationaleDialog()
            }
        }
    }
    
    /**
     * Dialog penjelasan mengapa izin lokasi diperlukan.
     * Ditampilkan sebelum sistem meminta izin, agar pengguna mengerti konteksnya.
     */
    private fun showLocationPermissionRationaleDialog() {
        MaterialAlertDialogBuilder(this)
            .setIcon(R.drawable.ic_location)
            .setTitle("Izin Lokasi Diperlukan")
            .setMessage(
                "Aplikasi memerlukan akses lokasi Anda untuk:\n\n" +
                "• Mendeteksi lokasi absensi terdekat\n" +
                "• Memvalidasi jarak Anda dari kantor/cabang\n" +
                "• Mencegah penyalahgunaan data absensi\n\n" +
                "Tanpa izin lokasi, fitur absensi tidak dapat digunakan."
            )
            .setCancelable(false)
            .setPositiveButton("Izinkan") { dialog, _ ->
                dialog.dismiss()
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
                // Jika dibatalkan dari proses refresh, pastikan spinner berhenti
                isRefreshingLocation = false
            }
            .show()
    }
    
    /**
     * Dialog yang ditampilkan ketika izin lokasi ditolak secara permanen.
     * Mengarahkan pengguna ke halaman Pengaturan aplikasi.
     */
    private fun showLocationPermissionDeniedDialog() {
        MaterialAlertDialogBuilder(this)
            .setIcon(R.drawable.ic_location)
            .setTitle("Izin Lokasi Ditolak")
            .setMessage(
                "Anda telah menonaktifkan izin lokasi secara permanen.\n\n" +
                "Untuk menggunakan fitur absensi, aktifkan kembali izin lokasi melalui:\n" +
                "Pengaturan → Aplikasi → Dakota Group Staff → Izin → Lokasi"
            )
            .setCancelable(false)
            .setPositiveButton("Buka Pengaturan") { dialog, _ ->
                dialog.dismiss()
                // Buka halaman pengaturan izin aplikasi
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton("Nanti Saja") { dialog, _ ->
                dialog.dismiss()
                isRefreshingLocation = false
            }
            .show()
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
    
    private fun submitAttendance(schedule: String, wfhKodeCabang: String? = null) {
        lifecycleScope.launch {
            val session = userPreferences.getSession().first()
            
            viewModel.submitAttendance(
                pt = session.pt,
                nip = session.nip,
                schedule = schedule,
                wfhKodeCabang = wfhKodeCabang,
                deviceId = session.imei,
                serialNumber = session.simId
            )
        }
    }
    
    private fun showAttendanceConfirmation(type: String, isWfh: Boolean = false, onConfirm: () -> Unit) {
        val agentName: String
        val distanceText: String
        
        if (isWfh) {
            agentName = selectedWfhAgent?.namaAgen ?: "Unknown"
            distanceText = "Mode: Work From Home"
        } else {
            agentName = viewModel.nearestAgent.value?.first?.namaAgen ?: "Unknown"
            val distance = viewModel.nearestAgent.value?.second?.toInt() ?: 0
            distanceText = "Jarak: ${distance}m"
        }
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Konfirmasi Absen $type")
            .setMessage("Anda akan absen $type di:\n\n$agentName\n$distanceText\n\nLanjutkan?")
            .setPositiveButton("Ya") { dialog, _ ->
                onConfirm()
                dialog.dismiss()
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    
    private fun showSelectAgentDialog() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_select_agent, null)
        dialog.setContentView(dialogView)

        val btnClose = dialogView.findViewById<android.widget.ImageButton>(R.id.btnClose)
        val etSearch = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etSearch)
        val rvAgents = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvAgents)
        val tvEmpty = dialogView.findViewById<android.widget.TextView>(R.id.tvEmpty)

        val agentAdapter = AgentAdapter { selectedAgent ->
            selectedWfhAgent = selectedAgent
            binding.btnSelectAgentWfh.text = selectedAgent.namaAgen
            // Update WFH button state after agent is selected
            updateWfhButtonsState()
            dialog.dismiss()
        }

        rvAgents.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        rvAgents.adapter = agentAdapter

        // Populate adapter
        viewModel.agentLocations.value?.let { result ->
            if (result is Result.Success) {
                agentAdapter.submitFullList(result.data)
                if (result.data.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                    rvAgents.visibility = View.GONE
                } else {
                    tvEmpty.visibility = View.GONE
                    rvAgents.visibility = View.VISIBLE
                }
            }
        }

        // Search feature
        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                agentAdapter.filter(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * Update WFH button states independently of GPS range.
     * WFH buttons are enabled only when:
     * 1. An agent/branch has been selected
     * 2. GPS location is available (to capture user's real coordinates)
     */
    private fun updateWfhButtonsState() {
        val isAgentSelected = selectedWfhAgent != null
        val isLocationAvailable = viewModel.userLocation.value != null
        val canSubmitWfh = isAgentSelected && isLocationAvailable
        binding.btnCheckInWfh.isEnabled = canSubmitWfh
        binding.btnCheckOutWfh.isEnabled = canSubmitWfh
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
