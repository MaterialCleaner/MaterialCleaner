package me.gm.cleaner.app

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.os.bundleOf
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class InfoDialog : AppCompatDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(requireArguments().getString(KEY_MESSAGE))
            .setPositiveButton(android.R.string.ok, null)
            .create()

    companion object {
        private const val KEY_MESSAGE: String = "me.gm.cleaner.key.message"

        fun newInstance(message: String): InfoDialog = InfoDialog().apply {
            arguments = bundleOf(KEY_MESSAGE to message)
        }
    }
}
