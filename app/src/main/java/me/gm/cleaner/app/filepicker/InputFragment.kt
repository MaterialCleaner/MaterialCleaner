package me.gm.cleaner.app.filepicker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.asLiveData
import com.google.android.material.internal.ViewUtils
import me.gm.cleaner.databinding.InputDialogBinding
import me.gm.cleaner.model.FileModel

class InputFragment : PickerFragment() {

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
        editText.setText(filePicker.selected)
        editText.post {
            editText.selectAll()
        }
        editText.doAfterTextChanged {
            val file = FileModel(it.toString(), true, true)
            filePicker.select(file, requireContext()) { err ->
                binding.mtrlPickerTextInputDate.error = err
            }
        }

        filePicker.selectedFlow.asLiveData().observe(viewLifecycleOwner) { path ->
            onSelectionChangedListeners.forEach { listener ->
                listener.accept(path)
            }
        }
        return binding.root
    }

    companion object {

        fun newInstance(filePicker: FilePicker): InputFragment = InputFragment().apply {
            arguments = bundleOf(FILE_PICKER_KEY to filePicker)
        }
    }
}
