package androidx.swiperefreshlayout.widget

import android.content.Context
import android.graphics.drawable.ShapeDrawable
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.google.android.material.R
import me.gm.cleaner.util.colorPrimary
import me.gm.cleaner.util.colorSurface

open class ThemedSwipeRefreshLayout(context: Context, attrs: AttributeSet?) :
    SwipeRefreshLayout(context, attrs) {

    fun applyM3Background() {
        val overlayColor = ContextCompat
            .getColorStateList(context, R.color.m3_popupmenu_overlay_color)!!
            .defaultColor
        val backgroundColor = ColorUtils.compositeColors(overlayColor, context.colorSurface)
        (mCircleView.background as ShapeDrawable).paint.color = backgroundColor
    }

    private fun init() {
        setColorSchemeColors(context.colorPrimary)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val child = childView
        if (child != null) {
            measureChild(child, widthMeasureSpec, heightMeasureSpec)
            setMeasuredDimension(
                child.measuredWidth + paddingLeft + paddingRight,
                child.measuredHeight + paddingTop + paddingBottom
            )
        }
    }

    private val childView: View?
        get() {
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                if (child != mCircleView) {
                    return child
                }
            }
            return null
        }

    init {
        init()
    }
}
