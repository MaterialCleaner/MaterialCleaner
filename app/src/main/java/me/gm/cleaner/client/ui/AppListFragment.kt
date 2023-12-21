package me.gm.cleaner.client.ui

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.MenuCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.gm.cleaner.R
import me.gm.cleaner.app.ConfirmationDialog
import me.gm.cleaner.client.CleanerClient
import me.gm.cleaner.dao.ServicePreferences
import me.gm.cleaner.databinding.ApplistFragmentBinding
import me.gm.cleaner.util.LogUtils
import me.gm.cleaner.util.buildStyledTitle
import me.gm.cleaner.util.colorAccent
import me.gm.cleaner.util.fitsSystemWindowInsets
import me.gm.cleaner.util.fixEdgeEffect
import me.gm.cleaner.util.overScrollIfContentScrollsPersistent
import me.gm.cleaner.util.submitListKeepPosition
import me.gm.cleaner.widget.ThemedTabBorderSwipeRefreshLayout
import me.gm.cleaner.widget.recyclerview.fastscroll.useThemeStyle
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import java.io.IOException
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date

class AppListFragment : BaseServiceSettingsFragment() {
    override val viewModel: AppListViewModel by viewModels()
    private lateinit var saveLogsLauncher: ActivityResultLauncher<String>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = ApplistFragmentBinding.inflate(inflater)

        val adapter = AppListAdapter(this).apply {
            stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
        val list = binding.list
        list.adapter = adapter
        list.layoutManager = GridLayoutManager(requireContext(), 1)
        list.setHasFixedSize(true)
        val fastScroll = FastScrollerBuilder(list)
            .useThemeStyle(requireContext())
            .build()
        list.fixEdgeEffect()
        list.overScrollIfContentScrollsPersistent()
        list.fitsSystemWindowInsets(fastScroll, savedInstanceState == null)
        binding.listContainer.setOnRefreshListener {
            viewModel.loadApps()
        }
        if (viewModel.isDone) {
            binding.progress.hide()
        }

        viewModel.uninstalledPackagesLiveData.observe(viewLifecycleOwner) { mutableUninstalledPackages ->
            if (mutableUninstalledPackages.isNotEmpty()) {
                val uninstalledPackages = mutableUninstalledPackages.toList()
                (mutableUninstalledPackages as MutableList).clear()
                ConfirmationDialog
                    .newInstance(
                        resources.getQuantityString(
                            R.plurals.uninstalled_package, uninstalledPackages.size,
                            uninstalledPackages.joinToString(getString(R.string.delimiter))
                        )
                    )
                    .apply {
                        addOnPositiveButtonClickListener {
                            ServicePreferences.removeStorageRedirect(uninstalledPackages)
                            ServicePreferences.removeReadOnly(uninstalledPackages)
                            val service = CleanerClient.service!!
                            val denyList = service.denyList - uninstalledPackages.toSet()
                            service.setDenyList(denyList.toTypedArray())
                        }
                    }
                    .show(childFragmentManager, null)
            }
        }
        viewModel.appsFlow.asLiveData().observe(viewLifecycleOwner) { apps ->
            when (apps) {
                is AppListState.Done -> {
                    val hideProgress = Runnable {
                        binding.progress.hide()
                        binding.listContainer.isEnabled = true
                        binding.listContainer.isRefreshing = false
                    }
                    if (binding.listContainer.isRefreshing) {
                        adapter.submitListKeepPosition(list, apps.list, hideProgress)
                    } else {
                        adapter.submitList(apps.list, hideProgress)
                    }
                }

                else -> {}
            }
        }
        ServicePreferences.preferencesChangeLiveData.observe(viewLifecycleOwner) {
            viewModel.updateAppsRuleCount()
        }

        saveLogsLauncher = registerForActivityResult(
            ActivityResultContracts.CreateDocument("text/*")
        ) { uri ->
            if (uri == null) return@registerForActivityResult
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    requireContext().contentResolver.openOutputStream(uri)?.use {
                        val writer = PrintWriter(it)
                        Shell.cmd("logcat -d").exec().out.forEach { line ->
                            writer.println(line)
                        }
                        writer.flush()
                        writer.close()
                    }
                } catch (e: IOException) {
                    LogUtils.handleThrowable(e)
                }
            }
        }
        super.onCreateView(inflater, container, savedInstanceState)
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.applist_toolbar, menu)
        val menuView = menu.findItem(R.id.menu_view).subMenu!!
        MenuCompat.setGroupDividerEnabled(menuView, true)
        super.onCreateOptionsMenu(menu, inflater)
        when (ServicePreferences.sortBy) {
            ServicePreferences.SORT_BY_NAME ->
                menu.findItem(R.id.menu_sort_by_name).isChecked = true

            ServicePreferences.SORT_BY_UPDATE_TIME ->
                menu.findItem(R.id.menu_sort_by_update_time).isChecked = true
        }
        menu.findItem(R.id.menu_rule_count).isChecked = ServicePreferences.ruleCount
        menu.findItem(R.id.menu_mount_state).isChecked = ServicePreferences.mountState
        menu.findItem(R.id.menu_hide_system_app).isChecked = ServicePreferences.isHideSystemApp
        menu.findItem(R.id.menu_hide_disabled_app).isChecked = ServicePreferences.isHideDisabledApp
        menu.findItem(R.id.menu_hide_no_storage_permissions).isChecked =
            ServicePreferences.isHideNoStoragePermissionApp
        arrayOf(
            menu.findItem(R.id.menu_header_sort), menu.findItem(R.id.menu_header_hide)
        ).forEach {
            it.title = requireContext().buildStyledTitle(
                it.title!!,
                androidx.appcompat.R.attr.textAppearancePopupMenuHeader,
                requireContext().colorAccent
            )
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_sort_by_name -> {
                item.isChecked = true
                ServicePreferences.sortBy = ServicePreferences.SORT_BY_NAME
            }

            R.id.menu_sort_by_update_time -> {
                item.isChecked = true
                ServicePreferences.sortBy = ServicePreferences.SORT_BY_UPDATE_TIME
            }

            R.id.menu_rule_count -> {
                val ruleCount = !item.isChecked
                item.isChecked = ruleCount
                ServicePreferences.ruleCount = ruleCount
            }

            R.id.menu_mount_state -> {
                val mountState = !item.isChecked
                item.isChecked = mountState
                ServicePreferences.mountState = mountState
            }

            R.id.menu_hide_system_app -> {
                val isHideSystemApp = !item.isChecked
                item.isChecked = isHideSystemApp
                ServicePreferences.isHideSystemApp = isHideSystemApp
            }

            R.id.menu_hide_disabled_app -> {
                val isHideDisabledApp = !item.isChecked
                item.isChecked = isHideDisabledApp
                ServicePreferences.isHideDisabledApp = isHideDisabledApp
            }

            R.id.menu_hide_no_storage_permissions -> {
                val isHideNoStoragePermissionApp = !item.isChecked
                item.isChecked = isHideNoStoragePermissionApp
                ServicePreferences.isHideNoStoragePermissionApp = isHideNoStoragePermissionApp
            }

            R.id.menu_refresh -> {
                requireView().findViewById<ThemedTabBorderSwipeRefreshLayout>(R.id.list_container)
                    .isRefreshing = true
                viewModel.loadApps()
            }

            R.id.menu_logcat -> {
                val formatter = SimpleDateFormat.getDateTimeInstance()
                val date = formatter.format(Date(System.currentTimeMillis()))
                saveLogsLauncher.launch("logcat_${Build.VERSION.SDK_INT}_$date.log")
            }

            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        saveLogsLauncher.unregister()
    }
}
