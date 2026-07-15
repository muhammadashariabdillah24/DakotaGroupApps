package com.dakotagroupstaff

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.dakotagroupstaff.data.Result
import com.dakotagroupstaff.data.local.preferences.UserPreferences
import com.google.gson.Gson
import com.dakotagroupstaff.databinding.ActivityMainBinding
import com.dakotagroupstaff.ui.adapter.RecentMenuAdapter
import com.dakotagroupstaff.ui.base.BaseActivity
import com.dakotagroupstaff.ui.dialog.PhotoViewerDialog
import com.dakotagroupstaff.ui.kepegawaian.KepegawaianMenuActivity
import com.dakotagroupstaff.ui.login.LoginViewModel
import com.dakotagroupstaff.ui.main.MainViewModel
import com.dakotagroupstaff.util.ImageUrlHelper
import com.dakotagroupstaff.util.SecurityChecker
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.system.exitProcess
import com.dakotagroupstaff.data.local.preferences.dataStore
import com.dakotagroupstaff.data.remote.response.EmployeeBioRequest
import com.dakotagroupstaff.data.remote.retrofit.ApiConfig

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private val loginViewModel: LoginViewModel by viewModel()
    private val mainViewModel: MainViewModel by viewModel()
    private val userPreferences: UserPreferences by inject()
    private lateinit var recentMenuAdapter: RecentMenuAdapter

    // Session yang sedang aktif (disimpan saat dashboard pertama kali dibuka)
    private var currentSession: com.dakotagroupstaff.data.local.model.UserSession? = null

    // Dialog force logout
    private var forceLogoutDialog: AlertDialog? = null
    private var forceLogoutTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkRootedDeviceWithDialog()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecentMenus()
        checkSessionAndSetupUI()
        setupSwipeRefresh()
    }

    override fun onDestroy() {
        forceLogoutTimer?.cancel()
        try { forceLogoutDialog?.dismiss() } catch (_: Exception) {}
        forceLogoutDialog = null
        super.onDestroy()
    }

    // ─── Root Detection ───────────────────────────────────────────────────────

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

    // ─── Recent Menus ─────────────────────────────────────────────────────────

    private fun setupRecentMenus() {
        recentMenuAdapter = RecentMenuAdapter { menu ->
            try {
                startActivity(Intent(this, Class.forName(menu.activityClass)))
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

    // ─── Session & UI Setup ───────────────────────────────────────────────────

    /**
     * Cek session lokal (DataStore) dan tampilkan dashboard.
     * Verifikasi ke server dilakukan di CheckerActivity (saat booting).
     * Di sini kita hanya mengandalkan data yang sudah terverifikasi dari Checker.
     */
    private fun checkSessionAndSetupUI() {
        loginViewModel.getSession().observe(this) { session ->
            if (!session.isLoggedIn || session.nip.isBlank()) {
                navigateToLogin()
            } else {
                currentSession = session
                showDashboard()
                setupDashboard(session)
            }
        }
    }

    // ─── Pull-to-Refresh (memanggil Login API) ────────────────────────────────

    /**
     * Pull-to-refresh memanggil Login API dengan kredensial yang tersimpan.
     * Jika berhasil → data diperbarui, user tetap di app.
     * Jika gagal → NIP sudah login di perangkat lain → force logout.
     */
    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            val session = currentSession ?: return@setOnRefreshListener

            if (session.nip.isBlank() || session.imei.isBlank() || session.simId.isBlank()) {
                binding.swipeRefreshLayout.isRefreshing = false
                return@setOnRefreshListener
            }

            Log.d("MainActivity", "Pull-to-refresh: memanggil login API...")

            loginViewModel.login(
                pt           = session.pt,
                nip          = session.nip,
                deviceId     = session.imei,
                serialNumber = session.simId,
                email        = session.email
            ).observe(this) { result ->
                when (result) {
                    is Result.Loading -> { /* Loading indicator sudah berjalan */ }
                    is Result.Success -> {
                        binding.swipeRefreshLayout.isRefreshing = false
                        Log.d("MainActivity", "Pull-to-refresh: perangkat terverifikasi ✓")
                        Toast.makeText(this, "Data diperbarui ✓", Toast.LENGTH_SHORT).show()
                    }
                    is Result.Error -> {
                        binding.swipeRefreshLayout.isRefreshing = false
                        Log.w("MainActivity", "Pull-to-refresh: login gagal → force logout")
                        showForceLogoutDialog()
                    }
                }
            }
        }
    }

    // ─── Force Logout Dialog ──────────────────────────────────────────────────

    private fun showForceLogoutDialog() {
        if (isFinishing || isDestroyed) return
        if (forceLogoutDialog?.isShowing == true) return

        val dialogView  = layoutInflater.inflate(R.layout.dialog_force_logout, null)
        val tvCountdown = dialogView.findViewById<TextView>(R.id.tv_countdown)
        val btnKeluar   = dialogView.findViewById<MaterialButton>(R.id.btn_keluar)

        forceLogoutDialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnKeluar.setOnClickListener {
            forceLogoutTimer?.cancel()
            forceLogoutDialog?.dismiss()
            forceLogoutDialog = null
            performForceLogout()
        }

        forceLogoutDialog?.show()

        forceLogoutTimer = object : CountDownTimer(7000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val sLeft = (millisUntilFinished / 1000) + 1
                tvCountdown?.text = "($sLeft)"
            }
            override fun onFinish() {
                forceLogoutDialog?.dismiss()
                forceLogoutDialog = null
                performForceLogout()
            }
        }.start()
    }

    private fun performForceLogout() {
        lifecycleScope.launch {
            try {
                loginViewModel.clearAllData()
            } catch (e: Exception) {
                Log.e("MainActivity", "Error saat force logout: ${e.message}")
            } finally {
                navigateToLogin()
            }
        }
    }

    // ─── Dashboard UI ─────────────────────────────────────────────────────────

    private fun showDashboard() {
        binding.cardHeader.visibility         = View.VISIBLE
        binding.tvMenuTitle.visibility        = View.VISIBLE
        binding.layoutMenuButtons.visibility  = View.VISIBLE
        binding.btnSettings.visibility        = View.VISIBLE
    }

    private fun setupDashboard(session: com.dakotagroupstaff.data.local.model.UserSession) {
        binding.tvEmployeeName.text = session.nama
        binding.tvEmployeeNip.text  = getString(R.string.nip) + ": " + session.nip

        val companyName = when (session.pt) {
            "A"  -> getString(R.string.pt_dbs)
            "B"  -> getString(R.string.pt_dlb)
            "C"  -> getString(R.string.pt_logistik)
            else -> getString(R.string.pt_logistik)
        }
        binding.tvCompany.text = companyName

        loadProfilePhoto(session.nip, session.pt)

        val taskCode = session.taskCode.toIntOrNull() ?: 0
        binding.btnLihatSuratTugas.visibility = if (taskCode > 0) View.VISIBLE else View.GONE

        setupClickListeners(session)
        fetchAndSaveEmployeeBio(session.pt, session.nip)
    }

    private fun fetchAndSaveEmployeeBio(pt: String, nip: String) {
        lifecycleScope.launch {
            try {
                val apiService = ApiConfig.getApiService(userPreferences = userPreferences)
                val response = apiService.getEmployeeBio(pt, EmployeeBioRequest(nip))
                val data = response.data?.firstOrNull()
                if (data != null) {
                    val jabCode = data.jabCode?.trim() ?: ""
                    val jabNama = data.jabNama?.trim() ?: ""
                    userPreferences.updateJabatan(jabCode, jabNama)
                    userPreferences.saveBioData(Gson().toJson(data))
                    Log.d("MainActivity", "Saved jabatan: $jabCode - $jabNama & bio data")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to fetch bio: ${e.message}")
            }
        }
    }

    private fun loadProfilePhoto(nip: String, pt: String) {
        val photoUrl = ImageUrlHelper.constructPhotoUrl(pt, nip)

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
                    Log.e("MainActivity", "Profile photo load failed: ${e?.message}")
                    return false
                }
                override fun onResourceReady(
                    resource: android.graphics.drawable.Drawable,
                    model: Any,
                    target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                    dataSource: com.bumptech.glide.load.DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }
            })
            .circleCrop()
            .into(binding.ivProfile)
    }

    private fun setupClickListeners(session: com.dakotagroupstaff.data.local.model.UserSession) {
        binding.cardProfileFrame.setOnClickListener {
            showProfilePhotoDialog(session.nip)
        }
        binding.cardKepegawaian.setOnClickListener {
            startActivity(Intent(this, KepegawaianMenuActivity::class.java))
        }
        binding.cardOperasional.setOnClickListener {
            startActivity(Intent(this, com.dakotagroupstaff.ui.operasional.OperasionalMenuActivity::class.java))
        }
        binding.btnLihatSuratTugas.setOnClickListener {
            startActivity(Intent(this, com.dakotagroupstaff.ui.operasional.QuickAccessActivity::class.java))
        }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, com.dakotagroupstaff.ui.settings.SettingsActivity::class.java))
        }
    }

    private fun showProfilePhotoDialog(nip: String) {
        loginViewModel.getSession().observe(this) { session ->
            if (session.isLoggedIn) {
                val photoUrl = ImageUrlHelper.constructPhotoUrl(session.pt, nip)
                PhotoViewerDialog(this, photoUrl).show()
            }
        }
    }
}
