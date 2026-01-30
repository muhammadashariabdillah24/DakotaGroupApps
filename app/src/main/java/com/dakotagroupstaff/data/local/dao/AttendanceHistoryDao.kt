package com.dakotagroupstaff.data.local.dao

import androidx.room.*
import com.dakotagroupstaff.data.local.entity.AttendanceHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Attendance History Cache
 */
@Dao
interface AttendanceHistoryDao {
    
    @Query("SELECT * FROM attendance_history_cache WHERE abs_nip = :nip ORDER BY abs_tanggal DESC")
    fun getAttendanceHistory(nip: String): Flow<List<AttendanceHistoryEntity>>
    
    @Query("SELECT * FROM attendance_history_cache WHERE abs_nip = :nip ORDER BY abs_tanggal DESC")
    suspend fun getAttendanceHistorySync(nip: String): List<AttendanceHistoryEntity>
    
    @Query("SELECT * FROM attendance_history_cache WHERE abs_nip = :nip AND abs_tanggal >= :startDate AND abs_tanggal <= :endDate ORDER BY abs_tanggal DESC")
    fun getAttendanceHistoryByDateRange(nip: String, startDate: Long, endDate: Long): Flow<List<AttendanceHistoryEntity>>
    
    @Query("SELECT * FROM attendance_history_cache WHERE abs_id = :absId")
    suspend fun getAttendanceById(absId: Int): AttendanceHistoryEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(attendances: List<AttendanceHistoryEntity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attendance: AttendanceHistoryEntity)
    
    @Query("DELETE FROM attendance_history_cache WHERE abs_tanggal < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
    
    @Query("DELETE FROM attendance_history_cache WHERE abs_nip = :nip")
    suspend fun delete(nip: String)
    
    @Query("DELETE FROM attendance_history_cache")
    suspend fun clearAll()
}
