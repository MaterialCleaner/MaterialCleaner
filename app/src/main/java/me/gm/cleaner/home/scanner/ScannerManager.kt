package me.gm.cleaner.home.scanner

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
import com.topjohnwu.superuser.internal.Utils
import me.gm.cleaner.R
import me.gm.cleaner.client.CleanerClient
import me.gm.cleaner.dao.RootPreferences
import me.gm.cleaner.home.StaticScanner
import me.gm.cleaner.home.scanner.service.CacheScannerService
import me.gm.cleaner.home.scanner.service.LogScannerService
import me.gm.cleaner.home.scanner.service.TempScannerService
import me.gm.cleaner.home.scanner.sh.ApkScanner
import me.gm.cleaner.home.scanner.sh.ApkViewModel
import me.gm.cleaner.home.scanner.sh.BaseShScanner
import me.gm.cleaner.home.scanner.sh.EmptyDirScanner
import me.gm.cleaner.home.scanner.sh.EmptyDirViewModel
import me.gm.cleaner.home.scanner.sh.NonpublicDirScanner
import me.gm.cleaner.home.scanner.sh.NonpublicDirViewModel
import me.gm.cleaner.home.scanner.sh.SmallScanner
import me.gm.cleaner.home.scanner.sh.SmallViewModel
import me.gm.cleaner.home.scanner.su.CacheScanner
import me.gm.cleaner.home.scanner.su.CacheViewModel
import me.gm.cleaner.home.scanner.su.LogScanner
import me.gm.cleaner.home.scanner.su.LogViewModel
import me.gm.cleaner.home.scanner.su.TempScanner
import me.gm.cleaner.home.scanner.su.TempViewModel
import me.gm.cleaner.nio.file.WatchDir
import me.gm.cleaner.util.FileUtils
import java.lang.ref.WeakReference
import java.nio.file.Path
import java.nio.file.WatchEvent
import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.io.path.pathString

object ScannerManager {
    // MONITOR
    private val changedPaths: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())
    private val lock: Any = Object()
    private var lastMountedDirs: Set<String> = emptySet()
    private var dirWatcher: WatchDir? = null
    private var observerThread: Thread? = null

    data class FileChanges(
        val changedPaths: List<String> = emptyList()
    )

    val isFileChanged: Boolean
        get() = synchronized(lock) {
            val mountedDirs = CleanerClient.mountedDirs.toSet()
            if (lastMountedDirs != mountedDirs) {
                changedPaths += lastMountedDirs + mountedDirs -
                        lastMountedDirs.intersect(mountedDirs)
                // externalStorageDir cannot be correctly handled by some scanners.
                changedPaths -= FileUtils.externalStorageDir.path
                lastMountedDirs = mountedDirs
            }
            changedPaths.isNotEmpty()
        }

    var fileChanges: FileChanges
        get() = synchronized(lock) {
            FileChanges(changedPaths.toList())
        }
        set(value) = synchronized(lock) {
            changedPaths.addAll(value.changedPaths)
        }

    private fun clearFileChanges() {
        synchronized(lock) {
            changedPaths.clear()
        }
    }

    fun consumeFileChanges(): FileChanges =
        try {
            fileChanges
        } finally {
            clearFileChanges()
        }

    @MainThread
    fun startMonitor() {
        if (dirWatcher != null && observerThread?.isAlive == true) {
            return
        }
        stopMonitor()
        observerThread = Thread {
            val exceptionalPaths = FileUtils.defaultExternalNoScan
                .asSequence().map { it.toPath() }.toSet()
            dirWatcher = object : WatchDir(FileUtils.externalStorageDir.toPath(), true) {

                override fun register(dir: Path): Boolean =
                    if (dir in exceptionalPaths) false else super.register(dir)

                override fun onEvent(kind: WatchEvent.Kind<*>, child: Path) {
                    synchronized(lock) {
                        val path = child.pathString
                        changedPaths.add(path)
                    }
                }
            }
            dirWatcher!!.processEvents()
        }
        observerThread?.start()
    }

    @MainThread
    fun stopMonitor() {
        observerThread?.interrupt()
        clearFileChanges()
        dirWatcher = null
        observerThread = null
    }

    // SCANNER
    val staticScanners: List<StaticScanner>
        @SuppressLint("RestrictedApi")
        get() = listOfNotNull(
            if (!Utils.isRootImpossible() || Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                StaticScanner(
                    title = R.string.cache,
                    icon = R.drawable.ic_outline_cache_24,
                    scannerClass = CacheScanner::class.java,
                    viewModelClass = CacheViewModel::class.java,
                    serviceClass = CacheScannerService::class.java
                )
            } else {
                null
            },
            if (!Utils.isRootImpossible()) {
                StaticScanner(
                    title = R.string.log,
                    icon = R.drawable.ic_outline_log_24,
                    scannerClass = LogScanner::class.java,
                    viewModelClass = LogViewModel::class.java,
                    serviceClass = LogScannerService::class.java
                )
            } else {
                null
            },
            if (!Utils.isRootImpossible()) {
                StaticScanner(
                    title = R.string.temp,
                    icon = R.drawable.ic_outline_temp_24,
                    scannerClass = TempScanner::class.java,
                    viewModelClass = TempViewModel::class.java,
                    serviceClass = TempScannerService::class.java
                )
            } else {
                null
            },
            if (RootPreferences.isShowNonpublic) {
                StaticScanner(
                    title = R.string.nonpublic,
                    icon = R.drawable.ic_outline_standard_folder_24,
                    scannerClass = NonpublicDirScanner::class.java,
                    viewModelClass = NonpublicDirViewModel::class.java,
                    serviceClass = null
                )
            } else {
                null
            },
            StaticScanner(
                title = R.string.apk,
                icon = R.drawable.ic_outline_android_24,
                scannerClass = ApkScanner::class.java,
                viewModelClass = ApkViewModel::class.java,
                serviceClass = null
            ),
            StaticScanner(
                title = R.string.small,
                icon = R.drawable.ic_outline_small_24,
                scannerClass = SmallScanner::class.java,
                viewModelClass = SmallViewModel::class.java,
                serviceClass = null
            ),
            StaticScanner(
                title = R.string.empty,
                icon = R.drawable.ic_outline_empty_folder_24,
                scannerClass = EmptyDirScanner::class.java,
                viewModelClass = EmptyDirViewModel::class.java,
                serviceClass = null
            )
        )

    /**
     * Keep the reference of all running scanners.
     */
    private val _scanners: CopyOnWriteArrayList<BaseScanner> = CopyOnWriteArrayList<BaseScanner>()
    val scanners: List<BaseScanner>
        get() = _scanners
    lateinit var activityRef: WeakReference<AppCompatActivity>
    var lastRegisteredScannerPosition: Int = -1

    fun registerScanner(scanner: BaseScanner) {
        lastRegisteredScannerPosition =
            staticScanners.indexOfFirst { it.scannerClass == scanner.javaClass }
        try {
            _scanners.removeIf { it.javaClass == scanner.javaClass }
        } catch (e: UnsupportedOperationException) {
            // Try to support android N.
            _scanners.removeAll { it.javaClass == scanner.javaClass }
        }
        _scanners.add(scanner)
    }

    fun unregisterScanner(victim: BaseScanner) {
        try {
            _scanners.removeIf { it.javaClass == victim.javaClass }
        } catch (e: UnsupportedOperationException) {
            // Try to support android N.
            _scanners.removeAll { it.javaClass == victim.javaClass }
        }
    }

    fun unregisterAll() {
        _scanners.forEach { it.onDestroy() }
    }

    private val shScannersSequence: Sequence<BaseShScanner>
        get() = _scanners.asSequence().filter { it.info.isShScanner }.map { it as BaseShScanner }

    val shScanners: List<BaseShScanner>
        get() = shScannersSequence.toList()

    val runningShScanners: List<BaseShScanner>
        get() = shScannersSequence.filter { it.viewModel.isRunning }.toList()

    val inheritanceScanners: List<BaseScanner>
        get() = _scanners.filter { it.viewModel.isAcceptInheritance }

    fun shScannersRescan() {
        val fileChanges = consumeFileChanges()
        if (RootPreferences.isShowNonpublic) {
            NonpublicDirScanner().acceptMonitor(fileChanges).start()
        }
        ApkScanner().acceptMonitor(fileChanges).start()
        SmallScanner().acceptMonitor(fileChanges).start()
        EmptyDirScanner().acceptMonitor(fileChanges).start()
    }
}
