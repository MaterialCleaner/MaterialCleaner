package me.gm.cleaner.app.filepicker

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import me.gm.cleaner.R
import me.gm.cleaner.app.filepicker.FilePickerDialog.Companion.SelectType.Companion.SELECT_FILE
import me.gm.cleaner.app.filepicker.FilePickerDialog.Companion.SelectType.Companion.SELECT_FILE_AND_FOLDER
import me.gm.cleaner.app.filepicker.FilePickerDialog.Companion.SelectType.Companion.SELECT_FOLDER
import me.gm.cleaner.model.FileModel
import me.gm.cleaner.util.FileUtils
import me.gm.cleaner.util.fileNameComparator
import java.io.File

class FilePicker() : Parcelable {
    private val pathFlow: MutableStateFlow<String> =
        MutableStateFlow(FileUtils.externalStorageDir.path)
    var path: String
        get() = pathFlow.value
        set(value) {
            pathFlow.value = value
        }
    var root: String = FileUtils.externalStorageDir.path
    val canGoUp: Boolean
        get() = path.startsWith(root) && path != root

    fun goUp(select: Boolean) {
        check(canGoUp)
        path = path.substringBeforeLast(File.separator)
            .ifEmpty { File.separator }
        if (select) {
            select(FileModel(path, true, false))
        }
    }

    @FilePickerDialog.Companion.SelectType
    var selectType: Int = SELECT_FILE_AND_FOLDER
    val selectedFlow: MutableStateFlow<String> = MutableStateFlow("")
    var selected: String
        get() = selectedFlow.value
        private set(value) {
            selectedFlow.value = value
        }

    fun select(
        file: FileModel, context: Context? = null, errno: ((error: String?) -> Unit)? = null
    ): Boolean {
        if (selectType == SELECT_FILE && !file.isFile ||
            selectType == SELECT_FOLDER && !file.isDirectory
        ) {
            selected = ""
            return false
        }

        val path = file.path
        if (path.contains("\u0000") || path != File.separator &&
            path.endsWith(File.separator) ||
            path.endsWith(String(charArrayOf(File.separatorChar, '.'))) ||
            path.endsWith(String(charArrayOf(File.separatorChar, '.', '.'))) ||
            path.contains(String(charArrayOf(File.separatorChar, File.separatorChar))) ||
            path.contains(String(charArrayOf(File.separatorChar, '.', File.separatorChar))) ||
            path.contains(String(charArrayOf(File.separatorChar, '.', '.', File.separatorChar)))
        ) {
            if (errno != null) {
                errno(context!!.getString(R.string.file_picker_error_format))
            }
            selected = ""
            return false
        }

        if (root.endsWith(File.separator) && !FileUtils.childOf(root, path) ||
            !root.endsWith(File.separator) && !FileUtils.startsWith(root, path)
        ) {
            if (errno != null) {
                errno(context!!.getString(R.string.file_picker_error_starts_with, root))
            }
            selected = ""
            return false
        }

        selected = path
        if (errno != null) {
            errno(null)
        }
        return true
    }

    val fileListFlow: Flow<List<FilePickerModel>> =
        combine(pathFlow, selectedFlow) { path, selected ->
            var sequence = listFile(path).asSequence()
            sequence = sequence.sortedWith(fileNameComparator { it.path })
            sequence = if (selectType == SELECT_FOLDER) {
                sequence.filter { it.isDirectory }
            } else {
                sequence.sortedByDescending { it.isDirectory }
            }
            sequence
                .map { FilePickerModel(it, it.path == selected) }
                .toList()
        }

    constructor(parcel: Parcel) : this() {
        path = parcel.readString()!!
        root = parcel.readString()!!
        selectType = parcel.readInt()
        selected = parcel.readString()!!
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(path)
        parcel.writeString(root)
        parcel.writeInt(selectType)
        parcel.writeString(selected)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<FilePicker> {
        override fun createFromParcel(parcel: Parcel): FilePicker = FilePicker(parcel)
        override fun newArray(size: Int): Array<FilePicker?> = arrayOfNulls(size)
    }
}
