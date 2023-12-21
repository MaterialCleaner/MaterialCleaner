package me.gm.cleaner.home.scanner

import me.gm.cleaner.home.TrashModel
import me.gm.cleaner.nio.file.SimpleProgressedFileVisitor
import me.gm.cleaner.util.FileUtils
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.pathString
import kotlin.io.path.visitFileTree

abstract class ScannerFileVisitor(
    private val noScanPaths: Set<String>, private val predicate: ScannerPredicate
) : SimpleProgressedFileVisitor<Path>() {

    @OptIn(ExperimentalPathApi::class)
    override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
        if (noScanPaths.contains(dir.pathString)) {
            return FileVisitResult.SKIP_SUBTREE
        }
        if (predicate.preVisitDirectory(dir, attrs)) {
            if (noScanPaths.any { FileUtils.startsWith(dir, it) }) {
                dir.visitFileTree {
                    onPreVisitDirectory { directory, attributes ->
                        if (noScanPaths.none { FileUtils.startsWith(directory, it) }) {
                            onTrashFound(
                                TrashModel.Builder(directory.pathString, attributes.isDirectory)
                            )
                        }
                        FileVisitResult.CONTINUE
                    }
                    onVisitFile { file, attributes ->
                        if (!noScanPaths.contains(file.pathString)) {
                            onTrashFound(
                                TrashModel.Builder(file.pathString, attributes.isDirectory)
                            )
                        }
                        FileVisitResult.CONTINUE
                    }
                }
            } else {
                onTrashFound(TrashModel.Builder(dir.pathString, attrs.isDirectory))
            }
            return FileVisitResult.SKIP_SUBTREE
        }
        return super.preVisitDirectory(dir, attrs)
    }

    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
        if (!noScanPaths.contains(file.pathString) && predicate.visitFile(file, attrs)) {
            onTrashFound(TrashModel.Builder(file.pathString, attrs.isDirectory))
        }
        return super.visitFile(file, attrs)
    }

    override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult =
        FileVisitResult.CONTINUE

    override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
        if (predicate.postVisitDirectory(dir)) {
            onTrashFound(TrashModel.Builder(dir.pathString, true))
        }
        return FileVisitResult.CONTINUE
    }

    abstract fun onTrashFound(trashBuilder: TrashModel.Builder)
}
