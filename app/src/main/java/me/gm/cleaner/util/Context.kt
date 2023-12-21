package me.gm.cleaner.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.TextAppearanceSpan
import android.widget.Toast
import androidx.annotation.AnyRes
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.core.content.getSystemService

fun Context.startActivitySafe(intent: Intent) {
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
    }
}

fun Context.buildStyledTitle(
    text: CharSequence,
    style: Int = com.google.android.material.R.attr.textAppearanceBody2,
    color: Int? = colorPrimary
): SpannableStringBuilder = SpannableStringBuilder(text).apply {
    setSpan(
        TextAppearanceSpan(this@buildStyledTitle, getResourceIdByAttr(style)), 0, length,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    if (color != null) {
        setSpan(ForegroundColorSpan(color), 0, length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
    }
}

inline fun <T : TypedArray, R> T.use(block: (T) -> R): R = try {
    block(this)
} finally {
    recycle()
}

fun Context.getDimenByAttr(@AttrRes attr: Int): Float =
    obtainStyledAttributes(intArrayOf(attr)).use {
        it.getDimension(0, 0F)
    }

@ColorInt
fun Context.getColorByAttr(@AttrRes attr: Int): Int? =
    obtainStyledAttributes(intArrayOf(attr)).use {
        it.getColorStateList(0)?.defaultColor
    }

fun Context.getColorStateListByAttr(@AttrRes attr: Int): ColorStateList? =
    obtainStyledAttributes(intArrayOf(attr)).use {
        it.getColorStateList(0)
    }

fun Context.getDrawableByAttr(@AttrRes attr: Int): Drawable? =
    obtainStyledAttributes(intArrayOf(attr)).use {
        it.getDrawable(0)
    }

@AnyRes
fun Context.getResourceIdByAttr(@AttrRes attr: Int, index: Int = 0): Int =
    obtainStyledAttributes(intArrayOf(attr)).use {
        it.getResourceId(index, 0)
    }

fun Context.dpToPx(dps: Int): Int {
    val density = resources.displayMetrics.density
    return (dps * density + 0.5F).toInt()
}

fun Context.pxToDp(px: Int): Int {
    val density = resources.displayMetrics.density
    return (px / density).toInt()
}

val Context.hasWifiTransport: Boolean
    get() {
        val connManager = getSystemService<ConnectivityManager>()!!
        val capabilities = connManager.getNetworkCapabilities(connManager.activeNetwork)
        return capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    }
