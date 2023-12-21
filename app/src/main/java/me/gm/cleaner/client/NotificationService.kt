package me.gm.cleaner.client

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.IBinder
import android.os.Process
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import me.gm.cleaner.BuildConfig.APPLICATION_ID
import me.gm.cleaner.R
import me.gm.cleaner.client.ui.storageredirect.MountWizard
import me.gm.cleaner.dao.AppLabelCache
import me.gm.cleaner.dao.RootPreferences
import me.gm.cleaner.dao.ServiceMoreOptionsPreferences
import me.gm.cleaner.dao.ServicePreferences
import me.gm.cleaner.net.OnlineAppTypeMarks
import me.gm.cleaner.starter.Starter
import me.gm.cleaner.util.FileUtils.toUserId
import me.gm.cleaner.util.PermissionUtils.notifySafe
import me.gm.cleaner.util.getParcelableExtraCompat

class NotificationService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    private fun computeHashCode(packageName: String, action: String): Int =
        31 * packageName.hashCode() + action.hashCode()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent ?: return super.onStartCommand(intent, flags, startId)
        if (intent.hasExtra(Intent.EXTRA_INTENT)) {
            /** from [ServerReceiver] */
            onReceive(this, intent.getParcelableExtraCompat(Intent.EXTRA_INTENT)!!)
        } else {
            /** from [android.app.Notification.actions] */
            val name = intent.getStringExtra(NAME)
            when (name) {
                NOTIFICATION_CHANNEL_SRPROMPT -> {
                    val packageInfo =
                        intent.getParcelableExtraCompat<PackageInfo>(Intent.EXTRA_PACKAGE_NAME)!!
                    NotificationManagerCompat.from(this).cancel(
                        computeHashCode(packageInfo.packageName, name)
                    )
                    MainScope().launch(Dispatchers.IO) {
                        val service = CleanerClient.service!!
                        val denyList = service.denyList + packageInfo.packageName
                        service.setDenyList(denyList.toTypedArray())
                    }
                }

                NOTIFICATION_CHANNEL_SHUTDOWN, NOTIFICATION_CHANNEL_UPDATED -> {
                    NotificationManagerCompat.from(this).cancel(name.hashCode())
                    MainScope().launch(Dispatchers.IO) {
                        if (Shell.getShell().isRoot) {
                            try {
                                Starter.writeDataFiles(this@NotificationService)
                                Shell.cmd(Starter.command).exec()
                            } catch (e: Throwable) {
                                e.printStackTrace()
                            }
                        }
                    }
                }

                NOTIFICATION_CHANNEL_ADDED_UNDO -> {
                    val packageInfo =
                        intent.getParcelableExtraCompat<PackageInfo>(Intent.EXTRA_PACKAGE_NAME)!!
                    NotificationManagerCompat.from(this).cancel(
                        computeHashCode(packageInfo.packageName, NOTIFICATION_CHANNEL_ADDED)
                    )
                    MainScope().launch(Dispatchers.IO) {
                        ServicePreferences.removeStorageRedirect(
                            getSharedProcessPackages(packageInfo).map { it.packageName }
                        )
                        if (CleanerClient.pingBinder()) {
                            CleanerClient.service!!.notifySrChanged()
                        }
                    }
                }

                NOTIFICATION_CHANNEL_ADDED_HIDE -> {
                    val packageInfo =
                        intent.getParcelableExtraCompat<PackageInfo>(Intent.EXTRA_PACKAGE_NAME)!!
                    NotificationManagerCompat.from(this).cancel(
                        computeHashCode(packageInfo.packageName, NOTIFICATION_CHANNEL_ADDED)
                    )
                    MainScope().launch(Dispatchers.IO) {
                        val service = CleanerClient.service!!
                        val denyList = service.denyList + packageInfo.packageName
                        service.setDenyList(denyList.toTypedArray())
                    }
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    fun onReceive(context: Context, intent: Intent) {
        if (Process.myUid().toUserId() > 0) return
        when (intent.action) {
            ACTION_REDIRECTED_TO_INTERNAL,
            ACTION_MEDIA_NOT_FOUND -> {
                val prompt = intent.getStringExtra(Intent.EXTRA_TITLE)!!
                val packageInfo =
                    intent.getParcelableExtraCompat<PackageInfo>(Intent.EXTRA_PACKAGE_NAME)!!
                buildPromptNotification(context, prompt, packageInfo)
            }

            ACTION_LOGCAT_SHUTDOWN -> buildLogcatShutdownNotification(context)
            ACTION_MY_PACKAGE_REPLACED -> {
                buildSelfUpdatedNotification(context)
                if (RootPreferences.isStartOnBoot) {
                    MainScope().launch(Dispatchers.IO) {
                        try {
                            Starter.writeSourceDir(context)
                        } catch (e: Throwable) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            ACTION_PACKAGE_ADDED -> if (ServiceMoreOptionsPreferences.applyTemplateOnPackageAdded) {
                val packageInfo =
                    intent.getParcelableExtraCompat<PackageInfo>(Intent.EXTRA_PACKAGE_NAME)!!
                if (packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0) {
                    return
                }
                val wizard = MountWizard(packageInfo)
                val answers = ServiceMoreOptionsPreferences.editMountRulesTemplate
                val rulesByTemplate = wizard.createRules(answers)
                ServicePreferences.putStorageRedirect(
                    rulesByTemplate, getSharedProcessPackages(packageInfo).map { it.packageName }
                )
                CleanerClient.service!!.notifySrChanged()
                buildPackageAddedNotification(context, packageInfo)
                MainScope().launch {
                    OnlineAppTypeMarks.fetch(context, packageInfo).onSuccess { appTypeMarks ->
                        appTypeMarks ?: return@onSuccess
                        wizard.answerBasedOnRecord(answers, emptyList(), appTypeMarks)

                        if (rulesByTemplate ==
                            ServicePreferences.getPackageSrZipped(packageInfo.packageName)
                        ) {
                            ServicePreferences.putStorageRedirect(
                                wizard.createRules(answers),
                                getSharedProcessPackages(packageInfo).map { it.packageName }
                            )
                            CleanerClient.service!!.notifySrChanged()
                        }
                    }
                }
            }
        }
    }

    private fun buildPromptNotification(
        context: Context, prompt: String, packageInfo: PackageInfo
    ) {
        val id = computeHashCode(packageInfo.packageName, NOTIFICATION_CHANNEL_SRPROMPT)
        val intent = Intent(context, NotificationService::class.java).apply {
            putExtra(NAME, NOTIFICATION_CHANNEL_SRPROMPT)
            putExtra(Intent.EXTRA_PACKAGE_NAME, packageInfo)
        }
        val pendingIntent = PendingIntent.getService(
            context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val style = NotificationCompat.BigTextStyle().bigText(prompt)
        val notification = NotificationCompat
            .Builder(context, NOTIFICATION_CHANNEL_SRPROMPT)
            .setContentText(prompt)
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.outline_error_outline_24)
            .setColor(context.getColor(R.color.color_primary))
            .setStyle(style)
            .addAction(0, context.getString(R.string.no_prompt_for_this_app), pendingIntent)
            .build()
        NotificationManagerCompat.from(context).run {
            val channel = NotificationChannelCompat
                .Builder(NOTIFICATION_CHANNEL_SRPROMPT, NotificationManager.IMPORTANCE_MAX)
                .setName(context.getString(R.string.storage_redirect_prompt))
                .setSound(null, null)
                .build()
            createNotificationChannel(channel)
            notifySafe(context, id, notification)
        }
    }

    private fun buildLogcatShutdownNotification(context: Context) {
        val intent = Intent(context, NotificationService::class.java).apply {
            putExtra(NAME, NOTIFICATION_CHANNEL_SHUTDOWN)
        }
        val pendingIntent = PendingIntent.getService(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val contentIntent = PendingIntent.getActivity(
            context, 0,
            packageManager.getLaunchIntentForPackage(APPLICATION_ID),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat
            .Builder(context, NOTIFICATION_CHANNEL_SHUTDOWN)
            .setContentTitle(context.getString(R.string.service_exception_logcat_shutdown))
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.ic_outline_close_24)
            .setColor(context.getColor(R.color.color_primary))
            .setContentIntent(contentIntent)
            .addAction(0, context.getString(R.string.confirm_exit_restart), pendingIntent)
            .build()
        NotificationManagerCompat.from(context).run {
            val channel = NotificationChannelCompat
                .Builder(NOTIFICATION_CHANNEL_SHUTDOWN, NotificationManager.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.service_exception_logcat_shutdown))
                .build()
            createNotificationChannel(channel)
            notifySafe(context, NOTIFICATION_CHANNEL_SHUTDOWN.hashCode(), notification)
        }
    }

    private fun buildSelfUpdatedNotification(context: Context) {
        val intent = Intent(context, NotificationService::class.java).apply {
            putExtra(NAME, NOTIFICATION_CHANNEL_UPDATED)
        }
        val pendingIntent = PendingIntent.getService(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat
            .Builder(context, NOTIFICATION_CHANNEL_UPDATED)
            .setContentTitle(context.getString(R.string.service_need_upgrade))
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.ic_outline_update_24)
            .setColor(context.getColor(R.color.color_primary))
            .addAction(0, context.getString(R.string.confirm_exit_restart), pendingIntent)
            .build()
        NotificationManagerCompat.from(context).run {
            val channel = NotificationChannelCompat
                .Builder(NOTIFICATION_CHANNEL_UPDATED, NotificationManager.IMPORTANCE_MAX)
                .setName(context.getString(R.string.service_need_upgrade))
                .setSound(null, null)
                .build()
            createNotificationChannel(channel)
            notifySafe(context, NOTIFICATION_CHANNEL_UPDATED.hashCode(), notification)
        }
    }

    private fun buildPackageAddedNotification(context: Context, packageInfo: PackageInfo) {
        val undoIntent = Intent(context, NotificationService::class.java).apply {
            putExtra(NAME, NOTIFICATION_CHANNEL_ADDED_UNDO)
            putExtra(Intent.EXTRA_PACKAGE_NAME, packageInfo)
        }
        val undoPendingIntent = PendingIntent.getService(
            context, computeHashCode(packageInfo.packageName, NOTIFICATION_CHANNEL_ADDED_UNDO),
            undoIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val hideIntent = Intent(context, NotificationService::class.java).apply {
            putExtra(NAME, NOTIFICATION_CHANNEL_ADDED_HIDE)
            putExtra(Intent.EXTRA_PACKAGE_NAME, packageInfo)
        }
        val hidePendingIntent = PendingIntent.getService(
            context, computeHashCode(packageInfo.packageName, NOTIFICATION_CHANNEL_ADDED_HIDE),
            hideIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat
            .Builder(context, NOTIFICATION_CHANNEL_ADDED)
            .setContentTitle(
                context.getString(
                    R.string.applied_template_to_package,
                    AppLabelCache.getPackageLabel(packageInfo)
                )
            )
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.ic_outline_autorenew_24)
            .setColor(context.getColor(R.color.color_primary))
            .addAction(0, context.getString(R.string.undo), undoPendingIntent)
            .addAction(0, context.getString(R.string.menu_add_to_hide_list), hidePendingIntent)
            .build()
        NotificationManagerCompat.from(context).run {
            val channel = NotificationChannelCompat
                .Builder(NOTIFICATION_CHANNEL_ADDED, NotificationManager.IMPORTANCE_NONE)
                .setName(context.getString(R.string.apply_template_on_package_added_title))
                .setSound(null, null)
                .setShowBadge(false)
                .build()
            createNotificationChannel(channel)
            notifySafe(
                context,
                computeHashCode(packageInfo.packageName, NOTIFICATION_CHANNEL_ADDED),
                notification
            )
        }
    }

    companion object {
        const val ACTION_REDIRECTED_TO_INTERNAL: String =
            "$APPLICATION_ID.intent.action.ACTION_REDIRECTED_TO_INTERNAL"
        const val ACTION_MEDIA_NOT_FOUND: String =
            "$APPLICATION_ID.intent.action.ACTION_MEDIA_NOT_FOUND"
        private const val NOTIFICATION_CHANNEL_SRPROMPT: String =
            "storage_redirect_prompt"

        const val ACTION_LOGCAT_SHUTDOWN: String =
            "$APPLICATION_ID.intent.action.ACTION_LOGCAT_SHUTDOWN"
        private const val NOTIFICATION_CHANNEL_SHUTDOWN: String = "shutdown"

        const val ACTION_MY_PACKAGE_REPLACED: String =
            "$APPLICATION_ID.intent.action.ACTION_MY_PACKAGE_REPLACED"
        private const val NOTIFICATION_CHANNEL_UPDATED: String = "self_updated"

        const val ACTION_PACKAGE_ADDED: String =
            "$APPLICATION_ID.intent.action.ACTION_PACKAGE_ADDED"
        private const val NOTIFICATION_CHANNEL_ADDED: String = "package_added"
        private const val NOTIFICATION_CHANNEL_ADDED_UNDO: String = "package_added_undo"
        private const val NOTIFICATION_CHANNEL_ADDED_HIDE: String = "package_added_hide"

        private const val NAME: String = "action"
    }
}
