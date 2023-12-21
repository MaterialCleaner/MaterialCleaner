package me.gm.cleaner.client

import android.os.FileObserver
import androidx.fragment.app.Fragment
import me.gm.cleaner.R

fun getPathWithEvent(path: String, flags: Int, fragment: Fragment): CharSequence {
    val eventStrId = when {
        hasFlag(flags, FileObserver.CREATE) -> R.string.filesystem_record_flags_create
        hasFlag(flags, FileObserver.OPEN) &&
                !hasFlag(flags, FileObserver.CREATE) -> R.string.filesystem_record_flags_open
        hasFlag(flags, FileObserver.DELETE) -> R.string.filesystem_record_flags_delete
        hasFlag(flags, FileObserver.MOVED_FROM) -> R.string.filesystem_record_flags_move_from
        hasFlag(flags, FileObserver.MOVED_TO) -> R.string.filesystem_record_flags_move_to
        else -> throw IllegalArgumentException()
    }
    val objectStrId = if (hasFlag(flags, 0x40000000)) R.string.filesystem_record_flags_dir
    else R.string.filesystem_record_flags_file
    return fragment.getString(eventStrId, fragment.getString(objectStrId), path)
}

private fun hasFlag(flags: Int, mask: Int): Boolean = flags and mask != 0
