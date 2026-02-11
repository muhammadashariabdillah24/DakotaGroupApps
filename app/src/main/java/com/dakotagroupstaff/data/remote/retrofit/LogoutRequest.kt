package com.dakotagroupstaff.data.remote.retrofit

import com.google.gson.annotations.SerializedName

data class LogoutRequest(
    @SerializedName("refreshToken")
    val refreshToken: String,
    
    @SerializedName("nip")
    val nip: String
)
