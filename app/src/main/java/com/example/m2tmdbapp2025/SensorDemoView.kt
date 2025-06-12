package com.example.m2tmdbapp2025

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.hardware.SensorEvent
import android.util.AttributeSet
import android.view.View
import kotlin.math.*

class SensorDemoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val DIAL_SCALE = 0.9f

    private var dialCaption: String = ""

    private var dialColor = Color.BLUE

    private val dialNeedleColors: Array<String> =
        context.resources.getStringArray(R.array.dial_needle_colors)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 4f * resources.displayMetrics.density
        textAlign = Paint.Align.CENTER
        textSize = 14f * resources.displayMetrics.density
    }

    private var sensorEvent: SensorEvent? = null

    fun setCaption(caption: String) {
        dialCaption = caption
        invalidate()
    }

    fun setSensorEvent(event: SensorEvent?) {
        sensorEvent = event
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        paint.style = Paint.Style.FILL
        paint.color = dialColor
        val yCaption = -paint.ascent()
        canvas.drawText(dialCaption, w/2, yCaption, paint)

        val captionHeight = paint.descent() + paint.ascent().unaryMinus()
        val dialAreaTop = captionHeight + 8f * resources.displayMetrics.density
        val dialAreaHeight = h - dialAreaTop

        val cx = w / 2
        val cy = dialAreaTop + dialAreaHeight / 2
        val radius = min(w, dialAreaHeight) * 0.5f * DIAL_SCALE

        paint.style = Paint.Style.STROKE
        paint.color = dialColor
        canvas.drawCircle(cx, cy, radius, paint)

        sensorEvent?.let { ev ->
            paint.style = Paint.Style.STROKE
            ev.values.forEachIndexed { i, value ->
                if (i >= dialNeedleColors.size) return@forEachIndexed
                paint.color = dialNeedleColors[i].toInt()
                val angle = (value.toDouble() / ev.sensor.maximumRange.toDouble()) * 2 * Math.PI
                val x2 = cx + radius * sin(angle).toFloat()
                val y2 = cy - radius * cos(angle).toFloat()
                canvas.drawLine(cx, cy, x2, y2, paint)
            }
        }

        val maxRangeText = sensorEvent
            ?.sensor
            ?.maximumRange
            ?.toString()
            ?: "unknown maxrange"

        paint.style = Paint.Style.FILL
        paint.color = dialColor

        val textY = cy + radius / 2 - (paint.descent() + paint.ascent()) / 2
        canvas.drawText(maxRangeText, cx, textY, paint)
    }
}
