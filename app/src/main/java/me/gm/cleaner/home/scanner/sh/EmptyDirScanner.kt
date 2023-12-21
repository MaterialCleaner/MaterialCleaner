package me.gm.cleaner.home.scanner.sh

import me.gm.cleaner.R
import me.gm.cleaner.home.StaticScanner
import me.gm.cleaner.home.TrashModel
import me.gm.cleaner.util.listDirectoryEntriesSafe
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.Path
import kotlin.io.path.pathString

class EmptyDirScanner(
    info: StaticScanner = StaticScanner(
        title = R.string.empty,
        icon = R.drawable.ic_outline_empty_folder_24,
        scannerClass = EmptyDirScanner::class.java,
        viewModelClass = EmptyDirViewModel::class.java,
        serviceClass = null
    )
) : BaseShScanner(info) {

    override fun removeChangedInheritance(scanPaths: List<Path>) {
        super.removeChangedInheritance(scanPaths)
        viewModel.removeTrashIf {
            val child = it.path
            for (parent in scanPaths) {
                if (parent.startsWith(child)) {
                    return@removeTrashIf true
                }
            }
            false
        }
    }

    private var inheritance: Set<String>? = null

    override fun initScanPaths(): List<Path> =
        if (viewModel.isAcceptInheritance) {
            removeChangedInheritance(changedPaths.map { Path(it) })
            val scanPaths = distinctChangedPaths(
                changedPaths.map { changedPath -> Path(toRootDir(changedPath)) }
            )
            inheritance = viewModel.trashes.map { it.path }.toSet()
            scanPaths
        } else {
            super.initScanPaths()
        }

    override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): Boolean =
        viewModel.isAcceptInheritance && inheritance!!.contains(dir.pathString)

    override fun postVisitDirectory(dir: Path): Boolean {
        val ls = dir.listDirectoryEntriesSafe().map { it.pathString }
        if (ls.isEmpty()) {
            return true
        } else {
            val trashes = viewModel.trashes.map { it.path }
            if (trashes.containsAll(ls)) {
                viewModel.removeTrashIf { ls.contains(it.path) } // removeAll(ls)
                return true
            }
        }
        return false
    }

    override fun onTrashFound(trashBuilder: TrashModel.Builder) {
        if (viewModel.isAcceptInheritance && inheritance!!.contains(trashBuilder.path)) {
            return
        } else {
            super.onTrashFound(trashBuilder)
        }
    }

    override fun onFinish(reason: Int) {
        super.onFinish(reason)
        inheritance = null
    }
}
