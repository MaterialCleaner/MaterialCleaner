package me.gm.cleaner.dao

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import me.gm.cleaner.SharedConstants
import me.gm.cleaner.annotation.App
import me.gm.cleaner.annotation.Server
import me.gm.cleaner.shared.R
import me.gm.cleaner.util.FileUtils
import me.gm.cleaner.util.toList
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.ByteBuffer

object ServicePreferences {
    const val SORT_BY_NAME: Int = 0
    const val SORT_BY_UPDATE_TIME: Int = 1
    private var broadcasting: Boolean = false
    private val _preferencesChangeLiveData: MutableLiveData<SharedPreferences> = MutableLiveData()
    val preferencesChangeLiveData: LiveData<SharedPreferences>
        get() = _preferencesChangeLiveData
    lateinit var preferences: SharedPreferences
        private set
    private lateinit var res: Resources

    private lateinit var storageRedirectFile: File
    private var inBatch: Boolean = false
    private var storageRedirectCache: JSONObject? = null

    private lateinit var readOnlyFile: File
    private var readOnlyCache: JSONObject? = null

    private lateinit var denylistFile: File
    private var denylistCache: List<String>? = null

    @App
    @Server
    fun init(context: Context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context)
        res = context.resources
        storageRedirectFile = context.filesDir.resolve(SharedConstants.PREF_STORAGE_REDIRECT)
        readOnlyFile = context.filesDir.resolve(SharedConstants.READ_ONLY)
        denylistFile = context.filesDir.resolve(SharedConstants.DENY_LIST_KEY)
        readStorageRedirect()
    }

    private fun notifyListeners() {
        if (broadcasting) {
            return
        }
        broadcasting = true
        _preferencesChangeLiveData.postValue(preferences)
        broadcasting = false
    }

    // APP LIST CONFIG
    @App
    var sortBy: Int
        get() = preferences.getInt(res.getString(R.string.sort_by_key), SORT_BY_NAME)
        set(value) {
            preferences.edit {
                putInt(res.getString(R.string.sort_by_key), value)
            }
            notifyListeners()
        }

    @App
    var ruleCount: Boolean
        get() = preferences.getBoolean(res.getString(R.string.menu_rule_count_key), true)
        set(value) = putBoolean(R.string.menu_rule_count_key, value)

    @App
    var mountState: Boolean
        get() = preferences.getBoolean(res.getString(R.string.menu_mount_state_key), true)
        set(value) = putBoolean(R.string.menu_mount_state_key, value)

    @App
    var isHideSystemApp: Boolean
        get() = preferences.getBoolean(res.getString(R.string.menu_hide_system_app_key), true)
        set(value) = putBoolean(R.string.menu_hide_system_app_key, value)

    @App
    var isHideDisabledApp: Boolean
        get() = preferences.getBoolean(res.getString(R.string.menu_hide_disabled_app_key), true)
        set(value) = putBoolean(R.string.menu_hide_disabled_app_key, value)

    @App
    var isHideNoStoragePermissionApp: Boolean
        get() = preferences.getBoolean(
            res.getString(R.string.menu_hide_no_storage_permissions_key), false
        )
        set(value) = putBoolean(R.string.menu_hide_no_storage_permissions_key, value)

    @App
    var isHideAppSpecificStorage: Boolean
        get() = preferences.getBoolean(
            res.getString(R.string.menu_hide_app_specific_storage_key), false
        )
        set(value) = putBoolean(R.string.menu_hide_app_specific_storage_key, value)

    private fun putBoolean(@StringRes key: Int, value: Boolean) {
        preferences.edit {
            putBoolean(res.getString(key), value)
        }
        notifyListeners()
    }

    // STORAGE REDIRECT
    @App
    fun putStorageRedirect(rawRules: List<Pair<String, String>>, packageNames: List<String>) {
        if (rawRules.isEmpty()) {
            removeStorageRedirect(packageNames)
            return
        }
        val rules = JSONArray()
        rawRules.forEach { rules.put(JSONArray(it.toList())) }
        val all = readStorageRedirect()
        packageNames.forEach { all.put(it, rules) }
        writeStorageRedirect(all)
    }

    @App
    fun removeStorageRedirect(packageNames: List<String>) {
        val all = readStorageRedirect()
        packageNames.forEach { all.remove(it) }
        writeStorageRedirect(all)
    }

    @App
    fun getUninstalledSrPackages(installedPackages: Set<String>): List<String> {
        val packages = readStorageRedirect().keys().asSequence()
        return (packages - installedPackages).toList()
    }

    @App
    @Server
    val srPackages: Set<String>
        get() = readStorageRedirect().keys().asSequence().toSet()

    @App
    @Server
    val srRulesCount: Int
        get() {
            var count = 0
            val all = readStorageRedirect()
            all.keys().forEach {
                count += all.getJSONArray(it).length()
            }
            return count
        }

    @App
    @Server
    fun getPackageSrCount(packageName: String): Int {
        val all = readStorageRedirect()
        if (all.has(packageName)) {
            return all.getJSONArray(packageName).length()
        }
        return 0
    }

    @App
    @Server
    fun getPackageSr(packageName: String, userId: Int): Pair<List<String>, List<String>> {
        val source = mutableListOf<String>()
        val target = mutableListOf<String>()
        val all = readStorageRedirect()
        if (all.has(packageName)) {
            val rules = all.getJSONArray(packageName)
            for (i in 0 until rules.length()) {
                val rule = rules.getJSONArray(i)
                source.add(getPathAsUserQuickly(rule.getString(0), userId))
                target.add(getPathAsUserQuickly(rule.getString(1), userId))
            }
        }
        return source to target
    }

    @App
    @Server
    fun getPackageSrZipped(packageName: String, userId: Int = 0): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        val all = readStorageRedirect()
        if (all.has(packageName)) {
            val rules = all.getJSONArray(packageName)
            for (i in 0 until rules.length()) {
                val rule = rules.getJSONArray(i)
                list.add(
                    Pair(
                        getPathAsUserQuickly(rule.getString(0), userId),
                        getPathAsUserQuickly(rule.getString(1), userId)
                    )
                )
            }
        }
        return list
    }

    @Server
    @Synchronized
    fun invalidateSrCache() {
        storageRedirectCache = null
    }

    @App
    @Synchronized
    fun beginBatchOperation() {
        inBatch = true
    }

    @App
    @Synchronized
    fun endBatchOperation() {
        inBatch = false
        writeStorageRedirect(storageRedirectCache!!)
    }

    @Synchronized
    private fun writeStorageRedirect(json: JSONObject) {
        storageRedirectCache = json
        if (inBatch) {
            return
        }
        try {
            storageRedirectFile.createNewFile()
            val bb = ByteBuffer.wrap(json.toString().toByteArray())
            storageRedirectFile.outputStream().use { it.channel.write(bb) }
            notifyListeners()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @App
    @Server
    fun readRawStorageRedirect(): String = storageRedirectFile.readText()

    @Synchronized
    private fun readStorageRedirect(): JSONObject {
        if (storageRedirectCache == null) {
            storageRedirectCache = try {
                JSONObject(readRawStorageRedirect())
            } catch (e: Exception) {
                if (e !is FileNotFoundException) {
                    e.printStackTrace()
                }
                JSONObject()
            }
        }
        return storageRedirectCache!!
    }

    private fun getPathAsUserQuickly(path: String, userId: Int): String = if (userId == 0) {
        path
    } else {
        FileUtils.getPathAsUser(path, userId)
    }

    // READ ONLY
    @App
    fun putReadOnly(rawRules: List<String>, packageNames: List<String>) {
        if (rawRules.isEmpty()) {
            removeReadOnly(packageNames)
            return
        }
        val rules = JSONArray(rawRules)
        val all = readReadOnly()
        packageNames.forEach { all.put(it, rules) }
        writeReadOnly(all)
    }

    @App
    fun removeReadOnly(packageNames: List<String>) {
        val all = readReadOnly()
        packageNames.forEach { all.remove(it) }
        writeReadOnly(all)
    }

    @App
    fun getUninstalledReadOnlyPackages(installedPackages: Set<String>): List<String> {
        val packages = readReadOnly().keys().asSequence()
        return (packages - installedPackages).toList()
    }

    @App
    fun getPackageReadOnly(packageName: String, userId: Int = 0): List<String> {
        val all = readReadOnly()
        if (!all.has(packageName)) {
            return emptyList()
        }
        return all.getJSONArray(packageName).toList().map { path ->
            getPathAsUserQuickly(path, userId)
        }
    }

    @Server
    fun getAllReadOnly(): Map<String, List<String>> {
        val ret = mutableMapOf<String, List<String>>()
        val all = readReadOnly()
        all.keys().forEach { ret[it] = all.getJSONArray(it).toList() }
        return ret
    }

    @Server
    @Synchronized
    fun invalidateReadOnlyCache() {
        readOnlyCache = null
    }

    @Synchronized
    private fun writeReadOnly(json: JSONObject) {
        readOnlyCache = json
        try {
            readOnlyFile.createNewFile()
            val bb = ByteBuffer.wrap(json.toString().toByteArray())
            readOnlyFile.outputStream().use { it.channel.write(bb) }
            notifyListeners()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @App
    @Server
    fun readRawReadOnly(): String {
        readOnlyFile.inputStream().use {
            val bb = ByteBuffer.allocate(it.available())
            it.channel.read(bb)
            return String(bb.array())
        }
    }

    @Synchronized
    private fun readReadOnly(): JSONObject {
        if (readOnlyCache == null) {
            readOnlyCache = try {
                JSONObject(readRawReadOnly())
            } catch (e: Exception) {
                if (e !is FileNotFoundException) {
                    e.printStackTrace()
                }
                JSONObject()
            }
        }
        return readOnlyCache!!
    }

    // FILE SYSTEM RECORD
    @Server
    var denylist: List<String>
        @Synchronized
        get() = try {
            if (denylistCache == null) {
                denylistFile.inputStream().use { input ->
                    val bb = ByteBuffer.allocate(input.available())
                    input.channel.read(bb)
                    denylistCache = String(bb.array())
                        .split('\n')
                        .filterNot { it.isBlank() }
                }
            }
            denylistCache!!
        } catch (e: IOException) {
            if (e !is FileNotFoundException) {
                e.printStackTrace()
            }
            emptyList()
        }
        @Synchronized
        set(value) {
            try {
                denylistCache = value
                denylistFile.createNewFile()
                val bb = ByteBuffer.wrap(value.joinToString("\n").toByteArray())
                denylistFile.outputStream().use { it.channel.write(bb) }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

    // EXTRA
    @Server
    val enableRelatime: Boolean
        get() = preferences.getBoolean(res.getString(R.string.enable_relatime_key), false)

    @App
    @Server
    val aggressivelyPromptForReadingMediaFiles: Boolean
        get() = preferences.getBoolean(
            res.getString(R.string.aggressively_prompt_for_reading_media_files_key), true
        )

    @App
    @Server
    val autoLogging: Boolean
        get() = preferences.getBoolean(res.getString(R.string.auto_logging_key), true)

    @App
    @Server
    val recordSharedStorage: Boolean
        get() = preferences.getBoolean(res.getString(R.string.record_shared_storage_key), false)

    @App
    @Server
    val recordExternalAppSpecificStorage: Boolean
        get() = recordSharedStorage && preferences.getBoolean(
            res.getString(R.string.record_external_app_specific_storage_key), false
        )

    @App
    @Server
    val upsert: Boolean
        get() = preferences.getBoolean(res.getString(R.string.upsert_key), true)
}
