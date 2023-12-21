package me.gm.cleaner.util

import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.pathString
import kotlin.io.path.toPath

fun Path.encode(): URI = toUri().copy(scheme = null)

fun Path.encodeToString(): String = encode().toASCIIString()

@JvmOverloads
fun String.decodeURI(scheme: String = FileSystems.getDefault().provider().scheme): String =
    URI.create(this).copy(scheme = scheme).toPath().pathString

@JvmOverloads
fun Path.listDirectoryEntriesSafe(glob: String = "*"): List<Path> =
    try {
        listDirectoryEntries(glob)
    } catch (e: Exception) {
        emptyList()
    }
