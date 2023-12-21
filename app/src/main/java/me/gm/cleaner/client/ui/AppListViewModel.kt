package me.gm.cleaner.client.ui

import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import me.gm.cleaner.client.CleanerClient
import me.gm.cleaner.dao.ServicePreferences
import me.gm.cleaner.util.PermissionUtils
import me.gm.cleaner.util.collatorComparator

class AppListViewModel(application: Application) : BaseServiceSettingsViewModel(application) {
    private val _appsFlow: MutableStateFlow<AppListState> = MutableStateFlow(AppListState.Loading)
    val isDone: Boolean
        get() = _appsFlow.value is AppListState.Done

    private val _uninstalledPackagesLiveData: MutableLiveData<List<String>> =
        MutableLiveData<List<String>>(emptyList())
    val uninstalledPackagesLiveData: LiveData<List<String>>
        get() = _uninstalledPackagesLiveData

    val appsFlow: Flow<AppListState> = combine(
        _appsFlow, _isSearchingFlow, _queryTextFlow
    ) { state, isSearching, queryText ->
        when (state) {
            is AppListState.Loading -> return@combine AppListState.Loading
            else -> {}
        }
        val list = (state as AppListState.Done).list
        var sequence = list.asSequence()
        if (ServicePreferences.isHideSystemApp) {
            sequence = sequence.filter {
                it.packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0
            }
        }
        if (ServicePreferences.isHideDisabledApp) {
            sequence = sequence.filter {
                it.packageInfo.applicationInfo.enabled
            }
        }
        if (ServicePreferences.isHideNoStoragePermissionApp) {
            sequence = sequence.filter {
                PermissionUtils.containsStoragePermissions(it.packageInfo)
            }
        }
        if (isSearching) {
            sequence = sequence.filter {
                it.label.contains(queryText, true) ||
                        it.packageInfo.packageName.contains(queryText, true) ||
                        (it.packageInfo.sharedUserId ?: "").contains(queryText, true)
            }
        }
        sequence = when (ServicePreferences.sortBy) {
            ServicePreferences.SORT_BY_NAME ->
                sequence.sortedWith(collatorComparator { it.label })

            ServicePreferences.SORT_BY_UPDATE_TIME ->
                sequence.sortedByDescending { it.packageInfo.lastUpdateTime }

            else -> throw IllegalArgumentException()
        }
        if (ServicePreferences.ruleCount) {
            sequence = sequence.sortedByDescending {
                val c1 = if (it.mountRulesCount > 0) 2 else 0
                val c2 = if (it.readOnlyCount > 0) 1 else 0
                c1 + c2
            }
        }
        if (ServicePreferences.mountState) {
            sequence = sequence.sortedByDescending {
                it.mountState
            }
        }
        AppListState.Done(sequence.toList())
    }

    fun loadApps() {
        viewModelScope.launch {
            _appsFlow.value = AppListState.Loading
            val list = AppListLoader().load()

            val installedPackages = list
                .asSequence()
                .map { it.packageInfo.packageName }
                .toSet()
            val uninstalledPackages =
                (ServicePreferences.getUninstalledSrPackages(installedPackages) +
                        ServicePreferences.getUninstalledReadOnlyPackages(installedPackages) +
                        CleanerClient.service!!.denyList - installedPackages).distinct()
            if (uninstalledPackages.isNotEmpty()) {
                _uninstalledPackagesLiveData.postValue(uninstalledPackages.toMutableList())
            }
            _appsFlow.value = AppListState.Done(list)
        }
    }

    fun updateAppsRuleCount() {
        viewModelScope.launch {
            val value = _appsFlow.value
            if (value is AppListState.Done) {
                _appsFlow.value = AppListState.Loading
                val list = AppListLoader().updateRuleCount(value.list)
                _appsFlow.value = AppListState.Done(list)
            }
        }
    }

    init {
        loadApps()
    }
}

sealed class AppListState {
    data object Loading : AppListState()
    data class Done(val list: List<AppListModel>) : AppListState()
}
