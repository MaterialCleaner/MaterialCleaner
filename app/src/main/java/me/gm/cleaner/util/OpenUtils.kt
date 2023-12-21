package me.gm.cleaner.util

import android.content.Context
import android.content.Intent
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import me.gm.cleaner.BuildConfig
import me.gm.cleaner.client.ui.storageredirect.MimeUtils
import java.io.File

object OpenUtils {

    fun open(context: Context, path: String, isDir: Boolean) {
        // All other actions (OPEN_DOCUMENT, CREATE_DOCUMENT, GET_CONTENT, OPEN_DOCUMENT_TREE)
        // are intercepted by DocumentsUI, thus we can only use ACTION_VIEW.
        // Starting in Android 12, it's not possible to browse file using DocumentsUI.
        // https://source.android.com/docs/core/architecture/modular-system/documentsui
        val intent = Intent(Intent.ACTION_VIEW).apply {
            if (isDir) {
                setDataAndType(path.toUri(), DocumentsContract.Document.MIME_TYPE_DIR)
            } else {
                val file = File(path)
                try {
                    setDataAndType(
                        FileProvider.getUriForFile(
                            context, "${BuildConfig.APPLICATION_ID}.fileprovider", file
                        ),
                        MimeUtils.resolveMimeType(file)
                    )
                } catch (e: IllegalArgumentException) {
                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                    return
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
        }
        context.startActivitySafe(intent)
    }

    fun open(context: Context, path: String, flags: Int) {
        val isDir = flags and 0x40000000 != 0
        open(context, path, isDir)
    }
}
