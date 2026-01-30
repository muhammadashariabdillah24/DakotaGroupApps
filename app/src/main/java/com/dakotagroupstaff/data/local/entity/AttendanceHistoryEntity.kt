package com.dakotagroupstaff.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Attendance History Cache Entity
 * Cached from: POST /api/v1/attendance?pt=<pt>
 * Update Strategy: Daily (keep last 3 months)
 * KETERANGAN values: M (Masuk/In), K (Keluar/Out), H (Hadir), O (Out)
 */
@Entity(tableName = "attendance_history_cache")
data class AttendanceHistoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "abs_id")
    val absId: Int,
    
    @ColumnInfo(name = "abs_nip")
    val absNip: String,
    
    @ColumnInfo(name = "abs_tanggal")
    val absTanggal: Long, // Unix timestamp
    
    @ColumnInfo(name = "abs_schedule")
    val absSchedule: String,
    
    @ColumnInfo(name = "abs_in_time")
    val absInTime: String? = null,
    
    @ColumnInfo(name = "abs_in_location")
    val absInLocation: String? = null,
    
    @ColumnInfo(name = "abs_out_time")
    val absOutTime: String? = null,
    
    @ColumnInfo(name = "abs_out_location")
    val absOutLocation: String? = null,
    
    @ColumnInfo(name = "abs_status")
    val absStatus: String,
    
    @ColumnInfo(name = "keterangan")
    val keterangan: String = "H", // M=Masuk, K=Keluar, H=Hadir, O=Out
    
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
    
    /**
     * Check if attendance data is within last 3 months
     */
    fun isWithinThreeMonths(): Boolean {
        val threeMonthsInMillis = 90 * 24 * 60 * 60 * 1000L
        return (System.currentTimeMillis() - absTanggal) < threeMonthsInMillis
    }
}
