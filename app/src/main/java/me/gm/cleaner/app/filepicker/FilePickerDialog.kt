package me.gm.cleaner.app.filepicker

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.annotation.IntDef
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.commitNow
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import me.gm.cleaner.R
import me.gm.cleaner.app.filepicker.FilePickerDialog.Companion.InputMode.Companion.INPUT_MODE_FILE_LIST
import me.gm.cleaner.app.filepicker.FilePickerDialog.Companion.InputMode.Companion.INPUT_MODE_KEY
import me.gm.cleaner.app.filepicker.FilePickerDialog.Companion.InputMode.Companion.INPUT_MODE_TEXT
import me.gm.cleaner.app.filepicker.FilePickerDialog.Companion.SelectType.Companion.SELECT_FILE
import me.gm.cleaner.app.filepicker.FilePickerDialog.Companion.SelectType.Companion.SELECT_FILE_AND_FOLDER
import me.gm.cleaner.app.filepicker.FilePickerDialog.Companion.SelectType.Companion.SELECT_FOLDER
import me.gm.cleaner.databinding.FilePickerDialogBinding
import java.util.function.Consumer

class FilePickerDialog : AppCompatDialogFragment() {
    private val viewModel: FilePickerViewModel by viewModels()
    private val pendingViewModelActions: MutableList<Runnable> = mutableListOf()
    private lateinit var pickerFragment: PickerFragment

    @InputMode
    private var inputMode: Int = INPUT_MODE_FILE_LIST

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val iterator = pendingViewModelActions.iterator()
            while (iterator.hasNext()) {
                iterator.next().run()
                iterator.remove()
            }
            selectPath(viewModel.filePicker.path)
        } else {
            inputMode = savedInstanceState.getInt(INPUT_MODE_KEY)
        }
    }

    private fun handleAction(action: Runnable) {
        if (!isAdded) {
            pendingViewModelActions += action
        } else {
            action.run()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = FilePickerDialogBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.file_picker_title)
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                viewModel.onPositiveButtonClickListeners.forEach { listener ->
                    listener.accept(viewModel.filePicker.selected)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        val headerToggleButton = binding.header.mtrlPickerHeaderToggle
        headerToggleButton.setImageResource(
            when (inputMode) {
                INPUT_MODE_FILE_LIST -> R.drawable.ic_outline_edit_24
                INPUT_MODE_TEXT -> R.drawable.ic_outline_list_24
                else -> 0
            }
        )
        headerToggleButton.setOnClickListener {
            inputMode = 1 xor inputMode
            headerToggleButton.setImageResource(
                when (inputMode) {
                    INPUT_MODE_FILE_LIST -> R.drawable.ic_outline_edit_24
                    INPUT_MODE_TEXT -> R.drawable.ic_outline_list_24
                    else -> 0
                }
            )
            startPickerFragment()
        }

        viewModel.filePicker.selectedFlow.asLiveData().observe(this) { path ->
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).isEnabled = path.isNotEmpty()
            binding.header.mtrlPickerHeaderSelectionText.text = path.ifEmpty {
                getString(R.string.file_picker_unselected)
            }
        }
        return dialog
    }

    override fun onStart() {
        super.onStart()
        startPickerFragment()
    }

    private fun startPickerFragment() {
        pickerFragment = when (inputMode) {
            INPUT_MODE_FILE_LIST -> FileListFragment.newInstance(viewModel.filePicker)
            INPUT_MODE_TEXT -> InputFragment.newInstance(viewModel.filePicker)
            else -> throw IllegalArgumentException()
        }
        childFragmentManager.commitNow {
            replace(R.id.picker_frame, pickerFragment)
        }
        pickerFragment.addOnSelectionChangedListener { path ->
            selectPath(path)
        }
    }

    private fun selectPath(path: String) {
        val file = createFileModel(path)
        if (viewModel.filePicker.select(file)) {
            viewModel.filePicker.path = path
            if (file.isFile) {
                viewModel.filePicker.goUp(false)
            }
        } else {
            // The path is verified value, so it should be selected anyway.
            file.isDirectory = true
            file.isFile = true
            viewModel.filePicker.select(file)
        }
    }

    override fun onStop() {
        pickerFragment.clearOnSelectionChangedListeners()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(INPUT_MODE_KEY, inputMode)
    }

    /** The supplied listener is called when the user confirms a valid selection.  */
    fun addOnPositiveButtonClickListener(onPositiveButtonClickListener: Consumer<String>) =
        handleAction {
            viewModel.onPositiveButtonClickListeners.add(onPositiveButtonClickListener)
        }

    /**
     * Removes a listener previously added via [FilePickerDialog.addOnPositiveButtonClickListener].
     */
    fun removeOnPositiveButtonClickListener(onPositiveButtonClickListener: Consumer<String>) =
        handleAction {
            viewModel.onPositiveButtonClickListeners.remove(onPositiveButtonClickListener)
        }

    /**
     * Removes all listeners added via [FilePickerDialog.addOnPositiveButtonClickListener].
     */
    fun clearOnPositiveButtonClickListeners() = handleAction {
        viewModel.onPositiveButtonClickListeners.clear()
    }

    fun setPath(path: String) = handleAction {
        viewModel.filePicker.path = path
    }

    fun setRoot(root: String) = handleAction {
        viewModel.filePicker.root = root
    }

    fun setSelectType(@SelectType selectType: Int) = handleAction {
        viewModel.filePicker.selectType = selectType
    }

    companion object {
        @IntDef(value = [SELECT_FILE, SELECT_FOLDER, SELECT_FILE_AND_FOLDER])
        @Retention(AnnotationRetention.SOURCE)
        annotation class SelectType {
            companion object {
                const val SELECT_FILE: Int = 0
                const val SELECT_FOLDER: Int = 1
                const val SELECT_FILE_AND_FOLDER: Int = 2
            }
        }

        @IntDef(value = [INPUT_MODE_FILE_LIST, INPUT_MODE_TEXT])
        @Retention(AnnotationRetention.SOURCE)
        annotation class InputMode {
            companion object {
                const val INPUT_MODE_KEY: String = "INPUT_MODE_KEY"

                /** File picker will start with file list view.  */
                const val INPUT_MODE_FILE_LIST: Int = 0

                /** File picker will start with input text view.  */
                const val INPUT_MODE_TEXT: Int = 1
            }
        }
    }
}
