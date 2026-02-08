package com.dakotagroupstaff.di

import android.content.Context
import androidx.room.Room
import com.dakotagroupstaff.data.local.preferences.dataStore
import com.dakotagroupstaff.data.local.room.AppDatabase
import com.dakotagroupstaff.data.remote.retrofit.ApiConfig
import com.dakotagroupstaff.data.repository.LeaveRepository
import com.dakotagroupstaff.data.repository.SalaryRepository

/**
 * Dependency Injection helper object
 * Provides repositories for manual dependency injection
 * Used when Koin is not available or for specific use cases
 */
object Injection {
    
    /**
     * Provide SalaryRepository instance
     */
    fun provideSalaryRepository(context: Context): SalaryRepository {
        val userPreferences = com.dakotagroupstaff.data.local.preferences.UserPreferences.getInstance(context.dataStore)
        val apiService = ApiConfig.getApiService(userPreferences = userPreferences)
        return SalaryRepository.getInstance(apiService)
    }
    
    /**
     * Provide LeaveRepository instance
     */
    fun provideLeaveRepository(context: Context): LeaveRepository {
        val userPreferences = com.dakotagroupstaff.data.local.preferences.UserPreferences.getInstance(context.dataStore)
        val apiService = ApiConfig.getApiService(userPreferences = userPreferences)
        val database = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "dakota_group_staff.db"
        ).build()
        
        return LeaveRepository(
            apiService = apiService,
            leaveBalanceDao = database.leaveBalanceDao(),
            leaveDetailsDao = database.leaveDetailsDao()
        )
    }
}
