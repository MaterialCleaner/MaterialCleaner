package me.gm.cleaner.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ParceledPath(
    val ASCIIString: String,
    val attrs: ParceledBasicFileAttributes
) : Parcelable
