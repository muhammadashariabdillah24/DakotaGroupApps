package com.dakotagroupstaff.data.local.dao

import androidx.room.*
import com.dakotagroupstaff.data.local.entity.LeaveDetailsEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Leave Details Cache
 */
@Dao
interface LeaveDetailsDao {
    
    @Query("SELECT * FROM leave_details_cache WHERE nip = :nip ORDER BY tgl_awal DESC")
    fun getLeaveDetails(nip: String): Flow<List<LeaveDetailsEntity>>
    
    @Query("SELECT * FROM leave_details_cache WHERE nip = :nip AND tahun = :tahun ORDER BY tgl_awal DESC")
    fun getLeaveDetailsByYear(nip: String, tahun: String): Flow<List<LeaveDetailsEntity>>
    
    @Query("SELECT * FROM leave_details_cache WHERE nip = :nip ORDER BY tgl_awal DESC")
    suspend fun getLeaveDetailsSync(nip: String): List<LeaveDetailsEntity>
    
    @Query("SELECT * FROM leave_details_cache WHERE nip = :nip AND status = :status ORDER BY tgl_awal DESC")
    fun getLeaveDetailsByStatus(nip: String, status: String): Flow<List<LeaveDetailsEntity>>
    
    @Query("SELECT * FROM leave_details_cache WHERE leave_id = :leaveId")
    suspend fun getLeaveById(leaveId: String): LeaveDetailsEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(leaveDetails: List<LeaveDetailsEntity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(leaveDetails: LeaveDetailsEntity)
    
    @Update
    suspend fun update(leaveDetails: LeaveDetailsEntity)
    
    @Query("DELETE FROM leave_details_cache WHERE nip = :nip")
    suspend fun delete(nip: String)
    
    @Query("DELETE FROM leave_details_cache WHERE nip = :nip AND tahun = :tahun")
    suspend fun deleteByYear(nip: String, tahun: String)
    
    @Query("DELETE FROM leave_details_cache")
    suspend fun clearAll()
}
