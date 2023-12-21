package me.gm.cleaner.nio.fs

import android.system.ErrnoException
import android.system.Os
import java.nio.file.FileSystem
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.Path
import kotlin.io.path.pathString

private class StructStatFileSystem(fileSystem: FileSystem) :
    DelegateFileSystem(StructStatProvider(fileSystem), fileSystem)

private class StructStatProvider(fileSystem: FileSystem) :
    DelegateFileSystem.DelegateProvider(fileSystem) {

    override fun <A : BasicFileAttributes?> readAttributes(
        file: Path, type: Class<A>, vararg options: LinkOption
    ): A {
        val followLinks = !options.contains(LinkOption.NOFOLLOW_LINKS)
        val stat = try {
            if (followLinks) {
                Os.stat(file.pathString)
            } else {
                Os.lstat(file.pathString)
            }
        } catch (e: ErrnoException) {
            throw e.rethrowAsIOException(file)
        }
        return StructStatFileAttributesImpl(stat) as A
    }
}

class StructStatPath private constructor(path: Path) :
    DelegateFileSystem.DelegatePath(StructStatFileSystem(path.fileSystem), path) {

    companion object {
        fun get(path: String): StructStatPath = StructStatPath(Path(path))
        fun wrap(path: Path): StructStatPath = StructStatPath(path)
    }
}
