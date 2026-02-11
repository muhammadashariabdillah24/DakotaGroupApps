package com.dakotagroupstaff.data.remote.retrofit

import com.google.gson.annotations.SerializedName

data class RefreshTokenRequest(
    @SerializedName("refreshToken")
    val refreshToken: String,
    
    @SerializedName("nip")
    val nip: String,
    
    @SerializedName("deviceId")
    val deviceId: String
)
