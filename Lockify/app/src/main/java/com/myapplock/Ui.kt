package com.myapplock

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** Shared colors and small view helpers for the Lockify look. */
object Ui {
    val BG = Color.parseColor("#050B24")
    val CARD = Color.parseColor("#0E1A3F")
    val CARD_STROKE = Color.parseColor("#1D2F66")
    val ACCENT = Color.parseColor("#2F7BFF")
    val CYAN = Color.parseColor("#22D3EE")
    val TEXT = Color.parseColor("#FFFFFF")
    val MUTED = Color.parseColor("#8FA0CC")
    val OK = Color.parseColor("#2EE6A6")
    val BAD = Color.parseColor("#FF5C6C")

    const val TAGLINE = "World's Safest & Ads-Free App Lock"
    const val DEV_NAME = "Mukidul Islam Zihad"
    const val DEV_ROLE = "Cybersecurity, OSINT & SOC Analyst — Dhaka, Bangladesh"
    const val DEV_URL = "https://mukidulislamzihad.github.io/my-portfolio/"

    fun dp(ctx: Context, v: Int): Int = (v * ctx.resources.displayMetrics.density).toInt()

    fun rounded(fill: Int, radiusDp: Int, ctx: Context, stroke: Int? = null): GradientDrawable =
        GradientDrawable().apply {
            setColor(fill)
            cornerRadius = dp(ctx, radiusDp).toFloat()
            if (stroke != null) setStroke(dp(ctx, 1), stroke)
        }

    fun gradient(ctx: Context, radiusDp: Int): GradientDrawable =
        GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(ACCENT, CYAN)).apply {
            cornerRadius = dp(ctx, radiusDp).toFloat()
        }

    fun card(ctx: Context): LinearLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(CARD, 18, ctx, CARD_STROKE)
        setPadding(dp(ctx, 16), dp(ctx, 14), dp(ctx, 16), dp(ctx, 14))
    }

    fun text(ctx: Context, s: String, size: Float, color: Int = TEXT, bold: Boolean = false): TextView =
        TextView(ctx).apply {
            text = s
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    fun primaryButton(ctx: Context, s: String): Button = Button(ctx).apply {
        text = s
        isAllCaps = false
        setTextColor(Color.WHITE)
        textSize = 15f
        setTypeface(typeface, Typeface.BOLD)
        background = gradient(ctx, 14)
        stateListAnimator = null
        minHeight = dp(ctx, 48)
    }

    fun ghostButton(ctx: Context, s: String): Button = Button(ctx).apply {
        text = s
        isAllCaps = false
        setTextColor(CYAN)
        textSize = 14f
        background = rounded(Color.TRANSPARENT, 14, ctx, ACCENT)
        stateListAnimator = null
        minHeight = dp(ctx, 44)
    }

    fun smallButton(ctx: Context, s: String): Button = ghostButton(ctx, s).apply {
        textSize = 13f
        minHeight = dp(ctx, 36)
        minimumHeight = dp(ctx, 36)
        setPadding(dp(ctx, 14), 0, dp(ctx, 14), 0)
    }

    fun centered(ctx: Context): LinearLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setBackgroundColor(BG)
        setPadding(dp(ctx, 20), dp(ctx, 20), dp(ctx, 20), dp(ctx, 20))
    }
}
