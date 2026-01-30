package com.dakotagroupstaff.data.repository

import com.dakotagroupstaff.data.local.dao.RecentMenuDao
import com.dakotagroupstaff.data.local.entity.RecentMenuEntity
import kotlinx.coroutines.flow.Flow

class MenuRepository(private val recentMenuDao: RecentMenuDao) {

    fun getRecentMenus(): Flow<List<RecentMenuEntity>> = recentMenuDao.getRecentMenus()

    suspend fun saveToHistory(menuId: String, name: String, iconRes: Int, activityClass: String) {
        val recentMenu = RecentMenuEntity(
            menuId = menuId,
            menuName = name,
            iconResId = iconRes,
            activityClass = activityClass,
            lastOpened = System.currentTimeMillis()
        )
        recentMenuDao.insertRecentMenu(recentMenu)
    }

    suspend fun clearHistory() {
        recentMenuDao.clearHistory()
    }

    companion object {
        @Volatile
        private var instance: MenuRepository? = null

        fun getInstance(recentMenuDao: RecentMenuDao): MenuRepository =
            instance ?: synchronized(this) {
                instance ?: MenuRepository(recentMenuDao)
            }.also { instance = it }
    }
}
