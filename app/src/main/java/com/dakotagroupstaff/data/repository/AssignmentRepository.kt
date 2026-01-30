package com.dakotagroupstaff.data.repository

import com.dakotagroupstaff.data.Result
import com.dakotagroupstaff.data.remote.response.*
import com.dakotagroupstaff.data.remote.retrofit.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Repository for Letter of Assignment (Surat Tugas) feature
 * Handles data operations for assignment management and operational costs
 */
class AssignmentRepository(
    private val apiService: ApiService
) {
    
    /**
     * Get agent locations to map agent codes to names
     * @param pt Company code (DBS, DLB, Logistik)
     * @return Flow of Result containing list of AgentLocationData
     */
    fun getAgentLocations(
        pt: String
    ): Flow<Result<List<AgentLocationData>>> = flow {
        emit(Result.Loading)
        
        try {
            val response = apiService.getAgentLocations(pt = pt)
            
            if (response.success) {
                emit(Result.Success(response.data ?: emptyList()))
            } else {
                emit(Result.Error(response.message ?: "Failed to get agent locations"))
            }
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Failed to get agent locations"))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get active letter of assignment for a driver
     * @param pt Company code (DBS, DLB, Logistik)
     * @param nip Driver's NIP
     * @return Flow of Result containing LetterOfAssignmentData
     */
    fun getLetterOfAssignment(
        pt: String,
        nip: String
    ): Flow<Result<LetterOfAssignmentData>> = flow {
        emit(Result.Loading)
        
        try {
            val response = apiService.getLetterOfAssignment(
                pt = pt,
                request = LetterOfAssignRequest(nip)
            )
            
            if (response.success && response.data?.isNotEmpty() == true) {
                emit(Result.Success(response.data[0]))
            } else {
                emit(Result.Error(response.message ?: "No active assignment found"))
            }
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Failed to get letter of assignment"))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get all assignments list for a driver
     * @param pt Company code (DBS, DLB, Logistik)
     * @param nip Driver's NIP
     * @return Flow of Result containing list of AssignmentListItem
     */
    fun getAssignmentList(
        pt: String,
        nip: String
    ): Flow<Result<List<AssignmentListItem>>> = flow {
        emit(Result.Loading)
        
        try {
            val response = apiService.getAssignmentList(
                pt = pt,
                request = LetterOfAssignRequest(nip)
            )
            
            if (response.success) {
                emit(Result.Success(response.data ?: emptyList()))
            } else {
                emit(Result.Error(response.message ?: "Failed to get assignment list"))
            }
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Failed to get assignment list"))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get specific assignment details by sID
     * @param pt Company code (DBS, DLB, Logistik)
     * @param nip Driver's NIP
     * @param sID Assignment ID
     * @return Flow of Result containing LetterOfAssignmentData
     */
    fun getLetterOfAssignmentBySID(
        pt: String,
        nip: String,
        sID: String
    ): Flow<Result<LetterOfAssignmentData>> = flow {
        emit(Result.Loading)
        
        try {
            val response = apiService.getLetterOfAssignment(
                pt = pt,
                request = LetterOfAssignRequest(nip)
            )
            
            if (response.success && response.data?.isNotEmpty() == true) {
                // Find the specific assignment by sID
                val assignment = response.data.find { it.sID == sID }
                if (assignment != null) {
                    emit(Result.Success(assignment))
                } else {
                    emit(Result.Error("Assignment with ID $sID not found"))
                }
            } else {
                emit(Result.Error(response.message ?: "Failed to get assignment"))
            }
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Failed to get assignment"))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Update GPS location for an assignment
     * @param pt Company code (DBS, DLB, Logistik)
     * @param sID Assignment ID
     * @param lat Latitude (optional, null if no GPS)
     * @param lon Longitude (optional, null if no GPS)
     * @return Flow of Result containing UpdateLocationData
     */
    fun updateAssignmentLocation(
        pt: String,
        sID: String,
        lat: String? = null,
        lon: String? = null
    ): Flow<Result<UpdateLocationData>> = flow {
        emit(Result.Loading)
        
        try {
            val response = apiService.updateAssignmentLocation(
                pt = pt,
                request = UpdateLocationRequest(sID, lat, lon)
            )
            
            if (response.success && response.data != null) {
                emit(Result.Success(response.data))
            } else {
                emit(Result.Error(response.message ?: "Failed to update location"))
            }
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Failed to update location"))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get operational cost items (master data)
     * @param pt Company code (DBS, DLB, Logistik)
     * @return Flow of Result containing list of OperationalCostItem
     */
    fun getOperationalCostItems(
        pt: String
    ): Flow<Result<List<OperationalCostItem>>> = flow {
        emit(Result.Loading)
        
        try {
            val response = apiService.getOperationalCostItems(pt = pt)
            
            if (response.success) {
                emit(Result.Success(response.data ?: emptyList()))
            } else {
                emit(Result.Error(response.message ?: "Failed to get cost items"))
            }
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Failed to get cost items"))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get down payment operational costs
     * @param pt Company code (DBS, DLB, Logistik)
     * @param sID Assignment ID
     * @return Flow of Result containing list of arrays with cost data
     */
    fun getDownPaymentCosts(
        pt: String,
        sID: String
    ): Flow<Result<List<List<String>>>> = flow {
        emit(Result.Loading)
        
        try {
            val response = apiService.getOperationalCostList(
                pt = pt,
                request = OperationalCostListRequest(sID, "dp")
            )
            
            if (response.success) {
                // Return empty list if data is null or empty
                emit(Result.Success(response.data ?: emptyList()))
            } else {
                emit(Result.Error(response.message ?: "Failed to get down payment costs"))
            }
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Failed to get down payment costs"))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get additional operational costs
     * @param pt Company code (DBS, DLB, Logistik)
     * @param sID Assignment ID
     * @return Flow of Result containing list of arrays with cost data
     */
    fun getAdditionalOperationalCosts(
        pt: String,
        sID: String
    ): Flow<Result<List<List<String>>>> = flow {
        emit(Result.Loading)
        
        try {
            val response = apiService.getOperationalCostList(
                pt = pt,
                request = OperationalCostListRequest(sID, "op")
            )
            
            if (response.success) {
                // Return empty list if data is null or empty
                emit(Result.Success(response.data ?: emptyList()))
            } else {
                emit(Result.Error(response.message ?: "Failed to get additional costs"))
            }
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Failed to get additional costs"))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get fuel/BBM records
     * @param pt Company code (DBS, DLB, Logistik)
     * @param sID Assignment ID
     * @return Flow of Result containing list of arrays with fuel data
     */
    fun getFuelRecords(
        pt: String,
        sID: String
    ): Flow<Result<List<List<String>>>> = flow {
        emit(Result.Loading)
        
        try {
            val response = apiService.getOperationalCostList(
                pt = pt,
                request = OperationalCostListRequest(sID, "vcn")
            )
            
            if (response.success) {
                // Return empty list if data is null or empty
                emit(Result.Success(response.data ?: emptyList()))
            } else {
                emit(Result.Error(response.message ?: "Failed to get fuel records"))
            }
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Failed to get fuel records"))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get voucher costs
     * @param pt Company code (DBS, DLB, Logistik)
     * @param sID Assignment ID
     * @return Flow of Result containing list of arrays with voucher data
     */
    fun getVoucherCosts(
        pt: String,
        sID: String
    ): Flow<Result<List<List<String>>>> = flow {
        emit(Result.Loading)
        
        try {
            val response = apiService.getOperationalCostList(
                pt = pt,
                request = OperationalCostListRequest(sID, "vc")
            )
            
            if (response.success) {
                // Return empty list if data is null or empty
                emit(Result.Success(response.data ?: emptyList()))
            } else {
                emit(Result.Error(response.message ?: "Failed to get voucher costs"))
            }
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Failed to get voucher costs"))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Save operational cost
     * @param pt Company code (DBS, DLB, Logistik)
     * @param sID Assignment ID
     * @param itemId Cost item ID
     * @param nominal Amount
     * @param keterangan Notes (optional)
     * @param tipe Type: "insert" or "update"
     * @return Flow of Result containing SaveOperationalCostData
     */
    fun saveOperationalCost(
        pt: String,
        sID: String,
        itemId: String,
        nominal: String,
        keterangan: String? = null,
        tipe: String = "insert"
    ): Flow<Result<SaveOperationalCostData>> = flow {
        emit(Result.Loading)
        
        try {
            val response = apiService.saveOperationalCost(
                pt = pt,
                request = SaveOperationalCostRequest(
                    sID = sID,
                    itemId = itemId,
                    nominal = nominal,
                    keterangan = keterangan,
                    tipe = tipe
                )
            )
            
            if (response.success && response.data != null) {
                emit(Result.Success(response.data))
            } else {
                emit(Result.Error(response.message ?: "Failed to save operational cost"))
            }
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Failed to save operational cost"))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Check approval status for operational cost
     * @param pt Company code (DBS, DLB, Logistik)
     * @param sID Assignment ID
     * @return Flow of Result containing ApprovalStatusData
     */
    fun checkOperationalCostApproval(
        pt: String,
        sID: String
    ): Flow<Result<ApprovalStatusData>> = flow {
        emit(Result.Loading)
        
        try {
            val response = apiService.checkOperationalCostApproval(
                pt = pt,
                request = CheckApprovalRequest(sID)
            )
            
            if (response.success && response.data != null) {
                emit(Result.Success(response.data))
            } else {
                emit(Result.Error(response.message ?: "Failed to check approval status"))
            }
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Failed to check approval status"))
        }
    }.flowOn(Dispatchers.IO)
}
