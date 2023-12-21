package me.gm.cleaner.home

import android.content.pm.PackageInfo
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import me.gm.cleaner.dao.AppLabelCache

@Parcelize
data class TrashModel(
    val path: String,
    val isDir: Boolean,
    val isChecked: Boolean,
    val isInUse: Boolean,
    val length: Long,
    val packageInfo: PackageInfo?,
    val label: String?,
) : Parcelable {

    class Builder(
        var path: String,
        var isDir: Boolean,
        var isChecked: Boolean = true,
        var isInUse: Boolean = false,
        var length: Long = 0,
        private var packageInfo: PackageInfo? = null,
        private var label: String? = null,
    ) {

        fun setPackageInfo(packageInfo: PackageInfo?): Builder {
            this.packageInfo = packageInfo
            label = if (packageInfo == null) {
                null
            } else {
                AppLabelCache.getPackageLabel(packageInfo)
            }
            return this
        }

        fun setPackageArchiveInfo(packageInfo: PackageInfo?): Builder {
            this.packageInfo = packageInfo
            label = if (packageInfo == null) {
                null
            } else {
                AppLabelCache.getPackageLabelFromPackageManager(packageInfo.applicationInfo)
            }
            return this
        }

        fun build(): TrashModel =
            TrashModel(path, isDir, isChecked, isInUse, length, packageInfo, label)
    }
}
