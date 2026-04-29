package com.dakotagroupstaff.ui.base

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dakotagroupstaff.ui.login.LoginActivity
import com.dakotagroupstaff.utils.NetworkMonitor
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * BaseActivity — Base class for ALL activities in the app.
 *
 * Provides two automatic features:
 * 1. **No-Internet Dialog**: Monitors network connectivity via [NetworkMonitor].
 *    When internet is lost, shows a non-cancellable dialog forcing the user to
 *    re-enable Wi-Fi or Mobile Data. Auto-dismisses when internet is restored.
 *
 * 2. **Safe to extend**: All subclasses just extend BaseActivity instead of
 *    AppCompatActivity. No extra setup needed.
 *
 * Usage:
 * ```kotlin
 * class MyActivity : BaseActivity() { ... }
 * ```
 */
abstract class BaseActivity : AppCompatActivity() {

    private val networkMonitor: NetworkMonitor by inject()

    // Holds the currently displayed "no internet" dialog — null when not showing
    private var noInternetDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observeNetworkConnectivity()
    }

    override fun onDestroy() {
        // Dismiss dialogs to prevent WindowLeakedException
        noInternetDialog?.dismiss()
        noInternetDialog = null
        super.onDestroy()
    }

    // ─── Network Connectivity ───────────────────────────────────────────────

    /**
     * Observes [NetworkMonitor.isConnected] only while the Activity is at least STARTED.
     * - Internet lost → show blocking dialog
     * - Internet restored → dismiss dialog automatically
     */
    private fun observeNetworkConnectivity() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                networkMonitor.isConnected.collect { isConnected ->
                    Log.d("BaseActivity", "${javaClass.simpleName} — isConnected=$isConnected")
                    if (isConnected) {
                        dismissNoInternetDialog()
                    } else {
                        showNoInternetDialog()
                    }
                }
            }
        }
    }

    /**
     * Shows the non-cancellable "no internet" dialog.
     * Has two action buttons:
     *  - "Wi-Fi" → opens Wi-Fi settings
     *  - "Data Selular" → opens mobile data / SIM settings
     *
     * The dialog auto-dismisses when internet is restored.
     */
    private fun showNoInternetDialog() {
        if (isFinishing || isDestroyed) return
        if (noInternetDialog?.isShowing == true) return  // Already showing

        noInternetDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Tidak Ada Koneksi Internet")
            .setMessage(
                "Aplikasi Dakota Group Staff memerlukan koneksi internet untuk beroperasi.\n\n" +
                "Silahkan aktifkan:\n" +
                "  • Wi-Fi  — untuk jaringan lokal/kantor\n" +
                "  • Data Selular  — jika Wi-Fi tidak tersedia\n\n" +
                "Dialog ini akan hilang otomatis setelah koneksi tersedia."
            )
            .setCancelable(false)  // User MUST enable internet — cannot dismiss
            .setNeutralButton("Pengaturan Wi-Fi") { _, _ ->
                openWifiSettings()
            }
            .setPositiveButton("Data Selular") { _, _ ->
                openMobileDataSettings()
            }
            .create()
            .also { it.show() }

        Log.d("BaseActivity", "No-internet dialog shown in ${javaClass.simpleName}")
    }

    /**
     * Dismisses the "no internet" dialog if it is currently showing.
     */
    private fun dismissNoInternetDialog() {
        if (noInternetDialog?.isShowing == true) {
            noInternetDialog?.dismiss()
            Log.d("BaseActivity", "No-internet dialog dismissed — internet restored")
        }
        noInternetDialog = null
    }

    /**
     * Navigates to [LoginActivity] and clears the entire back stack.
     * Can be called from any Activity.
     */
    fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // ─── Shared Error / Success Dialogs ──────────────────────────────────────
    //
    // Semua subclass Activity dapat memanggil fungsi-fungsi ini tanpa perlu
    // mendefinisikan ulang. Ini menghilangkan duplikasi kode di setiap fitur.

    /**
     * Menampilkan dialog error standar dengan tombol "OK".
     * Gunakan untuk error dari API (Result.Error) yang perlu dibaca user.
     *
     * @param message Pesan error yang akan ditampilkan
     * @param title Judul dialog (default: "Error")
     * @param onDismiss Callback opsional saat dialog ditutup
     */
    fun showErrorDialog(
        message: String,
        title: String = "Terjadi Kesalahan",
        onDismiss: (() -> Unit)? = null
    ) {
        if (isFinishing || isDestroyed) return
        runOnUiThread {
            MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                    onDismiss?.invoke()
                }
                .show()
        }
    }

    /**
     * Menampilkan dialog sukses standar dengan tombol "OK".
     * Gunakan untuk konfirmasi aksi yang berhasil dan butuh perhatian user.
     *
     * @param message Pesan sukses yang akan ditampilkan
     * @param title Judul dialog (default: "Berhasil")
     * @param onDismiss Callback opsional saat dialog ditutup
     */
    fun showSuccessDialog(
        message: String,
        title: String = "Berhasil",
        onDismiss: (() -> Unit)? = null
    ) {
        if (isFinishing || isDestroyed) return
        runOnUiThread {
            MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                    onDismiss?.invoke()
                }
                .show()
        }
    }

    /**
     * Menampilkan dialog informasi umum.
     *
     * @param message Pesan yang akan ditampilkan
     * @param title Judul dialog
     * @param onDismiss Callback opsional saat dialog ditutup
     */
    fun showInfoDialog(
        message: String,
        title: String = "Informasi",
        onDismiss: (() -> Unit)? = null
    ) {
        if (isFinishing || isDestroyed) return
        runOnUiThread {
            MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                    onDismiss?.invoke()
                }
                .show()
        }
    }

    /**
     * Menampilkan Toast singkat.
     * Gunakan untuk feedback ringan yang tidak kritis (sukses, info singkat).
     *
     * @param message Pesan Toast
     */
    fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    /**
     * Menampilkan Toast panjang.
     * Gunakan untuk pesan yang butuh waktu lebih lama untuk dibaca.
     *
     * @param message Pesan Toast
     */
    fun showLongToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show()
    }

    // ─── Settings Navigation ─────────────────────────────────────────────────

    /**
     * Opens the system Wi-Fi settings panel.
     */
    private fun openWifiSettings() {
        try {
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        } catch (e: Exception) {
            Log.e("BaseActivity", "Cannot open Wi-Fi settings", e)
            // Fallback to general wireless settings
            startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
        }
    }

    /**
     * Opens the system Mobile Data / SIM Card settings.
     * Falls back to general network settings on older devices.
     */
    private fun openMobileDataSettings() {
        try {
            // Android 10+ — direct mobile data settings
            startActivity(Intent(Settings.ACTION_DATA_ROAMING_SETTINGS))
        } catch (e: Exception) {
            try {
                // Fallback: general network & internet settings
                startActivity(Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS))
            } catch (e2: Exception) {
                Log.e("BaseActivity", "Cannot open mobile data settings", e2)
                // Last resort: general wireless settings
                startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
            }
        }
    }
}
