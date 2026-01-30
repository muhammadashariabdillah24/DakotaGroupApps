package com.dakotagroupstaff.ui.kepegawaian.leave

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dakotagroupstaff.data.Result
import com.dakotagroupstaff.data.local.entity.LeaveBalanceEntity
import com.dakotagroupstaff.data.local.entity.LeaveDetailsEntity
import com.dakotagroupstaff.data.remote.response.LeaveSubmissionData
import com.dakotagroupstaff.data.remote.response.LeaveType
import com.dakotagroupstaff.data.remote.retrofit.LeaveSubmissionRequest
import com.dakotagroupstaff.data.repository.LeaveRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class LeaveViewModel(
    private val leaveRepository: LeaveRepository
) : ViewModel() {
    
    private val _leaveBalance = MutableLiveData<Result<LeaveBalanceEntity>>()
    val leaveBalance: LiveData<Result<LeaveBalanceEntity>> = _leaveBalance
    
    private val _leaveDetails = MutableLiveData<Result<List<LeaveDetailsEntity>>>()
    val leaveDetails: LiveData<Result<List<LeaveDetailsEntity>>> = _leaveDetails
    
    private val _submitResult = MutableLiveData<Result<LeaveSubmissionData>>()
    val submitResult: LiveData<Result<LeaveSubmissionData>> = _submitResult
    
    private val _selectedLeaveType = MutableLiveData<LeaveType?>()
    val selectedLeaveType: LiveData<LeaveType?> = _selectedLeaveType
    
    private val _startDate = MutableLiveData<Date?>()
    val startDate: LiveData<Date?> = _startDate
    
    private val _endDate = MutableLiveData<Date?>()
    val endDate: LiveData<Date?> = _endDate
    
    private val _description = MutableLiveData<String>()
    val description: LiveData<String> = _description
    
    private val _superAtasanList = MutableLiveData<Result<List<com.dakotagroupstaff.data.remote.response.SuperAtasanData>>>()
    val superAtasanList: LiveData<Result<List<com.dakotagroupstaff.data.remote.response.SuperAtasanData>>> = _superAtasanList
    
    /**
     * Get leave balance from repository
     */
    fun getLeaveBalance(pt: String, nip: String, tahun: String, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            leaveRepository.getLeaveBalance(pt, nip, tahun, forceRefresh).collect {
                _leaveBalance.postValue(it)
            }
        }
    }
    
    /**
     * Get leave details/history from repository
     */
    fun getLeaveDetails(pt: String, nip: String, tahun: String, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            leaveRepository.getLeaveDetails(pt, nip, tahun, forceRefresh).collect {
                _leaveDetails.postValue(it)
            }
        }
    }
    
    /**
     * Submit leave request
     */
    fun submitLeaveRequest(
        pt: String,
        nip: String,
        atasan1: String,
        atasan2: String,
        leaveBalance: Int
    ) {
        val leaveType = _selectedLeaveType.value
        val startDate = _startDate.value
        val endDate = _endDate.value
        val description = _description.value
        
        // Validation
        if (leaveType == null) {
            _submitResult.postValue(Result.Error("Pilih jenis cuti/izin"))
            return
        }
        
        if (description.isNullOrBlank()) {
            _submitResult.postValue(Result.Error("Masukkan keterangan"))
            return
        }
        
        if (atasan1.isBlank()) {
            _submitResult.postValue(Result.Error("Pilih Atasan 1"))
            return
        }
        
        if (atasan2.isBlank()) {
            _submitResult.postValue(Result.Error("Pilih Atasan 2"))
            return
        }
        
        // Check if leave type requires date period
        val requiresDatePeriod = leaveType.code !in listOf("DT", "PC", "MP")
        
        // For types that require date period, validate dates
        if (requiresDatePeriod) {
            if (startDate == null) {
                _submitResult.postValue(Result.Error("Pilih tanggal mulai"))
                return
            }
            
            if (endDate == null) {
                _submitResult.postValue(Result.Error("Pilih tanggal akhir"))
                return
            }
        }
        
        // Format dates - use current date for types that don't require date period
        val dateFormat = SimpleDateFormat("M/d/yyyy", Locale.US)
        val currentDate = Date()
        val tgla = dateFormat.format(startDate ?: currentDate)
        val tgle = dateFormat.format(endDate ?: currentDate)
        
        // Determine if leave should be deducted from balance or salary
        val shouldDeductFromLeave = leaveBalance > 0
        
        val request = LeaveSubmissionRequest(
            nip = nip,
            tgla = tgla,
            tgle = tgle,
            status = leaveType.code,
            keterangan = description,
            atasan1 = atasan1,
            atasan2 = atasan2,
            pgaji = if (shouldDeductFromLeave) "N" else "Y",
            pcuti = if (shouldDeductFromLeave) "Y" else "N"
        )
        
        viewModelScope.launch {
            _submitResult.postValue(Result.Loading)
            val result = leaveRepository.submitLeaveRequest(pt, request)
            _submitResult.postValue(result)
        }
    }
    
    /**
     * Select leave type
     */
    fun selectLeaveType(leaveType: LeaveType) {
        _selectedLeaveType.value = leaveType
    }
    
    /**
     * Set start date
     */
    fun setStartDate(date: Date) {
        _startDate.value = date
        
        // Reset end date if it's before start date
        val endDate = _endDate.value
        if (endDate != null && endDate.before(date)) {
            _endDate.value = null
        }
    }
    
    /**
     * Set end date
     */
    fun setEndDate(date: Date) {
        _endDate.value = date
    }
    
    /**
     * Set description
     */
    fun setDescription(text: String) {
        _description.value = text
    }
    
    /**
     * Get Super Atasan list from repository
     */
    fun getSuperAtasan(pt: String) {
        viewModelScope.launch {
            _superAtasanList.postValue(Result.Loading)
            val result = leaveRepository.getSuperAtasan(pt)
            _superAtasanList.postValue(result)
        }
    }
    
    /**
     * Reset form
     */
    fun resetForm() {
        _selectedLeaveType.value = null
        _startDate.value = null
        _endDate.value = null
        _description.value = ""
        _submitResult.value = Result.Loading
    }
    
    /**
     * Get minimum date for date picker based on leave type
     */
    fun getMinimumDate(): Date {
        return when (_selectedLeaveType.value) {
            LeaveType.CUTI, LeaveType.IZIN -> Date() // Today onwards
            else -> Date(0) // Any date for sick leave, etc.
        }
    }
    
    /**
     * Get maximum date for date picker based on leave type
     */
    fun getMaximumDate(): Date? {
        return when (_selectedLeaveType.value) {
            LeaveType.SAKIT -> Date() // Today or earlier for sick leave
            else -> null // No limit for other types
        }
    }
}
