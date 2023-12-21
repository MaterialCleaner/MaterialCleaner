package me.gm.cleaner.server.observer

import android.app.ActivityManager
import androidx.annotation.CallSuper
import api.SystemService
import me.gm.cleaner.dao.ServicePreferences
import me.gm.cleaner.util.FileUtils.toUserId
import java.util.concurrent.CopyOnWriteArraySet

abstract class BaseProcessObserver : BaseObserver() {
    protected val mounter: Mounter = Mounter()

    val mountedEmulatedStorage: CopyOnWriteArraySet<Int> = CopyOnWriteArraySet()

    protected fun isMounterActiveForUser(userId: Int): Boolean =
        mountedEmulatedStorage.contains(userId)

    protected fun isMounterActiveForUid(uid: Int): Boolean = isMounterActiveForUser(uid.toUserId())

    fun waitMount(packageName: String, pid: Int, uid: Int): Boolean {
        if (!isMounterActiveForUid(uid)) {
            return true
        }
        if (ServicePreferences.getPackageSrCount(packageName) > 0) {
            return mounter.bindMount(packageName, pid, uid)
        }
        return true
    }

    private fun getRunningAppProcesses(packageNames: Array<String>): List<ActivityManager.RunningAppProcessInfo> =
        SystemService.getRunningAppProcessesNoThrow().filter { procInfo ->
            isMounterActiveForUid(procInfo.uid) && procInfo.pkgList.any { packageNames.contains(it) }
        }

    fun forceStopPackages(packageNames: Array<String>) {
        getRunningAppProcesses(packageNames).forEach { procInfo ->
            procInfo.pkgList.forEach { packageName ->
                SystemService.forceStopPackageNoThrow(packageName, procInfo.uid.toUserId())
            }
        }
    }

    fun remountForPackages(packageNames: Array<String>) {
        mounter.forProcList(getRunningAppProcesses(packageNames), false, true)
    }

    private fun getRunningAppProcesses(packageNames: Iterable<String>): List<ActivityManager.RunningAppProcessInfo> =
        SystemService.getRunningAppProcessesNoThrow().filter { procInfo ->
            isMounterActiveForUid(procInfo.uid) && procInfo.pkgList.any { packageNames.contains(it) }
        }

    fun remountAll() {
        mounter.forProcListAsync(getRunningAppProcesses(ServicePreferences.srPackages), false, true)
    }

    fun remountAllWithCheck() {
        mounter.forProcListAsync(getRunningAppProcesses(ServicePreferences.srPackages), true, true)
    }

    fun recordAll() {
        mounter.forProcListAsync(getRunningAppProcesses(ServicePreferences.srPackages), true, false)
    }

    fun isFuseBpfEnabled(): Boolean = mounter.isFuseBpfEnabled

    fun getStartUpAwarePids(packageName: String): Set<Int> = mounter.getRecordedPids(packageName)

    fun getAllStartUpAwarePids(): Set<Int> = mounter.getAllRecordedPids()

    fun getMountFailedPids(): Set<Int> = mounter.getMountFailedPids()

    fun getMountedPackages(): Set<String> = mounter.getMountedPackages()

    fun getMountedDirs(): List<String> = mounter.getMountedDirs()

    @CallSuper
    override fun onDestroy() {
        super.onDestroy()
        mounter.onDestroy()
    }
}
