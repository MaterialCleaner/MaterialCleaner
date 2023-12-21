package me.gm.cleaner.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UnixFileKey(
    val st_dev: Long,
    val st_ino: Long,
) : Parcelable
