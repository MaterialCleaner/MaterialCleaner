package me.gm.cleaner.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes

@SuppressLint("AppCompatCustomView")
open class EditableText @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : TextView(context, attrs, defStyleAttr, defStyleRes) {

    override fun getDefaultEditable(): Boolean = true
}
