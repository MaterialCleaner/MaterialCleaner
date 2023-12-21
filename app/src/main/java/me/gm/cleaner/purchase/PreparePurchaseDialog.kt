package me.gm.cleaner.purchase

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import me.gm.cleaner.R
import me.gm.cleaner.databinding.ProgressDialogBinding

class PreparePurchaseDialog : AppCompatDialogFragment() {
    private val viewModel: PurchaseViewModel by purchaseActivityViewModels()
    private val productId: Int by lazy { requireArguments().getInt(KEY_ID) }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        isCancelable = false
        val binding = ProgressDialogBinding.inflate(LayoutInflater.from(requireContext()))

        viewModel.connectionState.observe(this) { result ->
            when (result) {
                is ServerConnectionState.Connecting -> bindConnecting(binding)
                is ServerConnectionState.AlreadyOwned -> dismiss()
                is ServerConnectionState.GoPurchase -> {
                    viewModel.launchPurchaseFlow(requireActivity())
                    dismiss()
                }

                is ServerConnectionState.RecoverableError ->
                    bindConnectionFailed(binding, true, result.message)
                is ServerConnectionState.Error ->
                    bindConnectionFailed(binding, false, result.message)
            }
        }
        if (savedInstanceState == null) {
            viewModel.waitOrStartServerConnection(productId)
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }

    private fun bindConnecting(binding: ProgressDialogBinding) {
        binding.progress.isVisible = true
        binding.text.setText(R.string.billing_connecting)
        binding.button.setText(android.R.string.cancel)
        binding.button.setOnClickListener {
            viewModel.endServerConnection()
            dismiss()
        }
    }

    private fun bindConnectionFailed(
        binding: ProgressDialogBinding, isRecoverableError: Boolean, errorMessage: String
    ) {
        binding.progress.isVisible = false
        binding.text.text = requireContext().getString(
            R.string.billing_connect_failed, errorMessage
        )
        if (isRecoverableError) {
            binding.button.setText(R.string.retry)
            binding.button.setOnClickListener {
                viewModel.waitOrStartServerConnection(productId)
            }
        } else {
            binding.button.setText(android.R.string.cancel)
            binding.button.setOnClickListener {
                viewModel.endServerConnection()
                dismiss()
            }
        }
    }

    companion object {
        private const val KEY_ID: String = "me.gm.cleaner.key.id"

        fun newInstance(@ProductId productId: Int): PreparePurchaseDialog =
            PreparePurchaseDialog().apply {
                arguments = bundleOf(KEY_ID to productId)
            }
    }
}
