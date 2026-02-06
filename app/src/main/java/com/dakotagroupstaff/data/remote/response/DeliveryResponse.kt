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

// ==================== NEW GRANULAR DELIVERY ENDPOINTS ====================

/**
 * Check Delivery Status Request
 * API: POST /api/v1/delivery/check-status?pt=<pt>
 */
data class CheckDeliveryStatusRequest(
    @SerializedName("noLoper")
    val noLoper: String,
    
    @SerializedName("btt")
    val btt: String
)

/**
 * Check Delivery Status Response
 */
data class CheckDeliveryStatusData(
    @SerializedName("status")
    val status: String,  // NODATA, FOTO, TTD, DATAEXISTS
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("detail")
    val detail: String?,
    
    @SerializedName("noLoper")
    val noLoper: String,
    
    @SerializedName("btt")
    val btt: String,
    
    @SerializedName("received")
    val received: Boolean?,
    
    @SerializedName("hasPhoto")
    val hasPhoto: Boolean?,
    
    @SerializedName("hasSignature")
    val hasSignature: Boolean?,
    
    @SerializedName("fotoCode")
    val fotoCode: String?,
    
    @SerializedName("ttdCode")
    val ttdCode: String?
)

/**
 * Upload Photo Request
 * API: POST /api/v1/delivery/upload-photo?pt=<pt>
 */
data class UploadPhotoRequest(
    @SerializedName("base64Image")
    val base64Image: String
)

/**
 * Upload Photo Response
 */
data class UploadPhotoData(
    @SerializedName("result")
    val result: String,  // "imgdbs"
    
    @SerializedName("imageCode")
    val imageCode: String,
    
    @SerializedName("filePath")
    val filePath: String?,
    
    @SerializedName("fileName")
    val fileName: String?
)

/**
 * Update Delivery Photo Request
 * API: POST /api/v1/delivery/update-photo?pt=<pt>
 */
data class UpdateDeliveryPhotoRequest(
    @SerializedName("noLoper")
    val noLoper: String,
    
    @SerializedName("noBTT")
    val noBtt: String,
    
    @SerializedName("imgCode")
    val imgCode: String
)

/**
 * Update Delivery Photo Response
 */
data class UpdateDeliveryPhotoData(
    @SerializedName("success")
    val success: String,  // "done"
    
    @SerializedName("noLoper")
    val noLoper: String,
    
    @SerializedName("noBTT")
    val noBtt: String,
    
    @SerializedName("imgCode")
    val imgCode: String,
    
    @SerializedName("rowsAffected")
    val rowsAffected: Int
)

/**
 * Update Delivery Signature Request
 * API: POST /api/v1/delivery/update-signature?pt=<pt>
 */
data class UpdateDeliverySignatureRequest(
    @SerializedName("noLoper")
    val noLoper: String,
    
    @SerializedName("noBTT")
    val noBtt: String,
    
    @SerializedName("imgCode")
    val imgCode: String
)

/**
 * Update Delivery Signature Response
 */
data class UpdateDeliverySignatureData(
    @SerializedName("success")
    val success: String,  // "done"
    
    @SerializedName("noLoper")
    val noLoper: String,
    
    @SerializedName("noBTT")
    val noBtt: String,
    
    @SerializedName("imgCode")
    val imgCode: String,
    
    @SerializedName("type")
    val type: String,  // "signature"
    
    @SerializedName("rowsAffected")
    val rowsAffected: Int
)

