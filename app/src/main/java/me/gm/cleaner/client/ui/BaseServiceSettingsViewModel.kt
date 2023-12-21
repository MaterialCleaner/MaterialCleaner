package me.gm.cleaner.client.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow

abstract class BaseServiceSettingsViewModel(application: Application) :
    AndroidViewModel(application) {
    protected val _isSearchingFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)
    var isSearching: Boolean
        get() = _isSearchingFlow.value
        set(value) {
            _isSearchingFlow.value = value
        }
    protected val _queryTextFlow: MutableStateFlow<String> = MutableStateFlow("")
    var queryText: String
        get() = _queryTextFlow.value
        set(value) {
            _queryTextFlow.value = value
        }
}
