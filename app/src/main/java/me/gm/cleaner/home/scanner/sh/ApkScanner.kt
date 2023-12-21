package me.gm.cleaner.home.scanner.sh

import android.content.pm.PackageManager
import me.gm.cleaner.R
import me.gm.cleaner.home.StaticScanner
import me.gm.cleaner.home.TrashModel
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.extension

class ApkScanner(
    info: StaticScanner = StaticScanner(
        title = R.string.apk,
        icon = R.drawable.ic_outline_android_24,
        scannerClass = ApkScanner::class.java,
        viewModelClass = ApkViewModel::class.java,
        serviceClass = null
    )
) : BaseShScanner(info) {
    override val String.isInUse: Boolean
        get() = false
    private val pm: PackageManager = activity.packageManager

    override fun visitFile(file: Path, attrs: BasicFileAttributes): Boolean {
        val extension = file.extension
        return extension.contains("apk", true) && extension.length <= 4
    }

    override fun onTrashFound(trashBuilder: TrashModel.Builder) {
        val path = trashBuilder.path
        val pi = pm.getPackageArchiveInfo(path, 0)?.apply {
            applicationInfo?.apply {
                sourceDir = path
                publicSourceDir = path
            }
        }
        if (pi == null) {
            trashBuilder.isChecked = false
        } else {
            trashBuilder.setPackageArchiveInfo(pi)
        }
        super.onTrashFound(trashBuilder)
    }
}
