package com.dakotagroupstaff.ui.settings

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.dakotagroupstaff.R

class DataUsageChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.primary)
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_primary)
        textSize = 32f
        textAlign = Paint.Align.CENTER
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_secondary)
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    private var dataList = emptyList<MonthlyDataUsage>()

    fun setData(data: List<MonthlyDataUsage>) {
        dataList = data
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (dataList.isEmpty()) return

        val maxValue = dataList.maxOfOrNull { it.usageMB } ?: 1.0
        val chartHeight = height - 150f
        val barWidth = width / (dataList.size * 2f)
        val spacing = barWidth / 2f

        dataList.forEachIndexed { index, data ->
            val left = (index * (barWidth + spacing)) + spacing
            val barHeight = ((data.usageMB / maxValue) * chartHeight).toFloat()
            val top = chartHeight - barHeight + 50f
            val right = left + barWidth
            val bottom = chartHeight + 50f

            // Draw bar
            canvas.drawRoundRect(
                RectF(left, top, right, bottom),
                16f, 16f,
                barPaint
            )

            // Draw value
            canvas.drawText(
                String.format("%.1f MB", data.usageMB),
                left + barWidth / 2f,
                top - 10f,
                textPaint
            )

            // Draw month label
            canvas.drawText(
                data.monthName,
                left + barWidth / 2f,
                bottom + 40f,
                labelPaint
            )
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = 600
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> minOf(desiredHeight, heightSize)
            else -> desiredHeight
        }

        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), height)
    }
}
