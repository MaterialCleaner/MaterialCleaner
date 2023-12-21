package me.gm.cleaner.home.scanner.service

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.Process
import android.os.RemoteException
import androidx.annotation.CallSuper
import androidx.core.os.bundleOf
import com.topjohnwu.superuser.ipc.RootService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.gm.cleaner.home.HomeConstants
import me.gm.cleaner.home.TrashModel
import me.gm.cleaner.home.scanner.ScannerFileVisitor
import me.gm.cleaner.home.scanner.ScannerPredicate
import me.gm.cleaner.nio.file.walkPathTreeProgressed
import me.gm.cleaner.util.FileUtils
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.Path
import kotlin.io.path.pathString

abstract class BaseScannerService : RootService(), Handler.Callback, ScannerPredicate {
    private lateinit var remoteMessenger: Messenger
    private var isShowLength: Boolean = true
    private var isScanSystemApp: Boolean = false
    private var remainingCapability: AtomicInteger = AtomicInteger(50000)

    private fun isMaximumReached(): Boolean = remainingCapability.get() < 0

    override fun handleMessage(msg: Message): Boolean {
        when (msg.what) {
            MSG_START_SCAN -> {
                remoteMessenger = msg.replyTo
                isShowLength = msg.arg1 and (1 shl 0) != 0
                isScanSystemApp = msg.arg1 and (1 shl 1) != 0
                remainingCapability.set(msg.arg2)
                val noScanPathsMap = msg.data.getStringArray(HomeConstants.NO_SCAN_PATHS)
                    ?.groupBy { FileUtils.extractPathOwnerPackageName(it) }
                    ?: emptyMap()

                val pis = packageManager.getInstalledPackages(0).filter {
                    isScanSystemApp || it.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0
                }
                MainScope().launch {
                    withContext(Dispatchers.IO) {
                        for (i in pis.indices) {
                            if (isMaximumReached()) {
                                break
                            }
                            val pi = pis[i]
                            val reply = Message.obtain().apply {
                                what = MSG_ON_SCAN_PACKAGE
                                arg1 /* progress */ = 100 * (i + 1) / pis.size
                                obj = pi
                            }
                            try {
                                remoteMessenger.send(reply)
                            } catch (e: RemoteException) {
                                e.printStackTrace()
                            }
                            val packageName = pi.packageName
                            val noScanPaths = (noScanPathsMap[packageName] ?: emptyList())
                                .toMutableList()
                            onScan(
                                FileUtils.buildExternalStorageAppDataDirs(packageName).toPath(),
                                noScanPaths, packageName
                            )
                            if (isMaximumReached()) {
                                break
                            }
                            if (!pi.applicationInfo.dataDir.contains(packageName)) {
                                noScanPaths.addAll(noScanPathsMap[null] ?: emptyList())
                            }
                            onScan(Path(pi.applicationInfo.dataDir), noScanPaths, packageName)
                        }
                    }
                    try {
                        val reply = Message.obtain().apply {
                            what = if (isMaximumReached()) {
                                MSG_SCAN_MAXIMUM_REACHED
                            } else {
                                MSG_SCAN_FINISH
                            }
                        }
                        remoteMessenger.send(reply)
                    } catch (e: RemoteException) {
                        e.printStackTrace()
                    }
                }
            }
        }
        return false
    }

    protected open fun onScan(
        scanDir: Path, noScanPaths: MutableList<String>, packageName: String
    ) {
        if (scanDir.startsWith(dataDir.parent!!)) {
            noScanPaths += scanDir.resolve("app_webview").pathString
        }

        scanDir.walkPathTreeProgressed(visitor = object :
            ScannerFileVisitor(noScanPaths.toSet(), this) {
            override fun onTrashFound(trashBuilder: TrashModel.Builder) {
                this@BaseScannerService.onTrashFound(trashBuilder)
            }
        })
    }

    override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): Boolean = false
    override fun visitFile(file: Path, attrs: BasicFileAttributes): Boolean = false
    override fun postVisitDirectory(dir: Path): Boolean = false

    @CallSuper
    protected open fun onTrashFound(trashBuilder: TrashModel.Builder) {
        if (remainingCapability.decrementAndGet() < 0) {
            return
        }
        if (isShowLength) trashBuilder.length = FileUtils.fileTreeSize(Path(trashBuilder.path))
        val reply = Message.obtain().apply {
            what = MSG_ON_TRASH_FOUND
            data = bundleOf(HomeConstants.TRASHES to trashBuilder.build())
        }
        try {
            remoteMessenger.send(reply)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    override fun onRebind(intent: Intent) {
        // This callback will be called when we are reusing a previously started root process
    }

    override fun onBind(intent: Intent): IBinder {
        val h = Handler(Looper.getMainLooper(), this)
        val m = Messenger(h)
        return m.binder
    }

    companion object {
        const val MSG_START_SCAN = 0
        const val MSG_ON_SCAN_PACKAGE = 1
        const val MSG_ON_TRASH_FOUND = 2
        const val MSG_SCAN_FINISH = 3
        const val MSG_SCAN_MAXIMUM_REACHED = 4

        init {
            // Only load the library when this class is loaded in a root process.
            // The classloader will load this class (and call this static block) in the non-root
            // process because we accessed it when constructing the Intent to send.
            // Add this check so we don't unnecessarily load native code that'll never be used.
            if (Process.myUid() == 0) {
                System.loadLibrary(String(charArrayOf('c', 'l', 'e', 'a', 'n', 'e', 'r')))
            }
        }
    }
}
