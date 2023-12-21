package me.gm.cleaner.net

import android.content.Context
import android.content.pm.PackageInfo
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.gm.cleaner.R
import me.gm.cleaner.client.ui.storageredirect.AppTypeMarks
import me.gm.cleaner.dao.ServiceMoreOptionsPreferences
import org.yaml.snakeyaml.Yaml

object OnlineAppTypeMarks {

    private fun buildPath(packageName: String): String =
        ServiceMoreOptionsPreferences.appsTypeMarksRepo + packageName + ".yml"

    fun buildURL(context: Context, packageName: String): SimpleCachedURL =
        SimpleCachedURL(context, buildPath(packageName))

    private fun buildDefaultPath(context: Context, packageName: String): String =
        context.getString(R.string.apps_type_marks_default) + packageName + ".yml"

    fun buildDefaultURL(context: Context, packageName: String): SimpleCachedURL =
        SimpleCachedURL(context, buildDefaultPath(context, packageName))

    suspend fun fetch(context: Context, pi: PackageInfo): Result<AppTypeMarks?> = runCatching {
        withContext(Dispatchers.IO) {
            buildURL(context, pi.packageName).openStream().use {
                val yaml = Yaml().load<Map<String, Any>>(it)
                if (yaml == null) {
                    // does not exist in the online repo
                    null
                } else {
                    AppTypeMarks(yaml, PackageInfoCompat.getLongVersionCode(pi))
                }
            }
        }
    }
}
