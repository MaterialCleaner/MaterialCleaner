package me.gm.cleaner.home.scanner

import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.gm.cleaner.client.CleanerClient
import me.gm.cleaner.dao.RootPreferences
import me.gm.cleaner.home.StaticScanner
import me.gm.cleaner.home.TrashModel
import me.gm.cleaner.home.scanner.service.BaseScannerService
import me.gm.cleaner.util.FileUtils
import java.util.function.Consumer
import java.util.regex.Pattern

abstract class BaseScanner protected constructor(val info: StaticScanner) {
    init {
        require(info.scannerClass == javaClass)
    }

    private val pattern: Pattern? =
        if (RootPreferences.noTick.isNotEmpty()) Pattern.compile(RootPreferences.noTick) else null
    private val String.isCheck: Boolean
        get() = pattern == null || !pattern.matcher(this).find()

    private val mountedDirs: List<String> = CleanerClient.mountedDirs
    protected open val String.isInUse: Boolean
        get() = mountedDirs.any { FileUtils.startsWith(this, it) }

    protected val activity: AppCompatActivity
        get() = ScannerManager.activityRef.get()!!
    val viewModel: ScannerViewModel = ViewModelProvider(activity)[info.viewModelClass!!]
    private var finishListener: Consumer<Int>? = null
    fun setOnFinishListener(l: Consumer<Int>): BaseScanner {
        finishListener = l
        return this
    }

    fun start() {
        viewModel.viewModelScope.launch {
            ScannerManager.registerScanner(this@BaseScanner)
            if (viewModel.isRunning) {
                return@launch
            }
            if (!onCheckPermission()) {
                onRequestPermission()
                return@launch
            }
            viewModel.progress = 0
            withContext(Dispatchers.IO) {
                runCatching {
                    onScan()
                }
            }
            if (info.isShScanner) {
                onFinish(BaseScannerService.MSG_SCAN_FINISH)
            }
        }
    }

    protected abstract fun onCheckPermission(): Boolean
    protected abstract fun onRequestPermission()
    protected abstract suspend fun onScan()

    @CallSuper
    protected open fun onTrashFound(trashBuilder: TrashModel.Builder) {
        trashBuilder.isChecked = trashBuilder.isChecked && trashBuilder.path.isCheck
        trashBuilder.isInUse = trashBuilder.path.isInUse
        viewModel.addTrash(trashBuilder.build())
    }

    protected open fun onFinish(reason: Int) {
        viewModel.isAcceptInheritance = true
        viewModel.progress = -1
        finishListener?.accept(reason)
    }

    @CallSuper
    open fun onDestroy() {
        viewModel.isAcceptInheritance = false
        ScannerManager.unregisterScanner(this)
    }
}
