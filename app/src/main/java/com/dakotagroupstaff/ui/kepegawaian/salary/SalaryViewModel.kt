package com.dakotagroupstaff.ui.kepegawaian.salary

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dakotagroupstaff.data.Result
import com.dakotagroupstaff.data.remote.response.SalarySlipData
import com.dakotagroupstaff.data.repository.SalaryRepository
import kotlinx.coroutines.launch

/**
 * Salary ViewModel
 * Following MVVM pattern from HistoryApp & MyQuranApp
 */
class SalaryViewModel(
    private val salaryRepository: SalaryRepository
) : ViewModel() {
    
    // Salary Slips State
    private val _salarySlips = MutableLiveData<Result<List<SalarySlipData>>>()
    val salarySlips: LiveData<Result<List<SalarySlipData>>> = _salarySlips
    
    // Error message
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    /**
     * Load salary slips for employee
     */
    fun loadSalarySlips(pt: String, nip: String, imei: String, simId: String) {
        viewModelScope.launch {
            salaryRepository.getSalarySlips(pt, nip, imei, simId).observeForever { result ->
                _salarySlips.value = result
                
                // Update error message if result is Error
                if (result is Result.Error) {
                    _errorMessage.value = result.message
                } else {
                    _errorMessage.value = null
                }
            }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
