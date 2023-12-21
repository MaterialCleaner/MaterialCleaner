package me.gm.cleaner.client.ui.storageredirect

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import me.gm.cleaner.R
import me.gm.cleaner.client.ui.storageredirect.ExitMode.Companion.SAVE_AND_EXIT
import me.gm.cleaner.client.ui.storageredirect.ExitMode.Companion.SAVE_AND_REMOUNT_AND_EXIT
import me.gm.cleaner.dao.PurchaseVerification

class AskRemountDialog : AppCompatDialogFragment() {
    private val parentFragment: StorageRedirectFragment
            by lazy { requireParentFragment() as StorageRedirectFragment }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(
                if (PurchaseVerification.isExpressPro) {
                    R.string.storage_redirect_remount_instantly
                } else {
                    R.string.storage_redirect_forcestop_instantly
                }
            )
            .setPositiveButton(R.string.yes) { _, _ ->
                navigateUp(SAVE_AND_REMOUNT_AND_EXIT)
            }
            .setNegativeButton(R.string.no) { _, _ ->
                navigateUp(SAVE_AND_EXIT)
            }
            .setNeutralButton(android.R.string.cancel, null)
            .create()

    private fun navigateUp(@ExitMode mode: Int) {
        parentFragment.onNavigateUp(mode)
    }
}
