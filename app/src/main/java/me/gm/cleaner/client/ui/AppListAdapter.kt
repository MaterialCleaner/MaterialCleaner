package me.gm.cleaner.client.ui

import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.core.view.forEach
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.BaseKtListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.gm.cleaner.R
import me.gm.cleaner.client.CleanerClient
import me.gm.cleaner.client.getSharedProcessPackages
import me.gm.cleaner.client.getSharedUserIdPackages
import me.gm.cleaner.dao.ServicePreferences
import me.gm.cleaner.databinding.ApplistItemBinding
import me.gm.cleaner.util.buildStyledTitle

class AppListAdapter(private val fragment: AppListFragment) :
    BaseKtListAdapter<AppListModel, AppListAdapter.ViewHolder>(CALLBACK) {
    private val activity: ServiceSettingsActivity =
        fragment.requireActivity() as ServiceSettingsActivity

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        ApplistItemBinding.inflate(LayoutInflater.from(parent.context))
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val model = getItem(position)
        binding.icon.load(model.packageInfo)
        binding.title.text = model.label
        binding.summary.text = run {
            val summary = mutableListOf<CharSequence>()
            if (model.mountRulesCount > 0) {
                summary += fragment.getString(
                    R.string.enabled_mount_rules_count, model.mountRulesCount
                )
            }
            if (model.readOnlyCount > 0) {
                summary += fragment.getString(R.string.enabled_read_only_count, model.readOnlyCount)
            }
            if (summary.isNotEmpty()) {
                activity.buildStyledTitle(
                    fragment.getString(
                        R.string.enabled,
                        summary.joinToString(fragment.getString(R.string.delimiter))
                    )
                )
            } else {
                model.packageInfo.packageName
            }
        }
        binding.status.text = when (model.mountState) {
            AppListModel.STATE_UNMOUNTED -> null
            AppListModel.STATE_MOUNTED -> activity.buildStyledTitle(
                fragment.getString(R.string.storage_redirect_list_mounted)
            )

            AppListModel.STATE_UNKNOWN -> activity.buildStyledTitle(
                fragment.getString(R.string.storage_redirect_list_unknown)
            )

            AppListModel.STATE_MOUNT_EXCEPTION -> activity.buildStyledTitle(
                fragment.getString(R.string.storage_redirect_list_mount_exception),
                color = activity.getColor(R.color.color_warning)
            )

            else -> throw IndexOutOfBoundsException()
        }
        binding.root.setOnClickListener {
            val navController = fragment.findNavController()
            if (navController.currentDestination?.id == R.id.service_settings_fragment) {
                val direction = ServiceSettingsFragmentDirections
                    .serviceSettingsToStorageRedirectAction(model.packageInfo)
                navController.navigate(direction)
            }
        }
        binding.root.setOnCreateContextMenuListener { menu, _, _ ->
            activity.menuInflater.inflate(R.menu.applist_item, menu)
            menu.setHeaderTitle(model.label)
            if (model.mountRulesCount == 0) {
                menu.removeItem(R.id.menu_delete_all_mount_rules)
            }
            if (model.readOnlyCount == 0) {
                menu.removeItem(R.id.menu_delete_all_read_only)
            }
            menu.forEach {
                it.setOnMenuItemClickListener { item ->
                    onContextItemSelected(item, model)
                }
            }
        }
    }

    private fun onContextItemSelected(item: MenuItem, model: AppListModel): Boolean =
        when (item.itemId) {
            R.id.menu_delete_all_mount_rules -> {
                fragment.lifecycleScope.launch(Dispatchers.IO) {
                    val sharedProcessPackages = getSharedProcessPackages(model.packageInfo)
                        .map { it.packageName }
                    ServicePreferences.removeStorageRedirect(sharedProcessPackages)
                    CleanerClient.service!!.notifySrChanged()
                    if (model.mountState != AppListModel.STATE_UNMOUNTED) {
                        CleanerClient.service!!.remount(sharedProcessPackages.toTypedArray())
                    }
                }
                true
            }

            R.id.menu_delete_all_read_only -> {
                fragment.lifecycleScope.launch(Dispatchers.IO) {
                    val sharedUserIdPackages = getSharedUserIdPackages(model.packageInfo)
                        .map { it.packageName }
                    ServicePreferences.removeReadOnly(sharedUserIdPackages)
                    CleanerClient.service!!.notifyReadOnlyChanged()
                }
                true
            }

            else -> false
        }

    class ViewHolder(val binding: ApplistItemBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {
        private val CALLBACK = object : DiffUtil.ItemCallback<AppListModel>() {
            override fun areItemsTheSame(
                oldItem: AppListModel, newItem: AppListModel
            ): Boolean = oldItem.packageInfo.packageName == newItem.packageInfo.packageName

            override fun areContentsTheSame(
                oldItem: AppListModel, newItem: AppListModel
            ): Boolean = oldItem == newItem
        }
    }
}
