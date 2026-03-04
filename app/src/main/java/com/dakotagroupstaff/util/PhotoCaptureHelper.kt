package com.dakotagroupstaff.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Helper class for photo capture functionality
 * Handles camera capture, image processing, and conversion to base64
 */
class PhotoCaptureHelper(
    private val activity: AppCompatActivity
) {
    
    private var currentPhotoFile: File? = null
    private var onPhotoCapture: ((String) -> Unit)? = null
    
    // Camera launcher
    private val takePictureLauncher: ActivityResultLauncher<Uri> = 
        activity.registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                currentPhotoFile?.let { file ->
                    val base64 = processImageToBase64(file)
                    onPhotoCapture?.invoke(base64)
                }
            }
        }
    
    /**
     * Launch camera to take photo
     */
    fun takePhoto(onCapture: (String) -> Unit) {
        this.onPhotoCapture = onCapture
        
        try {
            val photoFile = createImageFile(activity)
            currentPhotoFile = photoFile
            
            val photoUri = FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.fileprovider",
                photoFile
            )
            
            takePictureLauncher.launch(photoUri)
        } catch (e: Exception) {
            e.printStackTrace()
            onCapture.invoke("")
        }
    }
    
    /**
     * Create temporary image file
     */
    private fun createImageFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.cacheDir
        return File.createTempFile(
            "CHECKPOINT_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }
    
    /**
     * Process image file to base64 string
     * - Compress image
     * - Fix rotation
     * - Convert to base64
     */
    private fun processImageToBase64(file: File): String {
        return try {
            // Read bitmap
            var bitmap = BitmapFactory.decodeFile(file.absolutePath)
            
            // Fix rotation
            bitmap = fixImageRotation(file.absolutePath, bitmap)
            
            // Compress bitmap
            bitmap = compressBitmap(bitmap, 800, 800)
            
            // Convert to base64
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val bytes = outputStream.toByteArray()
            
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        } finally {
            // Delete temporary file
            file.delete()
        }
    }
    
    /**
     * Fix image rotation based on EXIF data
     */
    private fun fixImageRotation(path: String, bitmap: Bitmap): Bitmap {
        return try {
            val exif = ExifInterface(path)
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
            }
            
            if (!matrix.isIdentity) {
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }
        } catch (e: IOException) {
            e.printStackTrace()
            bitmap
        }
    }
    
    /**
     * Compress bitmap to target size
     */
    private fun compressBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }
        
        val ratio = Math.min(
            maxWidth.toFloat() / width,
            maxHeight.toFloat() / height
        )
        
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
