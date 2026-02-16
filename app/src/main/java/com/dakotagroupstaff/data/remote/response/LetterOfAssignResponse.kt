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
