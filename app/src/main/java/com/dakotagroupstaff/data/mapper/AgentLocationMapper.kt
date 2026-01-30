package com.dakotagroupstaff.data.mapper

import com.dakotagroupstaff.data.local.entity.AgentLocationEntity
import com.dakotagroupstaff.data.remote.response.AgentLocationData

/**
 * Mapper for Agent Location
 * Convert API Response to Room Entity and vice versa
 */
object AgentLocationMapper {
    
    /**
     * Convert API Response to Room Entity
     * Filters out agents with invalid GPS coordinates (empty lat/lon)
     */
    fun fromResponse(response: AgentLocationData): AgentLocationEntity? {
        // Validate GPS coordinates - reject if empty or invalid
        if (response.agenLat.isBlank() || response.agenLon.isBlank()) {
            return null
        }
        
        // Additional validation: check if coordinates are valid numbers
        val lat = response.agenLat.toDoubleOrNull()
        val lon = response.agenLon.toDoubleOrNull()
        
        if (lat == null || lon == null) {
            return null
        }
        
        return AgentLocationEntity(
            kodeAgen = response.kodeAgen,
            namaAgen = response.namaAgen,
            lat = response.agenLat,
            lon = response.agenLon,
            md5Code = response.agenMd5 ?: "",
            range = response.agenRange,
            cachedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * Convert List of API Response to List of Room Entity
     * Automatically filters out agents with invalid GPS coordinates
     */
    fun fromResponseList(responses: List<AgentLocationData>): List<AgentLocationEntity> {
        return responses.mapNotNull { fromResponse(it) }
    }
}
