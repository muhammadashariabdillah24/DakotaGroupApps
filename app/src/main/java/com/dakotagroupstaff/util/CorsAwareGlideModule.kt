package com.dakotagroupstaff.util

import android.content.Context
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import okhttp3.*
import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * Custom Glide Module to handle CORS issues when loading images from external domains
 * Adds proper headers to bypass CORS restrictions
 */
@GlideModule
class CorsAwareGlideModule : AppGlideModule() {
    
    companion object {
        private const val TAG = "CorsAwareGlideModule"
    }
    
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(CorsInterceptor())
            .build()
        
        registry.replace(
            GlideUrl::class.java,
            InputStream::class.java,
            OkHttpUrlLoader.Factory(client)
        )
        
        Log.d(TAG, "Glide module registered with CORS interceptor")
    }
    
    /**
     * Interceptor to add CORS-bypassing headers to image requests
     */
    private class CorsInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val originalRequest = chain.request()
            val url = originalRequest.url.toString()
            
            // Only apply CORS headers to our trusted domain
            if (ImageUrlHelper.isTrustedDomain(url)) {
                val newRequest = originalRequest.newBuilder()
                    .addHeader("Accept", "image/*")
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android) DakotaGroupStaff/1.0")
                    .addHeader("Referer", "https://dakotacargo.co.id/")
                    .addHeader("Origin", "https://dakotacargo.co.id")
                    .build()
                
                Log.d(TAG, "Adding CORS headers for: ${originalRequest.url}")
                return chain.proceed(newRequest)
            }
            
            // For other domains, proceed normally
            return chain.proceed(originalRequest)
        }
    }
    
    // Disable manifest parsing to avoid issues with integration libraries
    override fun isManifestParsingEnabled(): Boolean = false
}