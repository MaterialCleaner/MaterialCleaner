package me.gm.cleaner.nio.file

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

@Throws(IOException::class)
fun Path.walkPathTreeProgressed(
    followLinks: Boolean = false, maxDepth: Int = Int.MAX_VALUE,
    visitor: ProgressedFileVisitor<in Path>
): Path {
    var progress = 0F
    fun increaseProgress(incrementalProgress: Float) {
        progress += incrementalProgress
        visitor.onProgress(progress)
    }
    Files.walkFileTree(this, visitor)
    return this
}
