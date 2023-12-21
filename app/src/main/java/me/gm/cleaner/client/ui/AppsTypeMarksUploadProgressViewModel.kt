package me.gm.cleaner.client.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.gm.cleaner.net.OnlineAppTypeMarks
import me.gm.cleaner.net.UploadByUsers
import java.util.concurrent.atomic.AtomicInteger

class AppsTypeMarksUploadProgressViewModel(private val application: Application) :
    AndroidViewModel(application) {
    private val _progressLiveData: MutableLiveData<AppsTypeMarksUploadState> =
        MutableLiveData<AppsTypeMarksUploadState>(AppsTypeMarksUploadState.Uploading(0, ""))
    val progressLiveData: LiveData<AppsTypeMarksUploadState>
        get() = _progressLiveData

    fun uploadAppsTypeMarks(appsTypeMarks: List<Pair<String, String>>) {
        viewModelScope.launch {
            val finishedCount = AtomicInteger()
            val jobs = appsTypeMarks.map { (packageName, content) ->
                launch(Dispatchers.IO) {
                    ensureActive()
                    runCatching {
                        withContext(Dispatchers.IO) {
                            OnlineAppTypeMarks.buildDefaultURL(application, packageName)
                                .openStream()
                                .close()
                        }
                    }.onFailure {
                        // onFailure means the marks not exist
                        UploadByUsers.uploadAppTypeMarks(packageName, content)
                    }
                    _progressLiveData.postValue(
                        AppsTypeMarksUploadState.Uploading(
                            100 * finishedCount.incrementAndGet() / appsTypeMarks.size,
                            packageName
                        )
                    )
                }
            }
            jobs.joinAll()
            _progressLiveData.postValue(AppsTypeMarksUploadState.Done)
        }
    }
}

sealed class AppsTypeMarksUploadState {
    data class Uploading(val progress: Int, val packageName: String) : AppsTypeMarksUploadState()
    data object Done : AppsTypeMarksUploadState()
}
