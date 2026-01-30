package com.dakotagroupstaff.ui.operasional.loper

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class SignatureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 5f
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    private val path = Path()
    private val paths = mutableListOf<Pair<Path, Paint>>()
    private var currentX = 0f
    private var currentY = 0f

    var isEmpty = true
        private set

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                path.moveTo(x, y)
                currentX = x
                currentY = y
                isEmpty = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                path.quadTo(currentX, currentY, (x + currentX) / 2, (y + currentY) / 2)
                currentX = x
                currentY = y
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                path.lineTo(currentX, currentY)
                paths.add(Pair(Path(path), Paint(paint)))
                path.reset()
                invalidate()
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw all saved paths
        for ((savedPath, savedPaint) in paths) {
            canvas.drawPath(savedPath, savedPaint)
        }
        
        // Draw current path
        canvas.drawPath(path, paint)
    }

    fun clear() {
        paths.clear()
        path.reset()
        isEmpty = true
        invalidate()
    }

    fun getSignatureBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        draw(canvas)
        return bitmap
    }
}
