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
import kotlin.random.Random

/**
 * DeviceCheckManager — Polling tiap 3 menit untuk mendeteksi apakah NIP ini
 * masih terdaftar di perangkat ini (HRD_M_Karyawan: Kry_Imei1 + Kry_SimcardID1).
 *
 * DESAIN: Scope yang digunakan adalah scope EKSTERNAL (lifecycleScope dari Activity),
 * bukan scope internal. Ini memastikan:
 * - Polling berjalan selama Activity hidup
 * - Polling otomatis berhenti saat Activity destroy
 * - Tidak ada memory leak
 *
 * Jika server mengembalikan { valid: false }:
 *   → Tampilkan dialog peringatan dengan countdown 7 detik
 *   → Setelah countdown (atau klik Keluar) → clear semua data lokal → ke LoginActivity
 */
class DeviceCheckManager(
    private val context: Context,
    private val apiService: ApiService,
    private val userPreferences: UserPreferences,
    private val onForceLogout: () -> Unit
) {

    companion object {
        private const val TAG = "DeviceCheckManager"
        private const val POLL_INTERVAL_MS = 1 * 60 * 1000L  // 1 menit
        private const val JITTER_MS = 10 * 1000L              // ±10 detik jitter
        private const val COUNTDOWN_SECONDS = 7L
    }

    private var pollingJob: Job? = null
    private var activeDialog: AlertDialog? = null
    private var countDownTimer: CountDownTimer? = null

    /**
     * Mulai polling. Hanya berjalan jika user sedang login.
     * Panggil dari BaseActivity dengan lifecycleScope agar terikat lifecycle Activity.
     * @param scope CoroutineScope dari Activity (lifecycleScope)
     */
    fun start(scope: CoroutineScope) {
        if (pollingJob?.isActive == true) return  // Sudah berjalan

        pollingJob = scope.launch(Dispatchers.IO) {
            // Cek pertama dilakukan segera tanpa delay (saat app dibuka)
            checkDevice()

            // Kemudian polling dengan interval 3 menit + jitter
            while (isActive) {
                val jitter = Random.nextLong(-JITTER_MS, JITTER_MS)
                delay(POLL_INTERVAL_MS + jitter)
                checkDevice()
            }
        }
        Log.d(TAG, "Device check polling dimulai (interval: ~3 menit)")
    }

    /**
     * Hentikan polling.
     * Dipanggil otomatis saat lifecycleScope di-cancel (Activity destroy).
     * Bisa juga dipanggil manual saat logout.
     */
    fun stop() {
        pollingJob?.cancel()
        pollingJob = null
        countDownTimer?.cancel()
        Log.d(TAG, "Device check polling dihentikan")
    }

    /**
     * Bersihkan semua resource — dismiss dialog jika masih tampil.
     * Panggil dari BaseActivity.onDestroy().
     */
    fun destroy() {
        stop()
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
                Log.w(TAG, "Skip — IMEI atau SimId kosong di session")
                return
            }

            Log.d(TAG, "Memeriksa perangkat: NIP=$nip, PT=$pt")

            val response = apiService.checkDevice(
                pt = pt,
                request = CheckDeviceRequest(nip = nip, imei = imei, simId = simId)
            )

            if (!response.valid) {
                Log.w(TAG, "⚠️ Perangkat TIDAK valid! NIP=$nip sudah login di perangkat lain.")
                // Hentikan polling — tidak perlu cek lagi, dialog akan tampil
                pollingJob?.cancel()
                pollingJob = null

                withContext(Dispatchers.Main) {
                    showForceLogoutDialog()
                }
            } else {
                Log.d(TAG, "✓ Perangkat valid: NIP=$nip")
            }

        } catch (e: CancellationException) {
            // Job dibatalkan — normal, tidak perlu log error
            throw e
        } catch (e: Exception) {
            // Error jaringan atau server → skip, jangan kick user saat offline
            Log.w(TAG, "Gagal cek perangkat (skip, mungkin offline): ${e.message}")
        }
    }

    private fun showForceLogoutDialog() {
        val activity = context as? Activity ?: return
        if (activity.isFinishing || activity.isDestroyed) return

        // Dismiss dialog lama jika ada
        dismissDialog()

        val dialogView = LayoutInflater.from(context)
            .inflate(R.layout.dialog_force_logout, null)

        val tvCountdown = dialogView.findViewById<TextView>(R.id.tv_countdown)
        val btnKeluar = dialogView.findViewById<MaterialButton>(R.id.btn_keluar)

        activeDialog = MaterialAlertDialogBuilder(context)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnKeluar.setOnClickListener {
            countDownTimer?.cancel()
            activeDialog?.dismiss()
            activeDialog = null
            // Jalankan force logout di coroutine baru
            CoroutineScope(Dispatchers.IO).launch {
                performForceLogout()
            }
        }

        activeDialog?.show()

        // Countdown 7 detik — update UI di Main thread
        countDownTimer = object : CountDownTimer(COUNTDOWN_SECONDS * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000) + 1
                tvCountdown?.text = "($secondsLeft)"
            }

            override fun onFinish() {
                activeDialog?.dismiss()
                activeDialog = null
                CoroutineScope(Dispatchers.IO).launch {
                    performForceLogout()
                }
            }
        }.start()
    }

    private suspend fun performForceLogout() {
        try {
            Log.d(TAG, "Force logout: menghapus semua data lokal")
            userPreferences.clearAllData()
        } catch (e: Exception) {
            Log.e(TAG, "Error saat clearAllData: ${e.message}")
        } finally {
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
