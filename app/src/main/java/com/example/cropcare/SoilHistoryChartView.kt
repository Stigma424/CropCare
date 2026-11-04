package com.example.cropcare

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import java.text.SimpleDateFormat
import java.util.*

class SoilHistoryChartView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private var records: List<SensorHistoryReading> = emptyList()
    private var metric = 0
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    fun show(readings: List<SensorHistoryReading>, selectedMetric: Int) {
        records = readings.filter { it.recordedAtMs > 0 }.sortedBy { it.recordedAtMs }; metric = selectedMetric
        contentDescription = "Graph of loaded readings. ${records.count { HistoryFilter.value(it, metric) != null }} measured points. Exact measurements are in the history cards."
        invalidate()
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = resources.displayMetrics.density
        val left = 65 * d; val right = width - 12 * d; val top = 20 * d; val bottom = height - 38 * d
        paint.textSize = 12 * resources.displayMetrics.scaledDensity; paint.color = Color.DKGRAY
        val values = records.mapNotNull { HistoryFilter.value(it, metric) }
        if (values.isEmpty() || right <= left || bottom <= top) {
            canvas.drawText("No timestamped graph data in loaded selection", 8 * d, 40 * d, paint); return
        }
        val min = values.minOrNull()!!; val max = values.maxOrNull()!!
        val pad = maxOf((max - min) * 0.15, kotlin.math.abs(max) * 0.02, 0.5)
        val low = min - pad; val high = max + pad
        val first = records.first().recordedAtMs; val last = records.last().recordedAtMs
        for (i in 0..4) {
            val y = bottom - (bottom - top) * i / 4
            paint.color = Color.LTGRAY; paint.strokeWidth = d
            canvas.drawLine(left, y, right, y, paint)
            paint.color = Color.DKGRAY
            canvas.drawText(String.format(Locale.US, "%.1f", low + (high - low) * i / 4), 0f, y, paint)
        }
        var previous: Pair<Float, Float>? = null
        records.forEach { row ->
            val value = HistoryFilter.value(row, metric)
            if (value == null) previous = null else {
                val x = if (last == first) (left + right) / 2 else left + ((row.recordedAtMs - first).toDouble() / (last - first) * (right - left)).toFloat()
                val y = bottom - ((value - low) / (high - low) * (bottom - top)).toFloat()
                paint.color = Color.rgb(25, 135, 84); paint.strokeWidth = 2 * d
                previous?.let { canvas.drawLine(it.first, it.second, x, y, paint) }
                canvas.drawCircle(x, y, 3 * d, paint); previous = x to y
            }
        }
        paint.color = Color.DKGRAY
        val fmt = SimpleDateFormat("MMM d HH:mm", Locale.US).apply { timeZone = TimeZone.getTimeZone("Asia/Manila") }
        canvas.drawText(fmt.format(Date(first)), left, height - 12 * d, paint)
        val end = fmt.format(Date(last))
        if (first != last) canvas.drawText(end, right - paint.measureText(end), height - 12 * d, paint)
    }
}
