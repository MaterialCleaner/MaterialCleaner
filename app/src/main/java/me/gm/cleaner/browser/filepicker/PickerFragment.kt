package me.gm.cleaner.browser.filepicker

import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels

abstract class PickerFragment : Fragment() {
    protected val parentViewModel: FilePickerViewModel by viewModels({ requireParentFragment() })
}
