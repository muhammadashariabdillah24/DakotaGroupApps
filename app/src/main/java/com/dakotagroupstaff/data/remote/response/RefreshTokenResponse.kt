package com.dakotagroupstaff.data.remote.response

import com.google.gson.annotations.SerializedName

data class RefreshTokenData(
    @SerializedName("accessToken")
    val accessToken: String,
    
    @SerializedName("tokenType")
    val tokenType: String,
    
    @SerializedName("expiresIn")
    val expiresIn: Int
)
