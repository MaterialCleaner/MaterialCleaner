package me.gm.cleaner.client.ui.storageredirect

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import me.gm.cleaner.databinding.ListDialogBinding
import me.gm.cleaner.util.overScrollIfContentScrollsPersistent
import me.gm.cleaner.widget.recyclerview.StickyHeader

class RationaleDialog : AppCompatDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = ListDialogBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(requireArguments().getString(KEY_TITLE))
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok, null)
            .create()

        val adapter = RationaleAdapter()
        val layoutManager = GridLayoutManager(requireContext(), 1)
        val list = binding.recyclerView
        list.adapter = adapter
        list.layoutManager = layoutManager
        list.setHasFixedSize(true)
        list.overScrollIfContentScrollsPersistent(false)
        list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            val header = StickyHeader(object : StickyHeader.Callback {
                override fun pinItemPositions(): Set<Int> =
                    adapter.currentList.mapIndexedNotNullTo(mutableSetOf()) { index, rationaleModel ->
                        if (rationaleModel is RationaleModel.Header) index else null
                    }

                override fun findFirstVisibleItemPosition(): Int =
                    layoutManager.findFirstVisibleItemPosition()
            })

            init {
                list.post {
                    header.updatePinnedView(list)
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                header.updatePinnedView(list)
            }
        })

        adapter.submitList(
            requireArguments().getSerializable(KEY_CONTENT) as ArrayList<Pair<String, String>>
        )
        return dialog
    }

    companion object {
        private const val KEY_TITLE: String = "me.gm.cleaner.key.title"
        private const val KEY_CONTENT: String = "me.gm.cleaner.key.content"

        fun newInstance(title: String, content: ArrayList<Pair<String, String>>): RationaleDialog =
            RationaleDialog().apply {
                arguments = bundleOf(
                    KEY_TITLE to title,
                    KEY_CONTENT to content
                )
            }
    }
}
