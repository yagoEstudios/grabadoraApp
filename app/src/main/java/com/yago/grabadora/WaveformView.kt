package com.yago.grabadora

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.max

class WaveformView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val amps = ArrayList<Float>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF2F6BFF.toInt()
        strokeCap = Paint.Cap.ROUND
    }
    private val barWidth = 7f
    private val gap = 6f

    fun push(level: Float) {
        amps.add(level.coerceIn(0f, 1f))
        val maxBars = (width / (barWidth + gap)).toInt() + 1
        while (maxBars > 0 && amps.size > maxBars) amps.removeAt(0)
        invalidate()
    }

    fun clear() {
        amps.clear()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val h = height.toFloat()
        val cy = h / 2f
        paint.strokeWidth = barWidth
        var x = width - barWidth / 2f
        for (i in amps.indices.reversed()) {
            val a = max(0.02f, amps[i])
            val half = a * (h / 2f) * 0.9f
            canvas.drawLine(x, cy - half, x, cy + half, paint)
            x -= (barWidth + gap)
            if (x < 0) break
        }
    }
}
