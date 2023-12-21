package me.gm.cleaner.client.ui

import android.content.pm.PackageInfo
import me.gm.cleaner.model.FileSystemEvent

data class FileSystemRecordModel(
    val event: FileSystemEvent,
    val packageInfo: PackageInfo?,
    val label: String?,
    val isReadOnlyPath: Boolean,
)
