package me.gm.cleaner.dao

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import me.gm.cleaner.dao.SecurityHelper.read
import me.gm.cleaner.dao.SecurityHelper.write
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.Locale

object AppLabelCache {
    private lateinit var pm: PackageManager
    private lateinit var labelDir: File
    private var lastLocale: Locale = Locale.getDefault()
    private var labelCache: JSONObject? = null

    fun init(context: Context) {
        pm = context.packageManager
        labelDir = context.filesDir.resolve("label")
    }

    fun getPackageLabelFromPackageManager(applicationInfo: ApplicationInfo): String =
        pm.getApplicationLabel(applicationInfo).toString()

    @Synchronized
    fun getPackageLabelFromCache(packageInfo: PackageInfo, updateCacheIfStale: Boolean): String? {
        val labelCache = readLabel()
        val updateTimeToLabel = labelCache.optJSONArray(packageInfo.packageName) ?: JSONArray()
        return when {
            updateTimeToLabel.length() == 2 &&
                    packageInfo.lastUpdateTime == updateTimeToLabel.getLong(0) ->
                updateTimeToLabel.getString(1)

            !updateCacheIfStale -> null
            else -> {
                val label = getPackageLabelFromPackageManager(packageInfo.applicationInfo)
                updateTimeToLabel.put(0, packageInfo.lastUpdateTime)
                updateTimeToLabel.put(1, label)
                labelCache.put(packageInfo.packageName, updateTimeToLabel)
                writeLabel(labelCache)
                label
            }
        }
    }

    fun getPackageLabel(packageInfo: PackageInfo): String =
        getPackageLabelFromCache(packageInfo, true)!!

    @Synchronized
    fun updatePackageLabelCacheInBulk(
        installedPackages: List<PackageInfo>, removeUninstalled: Boolean
    ) {
        val labelCache = readLabel()
        var changed = false
        if (removeUninstalled) {
            val uninstalled = labelCache.keys().asSequence().toSet() -
                    installedPackages.asSequence().map { it.packageName }.toSet()
            uninstalled.forEach { packageName ->
                changed = true
                labelCache.remove(packageName)
            }
        }
        installedPackages.forEach { packageInfo ->
            if (getPackageLabelFromCache(packageInfo, false) == null) {
                changed = true

                val updateTimeToLabel = JSONArray()
                updateTimeToLabel.put(0, packageInfo.lastUpdateTime)
                updateTimeToLabel.put(
                    1, getPackageLabelFromPackageManager(packageInfo.applicationInfo)
                )
                labelCache.put(packageInfo.packageName, updateTimeToLabel)
            }
        }
        if (changed) {
            writeLabel(labelCache)
        }
    }

    private fun getLabelFile(): File = labelDir.resolve(Locale.getDefault().toLanguageTag())

    @Synchronized
    private fun writeLabel(json: JSONObject) {
        try {
            labelCache = json
            labelDir.mkdirs()
            val labelFile = getLabelFile()
            if (labelFile.exists()) {
                labelFile.delete()
            }
            val encryptedFile = SecurityHelper.encryptedFile(labelFile)
            encryptedFile.write(json.toString().toByteArray())
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @Synchronized
    private fun readLabel(): JSONObject {
        if (lastLocale !== Locale.getDefault()) {
            lastLocale = Locale.getDefault()
            labelCache = null
        }
        if (labelCache == null) {
            labelCache = try {
                val encryptedFile = SecurityHelper.encryptedFile(getLabelFile())
                JSONObject(encryptedFile.read().toString())
            } catch (e: IOException) {
                JSONObject()
            } catch (e: RuntimeException) {
                JSONObject()
            }
        }
        return labelCache!!
    }
}
