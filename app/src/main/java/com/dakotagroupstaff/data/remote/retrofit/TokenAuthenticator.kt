package com.dakotagroupstaff.data.remote.retrofit

import android.util.Log
import com.dakotagroupstaff.data.local.preferences.UserPreferences
import com.dakotagroupstaff.utils.SessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * TokenAuthenticator - Handles 401 responses by auto-refreshing access tokens.
 * Implements OkHttp Authenticator to intercept 401 errors and refresh tokens seamlessly.
 *
 * Flow:
 * 1. API returns 401 → OkHttp calls authenticate()
 * 2. Guard: jika request sebelumnya sudah memakai token baru, jangan refresh lagi
 * 3. Fetch refreshToken + NIP + IMEI dari DataStore
 * 4. Panggil /auth/refresh-token endpoint
 * 5. Jika berhasil → simpan accessToken baru, retry request asli secara transparan
 * 6. Jika gagal → hapus SEMUA token & emit sessionExpiredEvent agar Activity redirect ke Login
 */
class TokenAuthenticator(
    private val userPreferences: UserPreferences,
    private val apiServiceForRefresh: ApiService,  // Instance terpisah tanpa authenticator untuk hindari loop
    private val sessionManager: SessionManager? = null
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        Log.d("TokenAuthenticator", "=== 401 DETECTED - AUTH REQUIRED ===")
        Log.d("TokenAuthenticator", "URL: ${response.request.url}")
        Log.d("TokenAuthenticator", "Response count: ${response.responseCount()}")

        // Hindari infinite loop — jika refresh endpoint juga mengembalikan 401, hentikan
        if (response.request.url.encodedPath.contains("refresh-token")) {
            Log.e("TokenAuthenticator", "Refresh token endpoint returned 401 — forcing logout")
            handleRefreshFailure("Sesi anda telah berakhir, silahkan login kembali")
            return null
        }

        // Guard: OkHttp memanggil authenticate() setiap 401.
        // Jika response count > 1, artinya request sudah pernah di-retry dengan token baru
        // tapi masih 401 — hentikan untuk mencegah loop.
        if (response.responseCount() >= 2) {
            Log.e("TokenAuthenticator", "Request sudah di-retry 2x tapi masih 401 — menghentikan refresh")
            handleRefreshFailure("Sesi anda telah berakhir, silahkan login kembali")
            return null
        }

        // Prevent concurrent refresh dengan synchronized block
        synchronized(this) {
            return runBlocking {
                try {
                    val refreshToken = userPreferences.getRefreshToken().first()
                    val nip = userPreferences.getNip().first()
                    val imei = userPreferences.getImei().first()
                    val pt = userPreferences.getPt().first()

                    Log.d("TokenAuthenticator", "NIP: $nip | Has refreshToken: ${refreshToken.isNotEmpty()} | PT: $pt")

                    if (refreshToken.isEmpty() || nip.isEmpty()) {
                        Log.e("TokenAuthenticator", "refreshToken atau NIP kosong — tidak bisa refresh, paksa logout")
                        handleRefreshFailure("Sesi tidak ditemukan, silahkan login kembali")
                        return@runBlocking null
                    }

                    Log.d("TokenAuthenticator", "Mencoba refresh token untuk NIP: $nip")

                    // Panggil refresh token API menggunakan instance ApiService terpisah (tanpa authenticator)
                    val refreshRequest = RefreshTokenRequest(refreshToken, nip, imei)
                    val refreshResponse = apiServiceForRefresh.refreshAccessToken(pt, refreshRequest)

                    Log.d("TokenAuthenticator", "Refresh response — success: ${refreshResponse.success}, hasData: ${refreshResponse.data != null}, message: ${refreshResponse.message}")

                    if (refreshResponse.success && refreshResponse.data != null) {
                        val newAccessToken = refreshResponse.data.accessToken
                        val expiresIn = refreshResponse.data.expiresIn

                        // Simpan access token baru dan waktu kadaluarsanya
                        userPreferences.saveAccessToken(newAccessToken)
                        userPreferences.saveTokenExpiry(expiresIn)

                        Log.d("TokenAuthenticator", "✅ Token berhasil di-refresh (expiresIn: ${expiresIn}s), mengulangi request asal")

                        // Retry request asli dengan token baru
                        return@runBlocking response.request.newBuilder()
                            .header("Authorization", "Bearer $newAccessToken")
                            .build()
                    } else {
                        Log.e("TokenAuthenticator", "❌ Server menolak refresh token: ${refreshResponse.message}")
                        handleRefreshFailure("Sesi anda telah berakhir, silahkan login kembali")
                        return@runBlocking null
                    }
                } catch (e: Exception) {
                    Log.e("TokenAuthenticator", "❌ Exception saat refresh token: ${e.javaClass.simpleName} — ${e.message}")
                    // JANGAN paksa logout pada error jaringan — user mungkin offline
                    // Hanya paksa logout jika server secara eksplisit menolak (sudah ditangani di atas)
                    return@runBlocking null
                }
            }
        }
    }

    /**
     * Dipanggil ketika refresh token definitif invalid/expired.
     * Menghapus SEMUA token yang tersimpan secara lokal (accessToken DAN refreshToken)
     * dan mem-broadcast event sessionExpired agar Activity aktif bisa redirect ke LoginActivity.
     *
     * PENTING: Harus hapus refreshToken juga, bukan hanya accessToken.
     * Jika hanya accessToken yang dihapus, maka refreshToken yang sudah tidak valid
     * akan terus dicoba di request selanjutnya, menyebabkan loop "jwt expired".
     */
    private fun handleRefreshFailure(reason: String) {
        runBlocking {
            try {
                userPreferences.clearAccessToken()       // Hapus access token
                userPreferences.saveRefreshToken("")     // Kosongkan refresh token (invalidasi)
                Log.d("TokenAuthenticator", "✅ Semua token telah dihapus dari DataStore")
            } catch (e: Exception) {
                Log.e("TokenAuthenticator", "Gagal menghapus token dari DataStore", e)
            }
        }
        // Broadcast event — MainActivity dan observer lain akan redirect ke login
        sessionManager?.emitSessionExpired(reason)
        Log.d("TokenAuthenticator", "Session expired event dikirim: $reason")
    }

    /**
     * Hitung berapa kali response ini sudah di-retry.
     * Digunakan untuk mencegah loop infinite retry.
     */
    private fun Response.responseCount(): Int {
        var count = 1
        var prior = priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
