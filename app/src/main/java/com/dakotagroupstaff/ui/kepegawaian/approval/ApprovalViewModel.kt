package com.dakotagroupstaff.ui.kepegawaian.approval

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dakotagroupstaff.data.Result
import com.dakotagroupstaff.data.remote.response.PendingApprovalData
import com.dakotagroupstaff.data.remote.response.SupervisorCheckData
import com.dakotagroupstaff.data.repository.LeaveRepository
import kotlinx.coroutines.launch

class ApprovalViewModel(
    private val leaveRepository: LeaveRepository
) : ViewModel() {
    
    private val _supervisorStatus = MutableLiveData<Result<SupervisorCheckData>>()
    val supervisorStatus: LiveData<Result<SupervisorCheckData>> = _supervisorStatus
    
    private val _pendingApprovals = MutableLiveData<Result<List<PendingApprovalData>>>()
    val pendingApprovals: LiveData<Result<List<PendingApprovalData>>> = _pendingApprovals
    
    private val _approvalResult = MutableLiveData<Result<com.dakotagroupstaff.data.remote.response.LeaveApprovalData>>()
    val approvalResult: LiveData<Result<com.dakotagroupstaff.data.remote.response.LeaveApprovalData>> = _approvalResult
    
    private val _rejectionResult = MutableLiveData<Result<com.dakotagroupstaff.data.remote.response.LeaveRejectionData>>()
    val rejectionResult: LiveData<Result<com.dakotagroupstaff.data.remote.response.LeaveRejectionData>> = _rejectionResult
    
    /**
     * Check if the current user is a supervisor
     */
    fun checkSupervisorStatus(pt: String, nip: String) {
        viewModelScope.launch {
            _supervisorStatus.postValue(Result.Loading)
            val result = leaveRepository.checkSupervisor(pt, nip)
            _supervisorStatus.postValue(result)
        }
    }
    
    /**
     * Get pending approval list for supervisor
     * Migrated from OldSystemApproval
     */
    fun getPendingApprovals(pt: String, nip: String) {
        viewModelScope.launch {
            _pendingApprovals.postValue(Result.Loading)
            val result = leaveRepository.getPendingApprovals(pt, nip)
            _pendingApprovals.postValue(result)
        }
    }
    
    /**
     * Approve leave request
     * Automatically detects if ATASAN1 == ATASAN2 and calls appropriate API
     * @param potongGaji - Deduction from salary (Y/N)
     * @param potongCuti - Deduction from leave balance (Y/N)
     * @param dispensasi - Dispensation/No deduction (Y/N)
     */
    fun approveLeaveRequest(
        pt: String,
        nipAtasan: String,
        leaveId: String,
        atasan1Nip: String,
        atasan2Nip: String,
        approvalValue: String = "Y",
        potongGaji: String,
        potongCuti: String,
        dispensasi: String
    ) {
        viewModelScope.launch {
            _approvalResult.postValue(Result.Loading)
            val result = if (atasan1Nip == atasan2Nip) {
                leaveRepository.approveLeaveSameSupervisor(
                    pt, nipAtasan, leaveId, approvalValue,
                    potongGaji, potongCuti, dispensasi
                )
            } else {
                leaveRepository.approveLeaveDifferentSupervisor(
                    pt, nipAtasan, leaveId, approvalValue,
                    potongGaji, potongCuti, dispensasi
                )
            }
            _approvalResult.postValue(result)
        }
    }
    
    /**
     * Reject leave request
     */
    fun rejectLeaveRequest(
        pt: String,
        nipAtasan: String,
        leaveId: String
    ) {
        viewModelScope.launch {
            _rejectionResult.postValue(Result.Loading)
            val result = leaveRepository.rejectLeaveRequest(pt, nipAtasan, leaveId, "N")
            _rejectionResult.postValue(result)
        }
    }
}

class ApprovalViewModelFactory(
    private val leaveRepository: LeaveRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ApprovalViewModel::class.java)) {
            return ApprovalViewModel(leaveRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
