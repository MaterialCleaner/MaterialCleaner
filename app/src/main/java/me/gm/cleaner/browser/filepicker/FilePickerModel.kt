package me.gm.cleaner.browser.filepicker

import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.readAttributes

data class FilePickerModel(
    val path: Path,
    val attrs: BasicFileAttributes = path.readAttributes(LinkOption.NOFOLLOW_LINKS),
    val isSelected: Boolean
)
