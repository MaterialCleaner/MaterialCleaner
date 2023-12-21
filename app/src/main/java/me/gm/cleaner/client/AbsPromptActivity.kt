package me.gm.cleaner.client

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Process
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.common.collect.ListMultimap
import com.google.common.collect.Multimaps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.gm.cleaner.R
import me.gm.cleaner.app.filepicker.FilePickerDialog
import me.gm.cleaner.client.ui.storageredirect.MimeUtils
import me.gm.cleaner.client.ui.storageredirect.MountWizard
import me.gm.cleaner.dao.AppLabelCache
import me.gm.cleaner.dao.MountRules
import me.gm.cleaner.dao.RootPreferences
import me.gm.cleaner.dao.ServicePreferences
import me.gm.cleaner.databinding.PromptDialogBinding
import me.gm.cleaner.settings.theme.ThemeUtil
import me.gm.cleaner.util.FileUtils
import me.gm.cleaner.util.FileUtils.toUserId
import me.gm.cleaner.util.getParcelableExtraCompat
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.pathString

abstract class AbsPromptActivity : AppCompatActivity() {
    private lateinit var packageInfo: PackageInfo
    private lateinit var recommendMountRuleToken: Pair<String, String>
    private lateinit var dialog: AlertDialog

    override fun attachBaseContext(newBase: Context) {
        val configuration = newBase.resources.configuration
        configuration.setLocale(RootPreferences.locale)
        AppCompatDelegate.setDefaultNightMode(ThemeUtil.getDarkTheme())
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.hasExtra(Intent.EXTRA_INTENT)) {
            /** from [ServerReceiver] */
            onReceive(intent.getParcelableExtraCompat(Intent.EXTRA_INTENT)!!)
        }
    }

    private fun createRecommendMountRule(
        packageInfo: PackageInfo, intent: Intent
    ): Pair<String, String> {
        val label = AppLabelCache.getPackageLabel(packageInfo)
        val path = intent.getStringExtra(Intent.EXTRA_STREAM)!!
        val mediaType = MimeUtils.resolveMediaType(MimeUtils.resolveMimeType(File(path)))
        val wizard = MountWizard(packageInfo)
        val recommendMountRule = when (intent.action) {
            NotificationService.ACTION_REDIRECTED_TO_INTERNAL ->
                wizard.getRecommendDirs(mediaType)[0].resolve(label).path to Path(path).parent.pathString

            NotificationService.ACTION_MEDIA_NOT_FOUND -> {
                val standardParents = FileUtils.standardDirs
                    .map { FileUtils.externalStorageDir.resolve(it) }
                val recommendAccessibleDir = standardParents.firstOrNull {
                    FileUtils.startsWith(it, path)
                }?.path ?: Path(path).parent.pathString
                recommendAccessibleDir to recommendAccessibleDir
            }

            else -> throw IllegalArgumentException()
        }

        val mountedPath = intent.getStringExtra(Intent.EXTRA_TEXT)!!
        recommendMountRuleToken = recommendMountRule.copy()
        recommendMountRuleToFilesToMigrate.put(
            recommendMountRuleToken, FilesToMigrate(path, mountedPath)
        )
        return recommendMountRule
    }

    fun onReceive(intent: Intent) {
        if (Process.myUid().toUserId() > 0) return
        packageInfo = intent.getParcelableExtraCompat(Intent.EXTRA_PACKAGE_NAME)!!
        var recommendMountRule = createRecommendMountRule(packageInfo, intent)

        val binding = PromptDialogBinding.inflate(layoutInflater)
        binding.mountRule.add1.isVisible = false
        binding.mountRule.add2.isVisible = false
        binding.mountRule.text1.text = recommendMountRule.first
        binding.mountRule.text2.text = recommendMountRule.second
        binding.mountRule.frame1.setOnClickListener {
            FilePickerDialog()
                .apply {
                    setPath(recommendMountRule.first)
                    setSelectType(FilePickerDialog.Companion.SelectType.SELECT_FOLDER)
                    addOnPositiveButtonClickListener { dir ->
                        recommendMountRule = dir to recommendMountRule.second
                        binding.mountRule.text1.text = dir
                    }
                }
                .show(supportFragmentManager, null)
        }
        binding.mountRule.frame2.setOnClickListener {
            FilePickerDialog()
                .apply {
                    setPath(recommendMountRule.second)
                    setSelectType(FilePickerDialog.Companion.SelectType.SELECT_FOLDER)
                    addOnPositiveButtonClickListener { dir ->
                        recommendMountRule = recommendMountRule.first to dir
                        binding.mountRule.text2.text = dir
                    }
                }
                .show(supportFragmentManager, null)
        }
        binding.migrate.isVisible =
            intent.action == NotificationService.ACTION_REDIRECTED_TO_INTERNAL

        val packageName = packageInfo.packageName
        val prompt = intent.getStringExtra(Intent.EXTRA_TITLE)!!
        dialog = MaterialAlertDialogBuilder(this)
            .setMessage(prompt + "\n" + getString(R.string.add_mount_rule_to_fix_prompt))
            .setView(binding.root)
            .setPositiveButton(R.string.yes) { _, _ ->
                lifecycleScope.launch {
                    val filesToMigrate = recommendMountRuleToFilesToMigrate
                        .removeAll(recommendMountRuleToken)
                    withContext(Dispatchers.IO) {
                        val wizard = MountWizard(packageInfo)
                        val answers = wizard.retrodictAnswers(
                            ServicePreferences.getPackageSrZipped(packageName)
                        )
                        answers.q2 = true
                        answers.updateMountRules {
                            when (intent.action) {
                                NotificationService.ACTION_REDIRECTED_TO_INTERNAL -> {
                                    add(size - 1, recommendMountRule)
                                }

                                NotificationService.ACTION_MEDIA_NOT_FOUND -> {
                                    add(0, recommendMountRule)
                                }
                            }
                        }
                        val rules = wizard.createRules(answers)

                        if (intent.action == NotificationService.ACTION_REDIRECTED_TO_INTERNAL &&
                            binding.migrate.isChecked
                        ) {
                            filesToMigrate.forEach { (path, mountedPath) ->
                                val newPath = MountRules(rules).getMountedPath(path)
                                CleanerClient.service!!.move(mountedPath, newPath)
                                // Also try move for origin path to support media store insert.
                                CleanerClient.service!!.move(path, newPath)
                                MediaScannerConnection.scanFile(
                                    applicationContext, arrayOf(mountedPath, path, newPath),
                                    null, null
                                )
                            }
                        }

                        updateDenyList(binding.hidelist.isChecked, packageName)

                        val sharedProcessPackages = getSharedProcessPackages(packageInfo)
                            .map { it.packageName }
                        ServicePreferences.putStorageRedirect(rules, sharedProcessPackages)
                        CleanerClient.service!!.notifySrChanged()
                        CleanerClient.service!!.remount(sharedProcessPackages.toTypedArray())
                    }
                    finish()
                }
            }
            .setNegativeButton(R.string.no) { _, _ ->
                lifecycleScope.launch {
                    val filesToMigrate = recommendMountRuleToFilesToMigrate
                        .removeAll(recommendMountRuleToken)
                    withContext(Dispatchers.IO) {
                        updateDenyList(binding.hidelist.isChecked, packageName)
                    }
                    finish()
                }
            }
            .setCancelable(false)
            .create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
    }

    private fun updateDenyList(isChecked: Boolean, packageName: String) {
        val service = CleanerClient.service!!
        val denyList = if (isChecked) {
            service.denyList + packageName
        } else {
            service.denyList - packageName
        }
        service.setDenyList(denyList.toTypedArray())
    }

    override fun onResume() {
        super.onResume()
        if (CleanerClient.service!!.denyList.contains(packageInfo.packageName) ||
            !recommendMountRuleToFilesToMigrate.containsKey(recommendMountRuleToken)
        ) {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Dismiss the dialogs to avoid the window is leaked
        if (::dialog.isInitialized) {
            dialog.dismiss()
        }
    }

    companion object {
        data class FilesToMigrate(
            val originalPath: String,
            val mountedPath: String
        )

        val recommendMountRuleToFilesToMigrate: ListMultimap<Pair<String, String>, FilesToMigrate> =
            Multimaps.newListMultimap(mutableMapOf()) { mutableListOf() }
    }
}
