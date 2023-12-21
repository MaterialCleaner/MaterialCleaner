package me.gm.cleaner.client.ui.storageredirect

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.annotation.IntDef
import androidx.core.net.toUri
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.gm.cleaner.R
import me.gm.cleaner.app.BaseFragment
import me.gm.cleaner.app.ConfirmationDialog
import me.gm.cleaner.app.InfoDialog
import me.gm.cleaner.client.CleanerClient
import me.gm.cleaner.client.ui.storageredirect.ConnectionState.Companion.LEVEL_ERROR
import me.gm.cleaner.client.ui.storageredirect.ConnectionState.Companion.LEVEL_INFO
import me.gm.cleaner.client.ui.storageredirect.ConnectionState.Companion.LEVEL_WARN
import me.gm.cleaner.client.ui.storageredirect.ExitMode.Companion.ASK_OR_EXIT
import me.gm.cleaner.client.ui.storageredirect.ExitMode.Companion.EXIT
import me.gm.cleaner.client.ui.storageredirect.ExitMode.Companion.SAVE_AND_EXIT
import me.gm.cleaner.client.ui.storageredirect.ExitMode.Companion.SAVE_AND_REMOUNT_AND_EXIT
import me.gm.cleaner.dao.AppLabelCache
import me.gm.cleaner.dao.PurchaseVerification
import me.gm.cleaner.dao.ServiceMoreOptionsPreferences
import me.gm.cleaner.dao.ServicePreferences
import me.gm.cleaner.databinding.StorageRedirectFragmentBinding
import me.gm.cleaner.net.NetworkConnectionState
import me.gm.cleaner.net.Website
import me.gm.cleaner.util.DividerDecoration
import me.gm.cleaner.util.FileUtils
import me.gm.cleaner.util.PermissionUtils
import me.gm.cleaner.util.SystemPropertiesUtils
import me.gm.cleaner.util.fitsSystemWindowInsets
import me.gm.cleaner.util.fixEdgeEffect
import me.gm.cleaner.util.listFormat
import me.gm.cleaner.util.overScrollIfContentScrollsPersistent
import me.gm.cleaner.util.startActivitySafe
import me.gm.cleaner.widget.recyclerview.submitDiffList

class StorageRedirectFragment : BaseFragment() {
    private val args: StorageRedirectFragmentArgs by navArgs()
    private val viewModel: StorageRedirectViewModel by viewModels()
    private val preferenceChanged: Boolean
        get() = ServicePreferences.getPackageSrZipped(args.pi.packageName) != viewModel.mountRules ||
                ServicePreferences.getPackageReadOnly(args.pi.packageName) != viewModel.readOnlyPaths

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = StorageRedirectFragmentBinding.inflate(layoutInflater)
        setAppBar(binding.root)
        val toolbar = requireToolbar()
        toolbar.post {
            toolbar.setNavigationOnClickListener { onNavigateUp(ASK_OR_EXIT) }
            toolbar.setNavigationIcon(R.drawable.ic_outline_close_24)
            toolbar.title = AppLabelCache.getPackageLabel(args.pi)
        }

        val list = binding.list
        requireAppBarLayout().setLiftOnScrollTargetView(list)
        val mountHeaderAdapter = MountHeaderAdapter()
        val buttonsAdapter = ButtonsAdapter(this, viewModel)
        val adapters = ConcatAdapter(mountHeaderAdapter, buttonsAdapter)
        list.adapter = adapters
        val layoutManager = GridLayoutManager(requireContext(), 1)
        list.layoutManager = layoutManager
        list.setHasFixedSize(true)
        list.fixEdgeEffect()
        list.overScrollIfContentScrollsPersistent()
        list.addItemDecoration(DividerDecoration(list).apply {
            setDivider(resources.getDrawable(R.drawable.list_divider_material, null))
            setAllowDividerAfterLastItem(false)
        })
        list.fitsSystemWindowInsets()

        val hintAdapter = HintCategoryAdapter()
        viewModel.hintsLiveData.observe(viewLifecycleOwner) { hints ->
            if (hints.isNotEmpty()) {
                hintAdapter.submitList(hints) {
                    adapters.addAdapter(
                        adapters.adapters.indexOfFirst { it is StatusCategoryAdapter } + 1,
                        hintAdapter
                    )
                    if (savedInstanceState == null &&
                        layoutManager.findFirstCompletelyVisibleItemPosition() == 0
                    ) {
                        list.scrollToPosition(0)
                    }
                }
            }
        }

        val mountRuleTitleAdapter = MountRuleTitleAdapter()
        val mountRulesAdapter = MountRulesAdapter(this, viewModel)
        viewModel.mountRulesLiveData.observe(viewLifecycleOwner) { mountRules ->
            mountRulesAdapter.submitDiffList(mountRules)
            toolbar.subtitle = if (preferenceChanged) getString(R.string.storage_redirect_not_saved)
            else null
        }
        val wizardAdapter = WizardAdapter(this, viewModel)

        viewModel.modeLiveData.observe(viewLifecycleOwner) { mode ->
            val dynamicAdapters = listOf(mountRuleTitleAdapter, mountRulesAdapter, wizardAdapter)
            val currentAdapters = when (mode) {
                is Mode.Welcome -> emptyList()
                is Mode.Editor -> listOf(mountRuleTitleAdapter, mountRulesAdapter)
                is Mode.Wizard -> listOf(wizardAdapter)
                else -> throw IllegalStateException()
            }
            (dynamicAdapters - currentAdapters).forEach {
                adapters.removeAdapter(it)
            }
            currentAdapters.forEachIndexed { index, adapter ->
                adapters.addAdapter(
                    adapters.adapters.indexOfFirst { it is MountHeaderAdapter } + index + 1,
                    adapter
                )
            }
            buttonsAdapter.notifyItemChanged(0)
        }
        viewModel.testLiveData.observe(viewLifecycleOwner) {
            buttonsAdapter.notifyItemChanged(0)
        }
        viewModel.mountRulesLiveData.observe(viewLifecycleOwner) {
            if (viewModel.test.isNotEmpty()) {
                buttonsAdapter.notifyItemChanged(0)
            }
        }

        if (savedInstanceState == null) {
            if (args.pi.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0 &&
                ("android.media" == args.pi.sharedUserId ||
                        args.pi.packageName.startsWith("com.") &&
                        args.pi.packageName.contains(".android.providers.media"))
            ) {
                ConfirmationDialog
                    .newInstance(getString(R.string.storage_redirect_confirm_media))
                    .apply {
                        addOnPositiveButtonClickListener {
                            it.startActivitySafe(
                                Intent(Intent.ACTION_VIEW).apply {
                                    data = Website.mediaProviderManager.toUri()
                                }
                            )
                        }
                    }
                    .show(childFragmentManager, null)
            }
            if (args.pi.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0) {
                viewModel.addHint(
                    LEVEL_ERROR, getString(R.string.storage_redirect_warn_system)
                )
            }
            if (!PermissionUtils.containsStoragePermissions(args.pi)) {
                viewModel.addHint(
                    LEVEL_INFO, getString(R.string.storage_redirect_warn_no_storage_permissions)
                )
            }
            viewModel.initSettings(args.pi)
            if (ServiceMoreOptionsPreferences.openWizardByDefault && PurchaseVerification.isExpressPro) {
                viewModel.mode = Mode.Wizard
            } else if (viewModel.mountRules.isNotEmpty()) {
                viewModel.mode = Mode.Editor
            }
        }
        viewModel.initMountWizard(args.pi)

        if (SystemPropertiesUtils.getBoolean("persist.sys.fuse", false)!! &&
            CleanerClient.zygiskEnabled && PurchaseVerification.isExpressPro
        ) {
            val readOnlyHeaderAdapter = ReadOnlyHeaderAdapter()
            val readOnlyAdapter = ReadOnlyAdapter(this, viewModel)
            viewModel.readOnlyPathsLiveData.observe(viewLifecycleOwner) { readOnlyPaths ->
                readOnlyAdapter.submitDiffList(readOnlyPaths)
                toolbar.subtitle =
                    if (preferenceChanged) getString(R.string.storage_redirect_not_saved) else null
            }
            adapters.addAdapter(readOnlyHeaderAdapter)
            adapters.addAdapter(readOnlyAdapter)
        }

        val permissions = viewModel.loadStoragePermissions(args.pi)
        if (permissions.isNotEmpty()) {
            val permissionsAdapter = PermissionsCategoryAdapter(
                permissions, args.pi.applicationInfo, viewModel
            )
            adapters.addAdapter(permissionsAdapter)
        }

        lifecycleScope.launch {
            if (viewModel.runningStatus == null) {
                viewModel.loadRunningStatusAsync(args.pi.packageName, requireContext()).await()
            }
            val runningStatus = viewModel.runningStatus
            if (runningStatus?.isNotEmpty() == true) {
                val statusAdapter = StatusCategoryAdapter(runningStatus)
                adapters.addAdapter(0, statusAdapter)
                if (savedInstanceState == null && layoutManager.findFirstCompletelyVisibleItemPosition() == 0) {
                    list.scrollToPosition(0)
                }
            }

            if (viewModel.isInMagiskDenyList == null) {
                viewModel.loadIsInMagiskDenyListAsync(args.pi.packageName).await()
                if (viewModel.isInMagiskDenyList!!) {
                    viewModel.addHint(
                        LEVEL_WARN, getString(R.string.storage_redirect_warn_denylist)
                    )
                }
            }

            if (viewModel.sharedUserIdPackages == null) {
                viewModel.loadSharedUserIdPackagesAsync(args.pi).await()
                val sharedUserIdPackages = viewModel.sharedUserIdPackages!!
                if (sharedUserIdPackages.size > 1) {
                    viewModel.addHint(
                        LEVEL_INFO, getString(
                            R.string.storage_redirect_warn_shared_userid,
                            args.pi.sharedUserId,
                            sharedUserIdPackages
                                .filter { it.packageName != args.pi.packageName }
                                .map { AppLabelCache.getPackageLabel(it) }
                                .listFormat(getString(R.string.delimiter))
                        )
                    )
                }
            }

            if (viewModel.sharedProcessPackages == null) {
                viewModel.loadSharedProcessPackagesAsync(args.pi).await()
                val sharedProcessPackages = viewModel.sharedProcessPackages!!
                if (sharedProcessPackages.size > 1) {
                    viewModel.addHint(
                        LEVEL_INFO, getString(
                            R.string.storage_redirect_warn_shared_proc,
                            args.pi.applicationInfo.processName,
                            sharedProcessPackages
                                .filter { it.packageName != args.pi.packageName }
                                .map { AppLabelCache.getPackageLabel(it) }
                                .listFormat(getString(R.string.delimiter))
                        )
                    )
                }
            }
        }

        viewModel.appTypeMarksLiveData.observe(viewLifecycleOwner) { appTypeMarks ->
            setHasOptionsMenu(false)
            setHasOptionsMenu(true)
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    onNavigateUp(ASK_OR_EXIT)
                }
            }
        )
        super.onCreateView(inflater, container, savedInstanceState)
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        when (viewModel.appTypeMarks) {
            is NetworkConnectionState.Loading -> {
                inflater.inflate(R.menu.toolbar_app_type_marks, menu)
                menu.findItem(R.id.menu_apps_type_marks).setIcon(R.drawable.outline_refresh_24)
            }

            is NetworkConnectionState.Success -> if (viewModel.appTypeMarks.getOrNull() != null) {
                inflater.inflate(R.menu.toolbar_app_type_marks, menu)
            }

            is NetworkConnectionState.Failure -> {
                inflater.inflate(R.menu.toolbar_app_type_marks, menu)
                menu.findItem(R.id.menu_apps_type_marks)
                    .setIcon(R.drawable.outline_error_outline_24)
            }
        }
        inflater.inflate(R.menu.toolbar_save, menu)
        if (PurchaseVerification.isExpressPro) {
            inflater.inflate(R.menu.toolbar_add_read_only_template, menu)
        }
        inflater.inflate(R.menu.applist_item, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.menu_save -> {
            if (viewModel.mode is Mode.Wizard) {
                viewModel.updateMountRules {
                    clear()
                    addAll(viewModel.wizard.createRules())
                    add(null to null)
                }
            }
            if (preferenceChanged) {
                if (!PurchaseVerification.isExpressPro) {
                    onNavigateUp(SAVE_AND_REMOUNT_AND_EXIT)
                } else {
                    val rules = viewModel.rules
                    val inaccessibleReadOnlyPaths = viewModel.readOnlyPaths.filter { path ->
                        rules.getAccessiblePlaces(path).isEmpty()
                    }
                    if (inaccessibleReadOnlyPaths.isNotEmpty()) {
                        ConfirmationDialog
                            .newInstance(
                                getString(
                                    R.string.storage_redirect_read_only_paths_inaccessible,
                                    inaccessibleReadOnlyPaths.joinToString("\n")
                                )
                            )
                            .apply {
                                addOnPositiveButtonClickListener {
                                    onNavigateUp(SAVE_AND_REMOUNT_AND_EXIT)
                                }
                            }
                            .show(childFragmentManager, null)
                    } else {
                        val dirOps = viewModel.wizard.getRecommendDirOps(
                            ServicePreferences.getPackageSrZipped(args.pi.packageName),
                            viewModel.mountRules
                        )
                        if (dirOps.isEmpty()) {
                            onNavigateUp(SAVE_AND_REMOUNT_AND_EXIT)
                        } else {
                            val entries = ArrayList<Pair<String, String>>(dirOps.size)
                            val moveItems = ArrayList<Boolean>(dirOps.size)
                            val checkedItems = ArrayList<Boolean>(dirOps.size)
                            dirOps.forEach { op ->
                                entries += op.from to op.to
                                moveItems += op is MountWizard.DirOp.Move
                                checkedItems += op.checkByDefault
                            }
                            RecommendDirOpsDialog
                                .newInstance(
                                    args.pi.packageName,
                                    entries,
                                    moveItems.toBooleanArray(),
                                    checkedItems.toBooleanArray()
                                )
                                .show(childFragmentManager, null)
                        }
                    }
                }
            } else if (viewModel.runningStatus?.isNotEmpty() == true &&
                viewModel.hasMountException
            ) {
                AskRemountDialog()
                    .show(childFragmentManager, null)
            } else {
                onNavigateUp(SAVE_AND_EXIT)
            }
            true
        }

        R.id.menu_apps_type_marks -> {
            when (viewModel.appTypeMarks) {
                is NetworkConnectionState.Loading -> {
                    InfoDialog
                        .newInstance(getString(R.string.loading))
                        .show(childFragmentManager, null)
                }

                is NetworkConnectionState.Success -> {
                    val appTypeMarks = viewModel.appTypeMarks.getOrNull()
                    if (appTypeMarks != null) {
                        val paths = appTypeMarks.marks.joinToString("\n")
                        val hint = when (appTypeMarks.type) {
                            AppType.COMMON -> getString(
                                R.string.storage_redirect_apps_type_marks_hint,
                                getString(R.string.storage_redirect_common_type),
                                getString(R.string.storage_redirect_common_rationale),
                                paths
                            )

                            AppType.DOWNLOAD -> getString(
                                R.string.storage_redirect_apps_type_marks_hint,
                                getString(R.string.storage_redirect_download_type),
                                getString(R.string.storage_redirect_download_rationale),
                                paths
                            )

                            AppType.ALL_FILES_ACCESS -> getString(
                                R.string.storage_redirect_apps_type_marks_hint,
                                getString(R.string.storage_redirect_all_files_access_type),
                                getString(R.string.storage_redirect_all_files_access_rationale),
                                paths
                            )

                            else -> getString(R.string.storage_redirect_apps_type_marks_err)
                        } + if (ServiceMoreOptionsPreferences.isUsingDefaultRepo) {
                            getString(R.string.storage_redirect_apps_type_marks_default_repo_hint)
                        } else {
                            ""
                        }
                        InfoDialog
                            .newInstance(hint)
                            .show(childFragmentManager, null)
                    }
                }

                is NetworkConnectionState.Failure -> {
                    InfoDialog
                        .newInstance(getString(R.string.network_connection_failed))
                        .show(childFragmentManager, null)
                }
            }
            true
        }

        R.id.menu_add_read_only_template -> {
            viewModel.updateReadOnlyPaths {
                val rules = viewModel.rules
                val mountedReadOnlyPaths =
                    ServiceMoreOptionsPreferences.editReadOnlyTemplate.asSequence()
                        .map { path ->
                            rules.getMountedPath(path)
                        }
                        .filterNot { path ->
                            FileUtils.isKnownAppDirPaths(path, args.pi.packageName)
                        }
                        .filterNot { path ->
                            viewModel.readOnlyPaths.contains(path)
                        }
                        .sorted()
                        .toList()
                addAll(size - 1, mountedReadOnlyPaths)
            }
            true
        }

        R.id.menu_delete_all_mount_rules -> {
            viewModel.updateMountRules {
                clear()
                add(null to null)
            }
            true
        }

        R.id.menu_delete_all_read_only -> {
            viewModel.updateReadOnlyPaths {
                clear()
                add(null)
            }
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    fun onNavigateUp(@ExitMode mode: Int) {
        when (mode) {
            ASK_OR_EXIT -> if (preferenceChanged) {
                ConfirmationDialog
                    .newInstance(getString(R.string.quit_without_save))
                    .apply {
                        addOnPositiveButtonClickListener {
                            val parentFragment =
                                it.requireParentFragment() as StorageRedirectFragment
                            parentFragment.onNavigateUp(EXIT)
                        }
                    }
                    .show(childFragmentManager, null)
            } else {
                findNavController().navigateUp()
            }

            SAVE_AND_EXIT -> {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        viewModel.writeMountRules()
                        viewModel.writeReadOnlyPaths()
                        viewModel.permissionToGrant.entries.forEach { (permission, grant) ->
                            if (grant != PackageManager.PERMISSION_GRANTED &&
                                viewModel.isRuntime(permission)
                            ) {
                                viewModel.setPackagePermission(
                                    args.pi.applicationInfo, permission, true
                                )
                            }
                        }
                    }
                    findNavController().navigateUp()
                }
            }

            SAVE_AND_REMOUNT_AND_EXIT -> {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        viewModel.writeMountRules()
                        viewModel.writeReadOnlyPaths()
                        viewModel.permissionToGrant.entries.forEach { (permission, grant) ->
                            if (grant != PackageManager.PERMISSION_GRANTED) {
                                viewModel.setPackagePermission(
                                    args.pi.applicationInfo, permission, true
                                )
                            }
                        }
                        if (viewModel.runningStatus?.isNotEmpty() == true) {
                            CleanerClient.service!!.remount(
                                viewModel.sharedProcessPackages!!
                                    .map { it.packageName }
                                    .toTypedArray()
                            )
                        }
                    }
                    findNavController().navigateUp()
                }
            }

            EXIT -> findNavController().navigateUp()
        }
    }
}

@IntDef(value = [ASK_OR_EXIT, SAVE_AND_EXIT, SAVE_AND_REMOUNT_AND_EXIT, EXIT])
@Retention(AnnotationRetention.SOURCE)
annotation class ExitMode {
    companion object {
        const val ASK_OR_EXIT: Int = 0
        const val SAVE_AND_EXIT: Int = 1
        const val SAVE_AND_REMOUNT_AND_EXIT: Int = 2
        const val EXIT: Int = 3
    }
}
