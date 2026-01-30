package com.dakotagroupstaff.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Leave Details Cache Entity
 * Cached from: POST /api/v1/leave/details?pt=<pt>
 * Update Strategy: Daily
 */
@Entity(tableName = "leave_details_cache")
data class LeaveDetailsEntity(
    @PrimaryKey
    @ColumnInfo(name = "leave_id")
    val leaveId: String,
    
    @ColumnInfo(name = "nip")
    val nip: String,
    
    @ColumnInfo(name = "tahun")
    val tahun: String,
    
    @ColumnInfo(name = "tgl_awal")
    val tglAwal: Long, // Unix timestamp
    
    @ColumnInfo(name = "tgl_akhir")
    val tglAkhir: Long, // Unix timestamp
    
    @ColumnInfo(name = "jenis_cuti")
    val jenisCuti: String,
    
    @ColumnInfo(name = "keterangan")
    val keterangan: String,
    
    @ColumnInfo(name = "status")
    val status: String, // Pending, Approved, Rejected
    
    @ColumnInfo(name = "potong_gaji")
    val potongGaji: String,
    
    @ColumnInfo(name = "potong_cuti")
    val potongCuti: String,
    
    @ColumnInfo(name = "dispensasi")
    val dispensasi: String,
    
    @ColumnInfo(name = "atasan1_approve")
    val atasan1Approve: String,
    
    @ColumnInfo(name = "atasan2_approve")
    val atasan2Approve: String,
    
    @ColumnInfo(name = "approved_by")
    val approvedBy: String? = null,
    
    @ColumnInfo(name = "approval_date")
    val approvalDate: Long? = null,
    
    @ColumnInfo(name = "rejection_reason")
    val rejectionReason: String? = null,
    
    @ColumnInfo(name = "cached_at")
    val cachedAt: Long = System.currentTimeMillis()
) {
    /**
     * Check if cache is still valid (1 day)
     */
    fun isValid(): Boolean {
        val oneDayInMillis = 24 * 60 * 60 * 1000L
        return (System.currentTimeMillis() - cachedAt) < oneDayInMillis
    }
}
