package com.dakotagroupstaff.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.dakotagroupstaff.BuildConfig
import com.dakotagroupstaff.R
import com.dakotagroupstaff.data.local.preferences.UserPreferences
import com.dakotagroupstaff.data.local.preferences.dataStore
import com.dakotagroupstaff.data.local.room.AppDatabase
import com.dakotagroupstaff.data.remote.response.SubmitDeliveryRequest
import com.dakotagroupstaff.data.remote.retrofit.ApiConfig
import com.dakotagroupstaff.data.repository.DeliveryRepository
import com.dakotagroupstaff.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.withContext

class BatchSubmitBttWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    private val notificationId = 1001
    private val channelId = "btt_batch_submission"
    
    companion object {
        private const val TAG = "BatchSubmitBttWorker"
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Starting BatchSubmitBttWorker")
            }
            
            // Check network availability (basic check)
            if (!NetworkUtils.isNetworkAvailable(applicationContext)) {
                Log.e(TAG, "Network not available")
                return@withContext Result.failure()
            }
            
            // Initialize dependencies using local creation (could use Koin if preferred)
            val userPreferences = UserPreferences.getInstance(applicationContext.dataStore)
            val apiService = ApiConfig.getApiService()
            val database = AppDatabase.getDatabase(applicationContext)
            val repository = DeliveryRepository(
                apiService = apiService,
                userPreferences = userPreferences,
                deliveryListDao = database.deliveryListDao()
            )
            
            // Get user NIP
            val userNip = userPreferences.getNip().first()
            if (userNip.isEmpty()) {
                Log.e(TAG, "User NIP is empty")
                return@withContext Result.failure()
            }
            
            // Get all sent deliveries from local storage
            val sentDeliveries = repository.getSentDeliveries().first()
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Found ${sentDeliveries.size} BTT to process")
            }
            
            if (sentDeliveries.isEmpty()) {
                return@withContext Result.success()
            }
            
            // Try to set foreground (only works when app is in background)
            // If this fails (app is in foreground), continue without crashing
            try {
                setForeground(createForegroundInfo(0, sentDeliveries.size))
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Foreground service started successfully")
                }
            } catch (e: IllegalStateException) {
                // App is in foreground, foreground service is not needed
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "App is in foreground, skipping foreground service")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set foreground: ${e.message}")
            }
            
            var successCount = 0
            var failCount = 0
            
            // Process each BTT
            sentDeliveries.forEachIndexed { index, entity ->
                try {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Processing BTT ${entity.noBtt} (${index + 1}/${sentDeliveries.size})")
                    }
                    
                    // Update progress notification (only if foreground service is active)
                    try {
                        setForeground(createForegroundInfo(index + 1, sentDeliveries.size))
                    } catch (e: IllegalStateException) {
                        // Foreground service not active, show regular progress notification instead
                        showProgressNotification(index + 1, sentDeliveries.size)
                    } catch (e: Exception) { 
                        // Ignore other errors
                    }
                    
                    // Prepare request
                    val request = SubmitDeliveryRequest(
                        noLoper = entity.noLoper,
                        noBtt = entity.noBtt,
                        bTerimaYn = "Y",
                        reasonId = "",
                        bPenerima = entity.penerima,
                        nip = userNip,
                        lat = entity.latitude ?: "0.0",
                        lon = entity.longitude ?: "0.0",
                        foto = entity.fotoBase64 ?: "",
                        ttd = entity.ttdBase64 ?: ""
                    )
                    
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Submitting BTT ${entity.noBtt} - NoLoper: ${entity.noLoper}, Penerima: ${entity.penerima}")
                    }
                    
                    // Submit to API and wait for FINAL result (skip Loading state)
                    // Using last() to get the final emission (Success or Error)
                    val result = repository.submitDeliveryData(request)
                        .last() // Wait for the final emission
                    
                    when (result) {
                        is com.dakotagroupstaff.data.Result.Success -> {
                            if (BuildConfig.DEBUG) {
                                Log.d(TAG, "✅ Successfully submitted BTT ${entity.noBtt}")
                            }
                            // Delete from local storage on success
                            repository.removeSentDelivery(entity.noBtt)
                            successCount++
                        }
                        is com.dakotagroupstaff.data.Result.Error -> {
                            Log.e(TAG, "❌ Failed to submit BTT ${entity.noBtt}: ${result.message}")
                            failCount++
                        }
                        else -> {
                            // This shouldn't happen with last(), but handle it anyway
                            Log.w(TAG, "⚠️ Unexpected result state for BTT ${entity.noBtt}")
                            failCount++
                        }
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "💥 Exception processing BTT ${entity.noBtt}: ${e.message}", e)
                    failCount++
                }
            }
            
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Process completed. Success: $successCount, Failed: $failCount")
            }
            
            // Show completion notification
            showCompletionNotification(successCount, failCount, sentDeliveries.size)
            
            // Return success if at least some were processed, or if the process finished without a crash
            // We return success to prevent WorkManager from automatically retrying the whole batch
            // The failed ones remain in the database anyway
            return@withContext Result.success()
            
        } catch (e: Exception) {
            Log.e(TAG, "Critical error in worker: ${e.message}", e)
            return@withContext Result.failure()
        }
    }
    
    private fun createForegroundInfo(progress: Int, total: Int): ForegroundInfo {
        createNotificationChannel()
        
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Mengirim BTT ke Server")
            .setContentText("Memproses $progress dari $total BTT...")
            .setSmallIcon(R.drawable.ic_delivery)
            .setProgress(total, progress, false)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }
    
    private fun showCompletionNotification(success: Int, failed: Int, total: Int) {
        val message = if (failed == 0) {
            "Berhasil mengirim $success dari $total BTT"
        } else {
            "Berhasil: $success, Gagal: $failed dari $total BTT"
        }
        
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Proses Selesai")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_check_circle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(notificationId + 1, notification)
    }
    
    /**
     * Show progress notification for foreground execution
     * (when foreground service cannot be started because app is already in foreground)
     */
    private fun showProgressNotification(progress: Int, total: Int) {
        createNotificationChannel()
        
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Mengirim BTT ke Server")
            .setContentText("Memproses $progress dari $total BTT...")
            .setSmallIcon(R.drawable.ic_delivery)
            .setProgress(total, progress, false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
            .build()
        
        notificationManager.notify(notificationId, notification)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Batch BTT Submission",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Menampilkan progress pengiriman BTT ke server"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
