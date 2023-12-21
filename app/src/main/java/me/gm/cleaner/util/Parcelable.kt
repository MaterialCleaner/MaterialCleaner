package me.gm.cleaner.util

import android.os.BadParcelableException
import android.os.Parcel
import android.os.Parcelable
import android.util.Base64
import java.lang.reflect.Modifier

fun Parcelable.toBase64String(): String {
    val parcel = Parcel.obtain()
//    parcel.writeParcelable(this, 0)
    writeToParcel(parcel, 0)
    val bytes = parcel.marshall()
    parcel.recycle()
    return Base64.encodeToString(bytes, Base64.NO_WRAP)
}

inline fun <reified T : Parcelable> String.toParcelable(
    loader: ClassLoader? = T::class.java.classLoader, clazz: Class<T> = T::class.java
): T {
    val bytes = Base64.decode(this, Base64.DEFAULT)
    val parcel = Parcel.obtain()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)
    try {
//        return ParcelCompat.readParcelable(parcel, loader, clazz) as T
        val creator = findParcelableCreator(clazz)
        if (creator is Parcelable.ClassLoaderCreator<*>) {
            return creator.createFromParcel(parcel, loader) as T
        }
        return creator.createFromParcel(parcel)
    } finally {
        parcel.recycle()
    }
}

fun <T> findParcelableCreator(parcelableClass: Class<T>): Parcelable.Creator<T> {
    val name = parcelableClass.name;
    val f = parcelableClass.getField("CREATOR")
    if (f.modifiers and Modifier.STATIC == 0) {
        throw BadParcelableException(
            "Parcelable protocol requires "
                    + "the CREATOR object to be static on class " + name
        )
    }
    val creatorType = f.type
    if (!Parcelable.Creator::class.java.isAssignableFrom(creatorType)) {
        // Fail before calling Field.get(), not after, to avoid initializing
        // parcelableClass unnecessarily.
        throw BadParcelableException(
            "Parcelable protocol requires a "
                    + "Parcelable.Creator object called "
                    + "CREATOR on class " + name
        )
    }
    return f[null] as Parcelable.Creator<T>
}
