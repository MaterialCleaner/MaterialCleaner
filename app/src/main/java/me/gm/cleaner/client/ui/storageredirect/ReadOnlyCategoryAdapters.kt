package me.gm.cleaner.client.ui.storageredirect

import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.core.view.forEach
import androidx.core.view.isVisible
import androidx.recyclerview.widget.BaseKtListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import me.gm.cleaner.R
import me.gm.cleaner.app.filepicker.FilePickerDialog
import me.gm.cleaner.app.filepicker.FilePickerDialog.Companion.SelectType.Companion.SELECT_FILE_AND_FOLDER
import me.gm.cleaner.databinding.StorageRedirectCategoryReadOnlyHeaderBinding
import me.gm.cleaner.databinding.StorageRedirectCategoryReadOnlyItemBinding
import me.gm.cleaner.util.ClipboardUtils
import me.gm.cleaner.util.DividerViewHolder
import me.gm.cleaner.util.OpenUtils

class ReadOnlyHeaderAdapter : RecyclerView.Adapter<ReadOnlyHeaderAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        StorageRedirectCategoryReadOnlyHeaderBinding.inflate(LayoutInflater.from(parent.context))
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {}

    override fun getItemCount(): Int = 1

    class ViewHolder(val binding: StorageRedirectCategoryReadOnlyHeaderBinding) :
        DividerViewHolder(binding.root) {
        init {
            isDividerAllowedAbove = true
        }
    }
}

class ReadOnlyAdapter(
    private val fragment: StorageRedirectFragment, private val viewModel: StorageRedirectViewModel
) : BaseKtListAdapter<String?, ReadOnlyAdapter.ViewHolder>(CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        StorageRedirectCategoryReadOnlyItemBinding.inflate(LayoutInflater.from(parent.context))
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val dir = getItem(position)
        binding.text.text = dir
        binding.text.isVisible = dir != null
        binding.add.isVisible = dir == null
        binding.frame.setOnClickListener {
            FilePickerDialog()
                .apply {
                    if (dir != null) {
                        setPath(dir)
                    }
                    setSelectType(SELECT_FILE_AND_FOLDER)
                    addOnPositiveButtonClickListener { path ->
                        val position = holder.bindingAdapterPosition
                        if (position != RecyclerView.NO_POSITION) {
                            viewModel.updateReadOnlyPaths {
                                set(position, path)
                                if (position == itemCount - 1) {
                                    add(null)
                                }
                            }
                        }
                    }
                }
                .show(fragment.childFragmentManager, null)
        }
        binding.frame.setOnCreateContextMenuListener { menu, _, _ ->
            if (dir != null) {
                fragment.requireActivity().menuInflater.inflate(
                    R.menu.mount_rules_editor_items, menu
                )
                val bindingAdapterPosition = holder.bindingAdapterPosition
                menu.setHeaderTitle(bindingAdapterPosition.toString())
                menu.forEach { item ->
                    item.setOnMenuItemClickListener {
                        onContextItemSelected(item, bindingAdapterPosition, dir)
                    }
                }
            }
        }
    }

    private fun onContextItemSelected(item: MenuItem, position: Int, path: String): Boolean =
        when (item.itemId) {
            R.id.menu_open -> {
                OpenUtils.open(fragment.requireContext(), path, true)
                true
            }

            R.id.menu_copy -> {
                ClipboardUtils.put(fragment.requireContext(), path)
                Snackbar.make(
                    fragment.requireView(), fragment.getString(R.string.copied, path),
                    Snackbar.LENGTH_SHORT
                ).show()
                true
            }

            R.id.menu_delete -> {
                viewModel.updateReadOnlyPaths {
                    removeAt(position)
                }
                true
            }

            else -> {
                false
            }
        }

    class ViewHolder(val binding: StorageRedirectCategoryReadOnlyItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    companion object {
        private val CALLBACK = object : DiffUtil.ItemCallback<String?>() {
            override fun areItemsTheSame(oldItem: String, newItem: String): Boolean =
                oldItem == newItem

            override fun areContentsTheSame(oldItem: String, newItem: String): Boolean =
                oldItem == newItem
        }
    }
}
