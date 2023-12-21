package me.gm.cleaner.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ParceledBasicFileAttributes(
    val lastModifiedTime: Long,
    val lastAccessTime: Long,
    val creationTime: Long,
    val isRegularFile: Boolean,
    val isDirectory: Boolean,
    val isSymbolicLink: Boolean,
    val isOther: Boolean,
    val size: Long,
    val fileKey: UnixFileKey,
) : Parcelable
