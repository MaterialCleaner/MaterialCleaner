package me.gm.cleaner.home.ui

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.core.view.children
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.internal.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.gm.cleaner.BuildConfig
import me.gm.cleaner.R
import me.gm.cleaner.about.AboutActivity
import me.gm.cleaner.app.BaseFragment
import me.gm.cleaner.app.ConfirmationDialog
import me.gm.cleaner.app.InfoDialog
import me.gm.cleaner.client.CleanerClient
import me.gm.cleaner.dao.RootPreferences
import me.gm.cleaner.databinding.HomeFragmentBinding
import me.gm.cleaner.databinding.MtrlAlertSelectDialogItemBinding
import me.gm.cleaner.home.scanner.ScannerManager
import me.gm.cleaner.net.Website
import me.gm.cleaner.purchase.PurchaseActivity
import me.gm.cleaner.settings.SettingsActivity
import me.gm.cleaner.util.BuildConfigUtils
import me.gm.cleaner.util.FileUtils.toUserId
import me.gm.cleaner.util.PermissionUtils
import me.gm.cleaner.util.fitsSystemWindowInsets
import me.gm.cleaner.util.fixEdgeEffect
import me.gm.cleaner.util.overScrollIfContentScrollsPersistent
import me.gm.cleaner.util.startActivitySafe
import me.gm.cleaner.util.toScaledBitmap
import java.io.IOException
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date

class HomeFragment : BaseFragment() {
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var logs: List<String>
    private val handler: Handler by lazy { Handler(Looper.getMainLooper()) }
    private val timeOutRunnable: Runnable = Runnable {
        if (!CleanerClient.pingBinder()) {
            viewModel.viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    logs = Shell.cmd("logcat -d").exec().out.filter {
                        it.contains(BuildConfig.APPLICATION_ID) ||
                                it.contains(" E ") ||
                                it.contains("AndroidRuntime")
                    }
                }
                if (!isStateSaved) {
                    timeOutDialog.show(childFragmentManager, null)
                }
            }
        }
    }
    val timeOutDialog: ConfirmationDialog by lazy {
        ConfirmationDialog
            .newInstance(getString(R.string.time_out))
            .apply {
                addOnPositiveButtonClickListener {
                    val formatter = SimpleDateFormat.getDateTimeInstance()
                    val date = formatter.format(Date(System.currentTimeMillis()))
                    saveLogsLauncher?.launch("Cleaner_${BuildConfig.VERSION_NAME}_$date.log")
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = HomeFragmentBinding.inflate(inflater)
        setAppBar(binding.root)
        val toolbar = requireToolbar()
        toolbar.post {
            val emptyDrawable = AppCompatResources
                .getDrawable(requireContext(), android.R.color.transparent)
            toolbar.navigationIcon = emptyDrawable
            val navButtonView = toolbar.children.filterIsInstance<ImageButton>()
                .first { it.drawable == emptyDrawable }
            navButtonView.background = null

            val size = resources.getDimension(R.dimen.badge_size).toInt()
            val bitmap = AppCompatResources
                .getDrawable(requireContext(), R.drawable.ic_launcher_round)!!
                .toBitmap(size, size).toScaledBitmap(size, size)
            navButtonView.setImageBitmap(bitmap)
        }

        val list = binding.list
        list.adapter = ConcatAdapter(
            homeServiceAdapter(this, viewModel),
            HomeCleanerAdapter(this)
        )
        list.layoutManager = GridLayoutManager(requireContext(), 1)
        list.setHasFixedSize(true)
        list.fixEdgeEffect()
        list.overScrollIfContentScrollsPersistent()
        list.fitsSystemWindowInsets()

        viewModel.starterMessageLiveData.observe(viewLifecycleOwner) {
            when {
                it == null -> return@observe
                it == "Success" -> {
                    handler.postDelayed(timeOutRunnable, DEADLINE)
                    viewModel.starterMessage = null
                    viewModel.startedServiceOnThisLaunch = true
                }

                it.startsWith("Failure: ") -> {
                    InfoDialog
                        .newInstance(it)
                        .show(childFragmentManager, null)
                    viewModel.isShowStarterProgress = false
                    viewModel.starterMessage = null
                }
            }
        }

        saveLogsLauncher = registerForActivityResult(CreateDocument("text/*")) { uri ->
            if (uri == null) return@registerForActivityResult
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    requireContext().contentResolver.openOutputStream(uri)?.use {
                        val writer = PrintWriter(it)
                        logs.forEach { line ->
                            writer.println(line)
                        }
                        writer.flush()
                        writer.close()
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        postponeEnterTransition()
        super.onCreateView(inflater, container, savedInstanceState)
        return binding.root
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.home_toolbar, menu)
        val serviceSupport = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                !Utils.isRootImpossible() && Process.myUid().toUserId() == 0
        if (!serviceSupport) {
            menu.removeItem(R.id.action_start_or_stop)
        }
        menu.findItem(R.id.action_settings).intent = Intent(
            requireContext(), SettingsActivity::class.java
        )
        menu.findItem(R.id.action_help).intent = Intent(Intent.ACTION_VIEW).apply {
            data = Website.wiki.toUri()
        }
        menu.findItem(R.id.action_purchase).intent = Intent(
            requireContext(), PurchaseActivity::class.java
        )
        menu.findItem(R.id.action_about).intent = Intent(
            requireContext(), AboutActivity::class.java
        )
    }

    class ContactDialog : AppCompatDialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val itemToAction = listOf(
                // 0
                getString(R.string.email) to {
                    startActivitySafe(
                        Intent(Intent.ACTION_SENDTO).apply {
                            data = "mailto:".toUri()
                            putExtra(
                                Intent.EXTRA_SUBJECT, arrayOf(
                                    getString(R.string.app_name),
                                    BuildConfig.VERSION_NAME,
                                    Build.VERSION.RELEASE
                                ).joinToString(" ")
                            )
                            putExtra(Intent.EXTRA_EMAIL, arrayOf(Website.email))
                        }
                    )
                },
                // 1
                getString(R.string.github) to {
                    startActivitySafe(
                        Intent(Intent.ACTION_VIEW).apply {
                            data = Website.issues.toUri()
                        }
                    )
                },
                // 2
                if (BuildConfigUtils.isGoogleplayFlavor) {
                    getString(R.string.telegram) to {
                        startActivitySafe(
                            Intent(Intent.ACTION_VIEW).apply {
                                data = Website.telegram.toUri()
                            }
                        )
                    }
                } else {
                    getString(R.string.qq_channel) to {
                        startActivitySafe(
                            Intent(Intent.ACTION_VIEW).apply {
                                data = Website.qqChannel.toUri()
                            }
                        )
                    }
                },
            )
            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.contact)
                .setItems(itemToAction.unzip().first.toTypedArray()) { _, which ->
                    val action = which - 1 /* headerViewsCount */
                    itemToAction[action].second()
                }
                .create()

            val listView = dialog.listView
            val binding = MtrlAlertSelectDialogItemBinding.inflate(
                LayoutInflater.from(requireContext()), listView, false
            )
            val titleView = binding.text1
            if (BuildConfigUtils.isGoogleplayFlavor) {
                titleView.setText(R.string.contact_message_googleplay)
            } else {
                titleView.setText(R.string.contact_message_github)
            }
            listView.addHeaderView(titleView, null, false)
            return dialog
        }
    }

    class StopServiceDialog : AppCompatDialogFragment() {
        private val parentViewModel: HomeViewModel by viewModels({ requireParentFragment() })

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
            MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.confirm_exit)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    CleanerClient.exit()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.confirm_exit_restart) { _, _ ->
                    parentViewModel.startService(requireContext())
                }
                .create()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_start_or_stop -> {
            handler.removeCallbacks(timeOutRunnable)
            if (CleanerClient.serverVersion == BuildConfig.VERSION_CODE) {
                StopServiceDialog().show(childFragmentManager, null)
            } else {
                viewModel.startService(requireContext())
            }
            true
        }

        R.id.action_contact -> {
            ContactDialog().show(childFragmentManager, null)
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    override fun onStart() {
        super.onStart()
        if (RootPreferences.isMonitor && ScannerManager.runningShScanners.isEmpty() &&
            PermissionUtils.checkSelfStoragePermissions(requireContext()) &&
            (ScannerManager.isFileChanged || ScannerManager.inheritanceScanners.isEmpty())
        ) {
            ScannerManager.shScannersRescan()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        saveLogsLauncher?.unregister()
        saveLogsLauncher = null
    }

    companion object {
        private const val DEADLINE: Long = 3000L
        private var saveLogsLauncher: ActivityResultLauncher<String>? = null
    }
}
