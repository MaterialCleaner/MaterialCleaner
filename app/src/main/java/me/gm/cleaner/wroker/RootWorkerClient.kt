package me.gm.cleaner.wroker

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import me.gm.cleaner.browser.IRootWorkerService
import me.gm.cleaner.client.CleanerClient
import me.gm.cleaner.nio.RootWorkerService
import java.nio.file.AccessDeniedException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object RootWorkerClient {
    private lateinit var intent: Intent

    fun init(context: Context): RootWorkerClient {
        if (!::intent.isInitialized) {
            intent = Intent(context.applicationContext, LibSuWorkerService::class.java)
        }
        return this
    }

    private fun launch(): IRootWorkerService = runBlocking {
        suspendCancellableCoroutine { continuation ->
            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, service: IBinder) {
                    val serviceInterface = IRootWorkerService.Stub.asInterface(service)
                    continuation.resume(serviceInterface)
                }

                override fun onServiceDisconnected(name: ComponentName) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(
                            RemoteException("libsu onServiceDisconnected")
                        )
                    }
                }

                override fun onBindingDied(name: ComponentName) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(
                            RemoteException("libsu onBindingDied")
                        )
                    }
                }

                override fun onNullBinding(name: ComponentName) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(
                            RemoteException("libsu onNullBinding")
                        )
                    }
                }
            }
            launch(Dispatchers.Main.immediate) {
                RootService.bind(intent, connection)
            }
            continuation.invokeOnCancellation {
                launch(Dispatchers.Main.immediate) {
                    RootService.unbind(connection)
                }
            }
        }
    }

    private var service: IRootWorkerService? = null

    @JvmStatic
    @Synchronized
    fun get(path: String): IRootWorkerService {
        service = CleanerClient.service?.newRootWorkerService()
        if (service == null) {
            if (!Shell.getShell().isRoot) {
                throw AccessDeniedException(path)
            }
            service = launch()
        }
        service?.asBinder()?.linkToDeath(
            object : IBinder.DeathRecipient {
                override fun binderDied() {
                    service!!.asBinder().unlinkToDeath(this, 0)
                    service = null
                }
            },
            0
        )
        return service!!
    }
}

class LibSuWorkerService : RootService() {

    override fun onBind(intent: Intent): IBinder = RootWorkerService()
}
