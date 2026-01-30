package com.dakotagroupstaff.data.remote.response

import com.google.gson.annotations.SerializedName

/**
 * Agent Location Response Data
 * Based on API endpoint: GET /api/v1/agent/locations?pt=<pt>
 */
data class AgentLocationData(
    @SerializedName("KodeAgen")
    val kodeAgen: Int,
    
    @SerializedName("NamaAgen")
    val namaAgen: String,
    
    @SerializedName("AgenLat")
    val agenLat: String,
    
    @SerializedName("AgenLon")
    val agenLon: String,
    
    @SerializedName("AgenMD5")
    val agenMd5: String? = null,
    
    @SerializedName("AgenRange")
    val agenRange: String
)