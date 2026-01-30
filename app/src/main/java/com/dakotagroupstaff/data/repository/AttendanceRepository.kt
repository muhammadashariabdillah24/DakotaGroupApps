package com.dakotagroupstaff.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.dakotagroupstaff.data.Result
import com.dakotagroupstaff.data.local.dao.AgentLocationDao
import com.dakotagroupstaff.data.local.dao.AttendanceHistoryDao
import com.dakotagroupstaff.data.local.entity.AgentLocationEntity
import com.dakotagroupstaff.data.local.entity.AttendanceHistoryEntity
import com.dakotagroupstaff.data.mapper.AgentLocationMapper
import com.dakotagroupstaff.data.remote.response.ApiResponse
import com.dakotagroupstaff.data.remote.response.SubmitAttendanceData
import com.dakotagroupstaff.data.remote.response.SubmitAttendanceRequest
import com.dakotagroupstaff.data.remote.retrofit.ApiService
import com.dakotagroupstaff.data.remote.retrofit.AttendanceRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Attendance Repository
 * Following Clean Architecture from HistoryApp & MyQuranApp
 * Handles:
 * - Agent locations cache
 * - GPS distance calculation
 * - Attendance submission
 * - Attendance history
 */
class AttendanceRepository private constructor(
    private val apiService: ApiService,
    private val agentLocationDao: AgentLocationDao,
    private val attendanceHistoryDao: AttendanceHistoryDao
) {
    
    companion object {
        @Volatile
        private var instance: AttendanceRepository? = null
        
        fun getInstance(
            apiService: ApiService,
            agentLocationDao: AgentLocationDao,
            attendanceHistoryDao: AttendanceHistoryDao
        ): AttendanceRepository =
            instance ?: synchronized(this) {
                instance ?: AttendanceRepository(
                    apiService,
                    agentLocationDao,
                    attendanceHistoryDao
                )
            }.also { instance = it }
    }
    
    /**
     * Get Agent Locations (with cache-first strategy)
     * Updates cache every 30 days
     */
    fun getAgentLocations(pt: String): LiveData<Result<List<AgentLocationEntity>>> = liveData {
        emit(Result.Loading)
        
        try {
            // 1. Check local cache first
            val cachedLocations = agentLocationDao.getAllLocationsSync()
            
            Log.d("AttendanceRepository", "Found ${cachedLocations.size} cached locations")
            
            if (cachedLocations.isNotEmpty() && cachedLocations.first().isValid()) {
                // Use cached data if valid
                Log.d("AttendanceRepository", "Using valid cached data")
                emit(Result.Success(cachedLocations))
            } else {
                // 2. Fetch from network
                Log.d("AttendanceRepository", "Fetching agent locations from API for pt=$pt")
                val response = apiService.getAgentLocations(pt)
                
                Log.d("AttendanceRepository", "API Response: success=${response.success}, data size=${response.data?.size ?: 0}")
                
                if (response.success && response.data != null) {
                    // 3. Map to entities and save to cache
                    val entities = response.data.mapNotNull { 
                        AgentLocationMapper.fromResponse(it)
                    }
                    
                    if (entities.isEmpty()) {
                        emit(Result.Error("Tidak ada lokasi agen dengan GPS yang valid"))
                        return@liveData
                    }
                    
                    // Clear old cache and insert new data
                    agentLocationDao.clearAll()
                    agentLocationDao.insertAll(entities)
                    
                    Log.d("AttendanceRepository", "Cached ${entities.size} valid agent locations")
                    emit(Result.Success(entities))
                } else {
                    // Fallback to cached data if API fails
                    if (cachedLocations.isNotEmpty()) {
                        emit(Result.Success(cachedLocations))
                    } else {
                        emit(Result.Error(response.getResponseMessage()))
                    }
                }
            }
        } catch (e: HttpException) {
            Log.e("AttendanceRepository", "HTTP Error: ${e.message()}")
            
            // Try to use cached data
            val cachedLocations = agentLocationDao.getAllLocationsSync()
            if (cachedLocations.isNotEmpty()) {
                emit(Result.Success(cachedLocations))
            } else {
                emit(Result.Error(e.message() ?: "Network error"))
            }
        } catch (e: Exception) {
            Log.e("AttendanceRepository", "Error: ${e.message}")
            
            // Try to use cached data
            val cachedLocations = agentLocationDao.getAllLocationsSync()
            if (cachedLocations.isNotEmpty()) {
                emit(Result.Success(cachedLocations))
            } else {
                emit(Result.Error(e.message ?: "An error occurred"))
            }
        }
    }
    
    /**
     * Find nearest agent location from user coordinates
     */
    suspend fun findNearestAgent(
        userLat: Double,
        userLon: Double
    ): Result<Pair<AgentLocationEntity, Double>> = withContext(Dispatchers.IO) {
        try {
            val locations = agentLocationDao.getAllLocationsSync()
            
            if (locations.isEmpty()) {
                return@withContext Result.Error("No agent locations available")
            }
            
            // Calculate distances and find nearest
            val locationsWithDistance = locations.map { agent ->
                val distance = agent.distanceFrom(userLat, userLon)
                Pair(agent, distance)
            }
            
            val nearest = locationsWithDistance.minByOrNull { it.second }
            
            if (nearest != null) {
                Result.Success(nearest)
            } else {
                Result.Error("Could not find nearest agent")
            }
        } catch (e: Exception) {
            Log.e("AttendanceRepository", "Error finding nearest agent: ${e.message}")
            Result.Error(e.message ?: "Error finding nearest agent")
        }
    }
    
    /**
     * Submit Attendance (Check In/Out)
     * Returns ApiResponse directly from backend
     */
    fun submitAttendance(
        pt: String,
        nip: String,
        kodeCabang: String,
        latitude: Double,
        longitude: Double,
        schedule: String, // "M" or "K"
        deviceId: String? = null,
        serialNumber: String? = null
    ): LiveData<Result<String>> = liveData {
        emit(Result.Loading)
        
        try {
            val request = SubmitAttendanceRequest(
                nip = nip,
                kodeCabang = kodeCabang,
                latitude = latitude.toString(),
                longitude = longitude.toString(),
                schedule = schedule.uppercase(),
                deviceId = deviceId,
                serialNumber = serialNumber
            )
            
            Log.d("AttendanceRepository", "Submitting attendance: pt=$pt, nip=$nip, schedule=${schedule.uppercase()}")
            
            // Get response directly as ApiResponse object (no manual parsing needed)
            val response = apiService.submitAttendance(pt, request)
            
            Log.d("AttendanceRepository", "Response: success=${response.success}, message=${response.getResponseMessage()}")
            
            if (response.success) {
                // Refresh attendance history after successful submission
                refreshAttendanceHistory(pt, nip)
                emit(Result.Success(response.getResponseMessage()))
            } else {
                emit(Result.Error(response.getResponseMessage()))
            }
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e("AttendanceRepository", "HTTP Error ${e.code()}: $errorBody")
            emit(Result.Error(errorBody ?: e.message() ?: "Network error"))
        } catch (e: Exception) {
            Log.e("AttendanceRepository", "Error submitting attendance: ${e.message}", e)
            emit(Result.Error(e.message ?: "An error occurred"))
        }
    }
    
    /**
     * Get Attendance History (with cache)
     */
    fun getAttendanceHistory(pt: String, nip: String): LiveData<Result<List<AttendanceHistoryEntity>>> = liveData {
        emit(Result.Loading)
        
        try {
            // 1. Emit cached data first
            val cachedData = attendanceHistoryDao.getAttendanceHistorySync(nip)
            if (cachedData.isNotEmpty()) {
                emit(Result.Success(cachedData))
            }
            
            // 2. Fetch fresh data from network
            val response = apiService.getAttendanceHistory(pt, AttendanceRequest(nip))
            
            if (response.success && response.data != null) {
                // 3. Map to entities
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val entities = response.data.map { data ->
                    AttendanceHistoryEntity(
                        absId = data.absId,
                        absNip = data.absNip,
                        absTanggal = try {
                            dateFormat.parse(data.absTanggal)?.time ?: System.currentTimeMillis()
                        } catch (e: Exception) {
                            System.currentTimeMillis()
                        },
                        absSchedule = data.absSchedule,
                        absInTime = data.absInTime,
                        absInLocation = data.absInLocation,
                        absOutTime = data.absOutTime,
                        absOutLocation = data.absOutLocation,
                        absStatus = data.absStatus,
                        keterangan = data.keterangan // Map KETERANGAN from API
                    )
                }
                
                // 4. Update cache
                attendanceHistoryDao.insertAll(entities)
                
                // 5. Emit fresh data
                emit(Result.Success(entities))
            } else {
                // Fallback to cached data
                if (cachedData.isEmpty()) {
                    emit(Result.Error(response.getResponseMessage()))
                }
            }
        } catch (e: HttpException) {
            Log.e("AttendanceRepository", "HTTP Error: ${e.message()}")
            
            // Fallback to cached data
            val cachedData = attendanceHistoryDao.getAttendanceHistorySync(nip)
            if (cachedData.isEmpty()) {
                emit(Result.Error(e.message() ?: "Network error"))
            }
        } catch (e: Exception) {
            Log.e("AttendanceRepository", "Error: ${e.message}")
            
            // Fallback to cached data
            val cachedData = attendanceHistoryDao.getAttendanceHistorySync(nip)
            if (cachedData.isEmpty()) {
                emit(Result.Error(e.message ?: "An error occurred"))
            }
        }
    }
    
    /**
     * Get Attendance History by Date Range (Flow)
     */
    fun getAttendanceByDateRange(
        nip: String,
        startDate: Long,
        endDate: Long
    ): Flow<List<AttendanceHistoryEntity>> {
        return attendanceHistoryDao.getAttendanceHistoryByDateRange(nip, startDate, endDate)
    }
    
    /**
     * Refresh attendance history from server
     */
    private suspend fun refreshAttendanceHistory(pt: String, nip: String) {
        try {
            val response = apiService.getAttendanceHistory(pt, AttendanceRequest(nip))
            if (response.success && response.data != null) {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val entities = response.data.map { data ->
                    AttendanceHistoryEntity(
                        absId = data.absId,
                        absNip = data.absNip,
                        absTanggal = try {
                            dateFormat.parse(data.absTanggal)?.time ?: System.currentTimeMillis()
                        } catch (e: Exception) {
                            System.currentTimeMillis()
                        },
                        absSchedule = data.absSchedule,
                        absInTime = data.absInTime,
                        absInLocation = data.absInLocation,
                        absOutTime = data.absOutTime,
                        absOutLocation = data.absOutLocation,
                        absStatus = data.absStatus,
                        keterangan = data.keterangan // Map KETERANGAN from API
                    )
                }
                attendanceHistoryDao.insertAll(entities)
            }
        } catch (e: Exception) {
            Log.e("AttendanceRepository", "Error refreshing attendance: ${e.message}")
        }
    }
}
