package me.gm.cleaner.client.ui

import android.content.pm.PackageInfo

data class AppPickerModel(
    val packageInfo: PackageInfo,
    val label: String,
    val isChecked: Boolean,
) {

    override fun hashCode(): Int {
        var result = label.hashCode()
        result = 31 * result + isChecked.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AppPickerModel

        if (label != other.label) return false
        if (isChecked != other.isChecked) return false

        return true
    }
}
