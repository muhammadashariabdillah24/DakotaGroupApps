package com.dakotagroupstaff.ui.operasional.assignment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dakotagroupstaff.data.Result
import com.dakotagroupstaff.data.remote.response.*
import com.dakotagroupstaff.data.repository.AssignmentRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for Letter of Assignment (Surat Tugas) feature
 * Manages UI state and business logic for:
 * - Assignment details and list
 * - Location tracking
 * - Operational costs (down payment and additional costs)
 */
class AssignmentViewModel(
    private val assignmentRepository: AssignmentRepository
) : ViewModel() {
    
    // ==================== Assignment Data ====================
    
    private val _assignment = MutableLiveData<Result<LetterOfAssignmentData>>()
    val assignment: LiveData<Result<LetterOfAssignmentData>> = _assignment
    
    private val _assignmentList = MutableLiveData<Result<List<AssignmentListItem>>>()
    val assignmentList: LiveData<Result<List<AssignmentListItem>>> = _assignmentList
    
    private val _locationUpdate = MutableLiveData<Result<UpdateLocationData>>()
    val locationUpdate: LiveData<Result<UpdateLocationData>> = _locationUpdate
    
    private val _agentLocations = MutableLiveData<Result<List<AgentLocationData>>>()
    val agentLocations: LiveData<Result<List<AgentLocationData>>> = _agentLocations
    
    // Cache for agent locations to avoid repeated API calls
    private var agentLocationsCache: List<AgentLocationData>? = null
    
    // ==================== Operational Cost Data ====================
    
    private val _costItems = MutableLiveData<Result<List<OperationalCostItem>>>()
    val costItems: LiveData<Result<List<OperationalCostItem>>> = _costItems
    
    private val _downPaymentCosts = MutableLiveData<Result<List<List<String>>>>()
    val downPaymentCosts: LiveData<Result<List<List<String>>>> = _downPaymentCosts
    
    private val _additionalCosts = MutableLiveData<Result<List<List<String>>>>()
    val additionalCosts: LiveData<Result<List<List<String>>>> = _additionalCosts
    
    private val _fuelRecords = MutableLiveData<Result<List<List<String>>>>()
    val fuelRecords: LiveData<Result<List<List<String>>>> = _fuelRecords
    
    private val _voucherCosts = MutableLiveData<Result<List<List<String>>>>()
    val voucherCosts: LiveData<Result<List<List<String>>>> = _voucherCosts
    
    private val _saveResult = MutableLiveData<Result<SaveOperationalCostData>>()
    val saveResult: LiveData<Result<SaveOperationalCostData>> = _saveResult
    
    private val _approvalStatus = MutableLiveData<Result<ApprovalStatusData>>()
    val approvalStatus: LiveData<Result<ApprovalStatusData>> = _approvalStatus
    
    // ==================== UI State ====================
    
    private val _selectedTabIndex = MutableLiveData<Int>(0)
    val selectedTabIndex: LiveData<Int> = _selectedTabIndex
    
    private val _isRefreshing = MutableLiveData<Boolean>(false)
    val isRefreshing: LiveData<Boolean> = _isRefreshing
    
    // ==================== Assignment Methods ====================
    
    /**
     * Get agent locations for mapping agent codes to names
     * @param pt Company code (DBS, DLB, Logistik)
     */
    fun getAgentLocations(pt: String) {
        // Return cached data if available
        if (agentLocationsCache != null) {
            _agentLocations.value = Result.Success(agentLocationsCache!!)
            return
        }
        
        viewModelScope.launch {
            assignmentRepository.getAgentLocations(pt).collect { result ->
                _agentLocations.postValue(result)
                // Cache successful results
                if (result is Result.Success) {
                    agentLocationsCache = result.data
                }
            }
        }
    }
    
    /**
     * Get agent name by agent code
     * @param agentCode Agent code (KodeAgen)
     * @return Agent name or null if not found
     */
    fun getAgentNameByCode(agentCode: Int): String? {
        return agentLocationsCache?.find { it.kodeAgen == agentCode }?.namaAgen
    }
    
    /**
     * Get active letter of assignment for a driver
     * @param pt Company code (DBS, DLB, Logistik)
     * @param nip Driver's NIP
     */
    fun getLetterOfAssignment(pt: String, nip: String) {
        viewModelScope.launch {
            _isRefreshing.value = true
            assignmentRepository.getLetterOfAssignment(pt, nip).collect { result ->
                _assignment.postValue(result)
                _isRefreshing.value = false
            }
        }
    }
    
    /**
     * Get all assignments list for a driver
     * @param pt Company code (DBS, DLB, Logistik)
     * @param nip Driver's NIP
     */
    fun getAssignmentList(pt: String, nip: String) {
        viewModelScope.launch {
            assignmentRepository.getAssignmentList(pt, nip).collect {
                _assignmentList.postValue(it)
            }
        }
    }
    
    /**
     * Get specific assignment by sID and load its details
     * @param pt Company code (DBS, DLB, Logistik)
     * @param nip Driver's NIP
     * @param sID Assignment ID to load
     */
    fun getLetterOfAssignmentBySID(pt: String, nip: String, sID: String) {
        viewModelScope.launch {
            _isRefreshing.value = true
            assignmentRepository.getLetterOfAssignmentBySID(pt, nip, sID).collect { result ->
                _assignment.postValue(result)
                _isRefreshing.value = false
                
                // If successful, reload operational costs
                if (result is Result.Success) {
                    getDownPaymentCosts(pt, sID)
                    getAdditionalOperationalCosts(pt, sID)
                    checkOperationalCostApproval(pt, sID)
                }
            }
        }
    }
    
    /**
     * Update GPS location for an assignment
     * @param pt Company code (DBS, DLB, Logistik)
     * @param sID Assignment ID
     * @param lat Latitude (null if no GPS)
     * @param lon Longitude (null if no GPS)
     */
    fun updateAssignmentLocation(
        pt: String,
        sID: String,
        lat: String? = null,
        lon: String? = null
    ) {
        viewModelScope.launch {
            assignmentRepository.updateAssignmentLocation(pt, sID, lat, lon).collect {
                _locationUpdate.postValue(it)
            }
        }
    }
    
    // ==================== Operational Cost Methods ====================
    
    /**
     * Get operational cost items (master data)
     * @param pt Company code (DBS, DLB, Logistik)
     */
    fun getOperationalCostItems(pt: String) {
        viewModelScope.launch {
            assignmentRepository.getOperationalCostItems(pt).collect {
                _costItems.postValue(it)
            }
        }
    }
    
    /**
     * Get down payment costs for an assignment
     * @param pt Company code (DBS, DLB, Logistik)
     * @param sID Assignment ID
     */
    fun getDownPaymentCosts(pt: String, sID: String) {
        viewModelScope.launch {
            _isRefreshing.value = true
            assignmentRepository.getDownPaymentCosts(pt, sID).collect { result ->
                _downPaymentCosts.postValue(result)
                _isRefreshing.value = false
            }
        }
    }
    
    /**
     * Get additional operational costs for an assignment
     * @param pt Company code (DBS, DLB, Logistik)
     * @param sID Assignment ID
     */
    fun getAdditionalOperationalCosts(pt: String, sID: String) {
        viewModelScope.launch {
            _isRefreshing.value = true
            assignmentRepository.getAdditionalOperationalCosts(pt, sID).collect { result ->
                _additionalCosts.postValue(result)
                _isRefreshing.value = false
            }
        }
    }
    
    /**
     * Get fuel/BBM records for an assignment
     * @param pt Company code (DBS, DLB, Logistik)
     * @param sID Assignment ID
     */
    fun getFuelRecords(pt: String, sID: String) {
        viewModelScope.launch {
            assignmentRepository.getFuelRecords(pt, sID).collect {
                _fuelRecords.postValue(it)
            }
        }
    }
    
    /**
     * Get voucher costs for an assignment
     * @param pt Company code (DBS, DLB, Logistik)
     * @param sID Assignment ID
     */
    fun getVoucherCosts(pt: String, sID: String) {
        viewModelScope.launch {
            assignmentRepository.getVoucherCosts(pt, sID).collect {
                _voucherCosts.postValue(it)
            }
        }
    }
    
    /**
     * Save operational cost
     * @param pt Company code (DBS, DLB, Logistik)
     * @param sID Assignment ID
     * @param itemId Cost item ID
     * @param nominal Amount
     * @param keterangan Notes (optional)
     * @param tipe Type: "insert" or "update"
     */
    fun saveOperationalCost(
        pt: String,
        sID: String,
        itemId: String,
        nominal: String,
        keterangan: String? = null,
        tipe: String = "insert"
    ) {
        // Validation
        if (itemId.isBlank()) {
            _saveResult.postValue(Result.Error("Pilih item biaya operasional"))
            return
        }
        
        if (nominal.isBlank() || nominal.toDoubleOrNull() == null || nominal.toDouble() <= 0) {
            _saveResult.postValue(Result.Error("Masukkan nominal yang valid"))
            return
        }
        
        viewModelScope.launch {
            assignmentRepository.saveOperationalCost(
                pt = pt,
                sID = sID,
                itemId = itemId,
                nominal = nominal,
                keterangan = keterangan,
                tipe = tipe
            ).collect {
                _saveResult.postValue(it)
                
                // Refresh additional costs after successful save
                if (it is Result.Success) {
                    getAdditionalOperationalCosts(pt, sID)
                }
            }
        }
    }
    
    /**
     * Check approval status for operational cost
     * @param pt Company code (DBS, DLB, Logistik)
     * @param sID Assignment ID
     */
    fun checkOperationalCostApproval(pt: String, sID: String) {
        viewModelScope.launch {
            assignmentRepository.checkOperationalCostApproval(pt, sID).collect {
                _approvalStatus.postValue(it)
            }
        }
    }
    
    // ==================== UI State Methods ====================
    
    /**
     * Set selected tab index
     * @param index Tab index (0 = Down Payment, 1 = Additional Costs)
     */
    fun setSelectedTab(index: Int) {
        _selectedTabIndex.value = index
    }
    
    /**
     * Reset save result (to clear previous messages)
     */
    fun resetSaveResult() {
        _saveResult.value = Result.Loading
    }
    
    /**
     * Refresh all data for current assignment
     * @param pt Company code (DBS, DLB, Logistik)
     * @param nip Driver's NIP
     * @param sID Assignment ID
     */
    fun refreshAllData(pt: String, nip: String, sID: String) {
        getLetterOfAssignment(pt, nip)
        getDownPaymentCosts(pt, sID)
        getAdditionalOperationalCosts(pt, sID)
        checkOperationalCostApproval(pt, sID)
    }
}
