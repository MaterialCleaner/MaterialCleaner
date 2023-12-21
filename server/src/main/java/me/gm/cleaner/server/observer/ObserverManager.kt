package me.gm.cleaner.server.observer

import android.util.Log
import me.gm.cleaner.client.CleanerHooksClient
import me.gm.cleaner.server.BuildConfig
import me.gm.cleaner.server.CleanerServer
import java.util.concurrent.ConcurrentHashMap

object ObserverManager {
    private val observers: MutableMap<Class<*>, BaseObserver> = ConcurrentHashMap()

    fun startAllObservers(server: CleanerServer) {
        Log.i(BuildConfig.LIBRARY_PACKAGE_NAME, "startAllObservers")
        val observers = mutableListOf<BaseObserver>()
        observers += EmulatedStorageMountObserver()
        observers += ActivityManagerLogsObserver(server)
        if (CleanerHooksClient.pingBinder()) {
            observers += FileSystemObserver(server)
            observers += IntentReceiver()
        } else {
            observers += DataAppDirObserver()
        }
        observers.forEach { it.start() }
    }

    fun registerObserver(observer: BaseObserver) {
        observers[observer.javaClass] = observer
    }

    fun unregisterObserver(observer: BaseObserver) {
        observers.remove(observer.javaClass)
    }

    fun getObservers(): Collection<BaseObserver> = observers.values

    @JvmStatic
    fun <T : BaseObserver> fastGetObserver(javaClass: Class<T>): T? =
        observers[javaClass] as T?

    fun <T : BaseObserver> getObserver(javaClass: Class<T>): T? =
        observers.values.firstOrNull { javaClass.isAssignableFrom(it.javaClass) } as T?

    fun stopAllObservers() {
        Log.i(BuildConfig.LIBRARY_PACKAGE_NAME, "stopAllObservers")
        observers.values.forEach { it.stop() }
        observers.clear()
    }
}
