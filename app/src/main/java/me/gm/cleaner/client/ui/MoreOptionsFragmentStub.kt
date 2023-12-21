package me.gm.cleaner.client.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.commit
import me.gm.cleaner.R
import me.gm.cleaner.databinding.MoreOptionsFragmentStubBinding

class MoreOptionsFragmentStub : BaseServiceSettingsFragment() {
    override val viewModel: BaseServiceSettingsViewModel
        get() = throw NoSuchFieldException()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        savedInstanceState ?: childFragmentManager.commit {
            replace(R.id.settings, MoreOptionsFragment())
        }
        val binding = MoreOptionsFragmentStubBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        // override to remove options menu
    }
}
