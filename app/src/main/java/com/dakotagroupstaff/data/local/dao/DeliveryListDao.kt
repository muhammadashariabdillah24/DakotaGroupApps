package com.dakotagroupstaff.data.local.dao

import androidx.room.*
import com.dakotagroupstaff.data.local.entity.DeliveryListEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Delivery List Cache
 * For drivers only
 */
@Dao
interface DeliveryListDao {
    
    @Query("SELECT * FROM delivery_list_cache ORDER BY cached_at DESC")
    fun getAllDeliveries(): Flow<List<DeliveryListEntity>>
    
    @Query("SELECT * FROM delivery_list_cache")
    suspend fun getAllDeliveriesSync(): List<DeliveryListEntity>
    
    @Query("SELECT * FROM delivery_list_cache WHERE status = :status ORDER BY cached_at DESC")
    fun getDeliveriesByStatus(status: String): Flow<List<DeliveryListEntity>>
    
    @Query("SELECT * FROM delivery_list_cache WHERE id = :id")
    suspend fun getDeliveryById(id: String): DeliveryListEntity?
    
    @Query("SELECT * FROM delivery_list_cache WHERE no_btt = :noBtt")
    suspend fun getDeliveryByBtt(noBtt: String): DeliveryListEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(deliveries: List<DeliveryListEntity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(delivery: DeliveryListEntity)
    
    @Update
    suspend fun update(delivery: DeliveryListEntity)
    
    @Query("DELETE FROM delivery_list_cache WHERE no_btt = :noBtt")
    suspend fun deleteByBtt(noBtt: String)
    
    @Query("DELETE FROM delivery_list_cache WHERE cached_at < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
    
    @Query("DELETE FROM delivery_list_cache")
    suspend fun clearAll()
}
