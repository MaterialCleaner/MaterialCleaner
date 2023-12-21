package me.gm.cleaner.util

import android.os.Build
import android.os.Bundle
import androidx.core.os.BundleCompat
import java.io.Serializable

inline fun <reified T> Bundle.getParcelableCompat(key: String): T? {
    classLoader = T::class.java.classLoader
    return BundleCompat.getParcelable(this, key, T::class.java)
}

inline fun <reified T : Serializable> Bundle.getSerializableCompat(key: String?): T? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getSerializable(key, T::class.java)
    } else {
        getSerializable(key) as T
    }
