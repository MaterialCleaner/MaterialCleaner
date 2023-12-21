package me.gm.cleaner.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.core.view.children
import me.gm.cleaner.util.colorPrimary
import me.gm.cleaner.util.textColorPrimary

class PrimaryTextLinearLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : AutoCheckLinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    override fun setChecked(checked: Boolean) {
        super.setChecked(checked)
        val textColor = if (checked) context.colorPrimary else context.textColorPrimary.defaultColor
        children.forEach {
            if (it is TextView) {
                it.setTextColor(textColor)
            }
        }
    }
}
