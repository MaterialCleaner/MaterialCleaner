package me.gm.cleaner.nio.fs

import android.os.Build
import android.system.ErrnoException
import android.system.OsConstants
import android.system.StructStat
import me.gm.cleaner.model.UnixFileKey
import java.io.IOException
import java.nio.file.AccessDeniedException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.FileSystemException
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.util.concurrent.TimeUnit
import kotlin.io.path.pathString

private fun toFileTime(sec: Long, nsec: Long): FileTime =
    if (nsec == 0L) {
        FileTime.from(sec, TimeUnit.SECONDS)
    } else {
        // truncate to microseconds to avoid overflow with timestamps
        // way out into the future. We can re-visit this if FileTime
        // is updated to define a from(secs,nsecs) method.
        val micro = sec * 1000000L + nsec / 1000L
        FileTime.from(micro, TimeUnit.MICROSECONDS)
    }

fun StructStat.lastModifiedTime(): FileTime = toFileTime(
    st_mtime,
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        st_mtim.tv_nsec
    } else {
        0L
    }
)

fun StructStat.lastAccessTime(): FileTime = toFileTime(
    st_atime,
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        st_atim.tv_nsec
    } else {
        0L
    }
)

fun StructStat.ctime(): FileTime = toFileTime(
    st_ctime,
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        st_ctim.tv_nsec
    } else {
        0L
    }
)

fun StructStat.isRegularFile(): Boolean = OsConstants.S_ISREG(st_mode)

fun StructStat.isDirectory(): Boolean = OsConstants.S_ISDIR(st_mode)

fun StructStat.isSymbolicLink(): Boolean = OsConstants.S_ISLNK(st_mode)

fun StructStat.isOther(): Boolean {
    val type = st_mode and OsConstants.S_IFMT
    return type != OsConstants.S_IFREG &&
            type != OsConstants.S_IFDIR &&
            type != OsConstants.S_IFLNK
}

fun StructStat.size(): Long = st_size

fun StructStat.fileKey(): UnixFileKey = UnixFileKey(st_dev, st_ino)

fun StructStat.linkCount(): Long = st_nlink

@Throws(IOException::class)
fun ErrnoException.rethrowAsIOException(file: Path): IOException {
    val x = when (errno) {
        OsConstants.EACCES -> AccessDeniedException(file.pathString, null, message)
        OsConstants.ENOENT -> NoSuchFileException(file.pathString, null, message)
        OsConstants.EEXIST -> FileAlreadyExistsException(file.pathString, null, message)
        else -> FileSystemException(file.pathString, null, message)
    }
    throw x
}
