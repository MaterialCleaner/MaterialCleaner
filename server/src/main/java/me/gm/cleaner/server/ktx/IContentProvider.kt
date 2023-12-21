package me.gm.cleaner.server.ktx

import android.content.AttributionSource
import android.content.IContentProvider
import android.os.Bundle
import android.os.RemoteException
import android.system.Os
import api.util.BuildUtils

@Throws(RemoteException::class)
fun IContentProvider.callCompat(
    callingPkg: String?, authority: String?, method: String?, arg: String?, extras: Bundle?
): Bundle {
    return when {
        BuildUtils.atLeast31() -> {
            try {
                call(
                    AttributionSource.Builder(Os.getuid()).setPackageName(callingPkg).build(),
                    authority, method, arg, extras
                )
            } catch (e: Throwable) {
                e.printStackTrace()
                call(callingPkg, null, authority, method, arg, extras)
            }
        }
        BuildUtils.atLeast30() -> {
            call(callingPkg, null, authority, method, arg, extras)
        }
        BuildUtils.atLeast29() -> {
            call(callingPkg, authority, method, arg, extras)
        }
        else -> {
            call(callingPkg, method, arg, extras)
        }
    }
}
