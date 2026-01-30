package com.dakotagroupstaff.data.remote.response

import com.google.gson.annotations.SerializedName

/**
 * Standard API Response wrapper from backend
 * Follows ResponseHandler format from DakotaGroupApps-Backend
 * 
 * Success response format:
 * {
 *   "success": true,
 *   "message": "...",
 *   "data": {...},
 *   "timestamp": "..."
 * }
 * 
 * Error response format:
 * {
 *   "success": false,
 *   "error": {
 *     "message": "...",
 *     "statusCode": 400,
 *     "details": "...",
 *     "timestamp": "..."
 *   }
 * }
 */
data class ApiResponse<T>(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String? = null, // Only present in success responses
    
    @SerializedName("data")
    val data: T? = null,
    
    @SerializedName("error")
    val error: ErrorResponse? = null, // Only present in error responses
    
    @SerializedName("timestamp")
    val timestamp: String? = null
) {
    /**
     * Get the appropriate message from either success or error response
     */
    fun getResponseMessage(): String {
        return message ?: error?.message ?: "Unknown error"
    }
}

data class ErrorResponse(
    @SerializedName("message")
    val message: String,
    
    @SerializedName("statusCode")
    val statusCode: Int,
    
    @SerializedName("details")
    val details: String? = null,
    
    @SerializedName("timestamp")
    val timestamp: String
)
