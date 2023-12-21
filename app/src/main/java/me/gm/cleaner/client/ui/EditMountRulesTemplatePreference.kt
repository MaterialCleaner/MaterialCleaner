package me.gm.cleaner.client.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.core.content.res.TypedArrayUtils
import androidx.preference.DialogPreference
import me.gm.cleaner.R
import me.gm.cleaner.client.ui.storageredirect.WizardAnswers
import me.gm.cleaner.dao.TempCodeRecords
import me.gm.cleaner.util.toBase64String
import me.gm.cleaner.util.toParcelable

class EditMountRulesTemplatePreference @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    @SuppressLint("RestrictedApi") @AttrRes defStyleAttr: Int = TypedArrayUtils.getAttr(
        context, androidx.preference.R.attr.dialogPreferenceStyle,
        android.R.attr.dialogPreferenceStyle
    ),
    @StyleRes defStyleRes: Int = 0
) : DialogPreference(context, attrs, defStyleAttr, defStyleRes) {
    private var _value: WizardAnswers = WizardAnswers(true)
    var value: String
        get() = _value.toBase64String()
        set(value) {
            try {
                _value = value.toParcelable()
                persistString(value)
            } catch (e: Throwable) {
                TempCodeRecords.fixBug("1.10")
                _value = WizardAnswers(true)
                persistString(_value.toBase64String())
            }
            notifyChanged()
        }

    override fun getDialogLayoutResource(): Int =
        R.layout.storage_redirect_category_mount_wizard_questions

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any? = a.getString(index)

    override fun onSetInitialValue(defaultValue: Any?) {
        value = getPersistedString(defaultValue as String?)
    }

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        if (isPersistent) {
            // No need to save instance state
            return superState
        }
        val myState = SavedState(superState)
        myState.mValue = value
        return myState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state == null || state.javaClass != SavedState::class.java) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state)
            return
        }
        val myState = state as SavedState
        super.onRestoreInstanceState(myState.superState)
        value = myState.mValue
    }

    private class SavedState : BaseSavedState {
        var mValue: String = ""

        constructor(source: Parcel) : super(source) {
            mValue = source.readString()!!
        }

        constructor(superState: Parcelable?) : super(superState)

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeString(mValue)
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(`in`: Parcel): SavedState = SavedState(`in`)
            override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
        }
    }
}
