package me.gm.cleaner.nio

import java.nio.file.FileSystemException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.SecureDirectoryStream
import java.nio.file.attribute.BasicFileAttributeView
import kotlin.io.path.deleteIfExists
import kotlin.io.path.isDirectory

@SinceKotlin("1.8")
fun Path.deleteRecursivelyInterruptable(onCheckContinuation: () -> Unit) {
    val suppressedExceptions = this.deleteRecursivelyImpl(onCheckContinuation)

    if (suppressedExceptions.isNotEmpty()) {
        throw FileSystemException("Failed to delete one or more files. See suppressed exceptions for details.")
            .apply {
                suppressedExceptions.forEach { addSuppressed(it) }
            }
    }
}

private class ExceptionsCollector(
    private val onCheckContinuation: () -> Unit,
    private val limit: Int = 64
) {
    var totalExceptions: Int = 0
        private set

    val collectedExceptions = mutableListOf<Exception>()

    var path: Path? = null

    fun enterEntry(name: Path) {
        path = path?.resolve(name)
    }

    fun exitEntry(name: Path) {
        require(name == path?.fileName)
        path = path?.parent
    }

    fun collect(exception: Exception) {
        totalExceptions += 1
        val shouldCollect = collectedExceptions.size < limit
        if (shouldCollect) {
            val restoredException = if (path != null) {
                // When SecureDirectoryStream is used, only entry name gets reported in exception message.
                // Thus, wrap such exceptions in FileSystemException with restored path.
                FileSystemException(path.toString()).initCause(exception) as FileSystemException
            } else {
                exception
            }
            collectedExceptions.add(restoredException)
        }
    }

    fun checkContinuation() {
        onCheckContinuation.invoke()
    }
}

private fun Path.deleteRecursivelyImpl(onCheckContinuation: () -> Unit): List<Exception> {
    val collector = ExceptionsCollector(onCheckContinuation)
    var useInsecure = true

    // TODO: KT-54077
    this.parent?.let { parent ->
        val directoryStream = try {
            Files.newDirectoryStream(parent)
        } catch (_: Throwable) {
            null
        }
        directoryStream?.use { stream ->
            if (stream is SecureDirectoryStream<Path>) {
                useInsecure = false
                collector.path = parent
                stream.handleEntry(this.fileName, collector)
            }
        }
    }

    if (useInsecure) {
        insecureHandleEntry(this, collector)
    }

    return collector.collectedExceptions
}

private inline fun collectIfThrows(collector: ExceptionsCollector, function: () -> Unit) {
    collector.checkContinuation()
    try {
        function()
    } catch (exception: Exception) {
        collector.collect(exception)
    }
}

private inline fun <R> tryIgnoreNoSuchFileException(function: () -> R): R? {
    return try {
        function()
    } catch (_: NoSuchFileException) {
        null
    }
}

// secure walk

private fun SecureDirectoryStream<Path>.handleEntry(name: Path, collector: ExceptionsCollector) {
    collector.enterEntry(name)

    collectIfThrows(collector) {
        if (this.isDirectory(name, LinkOption.NOFOLLOW_LINKS)) {
            val preEnterTotalExceptions = collector.totalExceptions

            this.enterDirectory(name, collector)

            // If something went wrong trying to delete the contents of the
            // directory, don't try to delete the directory as it will probably fail.
            if (preEnterTotalExceptions == collector.totalExceptions) {
                tryIgnoreNoSuchFileException { this.deleteDirectory(name) }
            }
        } else {
            tryIgnoreNoSuchFileException { this.deleteFile(name) } // deletes symlink itself, not its target
        }
    }

    collector.exitEntry(name)
}

private fun SecureDirectoryStream<Path>.enterDirectory(name: Path, collector: ExceptionsCollector) {
    collectIfThrows(collector) {
        tryIgnoreNoSuchFileException {
            this.newDirectoryStream(name, LinkOption.NOFOLLOW_LINKS)
        }?.use { directoryStream ->
            for (entry in directoryStream) {
                directoryStream.handleEntry(entry.fileName, collector)
            }
        }
    }
}

private fun SecureDirectoryStream<Path>.isDirectory(
    entryName: Path,
    vararg options: LinkOption
): Boolean {
    return tryIgnoreNoSuchFileException {
        this.getFileAttributeView(entryName, BasicFileAttributeView::class.java, *options)
            .readAttributes().isDirectory
    } ?: false
}

// insecure walk

private fun insecureHandleEntry(entry: Path, collector: ExceptionsCollector) {
    collectIfThrows(collector) {
        if (entry.isDirectory(LinkOption.NOFOLLOW_LINKS)) {
            val preEnterTotalExceptions = collector.totalExceptions

            insecureEnterDirectory(entry, collector)

            // If something went wrong trying to delete the contents of the
            // directory, don't try to delete the directory as it will probably fail.
            if (preEnterTotalExceptions == collector.totalExceptions) {
                entry.deleteIfExists()
            }
        } else {
            entry.deleteIfExists() // deletes symlink itself, not its target
        }
    }
}

private fun insecureEnterDirectory(path: Path, collector: ExceptionsCollector) {
    collectIfThrows(collector) {
        tryIgnoreNoSuchFileException {
            Files.newDirectoryStream(path)
        }?.use { directoryStream ->
            for (entry in directoryStream) {
                insecureHandleEntry(entry, collector)
            }
        }
    }
}
