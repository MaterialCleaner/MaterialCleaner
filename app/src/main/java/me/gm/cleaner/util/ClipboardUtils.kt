package me.gm.cleaner.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.core.content.getSystemService

// https://developer.android.com/about/versions/13/features/copy-paste

object ClipboardUtils {

    fun put(context: Context, str: CharSequence): Boolean {
        return put(context, ClipData.newPlainText("", str))
    }

    fun put(context: Context, clipData: ClipData): Boolean {
        return try {
            val clipboard = context.getSystemService<ClipboardManager>()!!
            clipboard.setPrimaryClip(clipData)
            true
        } catch (ignored: Exception) {
            false
        }
    }
}
