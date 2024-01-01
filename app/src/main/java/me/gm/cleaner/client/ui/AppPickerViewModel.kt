package me.gm.cleaner.client.ui

import android.app.Application
import android.content.pm.PackageInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import me.gm.cleaner.dao.AppLabelCache
import me.gm.cleaner.util.collatorComparator
import java.util.function.Consumer
import java.util.function.Supplier

class AppPickerViewModel(application: Application) : AndroidViewModel(application) {
    internal val onPositiveButtonClickListeners: MutableSet<Consumer<Set<PackageInfo>>> =
        mutableSetOf()

    private val _checkedAppsFlow: MutableStateFlow<Set<PackageInfo>> = MutableStateFlow(emptySet())
    var checkedApps: Set<PackageInfo>
        get() = _checkedAppsFlow.value
        set(value) {
            _checkedAppsFlow.value = value
        }

    fun toggle(packageInfo: PackageInfo) {
        if (packageInfo in checkedApps) {
            checkedApps -= packageInfo
        } else {
            checkedApps += packageInfo
        }
    }

    private val _filterTextFlow: MutableStateFlow<String> = MutableStateFlow("")
    var filterText: String
        get() = _filterTextFlow.value
        set(value) {
            _filterTextFlow.value = value
        }

    internal var allAppsSupplier: Supplier<List<PackageInfo>> = Supplier {
        application.packageManager.getInstalledPackages(0)
    }
    val allApps: List<PackageInfo> by lazy { allAppsSupplier.get() }
    var showingApps: List<AppPickerModel> = emptyList()
        private set

    private val _appsFlow: MutableStateFlow<List<AppPickerModel>> = MutableStateFlow(emptyList())
    val appsFlow: Flow<List<AppPickerModel>> =
        combine(_appsFlow, _filterTextFlow, _checkedAppsFlow) { apps, filterText, checkedApps ->
            val installedPackages = allApps.map { it.packageName }
            val uninstalledCheckedApps = checkedApps
                .filter { it.packageName !in installedPackages }
                .map { packageInfo ->
                    AppPickerModel(
                        packageInfo,
                        packageInfo.packageName,
                        true
                    )
                }
            var sequence = (apps + uninstalledCheckedApps).asSequence()
            if (filterText.isNotEmpty()) {
                sequence = sequence.filter {
                    it.label.contains(filterText, true) ||
                            it.packageInfo.packageName.contains(filterText, true) ||
                            (it.packageInfo.sharedUserId ?: "").contains(filterText, true)
                }
            }
            sequence = sequence.sortedWith(collatorComparator { it.label })
            sequence = sequence.map {
                if (it.isChecked != it.packageInfo in checkedApps) {
                    it.copy(isChecked = !it.isChecked)
                } else {
                    it
                }
            }
            sequence = sequence.sortedByDescending { it.isChecked }
            sequence.toList().also {
                showingApps = it
            }
        }

    fun init(pendingPreActions: List<Runnable>, pendingPostActions: List<Runnable>) {
        viewModelScope.launch(Dispatchers.Default) {
            pendingPreActions.forEach { it.run() }
            _appsFlow.tryEmit(allApps.map { pi ->
                AppPickerModel(
                    pi,
                    AppLabelCache.getPackageLabel(pi),
                    pi in checkedApps
                )
            })
            pendingPostActions.forEach { it.run() }
        }
    }
}
