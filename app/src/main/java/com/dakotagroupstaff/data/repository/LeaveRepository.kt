package com.dakotagroupstaff.data.repository

import com.dakotagroupstaff.data.Result
import com.dakotagroupstaff.data.local.dao.LeaveBalanceDao
import com.dakotagroupstaff.data.local.dao.LeaveDetailsDao
import com.dakotagroupstaff.data.local.entity.LeaveBalanceEntity
import com.dakotagroupstaff.data.local.entity.LeaveDetailsEntity
import com.dakotagroupstaff.data.mapper.toEntity
import com.dakotagroupstaff.data.remote.response.LeaveSubmissionData
import com.dakotagroupstaff.data.remote.response.PendingApprovalData
import com.dakotagroupstaff.data.remote.retrofit.ApiService
import com.dakotagroupstaff.data.remote.retrofit.LeaveRequest
import com.dakotagroupstaff.data.remote.retrofit.LeaveSubmissionRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class LeaveRepository(
    private val apiService: ApiService,
    private val leaveBalanceDao: LeaveBalanceDao,
    private val leaveDetailsDao: LeaveDetailsDao
) {
    
    /**
     * Get leave balance from local database or fetch from API
     * Implements offline-first strategy
     */
    fun getLeaveBalance(
        pt: String,
        nip: String,
        tahun: String,
        forceRefresh: Boolean = false
    ): Flow<Result<LeaveBalanceEntity>> = flow {
        emit(Result.Loading)
        
        try {
            // If force refresh, fetch from API first
            if (forceRefresh) {
                val response = apiService.getLeaveBalance(
                    pt = pt,
                    request = LeaveRequest(nip, tahun)
                )
                
                if (response.success && response.data?.isNotEmpty() == true) {
                    val leaveBalance = response.data[0].toEntity(nip, tahun)
                    leaveBalanceDao.insert(leaveBalance)
                    emit(Result.Success(leaveBalance))
                } else {
                    emit(Result.Error(response.message ?: "Failed to get leave balance"))
                }
            } else {
                // Get from local database (single emission to prevent multiple active collectors)
                // Filter by year to ensure we get the correct year's balance
                val cachedData = leaveBalanceDao.getLeaveBalanceByYear(nip, tahun).firstOrNull()
                
                if (cachedData != null) {
                    emit(Result.Success(cachedData))
                } else {
                    // If no cache, fetch from API
                    val response = apiService.getLeaveBalance(
                        pt = pt,
                        request = LeaveRequest(nip, tahun)
                    )
                    
                    if (response.success && response.data?.isNotEmpty() == true) {
                        val leaveBalance = response.data[0].toEntity(nip, tahun)
                        leaveBalanceDao.insert(leaveBalance)
                        emit(Result.Success(leaveBalance))
                    } else {
                        emit(Result.Error(response.message ?: "Failed to get leave balance"))
                    }
                }
            }
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Unknown error occurred"))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get leave details/history from local database or fetch from API
     * Implements offline-first strategy
     */
    fun getLeaveDetails(
        pt: String,
        nip: String,
        tahun: String,
        forceRefresh: Boolean = false
    ): Flow<Result<List<LeaveDetailsEntity>>> = flow {
        emit(Result.Loading)
        
        try {
            // Always fetch from API when force refresh
            if (forceRefresh) {
                val response = apiService.getLeaveDetails(
                    pt = pt,
                    request = LeaveRequest(nip, tahun)
                )
                
                if (response.success && response.data != null) {
                    val leaveDetails = response.data.map { it.toEntity(nip, tahun) }
                    // Delete only records for this specific year, not all years
                    leaveDetailsDao.deleteByYear(nip, tahun)
                    leaveDetailsDao.insertAll(leaveDetails)
                    emit(Result.Success(leaveDetails))
                } else {
                    emit(Result.Error(response.message ?: "Failed to get leave details"))
                }
            } else {
                // Get from local database (single emission to prevent multiple active collectors)
                val cachedData = leaveDetailsDao.getLeaveDetailsByYear(nip, tahun).firstOrNull()
                
                if (cachedData != null && cachedData.isNotEmpty()) {
                    emit(Result.Success(cachedData))
                } else {
                    // If no cache, fetch from API
                    val response = apiService.getLeaveDetails(
                        pt = pt,
                        request = LeaveRequest(nip, tahun)
                    )
                    
                    if (response.success && response.data != null) {
                        val leaveDetails = response.data.map { it.toEntity(nip, tahun) }
                        leaveDetailsDao.insertAll(leaveDetails)
                        emit(Result.Success(leaveDetails))
                    } else {
                        emit(Result.Error(response.message ?: "Failed to get leave details"))
                    }
                }
            }
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Unknown error occurred"))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Submit leave request to API
     */
    suspend fun submitLeaveRequest(
        pt: String,
        request: LeaveSubmissionRequest
    ): Result<LeaveSubmissionData> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.submitLeaveRequest(pt, request)
            
            if (response.success && response.data != null) {
                // Refresh leave details after successful submission
                refreshLeaveData(pt, request.nip ?: "", java.util.Calendar.getInstance().get(java.util.Calendar.YEAR).toString())
                Result.Success(response.data)
            } else {
                Result.Error(response.message ?: "Failed to submit leave request")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to submit leave request")
        }
    }
    
    /**
     * Check if NIP is a supervisor
     * Returns supervisor status from API
     */
    suspend fun checkSupervisor(
        pt: String,
        nip: String
    ): Result<com.dakotagroupstaff.data.remote.response.SupervisorCheckData> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.checkSupervisor(pt, nip)
            
            if (response.success && response.data != null) {
                Result.Success(response.data)
            } else {
                Result.Error(response.message ?: "Failed to check supervisor status")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to check supervisor status")
        }
    }
    
    /**
     * Get pending approvals for supervisor
     * Fetches list of leave requests that need approval from this supervisor
     * Migrated from OldSystemApproval
     */
    suspend fun getPendingApprovals(
        pt: String,
        nip: String
    ): Result<List<PendingApprovalData>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getPendingApprovals(pt, nip)
            
            if (response.success && response.data != null) {
                Result.Success(response.data)
            } else {
                Result.Error(response.message ?: "Failed to get pending approvals")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to get pending approvals")
        }
    }
    
    /**
     * Approve leave request (same supervisor - ATASAN1 == ATASAN2)
     * @param potongGaji - Deduction from salary (Y/N)
     * @param potongCuti - Deduction from leave balance (Y/N)
     * @param dispensasi - Dispensation/No deduction (Y/N)
     */
    suspend fun approveLeaveSameSupervisor(
        pt: String,
        nipAtasan: String,
        leaveId: String,
        approvalValue: String,
        potongGaji: String,
        potongCuti: String,
        dispensasi: String
    ): Result<com.dakotagroupstaff.data.remote.response.LeaveApprovalData> = withContext(Dispatchers.IO) {
        try {
            val request = com.dakotagroupstaff.data.remote.retrofit.ApprovalRequest(
                nipAtasan = nipAtasan,
                leaveId = leaveId,
                approvalValue = approvalValue,
                potongGaji = potongGaji,
                potongCuti = potongCuti,
                dispensasi = dispensasi
            )
            val response = apiService.approveLeaveSameSupervisor(pt, request)
            
            if (response.success && response.data != null) {
                Result.Success(response.data)
            } else {
                Result.Error(response.message ?: "Failed to approve leave request")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to approve leave request")
        }
    }
    
    /**
     * Approve leave request (different supervisor - ATASAN1 != ATASAN2)
     * @param potongGaji - Deduction from salary (Y/N)
     * @param potongCuti - Deduction from leave balance (Y/N)
     * @param dispensasi - Dispensation/No deduction (Y/N)
     */
    suspend fun approveLeaveDifferentSupervisor(
        pt: String,
        nipAtasan: String,
        leaveId: String,
        approvalValue: String,
        potongGaji: String,
        potongCuti: String,
        dispensasi: String
    ): Result<com.dakotagroupstaff.data.remote.response.LeaveApprovalData> = withContext(Dispatchers.IO) {
        try {
            val request = com.dakotagroupstaff.data.remote.retrofit.ApprovalRequest(
                nipAtasan = nipAtasan,
                leaveId = leaveId,
                approvalValue = approvalValue,
                potongGaji = potongGaji,
                potongCuti = potongCuti,
                dispensasi = dispensasi
            )
            val response = apiService.approveLeaveDifferentSupervisor(pt, request)
            
            if (response.success && response.data != null) {
                Result.Success(response.data)
            } else {
                Result.Error(response.message ?: "Failed to approve leave request")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to approve leave request")
        }
    }
    
    /**
     * Reject leave request
     */
    suspend fun rejectLeaveRequest(
        pt: String,
        nipAtasan: String,
        leaveId: String,
        activeStatus: String = "N"
    ): Result<com.dakotagroupstaff.data.remote.response.LeaveRejectionData> = withContext(Dispatchers.IO) {
        try {
            val request = com.dakotagroupstaff.data.remote.retrofit.RejectionRequest(
                nipAtasan = nipAtasan,
                leaveId = leaveId,
                activeStatus = activeStatus
            )
            val response = apiService.rejectLeaveRequest(pt, request)
            
            if (response.success && response.data != null) {
                Result.Success(response.data)
            } else {
                Result.Error(response.message ?: "Failed to reject leave request")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to reject leave request")
        }
    }
    
    /**
     * Refresh leave data (balance and details) from API
     */
    private suspend fun refreshLeaveData(pt: String, nip: String, tahun: String) {
        try {
            // Refresh balance
            val balanceResponse = apiService.getLeaveBalance(
                pt = pt,
                request = LeaveRequest(nip, tahun)
            )
            
            if (balanceResponse.success && balanceResponse.data?.isNotEmpty() == true) {
                val leaveBalance = balanceResponse.data[0].toEntity(nip, tahun)
                leaveBalanceDao.insert(leaveBalance)
            }
            
            // Refresh details
            val detailsResponse = apiService.getLeaveDetails(
                pt = pt,
                request = LeaveRequest(nip, tahun)
            )
            
            if (detailsResponse.success && detailsResponse.data != null) {
                val leaveDetails = detailsResponse.data.map { it.toEntity(nip, tahun) }
                // Delete only records for this specific year
                leaveDetailsDao.deleteByYear(nip, tahun)
                leaveDetailsDao.insertAll(leaveDetails)
            }
        } catch (e: Exception) {
            // Log error but don't throw - this is a background refresh
            e.printStackTrace()
        }
    }
    
    /**
     * Get Super Atasan list
     * Fetches list of super supervisors from API
     */
    suspend fun getSuperAtasan(
        pt: String
    ): Result<List<com.dakotagroupstaff.data.remote.response.SuperAtasanData>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getSuperAtasan(pt)
            
            if (response.success && response.data != null) {
                Result.Success(response.data)
            } else {
                Result.Error(response.message ?: "Failed to get super atasan list")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to get super atasan list")
        }
    }
    
    /**
     * Clear all leave data for a specific employee
     */
    suspend fun clearLeaveData(nip: String) = withContext(Dispatchers.IO) {
        try {
            leaveBalanceDao.delete(nip)
            leaveDetailsDao.delete(nip)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
