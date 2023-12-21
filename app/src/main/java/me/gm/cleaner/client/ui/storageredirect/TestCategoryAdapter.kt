package me.gm.cleaner.client.ui.storageredirect

import android.text.Selection
import android.text.Spannable
import androidx.core.view.isVisible
import me.gm.cleaner.R
import me.gm.cleaner.app.filepicker.FilePickerDialog
import me.gm.cleaner.app.filepicker.FilePickerDialog.Companion.SelectType.Companion.SELECT_FILE_AND_FOLDER
import me.gm.cleaner.databinding.StorageRedirectCategoryTestBinding
import me.gm.cleaner.util.listFormat

class TestCategoryAdapter(
    private val fragment: StorageRedirectFragment, private val viewModel: StorageRedirectViewModel
) {

    fun onViewAttachedToWindow(binding: StorageRedirectCategoryTestBinding) {
        arrayOf(
            binding.edit, binding.resultMountedPath, binding.resultAccessiblePlaces
        ).forEach { textView ->
            // Workaround "TextView does not support text selection. Selection cancelled."
            // @see https://stackoverflow.com/questions/37566303/edittext-giving-error-textview-does-not-support-text-selection-selection-canc
            textView.isEnabled = false
            textView.isEnabled = true
        }
    }

    fun onBindViewHolder(binding: StorageRedirectCategoryTestBinding) {
        val testPath = viewModel.test
        binding.edit.setText(testPath)
        binding.edit.clearFocus()
        binding.edit.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                v.clearFocus()
                FilePickerDialog()
                    .apply {
                        if (viewModel.test.isNotEmpty()) {
                            setPath(viewModel.test)
                        }
                        setSelectType(SELECT_FILE_AND_FOLDER)
                        addOnPositiveButtonClickListener { dir ->
                            viewModel.test = dir
                        }
                    }
                    .show(fragment.childFragmentManager, null)
            }
        }

        arrayOf(binding.resultMountedPath, binding.resultAccessiblePlaces).forEach { textView ->
            textView.isVisible = testPath.isNotEmpty()
        }
        if (testPath.isNotEmpty()) {
            val rules = viewModel.rules
            val mountedPath = rules.getMountedPath(testPath)
            binding.resultMountedPath.text = fragment.getString(
                R.string.storage_redirect_test_result_mounted_path, mountedPath
            )
            binding.resultMountedPath.setOnFocusChangeListener { _, _ ->
                val index = binding.resultMountedPath.text.indexOf(mountedPath)
                if (index != -1) {
                    Selection.setSelection(
                        binding.resultMountedPath.text as Spannable,
                        index, index + mountedPath.length
                    )
                }
            }

            val accessiblePlaces = rules.getAccessiblePlaces(testPath)
            binding.resultAccessiblePlaces.text = if (accessiblePlaces.isEmpty()) {
                fragment.getString(R.string.storage_redirect_test_result_inaccessible)
            } else {
                val accessiblePlacesFormat = accessiblePlaces.listFormat(
                    fragment.getString(R.string.delimiter)
                )
                binding.resultAccessiblePlaces.setOnFocusChangeListener { _, _ ->
                    val index = binding.resultAccessiblePlaces.text.indexOf(accessiblePlacesFormat)
                    if (index != -1) {
                        Selection.setSelection(
                            binding.resultAccessiblePlaces.text as Spannable,
                            index, index + accessiblePlacesFormat.length
                        )
                    }
                }
                fragment.getString(
                    R.string.storage_redirect_test_result_accessible_places, accessiblePlacesFormat
                )
            }
        }
    }
}
