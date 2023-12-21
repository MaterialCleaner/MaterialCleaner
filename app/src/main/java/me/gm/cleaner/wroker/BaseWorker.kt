package me.gm.cleaner.wroker

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import me.gm.cleaner.R
import me.gm.cleaner.nio.RootWorkerService

abstract class BaseWorker(private val appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    protected abstract val workName: String
    protected abstract val notificationIcon: Int
    protected abstract val notificationChannelName: String
    protected abstract val notificationTitle: String
    private var progress: Int = 0

    final override suspend fun getForegroundInfo(): ForegroundInfo {
        val cancelIntent = WorkManager.getInstance(appContext).createCancelPendingIntent(id)
        val notification = NotificationCompat
            .Builder(appContext, workName)
            .setContentTitle(notificationTitle)
            .setSmallIcon(notificationIcon)
            .setColor(appContext.getColor(R.color.color_primary))
            .setSound(null)
            .setOngoing(true)
            .setProgress(100, progress, false)
            .addAction(0, appContext.getString(android.R.string.cancel), cancelIntent)
            .build()
        NotificationManagerCompat.from(appContext).run {
            val channel = NotificationChannelCompat
                .Builder(workName, NotificationManager.IMPORTANCE_MAX)
                .setName(notificationChannelName)
                .build()
            createNotificationChannel(channel)
        }
        return ForegroundInfo(id.hashCode(), notification)
    }

    private val limiter = RootWorkerService.CallbackRateLimiter()

    protected fun updateForeground(progress: Int) {
        this.progress = progress
        limiter.tryTriggerCallback {
            setForegroundAsync(foregroundInfoAsync.get())
        }
    }
}
