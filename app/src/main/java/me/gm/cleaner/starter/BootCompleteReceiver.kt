package me.gm.cleaner.starter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Process
import com.topjohnwu.superuser.Shell
import me.gm.cleaner.client.CleanerClient
import me.gm.cleaner.dao.RootPreferences
import me.gm.cleaner.util.FileUtils.toUserId

class BootCompleteReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Process.myUid().toUserId() > 0) return
        if (RootPreferences.isStartOnBoot && !CleanerClient.pingBinder()) {
            if (Shell.getShell().isRoot) {
                runCatching {
                    if (RootPreferences.isStartOnBoot) {
                        Starter.writeSourceDir(context)
                    }
                    Starter.writeDataFiles(context)
                    Shell.cmd(Starter.command).exec()
                }
            }
        }
    }
}
