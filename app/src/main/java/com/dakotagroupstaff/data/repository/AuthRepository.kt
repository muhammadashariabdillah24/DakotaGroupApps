package com.dakotagroupstaff.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.dakotagroupstaff.data.Result
import com.dakotagroupstaff.data.local.model.UserSession
import com.dakotagroupstaff.data.local.preferences.UserPreferences
import com.dakotagroupstaff.data.remote.response.ApiResponse
import com.dakotagroupstaff.data.remote.response.LoginData
import com.dakotagroupstaff.data.remote.retrofit.ApiService
import com.dakotagroupstaff.data.remote.retrofit.LoginRequest
import com.dakotagroupstaff.data.remote.retrofit.LogoutRequest
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import retrofit2.HttpException

class AuthRepository private constructor(
    private val apiService: ApiService,
    private val userPreferences: UserPreferences
) {

    fun login(
        pt: String,
        nip: String,
        deviceId: String,
        serialNumber: String,
        email: String
    ): LiveData<Result<LoginData>> = liveData {
        emit(Result.Loading)
        try {
            Log.d("AuthRepository", "=== LOGIN REQUEST ===")
            Log.d("AuthRepository", "PT: $pt")
            Log.d("AuthRepository", "NIP: $nip")
            Log.d("AuthRepository", "DeviceId: $deviceId")
            Log.d("AuthRepository", "SerialNumber: $serialNumber")
            Log.d("AuthRepository", "Email: $email")
            Log.d("AuthRepository", "Request URL will be: /auth/login?pt=$pt")
            
            val loginRequest = LoginRequest(nip, deviceId, serialNumber, email)
            val response = apiService.login(pt, loginRequest)
            
            Log.d("AuthRepository", "=== LOGIN RESPONSE ===")
            Log.d("AuthRepository", "Success: ${response.success}")
            Log.d("AuthRepository", "Message: ${response.message}")
            Log.d("AuthRepository", "Error: ${response.error?.message}")
            Log.d("AuthRepository", "Data size: ${response.data?.size}")
            Log.d("AuthRepository", "=======================")
            
            if (response.success && !response.data.isNullOrEmpty()) {
                val loginData = response.data.first()
                
                // Save session to DataStore
                val userSession = UserSession(
                    nip = loginData.nip,
                    nama = loginData.nama,
                    atasan1 = loginData.atasan1,
                    atasan2 = loginData.atasan2,
                    pt = pt,
                    imei = deviceId,
                    simId = serialNumber,
                    email = email,
                    isLoggedIn = true,
                    areaKerja = loginData.areaKerja ?: "",
                    taskCode = loginData.taskCode ?: "",
                    taskDetail = loginData.taskDetail ?: "",
                    taskId = loginData.taskId ?: ""
                )
                
                userPreferences.saveSession(userSession)
                
                // Save JWT access token
                userPreferences.saveAccessToken(loginData.accessToken)
                
                // Save refresh token and token expiry
                userPreferences.saveRefreshToken(loginData.refreshToken)
                userPreferences.saveTokenExpiry(loginData.expiresIn)
                
                emit(Result.Success(loginData))
            } else {
                emit(Result.Error(response.getResponseMessage()))
            }
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e("AuthRepository", "=== HTTP EXCEPTION ===")
            Log.e("AuthRepository", "Code: ${e.code()}")
            Log.e("AuthRepository", "Message: ${e.message()}")
            Log.e("AuthRepository", "Error Body: $errorBody")
            Log.e("AuthRepository", "======================")
            
            val errorResponse = try {
                Gson().fromJson(errorBody, ApiResponse::class.java)
            } catch (_: Exception) {
                null
            }
            val errorMessage = errorResponse?.getResponseMessage() 
                ?: "Terjadi kesalahan pada server"
            emit(Result.Error(errorMessage))
        } catch (e: Exception) {
            Log.e("AuthRepository", "=== EXCEPTION ===")
            Log.e("AuthRepository", "Type: ${e.javaClass.simpleName}")
            Log.e("AuthRepository", "Message: ${e.message}")
            Log.e("AuthRepository", "Stack: ", e)
            Log.e("AuthRepository", "=================")
            emit(Result.Error(e.message ?: "Terjadi kesalahan jaringan"))
        }
    }

    fun getSession(): Flow<UserSession> {
        return userPreferences.getSession()
    }

    fun logout(): LiveData<Result<Boolean>> = liveData {
        emit(Result.Loading)
        try {
            val refreshToken = userPreferences.getRefreshToken().first()
            val nip = userPreferences.getNip().first()
            val pt = userPreferences.getPt().first()
            
            if (refreshToken.isNotEmpty() && nip.isNotEmpty()) {
                try {
                    // Call logout API to revoke refresh token
                    val logoutRequest = LogoutRequest(refreshToken, nip)
                    val response = apiService.logout(pt, logoutRequest)
                    
                    Log.d("AuthRepository", "Logout API response: ${response.message}")
                } catch (e: Exception) {
                    // Log but don't fail - still clear local session
                    Log.e("AuthRepository", "Error calling logout API", e)
                }
            }
            
            // Clear local session regardless of API call result
            userPreferences.clearAccessToken()
            userPreferences.logout()
            
            emit(Result.Success(true))
        } catch (e: Exception) {
            Log.e("AuthRepository", "Logout error", e)
            // Still clear local session even if exception occurs
            userPreferences.logout()
            emit(Result.Success(true))
        }
    }

    companion object {
        @Volatile
        private var instance: AuthRepository? = null

        fun getInstance(
            apiService: ApiService,
            userPreferences: UserPreferences
        ): AuthRepository =
            instance ?: synchronized(this) {
                instance ?: AuthRepository(apiService, userPreferences)
            }.also { instance = it }
    }
}
