package me.gm.cleaner.settings

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import androidx.core.view.ViewCompat
import androidx.preference.EditTextPreferenceDialogFragmentCompat
import com.google.android.material.dialog.InsetDialogOnTouchListener
import com.google.android.material.dialog.MaterialDialogs
import me.gm.cleaner.util.createMaterialAlertDialogThemedContext
import me.gm.cleaner.util.materialDialogBackgroundDrawable
import me.gm.cleaner.util.materialDialogBackgroundInsets

class MaterialEditTextPreferenceDialogFragmentCompat : EditTextPreferenceDialogFragmentCompat() {

    @SuppressLint("RestrictedApi")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val alertDialog = super.onCreateDialog(savedInstanceState)
        val window = alertDialog.window!!
        /* {@link Window#getDecorView()} should be called before any changes are made to the Window
         * as it locks in attributes and affects layout. */
        val decorView = window.decorView
        val context = requireContext().createMaterialAlertDialogThemedContext()
        val background = context.materialDialogBackgroundDrawable()
        background.elevation = ViewCompat.getElevation(decorView)
        val backgroundInsets = context.materialDialogBackgroundInsets()

        val insetDrawable = MaterialDialogs.insetDrawable(background, backgroundInsets)
        window.setBackgroundDrawable(insetDrawable)
        decorView.setOnTouchListener(InsetDialogOnTouchListener(alertDialog, backgroundInsets))
        return alertDialog
    }

    companion object {

        fun newInstance(key: String?): MaterialEditTextPreferenceDialogFragmentCompat {
            val fragment = MaterialEditTextPreferenceDialogFragmentCompat()
            val b = Bundle(1)
            b.putString(ARG_KEY, key)
            fragment.arguments = b
            return fragment
        }
    }
}
