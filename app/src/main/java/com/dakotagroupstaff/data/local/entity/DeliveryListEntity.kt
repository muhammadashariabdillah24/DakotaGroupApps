package com.dakotagroupstaff.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Delivery List Cache Entity
 * Cached from: POST /api/v1/delivery/list?pt=<pt>
 * Update Strategy: Daily
 * For drivers only - last 14 days of delivery data
 */
@Entity(tableName = "delivery_list_cache")
data class DeliveryListEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    
    @ColumnInfo(name = "no_loper")
    val noLoper: String,
    
    @ColumnInfo(name = "no_btt")
    val noBtt: String,
    
    @ColumnInfo(name = "penerima")
    val penerima: String,
    
    @ColumnInfo(name = "alamat")
    val alamat: String,
    
    @ColumnInfo(name = "jumlah_koli")
    val jumlahKoli: Int,
    
    @ColumnInfo(name = "status")
    val status: String, // Pending, Sent, Delivered, Cancelled
    
    @ColumnInfo(name = "tanggal_kirim")
    val tanggalKirim: Long? = null,
    
    @ColumnInfo(name = "keterangan")
    val keterangan: String? = null,
    
    @ColumnInfo(name = "cached_at")
    val cachedAt: Long = System.currentTimeMillis(),
    
    // Media data stored as Base64 for local submission draft
    @ColumnInfo(name = "foto_base64")
    val fotoBase64: String? = null,
    
    @ColumnInfo(name = "ttd_base64")
    val ttdBase64: String? = null,
    
    // GPS coordinates
    @ColumnInfo(name = "latitude")
    val latitude: String? = null,
    
    @ColumnInfo(name = "longitude")
    val longitude: String? = null
) {
    /**
     * Check if cache is still valid (1 day)
     */
    fun isValid(): Boolean {
        val oneDayInMillis = 24 * 60 * 60 * 1000L
        return (System.currentTimeMillis() - cachedAt) < oneDayInMillis
    }
    
    /**
     * Check if delivery data is within last 14 days
     */
    fun isWithinFourteenDays(): Boolean {
        val fourteenDaysInMillis = 14 * 24 * 60 * 60 * 1000L
        return (System.currentTimeMillis() - cachedAt) < fourteenDaysInMillis
    }
}
