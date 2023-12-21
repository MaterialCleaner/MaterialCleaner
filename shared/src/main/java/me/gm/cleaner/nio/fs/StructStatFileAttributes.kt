package me.gm.cleaner.nio.fs

import android.system.StructStat
import me.gm.cleaner.model.UnixFileKey
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime

interface StructStatFileAttributes : BasicFileAttributes {

    override fun fileKey(): UnixFileKey

    fun getStructStat(): StructStat

    fun linkCount(): Long
}

open class StructStatFileAttributesImpl(private val stat: StructStat) : StructStatFileAttributes {

    override fun lastModifiedTime(): FileTime = stat.lastModifiedTime()

    override fun lastAccessTime(): FileTime = stat.lastAccessTime()

    override fun creationTime(): FileTime = lastModifiedTime()

    override fun isRegularFile(): Boolean = stat.isRegularFile()

    override fun isDirectory(): Boolean = stat.isDirectory()

    override fun isSymbolicLink(): Boolean = stat.isSymbolicLink()

    override fun isOther(): Boolean = stat.isOther()

    override fun size(): Long = stat.size()

    override fun fileKey(): UnixFileKey = stat.fileKey()

    override fun getStructStat(): StructStat = stat

    override fun linkCount(): Long = stat.linkCount()
}
