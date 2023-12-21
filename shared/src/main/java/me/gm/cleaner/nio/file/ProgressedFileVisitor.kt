package me.gm.cleaner.nio.file

import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.attribute.BasicFileAttributes

interface ProgressedFileVisitor<T> : FileVisitor<T> {

    fun onProgress(progress: Float)
}

open class SimpleProgressedFileVisitor<T> : ProgressedFileVisitor<T> {

    override fun onProgress(progress: Float) {}

    override fun preVisitDirectory(dir: T, attrs: BasicFileAttributes): FileVisitResult {
        return FileVisitResult.CONTINUE
    }

    override fun visitFile(file: T, attrs: BasicFileAttributes): FileVisitResult {
        return FileVisitResult.CONTINUE
    }

    @Throws(IOException::class)
    override fun visitFileFailed(file: T, exc: IOException): FileVisitResult {
        throw exc
    }

    @Throws(IOException::class)
    override fun postVisitDirectory(dir: T, exc: IOException?): FileVisitResult {
        if (exc != null) throw exc
        return FileVisitResult.CONTINUE
    }
}
