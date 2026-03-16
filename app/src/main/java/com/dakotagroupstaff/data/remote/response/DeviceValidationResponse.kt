package com.dakotagroupstaff.data.remote.response

import com.google.gson.annotations.SerializedName

/**
 * Response model untuk endpoint POST /auth/validate-device
 * Digunakan untuk mendeteksi jika user sudah login di perangkat lain
 */
data class DeviceValidationResponse(
    @SerializedName("success")
    val success: Boolean = false,
    @SerializedName("valid")
    val valid: Boolean = false,
    @SerializedName("reason")
    val reason: String? = null,
    @SerializedName("message")
    val message: String? = null
)
