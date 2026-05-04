package com.dakotagroupstaff.ui.base

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import android.content.Context
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.widget.TextView
import com.dakotagroupstaff.R
import com.dakotagroupstaff.data.local.preferences.UserPreferences
import com.dakotagroupstaff.data.remote.retrofit.ApiService
import com.dakotagroupstaff.data.remote.retrofit.CheckDeviceRequest
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

/**
 * DeviceCheckManager — Polling tiap 3 menit untuk mendeteksi apakah NIP ini
 * masih terdaftar di perangkat ini (HRD_M_Karyawan: Kry_Imei1 + Kry_SimcardID1).
 *
 * Jika server mengembalikan { valid: false }:
 *   → Tampilkan dialog peringatan dengan countdown 7 detik
 *   → Setelah countdown (atau klik Keluar) → clear semua data lokal → ke LoginActivity
 *
 * Usage (di BaseActivity):
 *   deviceCheckManager.start()   // di onResume atau saat user login
 *   deviceCheckManager.stop()    // di onPause / onDestroy / saat user logout
 */
class DeviceCheckManager(
    private val context: Context,
    private val apiService: ApiService,
    private val userPreferences: UserPreferences,
    private val onForceLogout: () -> Unit
) {

    companion object {
        private const val TAG = "DeviceCheckManager"
        private const val POLL_INTERVAL_MS = 3 * 60 * 1000L  // 3 menit (base)
        private const val JITTER_MS = 30 * 1000L              // ±30 detik jitter
        private const val COUNTDOWN_SECONDS = 7L
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollingJob: Job? = null
    private var activeDialog: AlertDialog? = null
    private var countDownTimer: CountDownTimer? = null

    /**
     * Mulai polling. Hanya berjalan jika user sedang login.
     * Panggil dari BaseActivity.onResume() atau setelah login berhasil.
     */
    fun start() {
        if (pollingJob?.isActive == true) return  // Sudah berjalan

        pollingJob = scope.launch {
            while (isActive) {
                checkDevice()
                // Tambah jitter acak ±30 detik agar request tidak serentak dari semua device
                val jitter = ((-JITTER_MS)..(JITTER_MS)).random()
                delay(POLL_INTERVAL_MS + jitter)
            }
        }
        Log.d(TAG, "Device check polling dimulai (interval: 3 menit)")
    }

    /**
     * Hentikan polling.
     * Panggil dari BaseActivity.onStop() atau saat user logout.
     */
    fun stop() {
        pollingJob?.cancel()
        pollingJob = null
        countDownTimer?.cancel()
        dismissDialog()
        Log.d(TAG, "Device check polling dihentikan")
    }

    /**
     * Bersihkan semua resource.
     * Panggil dari BaseActivity.onDestroy().
     */
    fun destroy() {
        scope.cancel()
        countDownTimer?.cancel()
        dismissDialog()
    }

    // ─── Internal Logic ──────────────────────────────────────────────────────

    private suspend fun checkDevice() {
        try {
            val session = userPreferences.getSession().first()

            // Jangan cek jika user belum login
            if (!session.isLoggedIn || session.nip.isBlank()) {
                Log.d(TAG, "Skip — user belum login")
                return
            }

            val pt = session.pt
            val nip = session.nip
            val imei = session.imei
            val simId = session.simId

            if (imei.isBlank() || simId.isBlank()) {
                Log.w(TAG, "Skip — IMEI atau SimId kosong")
                return
            }

            Log.d(TAG, "Memeriksa perangkat: NIP=$nip, PT=$pt")

            val response = apiService.checkDevice(
                pt = pt,
                request = CheckDeviceRequest(nip = nip, imei = imei, simId = simId)
            )

            if (!response.valid) {
                Log.w(TAG, "Perangkat tidak valid! NIP=$nip mungkin sudah login di perangkat lain.")
                withContext(Dispatchers.Main) {
                    showForceLogoutDialog()
                }
            } else {
                Log.d(TAG, "Perangkat valid ✓")
            }

        } catch (e: CancellationException) {
            // Job dibatalkan — normal, tidak perlu log error
        } catch (e: Exception) {
            // Error jaringan atau server → skip, jangan kick user saat offline
            Log.w(TAG, "Gagal cek perangkat (skip): ${e.message}")
        }
    }

    private fun showForceLogoutDialog() {
        val activity = context as? Activity ?: return
        if (activity.isFinishing || activity.isDestroyed) return

        // Hentikan polling — dialog sudah tampil, tidak perlu cek lagi
        pollingJob?.cancel()
        pollingJob = null

        // Dismiss dialog lama jika ada
        dismissDialog()

        val dialogView = LayoutInflater.from(context)
            .inflate(R.layout.dialog_force_logout, null)

        val tvMessage = dialogView.findViewById<TextView>(R.id.tv_force_logout_message)
        val tvCountdown = dialogView.findViewById<TextView>(R.id.tv_countdown)
        val btnKeluar = dialogView.findViewById<MaterialButton>(R.id.btn_keluar)

        activeDialog = MaterialAlertDialogBuilder(context)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnKeluar.setOnClickListener {
            countDownTimer?.cancel()
            activeDialog?.dismiss()
            scope.launch { performForceLogout() }
        }

        activeDialog?.show()

        // Countdown 7 detik
        countDownTimer = object : CountDownTimer(COUNTDOWN_SECONDS * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000) + 1
                tvCountdown?.text = "($secondsLeft)"
            }

            override fun onFinish() {
                activeDialog?.dismiss()
                scope.launch { performForceLogout() }
            }
        }.start()
    }

    private suspend fun performForceLogout() {
        try {
            Log.d(TAG, "Force logout: menghapus semua data lokal")

            // Hapus semua data lokal (DataStore + Room DB via clearAllData)
            userPreferences.clearAllData()

            withContext(Dispatchers.Main) {
                onForceLogout()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saat force logout: ${e.message}")
            withContext(Dispatchers.Main) {
                onForceLogout()
            }
        }
    }

    private fun dismissDialog() {
        try {
            if (activeDialog?.isShowing == true) {
                activeDialog?.dismiss()
            }
        } catch (_: Exception) {}
        activeDialog = null
    }
}
