package me.gm.cleaner.client.ui

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import me.gm.cleaner.R
import me.gm.cleaner.databinding.ApplistUnsupportedFragmentBinding

class AppListUnsupportedFragment : BaseServiceSettingsFragment() {
    override val viewModel: BaseServiceSettingsViewModel
        get() = throw NoSuchFieldException()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = ApplistUnsupportedFragmentBinding.inflate(layoutInflater)
        binding.warning.text = getString(
            R.string.storage_redirect_unsupported, Build.VERSION.RELEASE
        )
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        // override to remove options menu
    }
}
