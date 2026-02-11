package com.dakotagroupstaff.data.remote.retrofit

import android.util.Log
import com.dakotagroupstaff.data.local.preferences.UserPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * TokenAuthenticator - Handles 401 responses by auto-refreshing access tokens
 * Implements OkHttp Authenticator to intercept 401 errors and refresh tokens seamlessly
 */
class TokenAuthenticator(
    private val userPreferences: UserPreferences,
    private val apiServiceForRefresh: ApiService  // Separate instance without authenticator
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        Log.d("TokenAuthenticator", "=== 401 DETECTED - AUTH REQUIRED ===")
        
        // Avoid infinite loop - if refresh also failed, give up
        if (response.request.url.encodedPath.contains("refresh-token")) {
            Log.e("TokenAuthenticator", "Refresh token endpoint failed - logging out")
            return null  // Will force logout
        }
        
        // Prevent concurrent refresh - synchronized block
        synchronized(this) {
            return runBlocking {
                try {
                    val refreshToken = userPreferences.getRefreshToken().first()
                    val nip = userPreferences.getNip().first()
                    val imei = userPreferences.getImei().first()
                    val pt = userPreferences.getPt().first()
                    
                    if (refreshToken.isEmpty() || nip.isEmpty()) {
                        Log.e("TokenAuthenticator", "No refresh token or NIP - cannot refresh")
                        return@runBlocking null
                    }
                    
                    Log.d("TokenAuthenticator", "Attempting to refresh token for NIP: $nip")
                    
                    // Call refresh token API
                    val refreshRequest = RefreshTokenRequest(refreshToken, nip, imei)
                    val refreshResponse = apiServiceForRefresh.refreshAccessToken(pt, refreshRequest)
                    
                    if (refreshResponse.success && refreshResponse.data != null) {
                        val newAccessToken = refreshResponse.data.accessToken
                        val expiresIn = refreshResponse.data.expiresIn
                        
                        // Save new tokens
                        userPreferences.saveAccessToken(newAccessToken)
                        userPreferences.saveTokenExpiry(expiresIn)
                        
                        Log.d("TokenAuthenticator", "✅ Token refreshed successfully")
                        
                        // Retry original request with new token
                        return@runBlocking response.request.newBuilder()
                            .header("Authorization", "Bearer $newAccessToken")
                            .build()
                    } else {
                        Log.e("TokenAuthenticator", "Refresh failed: ${refreshResponse.message}")
                        return@runBlocking null
                    }
                } catch (e: Exception) {
                    Log.e("TokenAuthenticator", "Error refreshing token", e)
                    return@runBlocking null
                }
            }
        }
    }
}
