package me.gm.cleaner.home.scanner.service

import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.name

class TempScannerService : BaseScannerService() {

    override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): Boolean {
        val name = dir.name
        return !name.contains("template", true) &&
                (name.contains("temp", true) || name.contains("tmp", true))
    }

    override fun visitFile(file: Path, attrs: BasicFileAttributes): Boolean {
        val name = file.name
        return name.endsWith(".temp", true) || name.endsWith(".tmp", true)
    }
}
