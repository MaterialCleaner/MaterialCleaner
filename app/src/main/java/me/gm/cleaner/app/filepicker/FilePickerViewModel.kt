package me.gm.cleaner.app.filepicker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import me.gm.cleaner.client.CleanerClient
import me.gm.cleaner.model.FileModel
import me.gm.cleaner.util.listDirectoryEntriesSafe
import java.util.function.Consumer
import kotlin.io.path.Path

class FilePickerViewModel(application: Application) : AndroidViewModel(application) {
    val onPositiveButtonClickListeners: MutableSet<Consumer<String>> = mutableSetOf()
    val filePicker: FilePicker = FilePicker()
}

fun createFileModel(path: String): FileModel = if (CleanerClient.pingBinder()) {
    CleanerClient.service!!.createFileModel(path)
} else {
    FileModel(Path(path))
}

fun listFile(path: String): List<FileModel> = if (CleanerClient.pingBinder()) {
    CleanerClient.service!!.listFiles(path).list
} else {
    Path(path).listDirectoryEntriesSafe()
        .map { file ->
            FileModel(file)
        }
}
