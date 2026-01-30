package com.dakotagroupstaff.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Leave Balance Cache Entity
 * Cached from: POST /api/v1/leave/balance?pt=<pt>
 * Update Strategy: Daily
 */
@Entity(tableName = "leave_balance_cache")
data class LeaveBalanceEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,
    
    @ColumnInfo(name = "nip")
    val nip: String,
    
    @ColumnInfo(name = "tahun")
    val tahun: String,
    
    @ColumnInfo(name = "saldo_cuti")
    val saldoCuti: String,
    
    @ColumnInfo(name = "jumlah_cuti")
    val jumlahCuti: String,
    
    @ColumnInfo(name = "cuti_terpakai")
    val cutiTerpakai: String,
    
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
