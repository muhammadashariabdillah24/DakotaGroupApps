package com.dakotagroupstaff.util

import android.content.Context
import android.location.Location
import android.os.Build
import com.scottyab.rootbeer.RootBeer

/**
 * Security checker utility for detecting:
 * 1. Fake GPS / Mock Location
 * 2. Rooted devices
 */
object SecurityChecker {
    
    /**
     * Check if device is rooted
     * @return true if device is rooted, false otherwise
     */
    fun isDeviceRooted(context: Context): Boolean {
        val rootBeer = RootBeer(context)
        return rootBeer.isRooted
    }
    
    /**
     * Check if location is from mock/fake GPS
     * @param location Location object to check
     * @return true if location is mocked, false otherwise
     */
    fun isMockLocation(location: Location?): Boolean {
        if (location == null) return false
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ (API 31+)
            location.isMock
        } else {
            // Android 11 and below (API 18-30)
            @Suppress("DEPRECATION")
            location.isFromMockProvider
        }
    }
    
    /**
     * Get detailed root detection information for debugging
     */
    fun getRootDetectionDetails(context: Context): String {
        val rootBeer = RootBeer(context)
        val details = StringBuilder()
        
        details.appendLine("Root Detection Details:")
        details.appendLine("- Is Rooted: ${rootBeer.isRooted}")
        details.appendLine("- Root Management Apps Detected: ${rootBeer.detectRootManagementApps()}")
        details.appendLine("- Potentially Dangerous Apps: ${rootBeer.detectPotentiallyDangerousApps()}")
        details.appendLine("- Test Keys: ${rootBeer.detectTestKeys()}")
        details.appendLine("- Busybox: ${rootBeer.checkForBusyBoxBinary()}")
        details.appendLine("- Su Binary: ${rootBeer.checkForSuBinary()}")
        details.appendLine("- Root Cloaking Apps: ${rootBeer.detectRootCloakingApps()}")
        
        return details.toString()
    }
}
