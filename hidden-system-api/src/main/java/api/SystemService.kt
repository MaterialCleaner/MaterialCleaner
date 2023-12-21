package api

import android.app.*
import android.content.IContentProvider
import android.content.IIntentReceiver
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.*
import android.os.*
import android.os.storage.IStorageEventListener
import android.os.storage.IStorageManager
import android.os.storage.VolumeInfo
import android.permission.IPermissionManager
import android.system.Os
import androidx.annotation.RequiresApi
import api.util.BuildUtils
import api.util.Logger.LOGGER
import com.android.internal.app.IAppOpsService

object SystemService {

    /*
     * Cannot replace these hardcoded service names to Context.class's **_SERVICE fields!
     * "server" module depends on "hidden-api-common" module, which has Context class stub.
     */

    private val activityManagerBinder by lazy {
        SystemServiceBinder<IActivityManager>("activity") {
            if (BuildUtils.atLeast26()) {
                IActivityManager.Stub.asInterface(it)
            } else {
                ActivityManagerNative.asInterface(it)
            }
        }
    }

    private val packageManagerBinder by lazy {
        SystemServiceBinder<IPackageManager>("package") {
            IPackageManager.Stub.asInterface(it)
        }
    }

    private val userManagerBinder by lazy {
        SystemServiceBinder<IUserManager>("user") {
            IUserManager.Stub.asInterface(it)
        }
    }

    private val appOpsServiceBinder by lazy {
        SystemServiceBinder<IAppOpsService>("appops") {
            IAppOpsService.Stub.asInterface(it)
        }
    }

    private val launcherAppsBinder by lazy {
        SystemServiceBinder<ILauncherApps>("launcherapps") {
            ILauncherApps.Stub.asInterface(it)
        }
    }

    @delegate:RequiresApi(30)
    private val permissionManagerBinder by lazy {
        SystemServiceBinder<IPermissionManager>("permissionmgr") {
            IPermissionManager.Stub.asInterface(it)
        }
    }

    private val storageManagerBinder by lazy {
        SystemServiceBinder<IStorageManager>("mount") {
            IStorageManager.Stub.asInterface(it)
        }
    }

    val activityManager get() = activityManagerBinder.service
    val packageManager get() = packageManagerBinder.service
    val userManager get() = userManagerBinder.service
    val appOpsService get() = appOpsServiceBinder.service
    val launcherApps get() = launcherAppsBinder.service
    val permissionManager get() = permissionManagerBinder.service
    val storageManager get() = storageManagerBinder.service

    @JvmStatic
    @Throws(RemoteException::class)
    fun broadcastIntentWithFeature(
        callingFeatureId: String?,
        intent: Intent?, resolvedType: String?, resultTo: IIntentReceiver?, resultCode: Int,
        resultData: String?, map: Bundle?, requiredPermissions: Array<String?>?,
        appOp: Int, options: Bundle?, serialized: Boolean, sticky: Boolean, userId: Int
    ): Int {
        val am = activityManager ?: throw RemoteException("can't get IActivityManager")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                return am.broadcastIntentWithFeature(
                    thread, callingFeatureId, intent, resolvedType, resultTo, resultCode,
                    resultData, null, requiredPermissions, null, null, appOp, null, serialized,
                    sticky, userId
                )
            } catch (ignored: NoSuchMethodError) {
            }
            am.broadcastIntentWithFeature(
                thread, callingFeatureId, intent, resolvedType, resultTo, resultCode, resultData,
                null, requiredPermissions, null, appOp, null, serialized, sticky, userId
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            am.broadcastIntentWithFeature(
                thread, callingFeatureId, intent, resolvedType, resultTo, resultCode, resultData,
                map, requiredPermissions, appOp, options, serialized, sticky, userId
            )
        } else {
            am.broadcastIntent(
                thread, intent, resolvedType, resultTo, resultCode, resultData, map,
                requiredPermissions, appOp, options, serialized, sticky, userId
            )
        }
    }

    @JvmStatic
    @Throws(RemoteException::class)
    fun checkPermission(permission: String?, pid: Int, uid: Int): Int {
        val am = activityManager ?: throw RemoteException("can't get IActivityManager")
        return am.checkPermission(permission, pid, uid)
    }

    @JvmStatic
    @Throws(RemoteException::class)
    fun getRunningAppProcesses(): List<ActivityManager.RunningAppProcessInfo> {
        val am = activityManager ?: throw RemoteException("can't get IActivityManager")
        return am.runningAppProcesses
    }

    @JvmStatic
    fun getRunningAppProcessesNoThrow(): List<ActivityManager.RunningAppProcessInfo> {
        val am = activityManager ?: throw RemoteException("can't get IActivityManager")
        return try {
            return am.runningAppProcesses
        } catch (tr: Throwable) {
            LOGGER.w(tr, "getRunningAppProcessesNoThrow failed")
            emptyList()
        }
    }

    @JvmStatic
    @Throws(RemoteException::class)
    fun getPackageInfo(packageName: String?, flags: Int, userId: Int): PackageInfo? {
        val pm = packageManager ?: throw RemoteException("can't get IPackageManger")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(packageName, flags.toLong(), userId)
        } else {
            pm.getPackageInfo(packageName, flags, userId)
        }
    }

    @JvmStatic
    @Throws(RemoteException::class)
    fun getApplicationInfo(packageName: String?, flags: Int, userId: Int): ApplicationInfo? {
        val pm = packageManager ?: throw RemoteException("can't get IPackageManger")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getApplicationInfo(packageName, flags.toLong(), userId)
        } else {
            pm.getApplicationInfo(packageName, flags, userId)
        }
    }

    @JvmStatic
    @Throws(RemoteException::class)
    fun checkPermission(permName: String?, uid: Int): Int {
        return when {
            BuildUtils.atLeast31() -> {
                val pm = packageManager ?: throw RemoteException("can't get IPackageManger")
                pm.checkUidPermission(permName, uid)
            }

            BuildUtils.atLeast30() -> {
                val permmgr = permissionManager ?: throw RemoteException("can't get IPermission")
                permmgr.checkUidPermission(permName, uid)
            }

            else -> {
                val pm = packageManager ?: throw RemoteException("can't get IPackageManger")
                pm.checkUidPermission(permName, uid)
            }
        }
    }

    @JvmStatic
    @Throws(RemoteException::class)
    fun registerProcessObserver(processObserver: IProcessObserver?) {
        val am = activityManager ?: throw RemoteException("can't get IActivityManager")
        am.registerProcessObserver(processObserver)
    }

    @JvmStatic
    @Throws(RemoteException::class)
    fun unregisterProcessObserver(processObserver: IProcessObserver?) {
        val am = activityManager ?: throw RemoteException("can't get IActivityManager")
        am.unregisterProcessObserver(processObserver)
    }

    @JvmStatic
    @Throws(RemoteException::class)
    fun registerUidObserver(
        observer: IUidObserver?, which: Int, cutpoint: Int, callingPackage: String?
    ) {
        val am = activityManager ?: throw RemoteException("can't get IActivityManager")
        am.registerUidObserver(observer, which, cutpoint, callingPackage)
    }

    @JvmStatic
    @Throws(RemoteException::class)
    fun getPackageUid(packageName: String, flags: Int, userId: Int): Int {
        val pm = packageManager ?: throw RemoteException("can't get IPackageManger")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageUid(packageName, flags.toLong(), userId)
        } else {
            pm.getPackageUid(packageName, flags, userId)
        }
    }

    @JvmStatic
    @Throws(RemoteException::class)
    fun getPackagesForUid(uid: Int): Array<String>? {
        val pm = packageManager ?: throw RemoteException("can't get IPackageManger")
        return pm.getPackagesForUid(uid)
    }

    @JvmStatic
    @Throws(RemoteException::class)
    fun getInstalledApplications(flags: Int, userId: Int): ParceledListSlice<ApplicationInfo>? {
        val pm = packageManager ?: throw RemoteException("can't get IPackageManager")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledApplications(flags.toLong(), userId)
        } else {
            pm.getInstalledApplications(flags, userId)
        }
    }

    @JvmStatic
    @Throws(RemoteException::class)
    fun getInstalledPackages(flags: Int, userId: Int): ParceledListSlice<PackageInfo>? {
        val pm = packageManager ?: throw RemoteException("can't get IPackageManager")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(flags.toLong(), userId)
        } else {
            pm.getInstalledPackages(flags, userId)
        }
    }

    @JvmStatic
    fun resolveContentProvider(name: String, flags: Int, userId: Int): ProviderInfo? {
        val pm = packageManager ?: throw RemoteException("can't get IPackageManager")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.resolveContentProvider(name, flags.toLong(), userId)
        } else {
            pm.resolveContentProvider(name, flags, userId)
        }
    }

    @JvmStatic
    @Throws(RemoteException::class)
    fun getContentProviderExternal(
        name: String?, userId: Int, token: IBinder?, tag: String?
    ): IContentProvider? {
        val am = activityManager ?: throw RemoteException("can't get IActivityManager")
        return when {
            BuildUtils.atLeast29() ->
                am.getContentProviderExternal(name, userId, token, tag)?.provider

            BuildUtils.atLeast26() ->
                am.getContentProviderExternal(name, userId, token)?.provider

            else -> throw UnsupportedOperationException()
        }
    }

    @JvmStatic
    @Throws(RemoteException::class)
    fun removeContentProviderExternal(name: String?, token: IBinder?) {
        val am = activityManager ?: throw RemoteException("can't get IActivityManager")
        am.removeContentProviderExternal(name, token)
    }

    @JvmStatic
    @Throws(RemoteException::class)
    fun getUsers(
        excludePartial: Boolean, excludeDying: Boolean, excludePreCreated: Boolean
    ): List<UserInfo> {
        val um = userManager ?: throw RemoteException("can't get IUserManger")
        return if (BuildUtils.atLeast30()) {
            um.getUsers(excludePartial, excludeDying, excludePreCreated)
        } else {
            try {
                um.getUsers(excludeDying)
            } catch (e: NoSuchMethodError) {
                um.getUsers(excludePartial, excludeDying, excludePreCreated)
            }
        }
    }

    @JvmStatic
    @Throws(RemoteException::class)
    fun isUserUnlocked(userId: Int): Boolean {
        val um = userManager ?: throw RemoteException("can't get IUserManger")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            um.isUserUnlocked(userId)
        } else {
            true
        }
    }

    @JvmStatic
    fun getInstalledPackagesNoThrow(flags: Int, userId: Int): List<PackageInfo> {
        return try {
            getInstalledPackages(flags, userId)?.list ?: emptyList()
        } catch (tr: Throwable) {
            LOGGER.w(tr, "getInstalledPackages failed: flags=%d, user=%d", flags, userId)
            emptyList()
        }
    }

    @JvmStatic
    fun getInstalledPackagesFromAllUsersNoThrow(flags: Int): List<PackageInfo> {
        return try {
            val res = mutableListOf<PackageInfo>()
            val packageNames = mutableSetOf<String>()
            for (userId in getUserIdsNoThrow()) {
                for (pi in getInstalledPackagesNoThrow(flags, userId)) {
                    if (packageNames.add(pi.packageName)) {
                        res += pi
                    }
                }
            }
            return res
        } catch (tr: Throwable) {
            LOGGER.w(tr, "getInstalledPackagesFromAllUsers failed: flags=%d", flags)
            emptyList()
        }
    }

    @JvmStatic
    fun getInstalledApplicationsNoThrow(flags: Int, userId: Int): List<ApplicationInfo> {
        return try {
            getInstalledApplications(flags, userId)?.list ?: emptyList()
        } catch (tr: Throwable) {
            LOGGER.w(tr, "getInstalledApplications failed: flags=%d, user=%d", flags, userId)
            emptyList()
        }
    }

    @JvmStatic
    fun getPackageInfoNoThrow(packageName: String?, flags: Int, userId: Int): PackageInfo? {
        return try {
            getPackageInfo(packageName, flags, userId)
        } catch (tr: Throwable) {
            LOGGER.w(
                tr, "getPackageInfo failed: packageName=%s, flags=%d, user=%d",
                packageName, flags, userId
            )
            null
        }
    }

    @JvmStatic
    fun getApplicationInfoNoThrow(packageName: String?, flags: Int, userId: Int): ApplicationInfo? {
        return try {
            getApplicationInfo(packageName, flags, userId)
        } catch (tr: Throwable) {
            LOGGER.w(
                tr, "getApplicationInfo failed: packageName=%s, flags=%d, user=%d",
                packageName, flags, userId
            )
            null
        }
    }

    @JvmStatic
    fun getUserIdsNoThrow(): List<Int> {
        val users = mutableListOf<Int>()
        try {
            for (ui in getUsers(
                excludePartial = true, excludeDying = true, excludePreCreated = true
            )) {
                users.add(ui.id)
            }
        } catch (tr: Throwable) {
            users.clear()
            users.add(0)
        }
        return users
    }

    @JvmStatic
    fun getPackagesForUidNoThrow(uid: Int): List<String> {
        val packages = mutableListOf<String>()
        try {
            packages.addAll(getPackagesForUid(uid)?.filterNotNull().orEmpty())
        } catch (tr: Throwable) {
        }
        return packages
    }

    @JvmStatic
    fun forceStopPackageNoThrow(packageName: String?, userId: Int) {
        val am = activityManager ?: throw RemoteException("can't get IActivityManager")
        try {
            am.forceStopPackage(packageName, userId)
        } catch (e: Exception) {
        }
    }

    @JvmStatic
    @Throws(RemoteException::class)
    fun addOnAppsChangedListener(callingPackage: String?, listener: IOnAppsChangedListener?) {
        val la = launcherApps ?: throw RemoteException("can't get ILauncherApps")
        if (BuildUtils.atLeast24()) {
            la.addOnAppsChangedListener(callingPackage, listener)
        } else {
            la.addOnAppsChangedListener(listener)
        }
    }

    @JvmStatic
    @Throws(RemoteException::class)
    fun startActivity(intent: Intent?, mimeType: String?, userId: Int) {
        val am = activityManager ?: throw RemoteException("can't get IActivityManager")
        am.startActivityAsUser(
            null, if (Os.getuid() == 2000) "com.android.shell" else null, intent, mimeType,
            null, null, 0, 0, null, null, userId
        )
    }

    @JvmStatic
    fun startActivityNoThrow(intent: Intent, mimeType: String?, userId: Int) {
        try {
            startActivity(intent, mimeType, userId)
        } catch (tr: Throwable) {
            LOGGER.w(tr, "startActivity failed: ", intent.toString())
        }
    }

    private val thread: IApplicationThread? by lazy {
        ActivityThread.currentActivityThread().applicationThread
    }

    @JvmStatic
    @Throws(RemoteException::class)
    fun registerReceiver(
        callerPackage: String?, callingFeatureId: String?, receiver: IIntentReceiver?,
        filter: IntentFilter?, requiredPermission: String?, userId: Int, flags: Int
    ): Intent? {
        val am = activityManager ?: throw RemoteException("can't get IActivityManager")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            am.registerReceiverWithFeature(
                thread, callerPackage, callingFeatureId, "null", receiver, filter,
                requiredPermission, userId, flags
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            am.registerReceiverWithFeature(
                thread, callerPackage, callingFeatureId, receiver, filter, requiredPermission,
                userId, flags
            )
        } else {
            am.registerReceiver(
                thread, callerPackage, receiver, filter, requiredPermission, userId, flags
            )
        }
    }

    @JvmStatic
    @Throws(RemoteException::class)
    fun unregisterReceiver(receiver: IIntentReceiver) {
        val am = activityManager ?: throw RemoteException("can't get IActivityManager")
        am.unregisterReceiver(receiver)
    }

    @JvmStatic
    @Throws(RemoteException::class)
    fun finishReceiver(
        who: IBinder?, resultCode: Int, resultData: String?, map: Bundle?, abortBroadcast: Boolean,
        flags: Int
    ) {
        val am = activityManager ?: throw RemoteException("can't get IActivityManager")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            thread?.asBinder()?.let { thread ->
                am.finishReceiver(thread, resultCode, resultData, map, abortBroadcast, flags)
            }
        } else {
            am.finishReceiver(who, resultCode, resultData, map, abortBroadcast, flags)
        }
    }

    @JvmStatic
    @Throws(RemoteException::class)
    fun grantRuntimePermission(packageName: String?, permissionName: String?, userId: Int) {
        if (BuildUtils.atLeast30()) {
            val pm = permissionManager ?: throw RemoteException("can't get IPermissionManager")
            pm.grantRuntimePermission(packageName, permissionName, userId)
        } else {
            val pm = packageManager ?: throw RemoteException("can't get IPackageManger")
            pm.grantRuntimePermission(packageName, permissionName, userId)
        }
    }

    @JvmStatic
    @Throws(RemoteException::class)
    fun revokeRuntimePermission(packageName: String?, permissionName: String?, userId: Int) {
        if (BuildUtils.atLeast30()) {
            val pm = permissionManager ?: throw RemoteException("can't get IPermissionManager")
            try {
                pm.revokeRuntimePermission(packageName, permissionName, userId, null)
            } catch (e: NoSuchMethodError) {
                pm.revokeRuntimePermission(packageName, permissionName, userId)
            }
        } else {
            val pm = packageManager ?: throw RemoteException("can't get IPackageManger")
            pm.revokeRuntimePermission(packageName, permissionName, userId)
        }
    }

    @JvmStatic
    @Throws(RemoteException::class)
    fun getOpsForPackage(uid: Int, packageName: String, ops: IntArray?)
            : List<* /* AppOpsManager.PackageOps */>? {
        val ao = appOpsService ?: throw RemoteException("can't get IAppOpsService")
        return ao.getOpsForPackage(uid, packageName, ops)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @JvmStatic
    @Throws(RemoteException::class)
    fun getUidOps(uid: Int, ops: IntArray?): List<* /* AppOpsManager.PackageOps */>? {
        val ao = appOpsService ?: throw RemoteException("can't get IAppOpsService")
        return ao.getUidOps(uid, ops)
    }

    @JvmStatic
    @Throws(RemoteException::class)
    fun setUidMode(code: Int, uid: Int, mode: Int) {
        val ao = appOpsService ?: throw RemoteException("can't get IAppOpsService")
        ao.setUidMode(code, uid, mode)
    }

    @JvmStatic
    fun registerStorageEventListener(listener: IStorageEventListener) {
        val sm = storageManager ?: throw RemoteException("can't get IStorageManager")
        sm.registerListener(listener)
    }

    @JvmStatic
    fun unregisterStorageEventListener(listener: IStorageEventListener) {
        val sm = storageManager ?: throw RemoteException("can't get IStorageManager")
        sm.unregisterListener(listener)
    }

    @JvmStatic
    fun getVolumes(flags: Int): Array<out VolumeInfo> {
        val sm = storageManager ?: throw RemoteException("can't get IStorageManager")
        return sm.getVolumes(flags)
    }
}
