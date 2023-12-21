/*
 * Copyright 2021 Green Mushroom
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.gm.cleaner.util

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.RecordingCanvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import android.widget.EdgeEffect
import androidx.core.view.doOnPreDraw
import androidx.core.view.forEach
import androidx.recyclerview.widget.BaseListAdapter
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.zhanghai.android.fastscroll.FastScroller

private val rect = Rect()

fun <T, VH : RecyclerView.ViewHolder> BaseListAdapter<T, VH>.submitListKeepPosition(
    recyclerView: RecyclerView, list: List<T>, commitCallback: Runnable? = null
) {
    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
    val position = layoutManager.findFirstVisibleItemPosition()
    if (position == RecyclerView.NO_POSITION) {
        submitList(list, commitCallback)
    } else {
        recyclerView.getDecoratedBoundsWithMargins(
            layoutManager.findViewByPosition(position)!!, rect
        )
        submitList(list) {
            layoutManager.scrollToPositionWithOffset(position, rect.top - recyclerView.paddingTop)
            commitCallback?.run()
        }
    }
}

class DividerDecoration(private val list: RecyclerView) : RecyclerView.ItemDecoration() {
    private lateinit var divider: Drawable
    private var dividerHeight = 0
    private var allowDividerAfterLastItem = true

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (!::divider.isInitialized) {
            return
        }
        val width = parent.width
        parent.forEach { view ->
            if (shouldDrawDividerBelow(view, parent)) {
                val top = view.y.toInt() + view.height
                divider.setBounds(0, top, width, top + dividerHeight)
                divider.setTint(parent.context.colorControlHighlight)
                divider.draw(c)
            }
        }
    }

    override fun getItemOffsets(
        outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
    ) {
        if (shouldDrawDividerBelow(view, parent)) {
            outRect.bottom = dividerHeight
        }
    }

    private fun shouldDrawDividerBelow(view: View, parent: RecyclerView): Boolean {
        val holder = parent.getChildViewHolder(view)
        val dividerAllowedBelow = holder is DividerViewHolder && holder.isDividerAllowedBelow
        if (dividerAllowedBelow) {
            return true
        }
        var nextAllowed = allowDividerAfterLastItem
        val index = parent.indexOfChild(view)
        if (index < parent.childCount - 1) {
            val nextView = parent.getChildAt(index + 1)
            val nextHolder = parent.getChildViewHolder(nextView)
            nextAllowed = nextHolder is DividerViewHolder && nextHolder.isDividerAllowedAbove
        }
        return nextAllowed
    }

    fun setDivider(divider: Drawable) {
        dividerHeight = divider.intrinsicHeight
        this.divider = divider
        list.invalidateItemDecorations()
    }

    fun setDividerHeight(dividerHeight: Int) {
        this.dividerHeight = dividerHeight
        list.invalidateItemDecorations()
    }

    fun setAllowDividerAfterLastItem(allowDividerAfterLastItem: Boolean) {
        this.allowDividerAfterLastItem = allowDividerAfterLastItem
    }
}

abstract class DividerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    /**
     * Dividers are only drawn between items if both items allow it, or above the first and below
     * the last item if that item allows it.
     *
     * @return `true` if dividers are allowed above this item
     */
    var isDividerAllowedAbove = false

    /**
     * Dividers are only drawn between items if both items allow it, or above the first and below
     * the last item if that item allows it.
     *
     * @return `true` if dividers are allowed below this item
     */
    var isDividerAllowedBelow = false
}

private class AlwaysClipToPaddingEdgeEffectFactory : RecyclerView.EdgeEffectFactory() {

    override fun createEdgeEffect(view: RecyclerView, direction: Int) =
        object : EdgeEffect(view.context) {
            private var ensureSize = false

            private fun ensureSize() {
                if (ensureSize) return
                ensureSize = true

                when (direction) {
                    DIRECTION_LEFT -> {
                        setSize(
                            view.measuredHeight - view.paddingTop - view.paddingBottom,
                            view.measuredWidth - view.paddingLeft - view.paddingRight
                        )
                    }

                    DIRECTION_TOP -> {
                        setSize(
                            view.measuredWidth - view.paddingLeft - view.paddingRight,
                            view.measuredHeight - view.paddingTop - view.paddingBottom
                        )
                    }

                    DIRECTION_RIGHT -> {
                        setSize(
                            view.measuredHeight - view.paddingTop - view.paddingBottom,
                            view.measuredWidth - view.paddingLeft - view.paddingRight
                        )
                    }

                    DIRECTION_BOTTOM -> {
                        setSize(
                            view.measuredWidth - view.paddingLeft - view.paddingRight,
                            view.measuredHeight - view.paddingTop - view.paddingBottom
                        )
                    }
                }
            }

            private fun getCurrentEdgeEffectBehavior(): Int =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    !ValueAnimator.areAnimatorsEnabled()
                ) {
                    TYPE_NONE
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    TYPE_STRETCH
                } else {
                    TYPE_GLOW
                }

            override fun draw(canvas: Canvas): Boolean {
                ensureSize()
                val edgeEffectBehavior = getCurrentEdgeEffectBehavior()
                if (edgeEffectBehavior == TYPE_GLOW) {
                    val restore = canvas.save()
                    when (direction) {
                        DIRECTION_LEFT -> {
                            canvas.translate(view.paddingBottom.toFloat(), 0f)
                        }

                        DIRECTION_TOP -> {
                            canvas.translate(view.paddingLeft.toFloat(), view.paddingTop.toFloat())
                        }

                        DIRECTION_RIGHT -> {
                            canvas.translate(-view.paddingTop.toFloat(), 0f)
                        }

                        DIRECTION_BOTTOM -> {
                            canvas.translate(
                                view.paddingRight.toFloat(),
                                view.paddingBottom.toFloat()
                            )
                        }
                    }
                    val res = super.draw(canvas)
                    canvas.restoreToCount(restore)
                    return res
                } else if (edgeEffectBehavior == TYPE_STRETCH &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && canvas is RecordingCanvas
                ) {
                }
                return super.draw(canvas)
            }
        }.apply {
            // debug glow type
//            val field = EdgeEffect::class.java.getDeclaredField("mEdgeEffectType")
//            field.isAccessible = true
//            field.set(this, 0)
        }

    companion object {
        private const val TYPE_NONE: Int = -1
        private const val TYPE_GLOW: Int = 0
        private const val TYPE_STRETCH: Int = 1
    }
}

fun RecyclerView.fixEdgeEffect(alwaysClipToPadding: Boolean = true) {
    edgeEffectFactory = if (alwaysClipToPadding && !clipToPadding) {
        AlwaysClipToPaddingEdgeEffectFactory()
    } else {
        RecyclerView.EdgeEffectFactory()
    }
}

fun RecyclerView.overScrollIfContentScrollsPersistent(supportsChangeAnimations: Boolean = true) {
    doOnPreDraw {
        overScrollIfContentScrolls()
    }
    addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> overScrollIfContentScrolls() }
    itemAnimator = object : DefaultItemAnimator() {
        init {
            this.supportsChangeAnimations = supportsChangeAnimations
        }

        override fun onAnimationFinished(viewHolder: RecyclerView.ViewHolder) {
            super.onAnimationFinished(viewHolder)
            overScrollIfContentScrolls()
        }
    }
}

fun RecyclerView.overScrollIfContentScrolls() {
    overScrollMode = if (isContentScrolls()) {
        View.OVER_SCROLL_IF_CONTENT_SCROLLS
    } else {
        View.OVER_SCROLL_NEVER
    }
}

fun RecyclerView.isContentScrolls(): Boolean {
    val layoutManager = layoutManager
    if (layoutManager == null || adapter == null || adapter?.itemCount == 0) {
        return false
    }
    if (!isItemCompletelyVisible(0)) {
        return true
    }
    return !isItemCompletelyVisible(layoutManager.itemCount - 1)
}

fun RecyclerView.isItemCompletelyVisible(position: Int): Boolean {
    val vh = findViewHolderForAdapterPosition(position)
    vh ?: return false
    return layoutManager?.isViewPartiallyVisible(vh.itemView, true, true) == true
}

fun View.fitsSystemWindowInsets(fastScroller: FastScroller? = null, fixScroll: Boolean = false) {
    val paddingLeft = paddingLeft
    val paddingTop = paddingTop
    val paddingRight = paddingRight
    val paddingBottom = paddingBottom
    var scrollFixed = !fixScroll
    setOnApplyWindowInsetsListener { view, insets ->
        view.setPadding(
            paddingLeft, paddingTop + insets.systemWindowInsetTop,
            paddingRight, paddingBottom + insets.systemWindowInsetBottom
        )
        if (!scrollFixed) {
            scrollFixed = true
            view.scrollBy(0, -insets.systemWindowInsetTop)
        }
        fastScroller?.setPadding(
            0, paddingTop + insets.systemWindowInsetTop,
            0, paddingBottom + insets.systemWindowInsetBottom
        )
        insets
    }
}
