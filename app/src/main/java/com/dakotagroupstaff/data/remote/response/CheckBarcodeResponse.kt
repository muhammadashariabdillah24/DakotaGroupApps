package com.dakotagroupstaff.data.remote.response

import com.google.gson.annotations.SerializedName

/**
 * Response data for barcode check
 */
data class CheckBarcodeData(
    @SerializedName("found")
    val found: Boolean,
    
    @SerializedName("value")
    val value: String,
    
    @SerializedName("koliId")
    val koliId: String? = null,
    
    @SerializedName("bttId")
    val bttId: String? = null,
    
    @SerializedName("totalKoli")
    val totalKoli: Int? = null,

    @SerializedName("message")
    val message: String? = null
)
