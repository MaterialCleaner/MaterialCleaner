package me.gm.cleaner.util

import android.os.Parcel
import androidx.core.os.ParcelCompat
import java.io.Serializable

inline fun <reified T : Serializable> Parcel.readSerializableCompat()
        : T? = ParcelCompat.readSerializable(this, T::class.java.classLoader, T::class.java)
