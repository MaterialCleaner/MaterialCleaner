package me.gm.cleaner.home.ui

import android.graphics.Bitmap
import android.text.SpannableStringBuilder
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.forEach
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.BaseKtListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.snackbar.Snackbar
import me.gm.cleaner.R
import me.gm.cleaner.dao.RootPreferences
import me.gm.cleaner.databinding.TrashItemBinding
import me.gm.cleaner.home.TrashModel
import me.gm.cleaner.home.scanner.ScannerManager
import me.gm.cleaner.home.scanner.ScannerManager.FileChanges
import me.gm.cleaner.util.ClipboardUtils
import me.gm.cleaner.util.OpenUtils
import me.gm.cleaner.util.buildStyledTitle
import me.gm.cleaner.util.colorControlNormal
import java.io.File
import java.util.regex.Pattern

class TrashAdapter(private val fragment: TrashFragment) :
    BaseKtListAdapter<TrashModel, TrashAdapter.ViewHolder>(CALLBACK) {
    private val activity: HomeActivity = fragment.requireActivity() as HomeActivity
    private val args: TrashFragmentArgs by fragment.navArgs()
    private val viewModel: TrashViewModel by fragment.viewModels()
    private val inUse: SpannableStringBuilder by lazy {
        activity.buildStyledTitle(fragment.getString(R.string.in_use))
    }
    private val badge: Bitmap? by lazy {
        when (args.icon) {
            R.drawable.ic_outline_android_24 -> AppCompatResources
                .getDrawable(activity, R.drawable.ic_outline_android_24)!!
                .apply {
                    setTint(activity.colorControlNormal)
                }
                .toBitmap()

            R.drawable.ic_outline_small_24 -> AppCompatResources
                .getDrawable(activity, R.drawable.ic_outline_file_24)!!
                .apply {
                    setTint(activity.colorControlNormal)
                }
                .toBitmap()

            R.drawable.ic_outline_standard_folder_24,
            R.drawable.ic_outline_empty_folder_24 -> AppCompatResources
                .getDrawable(activity, R.drawable.ic_outline_folder_24)!!
                .apply {
                    setTint(activity.colorControlNormal)
                }
                .toBitmap()

            else -> null
        }
    }

    override fun getItemId(position: Int): Long = getItem(position).path.hashCode().toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        TrashItemBinding.inflate(LayoutInflater.from(parent.context))
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val trash = getItem(position)
        binding.root.isChecked = trash.isChecked
        val path = trash.path
        binding.title.text = path
        when {
            trash.isInUse -> {
                binding.summary.isVisible = true
                binding.summary.text = inUse
            }

            RootPreferences.isShowLength -> {
                binding.summary.isVisible = true
                binding.summary.text = Formatter.formatFileSize(activity, trash.length)
            }

            else -> binding.summary.isVisible = false
        }
        val packageInfo = trash.packageInfo
        if (packageInfo != null) {
            binding.icon.setImageBitmap(null)
            binding.apk.load(packageInfo)
        } else {
            binding.icon.setImageBitmap(badge)
            binding.apk.load(null)
        }
        if (viewModel.requestedState == TrashState.Cleaning::class.java) {
            binding.root.isClickable = false
            binding.root.isLongClickable = false
        } else {
            binding.root.setOnClickListener {
                viewModel.toggle(trash)
            }
            binding.root.setOnCreateContextMenuListener { menu, _, _ ->
                activity.menuInflater.inflate(R.menu.trash_items, menu)
                menu.setHeaderTitle(path.substringAfterLast(File.separator))
                if (RootPreferences.noTick.isNotEmpty()) {
                    val pattern = Pattern.compile(RootPreferences.noTick)
                    if (pattern.matcher(path).find()) {
                        menu.removeItem(R.id.menu_no_tick)
                    }
                }
                menu.forEach {
                    it.setOnMenuItemClickListener { item ->
                        onContextItemSelected(item, trash)
                    }
                }
            }
        }
    }

    private fun onContextItemSelected(item: MenuItem, trash: TrashModel): Boolean {
        val path = trash.path
        when (item.itemId) {
            R.id.menu_open -> {
                OpenUtils.open(fragment.requireContext(), path, trash.isDir)
            }

            R.id.menu_copy -> {
                ClipboardUtils.put(activity, path)
                Snackbar.make(
                    activity.findViewById(R.id.hint), fragment.getString(R.string.copied, path),
                    Snackbar.LENGTH_SHORT
                ).show()
            }

            R.id.menu_no_tick -> {
                if (trash.isChecked) {
                    viewModel.toggle(trash)
                }
                RootPreferences.noTick = if (RootPreferences.noTick.isEmpty()) path
                else "${RootPreferences.noTick}|$path"
                if (RootPreferences.isMonitor) {
                    ScannerManager.shScanners.forEach { it.onDestroy() }
                }
            }

            R.id.menu_no_scan -> {
                viewModel.remove(trash)
                RootPreferences.noScan += path
                ScannerManager.fileChanges = FileChanges(listOf(path))
            }

            else -> return false
        }
        return true
    }

    override fun submitList(list: List<TrashModel>?) {
        if (list == null || !tooHeavy(list)) {
            super.submitList(list)
        } else {
            setCurrentList(list) {
                notifyDataSetChanged()
            }
        }
    }

    override fun submitList(list: List<TrashModel>?, commitCallback: Runnable?) {
        if (list == null || !tooHeavy(list)) {
            super.submitList(list, commitCallback)
        } else {
            setCurrentList(list) {
                notifyDataSetChanged()
                commitCallback?.run()
            }
        }
    }

    private fun tooHeavy(list: List<TrashModel>?): Boolean =
        list != null && list.size > SLOP && currentList.size > SLOP

    class ViewHolder(val binding: TrashItemBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {
        private val CALLBACK = object : DiffUtil.ItemCallback<TrashModel>() {
            override fun areItemsTheSame(oldItem: TrashModel, newItem: TrashModel): Boolean =
                oldItem.path == newItem.path

            override fun areContentsTheSame(oldItem: TrashModel, newItem: TrashModel): Boolean =
                oldItem == newItem
        }
        const val SLOP: Int = 999
    }
}
