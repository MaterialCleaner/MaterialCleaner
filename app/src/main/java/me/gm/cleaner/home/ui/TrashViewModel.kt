package me.gm.cleaner.home.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import me.gm.cleaner.dao.RootPreferences
import me.gm.cleaner.home.TrashModel
import me.gm.cleaner.home.scanner.ScannerViewModel
import me.gm.cleaner.wroker.DeleteWorker
import java.util.UUID

class TrashViewModel(application: Application) : AndroidViewModel(application) {
    private val _isSearchingFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)
    var isSearching: Boolean
        get() = _isSearchingFlow.value
        set(value) {
            _isSearchingFlow.value = value
        }
    private val _queryTextFlow: MutableStateFlow<String> = MutableStateFlow("")
    var queryText: String
        get() = _queryTextFlow.value
        set(value) {
            _queryTextFlow.value = value
        }
    private val _toggedTrashesFlow: MutableStateFlow<Set<String>> = MutableStateFlow(emptySet())
    private var toggedTrashes: Set<String>
        get() = _toggedTrashesFlow.value
        private set(value) {
            _toggedTrashesFlow.value = value
        }
    private val _deletedTrashesFlow: MutableStateFlow<Set<String>> = MutableStateFlow(emptySet())
    private var deletedTrashes: Set<String>
        get() = _deletedTrashesFlow.value
        set(value) {
            _deletedTrashesFlow.value = value
        }
    private val _requestedStateFlow: MutableStateFlow<Class<out TrashState>> =
        MutableStateFlow(TrashState.Done::class.java)
    var requestedState: Class<out TrashState>
        get() = _requestedStateFlow.value
        private set(value) {
            _requestedStateFlow.value = value
        }

    fun trashesFlow(
        trashesFlow: Flow<List<TrashModel>>, progressFlow: Flow<Int>
    ): Flow<TrashState> = combine(
        trashesFlow, progressFlow, _isSearchingFlow, _queryTextFlow,
        _toggedTrashesFlow, _deletedTrashesFlow, _requestedStateFlow
    ) { source, progress, isSearching, queryText, toggedTrashes, deletedTrashes, requestedState ->
        var sequence = source.asSequence()
        sequence = sequence.filterNot {
            it.path in deletedTrashes
        }
        if (isSearching) {
            sequence = sequence.filter {
                val pi = it.packageInfo
                return@filter if (pi == null) {
                    it.path.contains(queryText, true)
                } else {
                    it.path.contains(queryText, true) ||
                            it.label!!.contains(queryText, true) ||
                            pi.packageName.contains(queryText, true)
                }
            }
        }
        if (!ScannerViewModel.isRunning(progress) &&
            RootPreferences.isShowLength && RootPreferences.isSort
        ) {
            sequence = sequence.sortedByDescending { it.length }
        }
        sequence = sequence.map {
            if (it.path in toggedTrashes) {
                it.copy(isChecked = !it.isChecked)
            } else {
                it
            }
        }
        val list = sequence.toList()
        if (ScannerViewModel.isRunning(progress)) {
            TrashState.Scanning(list, progress)
        } else when (requestedState) {
            TrashState.Done::class.java -> TrashState.Done(list).also { trashes = list }
            TrashState.Cleaning::class.java -> TrashState.Cleaning(list)
            TrashState.Cleared::class.java -> TrashState.Cleared(list)
            else -> throw IllegalStateException()
        }
    }

    fun toggle(target: TrashModel) {
        if (target.isInUse) return
        if (target.path in toggedTrashes) {
            toggedTrashes -= target.path
        } else {
            toggedTrashes += target.path
        }
    }

    fun remove(target: TrashModel) {
        deletedTrashes += target.path
    }

    private var trashes: List<TrashModel> = emptyList()
    private var deleteRequestId: UUID? = null
    fun deleteTrashes() {
        requestedState = TrashState.Cleaning::class.java
        viewModelScope.launch {
            val request = DeleteWorker.createRequest(
                getApplication(),
                *trashes.asSequence()
                    .filter { it.isChecked && !it.isInUse }
                    .map { it.path }
                    .toList()
                    .toTypedArray()
            )
            val workManager = WorkManager.getInstance(getApplication())
            workManager.enqueueUniqueWork(
                DeleteWorker.WORK_NAME,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                request
            )
            deleteRequestId = request.id
            val workInfoLiveData = workManager.getWorkInfoByIdLiveData(request.id)
            workInfoLiveData.observeForever(object : Observer<WorkInfo> {
                override fun onChanged(value: WorkInfo) {
                    deletedTrashes += DeleteWorker.PROGRESS.removeAll(request.id)
                    if (value.state.isFinished) {
                        workInfoLiveData.removeObserver(this)
                        deleteRequestId = null
                        requestedState = TrashState.Cleared::class.java
                    }
                }
            })
        }
    }

    override fun onCleared() {
        deleteRequestId?.let {
            val workManager = WorkManager.getInstance(getApplication())
            workManager.cancelWorkById(it)
        }
    }
}

sealed class TrashState {
    data class Scanning(val list: List<TrashModel>, val progress: Int) : TrashState()
    data class Done(val list: List<TrashModel>) : TrashState()
    data class Cleaning(val list: List<TrashModel>) : TrashState()
    data class Cleared(val list: List<TrashModel>) : TrashState()
}

fun <T1, T2, T3, T4, T5, T6, R> combine(
    flow: Flow<T1>, flow2: Flow<T2>, flow3: Flow<T3>, flow4: Flow<T4>, flow5: Flow<T5>,
    flow6: Flow<T6>, transform: suspend (T1, T2, T3, T4, T5, T6) -> R
): Flow<R> = combine(flow, flow2, flow3, flow4, flow5, flow6) { args: Array<*> ->
    transform(
        args[0] as T1, args[1] as T2, args[2] as T3, args[3] as T4, args[4] as T5, args[5] as T6
    )
}

fun <T1, T2, T3, T4, T5, T6, T7, R> combine(
    flow: Flow<T1>, flow2: Flow<T2>, flow3: Flow<T3>, flow4: Flow<T4>, flow5: Flow<T5>,
    flow6: Flow<T6>, flow7: Flow<T7>, transform: suspend (T1, T2, T3, T4, T5, T6, T7) -> R
): Flow<R> = combine(flow, flow2, flow3, flow4, flow5, flow6, flow7) { args: Array<*> ->
    transform(
        args[0] as T1, args[1] as T2, args[2] as T3, args[3] as T4, args[4] as T5, args[5] as T6,
        args[6] as T7
    )
}
