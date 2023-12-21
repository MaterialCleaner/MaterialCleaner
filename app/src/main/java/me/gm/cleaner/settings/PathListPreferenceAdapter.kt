package me.gm.cleaner.settings

import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.core.view.forEach
import androidx.recyclerview.widget.BaseKtListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import me.gm.cleaner.R
import me.gm.cleaner.app.filepicker.FilePickerDialog
import me.gm.cleaner.databinding.PathListItemBinding
import java.io.File

class PathListPreferenceAdapter(private val fragment: PathListPreferenceFragmentCompat) :
    BaseKtListAdapter<String, PathListPreferenceAdapter.ViewHolder>(CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        PathListItemBinding.inflate(LayoutInflater.from(parent.context))
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val path = getItem(position)!!
        binding.title.text = path
        binding.root.setOnClickListener {
            FilePickerDialog()
                .apply {
                    setRoot(File.separator)
                    setPath(path)
                    addOnPositiveButtonClickListener { dir ->
                        fragment.newValues = fragment.newValues - path + dir
                    }
                }
                .show(fragment.childFragmentManager, null)
        }
        binding.root.setOnCreateContextMenuListener { menu, _, _ ->
            fragment.requireActivity().menuInflater.inflate(R.menu.item_delete, menu)
            menu.setHeaderTitle(path.substring(path.lastIndexOf(File.separator) + 1))
            menu.forEach {
                it.setOnMenuItemClickListener { item ->
                    onContextItemSelected(item, path)
                }
            }
        }
    }

    private fun onContextItemSelected(item: MenuItem, path: String): Boolean {
        if (item.itemId == R.id.menu_delete) {
            fragment.newValues -= path
            return true
        }
        return false
    }

    class ViewHolder(val binding: PathListItemBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {
        private val CALLBACK = object : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(oldItem: String, newItem: String): Boolean =
                oldItem == newItem

            override fun areContentsTheSame(oldItem: String, newItem: String): Boolean =
                oldItem == newItem
        }
    }
}
