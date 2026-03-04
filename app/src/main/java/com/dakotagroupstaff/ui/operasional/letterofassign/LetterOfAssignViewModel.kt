package com.dakotagroupstaff.ui.operasional.letterofassign

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dakotagroupstaff.data.repository.LetterOfAssignRepository
import com.dakotagroupstaff.data.remote.response.LetterOfAssignDetail
import com.dakotagroupstaff.data.remote.response.LoadingData
import com.dakotagroupstaff.data.remote.response.UnloadingData
import com.dakotagroupstaff.data.remote.response.NextPointData
import com.dakotagroupstaff.data.Result
import kotlinx.coroutines.launch

class LetterOfAssignViewModel(
    private val repository: LetterOfAssignRepository
) : ViewModel() {
    
    private val _letterOfAssign = MutableLiveData<Result<List<LetterOfAssignDetail>>>()
    val letterOfAssign: LiveData<Result<List<LetterOfAssignDetail>>> = _letterOfAssign
    
    private val _checkpointResult = MutableLiveData<Result<String>>()
    val checkpointResult: LiveData<Result<String>> = _checkpointResult
    
    private val _currentSID = MutableLiveData<String>()
    val currentSID: LiveData<String> = _currentSID
    
    // Loading (Muat) data
    private val _loadingData = MutableLiveData<Result<List<LoadingData>>>()
    val loadingData: LiveData<Result<List<LoadingData>>> = _loadingData
    
    // Unloading (Bongkar) data
    private val _unloadingData = MutableLiveData<Result<List<UnloadingData>>>()
    val unloadingData: LiveData<Result<List<UnloadingData>>> = _unloadingData
    
    // Next point data
    private val _nextPointData = MutableLiveData<Result<NextPointData>>()
    val nextPointData: LiveData<Result<NextPointData>> = _nextPointData
    
    // KM data
    private val _kmData = MutableLiveData<Result<String>>()
    val kmData: LiveData<Result<String>> = _kmData
    
    // Update KM result
    private val _updateKMResult = MutableLiveData<Result<String>>()
    val updateKMResult: LiveData<Result<String>> = _updateKMResult
    
    /**
     * Load letter of assign for driver
     */
    fun loadLetterOfAssign(nip: String, pt: String) {
        viewModelScope.launch {
            _letterOfAssign.value = Result.Loading
            val result = repository.getLetterOfAssign(nip, pt)
            _letterOfAssign.value = result
            
            // Save sID if successful
            if (result is Result.Success && result.data.isNotEmpty()) {
                _currentSID.value = result.data[0].sID
            }
        }
    }
    
    /**
     * Update GPS location to server
     */
    fun updateLocation(lat: Double, lon: Double) {
        val sID = _currentSID.value ?: return
        
        viewModelScope.launch {
            val pt = repository.getPt() ?: return@launch
            repository.updateLocation(sID, lat.toString(), lon.toString(), pt)
        }
    }
    
    /**
     * Submit checkpoint check-in
     */
    fun submitCheckpoint(
        agenID: String,
        km: String,
        lat: String,
        lon: String,
        urlpic: String,
        nip: String,
        urut: String
    ) {
        val sID = _currentSID.value ?: return
        
        viewModelScope.launch {
            val pt = repository.getPt() ?: return@launch
            _checkpointResult.value = Result.Loading
            val result = repository.submitCheckpoint(
                sID = sID,
                agenID = agenID,
                km = km,
                lat = lat,
                lon = lon,
                urlpic = urlpic,
                nip = nip,
                urut = urut,
                pt = pt
            )
            _checkpointResult.value = result
        }
    }
    
    /**
     * Complete letter of assign
     */
    fun completeAssignment() {
        val sID = _currentSID.value ?: return
        
        viewModelScope.launch {
            val pt = repository.getPt() ?: return@launch
            repository.completeAssignment(sID, pt)
        }
    }
    
    /**
     * Get loading (muat) data
     */
    fun getLoadingData(agenID: String) {
        val sID = _currentSID.value ?: return
        
        viewModelScope.launch {
            val pt = repository.getPt() ?: return@launch
            _loadingData.value = Result.Loading
            val result = repository.getLoadingData(sID, agenID, pt)
            _loadingData.value = result
        }
    }
    
    /**
     * Get unloading (bongkar) data
     */
    fun getUnloadingData(agenID: String) {
        val sID = _currentSID.value ?: return
        
        viewModelScope.launch {
            val pt = repository.getPt() ?: return@launch
            _unloadingData.value = Result.Loading
            val result = repository.getUnloadingData(sID, agenID, pt)
            _unloadingData.value = result
        }
    }
    
    /**
     * Get next point information
     */
    fun getNextPoint() {
        val sID = _currentSID.value ?: return
        
        viewModelScope.launch {
            val pt = repository.getPt() ?: return@launch
            _nextPointData.value = Result.Loading
            val result = repository.getNextPoint(sID, pt)
            _nextPointData.value = result
        }
    }
    
    /**
     * Get KM data for vehicle
     */
    fun getKMData(kendID: String) {
        viewModelScope.launch {
            val pt = repository.getPt() ?: return@launch
            _kmData.value = Result.Loading
            val result = repository.getKMData(kendID, pt)
            _kmData.value = result
        }
    }
    
    /**
     * Update KM
     */
    fun updateKM(agenId: String, urut: String, status: String, km: String) {
        val sID = _currentSID.value ?: return
        
        viewModelScope.launch {
            val pt = repository.getPt() ?: return@launch
            _updateKMResult.value = Result.Loading
            val result = repository.updateKM(sID, agenId, urut, status, km, pt)
            _updateKMResult.value = result
        }
    }
    
    // ========== Additional Features ==========
    
    // Alternative Route
    private val _alternativeRoute = MutableLiveData<Result<List<com.dakotagroupstaff.data.remote.response.AlternativeRouteData>>>()
    val alternativeRoute: LiveData<Result<List<com.dakotagroupstaff.data.remote.response.AlternativeRouteData>>> = _alternativeRoute
    
    // Lock/Unlock results
    private val _lockResult = MutableLiveData<Result<String>>()
    val lockResult: LiveData<Result<String>> = _lockResult
    
    // Less Items
    private val _lessItems = MutableLiveData<Result<List<com.dakotagroupstaff.data.remote.response.LessItemsData>>>()
    val lessItems: LiveData<Result<List<com.dakotagroupstaff.data.remote.response.LessItemsData>>> = _lessItems
    
    // Upload Photo result
    private val _uploadPhotoResult = MutableLiveData<Result<String>>()
    val uploadPhotoResult: LiveData<Result<String>> = _uploadPhotoResult
    
    /**
     * Get alternative route (rute cadangan)
     */
    fun getAlternativeRoute() {
        val sID = _currentSID.value ?: return
        
        viewModelScope.launch {
            val pt = repository.getPt() ?: return@launch
            _alternativeRoute.value = Result.Loading
            val result = repository.getAlternativeRoute(sID, pt)
            _alternativeRoute.value = result
        }
    }
    
    /**
     * Lock loading (muat)
     */
    fun lockLoading(agenID: String, loadHId: String) {
        val sID = _currentSID.value ?: return
        
        viewModelScope.launch {
            val pt = repository.getPt() ?: return@launch
            _lockResult.value = Result.Loading
            val result = repository.lockLoading(sID, agenID, loadHId, pt)
            _lockResult.value = result
        }
    }
    
    /**
     * Unlock loading (muat)
     */
    fun unlockLoading(agenID: String, loadHId: String) {
        val sID = _currentSID.value ?: return
        
        viewModelScope.launch {
            val pt = repository.getPt() ?: return@launch
            _lockResult.value = Result.Loading
            val result = repository.unlockLoading(sID, agenID, loadHId, pt)
            _lockResult.value = result
        }
    }
    
    /**
     * Lock unloading (bongkar)
     */
    fun lockUnloading(unloadId: String, nip: String) {
        viewModelScope.launch {
            val pt = repository.getPt() ?: return@launch
            _lockResult.value = Result.Loading
            val result = repository.lockUnloading(unloadId, nip, pt)
            _lockResult.value = result
        }
    }
    
    /**
     * Get less items (barang kurang)
     */
    fun getLessItems(nip: String) {
        viewModelScope.launch {
            val pt = repository.getPt() ?: return@launch
            _lessItems.value = Result.Loading
            val result = repository.getLessItems(nip, pt)
            _lessItems.value = result
        }
    }
    
    /**
     * Upload photo for checkpoint
     */
    fun uploadCheckpointPhoto(imageBase64: String) {
        viewModelScope.launch {
            val pt = repository.getPt() ?: return@launch
            _uploadPhotoResult.value = Result.Loading
            val result = repository.uploadCheckpointPhoto(imageBase64, pt)
            _uploadPhotoResult.value = result
        }
    }
}
