package me.gm.cleaner.home.scanner

import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes

interface ScannerPredicate {

    fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): Boolean

    fun visitFile(file: Path, attrs: BasicFileAttributes): Boolean

    fun postVisitDirectory(dir: Path): Boolean
}
