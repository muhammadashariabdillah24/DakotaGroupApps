package com.dakotagroupstaff.data.remote.response

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

/**
 * Delivery List Response
 * API: GET /api/v1/delivery/list?pt=<pt>
 */
data class DeliveryListResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: List<DeliveryItem>?,
    
    @SerializedName("timestamp")
    val timestamp: String
)

/**
 * Delivery Item (BTT)
 */
@Parcelize
data class DeliveryItem(
    @SerializedName("NOLOPER")
    val noLoper: String,
    
    @SerializedName("NIPSUPIR")
    val nipSupir: String,
    
    @SerializedName("TANGGALLOPER")
    val tanggalLoper: String,
    
    @SerializedName("NOBTT")
    val noBtt: String,
    
    @SerializedName("PENERIMA")
    val penerima: String,
    
    @SerializedName("ALAMAT")
    val alamat: String,
    
    @SerializedName("Propinsi")
    val propinsi: String,
    
    @SerializedName("Kota")
    val kota: String,
    
    @SerializedName("Kecamatan")
    val kecamatan: String,
    
    @SerializedName("Kelurahan")
    val kelurahan: String,
    
    @SerializedName("Kodepos")
    val kodepos: String,
    
    @SerializedName("TELPPENERIMA")
    val telpPenerima: String,
    
    @SerializedName("NAMAPENGIRIM")
    val namaPengirim: String,
    
    @SerializedName("TELPPENGIRIM")
    val telpPengirim: String,
    
    @SerializedName("JUMLAHKOLI")
    val jumlahKoli: Int,
    
    @SerializedName("BERAT")
    val berat: Int,
    
    @SerializedName("BERATVOLUME")
    val beratVolume: Int,
    
    @SerializedName("SERVICE")
    val service: String
) : Parcelable

/**
 * Request body for delivery list
 */
data class DeliveryListRequest(
    @SerializedName("nip")
    val nip: String
)

/**
 * Submit Delivery Request
 * API: POST /api/v1/delivery/submit?pt=<pt>
 */
data class SubmitDeliveryRequest(
    @SerializedName("noLoper")
    val noLoper: String,
    
    @SerializedName("noBTT")
    val noBtt: String,
    
    @SerializedName("bTerimaYN")
    val bTerimaYn: String,  // "Y" or "N"
    
    @SerializedName("reasonID")
    val reasonId: String,
    
    @SerializedName("bPenerima")
    val bPenerima: String,  // Nama penerima
    
    @SerializedName("nip")
    val nip: String,
    
    @SerializedName("lat")
    val lat: String,
    
    @SerializedName("lon")
    val lon: String,
    
    @SerializedName("foto")
    val foto: String,  // Base64 photo
    
    @SerializedName("ttd")
    val ttd: String  // Base64 signature
)

/**
 * Submit Delivery Response
 */
data class SubmitDeliveryData(
    @SerializedName("message")
    val message: String,
    
    @SerializedName("noLoper")
    val noLoper: String?,
    
    @SerializedName("noBTT")
    val noBtt: String?
)
