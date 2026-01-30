package com.dakotagroupstaff.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Agent Location Cache Entity
 * Cached from: GET /api/v1/agent/locations?pt=<pt>
 * Update Strategy: Every 30 days
 * CRITICAL: Used for offline GPS validation during attendance check-in/check-out
 */
@Entity(tableName = "agent_locations_cache")
data class AgentLocationEntity(
    @PrimaryKey
    @ColumnInfo(name = "kode_agen")
    val kodeAgen: Int,
    
    @ColumnInfo(name = "nama_agen")
    val namaAgen: String,
    
    @ColumnInfo(name = "lat")
    val lat: String,
    
    @ColumnInfo(name = "lon")
    val lon: String,
    
    @ColumnInfo(name = "md5_code")
    val md5Code: String = "",
    
    @ColumnInfo(name = "range")
    val range: String,
    
    @ColumnInfo(name = "cached_at")
    val cachedAt: Long = System.currentTimeMillis()
) {
    /**
     * Check if cache is still valid (30 days)
     */
    fun isValid(): Boolean {
        val thirtyDaysInMillis = 30 * 24 * 60 * 60 * 1000L
        return (System.currentTimeMillis() - cachedAt) < thirtyDaysInMillis
    }
    
    /**
     * Calculate distance from given coordinates (in meters)
     * Using Haversine formula
     * Returns Double.MAX_VALUE if coordinates are invalid
     */
    fun distanceFrom(userLat: Double, userLon: Double): Double {
        // Validate coordinates before calculation
        val agentLat = lat.toDoubleOrNull()
        val agentLon = lon.toDoubleOrNull()
        
        if (agentLat == null || agentLon == null) {
            // Return max distance if invalid coordinates
            return Double.MAX_VALUE
        }
        
        val earthRadius = 6371000.0 // meters
        
        val lat1 = Math.toRadians(agentLat)
        val lat2 = Math.toRadians(userLat)
        val lon1 = Math.toRadians(agentLon)
        val lon2 = Math.toRadians(userLon)
        
        val dLat = lat2 - lat1
        val dLon = lon2 - lon1
        
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        
        return earthRadius * c
    }
    
    /**
     * Check if user is within range
     * Returns false if coordinates are invalid
     */
    fun isWithinRange(userLat: Double, userLon: Double): Boolean {
        val distance = distanceFrom(userLat, userLon)
        
        // If distance calculation failed (invalid coordinates), return false
        if (distance == Double.MAX_VALUE) {
            return false
        }
        
        val rangeMeters = range.toDoubleOrNull() ?: 100.0
        return distance <= rangeMeters
    }
}
