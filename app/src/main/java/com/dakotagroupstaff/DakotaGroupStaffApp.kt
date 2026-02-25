package com.dakotagroupstaff

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.dakotagroupstaff.data.local.preferences.UserPreferences
import com.dakotagroupstaff.data.local.preferences.dataStore
import com.dakotagroupstaff.di.databaseModule
import com.dakotagroupstaff.di.dataStoreModule
import com.dakotagroupstaff.di.networkModule
import com.dakotagroupstaff.di.repositoryModule
import com.dakotagroupstaff.di.viewModelModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class DakotaGroupStaffApp : Application() {
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onCreate() {
        super.onCreate()
        
        // Enable DayNight theme to follow system theme changes dynamically
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        
        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.ERROR else Level.NONE)
            androidContext(this@DakotaGroupStaffApp)
            modules(
                listOf(
                    networkModule,
                    databaseModule,
                    dataStoreModule,
                    repositoryModule,
                    viewModelModule
                )
            )
        }
        
        // Clear any stale session data on app startup
        // This ensures clean state after app reinstall
        clearStaleSessionData()
    }
    
    private fun clearStaleSessionData() {
        applicationScope.launch {
            try {
                val userPreferences = UserPreferences.getInstance(dataStore)
                // Check if we have a session but no valid tokens
                // This can happen after app reinstall with backup restore
                val session = userPreferences.getSession()
                session.collect { userSession ->
                    if (userSession.isLoggedIn) {
                        android.util.Log.d("DakotaGroupStaffApp", "Found existing session for NIP: ${userSession.nip}")
                        // Session exists, let MainActivity handle the validation
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("DakotaGroupStaffApp", "Error checking session: ${e.message}")
            }
        }
    }
}
