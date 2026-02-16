package com.dakotagroupstaff

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.dakotagroupstaff.di.databaseModule
import com.dakotagroupstaff.di.dataStoreModule
import com.dakotagroupstaff.di.networkModule
import com.dakotagroupstaff.di.repositoryModule
import com.dakotagroupstaff.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class DakotaGroupStaffApp : Application() {
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
    }
}
