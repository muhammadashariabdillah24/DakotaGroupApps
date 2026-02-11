package com.dakotagroupstaff.data.remote.response

import com.google.gson.annotations.SerializedName

/**
 * Login Response Data
 * Based on API endpoint: POST /auth/login?pt=<pt>
 */
data class LoginData(
    @SerializedName("NIP")
    val nip: String,
    
    @SerializedName("Nama")
    val nama: String,
    
    @SerializedName("Atasan1")
    val atasan1: String,
    
    @SerializedName("Atasan2")
    val atasan2: String,
    
    @SerializedName("SttReady")
    val sttReady: String,
    
    @SerializedName("TaskCode")
    val taskCode: String? = null,
    
    @SerializedName("TaskDetail")
    val taskDetail: String? = null,
    
    @SerializedName("TaskID")
    val taskId: String? = null,
    
    @SerializedName("Area Kerja")
    val areaKerja: String? = null,
    
    @SerializedName("accessToken")
    val accessToken: String = "",
    
    @SerializedName("refreshToken")
    val refreshToken: String = "",
    
    @SerializedName("tokenType")
    val tokenType: String = "Bearer",
    
    @SerializedName("expiresIn")
    val expiresIn: Int = 3600  // 1 hour in seconds
)
