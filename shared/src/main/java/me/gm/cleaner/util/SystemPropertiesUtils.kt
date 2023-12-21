package me.gm.cleaner.util

import java.lang.reflect.Method

object SystemPropertiesUtils {
    private val getMethod: Method by lazy {
        Class.forName("android.os.SystemProperties")
            .getMethod("get", String::class.java, String::class.java)
    }

    @JvmStatic
    fun get(key: String, def: String = ""): String =
        getMethod.invoke(null, key, def) as String

    @JvmStatic
    fun getBoolean(key: String, def: Boolean? = null): Boolean? =
        when (get(key)) {
            true.toString() -> true
            false.toString() -> false
            else -> def
        }
}
