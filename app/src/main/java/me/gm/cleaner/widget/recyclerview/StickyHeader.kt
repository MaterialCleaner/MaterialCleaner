package me.gm.cleaner.widget.recyclerview

import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.recyclerview.widget.RecyclerView
import java.lang.Float.min

class StickyHeader(private val callback: Callback) {
    private var pinnedItemPosition: Int = -1
    private var pinnedView: View? = null

    fun updatePinnedView(recyclerView: RecyclerView) {
        val pinItemPositions = callback.pinItemPositions().sorted()
        if (pinItemPositions.isEmpty()) {
            return
        }
        val position = callback.findFirstVisibleItemPosition()
        if (position == 0 &&
            recyclerView.findViewHolderForLayoutPosition(0)?.itemView?.top == 0
        ) {
            pinnedItemPosition = -1
            getParentView(recyclerView).removeView(pinnedView)
            return
        }

        val nextPosition = pinItemPositions.firstOrNull { pinItemPosition ->
            pinItemPosition > position
        }
        val previousPosition = pinItemPositions.last { pinItemPosition ->
            pinItemPosition <= position
        }
        pin(recyclerView, previousPosition)
        if (nextPosition != null) {
            val nextHolder = recyclerView.findViewHolderForLayoutPosition(nextPosition)
            if (nextHolder != null) {
                // Defer setTranslationY until pre draw to ensure pinnedView is measured.
                pinnedView!!.doOnPreDraw {
                    val dy = nextHolder.itemView.top - it.height
                    it.translationY = min(dy.toFloat(), 0F)
                }
            }
        }
    }

    private fun pin(recyclerView: RecyclerView, pinItemPosition: Int) {
        if (pinnedItemPosition != pinItemPosition) {
            getParentView(recyclerView).removeView(pinnedView)
            if (pinnedView != null) {
                pinnedItemPosition = pinItemPosition
            }
            pinnedView = createItemViewForPosition(recyclerView, pinItemPosition)
            getParentView(recyclerView).addView(pinnedView)
        }
    }

    private fun getParentView(v: View): ViewGroup = v.parent as ViewGroup

    private fun createItemViewForPosition(recyclerView: RecyclerView, pinItemPosition: Int): View? {
        val adapter = recyclerView.adapter ?: return null
        val holder = adapter.createViewHolder(
            recyclerView, adapter.getItemViewType(pinItemPosition)
        )
        adapter.bindViewHolder(holder, pinItemPosition)
        return holder.itemView
    }

    interface Callback {
        fun pinItemPositions(): Set<Int>
        fun findFirstVisibleItemPosition(): Int
    }
}
