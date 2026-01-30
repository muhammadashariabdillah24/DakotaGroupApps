package com.dakotagroupstaff.data.local.dao

import androidx.room.*
import com.dakotagroupstaff.data.local.entity.AgentLocationEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Agent Locations Cache
 * CRITICAL: Used for offline GPS validation
 */
@Dao
interface AgentLocationDao {
    
    @Query("SELECT * FROM agent_locations_cache ORDER BY nama_agen ASC")
    fun getAllLocations(): Flow<List<AgentLocationEntity>>
    
    @Query("SELECT * FROM agent_locations_cache")
    suspend fun getAllLocationsSync(): List<AgentLocationEntity>
    
    @Query("SELECT * FROM agent_locations_cache WHERE kode_agen = :kodeAgen")
    fun getLocation(kodeAgen: Int): Flow<AgentLocationEntity?>
    
    @Query("SELECT * FROM agent_locations_cache WHERE kode_agen = :kodeAgen")
    suspend fun getLocationSync(kodeAgen: Int): AgentLocationEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(locations: List<AgentLocationEntity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(location: AgentLocationEntity)
    
    @Query("DELETE FROM agent_locations_cache")
    suspend fun clearAll()
}
