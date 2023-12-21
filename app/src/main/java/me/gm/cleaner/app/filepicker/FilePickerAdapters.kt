package me.gm.cleaner.app.filepicker

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
import me.gm.cleaner.model.FileModel
import me.gm.cleaner.util.textColorPrimary
import java.io.File

class ViewHolder(val binding: FilePickerItemBinding) : RecyclerView.ViewHolder(binding.root)

class FileListAdapter(context: Context, private val filePicker: FilePicker) :
    BaseKtListAdapter<FilePickerModel, ViewHolder>(CALLBACK) {
    private val folderBadge: Bitmap by lazy {
        AppCompatResources
            .getDrawable(context, R.drawable.ic_outline_folder_24)!!
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
        binding.title.text = model.file.path.substringAfterLast(File.separator)
        if (model.file.isDirectory) {
            binding.icon.setImageBitmap(folderBadge)
            binding.root.setOnClickListener {
                filePicker.select(model.file)
                filePicker.path = model.file.path
            }
        } else {
            binding.icon.setImageBitmap(fileBadge)
            binding.root.setOnClickListener {
                if (!model.isSelected) {
                    filePicker.select(model.file)
                } else {
                    filePicker.select(FileModel(filePicker.path, true, false))
                }
            }
        }
        binding.root.isChecked = model.isSelected
    }

    companion object {
        private val CALLBACK = object : DiffUtil.ItemCallback<FilePickerModel>() {
            override fun areItemsTheSame(
                oldItem: FilePickerModel, newItem: FilePickerModel
            ): Boolean = oldItem.file.path == newItem.file.path

            override fun areContentsTheSame(
                oldItem: FilePickerModel, newItem: FilePickerModel
            ): Boolean = oldItem == newItem
        }
    }
}

class GoUpAdapter(private val filePicker: FilePicker) : RecyclerView.Adapter<ViewHolder>() {

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
