package com.dakotagroupstaff.ui.operasional.loper

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.dakotagroupstaff.data.local.entity.DeliveryListEntity
import com.dakotagroupstaff.data.remote.response.DeliveryItem
import com.dakotagroupstaff.data.remote.response.DeliveryListResponse
import com.dakotagroupstaff.data.repository.DeliveryRepository
import kotlinx.coroutines.launch

class LoperViewModel(private val deliveryRepository: DeliveryRepository) : ViewModel() {
    
    private val _deliveryList = MutableLiveData<com.dakotagroupstaff.data.Result<DeliveryListResponse>>()
    val deliveryList: LiveData<com.dakotagroupstaff.data.Result<DeliveryListResponse>> = _deliveryList
    
    val sentDeliveries: LiveData<List<DeliveryListEntity>> = 
        deliveryRepository.getSentDeliveries().asLiveData()
    
    fun getDeliveryList(nip: String) {
        viewModelScope.launch {
            deliveryRepository.getDeliveryList(nip).collect { result ->
                _deliveryList.postValue(result)
            }
        }
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
}
