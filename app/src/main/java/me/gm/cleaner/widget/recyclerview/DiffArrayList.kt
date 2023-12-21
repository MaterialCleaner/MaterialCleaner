package me.gm.cleaner.widget.recyclerview;

import androidx.recyclerview.widget.BaseKtListAdapter
import androidx.recyclerview.widget.RecyclerView

fun <T, VH : RecyclerView.ViewHolder> BaseKtListAdapter<T, VH>.submitDiffList(
    list: DiffArrayList<T>, commitCallback: Runnable? = null
) {
    if (list.hasPendingUpdates()) {
        setCurrentList(list.toList()) {
            list.consumePendingUpdates(this)
            commitCallback?.run()
        }
    } else {
        submitList(list.toList(), commitCallback)
    }
}
