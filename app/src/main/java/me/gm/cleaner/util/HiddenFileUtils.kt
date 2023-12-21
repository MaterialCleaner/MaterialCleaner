package me.gm.cleaner.util

import java.nio.file.Path
import kotlin.io.path.name

object HiddenFileUtils {

    fun toHidden(path: Path): Path = path.resolveSibling(".${path.name}")

    fun toUnhidden(path: Path): Path = path.resolveSibling(path.name.substringAfter('.', ""))
}
