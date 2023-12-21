package me.gm.cleaner.client.ui.storageredirect

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.gm.cleaner.R
import me.gm.cleaner.client.CleanerClient
import me.gm.cleaner.client.ui.storageredirect.ExitMode.Companion.SAVE_AND_REMOUNT_AND_EXIT

class RecommendDirOpsDialog : AppCompatDialogFragment() {
    private val parentFragment: StorageRedirectFragment
            by lazy { requireParentFragment() as StorageRedirectFragment }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val packageName = requireArguments().getString(KEY_PACKAGE_NAME)
        val dirOps = requireArguments().getSerializable(KEY_ENTRIES)
                as ArrayList<Pair<String, String>>
        val moveItems = requireArguments().getBooleanArray(KEY_MOVE_ITEMS)!!
        val entries = dirOps
            .mapIndexed { index, (from, to) ->
                if (moveItems[index]) {
                    getString(R.string.storage_redirect_move_dir, from, to)
                } else {
                    getString(R.string.storage_redirect_copy_dir, from, to)
                }
            }
            .toTypedArray()
        val checkedItems = requireArguments().getBooleanArray(KEY_CHECKED_ITEMS)!!

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.storage_redirect_dir_operation_recommendation)
            .setMultiChoiceItems(entries, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            .setPositiveButton(R.string.yes) { _, _ ->
                parentFragment.lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        dirOps.forEachIndexed { index, (from, to) ->
                            if (checkedItems[index]) {
                                if (moveItems[index]) {
                                    CleanerClient.service!!.move(from, to)
                                } else {
                                    CleanerClient.service!!.copy(from, to)
                                }
                            }
                        }
                        if (!CleanerClient.service!!.isFuseBpfEnabled) {
                            CleanerClient.service!!.switchSpecificAppsOwner(arrayOf(packageName))
                        }
                    }
                    navigateUp()
                }
            }
            .setNegativeButton(R.string.no) { _, _ ->
                navigateUp()
            }
            .setNeutralButton(android.R.string.cancel, null)
            .create()
    }

    private fun navigateUp() {
        parentFragment.onNavigateUp(SAVE_AND_REMOUNT_AND_EXIT)
    }

    companion object {
        private const val KEY_PACKAGE_NAME: String = "me.gm.cleaner.key.packageName"
        private const val KEY_ENTRIES: String = "me.gm.cleaner.key.entries"
        private const val KEY_MOVE_ITEMS: String = "me.gm.cleaner.key.moveItems"
        private const val KEY_CHECKED_ITEMS: String = "me.gm.cleaner.key.checkedItems"

        fun newInstance(
            packageName: String,
            entries: ArrayList<Pair<String, String>>,
            moveItems: BooleanArray,
            checkedItems: BooleanArray
        ): RecommendDirOpsDialog = RecommendDirOpsDialog().apply {
            arguments = bundleOf(
                KEY_PACKAGE_NAME to packageName,
                KEY_ENTRIES to entries,
                KEY_MOVE_ITEMS to moveItems,
                KEY_CHECKED_ITEMS to checkedItems
            )
        }
    }
}
