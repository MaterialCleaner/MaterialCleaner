package me.gm.cleaner.util

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.os.Build

object LibUtils {

    fun isSplitApk(ai: ApplicationInfo): Boolean = !ai.splitSourceDirs.isNullOrEmpty()

    @JvmStatic
    fun findLibSourceDir(splitSourceDirs: Array<String>): String = splitSourceDirs.first {
        val middleName = it.substringAfterLast('/')
            .removeSurrounding("split_config.", ".apk")
        middleName.startsWith("arm") || middleName.startsWith("x86")
    }

    @JvmStatic
    fun getLibSourceDir(ai: ApplicationInfo): String {
        return if (!isSplitApk(ai)) {
            ai.sourceDir
        } else {
            findLibSourceDir(ai.splitSourceDirs!!)
        }
    }

    fun getLibEntryName(name: String): String = "lib/${Build.SUPPORTED_ABIS[0]}/lib$name.so"

    @SuppressLint("UnsafeDynamicallyLoadedCode")
    @JvmStatic
    fun loadLibrary(filename: String, libname: String) {
        System.load("$filename!/${getLibEntryName(libname)}")
    }
}
