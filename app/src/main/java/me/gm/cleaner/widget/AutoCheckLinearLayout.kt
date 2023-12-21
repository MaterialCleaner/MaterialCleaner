package me.gm.cleaner.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.Checkable
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.core.view.children

open class AutoCheckLinearLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : CheckableLinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    override fun setChecked(checked: Boolean) {
        super.setChecked(checked)
        children.filterIsInstance<Checkable>().forEach {
            it.isChecked = checked
        }
    }

    override fun performClick(): Boolean {
        toggle()
        return super.performClick()
    }
}
