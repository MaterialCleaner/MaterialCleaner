package me.gm.cleaner.browser

import me.gm.cleaner.model.ParceledException

open class SimpleProgressListener : IProgressListener.Stub() {
    private var parceledException: ParceledException? = null

    override fun onProgress(progress: Float) {}

    override fun onException(exception: ParceledException) {
        parceledException = exception
    }

    fun throwIfHasException() {
        parceledException?.reproduce()
    }
}
