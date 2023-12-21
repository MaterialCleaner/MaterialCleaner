package me.gm.cleaner.app.filepicker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
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

        val fileListAdapter = FileListAdapter(requireContext(), filePicker)
        val goUpAdapter = GoUpAdapter(filePicker)
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
        filePicker.fileListFlow.asLiveData().observe(viewLifecycleOwner) { files ->
            fileListAdapter.submitList(files) {
                if (filePicker.canGoUp) {
                    adapters.addAdapter(0, goUpAdapter)
                } else {
                    adapters.removeAdapter(goUpAdapter)
                }
            }
        }

        filePicker.selectedFlow.asLiveData().observe(viewLifecycleOwner) { path ->
            onSelectionChangedListeners.forEach { listener ->
                listener.accept(path)
            }
        }
        return binding.root
    }

    companion object {

        fun newInstance(filePicker: FilePicker): FileListFragment = FileListFragment().apply {
            arguments = bundleOf(FILE_PICKER_KEY to filePicker)
        }
    }
}
