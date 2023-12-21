package me.gm.cleaner.server

object ServerConstants {
    const val APPLICATION_ID: String = "me.gm.cleaner"
    const val RECEIVER_ACTIVITY_NAME: String = "$APPLICATION_ID.client.ServerReceiver"
    const val ACTION_REDIRECTED_TO_INTERNAL: String =
        "$APPLICATION_ID.intent.action.ACTION_REDIRECTED_TO_INTERNAL"
    const val ACTION_MEDIA_NOT_FOUND: String =
        "$APPLICATION_ID.intent.action.ACTION_MEDIA_NOT_FOUND"
    const val ACTION_LOGCAT_SHUTDOWN: String =
        "$APPLICATION_ID.intent.action.ACTION_LOGCAT_SHUTDOWN"
    const val ACTION_MY_PACKAGE_REPLACED: String =
        "$APPLICATION_ID.intent.action.ACTION_MY_PACKAGE_REPLACED"
    const val ACTION_PACKAGE_ADDED: String = "$APPLICATION_ID.intent.action.ACTION_PACKAGE_ADDED"
    const val EXTRA_BINDER: String = "$APPLICATION_ID.intent.extra.BINDER"
    const val MPM_APPLICATION_ID: String = "me.gm.cleaner.plugin"
    const val MP_APPLICATION_ID: String = "com.android.providers.media.module"
}
