package me.gm.cleaner.home.scanner.service

import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.extension
import kotlin.io.path.name

class LogScannerService : BaseScannerService() {

    override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): Boolean {
        val name = dir.name
        return name.endsWith("log", true) && !name.endsWith("dialog", true) ||
                name.endsWith("logs", true)
    }

    override fun visitFile(file: Path, attrs: BasicFileAttributes): Boolean {
        val name = file.name
        if (name.contains("_log", true) && !name.contains("login", true)) {
            return true
        }
        val extension = file.extension
        return extension.endsWith("log", true) || extension.endsWith("logs", true)
    }
}
