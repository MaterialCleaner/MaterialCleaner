package me.gm.cleaner.home.scanner.sh

import android.provider.MediaStore.Files.FileColumns
import me.gm.cleaner.R
import me.gm.cleaner.client.ui.storageredirect.MimeUtils
import me.gm.cleaner.home.StaticScanner
import me.gm.cleaner.util.listDirectoryEntriesSafe
import java.io.IOException
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.Path
import kotlin.io.path.fileSize
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.readAttributes

class SmallScanner(
    info: StaticScanner = StaticScanner(
        title = R.string.small,
        icon = R.drawable.ic_outline_small_24,
        scannerClass = SmallScanner::class.java,
        viewModelClass = SmallViewModel::class.java,
        serviceClass = null
    )
) : BaseShScanner(info) {
    override val String.isInUse: Boolean
        get() = false

    override fun initScanPaths(): List<Path> =
        if (viewModel.isAcceptInheritance) {
            val notExist = mutableListOf<Path>()
            val existPaths = mutableListOf<Path>()
            changedPaths.asSequence()
                .map { Path(it) }
                .forEach { path ->
                    try {
                        val attrs =
                            path.readAttributes<BasicFileAttributes>(LinkOption.NOFOLLOW_LINKS)
                        if (attrs.isDirectory) {
                            existPaths.add(path)
                        } else {
                            existPaths.add(path.parent)
                        }
                    } catch (e: IOException) {
                        notExist.add(path)
                    }
                }
            val scanPaths = distinctChangedPaths(existPaths)
            removeChangedInheritance(notExist)
            removeChangedInheritance(scanPaths)
            scanPaths
        } else {
            super.initScanPaths()
        }

    private fun predicate(file: Path): Boolean {
        val name = file.name
        return file.fileSize() < 1000 && name != ".nomedia" && (
                name.startsWith(".") ||
                        MimeUtils.resolveMediaType(MimeUtils.resolveMimeType(file.toFile())) ==
                        FileColumns.MEDIA_TYPE_NONE
                )
    }

    override fun visitFile(file: Path, attrs: BasicFileAttributes): Boolean =
        predicate(file) || file.name == ".nomedia" && try {
            file.parent
                .listDirectoryEntriesSafe()
                .all {
                    it.isDirectory(LinkOption.NOFOLLOW_LINKS) || it.name == ".nomedia" ||
                            predicate(it)
                }
        } catch (e: Exception) {
            false
        }
}
