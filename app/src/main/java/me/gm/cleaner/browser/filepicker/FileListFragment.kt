package me.gm.cleaner.browser.filepicker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import me.gm.cleaner.databinding.ListDialogBinding
import me.gm.cleaner.util.overScrollIfContentScrollsPersistent
import me.gm.cleaner.widget.recyclerview.fastscroll.LinearLayoutViewHelper
import me.gm.cleaner.widget.recyclerview.fastscroll.useThemeStyle
import me.zhanghai.android.fastscroll.FastScrollerBuilder

class FileListFragment : PickerFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = ListDialogBinding.inflate(inflater)

        val fileListAdapter = FileListAdapter(requireContext(), parentViewModel)
        val goUpAdapter = GoUpAdapter(parentViewModel)
        val adapters = ConcatAdapter(fileListAdapter)
        val list = binding.recyclerView
        list.adapter = adapters
        list.layoutManager = GridLayoutManager(requireContext(), 1)
        FastScrollerBuilder(list)
            .setViewHelper(LinearLayoutViewHelper(list))
            .useThemeStyle(requireContext())
            .build()
        list.overScrollIfContentScrollsPersistent()
        list.itemAnimator = null
        parentViewModel.fileListFlow.asLiveData().observe(viewLifecycleOwner) { files ->
            fileListAdapter.submitList(files) {
                if (parentViewModel.canGoUp) {
                    adapters.addAdapter(0, goUpAdapter)
                } else {
                    adapters.removeAdapter(goUpAdapter)
                }
            }
        }
        return binding.root
    }
}
