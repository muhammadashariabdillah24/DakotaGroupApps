package com.dakotagroupstaff.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.InputStream

object ImageCompressor {
    
    /**
     * Compress and convert Bitmap to Base64
     * 
     * @param bitmap Original bitmap
     * @param maxWidth Maximum width (default 800px)
     * @param maxHeight Maximum height (default 800px)
     * @param quality JPEG quality (0-100, default 75)
     * @return Base64 encoded string
     */
    fun bitmapToBase64(
        bitmap: Bitmap,
        maxWidth: Int = 800,
        maxHeight: Int = 800,
        quality: Int = 75
    ): String {
        // Resize bitmap if needed
        val resizedBitmap = resizeBitmap(bitmap, maxWidth, maxHeight)
        
        // Compress to JPEG
        val outputStream = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val byteArray = outputStream.toByteArray()
        
        // Convert to Base64
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
    
    /**
     * Convert Base64 string back to Bitmap
     * 
     * @param base64String Base64 encoded image string
     * @return Bitmap or null if decoding fails
     */
    fun base64ToBitmap(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            android.util.Log.e("ImageCompressor", "Error decoding Base64 to Bitmap: ${e.message}", e)
            null
        }
    }
    
    /**
     * Load image from URI, compress, and convert to Base64
     * Handles rotation correction from EXIF data
     * 
     * @param inputStream InputStream from ContentResolver
     * @param maxWidth Maximum width (default 800px)
     * @param maxHeight Maximum height (default 800px)
     * @param quality JPEG quality (0-100, default 75)
     * @return Base64 encoded string
     */
    fun uriToBase64(
        inputStream: InputStream,
        maxWidth: Int = 800,
        maxHeight: Int = 800,
        quality: Int = 75
    ): String {
        // Decode image
        val bitmap = BitmapFactory.decodeStream(inputStream)
        
        // Compress and convert
        return bitmapToBase64(bitmap, maxWidth, maxHeight, quality)
    }
    
    /**
     * Resize bitmap while maintaining aspect ratio
     * 
     * @param bitmap Original bitmap
     * @param maxWidth Maximum width
     * @param maxHeight Maximum height
     * @return Resized bitmap
     */
    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        // Calculate scale factor
        val scale = minOf(
            maxWidth.toFloat() / width,
            maxHeight.toFloat() / height,
            1.0f  // Don't upscale if image is smaller
        )
        
        // If no scaling needed, return original
        if (scale >= 1.0f) {
            return bitmap
        }
        
        // Calculate new dimensions
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        
        // Create scaled bitmap
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
    
    /**
     * Correct image orientation based on EXIF data
     * Useful for images from camera that might be rotated
     * 
     * @param bitmap Original bitmap
     * @param exif ExifInterface from image file
     * @return Corrected bitmap
     */
    fun correctOrientation(bitmap: Bitmap, exif: ExifInterface): Bitmap {
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        
        return Bitmap.createBitmap(
            bitmap, 0, 0,
            bitmap.width, bitmap.height,
            matrix, true
        )
    }
    
    /**
     * Get estimated size of Base64 string in KB
     * 
     * @param base64String Base64 encoded string
     * @return Size in KB
     */
    fun getBase64SizeKB(base64String: String): Double {
        val bytes = base64String.length * 0.75 // Base64 overhead
        return bytes / 1024.0
    }
    
    /**
     * Validate Base64 string size
     * 
     * @param base64String Base64 encoded string
     * @param maxSizeKB Maximum size in KB
     * @return true if size is within limit
     */
    fun isValidSize(base64String: String, maxSizeKB: Int): Boolean {
        return getBase64SizeKB(base64String) <= maxSizeKB
    }
}
