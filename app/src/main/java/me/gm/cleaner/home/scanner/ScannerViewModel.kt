package me.gm.cleaner.home.scanner

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.gm.cleaner.home.TrashModel
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Predicate

abstract class ScannerViewModel : ViewModel() {
    // We want to call observers without changing trashes' content,
    // hence we prefer using MutableLiveData rather than MutableStateFlow.
    private val _trashesLiveData: MutableLiveData<MutableList<TrashModel>> =
        MutableLiveData(CopyOnWriteArrayList())
    val trashesLiveData: LiveData<MutableList<TrashModel>>
        get() = _trashesLiveData
    private val _trashes: MutableList<TrashModel>
        get() = _trashesLiveData.value!!
    val trashes: List<TrashModel>
        get() = _trashes

    fun addTrash(trash: TrashModel) {
        val list = _trashes.apply {
            add(trash)
        }
        _trashesLiveData.postValue(list)
    }

    fun removeTrashIf(filter: Predicate<TrashModel>) {
        val list = _trashes.apply {
            removeIf(filter)
        }
        _trashesLiveData.postValue(list)
    }

    private val _isAcceptInheritanceFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isAcceptInheritanceLiveData: LiveData<Boolean> = _isAcceptInheritanceFlow.asLiveData()
    var isAcceptInheritance: Boolean
        get() = _isAcceptInheritanceFlow.value
        set(value) {
            _isAcceptInheritanceFlow.value = value
        }

    private val _progressFlow: MutableStateFlow<Int> = MutableStateFlow(-2)
    val progressFlow: StateFlow<Int> = _progressFlow.asStateFlow()
    var progress: Int
        get() = _progressFlow.value
        set(value) {
            _progressFlow.value = value
        }
    val isRunning: Boolean
        get() = progress >= 0

    override fun onCleared() {
        ScannerManager.scanners.firstOrNull { javaClass == it.info.viewModelClass }?.onDestroy()
    }

    companion object {

        fun isRunning(progress: Int) = progress >= 0
    }
}
