package com.dakotagroupstaff.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dakotagroupstaff.data.local.entity.RecentMenuEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentMenuDao {
    @Query("SELECT * FROM recent_menus ORDER BY last_opened DESC LIMIT 10")
    fun getRecentMenus(): Flow<List<RecentMenuEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentMenu(menu: RecentMenuEntity)

    @Query("DELETE FROM recent_menus WHERE menu_id = :menuId")
    suspend fun deleteRecentMenu(menuId: String)

    @Query("DELETE FROM recent_menus")
    suspend fun clearHistory()
}
