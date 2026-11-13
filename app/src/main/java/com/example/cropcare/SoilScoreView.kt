package com.example.cropcare

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat
import java.util.Locale

/** Displays only the server score; an unavailable assessment never becomes a zero. */
class SoilScoreView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val scoreTypeface = ResourcesCompat.getFont(context, R.font.montserrat_bold)
    private var score: Double? = null
    fun setScore(value: Double?) {
        score = value?.takeIf { it.isFinite() && it in 0.0..100.0 }
        contentDescription = score?.let { "Provisional soil score ${String.format(Locale.US,"%.1f",it)} out of 100" } ?: "Soil score unavailable"
        invalidate()
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = resources.displayMetrics.density
        val size = minOf(width, height).toFloat(); val cx = width / 2f; val cy = height / 2f
        val radius = size / 2f - 9 * d
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 8 * d; paint.strokeCap = Paint.Cap.ROUND
        paint.color = Color.rgb(227, 237, 221); canvas.drawCircle(cx, cy, radius, paint)
        score?.let {
            paint.color = if (it < 40) Color.rgb(181, 130, 33) else Color.rgb(41, 121, 57)
            canvas.drawArc(RectF(cx-radius, cy-radius, cx+radius, cy+radius), -90f, (it * 3.6).toFloat(), false, paint)
        }
        paint.style = Paint.Style.FILL; paint.color = Color.rgb(30, 71, 42)
        paint.textAlign = Paint.Align.CENTER; paint.typeface = scoreTypeface ?: Typeface.DEFAULT_BOLD
        paint.textSize = 28 * resources.displayMetrics.scaledDensity
        canvas.drawText(score?.let { String.format(Locale.US, "%.1f", it).removeSuffix(".0") } ?: "—", cx, cy + 2 * d, paint)
        paint.textSize = 11 * resources.displayMetrics.scaledDensity; paint.color = Color.rgb(109, 126, 112)
        canvas.drawText("out of 100", cx, cy + 23 * d, paint)
    }
}
