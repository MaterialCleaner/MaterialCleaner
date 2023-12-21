package me.gm.cleaner.server.observer

import android.app.ActivityManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.ArrayMap
import android.util.ArraySet
import androidx.core.os.postDelayed
import api.SystemService
import com.google.common.collect.Multimaps
import com.google.common.collect.SetMultimap
import me.gm.cleaner.client.CleanerHooksClient
import me.gm.cleaner.dao.MountRules
import me.gm.cleaner.dao.PurchaseVerification.isLoosePro
import me.gm.cleaner.dao.ServicePreferences
import me.gm.cleaner.dao.ServicePreferences.getPackageSr
import me.gm.cleaner.dao.ServicePreferences.getPackageSrZipped
import me.gm.cleaner.util.FileUtils
import me.gm.cleaner.util.FileUtils.toUserId
import me.gm.cleaner.util.SystemPropertiesUtils
import java.io.File
import java.io.IOException
import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlin.math.min

class Mounter {
    private val thread: HandlerThread = HandlerThread("").apply { start() }
    private val handler: Handler = Handler(thread.looper)
    private val lock: Any = Object()

    private val pidRecords: SetMultimap<String, Int> =
        Multimaps.newSetMultimap(ArrayMap()) { ArraySet() }
    private val mountFailedPids: MutableList<Int> = mutableListOf()
    private val mkdirRecords: SetMultimap<String, String> =
        Multimaps.newSetMultimap(ArrayMap()) { ArraySet() }

    private val rmdirPackages: MutableSet<String> = mutableSetOf()
    private var rmdirQueueSize: Int = 0

    fun mountForAllPackages(): Boolean = ServicePreferences.enableRelatime ||
            !isFuseBpfEnabled && ServicePreferences.recordExternalAppSpecificStorage

    fun bindMountAsync(packageName: String, pid: Int, uid: Int) {
        handler.post {
            bindMountLocked(packageName, pid, uid)
        }
    }

    fun bindMount(packageName: String, pid: Int, uid: Int): Boolean = synchronized(lock) {
        bindMountLocked(packageName, pid, uid)
    }

    internal val isFuseBpfEnabled: Boolean by lazy {
        var is_enabled = SystemPropertiesUtils.getBoolean("ro.fuse.bpf.is_running")
        if (is_enabled != null) return@lazy is_enabled
        is_enabled = SystemPropertiesUtils.getBoolean("persist.sys.fuse.bpf.override")
        if (is_enabled != null) return@lazy is_enabled
        is_enabled = SystemPropertiesUtils.getBoolean("ro.fuse.bpf.enabled")
        if (is_enabled != null) return@lazy is_enabled

        try {
            // If the kernel has fuse-bpf, /sys/fs/fuse/features/fuse_bpf will exist and have the contents
            // 'supported\n' - see fs/fuse/inode.c in the kernel source
            val filename = "/sys/fs/fuse/features/fuse_bpf"
            Path(filename).readText() == "supported\n"
        } catch (e: IOException) {
            false
        }
    }

    private val unsupportedForFreeUsers: Boolean by lazy {
        Build.VERSION.SDK_INT >= 35 && !isLoosePro
    }

    private fun getMkdirList(rules: MountRules): List<String> = rules.mountPoint + rules.sources

    private fun bindMountLocked(packageName: String, pid: Int, uid: Int): Boolean {
        val enableRelatime = ServicePreferences.enableRelatime &&
                !ServicePreferences.denylist.contains(packageName)
        val recordExternalAppSpecificStorage =
            ServicePreferences.recordExternalAppSpecificStorage &&
                    !ServicePreferences.denylist.contains(packageName) &&
                    CleanerHooksClient.pingBinder()

        if (ServicePreferences.getPackageSrCount(packageName) == 0) {
            return FileUtils.bind_mount(
                pid, uid, enableRelatime,
                !isFuseBpfEnabled && recordExternalAppSpecificStorage, false,
                emptyArray(), emptyArray()
            )
        }

        if (unsupportedForFreeUsers) {
            return true
        }

        pidRecords.put(packageName, pid)
        val userId = uid.toUserId()
        val rules: MountRules
        if (!mkdirRecords.containsKey(packageName)) {
            rules = MountRules(getPackageSrZipped(packageName, userId))
            val mkdirRecord = mutableSetOf<String>()
            getMkdirList(rules).forEach { record(mkdirRecord, it) }
            if (FileUtils.auto_prepare_dirs(mkdirRecord.toTypedArray(), uid)) {
                mkdirRecords.putAll(packageName, mkdirRecord)
            }
        } else {
            rules = MountRules(getPackageSr(packageName, userId))
        }
        val ret = FileUtils.bind_mount(
            pid, uid, enableRelatime,
            !isFuseBpfEnabled && recordExternalAppSpecificStorage, isFuseBpfEnabled,
            rules.sources.toTypedArray(), rules.targets.toTypedArray()
        )
        if (ret) {
            mountFailedPids.remove(pid)
        } else {
            mountFailedPids.add(pid)
        }
        return ret
    }

    private fun record(mkdirRecord: MutableSet<String>, dir: String) {
        if (FileUtils.childOf(FileUtils.externalStorageDirParent, dir)) {
            mkdirRecord.add(dir)
            val parent = File(dir).parent ?: return
            record(mkdirRecord, parent)
        }
    }

    fun forProcListAsync(
        procList: List<ActivityManager.RunningAppProcessInfo>,
        checkMountState: Boolean,
        remount: Boolean
    ) {
        handler.post {
            forProcList(procList, checkMountState, remount)
        }
    }

    fun forProcList(
        procList: List<ActivityManager.RunningAppProcessInfo>,
        checkMountState: Boolean,
        remount: Boolean
    ) {
        synchronized(lock) {
            if (checkMountState) {
                val remountProcList = checkAndRecordLocked(procList)
                if (remount) {
                    remountLocked(remountProcList)
                }
            } else if (remount) {
                remountLocked(procList)
            }
        }
    }

    private fun checkAndRecordLocked(procList: List<ActivityManager.RunningAppProcessInfo>)
            : List<ActivityManager.RunningAppProcessInfo> {
        val remountPackages = mutableSetOf<String>()
        for (procInfo in procList) {
            procInfo.pkgList.forEach { packageName ->
                val rules = MountRules(getPackageSr(packageName, procInfo.uid.toUserId()))
                val targets = rules.targets
                val mountedIndices =
                    FileUtils.check_mounts(procInfo.pid, targets.toTypedArray())
                if (mountedIndices == null) {
                    // unknown mount status
                } else if (targets.size == mountedIndices.size && mountedIndices.all { it != -1 }) {
                    // record pid
                    pidRecords.put(packageName, procInfo.pid)
                    if (!mkdirRecords.containsKey(packageName)) {
                        mkdirRecords.putAll(packageName, getMkdirList(rules))
                    }
                } else {
                    remountPackages += packageName
                }
            }
        }
        val remountProcList = procList.filter { procInfo ->
            procInfo.pkgList.any { remountPackages.contains(it) }
        }
        return remountProcList
    }

    private fun remountLocked(procList: List<ActivityManager.RunningAppProcessInfo>) {
        for (procInfo in procList) {
            procInfo.pkgList.forEach { packageName ->
                removeMountDirsLocked(packageName, procInfo.uid)
            }
        }
        for (procInfo in procList) {
            procInfo.pkgList.forEach { packageName ->
                bindMountLocked(packageName, procInfo.pid, procInfo.uid)
            }
        }
    }

    fun notifyProcessKilled(packageName: String, pid: Int, uid: Int) {
        handler.post {
            synchronized(lock) {
                notifyProcessKilledLocked(packageName, pid, uid)
            }
        }
    }

    private fun notifyProcessKilledLocked(packageName: String, pid: Int, uid: Int) {
        pidRecords.remove(packageName, pid)
        mountFailedPids.remove(pid)
        if (pidRecords.containsKey(packageName)) {
            rmdirPackages.add(packageName)
            ++rmdirQueueSize
            handler.postDelayed(
                min(VALIDATE_PID_DELAY_PER_PACKAGE * rmdirPackages.size, VALIDATE_PID_DELAY_MAX)
            ) {
                synchronized(lock) {
                    if (--rmdirQueueSize == 0 && rmdirPackages.isNotEmpty()) {
                        validatePidRecordsLocked()
                    }
                }
            }
        } else {
            rmdirPackages.remove(packageName)
            removeMountDirsLocked(packageName, uid)
        }
    }

    private fun validatePidRecordsLocked() {
        rmdirPackages.clear()
        // /proc
        val proc = CharArray(0x5)

        proc[0x0] = '-'
        proc[0x1] = 'p'
        proc[0x2] = 's'
        proc[0x3] = 'm'
        proc[0x4] = 'c'

        for (i in 0..0x4) {
            proc[i] = (proc[i].code xor (i + 0x5) % 3).toChar()
        }
        pidRecords.keySet().toList().forEach { packageName ->
            val validValue = pidRecords[packageName].filter { pid ->
                File(String(proc), pid.toString()).exists()
            }
            val oldValues = pidRecords.replaceValues(packageName, validValue)
            mountFailedPids.removeAll(oldValues - validValue.toSet())
            if (validValue.isEmpty()) {
                val uid = SystemService.getApplicationInfo(packageName, 0, 0)?.uid ?: -1
                removeMountDirsLocked(packageName, uid)
            }
        }
    }

    private fun removeMountDirsLocked(packageName: String, uid: Int) {
        if (!mkdirRecords.containsKey(packageName)) {
            return
        }
        val others = mkdirRecords.asMap()
            .filterKeys { it != packageName }
            .values
            .flatten()
            .toSet()
        val dirs = mkdirRecords[packageName]
            .filter { !others.contains(it) }
            .sortedByDescending { it.length }
        mkdirRecords.removeAll(packageName)
        dirs.forEach { dir ->
            FileUtils.rm_dir(dir)
        }
    }

    fun getRecordedPids(packageName: String): Set<Int> = synchronized(lock) {
        pidRecords[packageName].toSet()
    }

    fun getAllRecordedPids(): Set<Int> = synchronized(lock) {
        pidRecords.values().toSet()
    }

    fun getMountFailedPids(): Set<Int> = synchronized(lock) {
        mountFailedPids.toSet()
    }

    fun getMountedPackages(): Set<String> = synchronized(lock) {
        mkdirRecords.keySet().toSet()
    }

    fun getMountedDirs(): List<String> = synchronized(lock) {
        mkdirRecords.values().toList()
    }

    fun onDestroy() {
        thread.quit()
    }

    companion object {
        private const val VALIDATE_PID_DELAY_PER_PACKAGE: Long = 500L
        private const val VALIDATE_PID_DELAY_MAX: Long = 3000L
    }
}
