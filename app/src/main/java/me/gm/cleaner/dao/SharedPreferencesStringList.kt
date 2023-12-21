package me.gm.cleaner.dao

import android.content.SharedPreferences
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import me.gm.cleaner.util.toBase64String
import me.gm.cleaner.util.toParcelable

@Parcelize
data class ParceledStringList(val value: List<String>) : Parcelable

fun SharedPreferences.getStringList(key: String, defValue: List<String>?): List<String>? =
    getString(key, null)?.toParcelable<ParceledStringList>()?.value ?: defValue

fun SharedPreferences.Editor.putStringList(key: String, value: List<String>) {
    putString(key, ParceledStringList(value).toBase64String())
}
