package com.dakotagroupstaff.data.local.dao

import androidx.room.*
import com.dakotagroupstaff.data.local.entity.EmployeeBioEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Employee Bio Cache
 */
@Dao
interface EmployeeBioDao {
    
    @Query("SELECT * FROM employee_bio_cache WHERE nip = :nip")
    fun getEmployeeBio(nip: String): Flow<EmployeeBioEntity?>
    
    @Query("SELECT * FROM employee_bio_cache WHERE nip = :nip")
    suspend fun getEmployeeBioSync(nip: String): EmployeeBioEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(employeeBio: EmployeeBioEntity)
    
    @Update
    suspend fun update(employeeBio: EmployeeBioEntity)
    
    @Query("DELETE FROM employee_bio_cache WHERE nip = :nip")
    suspend fun delete(nip: String)
    
    @Query("DELETE FROM employee_bio_cache")
    suspend fun clearAll()
}
