package com.dakotagroupstaff.util

import android.util.Log

/**
 * Helper class for constructing image URLs based on company selection
 * Handles CORS issues by providing proper URL construction
 */
object ImageUrlHelper {
    
    private const val TAG = "ImageUrlHelper"
    
    /**
     * Get base URL for employee photos based on company code
     * @param pt Company code: "A"=DBS, "B"=DLB, "C"=DLI
     * @return Base URL for photo assets
     */
    fun getPhotoBaseUrl(pt: String): String {
        val baseUrl = when (pt) {
            "A" -> "https://dakotacargo.co.id/hrd/Foto" // DBS
            "B" -> "https://dakotacargo.co.id/hrd/Foto" // DLB
            "C" -> "https://dakotacargo.co.id/logistik/hrd/Foto" // DLI
            else -> "https://dakotacargo.co.id/hrd/Foto" // Default fallback
        }
        
        Log.d(TAG, "Company: $pt, Photo Base URL: $baseUrl")
        return baseUrl
    }
    
    /**
     * Construct full photo URL for an employee
     * @param pt Company code
     * @param nip Employee NIP
     * @return Complete photo URL
     */
    fun constructPhotoUrl(pt: String, nip: String): String {
        val baseUrl = getPhotoBaseUrl(pt)
        val photoUrl = "$baseUrl/${nip}.jpg"
        Log.d(TAG, "Constructed photo URL: $photoUrl")
        return photoUrl
    }
    
    /**
     * Construct app logo URL
     * @param pt Company code (optional - logo is same for all companies)
     * @return Complete logo URL
     */
    fun constructLogoUrl(pt: String? = null): String {
        // Logo is the same for all companies
        val baseUrl = "https://dakotacargo.co.id/hrd/Foto"
        val logoUrl = "$baseUrl/dakotagroup.png"
        Log.d(TAG, "Constructed logo URL: $logoUrl")
        return logoUrl
    }
    
    /**
     * Validate if the URL is from our trusted domain
     * @param url URL to validate
     * @return true if URL is from dakotacargo.co.id domain
     */
    fun isTrustedDomain(url: String): Boolean {
        return url.contains("dakotacargo.co.id")
    }
}