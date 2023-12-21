package me.gm.cleaner.widget.recyclerview

import android.animation.ValueAnimator
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

open class ChildItemAnimator(private val list: RecyclerView) : OverridableDefaultItemAnimator() {

    init {
        check(list.hasFixedSize())
    }

    private var pendingRemovals: List<RecyclerView.ViewHolder> = emptyList()
    private var pendingMoves: List<MoveInfo> = emptyList()
    private var pendingChanges: List<ChangeInfo> = emptyList()
    private var pendingAdditions: List<RecyclerView.ViewHolder> = emptyList()
    private var isResizing: Boolean = false
    private var deltaHeight: Int = 0

    fun resize() {
        if (isResizing || deltaHeight == 0) {
            return
        }
        pendingRemovals = emptyList()
        pendingMoves = emptyList()
        pendingChanges = emptyList()
        pendingAdditions = emptyList()
        isResizing = true
        val width = list.measuredWidth
        val height = list.measuredHeight
        val deltaX = 0
        val deltaY = deltaHeight
        deltaHeight = 0
        ValueAnimator.ofFloat(0F, 1F).apply {
            duration = moveDuration
            addUpdateListener { valueAnimator ->
                val fraction = valueAnimator.animatedValue as Float
                val params = list.layoutParams as ViewGroup.LayoutParams
                params.width = (width + fraction * deltaX).toInt()
                params.height = (height + fraction * deltaY).toInt()
                list.layoutParams = params
                if (fraction == 1F) {
                    isResizing = false
                }
            }
            start()
        }
    }

    override fun runPendingAnimations() {
        pendingRemovals = ArrayList(mPendingRemovals)
        pendingMoves = ArrayList(mPendingMoves)
        pendingChanges = ArrayList(mPendingChanges)
        pendingAdditions = ArrayList(mPendingAdditions)
        pendingRemovals.forEach { holder ->
            deltaHeight -= holder.itemView.height
        }
        pendingAdditions.forEach { holder ->
            deltaHeight += holder.itemView.height
        }
        pendingChanges.forEach { changeInfo ->
            deltaHeight += changeInfo.newHolder.itemView.height - changeInfo.oldHolder.itemView.height
        }
        super.runPendingAnimations()
    }

    override fun animateRemoveImpl(holder: RecyclerView.ViewHolder) {
        super.animateRemoveImpl(holder)
        if (pendingRemovals.isEmpty()) {
            return
        }
        if (pendingMoves.isEmpty() && pendingChanges.isEmpty() && pendingAdditions.isEmpty()) {
            resize()
        }
    }

    override fun animateAddImpl(holder: RecyclerView.ViewHolder) {
        super.animateAddImpl(holder)
        if (pendingAdditions.isEmpty()) {
            return
        }
        resize()
    }

    override fun animateMoveImpl(
        holder: RecyclerView.ViewHolder, fromX: Int, fromY: Int, toX: Int, toY: Int
    ) {
        super.animateMoveImpl(holder, fromX, fromY, toX, toY)
        if (pendingMoves.isEmpty()) {
            return
        }
        resize()
    }

    override fun animateChangeImpl(changeInfo: ChangeInfo) {
        super.animateChangeImpl(changeInfo)
        if (pendingChanges.isEmpty()) {
            return
        }
        resize()
    }
}
