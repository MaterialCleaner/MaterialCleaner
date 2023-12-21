package me.gm.cleaner.util

import android.content.Context
import android.icu.text.ListFormatter
import android.os.Build
import android.text.format.DateUtils
import me.gm.cleaner.dao.RootPreferences
import java.util.Calendar

object FormatUtils {

    private val then: Calendar = Calendar.getInstance()
    private val now: Calendar = Calendar.getInstance()

    fun formatDateTime(context: Context, timeMillis: Long): String {
        then.timeInMillis = timeMillis
        now.timeInMillis = System.currentTimeMillis()
        val flags = DateUtils.FORMAT_NO_NOON or DateUtils.FORMAT_NO_MIDNIGHT or
                DateUtils.FORMAT_ABBREV_ALL or DateUtils.FORMAT_SHOW_TIME or when {
            then[Calendar.YEAR] != now[Calendar.YEAR] -> DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_SHOW_DATE
            then[Calendar.DAY_OF_YEAR] != now[Calendar.DAY_OF_YEAR] -> DateUtils.FORMAT_SHOW_DATE
            else -> 0
        }
        return DateUtils.formatDateTime(context, timeMillis, flags)
    }
}

fun Collection<*>.listFormat(delimiter: String): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        ListFormatter.getInstance(RootPreferences.locale).format(this)
    } else {
        joinToString(delimiter)
    }
