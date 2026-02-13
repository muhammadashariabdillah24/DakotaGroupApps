package com.dakotagroupstaff.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dakotagroupstaff.data.local.model.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferences private constructor(private val dataStore: DataStore<Preferences>) {

    private val NIP_KEY = stringPreferencesKey("nip")
    private val NAMA_KEY = stringPreferencesKey("nama")
    private val ATASAN1_KEY = stringPreferencesKey("atasan1")
    private val ATASAN2_KEY = stringPreferencesKey("atasan2")
    private val PT_KEY = stringPreferencesKey("pt")
    private val IMEI_KEY = stringPreferencesKey("imei")
    private val SIM_ID_KEY = stringPreferencesKey("sim_id")
    private val EMAIL_KEY = stringPreferencesKey("email")
    private val IS_LOGGED_IN_KEY = booleanPreferencesKey("is_logged_in")
    private val AREA_KERJA_KEY = stringPreferencesKey("area_kerja")
    private val TASK_CODE_KEY = stringPreferencesKey("task_code")
    private val TASK_DETAIL_KEY = stringPreferencesKey("task_detail")
    private val TASK_ID_KEY = stringPreferencesKey("task_id")
    private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
    private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
    private val TOKEN_EXPIRY_KEY = stringPreferencesKey("token_expiry")
    
    // Barcode scanner BTT collection
    private val SCANNED_BTT_IDS = stringSetPreferencesKey("scanned_btt_ids")
    private val CURRENT_BTT_TOTAL_KOLI = intPreferencesKey("current_btt_total_koli")
    private val CURRENT_BTT_NUMBER = stringPreferencesKey("current_btt_number")

    fun getSession(): Flow<UserSession> {
        return dataStore.data.map { preferences ->
            UserSession(
                nip = preferences[NIP_KEY] ?: "",
                nama = preferences[NAMA_KEY] ?: "",
                atasan1 = preferences[ATASAN1_KEY] ?: "",
                atasan2 = preferences[ATASAN2_KEY] ?: "",
                pt = preferences[PT_KEY] ?: "C",
                imei = preferences[IMEI_KEY] ?: "",
                simId = preferences[SIM_ID_KEY] ?: "",
                email = preferences[EMAIL_KEY] ?: "",
                isLoggedIn = preferences[IS_LOGGED_IN_KEY] ?: false,
                areaKerja = preferences[AREA_KERJA_KEY] ?: "",
                taskCode = preferences[TASK_CODE_KEY] ?: "",
                taskDetail = preferences[TASK_DETAIL_KEY] ?: "",
                taskId = preferences[TASK_ID_KEY] ?: ""
            )
        }
    }

    suspend fun saveSession(userSession: UserSession) {
        dataStore.edit { preferences ->
            preferences[NIP_KEY] = userSession.nip
            preferences[NAMA_KEY] = userSession.nama
            preferences[ATASAN1_KEY] = userSession.atasan1
            preferences[ATASAN2_KEY] = userSession.atasan2
            preferences[PT_KEY] = userSession.pt
            preferences[IMEI_KEY] = userSession.imei
            preferences[SIM_ID_KEY] = userSession.simId
            preferences[EMAIL_KEY] = userSession.email
            preferences[IS_LOGGED_IN_KEY] = true
            preferences[AREA_KERJA_KEY] = userSession.areaKerja
            preferences[TASK_CODE_KEY] = userSession.taskCode
            preferences[TASK_DETAIL_KEY] = userSession.taskDetail
            preferences[TASK_ID_KEY] = userSession.taskId
        }
    }

    suspend fun logout() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
    
    fun getNip(): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[NIP_KEY] ?: ""
        }
    }
    
    fun getPt(): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[PT_KEY] ?: "C"
        }
    }
    
    /**
     * Save JWT access token
     */
    suspend fun saveAccessToken(token: String) {
        dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = token
        }
    }
    
    /**
     * Get JWT access token
     */
    fun getAccessToken(): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[ACCESS_TOKEN_KEY] ?: ""
        }
    }
    
    /**
     * Clear JWT access token
     */
    suspend fun clearAccessToken() {
        dataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN_KEY)
        }
    }
    
    /**
     * Save refresh token
     */
    suspend fun saveRefreshToken(refreshToken: String) {
        dataStore.edit { preferences ->
            preferences[REFRESH_TOKEN_KEY] = refreshToken
        }
    }
    
    /**
     * Get refresh token
     */
    fun getRefreshToken(): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[REFRESH_TOKEN_KEY] ?: ""
        }
    }
    
    /**
     * Save token expiry time
     */
    suspend fun saveTokenExpiry(expiresIn: Int) {
        val expiryTime = System.currentTimeMillis() + (expiresIn * 1000L)
        dataStore.edit { preferences ->
            preferences[TOKEN_EXPIRY_KEY] = expiryTime.toString()
        }
    }
    
    /**
     * Check if token is expiring soon (within 5 minutes)
     */
    fun isTokenExpiringSoon(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            val expiryStr = preferences[TOKEN_EXPIRY_KEY] ?: return@map true
            val expiryTime = expiryStr.toLongOrNull() ?: return@map true
            val now = System.currentTimeMillis()
            val fiveMinutes = 5 * 60 * 1000L
            (expiryTime - now) < fiveMinutes
        }
    }
    
    /**
     * Get IMEI
     */
    fun getImei(): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[IMEI_KEY] ?: ""
        }
    }

    // === Scanned BTT Collection Functions ===
    
    /**
     * Add scanned BTT ID to collection
     */
    suspend fun addScannedBttId(bttId: String) {
        dataStore.edit { preferences ->
            val current = preferences[SCANNED_BTT_IDS] ?: emptySet()
            preferences[SCANNED_BTT_IDS] = current + bttId
        }
    }
    
    /**
     * Get all scanned BTT IDs
     */
    suspend fun getScannedBttIds(): Set<String> {
        val preferences = dataStore.data.map { it[SCANNED_BTT_IDS] ?: emptySet() }
        return preferences.first()
    }
    
    /**
     * Clear all scanned BTT IDs and session data
     */
    suspend fun clearScannedBttIds() {
        dataStore.edit { preferences ->
            preferences.remove(SCANNED_BTT_IDS)
            preferences.remove(CURRENT_BTT_TOTAL_KOLI)
            preferences.remove(CURRENT_BTT_NUMBER)
        }
    }
    
    /**
     * Set total koli for current BTT
     */
    suspend fun setCurrentBttTotalKoli(total: Int) {
        dataStore.edit { preferences ->
            preferences[CURRENT_BTT_TOTAL_KOLI] = total
        }
    }
    
    /**
     * Get total koli for current BTT
     */
    suspend fun getCurrentBttTotalKoli(): Int {
        val preferences = dataStore.data.map { it[CURRENT_BTT_TOTAL_KOLI] ?: 0 }
        return preferences.first()
    }
    
    /**
     * Set current BTT number being scanned
     */
    suspend fun setCurrentBttNumber(bttNumber: String) {
        dataStore.edit { preferences ->
            preferences[CURRENT_BTT_NUMBER] = bttNumber
        }
    }
    
    /**
     * Get current BTT number
     */
    suspend fun getCurrentBttNumber(): String {
        val preferences = dataStore.data.map { it[CURRENT_BTT_NUMBER] ?: "" }
        return preferences.first()
    }

    companion object {
        @Volatile
        private var INSTANCE: UserPreferences? = null

        fun getInstance(dataStore: DataStore<Preferences>): UserPreferences {
            return INSTANCE ?: synchronized(this) {
                val instance = UserPreferences(dataStore)
                INSTANCE = instance
                instance
            }
        }
    }
}
