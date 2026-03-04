package com.dakotagroupstaff.data.remote.response

import com.google.gson.annotations.SerializedName
import com.dakotagroupstaff.data.remote.response.ErrorResponse

/**
 * Response models for Letter of Assign API (PT DBS and PT DLB only)
 * Based on ASP SuratTugas endpoints
 */

/**
 * Response for GET /api/letter-of-assign/tugas
 * Contains list of letter of assignment details
 */
data class LetterOfAssignResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: List<LetterOfAssignDetail>? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("error")
    val error: ErrorResponse? = null
)

/**
 * Letter of Assignment Detail
 * Represents one checkpoint/route point
 */
data class LetterOfAssignDetail(
    @SerializedName("sID")
    val sID: String = "",

    @SerializedName("Keterangan")
    val keterangan: String = "",

    @SerializedName("StartAgen")
    val startAgen: String = "",

    @SerializedName("TglBerangkat")
    val tglBerangkat: String = "",

    @SerializedName("TglKembali")
    val tglKembali: String = "",

    @SerializedName("NoKendaraan")
    val noKendaraan: String = "",

    @SerializedName("Supir1NIP")
    val supir1NIP: String = "",

    @SerializedName("Supir1Nama")
    val supir1Nama: String = "",

    @SerializedName("Supir2NIP")
    val supir2NIP: String = "",

    @SerializedName("Supir2Nama")
    val supir2Nama: String = "",

    @SerializedName("TrUrut")
    val trUrut: String = "",

    @SerializedName("TrStatus")
    val trStatus: String = "", // 'B' = Berangkat, 'P' = Pulang

    @SerializedName("TrKdCabang")
    val trKdCabang: String = "",

    @SerializedName("TrCabang")
    val trCabang: String = "",

    @SerializedName("TrCabangLatH")
    val trCabangLatH: String = "",

    @SerializedName("TrCabangLongH")
    val trCabangLongH: String = "",

    @SerializedName("TrLastEvent")
    val trLastEvent: String = "",

    @SerializedName("TrAbsenIn")
    val trAbsenIn: String = "",

    @SerializedName("TrAbsenOut")
    val trAbsenOut: String = "",

    @SerializedName("TrKm")
    val trKm: String = "",

    @SerializedName("TrLat")
    val trLat: String = "",

    @SerializedName("TrLong")
    val trLong: String = "",

    @SerializedName("TrURLPic")
    val trURLPic: String = "",

    @SerializedName("lastcekin")
    val lastCekin: String = "0",

    @SerializedName("arah")
    val arah: String = "B" // 'B' = Berangkat, 'P' = Pulang
) {
    // Helper to check if checkpoint is already checked in
    fun isCheckedIn(): Boolean = trAbsenIn.isNotEmpty() && trAbsenIn != "null"
    
    // Helper to check if this is current checkpoint
    fun isCurrentCheckpoint(lastCekinValue: String): Boolean {
        val lastCekinInt = lastCekinValue.toIntOrNull() ?: 0
        val currentUrut = trUrut.toIntOrNull() ?: 0
        return currentUrut == lastCekinInt + 1 && !isCheckedIn()
    }
}

/**
 * Response for check location
 */
data class CheckLocationResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: CheckLocationData? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("error")
    val error: ErrorResponse? = null
)

data class CheckLocationData(
    @SerializedName("SUCCESS")
    val successStatus: String = "" // "DONE" or "NOGPS"
)

/**
 * Response for checkpoint submission
 */
data class CheckpointResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: CheckpointData? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("error")
    val error: ErrorResponse? = null
)

data class CheckpointData(
    @SerializedName("SUCCESS")
    val successStatus: String = "" // "DONE"
)

/**
 * Response for next point
 */
data class NextPointResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: List<NextPointData>? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("error")
    val error: ErrorResponse? = null
)

data class NextPointData(
    @SerializedName("NextPointNama")
    val nextPointNama: String = "",

    @SerializedName("jmlBTT")
    val jmlBTT: String = "",

    @SerializedName("jmlColly")
    val jmlColly: String = "",

    @SerializedName("totalBerat")
    val totalBerat: String = ""
)

/**
 * Response for complete assignment
 */
data class CompleteAssignmentResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: CompleteAssignmentData? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("error")
    val error: ErrorResponse? = null
)

data class CompleteAssignmentData(
    @SerializedName("SUCCESS")
    val successStatus: String = "" // "DONE"
)

/**
 * Response for loading (muat) data
 */
data class LoadingDataResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: List<LoadingData>? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("error")
    val error: ErrorResponse? = null
)

data class LoadingData(
    @SerializedName("sID")
    val sID: String = "",

    @SerializedName("agenid")
    val agenid: String = "",

    @SerializedName("nmAgen")
    val nmAgen: String = "",

    @SerializedName("LoadHID")
    val loadHID: String = "",

    @SerializedName("ApproveChecker")
    val approveChecker: String = "",

    @SerializedName("LockYN")
    val lockYN: String = "",

    @SerializedName("jmlSP")
    val jmlSP: String = "",

    @SerializedName("tujuanID")
    val tujuanID: String = "",

    @SerializedName("tujuanNama")
    val tujuanNama: String = "",

    @SerializedName("sttBTT")
    val sttBTT: String = "" // "0" = incomplete, "1" = complete
) {
    fun isComplete(): Boolean = sttBTT == "1"
    fun isLocked(): Boolean = lockYN == "Y"
}

// ========== Request Models ==========

/**
 * Request for GET Letter of Assign (now POST)
 */
data class GetLetterOfAssignRequest(
    @SerializedName("nip")
    val nip: String,

    @SerializedName("pt")
    val pt: String
)

/**
 * Request for Check Location
 */
data class CheckLocationRequest(
    @SerializedName("sID")
    val sID: String,

    @SerializedName("lat")
    val lat: String,

    @SerializedName("lon")
    val lon: String,

    @SerializedName("pt")
    val pt: String
)

/**
 * Request for Checkpoint
 */
data class CheckpointRequest(
    @SerializedName("sID")
    val sID: String,

    @SerializedName("agenID")
    val agenID: String,

    @SerializedName("tglUpdate")
    val tglUpdate: String,

    @SerializedName("km")
    val km: String,

    @SerializedName("lat")
    val lat: String,

    @SerializedName("lon")
    val lon: String,

    @SerializedName("urlpic")
    val urlpic: String,

    @SerializedName("nip")
    val nip: String,

    @SerializedName("urut")
    val urut: String,

    @SerializedName("pt")
    val pt: String
)

/**
 * Request for Complete Letter of Assign
 */
data class CompleteLetterOfAssignRequest(
    @SerializedName("sID")
    val sID: String,

    @SerializedName("pt")
    val pt: String
)

/**
 * Request for Get Muat (Loading) Data
 */
data class GetMuatRequest(
    @SerializedName("sID")
    val sID: String,

    @SerializedName("agenID")
    val agenID: String,

    @SerializedName("pt")
    val pt: String
)

/**
 * Response for unloading (bongkar) data
 */
data class UnloadingDataResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: List<UnloadingData>? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("error")
    val error: ErrorResponse? = null
)

data class UnloadingData(
    @SerializedName("BTTID")
    val bttID: String = "",

    @SerializedName("Berat")
    val berat: String = "",

    @SerializedName("discan")
    val discan: String = "",

    @SerializedName("jberangkat")
    val jberangkat: String = ""
) {
    fun isComplete(): Boolean = discan == jberangkat
}

/**
 * Response for loading detail
 */
data class LoadingDetailResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: List<LoadingDetail>? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("error")
    val error: ErrorResponse? = null
)

data class LoadingDetail(
    @SerializedName("BTTT_ID")
    val btttID: String = "",

    @SerializedName("BTTT_NamaBarang")
    val btttNamaBarang: String = "",

    @SerializedName("BTTT_Berat")
    val btttBerat: String = "",

    @SerializedName("discan")
    val discan: String = "",

    @SerializedName("BTTT_JmlUnit")
    val btttJmlUnit: String = "",

    @SerializedName("ket")
    val ket: String = ""
) {
    fun isComplete(): Boolean = discan == btttJmlUnit
    fun hasDescription(): Boolean = ket.isNotEmpty()
}

/**
 * Response for unloading detail
 */
data class UnloadingDetailResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: List<UnloadingDetail>? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("error")
    val error: ErrorResponse? = null
)

data class UnloadingDetail(
    @SerializedName("BTTID")
    val bttID: String = "",

    @SerializedName("Berat")
    val berat: String = "",

    @SerializedName("discan")
    val discan: String = "",

    @SerializedName("jberangkat")
    val jberangkat: String = ""
) {
    fun isComplete(): Boolean = discan == jberangkat
}

/**
 * Response for KM data
 */
data class KMDataResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: KMData? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("error")
    val error: ErrorResponse? = null
)

data class KMData(
    @SerializedName("KM")
    val km: String = "0"
)

/**
 * Request for Update KM
 */
data class UpdateKMRequest(
    @SerializedName("sID")
    val sID: String,

    @SerializedName("agenId")
    val agenId: String,

    @SerializedName("urut")
    val urut: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("km")
    val km: String,

    @SerializedName("pt")
    val pt: String
)

/**
 * Response for Update KM
 */
data class UpdateKMResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: UpdateKMData? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("error")
    val error: ErrorResponse? = null
)

data class UpdateKMData(
    @SerializedName("SUCCESS")
    val successStatus: String = ""
)

/**
 * Request for Get Bongkar (Unloading) Data
 */
data class GetBongkarRequest(
    @SerializedName("sID")
    val sID: String,

    @SerializedName("agenID")
    val agenID: String,

    @SerializedName("pt")
    val pt: String
)

/**
 * Request for Get Loading Detail
 */
data class GetLoadingDetailRequest(
    @SerializedName("sID")
    val sID: String,

    @SerializedName("agenID")
    val agenID: String,

    @SerializedName("destId")
    val destId: String,

    @SerializedName("loadHId")
    val loadHId: String,

    @SerializedName("pt")
    val pt: String
)

/**
 * Request for Get Unloading Detail
 */
data class GetUnloadingDetailRequest(
    @SerializedName("sID")
    val sID: String,

    @SerializedName("agenID")
    val agenID: String,

    @SerializedName("sourceId")
    val sourceId: String,

    @SerializedName("pt")
    val pt: String
)

/**
 * Request for Get KM
 */
data class KMRequest(
    @SerializedName("kendID")
    val kendID: String,

    @SerializedName("pt")
    val pt: String
)

// ========== ADDITIONAL FEATURES ==========

/**
 * Response for alternative route (rute cadangan)
 */
data class AlternativeRouteResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: List<AlternativeRouteData>? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("error")
    val error: ErrorResponse? = null
)

data class AlternativeRouteData(
    @SerializedName("agenIDCad")
    val agenIDCad: String = "",

    @SerializedName("agenNamaCad")
    val agenNamaCad: String = "",

    @SerializedName("agenUtama")
    val agenUtama: String = ""
)

/**
 * Response for lock/unlock loading
 */
data class LockLoadingResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: LockLoadingData? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("error")
    val error: ErrorResponse? = null
)

data class LockLoadingData(
    @SerializedName("SUCCESS")
    val successStatus: String = "" // "DONE"
)

/**
 * Response for lock/unlock unloading
 */
data class LockUnloadingResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: LockUnloadingData? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("error")
    val error: ErrorResponse? = null
)

data class LockUnloadingData(
    @SerializedName("SUCCESS")
    val successStatus: String = "" // "DONE"
)

/**
 * Response for less items (barang kurang)
 */
data class LessItemsResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: List<LessItemsData>? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("error")
    val error: ErrorResponse? = null
)

data class LessItemsData(
    @SerializedName("BTTID")
    val bttID: String = "",

    @SerializedName("BTTNo")
    val bttNo: String = "",

    @SerializedName("JmlKurang")
    val jmlKurang: String = "",

    @SerializedName("Keterangan")
    val keterangan: String = ""
)

/**
 * Response for upload photo
 */
data class LetterOfAssignUploadPhotoResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: LetterOfAssignUploadPhotoData? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("error")
    val error: ErrorResponse? = null
)

data class LetterOfAssignUploadPhotoData(
    @SerializedName("url")
    val url: String = "",

    @SerializedName("filename")
    val filename: String = ""
)

// ========== Request Models for Additional Features ==========

/**
 * Request for Get Alternative Route
 */
data class GetAlternativeRouteRequest(
    @SerializedName("sID")
    val sID: String,

    @SerializedName("pt")
    val pt: String
)

/**
 * Request for Lock Loading
 */
data class LockLoadingRequest(
    @SerializedName("sID")
    val sID: String,

    @SerializedName("agenID")
    val agenID: String,

    @SerializedName("loadHId")
    val loadHId: String,

    @SerializedName("pt")
    val pt: String
)

/**
 * Request for Unlock Loading
 */
data class UnlockLoadingRequest(
    @SerializedName("sID")
    val sID: String,

    @SerializedName("agenID")
    val agenID: String,

    @SerializedName("loadHId")
    val loadHId: String,

    @SerializedName("pt")
    val pt: String
)

/**
 * Request for Lock Unloading
 */
data class LockUnloadingRequest(
    @SerializedName("unloadId")
    val unloadId: String,

    @SerializedName("nip")
    val nip: String,

    @SerializedName("pt")
    val pt: String
)

/**
 * Request for Get Less Items
 */
data class GetLessItemsRequest(
    @SerializedName("nip")
    val nip: String,

    @SerializedName("pt")
    val pt: String
)

/**
 * Request for Upload Photo
 */
data class LetterOfAssignUploadPhotoRequest(
    @SerializedName("base64Image")
    val base64Image: String, // Base64 encoded image

    @SerializedName("pt")
    val pt: String
)
