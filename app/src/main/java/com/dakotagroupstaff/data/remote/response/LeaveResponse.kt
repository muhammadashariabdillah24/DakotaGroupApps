package com.dakotagroupstaff.data.remote.response

import com.google.gson.annotations.SerializedName

/**
 * Leave Balance Response
 */
data class LeaveBalanceData(
    @SerializedName("SALDOCUTI")
    val saldoCuti: String,
    
    @SerializedName("JUMLAHCUTI")
    val jumlahCuti: String,
    
    @SerializedName("CUTITERPAKAI")
    val cutiTerpakai: String
)

/**
 * Leave Details/History Response
 */
data class LeaveDetailsData(
    @SerializedName("ID")
    val id: String,
    
    @SerializedName("MULAI")
    val mulai: String,
    
    @SerializedName("AKHIR")
    val akhir: String,
    
    @SerializedName("STATUS")
    val status: String,
    
    @SerializedName("KETERANGAN")
    val keterangan: String,
    
    @SerializedName("POTONGGAJI")
    val potongGaji: String,
    
    @SerializedName("POTONGCUTI")
    val potongCuti: String,
    
    @SerializedName("DISPENSASI")
    val dispensasi: String,
    
    @SerializedName("BIAYA")
    val biaya: String,
    
    @SerializedName("AKTIF")
    val aktif: String,
    
    @SerializedName("FORM")
    val form: String,
    
    @SerializedName("ATASAN1 NIP")
    val atasan1Nip: String,
    
    @SerializedName("ATASAN1")
    val atasan1: String,
    
    @SerializedName("ATASAN2 NIP")
    val atasan2Nip: String,
    
    @SerializedName("ATASAN2")
    val atasan2: String,
    
    @SerializedName("SURAT")
    val surat: String
)

/**
 * Leave Submission Response
 */
data class LeaveSubmissionData(
    val id: String,
    val key: String,
    val status: String
)

/**
 * Leave Approval Response
 */
data class LeaveApprovalData(
    @SerializedName("STATUS")
    val status: String,
    
    val leaveId: String,
    val approvedBy: String,
    val approvalValue: String,
    val approvalLevel: String? = null
)

/**
 * Leave Rejection Response
 */
data class LeaveRejectionData(
    @SerializedName("STATUS")
    val status: String,
    
    val leaveId: String,
    val updatedBy: String,
    val activeStatus: String
)

/**
 * Pending Approval Item Response
 * Used in approval list for supervisors
 * Migrated from OldSystemApproval
 */
data class PendingApprovalData(
    @SerializedName("ID CUTI")
    val idCuti: String,
    
    @SerializedName("NIP PENGAJU")
    val nipPengaju: String,
    
    @SerializedName("NAMA PENGAJU")
    val namaPengaju: String,
    
    @SerializedName("TGL MULAI")
    val tglMulai: String,
    
    @SerializedName("TGL AKHIR")
    val tglAkhir: String,
    
    @SerializedName("STATUS")
    val status: String,
    
    @SerializedName("KETERANGAN")
    val keterangan: String,
    
    @SerializedName("SALDOCUTI")
    val saldoCuti: String,
    
    @SerializedName("ATASAN1 NIP")
    val atasan1Nip: String,
    
    @SerializedName("ATASAN1")
    val atasan1: String,
    
    @SerializedName("ATASAN2 NIP")
    val atasan2Nip: String,
    
    @SerializedName("ATASAN2")
    val atasan2: String,
    
    @SerializedName("SURAT")
    val surat: String,
    
    @SerializedName("POTONGGAJI")
    val potongGaji: String,
    
    @SerializedName("POTONGCUTI")
    val potongCuti: String,
    
    @SerializedName("DISPENSASI")
    val dispensasi: String,
    
    val totalDays: Int
)

/**
 * Leave Type Enum
 */
enum class LeaveType(val code: String, val displayName: String) {
    CUTI("C", "Cuti"),
    IZIN("I", "Izin"),
    SAKIT("S", "Sakit"),
    CUTI_BERSAMA("B", "Cuti Bersama"),
    DISPENSASI("G", "Dispensasi"),
    KLAIM_OBAT("K", "Klaim Obat"),
    DATANG_TERLAMBAT("DT", "Datang Terlambat"),
    PULANG_CEPAT("PC", "Pulang Cepat"),
    MENINGGALKAN_PEKERJAAN("MP", "Meninggalkan Pekerjaan");
    
    companion object {
        fun fromCode(code: String): LeaveType? {
            return values().find { it.code == code }
        }
    }
}

/**
 * Supervisor Check Response
 * Returns empty object {} if not supervisor, or STATUS="ATASAN " if supervisor
 */
data class SupervisorCheckData(
    @SerializedName("STATUS")
    val status: String? = null,
    
    val isSupervisor: Boolean? = null,
    
    val subordinatesCount: Int? = null
) {
    /**
     * Check if the NIP is a supervisor
     * Returns true if STATUS field contains "ATASAN"
     */
    fun isActuallySupervisor(): Boolean {
        return !status.isNullOrBlank() && status.contains("ATASAN", ignoreCase = true)
    }
}

/**
 * Super Atasan (Super Supervisor) Response
 * List of super supervisors from HRD_M_SuperAtasan table
 */
data class SuperAtasanData(
    @SerializedName("NIP")
    val nip: String,
    
    @SerializedName("NAMA")
    val nama: String
)

/**
 * Leave Status Display Helper
 */
object LeaveStatusHelper {
    fun getStatusName(code: String): String {
        return when (code) {
            "C" -> "Cuti"
            "I" -> "Izin"
            "S" -> "Sakit"
            "B" -> "Cuti Bersama"
            "G" -> "Dispensasi"
            "K" -> "Klaim Obat"
            "DT" -> "Datang Terlambat"
            "PC" -> "Pulang Cepat"
            "MP" -> "Meninggalkan Pekerjaan"
            else -> code
        }
    }
    
    fun getApprovalStatus(atasan1Approve: String, atasan2Approve: String): String {
        return when {
            atasan1Approve == "Y" && atasan2Approve == "Y" -> "Disetujui"
            atasan1Approve == "N" || atasan2Approve == "N" -> "Ditolak"
            else -> "Menunggu Persetujuan"
        }
    }
}
