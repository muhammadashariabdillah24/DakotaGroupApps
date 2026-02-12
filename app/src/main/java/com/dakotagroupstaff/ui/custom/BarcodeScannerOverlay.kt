package com.dakotagroupstaff.ui.custom

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * Custom overlay for barcode scanner
 * Draws corner-only borders with color animation
 */
class BarcodeScannerOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        isAntiAlias = true
    }

    private var scanColor = Color.WHITE
    private val cornerLength = 60f // Length of corner lines

    /**
     * Show success animation (green flash)
     */
    fun showSuccess() {
        scanColor = Color.GREEN
        invalidate()

        // Reset to white after 200ms
        postDelayed({
            scanColor = Color.WHITE
            invalidate()
        }, 200)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        paint.color = scanColor

        val centerX = width / 2f
        val centerY = height / 2f
        val scanBoxSize = 250f // Size of the scan area

        val left = centerX - scanBoxSize
        val top = centerY - scanBoxSize
        val right = centerX + scanBoxSize
        val bottom = centerY + scanBoxSize

        // Draw top-left corner
        canvas.drawLine(left, top, left + cornerLength, top, paint) // horizontal
        canvas.drawLine(left, top, left, top + cornerLength, paint) // vertical

        // Draw top-right corner
        canvas.drawLine(right - cornerLength, top, right, top, paint) // horizontal
        canvas.drawLine(right, top, right, top + cornerLength, paint) // vertical

        // Draw bottom-left corner
        canvas.drawLine(left, bottom - cornerLength, left, bottom, paint) // vertical
        canvas.drawLine(left, bottom, left + cornerLength, bottom, paint) // horizontal

        // Draw bottom-right corner
        canvas.drawLine(right, bottom - cornerLength, right, bottom, paint) // vertical
        canvas.drawLine(right - cornerLength, bottom, right, bottom, paint) // horizontal
    }
}
