package me.gm.cleaner.home.ui

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import me.gm.cleaner.R
import me.gm.cleaner.dao.PurchaseVerification
import me.gm.cleaner.dao.RootPreferences
import me.gm.cleaner.home.scanner.ScannerManager
import me.gm.cleaner.starter.Starter
import me.gm.cleaner.util.FileUtils

class HomeViewModel : ViewModel() {
    private val _isShowStarterProgressFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isShowStarterProgressLiveData: LiveData<Boolean> = _isShowStarterProgressFlow.asLiveData()
    var isShowStarterProgress: Boolean
        get() = _isShowStarterProgressFlow.value
        set(value) {
            _isShowStarterProgressFlow.value = value
        }

    private val _starterMessageFlow: MutableStateFlow<String?> = MutableStateFlow(null)
    val starterMessageLiveData: LiveData<String?> = _starterMessageFlow.asLiveData()
    var starterMessage: String?
        get() = _starterMessageFlow.value
        set(value) {
            _starterMessageFlow.value = value
        }

    var startedServiceOnThisLaunch: Boolean = false

    fun startService(context: Context) {
        if (Shell.getShell().isRoot) {
            isShowStarterProgress = true
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    Starter.writeDataFiles(context)
                    if (RootPreferences.isStartOnBoot) {
                        Starter.writeSourceDir(context)
                    }
                    Shell
                        .cmd(Starter.command)
                        .to(object : CallbackList<String>() {
                            override fun onAddElement(s: String) {
                                starterMessage = s
                            }
                        })
                        .submit()
                } catch (e: Throwable) {
                    starterMessage = "Failure: ${e.message}"
                }
            }
        } else {
            starterMessage = "Failure: ${context.getString(R.string.no_root_access)}"
        }
    }

    fun makeStandardDirs(): Boolean {
        var isCreated = false
        viewModelScope.launch {
            for (standardDirectory in FileUtils.standardDirs) {
                if (FileUtils.externalStorageDir.resolve(standardDirectory).mkdirs()) {
                    isCreated = true
                }
            }
            if (isCreated && RootPreferences.isMonitor) {
                delay(100)
                val fileChanges = ScannerManager.consumeFileChanges()
                ScannerManager.shScanners.forEach {
                    it.acceptMonitor(fileChanges).start()
                }
            }
        }
        return isCreated
    }

    private val _isRevalidatingLiveData: MutableLiveData<Boolean> = MutableLiveData(false)
    val isRevalidatingLiveData: LiveData<Boolean>
        get() = _isRevalidatingLiveData

    init {
        PurchaseVerification.getRevalidationCertificates().forEach { revalidationCertificate ->
            viewModelScope.launch(Dispatchers.IO) {
                _isRevalidatingLiveData.postValue(true)
                _isRevalidatingLiveData.postValue(false)
            }
        }
    }
}
