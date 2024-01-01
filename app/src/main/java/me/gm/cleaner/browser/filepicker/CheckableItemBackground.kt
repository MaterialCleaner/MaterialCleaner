package me.gm.cleaner.browser.filepicker

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.FloatRange
import androidx.appcompat.graphics.drawable.AnimatedStateListDrawableCompat
import me.gm.cleaner.util.colorPrimary
import me.gm.cleaner.util.shortAnimTime
import kotlin.math.roundToInt

object CheckableItemBackground {
    // We need an <animated-selector> (AnimatedStateListDrawable) with an item drawable referencing
    // a ColorStateList that adds an alpha to our primary color, which is a theme attribute. We
    // currently don't have any compat handling for ColorStateList inside drawable on pre-23,
    // although AppCompatResources do have compat handling for inflating ColorStateList directly.
    // Note that the <selector>s used in Material Components are color resources, so they are
    // inflated as ColorStateList instead of StateListDrawable and don't have this problem.
    @SuppressLint("RestrictedApi")
    fun create(context: Context): Drawable =
        AnimatedStateListDrawableCompat().apply {
            val shortAnimTime = context.shortAnimTime.toInt()
            setEnterFadeDuration(shortAnimTime)
            setExitFadeDuration(shortAnimTime)
            val checkedColor = modulatedAlpha(context.colorPrimary, 0.12f)
            addState(intArrayOf(android.R.attr.state_checked), ColorDrawable(checkedColor))
            addState(intArrayOf(), ColorDrawable(Color.TRANSPARENT))
        }

    private fun modulatedAlpha(
        color: Int,
        @FloatRange(from = 0.0, to = 1.0) alphaModulation: Float
    ): Int {
        val alpha = (Color.alpha(color) * alphaModulation).roundToInt()
        return ((alpha shl 24) or (color and 0x00FFFFFF))
    }
}
