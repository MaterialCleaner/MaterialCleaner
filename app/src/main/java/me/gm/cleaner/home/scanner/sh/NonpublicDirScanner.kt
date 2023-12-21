package me.gm.cleaner.home.scanner.sh

import android.annotation.SuppressLint
import me.gm.cleaner.R
import me.gm.cleaner.dao.RootPreferences
import me.gm.cleaner.home.StaticScanner
import me.gm.cleaner.home.TrashModel
import me.gm.cleaner.util.FileUtils
import me.gm.cleaner.util.listDirectoryEntriesSafe
import java.nio.file.LinkOption
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.pathString

class NonpublicDirScanner(
    info: StaticScanner = StaticScanner(
        title = R.string.nonpublic,
        icon = R.drawable.ic_outline_standard_folder_24,
        scannerClass = NonpublicDirScanner::class.java,
        viewModelClass = NonpublicDirViewModel::class.java,
        serviceClass = null
    )
) : BaseShScanner(info) {

    override fun initScanPaths(): List<Path> =
        if (viewModel.isAcceptInheritance) {
            val scanPaths = distinctChangedPaths(
                changedPaths.map { changedPath -> Path(toRootDir(changedPath)) }
            )
            removeChangedInheritance(scanPaths)
            scanPaths
        } else {
            viewModel.removeTrashIf { true }
            externalStorageDir.toPath().listDirectoryEntriesSafe()
        }

    @SuppressLint("SoonBlockedPrivateApi")
    override suspend fun onScan() {
        val scanPaths = initScanPaths()
        val noScanPaths = RootPreferences.noScan + FileUtils.androidDir.path
        scanPaths.forEachIndexed { i, path ->
            viewModel.progress = 100 * i / scanPaths.size
            if (path.isDirectory(LinkOption.NOFOLLOW_LINKS) &&
                !noScanPaths.any { path.startsWith(it) } &&
                !FileUtils.isStandardDirectory(path.name)
            ) {
                onTrashFound(TrashModel.Builder(path.pathString, true))
            }
        }
    }
}
