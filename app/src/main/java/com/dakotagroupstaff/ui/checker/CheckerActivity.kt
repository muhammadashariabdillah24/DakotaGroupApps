package com.dakotagroupstaff.ui.checker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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
 * 1. Minta semua runtime permission yang diperlukan, satu per satu.
 *    - Teks status di bawah logo berubah dinamis sesuai permission yang sedang diminta.
 * 2. Setelah semua permission selesai (diterima/ditolak) → jalankan Checker Flow:
 *    a. Cek DataStore: apakah user pernah login?
 *       - Belum pernah login → arahkan ke LoginActivity
 *       - Sudah pernah login → panggil API Login dengan kredensial yang tersimpan
 *          i.  Login API berhasil (200) → data cocok → arahkan ke MainActivity
 *          ii. Login API gagal (403)   → NIP sudah login di perangkat lain
 *                                     → tampilkan dialog force logout (7 detik)
 *                                     → hapus semua data lokal → arahkan ke LoginActivity
 */
class CheckerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCheckerBinding
    private val loginViewModel: LoginViewModel by viewModel()
    private val userPreferences: UserPreferences by inject()

    private var forceLogoutDialog: AlertDialog? = null
    private var forceLogoutTimer: CountDownTimer? = null

    // ─── Definisi semua runtime permission beserta teks status dinamis ─────────

    /**
     * Data class untuk memetakan sebuah permission ke teks status yang tampil
     * saat dialog permission tersebut sedang ditampilkan ke pengguna.
     *
     * [minSdk] digunakan untuk melewati permission yang hanya tersedia di API level tertentu.
     */
    private data class PermissionItem(
        val permission: String,
        val statusText: String,
        val minSdk: Int = Build.VERSION_CODES.BASE
    )

    /**
     * Daftar semua dangerous permission yang diminta secara berurutan.
     * Urutan disesuaikan dari yang paling kritis untuk fungsi utama aplikasi.
     */
    private val requiredPermissions: List<PermissionItem> = buildList {
        add(PermissionItem(
            permission = Manifest.permission.READ_PHONE_STATE,
            statusText = "Proses Perizinan Info Telepon..."
        ))
        add(PermissionItem(
            permission = Manifest.permission.READ_PHONE_NUMBERS,
            statusText = "Proses Perizinan Nomor Telepon...",
            minSdk = Build.VERSION_CODES.O  // Android 8+
        ))
        add(PermissionItem(
            permission = Manifest.permission.ACCESS_FINE_LOCATION,
            statusText = "Proses Perizinan Lokasi..."
        ))
        add(PermissionItem(
            permission = Manifest.permission.CAMERA,
            statusText = "Proses Perizinan Kamera..."
        ))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(PermissionItem(
                permission = Manifest.permission.POST_NOTIFICATIONS,
                statusText = "Proses Perizinan Notifikasi...",
                minSdk = Build.VERSION_CODES.TIRAMISU  // Android 13+
            ))
        }
    }

    // Indeks permission yang sedang diproses dalam antrian
    private var currentPermissionIndex = 0

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Mulai proses perizinan berurutan dari permission pertama
        requestNextPermission()
    }

    override fun onDestroy() {
        forceLogoutTimer?.cancel()
        try { forceLogoutDialog?.dismiss() } catch (_: Exception) {}
        forceLogoutDialog = null
        super.onDestroy()
    }

    // ─── Permission Flow (Sequential / Berurutan) ─────────────────────────────

    /**
     * Meminta permission berikutnya dalam antrian [requiredPermissions].
     *
     * - Jika permission sudah diberikan atau tidak relevan di API level ini → lanjut ke berikutnya.
     * - Jika perlu diminta → update teks status & tampilkan dialog sistem.
     * - Jika semua permission sudah selesai → jalankan [runCheckerFlow].
     */
    private fun requestNextPermission() {
        // Cari permission berikutnya yang belum diberikan
        while (currentPermissionIndex < requiredPermissions.size) {
            val item = requiredPermissions[currentPermissionIndex]

            // Lewati jika API level tidak memenuhi syarat minimum
            if (Build.VERSION.SDK_INT < item.minSdk) {
                currentPermissionIndex++
                continue
            }

            // Lewati jika permission sudah diberikan
            if (checkSelfPermission(item.permission) == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permission sudah diberikan: ${item.permission}")
                currentPermissionIndex++
                continue
            }

            // Permission belum diberikan — tampilkan teks status & minta ke pengguna
            Log.d(TAG, "Meminta permission [${currentPermissionIndex}]: ${item.permission}")
            updateStatus(item.statusText)
            requestPermissions(arrayOf(item.permission), REQUEST_CODE_PERMISSION)
            return  // Tunggu callback onRequestPermissionsResult sebelum lanjut
        }

        // Semua permission sudah diproses → lanjut ke checker flow
        Log.d(TAG, "Semua permission selesai diproses → runCheckerFlow")
        updateStatus("Memeriksa sesi...")
        lifecycleScope.launch {
            runCheckerFlow()
        }
    }

    /**
     * Dipanggil sistem setelah pengguna memilih Allow/Deny pada dialog permission.
     * Tidak peduli hasilnya (granted atau denied) — lanjut ke permission berikutnya.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSION) {
            val permission = permissions.firstOrNull() ?: ""
            val granted = grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
            Log.d(TAG, "Permission result: $permission → ${if (granted) "DITERIMA" else "DITOLAK"}")

            // Maju ke permission berikutnya terlepas dari hasil
            currentPermissionIndex++
            requestNextPermission()
        }
    }

    // ─── Checker Flow ─────────────────────────────────────────────────────────

    private suspend fun runCheckerFlow() {
        updateStatus("Memeriksa sesi...")

        val session = userPreferences.getSession().first()

        if (!session.isLoggedIn || session.nip.isBlank()) {
            // Belum pernah login → langsung ke LoginActivity
            Log.d(TAG, "Belum login → ke LoginActivity")
            navigateToLogin()
            return
        }

        // Sudah pernah login → panggil Login API dengan kredensial tersimpan
        Log.d(TAG, "Sudah login, verifikasi ke server: NIP=${session.nip}")
        updateStatus("Memverifikasi perangkat...")

        val deviceId     = session.imei
        val serialNumber = session.simId
        val email        = session.email
        val pt           = session.pt

        if (deviceId.isBlank() || serialNumber.isBlank()) {
            // Data perangkat tidak lengkap di DataStore → ke LoginActivity
            Log.w(TAG, "DeviceId/SerialNumber kosong di DataStore → ke LoginActivity")
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
                        Log.d(TAG, "Verifikasi berhasil ✓ → ke MainActivity")
                        navigateToMain()
                    }
                    is Result.Error -> {
                        Log.w(TAG, "Verifikasi gagal: ${result.message}")
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
                Log.d(TAG, "Force logout: hapus semua data lokal")
                loginViewModel.clearAllData()
            } catch (e: Exception) {
                Log.e(TAG, "Error saat force logout: ${e.message}")
            } finally {
                navigateToLogin()
            }
        }
    }

    // ─── Constants ────────────────────────────────────────────────────────────

    companion object {
        private const val TAG = "CheckerActivity"
        private const val REQUEST_CODE_PERMISSION = 2001
    }
}
