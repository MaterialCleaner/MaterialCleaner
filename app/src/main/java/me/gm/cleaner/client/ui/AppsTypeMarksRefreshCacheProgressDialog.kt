package me.gm.cleaner.client.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import me.gm.cleaner.databinding.ProgressDialogBinding

class AppsTypeMarksRefreshCacheProgressDialog : AppCompatDialogFragment() {
    private val viewModel: AppsTypeMarksRefreshCacheProgressViewModel by viewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        isCancelable = false
        val binding = ProgressDialogBinding.inflate(LayoutInflater.from(requireContext()))
        binding.progress.isIndeterminate = false
        binding.button.setText(android.R.string.cancel)
        binding.button.setOnClickListener {
            dismiss()
        }

        viewModel.progressLiveData.observe(this) { state ->
            when (state) {
                is AppsTypeMarksRefreshCacheState.Downloading -> {
                    binding.progress.setProgressCompat(state.progress, true)
                    binding.text.text = state.packageName
                }

                is AppsTypeMarksRefreshCacheState.Done -> dismiss()
            }
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }
}
