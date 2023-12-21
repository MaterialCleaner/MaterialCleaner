package me.gm.cleaner.client

import android.content.pm.PackageInfo
import android.os.IBinder
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import me.gm.cleaner.dao.PurchaseVerification
import me.gm.cleaner.server.ICleanerService

object CleanerClient {
    private val _serverVersionLiveData: MutableLiveData<Int> = MutableLiveData(-1)
    val serverVersionLiveData: LiveData<Int> = _serverVersionLiveData
    var serverVersion: Int
        get() = _serverVersionLiveData.value!!
        private set(value) {
            _serverVersionLiveData.postValue(value)
        }

    private var binder: IBinder? = null
    var service: ICleanerService? = null
        private set
    private val DEATH_RECIPIENT: IBinder.DeathRecipient = IBinder.DeathRecipient {
        binder = null
        service = null
        serverVersion = -1
    }

    fun pingBinder(): Boolean = binder?.pingBinder() == true

    @Synchronized
    fun onBinderReceived(newBinder: IBinder) {
        if (binder == newBinder) return
        binder?.unlinkToDeath(DEATH_RECIPIENT, 0)
        binder = newBinder
        binder?.linkToDeath(DEATH_RECIPIENT, 0)
        service = ICleanerService.Stub.asInterface(newBinder)
        serverVersion = service!!.serverVersion
        maybeSyncCertificate()
    }

    val zygiskEnabled: Boolean
        get() = pingBinder() && service?.zygiskModuleVersion != -1

    fun getInstalledPackages(flags: Int): List<PackageInfo> =
        service!!.getInstalledPackages(flags).list

    val mountedDirs: List<String>
        get() = try {
            if (pingBinder()) service!!.mountedDirs else emptyList()
        } catch (e: SecurityException) {
            emptyList()
        }

    fun updateCertificate(responseBody: String) {
        PurchaseVerification.updateCertificate(responseBody)
        if (pingBinder()) {
            service!!.syncCertificates(PurchaseVerification.maybeCertificates)
            service!!.syncSignatures(PurchaseVerification.signatures.toList())
        } else {
            PurchaseVerification.isSyncNeeded = true
        }
    }

    fun removeCertificate(purchaseToken: String) {
        PurchaseVerification.removeCertificate(purchaseToken)
        PurchaseVerification.signatures = emptySet()
        if (pingBinder()) {
            service!!.syncCertificates(PurchaseVerification.maybeCertificates)
            service!!.syncSignatures(PurchaseVerification.signatures.toList())
        } else {
            PurchaseVerification.isSyncNeeded = true
        }
    }

    fun maybeSyncCertificate() {
        if (PurchaseVerification.isSyncNeeded && pingBinder()) {
            service!!.syncCertificates(PurchaseVerification.maybeCertificates)
            service!!.syncSignatures(PurchaseVerification.signatures.toList())
            PurchaseVerification.isSyncNeeded = false
        }
    }

    fun exit() {
        runCatching {
            service!!.exit()
        }
    }
}
