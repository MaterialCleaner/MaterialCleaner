package me.gm.cleaner.starter

import android.content.Context
import android.system.Os
import me.gm.cleaner.R
import me.gm.cleaner.util.LibUtils
import java.io.*
import java.util.zip.ZipFile

object Starter {
    lateinit var command: String

    fun writeDataFiles(context: Context) {
        val dir = context.createDeviceProtectedStorageContext().filesDir
        val starter = copyStarter(context, dir.resolve("starter"))
        val sh = writeScript(context, dir.resolve("start.sh"), starter)
        command = "sh $sh --apk=${context.applicationInfo.sourceDir}"
    }

    @Throws(IOException::class)
    private fun copyStarter(context: Context, out: File): String {
        val so = LibUtils.getLibEntryName("starter")
        ZipFile(LibUtils.getLibSourceDir(context.applicationInfo)).use { apk ->
            val entry = apk.getEntry(so) ?: throw NoSuchFileException(File(so))
            apk.getInputStream(entry).copyTo(out.outputStream())
            return out.absolutePath
        }
    }

    @Throws(IOException::class)
    private fun writeScript(context: Context, out: File, starter: String): String {
        if (!out.exists()) {
            out.createNewFile()
        }
        val br = BufferedReader(InputStreamReader(context.resources.openRawResource(R.raw.start)))
        val pw = PrintWriter(FileWriter(out))
        var line = br.readLine()
        while (line != null) {
            pw.println(line.replace("@SOURCE@", "\"$starter\""))
            line = br.readLine()
        }
        pw.flush()
        pw.close()
        return out.absolutePath
    }

    fun writeSourceDir(context: Context): String {
        val dir = context.createDeviceProtectedStorageContext().filesDir
        val out = dir.resolve("source_dir")
        if (!out.exists()) {
            out.createNewFile()
        }
        val sourceDir = context.applicationInfo.sourceDir
        out.outputStream().use {
            it.write(sourceDir.toByteArray())
            it.flush()
        }
        return sourceDir
    }

    fun deleteSourceDir(context: Context) {
        val dir = context.createDeviceProtectedStorageContext().filesDir
        Os.remove(dir.resolve("source_dir").path)
    }
}
