package me.gm.cleaner.widget

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import android.util.AttributeSet
import android.widget.Checkable
import android.widget.LinearLayout
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.core.os.ParcelCompat

open class CheckableLinearLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes), Checkable {
    private var mChecked: Boolean = false
        set(value) {
            if (field == value) {
                return
            }
            field = value
            refreshDrawableState()
        }

    override fun setChecked(checked: Boolean) {
        mChecked = checked
    }

    override fun isChecked(): Boolean = mChecked

    override fun toggle() {
        isChecked = !mChecked
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray =
        super.onCreateDrawableState(extraSpace + 1).apply {
            if (mChecked) {
                mergeDrawableStates(this, CHECKED_STATE_SET)
            }
        }

    override fun onSaveInstanceState(): Parcelable {
        val state = SavedState(super.onSaveInstanceState())
        state.isChecked = isChecked
        return state
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        val ss = state as SavedState
        super.onRestoreInstanceState(ss.superState)
        isChecked = ss.isChecked
    }

    internal class SavedState : BaseSavedState {
        var isChecked: Boolean = false

        constructor(source: Parcel) : super(source) {
            isChecked = ParcelCompat.readBoolean(source)
        }

        constructor(superState: Parcelable?) : super(superState)

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            ParcelCompat.writeBoolean(out, isChecked)
        }

        companion object CREATOR : Creator<SavedState> {
            override fun createFromParcel(source: Parcel): SavedState = SavedState(source)
            override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
        }
    }

    companion object {
        private val CHECKED_STATE_SET: IntArray = intArrayOf(android.R.attr.state_checked)
    }
}
