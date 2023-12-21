package me.gm.cleaner.dao

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.preference.PreferenceManager
import me.gm.cleaner.R
import me.gm.cleaner.util.encodeToString
import java.io.File

object ManualToolsPreferences {
    const val SORT_BY_NAME: Int = 0
    const val SORT_BY_SIZE: Int = 1
    const val SORT_BY_TIME: Int = 2
    private lateinit var preferences: SharedPreferences
    private lateinit var res: Resources
    private lateinit var dataDir: File
    private lateinit var storageManager: StorageManager
    lateinit var snapshotDir: File
        private set
    lateinit var backupDir: File
        private set

    fun init(context: Context, dpsContext: Context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(dpsContext)
        res = context.resources
        dataDir = context.dataDir.parentFile!!
        storageManager = context.getSystemService()!!
        snapshotDir = context.filesDir.resolve("snapshot")
        backupDir = context.filesDir.resolve("backup")
    }

    var bookmarks: List<String>
        @SuppressLint("SoonBlockedPrivateApi")
        get() = preferences.getStringList(
            res.getString(R.string.bookmarks_key),
            run {
                val getPathFileMethod = StorageVolume::class.java.getDeclaredMethod("getPathFile")
                getPathFileMethod.isAccessible = true
                storageManager.storageVolumes.mapTo(mutableListOf(dataDir.toPath().encodeToString())) {
                    val volumeFile = getPathFileMethod.invoke(it) as File
                    volumeFile.toPath().encodeToString()
                }
            }
        )!!
        set(value) = preferences.edit {
            putStringList(res.getString(R.string.bookmarks_key), value)
        }

    var sortBy: Int
        get() = preferences.getInt(res.getString(R.string.browser_sort_by_key), SORT_BY_NAME)
        set(value) {
            preferences.edit { putInt(res.getString(R.string.browser_sort_by_key), value) }
        }

    var sortByDirsFirst: Boolean
        get() = preferences.getBoolean(res.getString(R.string.browser_sort_by_dirs_first_key), true)
        set(value) {
            preferences.edit {
                putBoolean(res.getString(R.string.browser_sort_by_dirs_first_key), value)
            }
        }
}
