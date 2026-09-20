package com.myplayer.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.ImageView

object Ui {
    val RED = 0xFFE53935.toInt()
    val BG = 0xFF141414.toInt()
    val NAV = 0xFF212121.toInt()
    val GRAY = 0xFF9E9E9E.toInt()
    val LINE = 0xFF3A3A3A.toInt()
    val MED: Typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    val LIGHT: Typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
    private val setKeys = arrayOf("g_vol", "g_bri", "g_seek", "g_dbl", "resume")
    private val setNames = arrayOf("Volume gesture", "Brightness gesture", "Seek gesture", "Double tap to seek", "Resume from last position")

    fun dp(c: Context, v: Int) = (v * c.resources.displayMetrics.density).toInt()

    fun icon(c: Context, res: Int, pad: Int = 12, color: Int = Color.WHITE): ImageView {
        val v = ImageView(c)
        v.setImageResource(res)
        v.setColorFilter(color)
        val p = dp(c, pad)
        v.setPadding(p, p, p, p)
        return v
    }

    fun shape(c: Context, color: Int, r: Int): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(color)
        g.cornerRadius = dp(c, r).toFloat()
        return g
    }

    fun circle(color: Int): GradientDrawable {
        val g = GradientDrawable()
        g.shape = GradientDrawable.OVAL
        g.setColor(color)
        return g
    }

    fun border(c: Context, r: Int): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(0)
        g.cornerRadius = dp(c, r).toFloat()
        g.setStroke(dp(c, 1), LINE)
        return g
    }

    fun wordmark(): CharSequence {
        val s = SpannableString("eve")
        s.setSpan(ForegroundColorSpan(Color.WHITE), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        s.setSpan(ForegroundColorSpan(RED), 1, 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        s.setSpan(ForegroundColorSpan(Color.WHITE), 2, 3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return s
    }

    fun fmt(ms: Long): String {
        val s = ms / 1000
        val h = s / 3600
        val m = (s % 3600) / 60
        val c = s % 60
        return if (h > 0) String.format("%d:%02d:%02d", h, m, c) else String.format("%d:%02d", m, c)
    }

    fun short(ms: Long): String = if (ms < 60000) (ms / 1000).toString() + "s" else (ms / 60000).toString() + "min"

    fun noExt(s: String) = s.substringBeforeLast('.')

    fun settingsDialog(a: Activity) {
        val on = BooleanArray(setKeys.size) { Lib.on(a, setKeys[it]) }
        AlertDialog.Builder(a).setTitle("Settings")
            .setMultiChoiceItems(setNames, on) { _, i, b -> Lib.setOn(a, setKeys[i], b) }
            .setPositiveButton("OK", null).show()
    }
}

class LevelBar(c: Context) : View(c) {
    var f1 = 0f
    var f2 = 0f
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(cv: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val clip = Path()
        clip.addRoundRect(0f, 0f, w, h, w / 2, w / 2, Path.Direction.CW)
        cv.clipPath(clip)
        p.color = 0xFF9A9A9A.toInt()
        cv.drawRect(0f, 0f, w, h, p)
        p.color = Color.WHITE
        cv.drawRect(0f, h * (1 - f1), w, h, p)
        if (f2 > f1) {
            p.color = Ui.RED
            cv.drawRect(0f, h * (1 - f2), w, h * (1 - f1), p)
        }
    }
}
