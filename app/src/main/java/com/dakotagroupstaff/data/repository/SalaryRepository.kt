package com.dakotagroupstaff.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.dakotagroupstaff.data.Result
import com.dakotagroupstaff.data.remote.response.SalarySlipData
import com.dakotagroupstaff.data.remote.response.SalarySlipsRequest
import com.dakotagroupstaff.data.remote.retrofit.ApiService
import kotlinx.coroutines.Dispatchers
import retrofit2.HttpException

/**
 * Salary Repository
 * Following Clean Architecture pattern
 * Handles:
 * - Fetching salary slips from API
 * - Error handling
 */
class SalaryRepository private constructor(
    private val apiService: ApiService
) {
    
    companion object {
        @Volatile
        private var instance: SalaryRepository? = null
        
        fun getInstance(
            apiService: ApiService
        ): SalaryRepository =
            instance ?: synchronized(this) {
                instance ?: SalaryRepository(apiService)
            }.also { instance = it }
    }
    
    /**
     * Get Salary Slips for employee
     * POST /salary/slips?pt=<pt>
     */
    fun getSalarySlips(
        pt: String,
        nip: String,
        imei: String,
        simId: String
    ): LiveData<Result<List<SalarySlipData>>> = liveData(Dispatchers.IO) {
        emit(Result.Loading)
        
        try {
            val request = SalarySlipsRequest(
                nip = nip,
                imei = imei,
                simId = simId
            )
            
            Log.d("SalaryRepository", "Fetching salary slips: pt=$pt, nip=$nip, imei=$imei, simId=$simId")
            
            val response = apiService.getSalarySlips(pt, request)
            
            Log.d("SalaryRepository", "API Response: success=${response.success}, data size=${response.data?.size ?: 0}")
            
            if (response.success && response.data != null) {
                emit(Result.Success(response.data))
            } else {
                emit(Result.Error(response.getResponseMessage()))
            }
        } catch (e: HttpException) {
            Log.e("SalaryRepository", "HTTP Error: ${e.message()}")
            emit(Result.Error(e.message() ?: "Network error"))
        } catch (e: Exception) {
            Log.e("SalaryRepository", "Error: ${e.message}")
            emit(Result.Error(e.message ?: "An error occurred"))
        }
    }
}
