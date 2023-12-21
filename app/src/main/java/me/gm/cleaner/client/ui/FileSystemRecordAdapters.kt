package me.gm.cleaner.client.ui

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.core.text.buildSpannedString
import androidx.core.text.strikeThrough
import androidx.core.view.forEach
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.BaseKtListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.snackbar.Snackbar
import me.gm.cleaner.R
import me.gm.cleaner.client.getPathWithEvent
import me.gm.cleaner.databinding.FilesystemRecordItemBinding
import me.gm.cleaner.databinding.LoadingMoreItemBinding
import me.gm.cleaner.util.ClipboardUtils
import me.gm.cleaner.util.FormatUtils
import me.gm.cleaner.util.OpenUtils
import java.io.File
import java.util.*

class FileSystemRecordAdapter(private val fragment: FileSystemRecordFragment) :
    BaseKtListAdapter<FileSystemRecordModel, FileSystemRecordAdapter.ViewHolder>(CALLBACK) {
    private val activity: FragmentActivity = fragment.requireActivity()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        FilesystemRecordItemBinding.inflate(LayoutInflater.from(parent.context))
    )

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val model = getItem(position)
        val packageInfo = model.packageInfo
        binding.icon.load(packageInfo)
        binding.title.text = if (packageInfo != null) {
            model.label
        } else {
            model.event.packageName
        }
        binding.time.text = FormatUtils.formatDateTime(activity, model.event.timeMillis)
        val path = model.event.path
        val pathWithEvent = getPathWithEvent(path, model.event.flags, fragment)
        binding.path.text = if (model.isReadOnlyPath) {
            buildSpannedString { strikeThrough { append(pathWithEvent) } }
        } else {
            pathWithEvent
        }
        var x = Float.NaN
        var y = Float.NaN
        binding.root.setOnTouchListener { _, motionEvent ->
            x = motionEvent.x
            y = motionEvent.y
            false
        }
        binding.root.setOnClickListener {
            it.showContextMenu(x, y)
        }
        binding.root.setOnCreateContextMenuListener { menu, _, _ ->
            activity.menuInflater.inflate(R.menu.filesystem_record_items, menu)
            menu.setHeaderTitle(path.substringAfterLast(File.separator))
            if (packageInfo == null) {
                menu.removeItem(R.id.menu_settings)
                menu.removeItem(R.id.menu_add_to_hide_list)
            }
            menu.forEach {
                it.setOnMenuItemClickListener { item ->
                    onContextItemSelected(item, model)
                }
            }
        }
    }

    private fun onContextItemSelected(item: MenuItem, model: FileSystemRecordModel): Boolean {
        val path = model.event.path
        when (item.itemId) {
            R.id.menu_settings -> {
                if (model.packageInfo == null) {
                    return false
                }
                val navController = fragment.findNavController()
                if (navController.currentDestination?.id == R.id.service_settings_fragment) {
                    val direction = ServiceSettingsFragmentDirections
                        .serviceSettingsToStorageRedirectAction(model.packageInfo)
                    navController.navigate(direction)
                }
            }

            R.id.menu_open -> {
                OpenUtils.open(fragment.requireContext(), path, model.event.flags)
            }

            R.id.menu_copy -> {
                ClipboardUtils.put(activity, path)
                Snackbar.make(
                    fragment.requireView(), fragment.getString(R.string.copied, path),
                    Snackbar.LENGTH_SHORT
                ).show()
            }

            R.id.menu_add_to_hide_list -> {
                if (model.packageInfo == null) {
                    return false
                } else {
                    fragment.viewModel.checkedFilterApps += model.packageInfo.packageName
                }
            }

            else -> return false
        }
        return true
    }

    class ViewHolder(val binding: FilesystemRecordItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    companion object {
        private val CALLBACK = object : DiffUtil.ItemCallback<FileSystemRecordModel>() {
            override fun areItemsTheSame(
                oldItem: FileSystemRecordModel, newItem: FileSystemRecordModel
            ): Boolean = oldItem.event == newItem.event

            override fun areContentsTheSame(
                oldItem: FileSystemRecordModel, newItem: FileSystemRecordModel
            ): Boolean = oldItem.event == newItem.event
        }
    }
}

class LoadingMoreAdapter : RecyclerView.Adapter<LoadingMoreAdapter.ViewHolder>() {
    private lateinit var progressBar: ProgressBar

    fun setProgress(progress: Int) {
        if (::progressBar.isInitialized) {
            progressBar.isIndeterminate = progress == -1
            progressBar.progress = progress
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        LoadingMoreItemBinding.inflate(LayoutInflater.from(parent.context))
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        progressBar = holder.binding.progress
        setProgress(-1)
    }

    override fun getItemCount(): Int = 1

    class ViewHolder(val binding: LoadingMoreItemBinding) : RecyclerView.ViewHolder(binding.root)
}
