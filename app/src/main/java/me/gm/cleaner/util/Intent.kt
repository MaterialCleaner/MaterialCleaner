package me.gm.cleaner.util

import android.content.Intent
import android.os.Parcelable
import androidx.core.content.IntentCompat
import java.io.Serializable

inline fun <reified T : Parcelable> Intent.getParcelableExtraCompat(name: String?)
        : T? = IntentCompat.getParcelableExtra(this, name, T::class.java)

inline fun <reified T : Parcelable> Intent.getParcelableArrayExtraCompat(name: String?)
        : Array<out Parcelable>? = IntentCompat.getParcelableArrayExtra(this, name, T::class.java)

inline fun <reified T : Parcelable> Intent.getParcelableArrayListExtraCompat(name: String?)
        : ArrayList<T>? = IntentCompat.getParcelableArrayListExtra(this, name, T::class.java)

inline fun <reified T : Serializable> Intent.getSerializableExtraCompat(name: String?)
        : T? = extras?.getSerializableCompat(name)
