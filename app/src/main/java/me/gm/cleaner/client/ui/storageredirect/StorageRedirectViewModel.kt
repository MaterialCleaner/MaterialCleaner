package me.gm.cleaner.client.ui.storageredirect

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Build
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.common.collect.HashBiMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import me.gm.cleaner.R
import me.gm.cleaner.client.CleanerClient
import me.gm.cleaner.client.getSharedProcessPackages
import me.gm.cleaner.client.getSharedUserIdPackages
import me.gm.cleaner.dao.MountRules
import me.gm.cleaner.dao.ServicePreferences
import me.gm.cleaner.model.PackageStatus
import me.gm.cleaner.net.NetworkConnectionState
import me.gm.cleaner.net.OnlineAppTypeMarks
import me.gm.cleaner.util.FileUtils.toUserId
import me.gm.cleaner.widget.recyclerview.DiffArrayList

class StorageRedirectViewModel(private val application: Application, state: SavedStateHandle) :
    AndroidViewModel(application) {
    private val savedModeMap: HashBiMap<Int, Mode> = HashBiMap.create(
        mapOf(
            0 to Mode.Welcome,
            1 to Mode.Wizard,
            2 to Mode.Editor,
        )
    )

    init {
        state.setSavedStateProvider(::mode.name) {
            bundleOf(::mode.name to savedModeMap.inverse()[mode])
        }
        state.setSavedStateProvider(::mountRules.name) {
            val unzipped = _mountRulesLiveData.value!!.unzip()
            bundleOf(
                "source" to ArrayList(unzipped.first),
                "target" to ArrayList(unzipped.second),
            )
        }
        state.setSavedStateProvider(::readOnlyPaths.name) {
            bundleOf(::readOnlyPaths.name to _readOnlyPathsLiveData.value!!)
        }
    }

    // ADAPTER
    private val _modeFlow: MutableStateFlow<Mode> = state.get<Bundle>(::mode.name).let { bundle ->
        if (bundle == null) {
            MutableStateFlow(Mode.Welcome)
        } else {
            MutableStateFlow(savedModeMap[bundle.getInt(::mode.name)]!!)
        }
    }
    val modeLiveData: LiveData<Mode> = _modeFlow.asLiveData()
    var mode: Mode
        get() = _modeFlow.value
        set(value) {
            _modeFlow.value = value
        }

    // INFO
    private val _hintsFlow: MutableStateFlow<MutableList<Pair<Int, String>>> =
        MutableStateFlow(mutableListOf())
    val hintsLiveData: LiveData<MutableList<Pair<Int, String>>> = _hintsFlow.asLiveData()
    fun addHint(level: Int, hint: String) {
        val list = _hintsFlow.value.toMutableList().apply {
            add(level to hint)
        }
        _hintsFlow.value = list
    }

    fun loadStoragePermissions(pi: PackageInfo): List<String> {
        pi.requestedPermissions ?: return emptyList()
        val storagePermissions = mutableListOf(
            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
            }
        }.filter {
            pi.requestedPermissions.contains(it)
        }
        return storagePermissions
    }

    var runningStatus: String? = null
        private set
    var hasMountException: Boolean = false
        private set

    // Don't use "application" to support language setting.
    fun loadRunningStatusAsync(packageName: String, context: Context) =
        viewModelScope.async(Dispatchers.Default) {
            val packageStatus = CleanerClient.service!!.getPackageStatus(
                packageName, PackageStatus.GET_FROM_ALL_PROCESS
            )
            if (packageStatus.pids.isEmpty()) {
                runningStatus = ""
                return@async
            }
            runningStatus = context.getString(
                R.string.storage_redirect_status_summary_prefix,
                packageStatus.pids.joinToString(context.getString(R.string.delimiter))
            )
            if (ServicePreferences.getPackageSrCount(packageName) == 0) {
                return@async
            }
            val mountedPids = mutableListOf<Int>()
            val startUpAwarePids = mutableListOf<Int>()
            val deletedPids = mutableListOf<Int>()
            val mkdirFailedPids = mutableListOf<Int>()
            val unknownPids = mutableListOf<Int>()
            val mountFailedPids = mutableListOf<Int>()
            packageStatus.pidFlags.forEachIndexed { index, pidFlag ->
                if (pidFlag and PackageStatus.PID_FLAG_MOUNTED != 0) {
                    mountedPids += packageStatus.pids[index]
                }
                if (pidFlag and PackageStatus.PID_FLAG_STARTUP_AWARE != 0) {
                    startUpAwarePids += packageStatus.pids[index]
                }
                if (pidFlag and PackageStatus.PID_FLAG_DELETED != 0) {
                    deletedPids += packageStatus.pids[index]
                }
                if (pidFlag and PackageStatus.PID_FLAG_MKDIR_FAILED != 0) {
                    mkdirFailedPids += packageStatus.pids[index]
                }
                if (pidFlag and PackageStatus.PID_FLAG_UNKNOWN != 0) {
                    unknownPids += packageStatus.pids[index]
                }
                if (pidFlag and PackageStatus.PID_FLAG_MOUNT_FAILED != 0) {
                    mountFailedPids += packageStatus.pids[index]
                }
            }
            if (packageStatus.pids.size == mountedPids.size) {
                runningStatus += context.getString(R.string.storage_redirect_status_summary_suffix_mounted)
                return@async
            }
            hasMountException = true
            val startUpUnawarePids = packageStatus.pids.toList() - startUpAwarePids
            if (startUpUnawarePids.isNotEmpty()) {
                runningStatus += context.resources.getQuantityString(
                    R.plurals.storage_redirect_status_summary_suffix_not_mounted,
                    startUpUnawarePids.size,
                    context.getString(R.string.storage_redirect_status_not_mounted_startup_unaware),
                    startUpUnawarePids.joinToString(context.getString(R.string.delimiter))
                )
            }
            if (deletedPids.isNotEmpty()) {
                runningStatus += context.resources.getQuantityString(
                    R.plurals.storage_redirect_status_summary_suffix_not_mounted,
                    deletedPids.size,
                    context.getString(R.string.storage_redirect_status_not_mounted_dir_deleted),
                    deletedPids.joinToString(context.getString(R.string.delimiter))
                )
            }
            mkdirFailedPids -= startUpUnawarePids
            if (mkdirFailedPids.isNotEmpty()) {
                runningStatus += context.resources.getQuantityString(
                    R.plurals.storage_redirect_status_summary_suffix_not_mounted,
                    mkdirFailedPids.size,
                    context.getString(R.string.storage_redirect_status_not_mounted_mkdir_failed),
                    mkdirFailedPids.joinToString(context.getString(R.string.delimiter))
                )
            }
            if (unknownPids.isNotEmpty()) {
                runningStatus += context.resources.getQuantityString(
                    R.plurals.storage_redirect_status_summary_suffix_not_mounted,
                    unknownPids.size,
                    context.getString(R.string.storage_redirect_status_not_mounted_unknown),
                    unknownPids.joinToString(context.getString(R.string.delimiter))
                )
            }
            mountFailedPids -= mkdirFailedPids
            if (mountFailedPids.isNotEmpty()) {
                runningStatus += context.resources.getQuantityString(
                    R.plurals.storage_redirect_status_summary_suffix_not_mounted,
                    mountFailedPids.size,
                    context.getString(R.string.storage_redirect_status_not_mounted_mount_failed),
                    mountFailedPids.joinToString(context.getString(R.string.delimiter))
                )
            }
            val remountedPids = packageStatus.pids.toList() -
                    mountedPids - startUpUnawarePids - deletedPids -
                    mkdirFailedPids - unknownPids - mountFailedPids
            if (remountedPids.isNotEmpty()) {
                runningStatus += context.resources.getQuantityString(
                    R.plurals.storage_redirect_status_summary_suffix_not_mounted,
                    remountedPids.size,
                    context.getString(R.string.storage_redirect_status_not_mounted_remount),
                    remountedPids.joinToString(context.getString(R.string.delimiter))
                )
            }
            runningStatus += context.getString(
                R.string.storage_redirect_status_summary_troubleshoot_mount_exception
            )
        }

    var isInMagiskDenyList: Boolean? = null

    fun loadIsInMagiskDenyListAsync(packageName: String) =
        viewModelScope.async(Dispatchers.Default) {
            isInMagiskDenyList = CleanerClient.zygiskEnabled &&
                    CleanerClient.service!!.isInMagiskDenyList(packageName)
        }

    var sharedUserIdPackages: List<PackageInfo>? = null
        private set

    fun loadSharedUserIdPackagesAsync(packageInfo: PackageInfo) =
        viewModelScope.async(Dispatchers.Default) {
            sharedUserIdPackages = getSharedUserIdPackages(packageInfo)
        }

    var sharedProcessPackages: List<PackageInfo>? = null
        private set

    fun loadSharedProcessPackagesAsync(packageInfo: PackageInfo) =
        viewModelScope.async(Dispatchers.Default) {
            sharedProcessPackages = getSharedProcessPackages(packageInfo)
        }

    // RULES
    private val _mountRulesLiveData: MutableLiveData<DiffArrayList<Pair<String?, String?>>> =
        state.get<Bundle>(::mountRules.name).let { bundle ->
            if (bundle == null) {
                MutableLiveData(DiffArrayList())
            } else {
                val source = bundle.getStringArrayList("source")!!
                val target = bundle.getStringArrayList("target")!!
                MutableLiveData(DiffArrayList(source.zip(target)))
            }
        }
    val mountRulesLiveData: LiveData<DiffArrayList<Pair<String?, String?>>>
        get() = _mountRulesLiveData
    var mountRules: List<Pair<String, String>>
        get() = _mountRulesLiveData.value!!.run { subList(0, size - 1) }
                as List<Pair<String, String>>
        private set(value) {
            _mountRulesLiveData.value = DiffArrayList<Pair<String?, String?>>(
                value.plus(null to null)
            )
        }
    val rules: MountRules
        get() = MountRules(mountRules)
    lateinit var wizard: MountWizard
        private set

    fun updateMountRules(action: DiffArrayList<Pair<String?, String?>>.() -> Unit) {
        _mountRulesLiveData.value = _mountRulesLiveData.value!!.also { action(it) }
    }

    fun writeMountRules() {
        val packageNames = sharedProcessPackages?.map { it.packageName } ?: emptyList()
        ServicePreferences.putStorageRedirect(mountRules, packageNames)
        CleanerClient.service!!.notifySrChanged()
        if (CleanerClient.service!!.isFuseBpfEnabled) {
            CleanerClient.service!!.switchSpecificAppsOwner(packageNames.toTypedArray())
        }
    }

    private val _readOnlyPathsLiveData: MutableLiveData<DiffArrayList<String?>> =
        state.get<Bundle>(::readOnlyPaths.name).let { bundle ->
            if (bundle == null) {
                MutableLiveData(DiffArrayList())
            } else {
                val readOnlyPaths = bundle.getStringArrayList(::readOnlyPaths.name)!!
                MutableLiveData(DiffArrayList(readOnlyPaths))
            }
        }
    val readOnlyPathsLiveData: LiveData<DiffArrayList<String?>>
        get() = _readOnlyPathsLiveData
    var readOnlyPaths: List<String>
        get() = _readOnlyPathsLiveData.value!!.run { subList(0, size - 1) } as List<String>
        private set(value) {
            _readOnlyPathsLiveData.value = DiffArrayList(value.plus(null))
        }

    fun updateReadOnlyPaths(action: DiffArrayList<String?>.() -> Unit) {
        _readOnlyPathsLiveData.value = _readOnlyPathsLiveData.value!!.also { action(it) }
    }

    fun writeReadOnlyPaths() {
        ServicePreferences.putReadOnly(
            readOnlyPaths, sharedUserIdPackages?.map { it.packageName } ?: emptyList()
        )
        CleanerClient.service!!.notifyReadOnlyChanged()
    }

    private val _appTypeMarksFlow: MutableStateFlow<NetworkConnectionState<AppTypeMarks?>> =
        MutableStateFlow(NetworkConnectionState.Loading())
    val appTypeMarksLiveData: LiveData<NetworkConnectionState<AppTypeMarks?>> =
        _appTypeMarksFlow.asLiveData()
    var appTypeMarks: NetworkConnectionState<AppTypeMarks?>
        get() = _appTypeMarksFlow.value
        private set(value) {
            _appTypeMarksFlow.value = value
        }

    fun initSettings(pi: PackageInfo) {
        mountRules = ServicePreferences.getPackageSrZipped(pi.packageName)
        readOnlyPaths = ServicePreferences.getPackageReadOnly(pi.packageName)
    }

    fun initMountWizard(pi: PackageInfo) {
        wizard = MountWizard(pi)
        viewModelScope.launch {
            OnlineAppTypeMarks.fetch(application, pi).onSuccess {
                appTypeMarks = NetworkConnectionState.Success(it)
            }.onFailure { e ->
                appTypeMarks = NetworkConnectionState.Failure(e)
            }
        }
    }

    private val _testFlow: MutableStateFlow<String> = MutableStateFlow("")
    val testLiveData: LiveData<String> = _testFlow.asLiveData()
    var test: String
        get() = _testFlow.value
        set(value) {
            _testFlow.value = value
        }

    // PERMISSIONS
    val permissionToGrant: MutableMap<String, Int> = mutableMapOf()

    fun isRuntime(permission: String): Boolean = when {
        permission == Manifest.permission.READ_EXTERNAL_STORAGE ||
                permission == Manifest.permission.WRITE_EXTERNAL_STORAGE -> {
            true
        }

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                permission == Manifest.permission.MANAGE_EXTERNAL_STORAGE -> {
            false
        }

        else -> {
            throw UnsupportedOperationException()
        }
    }

    fun setPackagePermission(ai: ApplicationInfo, permission: String, grant: Boolean) {
        val userId = ai.uid.toUserId()
        CleanerClient.service!!.setPackagePermission(
            ai, permission, isRuntime(permission), userId, grant
        )
    }
}

sealed class Mode {
    object Welcome : Mode()
    object Wizard : Mode()
    object Editor : Mode()
}
