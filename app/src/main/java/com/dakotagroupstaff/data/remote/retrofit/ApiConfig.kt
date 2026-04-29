package com.dakotagroupstaff.data.remote.retrofit

import android.util.Log
import com.dakotagroupstaff.BuildConfig
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import com.dakotagroupstaff.utils.SessionManager

/**
 * API Configuration with Security Features
 * 
 * Security Implementations:
 * 1. Certificate Pinning - Prevents MITM attacks
 * 2. Logging Control - Body logging only in debug builds
 * 3. Timeout Configuration - Prevents hanging connections
 * 
 * Performance Optimizations:
 * - Connection pooling (default in OkHttp)
 * - GZIP compression (automatic in Retrofit)
 * - Timeout settings to prevent memory leaks from hanging connections
 * 
 * Response Handling:
 * - Backend sends responses as JSON.stringify() (stringified JSON)
 * - Frontend uses custom converter to parse (equivalent to JSON.parse())
 * - This follows the project specification for mobile API communication
 */
object ApiConfig {
    
    /**
     * Get configured API Service instance
     * 
     * Features:
     * - Certificate pinning for production servers
     * - HTTP logging in debug mode only
     * - Proper timeout configuration
     * - Automatic JSON serialization/deserialization
     */
    fun getApiService(
        userPreferences: com.dakotagroupstaff.data.local.preferences.UserPreferences? = null,
        sessionManager: SessionManager? = null
    ): ApiService {
        // Create separate ApiService for token refresh (no authenticator to avoid loops)
        val refreshApiService = if (userPreferences != null) {
            createRefreshApiService()
        } else {
            null
        }
        
        // Configure logging - only show body in debug builds for security
        val loggingInterceptor = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
        } else {
            HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.NONE)
        }
        
        // Authorization header interceptor - inject JWT token + proactive refresh
        val authInterceptor = Interceptor { chain ->
            val token = if (userPreferences != null) {
                runBlocking {
                    // === PROACTIVE TOKEN REFRESH ===
                    // Cek apakah token akan expired dalam 5 menit ke depan.
                    // Jika ya, coba refresh SEBELUM mengirim request, sehingga server
                    // tidak pernah menerima token yang sudah expired.
                    val isExpiringSoon = userPreferences.isTokenExpiringSoon().first()
                    if (isExpiringSoon && refreshApiService != null) {
                        Log.d("ApiConfig", "Token akan segera expired, mencoba proactive refresh...")
                        try {
                            val refreshToken = userPreferences.getRefreshToken().first()
                            val nip = userPreferences.getNip().first()
                            val imei = userPreferences.getImei().first()
                            val pt = userPreferences.getPt().first()

                            if (refreshToken.isNotEmpty() && nip.isNotEmpty()) {
                                val refreshRequest = com.dakotagroupstaff.data.remote.retrofit.RefreshTokenRequest(
                                    refreshToken = refreshToken,
                                    nip = nip,
                                    deviceId = imei
                                )
                                val refreshResponse = refreshApiService.refreshAccessToken(pt, refreshRequest)
                                if (refreshResponse.success && refreshResponse.data != null) {
                                    userPreferences.saveAccessToken(refreshResponse.data.accessToken)
                                    userPreferences.saveTokenExpiry(refreshResponse.data.expiresIn)
                                    Log.d("ApiConfig", "✅ Proactive token refresh berhasil")
                                } else {
                                    Log.w("ApiConfig", "Proactive refresh ditolak server, melanjutkan dengan token lama")
                                }
                            }
                        } catch (e: Exception) {
                            // Jangan blokir request jika proactive refresh gagal (misal: offline)
                            // TokenAuthenticator akan menangani 401 secara reaktif jika terjadi
                            Log.w("ApiConfig", "Proactive refresh gagal (mungkin offline): ${e.message}")
                        }
                    }
                    // Ambil token terbaru (mungkin sudah di-refresh di atas)
                    userPreferences.getAccessToken().first()
                }
            } else {
                ""
            }

            val request = if (token.isNotEmpty()) {
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } else {
                chain.request()
            }

            chain.proceed(request)
        }
        
        // Custom interceptor to log raw response body for debugging
        val responseInterceptor = Interceptor { chain ->
            val request = chain.request()
            
            // Log request details
            if (BuildConfig.DEBUG) {
                Log.d("ApiConfig", "=== REQUEST ===")
                Log.d("ApiConfig", "URL: ${request.url}")
                Log.d("ApiConfig", "Method: ${request.method}")
                Log.d("ApiConfig", "Headers: ${request.headers}")
                Log.d("ApiConfig", "================")
            }
            
            val response = chain.proceed(request)
            
            // Log all requests in debug mode
            if (BuildConfig.DEBUG) {
                val responseBody = response.body
                val source = responseBody?.source()
                source?.request(Long.MAX_VALUE) // Buffer the entire body
                val buffer = source?.buffer
                
                val responseBodyString = buffer?.clone()?.readUtf8() ?: ""
                Log.d("ApiConfig", "=== RAW RESPONSE ===")
                Log.d("ApiConfig", "URL: ${request.url}")
                Log.d("ApiConfig", "Status Code: ${response.code}")
                Log.d("ApiConfig", "Content-Type: ${response.header("Content-Type")}")
                Log.d("ApiConfig", "Response Body: $responseBodyString")
                Log.d("ApiConfig", "====================")
            }
            
            response
        }
        
        // Get appropriate certificate pinner based on URL
        // - Production: Strict certificate pinning
        // - Development (localhost): No pinning
        val certificatePinner = CertificatePinnerHelper.getCertificatePinnerForUrl(BuildConfig.BASE_URL)
        
        // Build OkHttp client with security and performance configurations
        val clientBuilder = OkHttpClient.Builder()
            .addInterceptor(authInterceptor) // Add auth interceptor first to inject token
            .addInterceptor(responseInterceptor) // Add response interceptor first
            .addInterceptor(loggingInterceptor)
            // Add certificate pinning for security
            .certificatePinner(certificatePinner)
            // Configure timeouts to prevent memory leaks from hanging connections
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
        
        // Add TokenAuthenticator for auto-refresh on 401 (if userPreferences provided)
        if (userPreferences != null && refreshApiService != null) {
            clientBuilder.authenticator(TokenAuthenticator(userPreferences, refreshApiService, sessionManager))
        }
        
        val client = clientBuilder.build()
        
        // Configure Gson with lenient parsing to handle edge cases
        val gson = GsonBuilder()
            .setLenient()
            .create()
        
        // Build Retrofit instance with custom converter
        // Using StringResponseConverterFactory to handle JSON.stringify() from backend
        // This is equivalent to JSON.parse() in JavaScript
        // IMPORTANT: GsonConverterFactory must come BEFORE StringResponseConverterFactory
        // - GsonConverterFactory handles request body serialization (@Body parameters)
        // - StringResponseConverterFactory handles response body deserialization
        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson)) // For request bodies
            .addConverterFactory(StringResponseConverterFactory(gson)) // For response bodies

            .client(client)
            .build()
        
        return retrofit.create(ApiService::class.java)
    }
    
    /**
     * Create separate ApiService for token refresh (no authenticator)
     * This prevents infinite loops when refresh token endpoint returns 401
     */
    private fun createRefreshApiService(): ApiService {
        val loggingInterceptor = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
        } else {
            HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.NONE)
        }
        
        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        
        val gson = GsonBuilder()
            .setLenient()
            .create()
        
        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addConverterFactory(StringResponseConverterFactory(gson))
            .client(client)
            .build()
        
        return retrofit.create(ApiService::class.java)
    }
}
