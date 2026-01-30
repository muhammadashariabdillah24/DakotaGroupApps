package com.dakotagroupstaff.data.local.dao

import androidx.room.*
import com.dakotagroupstaff.data.local.entity.LeaveBalanceEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Leave Balance Cache
 */
@Dao
interface LeaveBalanceDao {
    
    @Query("SELECT * FROM leave_balance_cache WHERE nip = :nip ORDER BY id DESC LIMIT 1")
    fun getLeaveBalance(nip: String): Flow<LeaveBalanceEntity?>
    
    @Query("SELECT * FROM leave_balance_cache WHERE nip = :nip AND tahun = :tahun ORDER BY id DESC LIMIT 1")
    fun getLeaveBalanceByYear(nip: String, tahun: String): Flow<LeaveBalanceEntity?>
    
    @Query("SELECT * FROM leave_balance_cache WHERE nip = :nip ORDER BY id DESC LIMIT 1")
    suspend fun getLeaveBalanceSync(nip: String): LeaveBalanceEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(leaveBalance: LeaveBalanceEntity)
    
    @Update
    suspend fun update(leaveBalance: LeaveBalanceEntity)
    
    @Query("DELETE FROM leave_balance_cache WHERE nip = :nip")
    suspend fun delete(nip: String)
    
    @Query("DELETE FROM leave_balance_cache")
    suspend fun clearAll()
}
