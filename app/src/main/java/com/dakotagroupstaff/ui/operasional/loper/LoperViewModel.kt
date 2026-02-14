package com.dakotagroupstaff.ui.operasional.loper

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.dakotagroupstaff.data.local.entity.DeliveryListEntity
import com.dakotagroupstaff.data.remote.response.CheckBarcodeData
import com.dakotagroupstaff.data.remote.response.DeliveryItem
import com.dakotagroupstaff.data.remote.response.DeliveryListResponse
import com.dakotagroupstaff.data.repository.DeliveryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoperViewModel(
    private val deliveryRepository: DeliveryRepository,
    private val loperRepository: com.dakotagroupstaff.data.repository.LoperRepository
) : ViewModel() {
    
    private val _deliveryList = MutableLiveData<com.dakotagroupstaff.data.Result<DeliveryListResponse>>()
    val deliveryList: LiveData<com.dakotagroupstaff.data.Result<DeliveryListResponse>> = _deliveryList
    
    val sentDeliveries: LiveData<List<DeliveryListEntity>> = 
        deliveryRepository.getSentDeliveries().asLiveData()

    private val _checkBarcodeResult = MutableStateFlow<com.dakotagroupstaff.data.Result<CheckBarcodeData>?>(null)
    val checkBarcodeResult: StateFlow<com.dakotagroupstaff.data.Result<CheckBarcodeData>?> = _checkBarcodeResult

    private val _resultBarcodeBTTResult = MutableStateFlow<com.dakotagroupstaff.data.Result<Any>?>(null)
    val resultBarcodeBTTResult: StateFlow<com.dakotagroupstaff.data.Result<Any>?> = _resultBarcodeBTTResult
    
    fun getDeliveryList(nip: String) {
        viewModelScope.launch {
            deliveryRepository.getDeliveryList(nip).collect { result ->
                _deliveryList.postValue(result)
            }
        }
    }
    
    fun checkBarcode(barcode: String): Flow<com.dakotagroupstaff.data.Result<CheckBarcodeData>> {
        return loperRepository.checkBarcode(barcode)
    }
    
    fun resultBarcodeBTT(bttId: String, koliData: List<Any>, noLoper: String) {
        viewModelScope.launch {
            loperRepository.resultBarcodeBTT(bttId, koliData, noLoper).collect {
                _resultBarcodeBTTResult.value = it
            }
        }
    }
    
    fun resetResultBarcodeBTT() {
        _resultBarcodeBTTResult.value = null
    }
    
    suspend fun markAsSent(
        deliveryItem: DeliveryItem,
        receiverName: String,
        fotoBase64: String?,
        ttdBase64: String?,
        latitude: String?,
        longitude: String?
    ) {
        deliveryRepository.markDeliveryAsSent(
            deliveryItem,
            receiverName,
            fotoBase64,
            ttdBase64,
            latitude,
            longitude
        )
    }
    
    suspend fun removeSent(noBtt: String) {
        deliveryRepository.removeSentDelivery(noBtt)
    }
    
    suspend fun isDeliverySent(noBtt: String): Boolean {
        return deliveryRepository.isDeliverySent(noBtt)
    }
    
    /**
     * Check barcode against server
     */
}
