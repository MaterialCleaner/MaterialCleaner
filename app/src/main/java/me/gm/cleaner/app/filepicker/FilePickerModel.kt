package me.gm.cleaner.app.filepicker

import me.gm.cleaner.model.FileModel

data class FilePickerModel(
    val file: FileModel,
    val isSelected: Boolean,
)
