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
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
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

    suspend fun logout() {
        userPreferences.logout()
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
