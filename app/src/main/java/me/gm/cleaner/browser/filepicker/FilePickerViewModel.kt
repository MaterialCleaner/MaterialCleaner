package me.gm.cleaner.browser.filepicker

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import me.gm.cleaner.BuildConfig
import me.gm.cleaner.R
import me.gm.cleaner.browser.VirtualFileSystemProvider
import me.gm.cleaner.browser.VirtualFileSystemProvider.schemeRoot
import me.gm.cleaner.browser.filepicker.FilePickerDialog.Companion.SelectType
import me.gm.cleaner.util.FileUtils
import me.gm.cleaner.util.copy
import me.gm.cleaner.util.fileNameComparator
import me.gm.cleaner.util.listDirectoryEntriesSafe
import me.gm.cleaner.util.listFormat
import java.nio.file.Path
import java.util.function.Consumer
import kotlin.io.path.name

class FilePickerViewModel(application: Application) : AndroidViewModel(application) {
    internal val onPositiveButtonClickListeners: MutableSet<Consumer<Path>> = mutableSetOf()

    private fun defaultPath(): Path = VirtualFileSystemProvider.getPath(
        FileUtils.externalStorageDir.toURI().copy(scheme = schemeRoot)
    )

    private val pathFlow: MutableStateFlow<Path> = MutableStateFlow(defaultPath())
    var path: Path
        get() = pathFlow.value
        set(value) {
            pathFlow.value = value
        }
    var roots: List<Path> = listOf(defaultPath())
    val canGoUp: Boolean
        get() = roots.any { root -> path.startsWith(root) && path != root }

    fun goUp(select: Boolean) {
        if (BuildConfig.DEBUG) {
            check(canGoUp)
        }
        path = path.parent
        if (select) {
            select(path)
        }
    }

    @SelectType
    var selectType: Int = SelectType.SELECT_FILE_AND_FOLDER
    val selectedFlow: MutableStateFlow<Path?> = MutableStateFlow(null)
    var selected: Path?
        get() = selectedFlow.value
        set(value) {
            selectedFlow.value = value
        }

    fun select(
        rawCandidate: Path, context: Context? = null, errno: ((error: String?) -> Unit)? = null
    ): Boolean {
        val candidate = rawCandidate.normalize()

        if (roots.none { root -> candidate.startsWith(root) }) {
            errno?.invoke(
                context?.getString(
                    R.string.file_picker_error_starts_with,
                    roots.listFormat(context.getString(R.string.delimiter))
                )
            )
            selected = null
            return false
        }

        selected = candidate
        errno?.invoke(null)
        return true
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val fileListFlow: Flow<List<FilePickerModel>> = pathFlow
        .flatMapLatest { path ->
            flow {
                emit(path.listDirectoryEntriesSafe())
            }
        }
        .combine(selectedFlow) { fileList, selected ->
            var sequence = fileList.asSequence()
                .map { FilePickerModel(path = it, isSelected = it == selected) }
                .sortedWith(fileNameComparator { it.path.name })
            sequence = if (selectType == SelectType.SELECT_FOLDER) {
                sequence.filter { it.attrs.isDirectory }
            } else {
                sequence.sortedByDescending { it.attrs.isDirectory }
            }
            sequence.toList()
        }
        .flowOn(Dispatchers.IO)
}
