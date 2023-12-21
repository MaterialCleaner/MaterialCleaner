package me.gm.cleaner.client

import android.os.IBinder
import android.os.RemoteException
import api.SystemService
import me.gm.cleaner.dao.MountRules
import me.gm.cleaner.dao.PurchaseVerification
import me.gm.cleaner.dao.ServicePreferences
import me.gm.cleaner.server.CleanerServer
import me.gm.cleaner.server.ICleanerHooksService
import me.gm.cleaner.server.observer.ObserverManager
import me.gm.cleaner.util.SystemPropertiesUtils
import java.util.function.Consumer

object CleanerHooksClient {
    /**
     * Minimum required module version.
     */
    const val MIN_REQUIRED_ZYGISK_MODULE_VERSION: Int = 1494

    private var binder: IBinder? = null
    private var service: ICleanerHooksService? = null
    private var deathRecipient: IBinder.DeathRecipient? = null

    fun onStart(server: CleanerServer) {
        binder = CleanerHooksBinderRetriever.get() ?: return
        service = ICleanerHooksService.Stub.asInterface(binder)
        deathRecipient = object : SystemServiceDeathRecipient(binder) {
            override fun binderDied() {
                super.binderDied()
                server.handler.post {
                    ObserverManager.stopAllObservers()
                    server.waitSystemServices()
                    server.onStorageManagerServiceReady()
                }
            }
        }
        try {
            binder?.linkToDeath(deathRecipient!!, 0)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    private fun pingBinderInternal(): Boolean = binder?.pingBinder() == true

    @JvmStatic
    fun pingBinder(): Boolean =
        pingBinderInternal() && zygiskModuleVersion >= MIN_REQUIRED_ZYGISK_MODULE_VERSION

    val zygiskModuleVersion: Int
        get() = if (pingBinderInternal()) {
            service!!.moduleVersion
        } else {
            -1
        }

    val zygiskNeedUpgrade: Boolean
        get() = pingBinderInternal() && zygiskModuleVersion < MIN_REQUIRED_ZYGISK_MODULE_VERSION

    val needEnableInLsp: Boolean
        get() = pingBinderInternal() && !service!!.hasMediaProviderBinder()

    @JvmStatic
    fun whileAlive(c: Consumer<ICleanerHooksService>) {
        if (pingBinder()) {
            c.accept(service!!)
        }
    }

    @JvmStatic
    fun ICleanerHooksService.syncSrPackages() {
        if (SystemPropertiesUtils.getBoolean(
                "persist.sys.vold_app_data_isolation_enabled", false
            )!!
        ) {
            setSrPackages(ServicePreferences.srPackages.toList())
        }
    }

    @JvmStatic
    fun ICleanerHooksService.syncReadOnlyPaths() {
        if (PurchaseVerification.isLoosePro) {
            setReadOnlyPaths(ServicePreferences.getAllReadOnly())
        }
    }

    @JvmStatic
    fun ICleanerHooksService.syncMountPoint() {
        val mountPoint = mutableListOf<String>()
        val userIds = SystemService.getUserIdsNoThrow()
        for (packageName in ServicePreferences.srPackages) {
            for (userId in userIds) {
                val rules = MountRules(
                    ServicePreferences.getPackageSrZipped(packageName, userId)
                )
                mountPoint += rules.mountPoint
            }
        }
        setMountPoint(mountPoint)
    }

    @JvmStatic
    fun ICleanerHooksService.syncRecordExternalAppSpecificStorage() {
        setRecordExternalAppSpecificStorage(ServicePreferences.recordExternalAppSpecificStorage)
    }

    fun onDestroy() {
        if (pingBinderInternal()) {
            try {
                binder?.unlinkToDeath(deathRecipient!!, 0)
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        }
    }
}
