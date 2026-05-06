package com.dakotagroupstaff.ui.checker

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dakotagroupstaff.MainActivity
import com.dakotagroupstaff.R
import com.dakotagroupstaff.data.Result
import com.dakotagroupstaff.data.local.preferences.UserPreferences
import com.dakotagroupstaff.databinding.ActivityCheckerBinding
import com.dakotagroupstaff.ui.login.LoginActivity
import com.dakotagroupstaff.ui.login.LoginViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * CheckerActivity — Halaman booting pertama saat aplikasi dibuka.
 *
 * Alur:
 * 1. Cek DataStore: apakah user pernah login?
 *    - Belum pernah login → arahkan ke LoginActivity
 *    - Sudah pernah login → panggil API Login dengan kredensial yang tersimpan
 *       a. Login API berhasil (200) → data cocok → arahkan ke MainActivity
 *       b. Login API gagal (403)   → NIP sudah login di perangkat lain
 *                                  → tampilkan dialog force logout (7 detik)
 *                                  → hapus semua data lokal → arahkan ke LoginActivity
 */
class CheckerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCheckerBinding
    private val loginViewModel: LoginViewModel by viewModel()
    private val userPreferences: UserPreferences by inject()

    private var forceLogoutDialog: AlertDialog? = null
    private var forceLogoutTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            runCheckerFlow()
        }
    }

    override fun onDestroy() {
        forceLogoutTimer?.cancel()
        try { forceLogoutDialog?.dismiss() } catch (_: Exception) {}
        forceLogoutDialog = null
        super.onDestroy()
    }

    // ─── Checker Flow ─────────────────────────────────────────────────────────

    private suspend fun runCheckerFlow() {
        updateStatus("Memeriksa sesi...")

        val session = userPreferences.getSession().first()

        if (!session.isLoggedIn || session.nip.isBlank()) {
            // Belum pernah login → langsung ke LoginActivity
            Log.d("CheckerActivity", "Belum login → ke LoginActivity")
            navigateToLogin()
            return
        }

        // Sudah pernah login → panggil Login API dengan kredensial tersimpan
        Log.d("CheckerActivity", "Sudah login, verifikasi ke server: NIP=${session.nip}")
        updateStatus("Memverifikasi perangkat...")

        val deviceId     = session.imei
        val serialNumber = session.simId
        val email        = session.email
        val pt           = session.pt

        if (deviceId.isBlank() || serialNumber.isBlank()) {
            // Data perangkat tidak lengkap di DataStore → ke LoginActivity
            Log.w("CheckerActivity", "DeviceId/SerialNumber kosong di DataStore → ke LoginActivity")
            navigateToLogin()
            return
        }

        // Observe hasil login API
        loginViewModel.login(pt, session.nip, deviceId, serialNumber, email)
            .observe(this) { result ->
                when (result) {
                    is Result.Loading -> {
                        updateStatus("Memverifikasi perangkat...")
                    }
                    is Result.Success -> {
                        Log.d("CheckerActivity", "Verifikasi berhasil ✓ → ke MainActivity")
                        navigateToMain()
                    }
                    is Result.Error -> {
                        Log.w("CheckerActivity", "Verifikasi gagal: ${result.message}")
                        // Login API gagal → kemungkinan NIP sudah login di perangkat lain
                        // atau perangkat tidak terdaftar
                        showForceLogoutDialog()
                    }
                }
            }
    }

    private fun updateStatus(message: String) {
        runOnUiThread {
            binding.tvStatus.text = message
        }
    }

    // ─── Navigation ───────────────────────────────────────────────────────────

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    // ─── Force Logout Dialog ──────────────────────────────────────────────────

    /**
     * Tampilkan dialog peringatan dengan countdown 7 detik.
     * Setelah countdown habis atau tombol "Keluar" ditekan:
     *   → Hapus semua data lokal
     *   → Arahkan ke LoginActivity
     */
    private fun showForceLogoutDialog() {
        if (isFinishing || isDestroyed) return
        if (forceLogoutDialog?.isShowing == true) return

        val dialogView = layoutInflater.inflate(R.layout.dialog_force_logout, null)
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
                Log.d("CheckerActivity", "Force logout: hapus semua data lokal")
                loginViewModel.clearAllData()
            } catch (e: Exception) {
                Log.e("CheckerActivity", "Error saat force logout: ${e.message}")
            } finally {
                navigateToLogin()
            }
        }
    }
}
