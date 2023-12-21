package me.gm.cleaner.app

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.function.Consumer

class ConfirmationDialog : AppCompatDialogFragment() {
    private val viewModel: ConfirmationViewModel by viewModels()
    private val pendingViewModelActions: MutableList<Runnable> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val iterator = pendingViewModelActions.iterator()
            while (iterator.hasNext()) {
                iterator.next().run()
                iterator.remove()
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(requireArguments().getCharSequence(KEY_MESSAGE))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                viewModel.onPositiveButtonClickListeners.forEach { listener ->
                    listener.accept(this)
                }
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                viewModel.onNegativeButtonClickListeners.forEach { listener ->
                    listener.accept(this)
                }
            }
            .create()

    private fun handleAction(action: Runnable) {
        if (!isAdded) {
            pendingViewModelActions += action
        } else {
            action.run()
        }
    }

    fun addOnPositiveButtonClickListener(onPositiveButtonClickListener: Consumer<ConfirmationDialog>) =
        handleAction {
            viewModel.onPositiveButtonClickListeners.add(onPositiveButtonClickListener)
        }

    fun removeOnPositiveButtonClickListener(onPositiveButtonClickListener: Consumer<ConfirmationDialog>) =
        handleAction {
            viewModel.onPositiveButtonClickListeners.remove(onPositiveButtonClickListener)
        }

    fun clearOnPositiveButtonClickListeners() = handleAction {
        viewModel.onPositiveButtonClickListeners.clear()
    }

    fun addOnNegativeButtonClickListener(onNegativeButtonClickListener: Consumer<ConfirmationDialog>) =
        handleAction {
            viewModel.onNegativeButtonClickListeners.add(onNegativeButtonClickListener)
        }

    fun removeOnNegativeButtonClickListener(onNegativeButtonClickListener: Consumer<ConfirmationDialog>) =
        handleAction {
            viewModel.onNegativeButtonClickListeners.remove(onNegativeButtonClickListener)
        }

    fun clearOnNegativeButtonClickListeners() = handleAction {
        viewModel.onNegativeButtonClickListeners.clear()
    }

    companion object {
        private const val KEY_MESSAGE: String = "me.gm.cleaner.key.message"

        fun newInstance(message: CharSequence): ConfirmationDialog = ConfirmationDialog().apply {
            arguments = bundleOf(KEY_MESSAGE to message)
        }
    }
}
