package com.dakotagroupstaff.data.repository

import com.dakotagroupstaff.data.local.preferences.UserPreferences
import com.dakotagroupstaff.data.remote.response.CheckBarcodeData
import com.dakotagroupstaff.data.remote.retrofit.ApiService
import com.dakotagroupstaff.data.remote.retrofit.CheckBarcodeRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

/**
 * Repository for Loper (Delivery) operations
 */
class LoperRepository(
    private val apiService: ApiService,
    private val userPreferences: UserPreferences
) {

    /**
     * Check barcode BTT against server
     */
    fun checkBarcode(barcode: String): Flow<com.dakotagroupstaff.data.Result<CheckBarcodeData>> = flow {
        emit(com.dakotagroupstaff.data.Result.Loading)
        try {
            val pt = userPreferences.getPt().first()
            val response = apiService.checkBarcodeBTT(
                pt,
                CheckBarcodeRequest(barcode)
            )
            
            if (response.success && response.data != null) {
                emit(com.dakotagroupstaff.data.Result.Success(response.data))
            } else {
                emit(com.dakotagroupstaff.data.Result.Error(response.message ?: "Unknown error"))
            }
        } catch (e: Exception) {
            emit(com.dakotagroupstaff.data.Result.Error(e.message ?: "Network error"))
        }
    }

    companion object {
        @Volatile
        private var instance: LoperRepository? = null

        fun getInstance(apiService: ApiService, userPreferences: UserPreferences): LoperRepository =
            instance ?: synchronized(this) {
                instance ?: LoperRepository(apiService, userPreferences)
            }.also { instance = it }
    }
}
