package com.dakotagroupstaff.util

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Helper class for geocoding operations
 * Converts coordinates to human-readable address
 */
object GeocoderHelper {
    
    private const val TAG = "GeocoderHelper"
    
    /**
     * Get address from coordinates
     * @param context Application context
     * @param latitude Latitude coordinate
     * @param longitude Longitude coordinate
     * @return Formatted address string or null if failed
     */
    suspend fun getAddressFromCoordinates(
        context: Context,
        latitude: Double,
        longitude: Double
    ): String? = withContext(Dispatchers.IO) {
        try {
            if (!Geocoder.isPresent()) {
                Log.e(TAG, "Geocoder not present on this device")
                return@withContext null
            }
            
            val geocoder = Geocoder(context, Locale.getDefault())
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ - Use async API
                var result: String? = null
                try {
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        result = formatAddress(addresses)
                    }
                    // Wait a bit for callback
                    kotlinx.coroutines.delay(2000)
                } catch (e: Exception) {
                    Log.e(TAG, "Geocoder async error", e)
                }
                result
            } else {
                // Legacy API
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                formatAddress(addresses)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting address from coordinates", e)
            null
        }
    }
    
    /**
     * Format address from Address list
     */
    private fun formatAddress(addresses: List<Address>?): String? {
        if (addresses.isNullOrEmpty()) {
            return null
        }
        
        val address = addresses[0]
        return buildString {
            // Street address
            address.thoroughfare?.let { append("$it, ") }
            
            // Sub locality / District
            address.subLocality?.let { append("$it, ") }
            
            // City
            address.locality?.let { append("$it, ") }
            
            // Province
            address.adminArea?.let { append("$it ") }
            
            // Postal code
            address.postalCode?.let { append("$it, ") }
            
            // Country
            address.countryName?.let { append(it) }
        }.trim().trim(',').ifEmpty { null }
    }
    
    /**
     * Get short address (city and province only)
     */
    suspend fun getShortAddress(
        context: Context,
        latitude: Double,
        longitude: Double
    ): String? = withContext(Dispatchers.IO) {
        try {
            if (!Geocoder.isPresent()) {
                return@withContext null
            }
            
            val geocoder = Geocoder(context, Locale.getDefault())
            
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            
            if (addresses.isNullOrEmpty()) {
                return@withContext null
            }
            
            val address = addresses[0]
            buildString {
                address.locality?.let { append("$it, ") }
                address.adminArea?.let { append(it) }
            }.trim().trim(',').ifEmpty { null }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting short address", e)
            null
        }
    }
}
