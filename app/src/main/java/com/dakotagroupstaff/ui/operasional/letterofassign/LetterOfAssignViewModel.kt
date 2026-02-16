package com.dakotagroupstaff.ui.operasional.letterofassign

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dakotagroupstaff.data.repository.LetterOfAssignRepository
import com.dakotagroupstaff.data.remote.response.LetterOfAssignDetail
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
}
