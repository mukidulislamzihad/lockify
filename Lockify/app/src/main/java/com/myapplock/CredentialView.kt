package com.myapplock

import android.content.Context
import android.view.Gravity
import android.graphics.Color
import android.view.View
import android.widget.Button
import android.graphics.Typeface
import android.widget.LinearLayout
import android.widget.TextView

/** PIN pad or pattern grid depending on mode. The secret string goes to onSubmit. */
class CredentialView(ctx: Context, private val mode: String) : LinearLayout(ctx) {

    var onSubmit: ((String) -> Unit)? = null
    var onShort: (() -> Unit)? = null

    private val sb = StringBuilder()
    private var display: TextView? = null
    private var pattern: PatternView? = null

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        if (mode == Store.MODE_PATTERN) buildPattern() else buildPin()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun buildPin() {
        val t = TextView(context).apply {
            textSize = 28f
            gravity = Gravity.CENTER
            text = " "
            setTextColor(Ui.CYAN)
            letterSpacing = 0.3f
            setPadding(0, dp(8), 0, dp(16))
        }
        display = t
        addView(t)
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "⌫", "0", "OK")
        for (r in 0 until 4) {
            val row = LinearLayout(context)
            for (c in 0 until 3) {
                val k = keys[r * 3 + c]
                val b = Button(context).apply {
                    text = k
                    textSize = if (k == "OK") 16f else 22f
                    isAllCaps = false
                    setTypeface(typeface, Typeface.BOLD)
                    stateListAnimator = null
                    setPadding(0, 0, 0, 0)
                    if (k == "OK") {
                        setTextColor(Color.WHITE)
                        background = Ui.gradient(context, 36)
                    } else {
                        setTextColor(Color.WHITE)
                        background = Ui.rounded(Ui.CARD, 36, context, Ui.CARD_STROKE)
                    }
                    setOnClickListener { press(k) }
                }
                val lp = LayoutParams(dp(72), dp(72))
                lp.setMargins(dp(8), dp(5), dp(8), dp(5))
                row.addView(b, lp)
            }
            addView(row)
        }
    }

    private fun buildPattern() {
        val p = PatternView(context)
        p.onComplete = { list ->
            if (list.size < 4) {
                p.flashError()
                onShort?.invoke()
            } else {
                onSubmit?.invoke(list.joinToString(""))
            }
        }
        pattern = p
        addView(p, LayoutParams(dp(300), dp(300)))
    }

    private fun press(k: String) {
        when (k) {
            "⌫" -> {
                if (sb.isNotEmpty()) sb.deleteCharAt(sb.length - 1)
            }
            "OK" -> {
                if (sb.length >= 4) onSubmit?.invoke(sb.toString()) else onShort?.invoke()
                return
            }
            else -> {
                if (sb.length < 12) sb.append(k)
            }
        }
        render()
    }

    private fun render() {
        display?.text = if (sb.isEmpty()) " " else "●".repeat(sb.length)
    }

    fun clear() {
        sb.clear()
        render()
        pattern?.reset()
    }

    fun flashError() {
        val p = pattern
        if (p != null) p.flashError() else clear()
    }
}
