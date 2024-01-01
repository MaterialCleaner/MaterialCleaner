package me.gm.cleaner.browser.filepicker

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.internal.ViewUtils
import me.gm.cleaner.databinding.InputDialogBinding
import kotlin.io.path.pathString

class InputFragment : PickerFragment() {

    @SuppressLint("RestrictedApi")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = InputDialogBinding.inflate(inflater)

        val editText = binding.mtrlPickerTextInputDate.editText!!
        editText.setOnFocusChangeListener { view: View, hasFocus: Boolean ->
            if (!hasFocus) {
                ViewUtils.hideKeyboard(view)
            }
        }
        ViewUtils.requestFocusAndShowKeyboard(editText)
        editText.setText(parentViewModel.selected?.pathString)
        editText.post {
            editText.selectAll()
        }
        editText.doAfterTextChanged {
            parentViewModel.select(
                parentViewModel.path.fileSystem.getPath(it.toString()), requireContext()
            ) { err ->
                binding.mtrlPickerTextInputDate.error = err
            }
        }
        return binding.root
    }
}
