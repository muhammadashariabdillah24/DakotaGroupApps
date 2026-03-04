package com.dakotagroupstaff.data.repository

import android.util.Log
import com.dakotagroupstaff.data.local.preferences.UserPreferences
import com.dakotagroupstaff.data.remote.response.LetterOfAssignDetail
import com.dakotagroupstaff.data.remote.response.GetLetterOfAssignRequest
import com.dakotagroupstaff.data.remote.response.CheckLocationRequest
import com.dakotagroupstaff.data.remote.response.CheckpointRequest
import com.dakotagroupstaff.data.remote.response.CompleteLetterOfAssignRequest
import com.dakotagroupstaff.data.remote.response.GetMuatRequest
import com.dakotagroupstaff.data.remote.retrofit.ApiService
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
            Log.d(TAG, "=== LETTER OF ASSIGN DEBUG START ===")
            Log.d(TAG, "NIP: $nip")
            Log.d(TAG, "PT: $pt")
            
            // Check if user has access token
            val token = userPreferences.getAccessToken().first()
            if (token.isNullOrEmpty()) {
                Log.e(TAG, "ERROR: No access token found! User may not be logged in.")
                return Result.Error("Anda belum login. Silakan login terlebih dahulu.")
            }
            Log.d(TAG, "Access token exists: ${token.take(20)}...")
            
            val request = GetLetterOfAssignRequest(nip = nip, pt = pt)
            Log.d(TAG, "Sending request: ${request}")
            
            val response = apiService.getLetterOfAssign(request)
            Log.d(TAG, "Response received: success=${response.success}")
            Log.d(TAG, "Response message: ${response.message}")
            Log.d(TAG, "Response data size: ${response.data?.size ?: 0}")
            Log.d(TAG, "Response error: ${response.error}")
            
            if (response.success && response.data != null) {
                Log.d(TAG, "✓ SUCCESS: ${response.data.size} items found")
                if (response.data.isNotEmpty()) {
                    Log.d(TAG, "First item: sID=${response.data[0].sID}, keterangan=${response.data[0].keterangan}")
                }
                Result.Success(response.data)
            } else {
                val errorMsg = response.message ?: response.error?.message ?: "Unknown error"
                Log.e(TAG, "✗ API ERROR: $errorMsg")
                Log.e(TAG, "Full response: $response")
                Result.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "✗ EXCEPTION: ${e.javaClass.simpleName}")
            Log.e(TAG, "Exception message: ${e.message}")
            Log.e(TAG, "Stack trace:", e)
            Result.Error(e.message ?: "Network error")
        } finally {
            Log.d(TAG, "=== LETTER OF ASSIGN DEBUG END ===")
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
                nip = nip,  // Required for sp_AddOpr_T_Unload_BySJ when status is 'I'
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
            
            val request = CompleteLetterOfAssignRequest(sID = sID, pt = pt)
            val response = apiService.completeLetterOfAssign(request)
            
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
    
    /**
     * Get next point information
     */
    suspend fun getNextPoint(sID: String, pt: String): Result<com.dakotagroupstaff.data.remote.response.NextPointData> {
        return try {
            Log.d(TAG, "Getting next point: sID=$sID, pt=$pt")
            
            val response = apiService.getNextPoint(sID, pt)
            
            if (response.success && response.data != null && response.data.isNotEmpty()) {
                Log.d(TAG, "Next point found: ${response.data[0].nextPointNama}")
                Result.Success(response.data[0])
            } else {
                val errorMsg = response.message ?: response.error?.message ?: "No next point found"
                Log.e(TAG, "API error: $errorMsg")
                Result.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception getting next point", e)
            Result.Error(e.message ?: "Network error")
        }
    }
    
    /**
     * Get loading data (muat)
     */
    suspend fun getLoadingData(sID: String, agenID: String, pt: String): Result<List<com.dakotagroupstaff.data.remote.response.LoadingData>> {
        return try {
            Log.d(TAG, "Getting loading data: sID=$sID, agenID=$agenID, pt=$pt")
            
            val request = GetMuatRequest(sID = sID, agenID = agenID, pt = pt)
            val response = apiService.getLoadingData(request)
            
            if (response.success && response.data != null) {
                Log.d(TAG, "Loading data found: ${response.data.size} items")
                Result.Success(response.data)
            } else {
                val errorMsg = response.message ?: response.error?.message ?: "No loading data found"
                Log.e(TAG, "API error: $errorMsg")
                Result.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception getting loading data", e)
            Result.Error(e.message ?: "Network error")
        }
    }

    /**
     * Get unloading data (bongkar)
     */
    suspend fun getUnloadingData(sID: String, agenID: String, pt: String): Result<List<com.dakotagroupstaff.data.remote.response.UnloadingData>> {
        return try {
            Log.d(TAG, "Getting unloading data: sID=$sID, agenID=$agenID, pt=$pt")
            
            val request = com.dakotagroupstaff.data.remote.response.GetBongkarRequest(sID = sID, agenID = agenID, pt = pt)
            val response = apiService.getUnloadingData(request)
            
            if (response.success && response.data != null) {
                Log.d(TAG, "Unloading data found: ${response.data.size} items")
                Result.Success(response.data)
            } else {
                val errorMsg = response.message ?: response.error?.message ?: "No unloading data found"
                Log.e(TAG, "API error: $errorMsg")
                Result.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception getting unloading data", e)
            Result.Error(e.message ?: "Network error")
        }
    }

    /**
     * Get loading detail
     */
    suspend fun getLoadingDetail(sID: String, agenID: String, destId: String, loadHId: String, pt: String): Result<List<com.dakotagroupstaff.data.remote.response.LoadingDetail>> {
        return try {
            Log.d(TAG, "Getting loading detail: sID=$sID, agenID=$agenID, destId=$destId, loadHId=$loadHId, pt=$pt")
            
            val request = com.dakotagroupstaff.data.remote.response.GetLoadingDetailRequest(
                sID = sID, agenID = agenID, destId = destId, loadHId = loadHId, pt = pt
            )
            val response = apiService.getLoadingDetail(request)
            
            if (response.success && response.data != null) {
                Log.d(TAG, "Loading detail found: ${response.data.size} items")
                Result.Success(response.data)
            } else {
                val errorMsg = response.message ?: response.error?.message ?: "No loading detail found"
                Log.e(TAG, "API error: $errorMsg")
                Result.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception getting loading detail", e)
            Result.Error(e.message ?: "Network error")
        }
    }

    /**
     * Get unloading detail
     */
    suspend fun getUnloadingDetail(sID: String, agenID: String, sourceId: String, pt: String): Result<List<com.dakotagroupstaff.data.remote.response.UnloadingDetail>> {
        return try {
            Log.d(TAG, "Getting unloading detail: sID=$sID, agenID=$agenID, sourceId=$sourceId, pt=$pt")
            
            val request = com.dakotagroupstaff.data.remote.response.GetUnloadingDetailRequest(
                sID = sID, agenID = agenID, sourceId = sourceId, pt = pt
            )
            val response = apiService.getUnloadingDetail(request)
            
            if (response.success && response.data != null) {
                Log.d(TAG, "Unloading detail found: ${response.data.size} items")
                Result.Success(response.data)
            } else {
                val errorMsg = response.message ?: response.error?.message ?: "No unloading detail found"
                Log.e(TAG, "API error: $errorMsg")
                Result.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception getting unloading detail", e)
            Result.Error(e.message ?: "Network error")
        }
    }

    /**
     * Get KM data
     */
    suspend fun getKMData(kendID: String, pt: String): Result<String> {
        return try {
            Log.d(TAG, "Getting KM data: kendID=$kendID, pt=$pt")
            
            val request = com.dakotagroupstaff.data.remote.response.KMRequest(kendID = kendID, pt = pt)
            val response = apiService.getKMData(request)
            
            if (response.success && response.data != null) {
                Log.d(TAG, "KM data found: ${response.data.km}")
                Result.Success(response.data.km)
            } else {
                val errorMsg = response.message ?: response.error?.message ?: "No KM data found"
                Log.e(TAG, "API error: $errorMsg")
                Result.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception getting KM data", e)
            Result.Error(e.message ?: "Network error")
        }
    }

    /**
     * Update KM
     */
    suspend fun updateKM(sID: String, agenId: String, urut: String, status: String, km: String, pt: String): Result<String> {
        return try {
            Log.d(TAG, "Updating KM: sID=$sID, agenId=$agenId, urut=$urut, status=$status, km=$km, pt=$pt")
            
            val request = com.dakotagroupstaff.data.remote.response.UpdateKMRequest(
                sID = sID, agenId = agenId, urut = urut, status = status, km = km, pt = pt
            )
            val response = apiService.updateKM(request)
            
            if (response.success && response.data != null) {
                Log.d(TAG, "KM updated: ${response.data.successStatus}")
                Result.Success(response.data.successStatus)
            } else {
                val errorMsg = response.message ?: response.error?.message ?: "Failed to update KM"
                Log.e(TAG, "API error: $errorMsg")
                Result.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception updating KM", e)
            Result.Error(e.message ?: "Network error")
        }
    }

    /**
     * Get alternative route (rute cadangan)
     */
    suspend fun getAlternativeRoute(sID: String, pt: String): Result<List<com.dakotagroupstaff.data.remote.response.AlternativeRouteData>> {
        return try {
            Log.d(TAG, "Getting alternative route: sID=$sID, pt=$pt")
            
            val request = com.dakotagroupstaff.data.remote.response.GetAlternativeRouteRequest(sID = sID, pt = pt)
            val response = apiService.getAlternativeRoute(request)
            
            if (response.success && response.data != null) {
                Log.d(TAG, "Alternative route found: ${response.data.size} items")
                Result.Success(response.data)
            } else {
                val errorMsg = response.message ?: response.error?.message ?: "No alternative route found"
                Log.e(TAG, "API error: $errorMsg")
                Result.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception getting alternative route", e)
            Result.Error(e.message ?: "Network error")
        }
    }

    /**
     * Lock loading (muat)
     */
    suspend fun lockLoading(sID: String, agenID: String, loadHId: String, pt: String): Result<String> {
        return try {
            Log.d(TAG, "Locking loading: sID=$sID, agenID=$agenID, loadHId=$loadHId, pt=$pt")
            
            val request = com.dakotagroupstaff.data.remote.response.LockLoadingRequest(
                sID = sID, agenID = agenID, loadHId = loadHId, pt = pt
            )
            val response = apiService.lockLoading(request)
            
            if (response.success && response.data != null) {
                Log.d(TAG, "Loading locked: ${response.data.successStatus}")
                Result.Success(response.data.successStatus)
            } else {
                val errorMsg = response.message ?: response.error?.message ?: "Failed to lock loading"
                Log.e(TAG, "API error: $errorMsg")
                Result.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception locking loading", e)
            Result.Error(e.message ?: "Network error")
        }
    }

    /**
     * Unlock loading (muat)
     */
    suspend fun unlockLoading(sID: String, agenID: String, loadHId: String, pt: String): Result<String> {
        return try {
            Log.d(TAG, "Unlocking loading: sID=$sID, agenID=$agenID, loadHId=$loadHId, pt=$pt")
            
            val request = com.dakotagroupstaff.data.remote.response.UnlockLoadingRequest(
                sID = sID, agenID = agenID, loadHId = loadHId, pt = pt
            )
            val response = apiService.unlockLoading(request)
            
            if (response.success && response.data != null) {
                Log.d(TAG, "Loading unlocked: ${response.data.successStatus}")
                Result.Success(response.data.successStatus)
            } else {
                val errorMsg = response.message ?: response.error?.message ?: "Failed to unlock loading"
                Log.e(TAG, "API error: $errorMsg")
                Result.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception unlocking loading", e)
            Result.Error(e.message ?: "Network error")
        }
    }

    /**
     * Lock unloading (bongkar)
     */
    suspend fun lockUnloading(unloadId: String, nip: String, pt: String): Result<String> {
        return try {
            Log.d(TAG, "Locking unloading: unloadId=$unloadId, nip=$nip, pt=$pt")
            
            val request = com.dakotagroupstaff.data.remote.response.LockUnloadingRequest(
                unloadId = unloadId, nip = nip, pt = pt
            )
            val response = apiService.lockUnloading(request)
            
            if (response.success && response.data != null) {
                Log.d(TAG, "Unloading locked: ${response.data.successStatus}")
                Result.Success(response.data.successStatus)
            } else {
                val errorMsg = response.message ?: response.error?.message ?: "Failed to lock unloading"
                Log.e(TAG, "API error: $errorMsg")
                Result.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception locking unloading", e)
            Result.Error(e.message ?: "Network error")
        }
    }

    /**
     * Get less items (barang kurang)
     */
    suspend fun getLessItems(nip: String, pt: String): Result<List<com.dakotagroupstaff.data.remote.response.LessItemsData>> {
        return try {
            Log.d(TAG, "Getting less items: nip=$nip, pt=$pt")
            
            val request = com.dakotagroupstaff.data.remote.response.GetLessItemsRequest(nip = nip, pt = pt)
            val response = apiService.getLessItems(request)
            
            if (response.success && response.data != null) {
                Log.d(TAG, "Less items found: ${response.data.size} items")
                Result.Success(response.data)
            } else {
                val errorMsg = response.message ?: response.error?.message ?: "No less items found"
                Log.e(TAG, "API error: $errorMsg")
                Result.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception getting less items", e)
            Result.Error(e.message ?: "Network error")
        }
    }

    /**
     * Upload photo for checkpoint
     */
    suspend fun uploadCheckpointPhoto(imageBase64: String, pt: String): Result<String> {
        return try {
            Log.d(TAG, "Uploading checkpoint photo: pt=$pt")
            
            val request = com.dakotagroupstaff.data.remote.response.LetterOfAssignUploadPhotoRequest(
                base64Image = imageBase64, pt = pt
            )
            val response = apiService.uploadCheckpointPhoto(request)
            
            if (response.success && response.data != null) {
                Log.d(TAG, "Photo uploaded: ${response.data.url}")
                Result.Success(response.data.url)
            } else {
                val errorMsg = response.message ?: response.error?.message ?: "Failed to upload photo"
                Log.e(TAG, "API error: $errorMsg")
                Result.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception uploading photo", e)
            Result.Error(e.message ?: "Network error")
        }
    }
}