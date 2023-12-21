package me.gm.cleaner.widget.recyclerview.fastscroll

import android.content.Context
import me.gm.cleaner.R
import me.gm.cleaner.dao.RootPreferences
import me.zhanghai.android.fastscroll.FastScrollerBuilder

fun FastScrollerBuilder.useThemeStyle(context: Context): FastScrollerBuilder {
    useMd2Style()
    if (RootPreferences.material3) {
        setThumbDrawable(
            Utils.getGradientDrawableWithTintAttr(
                R.drawable.afs_md3_thumb, android.R.attr.colorControlActivated, context
            )!!
        )
    }
    return this
}
