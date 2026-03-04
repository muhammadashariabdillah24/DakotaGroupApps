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
    fun getApiService(userPreferences: com.dakotagroupstaff.data.local.preferences.UserPreferences? = null): ApiService {
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
        
        // Authorization header interceptor - inject JWT token
        val authInterceptor = Interceptor { chain ->
            val token = if (userPreferences != null) {
                runBlocking {
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
            clientBuilder.authenticator(TokenAuthenticator(userPreferences, refreshApiService))
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
