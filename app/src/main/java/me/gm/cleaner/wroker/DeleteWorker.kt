package me.gm.cleaner.wroker

import android.app.NotificationManager
import android.content.Context
import android.media.MediaScannerConnection
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.common.collect.ListMultimap
import com.google.common.collect.Multimaps
import me.gm.cleaner.R
import me.gm.cleaner.browser.SimpleProgressListener
import me.gm.cleaner.nio.RootWorkerService
import me.gm.cleaner.util.LogUtils
import me.gm.cleaner.util.PermissionUtils.notifySafe
import me.gm.cleaner.util.toList
import org.json.JSONArray
import java.io.InterruptedIOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.Path
import kotlin.io.path.isWritable
import kotlin.io.path.pathString
import kotlin.io.path.readText

class DeleteWorker(private val appContext: Context, workerParams: WorkerParameters) :
    BaseWorker(appContext, workerParams) {
    override val workName: String = WORK_NAME
    override val notificationIcon: Int = R.drawable.ic_outline_delete_24
    override val notificationChannelName: String = appContext.getString(R.string.delete)
    override val notificationTitle: String
        get() = appContext.getString(R.string.deleting, path)
    private var path: String = ""

    override suspend fun doWork(): Result = try {
        PROGRESS.clear()
        val inputPathsString = inputData.getStringArray(PATHS)
            ?: JSONArray(Path(inputData.getString(PATHS_FILE)!!).readText()).toList().toTypedArray()

        inputPathsString.forEachIndexed { index, inputPathString ->
            path = inputPathString
            val inputPath = Path(inputPathString)
            val workerService = if (inputPath.isWritable()) {
                RootWorkerService()
            } else {
                RootWorkerClient
                    .init(appContext)
                    .get(inputPath.pathString)
            }
            val listener = object : SimpleProgressListener() {
                override fun onProgress(progress: Float) {
                    if (isStopped) {
                        workerService.cancelWork(id.toString())
                    }
                    updateForeground((100 * (progress + index) / inputPathsString.size).toInt())
                }
            }
            workerService.delete(
                id.toString(),
                listener,
                inputPath.toUri().toASCIIString()
            )
            listener.throwIfHasException()
            setProgressAsync(workDataOf(DELETED_PATH to path)) // trigger observer
            PROGRESS.put(id, path)
        }

        MediaScannerConnection.scanFile(appContext, inputPathsString, null, null)
        Result.success()
    } catch (tr: Throwable) {
        if (tr is InterruptedIOException) {
            throw tr
        }

        val style = NotificationCompat.BigTextStyle().bigText(tr.stackTraceToString())
        val notification = NotificationCompat
            .Builder(appContext, workName)
            .setContentTitle(appContext.getString(R.string.failed_to_delete, path))
            .setContentText(tr.message)
            .setStyle(style)
            .setSmallIcon(notificationIcon)
            .setColor(appContext.getColor(R.color.color_primary))
            .build()
        NotificationManagerCompat.from(appContext).run {
            val channel = NotificationChannelCompat
                .Builder(workName, NotificationManager.IMPORTANCE_MAX)
                .setName(notificationChannelName)
                .build()
            createNotificationChannel(channel)
            notifySafe(appContext, workName.hashCode(), notification)
        }

        LogUtils.handleThrowable(tr)
        Result.failure()
    }

    companion object {
        const val WORK_NAME: String = "work_delete"
        const val PATHS: String = "paths"
        const val PATHS_FILE: String = "paths_file"
        const val DELETED_PATH: String = "deleted_path"
        val PROGRESS: ListMultimap<UUID, String> =
            Multimaps.newListMultimap(ConcurrentHashMap()) { mutableListOf() }

        fun createRequest(context: Context, vararg paths: String): OneTimeWorkRequest = try {
            OneTimeWorkRequestBuilder<DeleteWorker>()
                .setInputData(workDataOf(PATHS to paths))
                .build()
        } catch (e: IllegalStateException) {
            // Data cannot occupy more than 10240 bytes when serialized
            val id = UUID.randomUUID()
            val pathsFile = context.noBackupFilesDir.resolve(id.toString())
            pathsFile.writeText(JSONArray(paths).toString())
            OneTimeWorkRequestBuilder<DeleteWorker>()
                .setId(id)
                .setInputData(workDataOf(PATHS_FILE to pathsFile.path))
                .build()
        }
    }
}
