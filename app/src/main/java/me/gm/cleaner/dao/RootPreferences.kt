package me.gm.cleaner.dao

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import me.gm.cleaner.R
import java.util.Locale

object RootPreferences {
    // THEME
    lateinit var preferences: SharedPreferences
        private set
    private lateinit var res: Resources

    fun init(context: Context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context)
        res = context.resources
    }

    // ENABLE FUNCTIONS
    val isStartOnBoot: Boolean
        get() = preferences.getBoolean(res.getString(R.string.start_on_boot_key), false)
    val isPostNotification: Boolean
        get() = preferences.getBoolean(res.getString(R.string.post_notification_key), true)
    val isShowNonpublic: Boolean
        get() = preferences.getBoolean(res.getString(R.string.nonpublic_key), false)

    // SCAN
    val isShowLength: Boolean
        get() = preferences.getBoolean(res.getString(R.string.length_key), true)
    val isSort: Boolean
        get() = preferences.getBoolean(res.getString(R.string.sort_key), true)
    val isMonitor: Boolean
        get() = preferences.getBoolean(res.getString(R.string.monitor_key), true)
    val maximize: Int
        get() = preferences.getString(res.getString(R.string.maximize_key), null)?.toIntOrNull()
            ?: 50000
    val isConfirmDelete: Boolean
        get() = preferences.getBoolean(res.getString(R.string.confirm_delete_key), true)
    val isScanSystemApp: Boolean
        get() = preferences.getBoolean(res.getString(R.string.scan_system_app_key), false)

    var noTick: String
        get() = preferences.getString(res.getString(R.string.no_tick_key), "")!!
        set(value) = preferences.edit {
            putString(res.getString(R.string.no_tick_key), value)
        }
    var noScan: Set<String>
        get() = preferences.getStringSet(res.getString(R.string.no_scan_key), emptySet())!!
        set(value) = preferences.edit {
            putStringSet(res.getString(R.string.no_scan_key), value)
        }

    // LANGUAGE
    val locale: Locale
        get() {
            val tag = preferences.getString(res.getString(R.string.language_key), "SYSTEM")!!
            return if ("SYSTEM" == tag) Locale.getDefault() else Locale.forLanguageTag(tag)
        }

    // THEME
    val material3: Boolean
        get() = preferences.getBoolean(res.getString(R.string.theme_m3_key), true)
}
