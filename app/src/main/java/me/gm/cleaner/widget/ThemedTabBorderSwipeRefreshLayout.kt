package me.gm.cleaner.widget

import android.content.Context
import android.util.AttributeSet
import androidx.swiperefreshlayout.widget.ThemedSwipeRefreshLayout
import me.gm.cleaner.R
import me.gm.cleaner.dao.RootPreferences
import me.gm.cleaner.util.getDimenByAttr

class ThemedTabBorderSwipeRefreshLayout(context: Context, attrs: AttributeSet?) :
    ThemedSwipeRefreshLayout(context, attrs) {

    private fun init() {
        if (RootPreferences.material3) {
            applyM3Background()
        }
        val actionBarSizeAddTabHeight =
            context.getDimenByAttr(R.attr.actionBarSizeAddTabHeight).toInt()
        setProgressViewOffset(
            false, actionBarSizeAddTabHeight, progressViewEndOffset + actionBarSizeAddTabHeight
        )
    }

    init {
        init()
    }
}
