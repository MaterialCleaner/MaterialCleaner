package me.gm.cleaner.model

import android.os.Parcel
import android.os.Parcelable
import java.nio.file.CopyOption

class ParceledCopyOptions(val value: Array<out CopyOption>) : Parcelable {
    private constructor(source: Parcel) : this(
        Array(source.readInt()) {
            source.readParcelable<Parcelable>(CopyOption::class.java.classLoader)!! as CopyOption
        }
    )

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(value.size)
        for (option in value) {
            when (option) {
                is Parcelable -> {
                    dest.writeParcelable(option as Parcelable, flags)
                }

                else -> throw UnsupportedOperationException(option.toString())
            }
        }
    }

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<ParceledCopyOptions> {
            override fun createFromParcel(source: Parcel): ParceledCopyOptions =
                ParceledCopyOptions(source)

            override fun newArray(size: Int): Array<ParceledCopyOptions?> = arrayOfNulls(size)
        }
    }
}

fun Array<out CopyOption>.toParcelable(): ParceledCopyOptions = ParceledCopyOptions(this)
