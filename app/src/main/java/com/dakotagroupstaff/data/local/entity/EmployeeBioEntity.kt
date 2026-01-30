package com.dakotagroupstaff.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Employee Bio Data Cache Entity
 * Cached from: POST /api/v1/employee/bio?pt=<pt>
 * Update Strategy: Every 7 days
 */
@Entity(tableName = "employee_bio_cache")
data class EmployeeBioEntity(
    @PrimaryKey
    @ColumnInfo(name = "nip")
    val nip: String,
    
    @ColumnInfo(name = "atasan1")
    val atasan1: String,
    
    @ColumnInfo(name = "atasan1_nama")
    val atasan1Nama: String,
    
    @ColumnInfo(name = "atasan2")
    val atasan2: String? = null,
    
    @ColumnInfo(name = "atasan2_nama")
    val atasan2Nama: String? = null,
    
    @ColumnInfo(name = "bpjs")
    val bpjs: String,
    
    @ColumnInfo(name = "shift")
    val shift: String,
    
    @ColumnInfo(name = "status_pegawai")
    val statusPegawai: String,
    
    @ColumnInfo(name = "cached_at")
    val cachedAt: Long = System.currentTimeMillis()
) {
    /**
     * Check if cache is still valid (7 days)
     */
    fun isValid(): Boolean {
        val sevenDaysInMillis = 7 * 24 * 60 * 60 * 1000L
        return (System.currentTimeMillis() - cachedAt) < sevenDaysInMillis
    }
}
