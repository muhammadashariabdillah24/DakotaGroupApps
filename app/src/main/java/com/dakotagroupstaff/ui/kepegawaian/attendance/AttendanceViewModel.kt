package com.dakotagroupstaff.ui.kepegawaian.attendance

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.dakotagroupstaff.data.Result
import com.dakotagroupstaff.data.local.entity.AgentLocationEntity
import com.dakotagroupstaff.data.local.entity.AttendanceHistoryEntity
import com.dakotagroupstaff.data.repository.AttendanceRepository
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Attendance ViewModel
 * Following MVVM pattern from HistoryApp & MyQuranApp
 */
class AttendanceViewModel(
    private val attendanceRepository: AttendanceRepository
) : ViewModel() {
    
    // Agent Locations State
    private val _agentLocations = MutableLiveData<Result<List<AgentLocationEntity>>>()
    val agentLocations: LiveData<Result<List<AgentLocationEntity>>> = _agentLocations
    
    // Nearest Agent State
    private val _nearestAgent = MutableLiveData<Pair<AgentLocationEntity, Double>?>()
    val nearestAgent: LiveData<Pair<AgentLocationEntity, Double>?> = _nearestAgent
    
    // User Location State
    private val _userLocation = MutableLiveData<Pair<Double, Double>?>()
    val userLocation: LiveData<Pair<Double, Double>?> = _userLocation
    
    // Attendance Submission State
    private val _submitResult = MutableLiveData<Result<String>?>()
    val submitResult: LiveData<Result<String>?> = _submitResult
    
    // Attendance History State
    private val _attendanceHistory = MutableLiveData<Result<List<AttendanceHistoryEntity>>>()
    val attendanceHistory: LiveData<Result<List<AttendanceHistoryEntity>>> = _attendanceHistory
    
    // Loading state for location checking
    private val _isCheckingLocation = MutableLiveData(false)
    val isCheckingLocation: LiveData<Boolean> = _isCheckingLocation
    
    // Error message
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    /**
     * Load agent locations from cache or server
     */
    fun loadAgentLocations(pt: String) {
        viewModelScope.launch {
            attendanceRepository.getAgentLocations(pt).observeForever { result ->
                _agentLocations.value = result
            }
        }
    }
    
    /**
     * Update user location and find nearest agent
     */
    fun updateUserLocation(latitude: Double, longitude: Double, pt: String) {
        _userLocation.value = Pair(latitude, longitude)
        _isCheckingLocation.value = true
        
        viewModelScope.launch {
            // First, ensure agent locations are loaded
            if (_agentLocations.value !is Result.Success) {
                loadAgentLocations(pt)
                // Wait a bit for locations to load
                kotlinx.coroutines.delay(500)
            }
            
            // Find nearest agent
            val result = attendanceRepository.findNearestAgent(latitude, longitude)
            
            when (result) {
                is Result.Success -> {
                    _nearestAgent.value = result.data
                    _errorMessage.value = null
                }
                is Result.Error -> {
                    _nearestAgent.value = null
                    _errorMessage.value = result.message
                }
                is Result.Loading -> {
                    // No action
                }
            }
            
            _isCheckingLocation.value = false
        }
    }
    
    /**
     * Check if user is within range of any agent
     */
    fun isWithinRange(): Boolean {
        val nearest = _nearestAgent.value ?: return false
        val (agent, distance) = nearest
        val rangeMeters = agent.range.toDoubleOrNull() ?: 30.0
        
        return distance <= rangeMeters
    }
    
    /**
     * Get distance to nearest agent in meters
     */
    fun getDistanceToNearestAgent(): Double? {
        return _nearestAgent.value?.second
    }
    
    /**
     * Submit attendance (Check In or Check Out)
     * @param schedule "M" for Masuk (Check In), "K" for Keluar (Check Out)
     */
    fun submitAttendance(
        pt: String,
        nip: String,
        schedule: String,
        deviceId: String? = null,
        serialNumber: String? = null
    ) {
        val location = _userLocation.value
        val nearest = _nearestAgent.value
        
        // Comprehensive validation before submission
        if (location == null) {
            _errorMessage.value = "Lokasi tidak tersedia. Mohon aktifkan GPS"
            _submitResult.value = Result.Error("Lokasi GPS tidak tersedia")
            return
        }
        
        if (nearest == null) {
            _errorMessage.value = "Lokasi cabang/agen tidak ditemukan"
            _submitResult.value = Result.Error("Data lokasi cabang tidak tersedia")
            return
        }
        
        val (agent, distance) = nearest
        val rangeMeters = agent.range.toDoubleOrNull() ?: 30.0
        
        if (distance > rangeMeters) {
            _errorMessage.value = "Anda berada di luar jangkauan cabang/agen (${distance.toInt()}m dari ${agent.namaAgen})"
            return
        }
        
        // Get MD5 code from agent (CRITICAL: Backend validates against Agen_md5)
        // Check if MD5 code is blank before proceeding
        if (agent.md5Code.isBlank()) {
            _errorMessage.value = "Kode cabang tidak valid. Data lokasi perlu diperbarui"
            _submitResult.value = Result.Error("Kode cabang tidak valid")
            return
        }
        val kodeCabang = agent.md5Code
        
        viewModelScope.launch {
            attendanceRepository.submitAttendance(
                pt = pt,
                nip = nip,
                kodeCabang = kodeCabang,
                latitude = location.first,
                longitude = location.second,
                schedule = schedule,
                deviceId = deviceId,
                serialNumber = serialNumber
            ).observeForever { result ->
                _submitResult.value = result
            }
        }
    }
    
    /**
     * Load attendance history
     */
    fun loadAttendanceHistory(pt: String, nip: String) {
        viewModelScope.launch {
            attendanceRepository.getAttendanceHistory(pt, nip).observeForever { result ->
                _attendanceHistory.value = result
            }
        }
    }
    
    /**
     * Get attendance history for specific month
     */
    fun getAttendanceForMonth(nip: String, year: Int, month: Int): LiveData<List<AttendanceHistoryEntity>> {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1, 0, 0, 0)
        val startDate = calendar.timeInMillis
        
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endDate = calendar.timeInMillis
        
        return attendanceRepository.getAttendanceByDateRange(nip, startDate, endDate).asLiveData()
    }
    
    /**
     * Get attendance status for a specific date
     * Returns: "complete" (both M & K), "partial" (only M), "absent" (no record)
     * Based on KETERANGAN field from API:
     * - Green: Both M (Masuk) and K (Keluar) exist on the same date
     * - Yellow: Only M (Masuk) exists
     * - Red: No attendance record
     */
    fun getAttendanceStatus(date: Long): String {
        val history = _attendanceHistory.value
        
        if (history !is Result.Success) return "unknown"
        
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = date
        val targetDate = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        // Find all attendance records for the target date
        val recordsForDate = history.data.filter { attendance ->
            val attendanceCalendar = Calendar.getInstance()
            attendanceCalendar.timeInMillis = attendance.absTanggal
            attendanceCalendar.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis == targetDate
        }
        
        // No records for this date
        if (recordsForDate.isEmpty()) {
            return "absent"
        }
        
        // Check KETERANGAN values
        val keteranganValues = recordsForDate.map { it.keterangan.uppercase() }
        
        return when {
            // Both M (Masuk) and K (Keluar) exist -> Complete (Green)
            keteranganValues.contains("M") && keteranganValues.contains("K") -> "complete"
            // Only M (Masuk) exists -> Partial (Yellow)
            keteranganValues.contains("M") -> "partial"
            // Other cases (no M or K) -> Absent (Red)
            else -> "absent"
        }
    }
    
    /**
     * Reset submit result (for handling one-time events)
     */
    fun resetSubmitResult() {
        _submitResult.value = null
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * Refresh location check
     */
    fun refreshLocationCheck(pt: String) {
        val location = _userLocation.value
        if (location != null) {
            updateUserLocation(location.first, location.second, pt)
        } else {
            _errorMessage.value = "Lokasi tidak tersedia. Mohon aktifkan GPS"
        }
    }
}
