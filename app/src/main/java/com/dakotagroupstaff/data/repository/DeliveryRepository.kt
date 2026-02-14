package com.dakotagroupstaff.data.repository

import com.dakotagroupstaff.data.local.dao.DeliveryListDao
import com.dakotagroupstaff.data.local.entity.DeliveryListEntity
import com.dakotagroupstaff.data.local.preferences.UserPreferences
import com.dakotagroupstaff.data.remote.response.DeliveryItem
import com.dakotagroupstaff.data.remote.response.DeliveryListRequest
import com.dakotagroupstaff.data.remote.response.DeliveryListResponse
import com.dakotagroupstaff.data.remote.response.SubmitDeliveryData
import com.dakotagroupstaff.data.remote.response.SubmitDeliveryRequest
import com.dakotagroupstaff.data.remote.retrofit.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

class DeliveryRepository(
    private val apiService: ApiService,
    private val userPreferences: UserPreferences,
    private val deliveryListDao: DeliveryListDao
) {
    
    /**
     * Get delivery list (BTT Loper) - All pending BTT from API
     */
    fun getDeliveryList(nip: String): Flow<com.dakotagroupstaff.data.Result<DeliveryListResponse>> = flow {
        emit(com.dakotagroupstaff.data.Result.Loading)
        try {
            val pt = userPreferences.getPt().first()
            val request = DeliveryListRequest(nip = nip)
            val response = apiService.getDeliveryList(pt = pt, request = request)
            
            if (response.success) {
                emit(com.dakotagroupstaff.data.Result.Success(response))
            } else {
                emit(com.dakotagroupstaff.data.Result.Error(response.message))
            }
        } catch (e: Exception) {
            emit(com.dakotagroupstaff.data.Result.Error(e.message ?: "Terjadi kesalahan"))
        }
    }
    
    /**
     * Get sent BTT from local storage
     */
    fun getSentDeliveries(): Flow<List<DeliveryListEntity>> {
        return deliveryListDao.getDeliveriesByStatus("Sent")
    }
    
    /**
     * Mark BTT as sent (save to local storage with all media data)
     */
    suspend fun markDeliveryAsSent(
        deliveryItem: DeliveryItem,
        receiverName: String,
        fotoBase64: String?,
        ttdBase64: String?,
        latitude: String?,
        longitude: String?
    ) {
        val entity = DeliveryListEntity(
            id = deliveryItem.noBtt,
            noLoper = deliveryItem.noLoper,
            noBtt = deliveryItem.noBtt,
            penerima = receiverName,
            alamat = buildFullAddress(deliveryItem),
            jumlahKoli = deliveryItem.jumlahKoli,
            status = "Sent",
            tanggalKirim = System.currentTimeMillis(),
            keterangan = "Terkirim dengan foto dan tanda tangan",
            cachedAt = System.currentTimeMillis(),
            fotoBase64 = fotoBase64,
            ttdBase64 = ttdBase64,
            latitude = latitude,
            longitude = longitude
        )
        deliveryListDao.insert(entity)
    }
    
    /**
     * Remove BTT from sent list (delete from local storage)
     */
    suspend fun removeSentDelivery(noBtt: String) {
        deliveryListDao.deleteByBtt(noBtt)
    }
    
    /**
     * Check if BTT is already sent
     */
    suspend fun isDeliverySent(noBtt: String): Boolean {
        val entity = deliveryListDao.getDeliveryByBtt(noBtt)
        return entity != null && entity.status == "Sent"
    }
    
    /**
     * Get delivery by BTT number (for loading saved data)
     */
    suspend fun getDeliveryByBtt(noBtt: String): DeliveryListEntity? {
        return deliveryListDao.getDeliveryByBtt(noBtt)
    }
    
    private fun buildFullAddress(item: DeliveryItem): String {
        return buildString {
            append(item.alamat)
            if (item.kelurahan.isNotEmpty()) append(", ${item.kelurahan}")
            if (item.kecamatan.isNotEmpty()) append(", ${item.kecamatan}")
            if (item.kota.isNotEmpty()) append(", ${item.kota}")
            if (item.propinsi.isNotEmpty()) append(", ${item.propinsi}")
        }
    }
    
    /**
     * Submit delivery data with photo, signature, and koli barcode data
     * Automatically loads koliData from DataStore for the given BTT
     */
    fun submitDeliveryDataWithKoli(
        request: SubmitDeliveryRequest,
        noBtt: String
    ): Flow<com.dakotagroupstaff.data.Result<SubmitDeliveryData>> = flow {
        emit(com.dakotagroupstaff.data.Result.Loading)
        try {
            val pt = userPreferences.getPt().first()
            
            // Load koli data from DataStore for this BTT
            val scannedKoliIds = userPreferences.getScannedKoliForBtt(noBtt)
            
            // Build koliData list if there are scanned koli
            val koliData = if (scannedKoliIds.isNotEmpty()) {
                scannedKoliIds.map { koliId ->
                    mapOf("koliId" to koliId)
                }
            } else {
                emptyList()
            }
            
            // Create request with koli data
            val requestWithKoli = request.copy(koliData = koliData)
            
            val response = apiService.submitDeliveryData(pt = pt, request = requestWithKoli)
            
            if (response.success && response.data != null) {
                // Clear koli data from DataStore on success
                userPreferences.clearScannedKoliForBtt(noBtt)
                emit(com.dakotagroupstaff.data.Result.Success(response.data))
            } else {
                emit(com.dakotagroupstaff.data.Result.Error(response.message ?: "Gagal mengirim data"))
            }
        } catch (e: Exception) {
            emit(com.dakotagroupstaff.data.Result.Error(e.message ?: "Gagal mengirim data"))
        }
    }
    
    /**
     * Get scanned koli barcodes for a specific BTT from DataStore
     */
    suspend fun getScannedKoliForBtt(noBtt: String): Set<String> {
        return userPreferences.getScannedKoliForBtt(noBtt)
    }
    
    /**
     * Clear scanned koli data for a specific BTT from DataStore
     */
    suspend fun clearScannedKoliForBtt(noBtt: String) {
        userPreferences.clearScannedKoliForBtt(noBtt)
    }
}
