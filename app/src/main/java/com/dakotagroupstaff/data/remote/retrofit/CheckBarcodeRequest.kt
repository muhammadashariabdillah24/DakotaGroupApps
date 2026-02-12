package com.dakotagroupstaff.data.remote.retrofit

import com.google.gson.annotations.SerializedName

data class CheckBarcodeRequest(
    @SerializedName("barcode")
    val barcode: String
)
