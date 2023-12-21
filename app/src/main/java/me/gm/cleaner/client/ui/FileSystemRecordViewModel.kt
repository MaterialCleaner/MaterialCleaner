package me.gm.cleaner.client.ui

import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import me.gm.cleaner.client.CleanerClient
import me.gm.cleaner.dao.AppLabelCache
import me.gm.cleaner.dao.ServicePreferences
import me.gm.cleaner.model.BulkCursor
import me.gm.cleaner.model.FileSystemEvent
import me.gm.cleaner.server.IFileChangeObserver
import me.gm.cleaner.server.observer.FileSystemObserver
import me.gm.cleaner.util.FileUtils
import java.lang.ref.WeakReference
import java.util.concurrent.Executors
import kotlin.io.path.Path
import kotlin.io.path.pathString

class FileSystemRecordViewModel(application: Application) :
    BaseServiceSettingsViewModel(application) {
    internal val eventModelMapper: (FileSystemEvent) -> FileSystemRecordModel = { event ->
        val pi = CleanerClient.service!!.getPackageInfo(
            event.packageName, PackageManager.GET_PERMISSIONS
        )
        val readOnlyPaths = ServicePreferences.getPackageReadOnly(
            event.packageName, FileUtils.extractUserIdFromPath(event.path)
        )
        FileSystemRecordModel(
            event,
            pi,
            if (pi == null) null else AppLabelCache.getPackageLabel(pi),
            readOnlyPaths.contains(event.path) ||
                    readOnlyPaths.contains(Path(event.path).parent.pathString)
        )
    }

    internal val _fileSystemRecordLiveData: MutableLiveData<FileSystemRecordState> =
        MutableLiveData<FileSystemRecordState>(FileSystemRecordState.Loading)
    val fileSystemRecordLiveData: LiveData<FileSystemRecordState>
        get() = _fileSystemRecordLiveData
    internal val fileSystemRecord: MutableList<FileSystemRecordModel> = mutableListOf()
    internal var addedByObserver: Int = 0
    private lateinit var bulkCursor: BulkCursor<FileSystemEvent>

    internal val dispatcher: ExecutorCoroutineDispatcher =
        Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    private fun CoroutineScope.loadMoreForRequestedSize(size: Int): List<FileSystemRecordModel> {
        val fileSystemRecord = ArrayList<FileSystemEvent>(size)
        while (fileSystemRecord.size < size) {
            ensureActive()
            val list = bulkCursor.load(size - fileSystemRecord.size)
            if (list.isEmpty()) {
                break
            }
            fileSystemRecord += list
        }
        return fileSystemRecord.map(eventModelMapper)
    }

    private fun CoroutineScope.loadMoreInternal(size: Int): List<FileSystemRecordModel> {
        val fileSystemRecords = mutableListOf<FileSystemRecordModel>()
        var ultimatelyRequestedSize = size
        while (fileSystemRecords.size < ultimatelyRequestedSize) {
            val fileSystemRecord = loadMoreForRequestedSize(size)
            if (fileSystemRecord.isEmpty()) {
                break
            }
            fileSystemRecords += fileSystemRecord
            val packageName = fileSystemRecord[0].event.packageName
            if (fileSystemRecord.size == size &&
                fileSystemRecords.size < 10 * size &&
                fileSystemRecord.all { it.event.packageName == packageName }
            ) {
                ultimatelyRequestedSize += size
            }
        }
        return fileSystemRecords
    }

    fun loadMore() {
        viewModelScope.launch(dispatcher) {
            _fileSystemRecordLiveData.postValue(FileSystemRecordState.LoadingMore(-1))
            fileSystemRecord += loadMoreInternal(LOAD_SIZE)
            _fileSystemRecordLiveData.postValue(FileSystemRecordState.Done(fileSystemRecord.toList()))
        }
    }

    fun loadAll() {
        if (!::bulkCursor.isInitialized) {
            return
        }
        dispatcher.cancel()
        viewModelScope.launch(dispatcher) {
            ensureActive()
            val unloadedSize = bulkCursor.count - (fileSystemRecord.size - addedByObserver)
            var loaded = 0
            while (loaded < unloadedSize) {
                fileSystemRecord += loadMoreForRequestedSize(1)
                loaded++
                _fileSystemRecordLiveData.postValue(FileSystemRecordState.LoadingMore(100 * loaded / unloadedSize))
            }
            _fileSystemRecordLiveData.postValue(FileSystemRecordState.Done(fileSystemRecord.toList()))
        }
    }

    fun setLoading() {
        _fileSystemRecordLiveData.postValue(FileSystemRecordState.Loading)
    }

    fun reload() {
        dispatcher.cancel()
        viewModelScope.launch(dispatcher) {
            fileSystemRecord.clear()
            addedByObserver = 0
            ensureActive()
            if (::bulkCursor.isInitialized) {
                bulkCursor.close()
            }
            bulkCursor = CleanerClient.service!!.queryAllRecords(
                isHideAppSpecificStorage, if (isSearching) queryText else null
            )
            fileSystemRecord += loadMoreInternal(LOAD_SIZE)
            _fileSystemRecordLiveData.postValue(FileSystemRecordState.Done(fileSystemRecord.toList()))
        }
    }

    private val _checkedFilterAppsFlow: MutableStateFlow<Set<String>> =
        MutableStateFlow(CleanerClient.service!!.denyList.toSet())
    var checkedFilterApps: Set<String>
        get() = _checkedFilterAppsFlow.value
        set(value) {
            CleanerClient.service!!.setDenyList(value.toTypedArray())
            _checkedFilterAppsFlow.value = value
        }
    private val _isHideAppSpecificStorageFlow: MutableStateFlow<Boolean> =
        MutableStateFlow(ServicePreferences.isHideAppSpecificStorage)
    var isHideAppSpecificStorage: Boolean
        get() = _isHideAppSpecificStorageFlow.value
        set(value) {
            ServicePreferences.isHideAppSpecificStorage = value
            _isHideAppSpecificStorageFlow.value = value
        }

    private val fileSystemChangeObserver = FileChangeListener(this)

    fun startLoadingRecord() {
        viewModelScope.launch {
            combine(
                _checkedFilterAppsFlow, _isHideAppSpecificStorageFlow,
                _isSearchingFlow, _queryTextFlow
            ) { _, _, _, _ -> }.collect {
                setLoading()
                reload()
            }
        }
        CleanerClient.service!!.registerFileChangeObserver(fileSystemChangeObserver)
    }

    init {
        when {
            !ServicePreferences.recordSharedStorage &&
                    !ServicePreferences.recordExternalAppSpecificStorage ->
                _fileSystemRecordLiveData.postValue(FileSystemRecordState.Disabled)

            application
                .createDeviceProtectedStorageContext()
                .getDatabasePath(FileSystemObserver.DATABASE_NAME)
                .length() > 1000000000 /* 1GB */ ->
                _fileSystemRecordLiveData.postValue(FileSystemRecordState.DbTooLarge)

            else -> startLoadingRecord()
        }
    }

    override fun onCleared() {
        if (::bulkCursor.isInitialized) {
            bulkCursor.close()
        }
        dispatcher.close()
        CleanerClient.service!!.unregisterFileChangeObserver(fileSystemChangeObserver)
    }

    companion object {
        const val LOAD_SIZE: Int = 100
    }
}

sealed class FileSystemRecordState {
    data object Disabled : FileSystemRecordState()
    data object DbTooLarge : FileSystemRecordState()
    data object Loading : FileSystemRecordState()
    data class LoadingMore(val progress: Int) : FileSystemRecordState()
    data class Done(val list: List<FileSystemRecordModel>) : FileSystemRecordState()
}

// https://stackoverflow.com/questions/12204740/messenger-to-remote-service-causing-memory-leak/12206516#12206516
class FileChangeListener(viewModel: FileSystemRecordViewModel) : IFileChangeObserver.Stub() {
    private val viewModelRef: WeakReference<FileSystemRecordViewModel> = WeakReference(viewModel)

    override fun onEvent(
        timeMillis: Long, packageName: String, path: String, flags: Int,
        isAppSpecificStorage: Boolean
    ) {
        val viewModel = viewModelRef.get() ?: return
        viewModel.viewModelScope.launch(viewModel.dispatcher) {
            if (viewModel.isHideAppSpecificStorage && isAppSpecificStorage ||
                packageName in viewModel.checkedFilterApps
            ) {
                return@launch
            }
            if (!viewModel.isSearching || path.contains(viewModel.queryText, true)) {
                viewModel.fileSystemRecord.add(
                    0, viewModel.eventModelMapper(
                        FileSystemEvent(timeMillis, packageName, path, flags)
                    )
                )
                viewModel.addedByObserver++
                viewModel._fileSystemRecordLiveData.postValue(
                    FileSystemRecordState.Done(viewModel.fileSystemRecord.toList())
                )
            }
        }
    }
}
