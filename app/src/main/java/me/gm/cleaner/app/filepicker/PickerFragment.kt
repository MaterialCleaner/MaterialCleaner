package me.gm.cleaner.app.filepicker

import android.os.Bundle
import androidx.fragment.app.Fragment
import me.gm.cleaner.util.getParcelableCompat
import java.util.function.Consumer

abstract class PickerFragment : Fragment() {
    protected val onSelectionChangedListeners: MutableSet<Consumer<String>> = mutableSetOf()

    /** Adds a listener for selection changes.  */
    fun addOnSelectionChangedListener(listener: Consumer<String>): Boolean {
        return onSelectionChangedListeners.add(listener)
    }

    /** Removes a listener for selection changes.  */
    fun removeOnSelectionChangedListener(listener: Consumer<String>): Boolean {
        return onSelectionChangedListeners.remove(listener)
    }

    /** Removes all listeners for selection changes.  */
    fun clearOnSelectionChangedListeners() {
        onSelectionChangedListeners.clear()
    }

    protected lateinit var filePicker: FilePicker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activeBundle = savedInstanceState ?: requireArguments()
        filePicker = activeBundle.getParcelableCompat<FilePicker>(FILE_PICKER_KEY)!!
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)
        bundle.putParcelable(FILE_PICKER_KEY, filePicker)
    }

    companion object {
        const val FILE_PICKER_KEY: String = "FILE_PICKER_KEY"
    }
}
