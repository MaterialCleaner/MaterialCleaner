package me.gm.cleaner.widget.recyclerview.fastscroll

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.gm.cleaner.BuildConfig
import me.gm.cleaner.util.isContentScrolls

open class LinearLayoutViewHelper(private val list: RecyclerView) :
    BaseRecyclerViewHelper(list) {
    private val layoutManager: LinearLayoutManager = list.layoutManager as LinearLayoutManager
    private val itemCount: Int
        get() = list.adapter!!.itemCount
    private val mTempRect: Rect = Rect()

    private fun mockOffsetToReal(offset: Int): Int = mTempRect.height() * offset / mockItemSize

    private fun realOffsetToMock(offset: Int): Int = mockItemSize * offset / mTempRect.height()

    override fun getScrollRange(): Int = list.height +
            if (list.isContentScrolls()) {
                val firstIndex = layoutManager.findFirstVisibleItemPosition()
                getItemBounds(layoutManager.findViewByPosition(firstIndex)!!)
                val firstPartial = realOffsetToMock(list.paddingTop - mTempRect.top)

                val lastIndex = layoutManager.findLastVisibleItemPosition()
                getItemBounds(layoutManager.findViewByPosition(lastIndex)!!)
                val lastPartial = realOffsetToMock(mTempRect.bottom - list.height)

                realOffsetToMock(list.paddingBottom) +
                        mockItemSize * itemCount -
                        (mockItemSize * (lastIndex - firstIndex + 1) - firstPartial - lastPartial)
            } else {
                0
            }

    override fun getScrollOffset(): Int {
        val index = layoutManager.findFirstVisibleItemPosition()
        if (index == RecyclerView.NO_POSITION || index >= itemCount) {
            return 0
        }
        getItemBounds(layoutManager.findViewByPosition(index)!!)
        return index * mockItemSize - realOffsetToMock(mTempRect.top - list.paddingTop)
    }

    private fun getItemBounds(itemView: View) {
        list.getDecoratedBoundsWithMargins(itemView, mTempRect)
    }

    private var lastIndex: Int = -2
    private var lastOffset: Int = -1
    override fun scrollTo(offset: Int) {
        // Stop any scroll in progress for RecyclerView.
        list.stopScroll()
        val index = offset / mockItemSize
        val targetView = layoutManager.findViewByPosition(index)
        if (targetView != null) {
            getItemBounds(targetView)
            val realOffset = -mockOffsetToReal(offset - index * mockItemSize)
            layoutManager.scrollToPositionWithOffset(index, realOffset)
        } else {
            if (index == lastIndex - 1) {
                list.scrollBy(0, mockOffsetToReal(offset - lastOffset))
            } else {
                layoutManager.scrollToPositionWithOffset(index, 0)
            }
        }
        lastIndex = index
        lastOffset = offset
    }

    init {
        if (BuildConfig.DEBUG) {
            require(layoutManager !is GridLayoutManager || layoutManager.spanCount == 1)
        }
    }

    companion object {
        private const val mockItemSize: Int = 10000
    }
}
