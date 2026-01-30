package com.dakotagroupstaff.ui.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.dakotagroupstaff.R
import com.dakotagroupstaff.databinding.DialogPhotoViewerBinding

/**
 * Full-screen photo viewer dialog with zoom and pan support
 * Uses PhotoView library for gesture-based image manipulation
 */
class PhotoViewerDialog(
    context: Context,
    private val imageUrl: String
) : Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen) {

    private lateinit var binding: DialogPhotoViewerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Remove window title and make fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        
        binding = DialogPhotoViewerBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)
        
        // Set window to fullscreen
        window?.apply {
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
        
        setupPhotoView()
        setupCloseButton()
    }

    private fun setupPhotoView() {
        // Load image with Glide into PhotoView
        Glide.with(context)
            .load(imageUrl)
            .placeholder(R.drawable.ic_launcher_foreground)
            .error(R.drawable.ic_launcher_foreground)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    // Image load failed - keep placeholder
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    // Image loaded successfully
                    return false
                }
            })
            .into(binding.photoView)
    }

    private fun setupCloseButton() {
        binding.fabClose.setOnClickListener {
            dismiss()
        }
    }
}
