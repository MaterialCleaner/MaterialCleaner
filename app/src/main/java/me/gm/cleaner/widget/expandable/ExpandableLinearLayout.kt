package me.gm.cleaner.widget.expandable

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.animation.AnimationUtils
import me.gm.cleaner.util.shortAnimTime
import me.gm.cleaner.widget.AutoCheckLinearLayout

/**
 * Designed for a [android.widget.LinearLayout] that is an item of a [RecyclerView].
 */
class ExpandableLinearLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : AutoCheckLinearLayout(context, attrs, defStyleAttr, defStyleRes) {
    /**
     * true if [ExpandableChild]'s size never change.
     */
    var hasFixedSize: Boolean = false

    override fun setChecked(checked: Boolean) {
        if (animator.isRunning) {
            return
        }
        if (isChecked != checked) {
            refreshExpandState(checked, true)
        }
        super.setChecked(checked)
    }

    fun setCheckedNoAnim(checked: Boolean) {
        if (isChecked != checked) {
            refreshExpandState(checked, false)
        }
        super.setChecked(checked)
    }

    private var animatedValue: Float = 1F
    private var animator: ValueAnimator = ValueAnimator.ofFloat(0F, 1F).apply {
        duration = context.shortAnimTime
        interpolator == AnimationUtils.LINEAR_INTERPOLATOR
        addUpdateListener { valueAnimator ->
            this@ExpandableLinearLayout.animatedValue = valueAnimator.animatedValue as Float
            requestLayout()
        }
    }
    private var lastWidth: Int = 0
    private var lastHeight: Int = 0

    private lateinit var expandableChild: ViewGroup
    private var expandableChildSize: Int = -1
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (!::expandableChild.isInitialized) {
            // only one expandable child is supported
            expandableChild = children.first { it is ExpandableChild } as ViewGroup
        }
        if (animatedValue == 1F) {
            if (expandableChildSize == -1) {
                // measure initial size
                expandableChild.isVisible = true
            }
            if (hasFixedSize && expandableChildSize == -1 ||
                !hasFixedSize && expandableChild.isVisible
            ) {
                // measure and save expandable child size
                measureChildWithMargins(expandableChild, widthMeasureSpec, 0, heightMeasureSpec, 0)
                expandableChildSize = if (orientation == VERTICAL) {
                    expandableChild.measuredHeight
                } else {
                    expandableChild.measuredWidth
                }
            }
            expandableChild.isVisible = isChecked
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (animatedValue == 1F) {
            lastWidth = measuredWidth
            lastHeight = measuredHeight
        } else {
            // animate
            if (orientation == VERTICAL) {
                val height = if (isChecked) lastHeight + expandableChildSize * animatedValue
                else lastHeight - expandableChildSize * animatedValue
                setMeasuredDimension(
                    measuredWidthAndState,
                    resolveSizeAndState(
                        height.toInt(), MeasureSpec.UNSPECIFIED,
                        measuredState shl MEASURED_HEIGHT_STATE_SHIFT
                    )
                )
            } else {
                val width = if (isChecked) lastWidth + expandableChildSize * animatedValue
                else lastWidth - expandableChildSize * animatedValue
                setMeasuredDimension(
                    resolveSizeAndState(width.toInt(), MeasureSpec.UNSPECIFIED, measuredState),
                    measuredHeightAndState
                )
            }
        }
    }

    private fun refreshExpandState(checked: Boolean, animate: Boolean) {
        if (!::expandableChild.isInitialized) {
            return
        }
        if (checked) {
            expandableChild.isVisible = true
        }
        if (animate) {
            animator.start()
        }
    }
}

interface ExpandableChild
