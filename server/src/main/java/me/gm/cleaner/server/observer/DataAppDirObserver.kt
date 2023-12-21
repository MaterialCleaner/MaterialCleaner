package me.gm.cleaner.server.observer

import android.content.Intent
import android.os.Build
import android.os.FileObserver
import androidx.core.net.toUri
import api.SystemService
import java.io.File

class DataAppDirObserver : BaseIntentObserver(), ZygiskObserver {
    private val appDir = "/data/app"
    private val dirToPackageName = SystemService.getInstalledPackagesFromAllUsersNoThrow(0)
        .associate {
            it.applicationInfo.sourceDir
                .substring(appDir.length + 1)
                .substringBefore(File.separator) to it.packageName
        }
        .toMutableMap()
    private val fileObserver = object : FileObserver(appDir, CREATE or DELETE or MOVED_FROM) {
        private fun tryParsePackageName(path: String): String {
            val codePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                File(appDir, path).list()!![0]
            } else {
                path
            }
            return codePath.substringBefore(RANDOM_CODEPATH_PREFIX)
        }

        override fun onEvent(event: Int, path: String?) {
            if (path?.endsWith(RANDOM_DIR_SUFFIX) != true) {
                // ensure not tmp dir
                return
            }
            when (event and ALL_EVENTS) {
                CREATE -> {
                    Thread.sleep(1000)
                    val packageName = tryParsePackageName(path)
                    if (!dirToPackageName.values.contains(packageName)) {
                        mockBroadcastIntent(
                            Intent(Intent.ACTION_PACKAGE_ADDED).setData(packageName.toUri())
                        )
                    }
                    dirToPackageName[path] = packageName
                }

                DELETE, MOVED_FROM -> {
                    val packageName = dirToPackageName.remove(path)!!
                    if (dirToPackageName.values.contains(packageName)) {
                        mockBroadcastIntent(
                            Intent(Intent.ACTION_PACKAGE_REPLACED).setData(packageName.toUri())
                        )
                    } else {
                        mockBroadcastIntent(
                            Intent(Intent.ACTION_PACKAGE_FULLY_REMOVED).setData(packageName.toUri())
                        )
                    }
                }
            }
        }
    }

    init {
        fileObserver.startWatching()
    }

    override fun onDestroy() {
        super.onDestroy()
        fileObserver.stopWatching()
    }

    companion object {
        private const val RANDOM_DIR_PREFIX = "~~"
        private const val RANDOM_CODEPATH_PREFIX = '-'
        private const val RANDOM_DIR_SUFFIX = "=="
        private const val PACKAGE_SCHEME = "package"
    }
}
