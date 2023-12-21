package me.gm.cleaner.client.ui

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.os.bundleOf
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import me.gm.cleaner.R
import me.gm.cleaner.util.getSerializableCompat

class AppsTypeMarksUploadDialog : AppCompatDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val downloadApps = requireArguments()
            .getSerializableCompat<LinkedHashMap<String, List<String>>>(KEY_VALUES)!!.toList()
        val entries = downloadApps
            .map { (packageName, dirs) ->
                val paths = dirs.joinToString("\n")
                getString(
                    R.string.upload_apps_type_marks_hint,
                    packageName,
                    getString(R.string.storage_redirect_download_type),
                    paths
                )
            }
            .toTypedArray()
        val checkedItems = entries.map { true }.toBooleanArray()

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.upload_apps_type_marks_title)
            .setMultiChoiceItems(entries, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val rawAppsTypeMarks = arrayListOf<Pair<String, String>>()
                downloadApps.forEachIndexed { index, (packageName, dirs) ->
                    if (checkedItems[index]) {
                        val content = YamlDumper.dump("Download", 1L, dirs)
                        rawAppsTypeMarks += packageName to content
                    }
                }
                AppsTypeMarksUploadProgressDialog
                    .newInstance(rawAppsTypeMarks)
                    .show(parentFragmentManager, null)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    companion object {
        private const val KEY_VALUES: String = "me.gm.cleaner.key.values"

        fun newInstance(downloadApps: LinkedHashMap<String, List<String>>): AppsTypeMarksUploadDialog =
            AppsTypeMarksUploadDialog().apply {
                arguments = bundleOf(KEY_VALUES to downloadApps)
            }
    }
}
