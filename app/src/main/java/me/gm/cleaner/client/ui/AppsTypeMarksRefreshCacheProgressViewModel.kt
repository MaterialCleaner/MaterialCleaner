package me.gm.cleaner.client.ui

import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.gm.cleaner.client.CleanerClient
import me.gm.cleaner.net.OnlineAppTypeMarks
import java.util.concurrent.atomic.AtomicInteger

class AppsTypeMarksRefreshCacheProgressViewModel(private val application: Application) :
    AndroidViewModel(application) {
    private val _progressLiveData: MutableLiveData<AppsTypeMarksRefreshCacheState> =
        MutableLiveData<AppsTypeMarksRefreshCacheState>(
            AppsTypeMarksRefreshCacheState.Downloading(0, "")
        )
    val progressLiveData: LiveData<AppsTypeMarksRefreshCacheState>
        get() = _progressLiveData

    init {
        viewModelScope.launch {
            val installedNonSystemApps = CleanerClient.getInstalledPackages(0).filter {
                it.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0
            }
            val finishedCount = AtomicInteger()
            val jobs = installedNonSystemApps.map { packageInfo ->
                launch(Dispatchers.IO) {
                    ensureActive()
                    val packageName = packageInfo.packageName
                    runCatching {
                        withContext(Dispatchers.IO) {
                            OnlineAppTypeMarks.buildURL(application, packageName)
                                .invalidate()
                                .openStream()
                                .close()
                        }
                    }.onFailure {
                        // onFailure means the marks not exist
                    }
                    _progressLiveData.postValue(
                        AppsTypeMarksRefreshCacheState.Downloading(
                            100 * finishedCount.incrementAndGet() / installedNonSystemApps.size,
                            packageName
                        )
                    )
                }
            }
            jobs.joinAll()
            _progressLiveData.postValue(AppsTypeMarksRefreshCacheState.Done)
        }
    }
}

sealed class AppsTypeMarksRefreshCacheState {
    data class Downloading(val progress: Int, val packageName: String) :
        AppsTypeMarksRefreshCacheState()

    data object Done : AppsTypeMarksRefreshCacheState()
}
