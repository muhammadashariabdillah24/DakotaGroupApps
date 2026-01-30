package com.dakotagroupstaff.data.local.pref

import android.content.Context
import com.dakotagroupstaff.data.local.preferences.UserPreferences
import com.dakotagroupstaff.data.local.preferences.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * SessionManager - Wrapper for UserPreferences
 * Provides synchronous access to user session data
 * This is a compatibility layer for activities that need synchronous session access
 */
class SessionManager(context: Context) {
    
    private val userPreferences = UserPreferences.getInstance(context.dataStore)
    
    /**
     * Get NIP from session (synchronous)
     */
    fun getNip(): String? {
        return runBlocking {
            userPreferences.getSession().first().nip.takeIf { it.isNotEmpty() }
        }
    }
    
    /**
     * Get PT from session (synchronous)
     */
    fun getPt(): String? {
        return runBlocking {
            userPreferences.getSession().first().pt.takeIf { it.isNotEmpty() }
        }
    }
    
    /**
     * Get NAMA from session (synchronous)
     */
    fun getNama(): String? {
        return runBlocking {
            userPreferences.getSession().first().nama.takeIf { it.isNotEmpty() }
        }
    }
    
    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean {
        return runBlocking {
            userPreferences.getSession().first().isLoggedIn
        }
    }
    
    /**
     * Get IMEI from session (synchronous)
     */
    fun getImei(): String? {
        return runBlocking {
            userPreferences.getSession().first().imei.takeIf { it.isNotEmpty() }
        }
    }
    
    /**
     * Get SIM ID from session (synchronous)
     */
    fun getSimId(): String? {
        return runBlocking {
            userPreferences.getSession().first().simId.takeIf { it.isNotEmpty() }
        }
    }
    
    /**
     * Get Atasan1 from session (synchronous)
     */
    fun getAtasan1(): String? {
        return runBlocking {
            userPreferences.getSession().first().atasan1.takeIf { it.isNotEmpty() }
        }
    }
    
    /**
     * Get Atasan2 from session (synchronous)
     */
    fun getAtasan2(): String? {
        return runBlocking {
            userPreferences.getSession().first().atasan2.takeIf { it.isNotEmpty() }
        }
    }
}
