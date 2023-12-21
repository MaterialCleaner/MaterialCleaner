package me.gm.cleaner.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ParceledException(
    val exceptionClass: String,
    val message: String?
) : Parcelable {

    fun reproduce() {
        throw Class.forName(exceptionClass)
            .getDeclaredConstructor(String::class.java)
            .newInstance(message) as Throwable
    }

    companion object {

        @JvmStatic
        fun create(exception: Throwable): ParceledException = ParceledException(
            exception.javaClass.name,
            exception.message
        )
    }
}
