package me.gm.cleaner.client.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.commit
import androidx.navigation.fragment.findNavController
import me.gm.cleaner.R
import me.gm.cleaner.app.BaseFragment
import me.gm.cleaner.databinding.AppsTypeMarksFragmentStubBinding

class AppsTypeMarksSettingsFragmentStub : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        savedInstanceState ?: childFragmentManager.commit {
            replace(R.id.settings, AppsTypeMarksSettingsFragment())
        }
        val binding = AppsTypeMarksFragmentStubBinding.inflate(layoutInflater)
        setAppBar(binding.root)
        val toolbar = requireToolbar()
        toolbar.post {
            toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
            toolbar.setNavigationIcon(R.drawable.ic_outline_arrow_back_24)
            toolbar.setTitle(R.string.apps_type_marks_title)
        }
        return binding.root
    }
}
