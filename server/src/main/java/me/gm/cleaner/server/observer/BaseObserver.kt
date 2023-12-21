package me.gm.cleaner.server.observer

import androidx.annotation.CallSuper

abstract class BaseObserver {

    @CallSuper
    protected open fun onStart() {
        ObserverManager.registerObserver(this)
    }

    @CallSuper
    protected open fun onDestroy() {
        ObserverManager.unregisterObserver(this)
    }

    fun start() {
        onStart()
    }

    fun stop() {
        try {
            onDestroy()
        } catch (_: Throwable) {
        }
    }
}
