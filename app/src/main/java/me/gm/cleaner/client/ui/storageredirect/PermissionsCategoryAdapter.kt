package me.gm.cleaner.client.ui.storageredirect

import android.Manifest
import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.collection.ArrayMap
import androidx.collection.arrayMapOf
import androidx.recyclerview.widget.RecyclerView
import me.gm.cleaner.R
import me.gm.cleaner.client.CleanerClient
import me.gm.cleaner.databinding.StorageRedirectCategoryStoragePermissionsHeaderBinding
import me.gm.cleaner.databinding.StorageRedirectCategoryStoragePermissionsItemBinding
import me.gm.cleaner.util.DividerViewHolder
import rikka.preference.simplemenu.SimpleMenuPopupWindow

@SuppressLint("RestrictedApi")
class PermissionsCategoryAdapter(
    private val permissions: List<String>,
    private val ai: ApplicationInfo,
    private val viewModel: StorageRedirectViewModel
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemViewType(position: Int): Int = when (position) {
        0 -> R.layout.storage_redirect_category_storage_permissions_header
        else -> R.layout.storage_redirect_category_storage_permissions_item
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            R.layout.storage_redirect_category_storage_permissions_header -> HeaderViewHolder(
                StorageRedirectCategoryStoragePermissionsHeaderBinding.inflate(
                    LayoutInflater.from(parent.context)
                )
            )

            R.layout.storage_redirect_category_storage_permissions_item -> ItemViewHolder(
                StorageRedirectCategoryStoragePermissionsItemBinding.inflate(
                    LayoutInflater.from(parent.context)
                )
            )

            else -> throw IndexOutOfBoundsException()
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (position == 0) {
            val binding = (holder as HeaderViewHolder).binding
            return
        }
        val permission = permissions[position - 1]
        val grantResult = CleanerClient.service!!.getPackagePermission(
            ai, permission, viewModel.isRuntime(permission)
        )
        viewModel.permissionToGrant[permission] = grantResult
        val binding = (holder as ItemViewHolder).binding
        binding.title.setText(storagePermissionTranslations[permission]!!)
        binding.summary.setText(checkPermissionResultTranslations[grantResult]!!)

        val context = binding.root.context
        val popupWindow = SimpleMenuPopupWindow(context)
        popupWindow.setEntries(
            checkPermissionResultTranslations.values
                .map { context.getString(it) }
                .subList(0, 2)
                .toTypedArray()
        )
        popupWindow.setSelectedIndex(checkPermissionResultTranslations.keys.indexOf(grantResult))
        popupWindow.onItemClickListener = SimpleMenuPopupWindow.OnItemClickListener { i ->
            val clicked = checkPermissionResultTranslations.keys.toList()[i]
            try {
                when (clicked) {
                    PackageManager.PERMISSION_GRANTED -> viewModel.setPackagePermission(
                        ai, permission, true
                    )
                    PackageManager.PERMISSION_DENIED -> viewModel.setPackagePermission(
                        ai, permission, false
                    )
                }
                notifyItemChanged(position)
            } catch (tr: Throwable) {
                Toast.makeText(context, tr.message!!, Toast.LENGTH_SHORT).show()
            }
        }
        binding.root.setOnClickListener {
            val itemView = holder.itemView
            popupWindow.show(itemView, itemView.parent as View, binding.empty.x.toInt())
        }
    }

    override fun getItemCount(): Int = 1 + permissions.size

    class HeaderViewHolder(val binding: StorageRedirectCategoryStoragePermissionsHeaderBinding) :
        DividerViewHolder(binding.root) {
        init {
            isDividerAllowedAbove = true
        }
    }

    class ItemViewHolder(val binding: StorageRedirectCategoryStoragePermissionsItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    companion object {
        private val storagePermissionTranslations: ArrayMap<String, Int> = arrayMapOf(
            Manifest.permission.READ_EXTERNAL_STORAGE to R.string.storage_redirect_storage_permissions_read,
            Manifest.permission.WRITE_EXTERNAL_STORAGE to R.string.storage_redirect_storage_permissions_write,
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                put(
                    Manifest.permission.MANAGE_EXTERNAL_STORAGE,
                    R.string.storage_redirect_storage_permissions_manage
                )
            }
        }
        private val checkPermissionResultTranslations: LinkedHashMap<Int, Int> = linkedMapOf(
            PackageManager.PERMISSION_GRANTED to R.string.storage_redirect_storage_permissions_granted,
            PackageManager.PERMISSION_DENIED to R.string.storage_redirect_storage_permissions_denied,
            AppOpsManager.MODE_IGNORED to R.string.storage_redirect_storage_permissions_ignored,
        )
    }
}
