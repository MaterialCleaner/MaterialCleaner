package me.gm.cleaner.client

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import me.gm.cleaner.dao.SecurityHelper

class BinderProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        SecurityHelper.init(context!!)
        return true
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (extras == null) return null
        val reply = Bundle()
        if (METHOD_SEND_BINDER == method) {
            handleSendBinder(extras)
        }
        return reply
    }

    private fun handleSendBinder(extras: Bundle) {
        if (CleanerClient.pingBinder()) return
        val binder = extras.getBinder(EXTRA_BINDER) ?: return
        CleanerClient.onBinderReceived(binder)
    }

    // no other provider methods
    override fun query(
        uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0

    override fun update(
        uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?
    ): Int = 0

    companion object {
        const val METHOD_SEND_BINDER: String = "sendBinder"
        const val EXTRA_BINDER: String = "me.gm.cleaner.intent.extra.BINDER"
    }
}
