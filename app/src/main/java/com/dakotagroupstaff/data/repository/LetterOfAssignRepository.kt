package com.dakotagroupstaff.data.repository

import android.util.Log
import com.dakotagroupstaff.data.local.preferences.UserPreferences
import com.dakotagroupstaff.data.remote.response.LetterOfAssignDetail
import com.dakotagroupstaff.data.remote.retrofit.ApiService
import com.dakotagroupstaff.data.remote.retrofit.CheckLocationRequest
import com.dakotagroupstaff.data.remote.retrofit.CheckpointRequest
import com.dakotagroupstaff.data.remote.retrofit.GetMuatRequest
import com.dakotagroupstaff.data.Result
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repository for Letter of Assign (PT DBS and PT DLB only)
 */
class LetterOfAssignRepository(
    private val apiService: ApiService,
    private val userPreferences: UserPreferences
) {
    
    companion object {
        private const val TAG = "LetterOfAssignRepo"
    }
    
    /**
     * Get PT from user preferences
     */
    suspend fun getPt(): String? {
        return try {
            userPreferences.getPt().first()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting PT", e)
            null
        }
    }
    
    /**
     * Get letter of assign for driver
     */
    suspend fun getLetterOfAssign(nip: String, pt: String): Result<List<LetterOfAssignDetail>> {
        return try {
            Log.d(TAG, "Getting letter of assign for nip=$nip, pt=$pt")
            
            val response = apiService.getLetterOfAssign(nip, pt)
            
            if (response.success && response.data != null) {
                Log.d(TAG, "Success: ${response.data.size} items")
                Result.Success(response.data)
            } else {
                val errorMsg = response.message ?: response.error?.message ?: "Unknown error"
                Log.e(TAG, "API error: $errorMsg")
                Result.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception getting letter of assign", e)
            Result.Error(e.message ?: "Network error")
        }
    }
    
    /**
     * Update location to server
     */
    suspend fun updateLocation(sID: String, lat: String, lon: String, pt: String): Result<String> {
        return try {
            Log.d(TAG, "Updating location: sID=$sID, lat=$lat, lon=$lon, pt=$pt")
            
            val request = CheckLocationRequest(
                sID = sID,
                lat = lat,
                lon = lon,
                pt = pt
            )
            
            val response = apiService.checkLocation(request)
            
            if (response.success && response.data != null) {
                Log.d(TAG, "Location updated: ${response.data.successStatus}")
                Result.Success(response.data.successStatus)
            } else {
                val errorMsg = response.message ?: response.error?.message ?: "Unknown error"
                Log.e(TAG, "API error: $errorMsg")
                Result.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception updating location", e)
            Result.Error(e.message ?: "Network error")
        }
    }
    
    /**
     * Submit checkpoint check-in
     */
    suspend fun submitCheckpoint(
        sID: String,
        agenID: String,
        km: String,
        lat: String,
        lon: String,
        urlpic: String,
        nip: String,
        urut: String,
        pt: String
    ): Result<String> {
        return try {
            Log.d(TAG, "Submitting checkpoint: sID=$sID, agenID=$agenID, urut=$urut")
            
            // Format current date
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val currentDate = dateFormat.format(Date())
            
            val request = CheckpointRequest(
                sID = sID,
                agenID = agenID,
                tglUpdate = currentDate,
                km = km,
                lat = lat,
                lon = lon,
                urlpic = urlpic,
                nip = nip,
                urut = urut,
                pt = pt
            )
            
            val response = apiService.submitCheckpoint(request)
            
            if (response.success && response.data != null) {
                Log.d(TAG, "Checkpoint submitted: ${response.data.successStatus}")
                Result.Success(response.data.successStatus)
            } else {
                val errorMsg = response.message ?: response.error?.message ?: "Unknown error"
                Log.e(TAG, "API error: $errorMsg")
                Result.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception submitting checkpoint", e)
            Result.Error(e.message ?: "Network error")
        }
    }
    
    /**
     * Complete assignment
     */
    suspend fun completeAssignment(sID: String, pt: String): Result<String> {
        return try {
            Log.d(TAG, "Completing assignment: sID=$sID, pt=$pt")
            
            val response = apiService.completeLetterOfAssign(sID, pt)
            
            if (response.success && response.data != null) {
                Log.d(TAG, "Assignment completed: ${response.data.successStatus}")
                Result.Success(response.data.successStatus)
            } else {
                val errorMsg = response.message ?: response.error?.message ?: "Unknown error"
                Log.e(TAG, "API error: $errorMsg")
                Result.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception completing assignment", e)
            Result.Error(e.message ?: "Network error")
        }
    }
}