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
 * 2. Fetch refreshToken + NIP + IMEI from DataStore
 * 3. Call /auth/refresh-token endpoint
 * 4. If success → save new accessToken, retry original request transparently
 * 5. If fail → emit sessionExpiredEvent so any Activity can redirect to Login
 */
class TokenAuthenticator(
    private val userPreferences: UserPreferences,
    private val apiServiceForRefresh: ApiService,  // Separate instance without authenticator to avoid loops
    private val sessionManager: SessionManager? = null
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        Log.d("TokenAuthenticator", "=== 401 DETECTED - AUTH REQUIRED ===")

        // Avoid infinite loop - if refresh endpoint also returned 401, give up
        if (response.request.url.encodedPath.contains("refresh-token")) {
            Log.e("TokenAuthenticator", "Refresh token endpoint returned 401 — forcing logout")
            handleRefreshFailure("Sesi anda telah berakhir, silahkan login kembali")
            return null
        }

        // Prevent concurrent refresh with synchronized block
        synchronized(this) {
            return runBlocking {
                try {
                    val refreshToken = userPreferences.getRefreshToken().first()
                    val nip = userPreferences.getNip().first()
                    val imei = userPreferences.getImei().first()
                    val pt = userPreferences.getPt().first()

                    if (refreshToken.isEmpty() || nip.isEmpty()) {
                        Log.e("TokenAuthenticator", "No refresh token or NIP available — forcing logout")
                        handleRefreshFailure("Sesi tidak ditemukan, silahkan login kembali")
                        return@runBlocking null
                    }

                    Log.d("TokenAuthenticator", "Attempting to refresh token for NIP: $nip")

                    // Call refresh token API using a separate ApiService instance (no authenticator)
                    val refreshRequest = RefreshTokenRequest(refreshToken, nip, imei)
                    val refreshResponse = apiServiceForRefresh.refreshAccessToken(pt, refreshRequest)

                    if (refreshResponse.success && refreshResponse.data != null) {
                        val newAccessToken = refreshResponse.data.accessToken
                        val expiresIn = refreshResponse.data.expiresIn

                        // Persist the new access token and its expiry time
                        userPreferences.saveAccessToken(newAccessToken)
                        userPreferences.saveTokenExpiry(expiresIn)

                        Log.d("TokenAuthenticator", "✅ Token refreshed successfully, retrying original request")

                        // Retry the original failed request with the new token
                        return@runBlocking response.request.newBuilder()
                            .header("Authorization", "Bearer $newAccessToken")
                            .build()
                    } else {
                        Log.e("TokenAuthenticator", "Refresh token rejected by server: ${refreshResponse.message}")
                        handleRefreshFailure("Sesi anda telah berakhir, silahkan login kembali")
                        return@runBlocking null
                    }
                } catch (e: Exception) {
                    Log.e("TokenAuthenticator", "Exception while refreshing token", e)
                    // Do NOT force logout on network errors — user may just be offline
                    // Only force logout if we got a definitive rejection (handled above)
                    return@runBlocking null
                }
            }
        }
    }

    /**
     * Called when refresh token is definitively invalid/expired.
     * Clears locally stored tokens and broadcasts the session expired event
     * so any active Activity can redirect to LoginActivity.
     */
    private fun handleRefreshFailure(reason: String) {
        runBlocking {
            try {
                // Clear the invalid tokens from local storage
                userPreferences.clearAccessToken()
                Log.d("TokenAuthenticator", "Cleared access token from DataStore")
            } catch (e: Exception) {
                Log.e("TokenAuthenticator", "Failed to clear access token", e)
            }
        }
        // Broadcast event — MainActivity and any other observer will redirect to login
        sessionManager?.emitSessionExpired(reason)
        Log.d("TokenAuthenticator", "Session expired event emitted: $reason")
    }
}
