package me.gm.cleaner.util

import android.annotation.SuppressLint
import android.os.Environment
import android.util.Log
import androidx.core.text.isDigitsOnly
import me.gm.cleaner.AndroidFilesystemConfig.AID_USER_OFFSET
import java.io.File
import java.io.IOException
import java.nio.file.LinkOption
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.regex.Pattern
import kotlin.io.path.CopyActionContext
import kotlin.io.path.CopyActionResult
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.OnErrorResult
import kotlin.io.path.copyToRecursively
import kotlin.io.path.createParentDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.moveTo
import kotlin.io.path.pathString

object FileUtils {
    @Volatile
    private var externalStorageDirInternal: File? = null

    // /storage/emulated/{userId}
    val externalStorageDir: File
        @SuppressLint("SdCardPath")
        get() = externalStorageDirInternal ?: try {
            Environment.getExternalStorageDirectory()
        } catch (e: SecurityException) {
            // workaround Android 13 throws new SecurityException("callingPackage does not match UID");
            File("/sdcard").canonicalFile
        }

    fun setExternalStorageDir(dir: File) {
        externalStorageDirInternal = dir
    }

    // /storage/emulated
    val externalStorageDirParent: String
        get() = externalStorageDir.parent!!

    // /storage/emulated/{userId}/Android
    val androidDir: File
        get() = externalStorageDir.resolve("Android")

    // /storage/emulated/{userId}/Android/data
    val androidDataDir: File
        get() = androidDir.resolve("data")

    // /storage/emulated/{userId}/Android/media
    val androidMediaDir: File
        get() = androidDir.resolve("media")

    // /storage/emulated/{userId}/Android/obb
    val androidObbDir: File
        get() = androidDir.resolve("obb")

    // /storage/emulated/{userId}/Android/sandbox
    val androidSandboxDir: File
        get() = androidDir.resolve("sandbox")

    val defaultExternalNoScan: Array<File>
        get() = arrayOf(
            androidDataDir,
            androidMediaDir,
            androidObbDir,
            androidSandboxDir
        )

    inline fun buildExternalStorageAppDataDirs(packageName: String): File =
        androidDataDir.resolve(packageName)

    /** Matches known application dir paths. The first group contains the generic part of the path,
     * the second group contains the user id (or null if it's a public volume without users), the
     * third group contains the package name, and the fourth group the remainder of the path.
     */
    val KNOWN_APP_DIR_PATHS: Pattern by lazy {
        Pattern.compile("(?i)(^/storage/[^/]+/(?:([0-9]+)/)?Android/(?:data|obb)/)([^/]+)(/.*)?")
    }

    inline fun isKnownAppDirPaths(path: String, packageName: String): Boolean {
        val m = KNOWN_APP_DIR_PATHS.matcher(path)
        return m.matches() && m.group(3) == packageName
    }

    val APP_DATA_DIR_PATHS: Pattern by lazy {
        Pattern.compile("(?i)(^/[^/]+/[^/]+/)([0-9]+)(/)?([^/]+)?(/.*)?")
    }

    inline fun extractUserIdFromPath(path: String, fallbackUserId: Int = 0): Int {
        val m = APP_DATA_DIR_PATHS.matcher(path)
        if (m.matches()) {
            return m.group(2)!!.toInt()
        }
        return fallbackUserId
    }

    inline fun extractPathOwnerPackageName(path: String): String? {
        if (path.startsWith("/storage/", true)) {
            val m = KNOWN_APP_DIR_PATHS.matcher(path)
            if (m.matches()) {
                return m.group(3)
            }
        } else {
            val m = APP_DATA_DIR_PATHS.matcher(path)
            if (m.matches()) {
                return m.group(4)
            }
        }
        return null
    }

    inline fun getPathAsUser(path: String, userId: Int): String {
        val m = APP_DATA_DIR_PATHS.matcher(path)
        if (!m.matches()) {
            return path
        }
        val sb = StringBuilder()
        for (i in 1..m.groupCount()) {
            val group = m.group(i)
            when {
                group == null -> {
                    continue
                }

                group.isDigitsOnly() -> {
                    sb.append(userId)
                }

                else -> {
                    sb.append(group)
                }
            }
        }
        return sb.toString()
    }

    inline fun childOf(parent: File, child: File): Boolean = childOf(parent.path, child.path)
    inline fun childOf(parent: File, child: String): Boolean = childOf(parent.path, child)
    inline fun childOf(parent: String, child: File): Boolean = childOf(parent, child.path)
    inline fun childOf(parent: String, child: String): Boolean =
        parent.endsWith(File.separator, true) && child.startsWith(parent, true) ||
                !parent.endsWith(File.separator, true) &&
                child.startsWith(parent + File.separator, true)

    inline fun startsWith(parent: Path, child: String): Boolean =
        startsWith(parent.pathString, child)

    inline fun startsWith(parent: File, child: File): Boolean = startsWith(parent.path, child.path)
    inline fun startsWith(parent: File, child: String): Boolean = startsWith(parent.path, child)
    inline fun startsWith(parent: String, child: File): Boolean = startsWith(parent, child.path)

    inline fun startsWith(parent: String, child: String): Boolean =
        child.equals(parent, true) || parent.equals(File.separator, true) ||
                child.startsWith(parent + File.separator, true)

    @Throws(IOException::class)
    inline fun fileTreeSize(path: Path): Long = 0

    @OptIn(ExperimentalPathApi::class)
    inline fun move(source: Path, target: Path): Boolean {
        try {
            target.createParentDirectories()
        } catch (e: IOException) {
            return false
        }
        try {
            source.moveTo(
                target,
                LinkOption.NOFOLLOW_LINKS,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (e: NoSuchFileException) {
            return false
        } catch (e: IOException) {
            if (!copy(source, target)) {
                return false
            }
            source.deleteRecursively()
        }
        return true
    }

    @OptIn(ExperimentalPathApi::class)
    @JvmOverloads
    inline fun copy(
        source: Path, target: Path,
        noinline copyAction: CopyActionContext.(source: Path, target: Path) -> CopyActionResult = { src, dst ->
            src.copyToIgnoringExistingDirectory(dst, false)
        }
    ): Boolean {
        try {
            target.createParentDirectories()
        } catch (e: IOException) {
            return false
        }
        var errorOccurred = false
        try {
            source.copyToRecursively(
                target,
                { source, target, exception ->
                    Log.e(javaClass.name, "$source -> $target\n${exception.stackTraceToString()}")
                    errorOccurred = true
                    OnErrorResult.SKIP_SUBTREE
                },
                false,
                copyAction
            )
        } catch (e: NoSuchFileException) {
            errorOccurred = true
        }
        return !errorOccurred
    }

    @delegate:SuppressLint("SoonBlockedPrivateApi")
    val standardDirs: Array<String> by lazy {
        Environment::class.java.getDeclaredField("STANDARD_DIRECTORIES")
            .apply { isAccessible = true }[null] as Array<String>
    }

    fun isStandardDirectory(dir: String): Boolean {
        for (valid in standardDirs) {
            if (valid.equals(dir, true)) {
                return true
            }
        }
        return false
    }

    inline fun Int.toUserId(): Int = this / AID_USER_OFFSET
    inline fun Int.toAppId(): Int = this % AID_USER_OFFSET

    private external fun b(dir: String): Int
    fun rm_dir(dir: String): Int = b(dir)

    private external fun a(dirs: Array<String>, uid: Int): Boolean
    fun auto_prepare_dirs(dirs: Array<String>, uid: Int): Boolean = a(dirs, uid)

    private external fun a(dir: String, uid: Int, isPrivate: Boolean)
    fun switch_owner(dir: String, uid: Int, isPrivate: Boolean) {
        a(dir, uid, isPrivate)
    }

    /**
     * Process observers using this function should be careful since the cmdline changes when proc init.
     */
    private external fun a(pid: Int): String
    fun read_cmdline(pid: Int): String = a(pid)

    /**
     * Process observers using this function should be careful since the cgroup changes when proc init.
     */
    private external fun b(pid: Int): Int
    fun read_uid(pid: Int): Int = b(pid)

    private external fun a(pid: Int, targets: Array<String>): IntArray?
    fun check_mounts(pid: Int, targets: Array<String>): IntArray? = a(pid, targets)

    private external fun a(
        pid: Int, uid: Int, enableRelatime: Boolean, unmountDataRestriction: Boolean,
        fuseBypass: Boolean, sources: Array<String>, targets: Array<String>
    ): Boolean

    fun bind_mount(
        pid: Int, uid: Int, enableRelatime: Boolean, unmountDataRestriction: Boolean,
        fuseBypass: Boolean, sources: Array<String>, targets: Array<String>
    ): Boolean = a(pid, uid, enableRelatime, unmountDataRestriction, fuseBypass, sources, targets)
}
