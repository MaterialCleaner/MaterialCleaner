package me.gm.cleaner.client

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageInfo
import android.os.Bundle
import me.gm.cleaner.R
import me.gm.cleaner.dao.AppLabelCache
import me.gm.cleaner.dao.PurchaseVerification
import me.gm.cleaner.util.getParcelableExtraCompat

class ServerReceiver : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // binder
        val extra = intent.getBundleExtra(Intent.EXTRA_RESTRICTIONS_BUNDLE)
        if (extra != null) {
            val binder = extra.getBinder(BinderProvider.EXTRA_BINDER)!!
            CleanerClient.onBinderReceived(binder)
            intent.removeExtra(Intent.EXTRA_RESTRICTIONS_BUNDLE)
        }
        // dispatch by action
        when (intent.action) {
            NotificationService.ACTION_REDIRECTED_TO_INTERNAL,
            NotificationService.ACTION_MEDIA_NOT_FOUND -> {
                val packageInfo =
                    intent.getParcelableExtraCompat<PackageInfo>(Intent.EXTRA_PACKAGE_NAME)!!
                val label = AppLabelCache.getPackageLabel(packageInfo)
                val mountedPath = intent.getStringExtra(Intent.EXTRA_TEXT)
                val prompt = when (intent.action) {
                    NotificationService.ACTION_REDIRECTED_TO_INTERNAL ->
                        when (intent.type!!.toInt()) {
                            TYPE_INSERT -> getString(
                                R.string.prompt_redirect_to_internal_not_allowed, label, mountedPath
                            )

                            else -> getString(
                                R.string.prompt_redirected_to_internal, label, mountedPath
                            )
                        }

                    NotificationService.ACTION_MEDIA_NOT_FOUND ->
                        when (intent.type) {
                            Intent.EXTRA_SUBJECT -> getString(
                                R.string.prompt_media_not_found_aggressively, label, mountedPath
                            )

                            else -> getString(
                                R.string.prompt_media_not_found, label, mountedPath
                            )
                        }

                    else -> return
                }
                intent.putExtra(Intent.EXTRA_TITLE, prompt)

                if (PurchaseVerification.isStrictPro) {
                    startActivity(
                        Intent(this, PromptActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                            .putExtra(Intent.EXTRA_INTENT, intent)
                    )
                } else {
                    startService(
                        Intent(this, NotificationService::class.java)
                            .putExtra(Intent.EXTRA_INTENT, intent)
                    )
                }
            }

            NotificationService.ACTION_LOGCAT_SHUTDOWN,
            NotificationService.ACTION_MY_PACKAGE_REPLACED,
            NotificationService.ACTION_PACKAGE_ADDED -> startService(
                Intent(this, NotificationService::class.java)
                    .putExtra(Intent.EXTRA_INTENT, intent)
            )
        }
        finish()
    }

    companion object {
        const val TYPE_BROADCAST: Int = 0
        const val TYPE_CONNECTION: Int = 1
        const val TYPE_INSERT: Int = 2
    }
}
