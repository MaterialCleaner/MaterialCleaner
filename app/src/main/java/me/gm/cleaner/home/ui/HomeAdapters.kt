package me.gm.cleaner.home.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.os.Process
import android.os.storage.StorageManager
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.recyclerview.widget.BaseKtListAdapter
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.topjohnwu.superuser.internal.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.gm.cleaner.BuildConfig
import me.gm.cleaner.R
import me.gm.cleaner.client.CleanerClient
import me.gm.cleaner.client.ui.ServiceSettingsActivity
import me.gm.cleaner.dao.PurchaseVerification
import me.gm.cleaner.dao.RootPreferences
import me.gm.cleaner.dao.ServicePreferences
import me.gm.cleaner.databinding.HomeButtonBinding
import me.gm.cleaner.databinding.HomeCardBinding
import me.gm.cleaner.databinding.HomeCardButtonBinding
import me.gm.cleaner.databinding.HomeProgressBinding
import me.gm.cleaner.home.StaticScanner
import me.gm.cleaner.home.scanner.ScannerManager
import me.gm.cleaner.home.scanner.service.BaseScannerService
import me.gm.cleaner.net.Website
import me.gm.cleaner.util.FileUtils.toUserId
import me.gm.cleaner.util.colorBackgroundFloating
import me.gm.cleaner.util.colorError
import me.gm.cleaner.util.colorOnPrimaryContainer
import me.gm.cleaner.util.colorPrimary
import me.gm.cleaner.util.colorPrimaryContainer
import me.gm.cleaner.util.mediumAnimTime
import me.gm.cleaner.util.startActivitySafe
import me.gm.cleaner.util.textColorPrimary
import org.json.JSONObject
import java.net.URL

fun homeServiceAdapter(fragment: HomeFragment, viewModel: HomeViewModel): ConcatAdapter {
    val adapters = ConcatAdapter()

    val homeProgressAdapter by lazy { HomeProgressAdapter() }
    val homeCardAdapter = lazy { HomeCardAdapter(fragment, viewModel) }
    val homeCardButtonAdapter = lazy { HomeCardButtonAdapter(fragment) }

    viewModel.isShowStarterProgressLiveData.observe(fragment.viewLifecycleOwner) { show ->
        if (show) {
            adapters.addAdapter(0, homeProgressAdapter)
        } else {
            adapters.removeAdapter(homeProgressAdapter)
        }
    }
    viewModel.isRevalidatingLiveData.observe(fragment.viewLifecycleOwner) {
        homeCardAdapter.value.notifyItemChanged(0)
    }
    CleanerClient.serverVersionLiveData.observe(fragment.viewLifecycleOwner) { serverVersion ->
        if (serverVersion == -1) {
            if (homeCardAdapter.isInitialized()) {
                adapters.removeAdapter(homeCardAdapter.value)
            }
            if (homeCardButtonAdapter.isInitialized()) {
                adapters.removeAdapter(homeCardButtonAdapter.value)
            }
        } else {
            adapters.addAdapter(homeCardAdapter.value)
            if (serverVersion == BuildConfig.VERSION_CODE && Process.myUid().toUserId() == 0) {
                adapters.addAdapter(homeCardButtonAdapter.value)

                if (viewModel.startedServiceOnThisLaunch) {
                    viewModel.isShowStarterProgress = false
                    if (fragment.timeOutDialog.isAdded) {
                        fragment.timeOutDialog.dismiss()
                    }
                }
            }
        }
    }
    ServicePreferences.preferencesChangeLiveData.observe(fragment.viewLifecycleOwner) {
        if (homeCardButtonAdapter.isInitialized()) {
            homeCardButtonAdapter.value.notifyItemChanged(0)
        }
    }

    return adapters
}

class HomeProgressAdapter : RecyclerView.Adapter<HomeProgressAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        HomeProgressBinding.inflate(LayoutInflater.from(parent.context))
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {}

    override fun getItemCount(): Int = 1

    class ViewHolder(val binding: HomeProgressBinding) : RecyclerView.ViewHolder(binding.root)
}

class HomeCardAdapter(private val fragment: HomeFragment, private val viewModel: HomeViewModel) :
    RecyclerView.Adapter<HomeCardAdapter.ViewHolder>() {
    private val context: Context = fragment.requireContext()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        HomeCardBinding.inflate(LayoutInflater.from(parent.context))
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val serverVersion = CleanerClient.serverVersion
        binding.summary.text = fragment.getString(
            R.string.server_version,
            serverVersion,
            if (!CleanerClient.zygiskEnabled) {
                ""
            } else {
                fragment.getString(
                    R.string.zygisk_module_version,
                    CleanerClient.service!!.zygiskModuleVersion
                )
            }
        )
        when (CleanerClient.service!!.serverException) {
            0 -> when {
                serverVersion != BuildConfig.VERSION_CODE -> {
                    binding.setWarning(R.string.service_need_upgrade)
                }

                !PurchaseVerification.isExpressPro && PurchaseVerification.isLoosePro -> {
                    if (viewModel.isRevalidatingLiveData.value!!) {
                        binding.setWarning(R.string.certification_revalidating)
                    } else {
                        binding.setWarning(R.string.network_connection_failed)
                    }
                }

                else -> {
                    binding.setRunning()
                }
            }

            1 -> {
                binding.setWarning(R.string.service_zygisk_need_upgrade)
                binding.card.setOnClickListener {
                    startToDownloadZygiskModule()
                }
            }

            2 -> {
                binding.setError(R.string.service_exception_logcat_shutdown)
                binding.card.setOnClickListener {
                    startToDownloadZygiskModule()
                }
            }

            3 -> if (viewModel.startedServiceOnThisLaunch) {
                binding.setRunning()
            } else {
                binding.setError(R.string.service_exception_no_am_log)
            }

            4 -> binding.setError(R.string.service_exception_binder_dead)

            5 -> binding.setWarning(R.string.unaddressed_logs)

            6 -> binding.setError(R.string.storage_volumes_mount_exception)

            7 -> binding.setWarning(R.string.require_enable_in_lsp)
        }
    }

    private fun HomeCardBinding.setRunning() {
        if (RootPreferences.material3) {
            card.setCardBackgroundColor(context.colorPrimaryContainer)
            icon.imageTintList = ColorStateList.valueOf(context.colorOnPrimaryContainer)
            title.setTextColor(context.textColorPrimary)
            summary.setTextColor(context.textColorPrimary)
        } else {
            card.setCardBackgroundColor(context.colorPrimary)
        }
        card.isClickable = false
        icon.setImageResource(R.drawable.ic_baseline_check_circle_24)
        title.setText(R.string.service_running)
    }

    private fun HomeCardBinding.setWarning(warning: String) {
        card.setCardBackgroundColor(context.getColor(R.color.color_warning))
        card.isClickable = false
        icon.setImageResource(R.drawable.ic_baseline_info_24)
        title.text = warning
    }

    private fun HomeCardBinding.setWarning(resId: Int) {
        setWarning(fragment.getString(resId))
    }

    private fun HomeCardBinding.setError(error: String) {
        card.setCardBackgroundColor(context.colorError)
        card.isClickable = false
        icon.setImageResource(R.drawable.ic_baseline_error_24)
        title.setText(R.string.service_exception)
        summary.text = "${CleanerClient.service!!.serverPid}: $error"
    }

    private fun HomeCardBinding.setError(resId: Int) {
        setError(fragment.getString(resId))
    }

    private fun startToDownloadZygiskModule() {
        fragment.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    JSONObject(URL(Website.zygiskUpdateJson).readText())
                }
            }
            val json = result.getOrDefault(JSONObject())
            val zipUrl = json.optString("zipUrl")
            if (zipUrl.isNotEmpty()) {
                fragment.startActivitySafe(
                    Intent(Intent.ACTION_VIEW).apply {
                        data = zipUrl.toUri()
                    }
                )
            }
        }
    }

    override fun getItemCount(): Int = 1

    class ViewHolder(val binding: HomeCardBinding) : RecyclerView.ViewHolder(binding.root)
}

class HomeCardButtonAdapter(private val fragment: HomeFragment) :
    RecyclerView.Adapter<HomeCardButtonAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        HomeCardButtonBinding.inflate(LayoutInflater.from(parent.context))
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val count = ServicePreferences.srRulesCount +
                ServicePreferences.getAllReadOnly().values.sumOf { it.size }
        binding.summary.text = fragment.getString(
            R.string.enabled,
            fragment.resources.getQuantityString(
                R.plurals.enabled_rule_count, count, count
            )
        )
        binding.root.setOnClickListener {
            fragment.startActivity(
                Intent(fragment.requireContext(), ServiceSettingsActivity::class.java)
            )
        }
        if (!RootPreferences.material3) {
            binding.card.setCardBackgroundColor(binding.root.context.colorBackgroundFloating)
        }
    }

    override fun getItemCount(): Int = 1

    class ViewHolder(val binding: HomeCardButtonBinding) : RecyclerView.ViewHolder(binding.root)
}

class HomeCleanerAdapter(private val fragment: HomeFragment) :
    BaseKtListAdapter<StaticScanner, HomeCleanerAdapter.ButtonViewHolder>(CALLBACK) {
    private val activity: HomeActivity = fragment.requireActivity() as HomeActivity
    private val viewModel: HomeViewModel by fragment.viewModels()
    private val listener = object : SharedPreferences.OnSharedPreferenceChangeListener {
        override fun onSharedPreferenceChanged(
            sharedPreferences: SharedPreferences, key: String?
        ) {
            if (RootPreferences.preferences != sharedPreferences) return
            submitList(ScannerManager.staticScanners)
        }
    }

    init {
        // @see https://stackoverflow.com/questions/10150480/registeronsharedpreferencechangelistener-not-working-for-changes-made-in-differe
        RootPreferences.preferences.registerOnSharedPreferenceChangeListener(listener)
        submitList(ScannerManager.staticScanners)
    }

    override fun getItemViewType(position: Int): Int = R.layout.home_button

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ButtonViewHolder =
        ButtonViewHolder(
            HomeButtonBinding.inflate(LayoutInflater.from(parent.context))
        )

    @SuppressLint("RestrictedApi")
    override fun onBindViewHolder(holder: ButtonViewHolder, position: Int) {
        val binding = holder.binding
        val info = getItem(position)
        binding.title.setText(info.title)
        binding.icon.setImageResource(info.icon)
        binding.title.transitionName = "mTitleTextView$position"
        binding.progress.transitionName = fragment.getString(info.title)
        if (info.scannerClass != null) {
            binding.background.setOnClickListener {
                val scanner = info.newInstance()
                scanner
                    .setOnFinishListener { reason ->
                        if (RootPreferences.isMonitor && info.isShScanner) {
                            return@setOnFinishListener
                        }
                        val activity = ScannerManager.activityRef.get()!!
                        val isMaximumReached = reason == BaseScannerService.MSG_SCAN_MAXIMUM_REACHED
                        activity.lifecycleScope.launch {
                            // wait navigation
                            delay(activity.mediumAnimTime)
                            val view = activity.supportFragmentManager
                                .findFragmentById(R.id.nav_host)!!
                                .childFragmentManager
                                .fragments.first()!!
                                .requireView()
                            when (activity.findNavController(R.id.nav_host).currentDestination?.id) {
                                R.id.home_fragment -> {
                                    if (isMaximumReached) {
                                        Snackbar
                                            .make(
                                                view,
                                                R.string.maximum_reached,
                                                Snackbar.LENGTH_SHORT
                                            )
                                            .show()
                                    } else if (scanner.viewModel.trashes.isEmpty()) {
                                        Snackbar
                                            .make(
                                                view,
                                                activity.getString(
                                                    R.string.not_found,
                                                    activity.getString(info.title)
                                                ),
                                                Snackbar.LENGTH_SHORT
                                            )
                                            .show()
                                    }
                                }

                                R.id.trash_fragment -> {
                                    if (info == ScannerManager.scanners.lastOrNull()?.info) {
                                        scanner.onDestroy()
                                    }
                                    if (isMaximumReached) {
                                        Snackbar
                                            .make(
                                                view.findViewById(R.id.fab),
                                                R.string.maximum_reached,
                                                Snackbar.LENGTH_SHORT
                                            )
                                            .show()
                                    }
                                }
                            }
                        }
                    }
                    .start()
                val extras = FragmentNavigatorExtras(
                    binding.title to binding.title.transitionName,
                    binding.progress to binding.progress.transitionName
                )
                HomeToTrashNavigator(info, extras)
                    .navigate(activity.findNavController(R.id.nav_host))
            }
        }
        if (info.viewModelClass != null) {
            val scannerViewModel = ViewModelProvider(activity)[info.viewModelClass]
            scannerViewModel.trashesLiveData.observe(fragment.viewLifecycleOwner) {
                binding.text1.text = it.size.toString()
                binding.text1.isVisible =
                    scannerViewModel.isRunning || scannerViewModel.isAcceptInheritance
            }
            scannerViewModel.isAcceptInheritanceLiveData.observe(fragment.viewLifecycleOwner) {
                binding.text1.isVisible =
                    scannerViewModel.isRunning || scannerViewModel.isAcceptInheritance
            }
            scannerViewModel.progressFlow.asLiveData().observe(fragment.viewLifecycleOwner) {
                binding.progress.postOnAnimation {
                    binding.progress.progress = it
                    if (scannerViewModel.isRunning) {
                        binding.progress.show()
                    } else {
                        binding.progress.hide()
                    }
                }
            }
        }
        if (position == ScannerManager.lastRegisteredScannerPosition) {
            fragment.startPostponedEnterTransition()
        }

        when (info.title) {
            R.string.cache -> if (Utils.isRootImpossible()) {
                binding.background.setOnClickListener {
                    fragment.startActivityForResult(
                        Intent(StorageManager.ACTION_CLEAR_APP_CACHE), 0
                    )
                }
            }

            R.string.nonpublic -> {
                binding.background.setOnLongClickListener {
                    if (ScannerManager.runningShScanners.isEmpty() &&
                        viewModel.makeStandardDirs()
                    ) {
                        Snackbar.make(
                            fragment.requireView(), R.string.standard_dir, Snackbar.LENGTH_SHORT
                        ).show()
                        true
                    } else {
                        false
                    }
                }
            }
        }
    }

    class ButtonViewHolder(val binding: HomeButtonBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {
        private val CALLBACK = object : DiffUtil.ItemCallback<StaticScanner>() {
            override fun areItemsTheSame(
                oldItem: StaticScanner, newItem: StaticScanner
            ): Boolean = oldItem.title == newItem.title

            override fun areContentsTheSame(
                oldItem: StaticScanner, newItem: StaticScanner
            ): Boolean = oldItem == newItem
        }
    }
}
