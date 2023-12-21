package me.gm.cleaner.home.scanner.service

import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.name

class CacheScannerService : BaseScannerService() {

    override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): Boolean {
        val name = dir.name
        return name.contains("cache", true) && !name.contains("auth", true)
    }

    override fun visitFile(file: Path, attrs: BasicFileAttributes): Boolean {
        val name = file.name
        return name.endsWith(".cache", true)
    }
}
