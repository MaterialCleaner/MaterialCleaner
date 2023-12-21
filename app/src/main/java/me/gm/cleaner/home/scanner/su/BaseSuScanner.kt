package me.gm.cleaner.home.scanner.su

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageInfo
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import androidx.core.os.bundleOf
import androidx.lifecycle.viewModelScope
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.gm.cleaner.R
import me.gm.cleaner.app.InfoDialog
import me.gm.cleaner.dao.RootPreferences
import me.gm.cleaner.home.HomeConstants
import me.gm.cleaner.home.StaticScanner
import me.gm.cleaner.home.TrashModel
import me.gm.cleaner.home.scanner.BaseScanner
import me.gm.cleaner.home.scanner.service.BaseScannerService
import me.gm.cleaner.util.getParcelableCompat
import java.util.concurrent.Executors

abstract class BaseSuScanner protected constructor(info: StaticScanner) : BaseScanner(info),
    Handler.Callback {
    private var rootConnection: RootConnection? = null
    private var remoteMessenger: Messenger? = null
    private val dispatcher: ExecutorCoroutineDispatcher =
        Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private var packageInfo: PackageInfo? = null

    override fun handleMessage(msg: Message): Boolean {
        // Note here is a little trick-check msg on the main thread.
        when (msg.what) {
            BaseScannerService.MSG_ON_SCAN_PACKAGE -> viewModel.viewModelScope.launch {
                viewModel.progress = msg.arg1
                val pi = msg.obj as PackageInfo
                withContext(dispatcher) {
                    packageInfo = pi
                }
            }

            BaseScannerService.MSG_ON_TRASH_FOUND -> viewModel.viewModelScope.launch {
                val data = msg.data
                withContext(dispatcher) {
                    val trash = data.getParcelableCompat<TrashModel>(HomeConstants.TRASHES)
                        ?: return@withContext
                    val trashBuilder = TrashModel
                        .Builder(trash.path, trash.isDir, length = trash.length)
                        .setPackageInfo(packageInfo)
                    onTrashFound(trashBuilder)
                }
            }

            BaseScannerService.MSG_SCAN_FINISH,
            BaseScannerService.MSG_SCAN_MAXIMUM_REACHED -> viewModel.viewModelScope.launch {
                val reason = msg.what
                withContext(dispatcher) {
                    onFinish(reason)
                }
            }
        }
        return false
    }

    override fun onCheckPermission(): Boolean = Shell.getShell().isRoot

    override fun onRequestPermission() {
        InfoDialog
            .newInstance(activity.getString(R.string.no_root_access))
            .show(activity.supportFragmentManager, null)
    }

    public override suspend fun onScan() {
        if (viewModel.isAcceptInheritance) {
            onFinish(BaseScannerService.MSG_SCAN_FINISH)
        } else {
            viewModel.removeTrashIf { true }
            withContext(Dispatchers.Main) {
                rootConnection = RootConnection()
                RootService.bind(
                    Intent(activity.applicationContext, info.serviceClass), rootConnection!!
                )
            }
        }
    }

    private inner class RootConnection : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            remoteMessenger = Messenger(service)
            val message = Message.obtain(null, BaseScannerService.MSG_START_SCAN).apply {
                if (RootPreferences.isShowLength) arg1 = arg1 or (1 shl 0)
                if (RootPreferences.isScanSystemApp) arg1 = arg1 or (1 shl 1)
                arg2 = RootPreferences.maximize
                data = bundleOf(
                    HomeConstants.NO_SCAN_PATHS to RootPreferences.noScan.toTypedArray()
                )
            }
            message.replyTo = Messenger(Handler(Looper.getMainLooper(), this@BaseSuScanner))
            try {
                remoteMessenger?.send(message)
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            remoteMessenger = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (rootConnection != null) {
            RootService.unbind(rootConnection!!)
            rootConnection = null
        }
        dispatcher.close()
    }
}
