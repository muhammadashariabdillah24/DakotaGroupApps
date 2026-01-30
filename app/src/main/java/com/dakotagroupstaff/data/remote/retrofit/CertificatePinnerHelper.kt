package com.dakotagroupstaff.data.remote.retrofit

import okhttp3.CertificatePinner

/**
 * Certificate Pinning Helper for HTTPS connections
 * 
 * This implements SSL/TLS certificate pinning to prevent man-in-the-middle attacks
 * by validating the server's SSL certificate against known public keys.
 * 
 * Security Benefits:
 * - Prevents MITM attacks even if a Certificate Authority is compromised
 * - Ensures connection is established only with the legitimate server
 * - Protects sensitive data transmission (login credentials, personal data, etc.)
 * 
 * How to get certificate pins:
 * 
 * Method 1: Using OpenSSL (Linux/Mac/Git Bash on Windows)
 * ```bash
 * openssl s_client -connect stagingdakota.my.id:443 | openssl x509 -pubkey -noout | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | base64
 * ```
 * 
 * Method 2: Using Chrome Browser
 * 1. Navigate to https://stagingdakota.my.id
 * 2. Click the padlock icon → Certificate → Details
 * 3. Look for "Public Key Info" and hash it with SHA-256
 * 
 * Method 3: Let the app fail first (Development only)
 * 1. Run the app without pins
 * 2. OkHttp will throw an exception with the correct pins in the error message
 * 3. Copy the pins from the error and add them here
 * 
 * IMPORTANT NOTES:
 * - Update pins before certificates expire (typically every 1-3 years)
 * - Include backup pins (intermediate CA or root CA) to prevent app breakage
 * - Test thoroughly before releasing to production
 * - Monitor certificate expiration dates
 * 
 * @see <a href="https://square.github.io/okhttp/4.x/okhttp/okhttp3/-certificate-pinner/">OkHttp CertificatePinner Documentation</a>
 */
object CertificatePinnerHelper {
    
    /**
     * Get Certificate Pinner for production server
     * 
     * Pins for: stagingdakota.my.id
     * 
     * NOTE: These are example pins. You MUST replace them with actual pins from your server.
     * 
     * To obtain the correct pins:
     * 1. Use OpenSSL command above
     * 2. Or let the app connect without pins and copy the pin from the error message
     * 3. Include at least 2 pins (primary certificate + backup CA)
     */
    fun getCertificatePinner(): CertificatePinner {
        return CertificatePinner.Builder()
            .add(
                "stagingdakota.my.id",
                // TODO: Replace with actual certificate pins from stagingdakota.my.id
                // Primary certificate pin
                "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
                // Backup pin (intermediate or root CA)
                "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="
            )
            .build()
    }
    
    /**
     * Get Certificate Pinner for localhost development
     * 
     * For localhost testing (10.0.2.2 for Android emulator), certificate pinning
     * is typically disabled because localhost uses self-signed certificates.
     * 
     * WARNING: Only use this in debug builds!
     */
    fun getCertificatePinnerForLocalhost(): CertificatePinner {
        // Return empty pinner for localhost - no pinning on development server
        // Self-signed certificates on localhost cannot be pinned reliably
        return CertificatePinner.Builder().build()
    }
    
    /**
     * Check if the URL is localhost/development server
     */
    private fun isLocalhost(url: String): Boolean {
        return url.contains("localhost") || 
               url.contains("127.0.0.1") || 
               url.contains("10.0.2.2") ||
               url.contains("192.168.")
    }
    
    /**
     * Get appropriate Certificate Pinner based on URL
     * - Production URLs: Use strict certificate pinning
     * - Localhost/Development: No pinning
     */
    fun getCertificatePinnerForUrl(baseUrl: String): CertificatePinner {
        return if (isLocalhost(baseUrl)) {
            getCertificatePinnerForLocalhost()
        } else {
            getCertificatePinner()
        }
    }
}
