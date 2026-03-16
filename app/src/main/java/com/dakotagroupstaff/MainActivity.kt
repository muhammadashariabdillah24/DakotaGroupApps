package com.dakotagroupstaff

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.dakotagroupstaff.data.repository.AuthRepository
import com.dakotagroupstaff.databinding.ActivityMainBinding
import com.dakotagroupstaff.ui.adapter.RecentMenuAdapter
import com.dakotagroupstaff.ui.dialog.PhotoViewerDialog
import com.dakotagroupstaff.ui.kepegawaian.KepegawaianMenuActivity
import com.dakotagroupstaff.ui.login.LoginActivity
import com.dakotagroupstaff.ui.login.LoginViewModel
import com.dakotagroupstaff.ui.main.MainViewModel
import com.dakotagroupstaff.util.ImageUrlHelper
import com.dakotagroupstaff.util.SecurityChecker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private val loginViewModel: LoginViewModel by viewModel()
    private val mainViewModel: MainViewModel by viewModel()
    private val authRepository: AuthRepository by inject()
    private lateinit var recentMenuAdapter: RecentMenuAdapter
    private var isForcedLogoutDialogShowing = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Additional root check with user-friendly dialog
        checkRootedDeviceWithDialog()
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupRecentMenus()
        checkSessionAndSetupUI()
    }

    override fun onResume() {
        super.onResume()
        // Cek validitas perangkat setiap kali halaman kembali ke foreground
        checkDeviceValidity()
    }

    /**
     * Periksa apakah perangkat saat ini masih cocok dengan yang terdaftar di database.
     * Jika tidak cocok (login di perangkat lain), tampilkan dialog force logout.
     */
    private fun checkDeviceValidity() {
        lifecycleScope.launch {
            try {
                val result = authRepository.validateDevice()
                if (!result.valid && !isForcedLogoutDialogShowing) {
                    isForcedLogoutDialogShowing = true
                    showForcedLogoutDialog()
                }
            } catch (e: Exception) {
                // Gagal cek — abaikan agar tidak mengganggu saat offline
                android.util.Log.w("MainActivity", "Device validation failed, skipping: ${e.message}")
            }
        }
    }
    
    /**
     * Additional root detection with user-friendly dialog
     * This is a fallback check in case the app somehow bypassed Application-level check
     */
    private fun checkRootedDeviceWithDialog() {
        if (SecurityChecker.isDeviceRooted(this)) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Perangkat Terdeteksi Root")
                .setMessage("Aplikasi Dakota Group Staff tidak dapat digunakan pada perangkat yang telah di-root karena alasan keamanan.")
                .setCancelable(false)
                .setPositiveButton("Baik, Saya Mengerti") { _, _ ->
                    finishAffinity()
                    exitProcess(0)
                }
                .show()
        }
    }
    
    private fun setupRecentMenus() {
        recentMenuAdapter = RecentMenuAdapter { menu ->
            try {
                val intent = Intent(this, Class.forName(menu.activityClass))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Gagal membuka menu", Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.rvRecentMenus.adapter = recentMenuAdapter
        
        mainViewModel.recentMenus.observe(this) { menus ->
            if (menus.isNullOrEmpty()) {
                binding.tvHistoryTitle.visibility = View.GONE
                binding.rvRecentMenus.visibility = View.GONE
            } else {
                binding.tvHistoryTitle.visibility = View.VISIBLE
                binding.rvRecentMenus.visibility = View.VISIBLE
                recentMenuAdapter.submitList(menus)
            }
        }
    }
    
    private fun checkSessionAndSetupUI() {
        loginViewModel.getSession().observe(this) { session ->
            if (!session.isLoggedIn || !isValidSession(session)) {
                // User belum login atau session tidak valid, redirect ke LoginActivity
                navigateToLogin()
            } else {
                // User sudah login dengan session valid, tampilkan dashboard
                showDashboard()
                setupDashboard(session)
            }
        }
    }
    
    /**
     * Validate session to ensure all required fields are present
     * This prevents access with incomplete/stale session data
     */
    private fun isValidSession(session: com.dakotagroupstaff.data.local.model.UserSession): Boolean {
        // Check if NIP is not empty
        if (session.nip.isBlank()) {
            android.util.Log.w("MainActivity", "Session invalid: NIP is blank")
            return false
        }
        
        // Check if PT is valid
        if (session.pt.isBlank() || session.pt !in listOf("A", "B", "C")) {
            android.util.Log.w("MainActivity", "Session invalid: PT is invalid (${session.pt})")
            return false
        }
        
        // Check if nama is not empty
        if (session.nama.isBlank()) {
            android.util.Log.w("MainActivity", "Session invalid: Nama is blank")
            return false
        }
        
        android.util.Log.d("MainActivity", "Session valid for NIP: ${session.nip}")
        return true
    }
    
    private fun showDashboard() {
        // Tampilkan semua komponen dashboard
        binding.cardHeader.visibility = View.VISIBLE
        binding.tvMenuTitle.visibility = View.VISIBLE
        binding.layoutMenuButtons.visibility = View.VISIBLE
        binding.btnSettings.visibility = View.VISIBLE
    }
    
    private fun setupDashboard(session: com.dakotagroupstaff.data.local.model.UserSession) {
        // Set employee name
        binding.tvEmployeeName.text = session.nama
        
        // Set employee NIP
        binding.tvEmployeeNip.text = getString(R.string.nip) + ": " + session.nip
        
        // Set company name
        val companyName = when (session.pt) {
            "A" -> getString(R.string.pt_dbs)
            "B" -> getString(R.string.pt_dlb)
            "C" -> getString(R.string.pt_logistik)
            else -> getString(R.string.pt_logistik)
        }
        binding.tvCompany.text = companyName
        
        // Load profile photo from backend
        loadProfilePhoto(session.nip, session.pt)
        
        // Show/Hide "Lihat Surat Tugas" button based on TaskCode
        // TaskCode > 0 means user has active task
        val taskCode = session.taskCode.toIntOrNull() ?: 0
        binding.btnLihatSuratTugas.visibility = if (taskCode > 0) View.VISIBLE else View.GONE
        
        // Setup click listeners
        setupClickListeners(session)
    }
    
    private fun loadProfilePhoto(nip: String, pt: String) {
        // Construct profile photo URL using ImageUrlHelper
        val photoUrl = ImageUrlHelper.constructPhotoUrl(pt, nip)
        
        // Debug log
        android.util.Log.d("MainActivity", "=== PROFILE PHOTO DEBUG ===")
        android.util.Log.d("MainActivity", "NIP: $nip")
        android.util.Log.d("MainActivity", "Company (PT): $pt")
        android.util.Log.d("MainActivity", "Photo URL: $photoUrl")
        android.util.Log.d("MainActivity", "=========================")
        
        // Load image with Glide with detailed error logging
        Glide.with(this)
            .load(photoUrl)
            .placeholder(R.drawable.ic_launcher_foreground)
            .error(R.drawable.ic_launcher_foreground)
            .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                override fun onLoadFailed(
                    e: com.bumptech.glide.load.engine.GlideException?,
                    model: Any?,
                    target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    android.util.Log.e("MainActivity", "=== PROFILE PHOTO LOAD FAILED ===")
                    android.util.Log.e("MainActivity", "URL: $photoUrl")
                    android.util.Log.e("MainActivity", "Error: ${e?.message}")
                    android.util.Log.e("MainActivity", "Root causes:")
                    e?.rootCauses?.forEach { cause ->
                        android.util.Log.e("MainActivity", "  - ${cause.message}")
                    }
                    android.util.Log.e("MainActivity", "==================================")
                    return false // Return false to allow Glide to handle error drawable
                }

                override fun onResourceReady(
                    resource: android.graphics.drawable.Drawable,
                    model: Any,
                    target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                    dataSource: com.bumptech.glide.load.DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    android.util.Log.d("MainActivity", "=== PROFILE PHOTO LOADED SUCCESSFULLY ===")
                    android.util.Log.d("MainActivity", "URL: $photoUrl")
                    android.util.Log.d("MainActivity", "Data source: $dataSource")
                    android.util.Log.d("MainActivity", "=========================================")
                    return false // Return false to allow Glide to display the image
                }
            })
            .circleCrop()
            .into(binding.ivProfile)
    }
    
    private fun setupClickListeners(session: com.dakotagroupstaff.data.local.model.UserSession) {
        // Profile photo click - show fullscreen modal
        binding.cardProfileFrame.setOnClickListener {
            showProfilePhotoDialog(session.nip)
        }
        
        // KEPEGAWAIAN menu
        binding.cardKepegawaian.setOnClickListener {
            val intent = Intent(this, KepegawaianMenuActivity::class.java)
            startActivity(intent)
        }
        
        // OPERASIONAL menu
        binding.cardOperasional.setOnClickListener {
            val intent = Intent(this, com.dakotagroupstaff.ui.operasional.OperasionalMenuActivity::class.java)
            startActivity(intent)
        }
        
        // Lihat Surat Tugas button (Quick Access Modal)
        binding.btnLihatSuratTugas.setOnClickListener {
            val intent = Intent(this, com.dakotagroupstaff.ui.operasional.QuickAccessActivity::class.java)
            startActivity(intent)
        }
        
        // Settings button
        binding.btnSettings.setOnClickListener {
            val intent = Intent(this, com.dakotagroupstaff.ui.settings.SettingsActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun showProfilePhotoDialog(nip: String) {
        // Get current session to get company code
        loginViewModel.getSession().observe(this) { session ->
            if (session.isLoggedIn) {
                val photoUrl = ImageUrlHelper.constructPhotoUrl(session.pt, nip)
                
                // Debug log
                android.util.Log.d("MainActivity", "=== PHOTO DIALOG DEBUG ===")
                android.util.Log.d("MainActivity", "NIP: $nip")
                android.util.Log.d("MainActivity", "Company (PT): ${session.pt}")
                android.util.Log.d("MainActivity", "Photo URL: $photoUrl")
                android.util.Log.d("MainActivity", "========================")
                
                // Show fullscreen photo viewer dialog
                val dialog = PhotoViewerDialog(this, photoUrl)
                dialog.show()
            }
        }
    }
    
    private fun showLogoutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.logout))
            .setMessage("Apakah Anda yakin ingin logout?")
            .setPositiveButton("Ya") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Tidak") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Dialog force logout — tampil ketika akun terdeteksi login di perangkat lain.
     * Tidak bisa dibatalkan (setCancelable = false), dan harus menekan 'Oke'.
     */
    private fun showForcedLogoutDialog() {
        if (isFinishing || isDestroyed) return
        MaterialAlertDialogBuilder(this)
            .setTitle("Peringatan")
            .setMessage("Akun anda akan logout dari perangkat ini karena telah login diperangkat lain!")
            .setCancelable(false)
            .setPositiveButton("Oke") { _, _ ->
                performForcedLogout()
            }
            .show()
    }

    /**
     * Hapus semua sesi lokal dan arahkan ke halaman login.
     * Dipanggil saat force logout karena login di perangkat lain.
     */
    private fun performForcedLogout() {
        isForcedLogoutDialogShowing = false
        
        // Sign out dari Google jika login menggunakan Google
        val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
            com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
        ).build()
        val googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(this, gso)
        googleSignInClient.signOut()
        
        // Gunakan loginViewModel.logout() yang sudah menangani clear DataStore + revoke refresh token
        lifecycleScope.launch {
            loginViewModel.logout()
            navigateToLogin()
        }
    }

    private fun performLogout() {
        // Sign out dari Google jika login menggunakan Google
        val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
            com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
        ).build()
        val googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(this, gso)
        googleSignInClient.signOut()
        
        lifecycleScope.launch {
            loginViewModel.logout()
            Toast.makeText(this@MainActivity, "Logout berhasil", Toast.LENGTH_SHORT).show()
            navigateToLogin()
        }
    }
    
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
