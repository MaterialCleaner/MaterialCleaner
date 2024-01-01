package me.gm.cleaner.browser.filepicker

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import androidx.recyclerview.widget.BaseKtListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import me.gm.cleaner.R
import me.gm.cleaner.databinding.FilePickerItemBinding
import me.gm.cleaner.util.textColorPrimary
import kotlin.io.path.name

class ViewHolder(val binding: FilePickerItemBinding) : RecyclerView.ViewHolder(binding.root)

class FileListAdapter(context: Context, private val filePicker: FilePickerViewModel) :
    BaseKtListAdapter<FilePickerModel, ViewHolder>(CALLBACK) {
    private val folderBadge: Bitmap by lazy {
        AppCompatResources
            .getDrawable(context, R.drawable.ic_outline_folder_24)!!
            .apply {
                setTint(context.textColorPrimary.defaultColor)
            }
            .toBitmap()
    }
    private val linkBadge: Bitmap by lazy {
        AppCompatResources
            .getDrawable(context, R.drawable.outline_link_24)!!
            .apply {
                setTint(context.textColorPrimary.defaultColor)
            }
            .toBitmap()
    }
    private val fileBadge: Bitmap by lazy {
        AppCompatResources
            .getDrawable(context, R.drawable.ic_outline_file_24)!!
            .apply {
                setTint(context.textColorPrimary.defaultColor)
            }
            .toBitmap()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        FilePickerItemBinding.inflate(LayoutInflater.from(parent.context))
    ).apply {
        binding.root.background = CheckableItemBackground.create(parent.context)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val model = getItem(position)
        binding.title.text = model.path.name
        if (model.attrs.isDirectory) {
            binding.icon.setImageBitmap(folderBadge)
            binding.root.setOnClickListener {
                filePicker.select(model.path)
                filePicker.path = model.path
            }
        } else {
            if (model.attrs.isSymbolicLink) {
                binding.icon.setImageBitmap(linkBadge)
            } else {
                binding.icon.setImageBitmap(fileBadge)
            }
            binding.root.setOnClickListener {
                if (!model.isSelected) {
                    filePicker.select(model.path)
                } else {
                    filePicker.select(filePicker.path)
                }
            }
        }
        binding.root.isChecked = model.isSelected
    }

    companion object {
        private val CALLBACK = object : DiffUtil.ItemCallback<FilePickerModel>() {
            override fun areItemsTheSame(
                oldItem: FilePickerModel, newItem: FilePickerModel
            ): Boolean = oldItem.path == newItem.path

            override fun areContentsTheSame(
                oldItem: FilePickerModel, newItem: FilePickerModel
            ): Boolean = oldItem == newItem
        }
    }
}

class GoUpAdapter(private val filePicker: FilePickerViewModel) :
    RecyclerView.Adapter<ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        FilePickerItemBinding.inflate(LayoutInflater.from(parent.context))
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        binding.title.text = ".."
        binding.root.setOnClickListener {
            if (filePicker.canGoUp) {
                filePicker.goUp(true)
            }
        }
    }

    override fun getItemCount(): Int = 1
}
