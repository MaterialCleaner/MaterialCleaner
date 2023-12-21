package me.gm.cleaner.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class FileSystemEvent(
    val timeMillis: Long,
    val packageName: String,
    val path: String,
    val flags: Int,
) : Parcelable
