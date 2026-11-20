package com.myapplock

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot
import kotlin.math.min

/** 3x3 pattern grid. Returns the dot sequence (0..8) via onComplete. */
class PatternView(ctx: Context) : View(ctx) {

    var onComplete: ((List<Int>) -> Unit)? = null

    private val sel = ArrayList<Int>()
    private val cx = FloatArray(9)
    private val cy = FloatArray(9)
    private var rad = 0f
    private var fx = -1f
    private var fy = -1f
    private var err = false

    private val density = resources.displayMetrics.density
    private val idle = Color.parseColor("#5B6B9A")
    private val blue = Color.parseColor("#22D3EE")
    private val red = Color.parseColor("#FF5C6C")

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = idle }
    private val onPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = blue }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = blue
        strokeWidth = 6f * density
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val cell = min(w, h) / 3f
        rad = cell * 0.12f
        for (i in 0 until 9) {
            val col = i % 3
            val row = i / 3
            cx[i] = (w - 3 * cell) / 2f + cell * (col + 0.5f)
            cy[i] = (h - 3 * cell) / 2f + cell * (row + 0.5f)
        }
    }

    override fun onDraw(canvas: Canvas) {
        val c = if (err) red else blue
        onPaint.color = c
        linePaint.color = c
        for (k in 0 until sel.size - 1) {
            canvas.drawLine(cx[sel[k]], cy[sel[k]], cx[sel[k + 1]], cy[sel[k + 1]], linePaint)
        }
        if (fx >= 0f && sel.isNotEmpty() && !err) {
            val last = sel[sel.size - 1]
            canvas.drawLine(cx[last], cy[last], fx, fy, linePaint)
        }
        for (i in 0 until 9) {
            if (i in sel) canvas.drawCircle(cx[i], cy[i], rad * 1.4f, onPaint)
            else canvas.drawCircle(cx[i], cy[i], rad, dotPaint)
        }
    }

    private fun hit(x: Float, y: Float) {
        for (i in 0 until 9) {
            if (i !in sel && hypot(x - cx[i], y - cy[i]) < rad * 2.4f) {
                sel.add(i)
                break
            }
        }
    }

    override fun onTouchEvent(e: android.view.MotionEvent): Boolean {
        if (err) return true
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                sel.clear()
                fx = e.x
                fy = e.y
                hit(e.x, e.y)
            }
            MotionEvent.ACTION_MOVE -> {
                fx = e.x
                fy = e.y
                hit(e.x, e.y)
            }
            MotionEvent.ACTION_UP -> {
                fx = -1f
                fy = -1f
                if (sel.isNotEmpty()) onComplete?.invoke(sel.toList())
            }
            MotionEvent.ACTION_CANCEL -> {
                sel.clear()
                fx = -1f
                fy = -1f
            }
        }
        invalidate()
        return true
    }

    fun reset() {
        sel.clear()
        err = false
        fx = -1f
        fy = -1f
        invalidate()
    }

    fun flashError() {
        err = true
        invalidate()
        postDelayed({ reset() }, 600)
    }
}
