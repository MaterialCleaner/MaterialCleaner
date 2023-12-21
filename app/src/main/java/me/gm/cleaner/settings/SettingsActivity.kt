package me.gm.cleaner.settings

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.commit
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.snackbar.Snackbar
import com.topjohnwu.superuser.internal.Utils
import me.gm.cleaner.R
import me.gm.cleaner.app.BaseActivity
import me.gm.cleaner.dao.RootPreferences
import me.gm.cleaner.databinding.SettingsActivityBinding
import me.gm.cleaner.home.scanner.ScannerManager
import me.gm.cleaner.net.NOTIFICATION_CHANNEL
import me.gm.cleaner.settings.theme.ThemeUtil
import me.gm.cleaner.starter.Starter
import me.gm.cleaner.util.BuildConfigUtils
import me.gm.cleaner.util.FileUtils.toUserId
import me.gm.cleaner.util.PermissionUtils
import me.gm.cleaner.util.RequesterFragment
import java.util.Locale
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

class SettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState ?: supportFragmentManager.commit {
            replace(R.id.settings, SettingsFragment())
        }
        val binding = SettingsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class PostNotificationRequesterFragment : RequesterFragment() {
        override val requiredPermissions: Array<String> =
            arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        private val preference: SwitchPreferenceCompat by lazy {
            (parentFragment as SettingsFragment)
                .findPreference(getString(R.string.post_notification_key))!!
        }
        private var rationaleShowed: Boolean = false

        override fun dispatchRequestPermissions(
            permissions: Array<String>, savedInstanceState: Bundle?
        ) {
            rationaleShowed = false
            super.dispatchRequestPermissions(permissions, savedInstanceState)
        }

        override fun onRequestPermissionsSuccess(
            permissions: Set<String>, savedInstanceState: Bundle?
        ) {
            if (permissions.contains(Manifest.permission.POST_NOTIFICATIONS)) {
                preference.isChecked = true
            }
        }

        override fun onRequestPermissionsFailure(
            shouldShowRationale: Set<String>, permanentlyDenied: Set<String>,
            haveAskedUser: Boolean, savedInstanceState: Bundle?
        ) {
            if (shouldShowRationale.isNotEmpty()) {
                if (!haveAskedUser) {
                    rationaleShowed = true
                    onRequestPermissions(shouldShowRationale.toTypedArray(), savedInstanceState)
                }
            } else if (permanentlyDenied.isNotEmpty() && !rationaleShowed) {
                PermissionUtils.startNotificationSettings(requireContext())
            }
        }
    }

    class SettingsFragment : BaseSettingsFragment() {

        @SuppressLint("RestrictedApi")
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.setStorageDeviceProtected()
            addPreferencesFromResource(R.xml.root_preferences)

            val isStartOnBoot = findPreference<Preference>(getString(R.string.start_on_boot_key))!!
            isStartOnBoot.isVisible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    !Utils.isRootImpossible() && Process.myUid().toUserId() == 0
            isStartOnBoot.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, newValue ->
                    try {
                        if (newValue as Boolean) {
                            Starter.writeSourceDir(requireContext())
                        } else {
                            Starter.deleteSourceDir(requireContext())
                        }
                    } catch (e: Throwable) {
                        Snackbar
                            .make(requireView(), e.message.toString(), Snackbar.LENGTH_SHORT)
                            .show()
                        return@OnPreferenceChangeListener false
                    }
                    true
                }

            val postNotification =
                findPreference<SwitchPreferenceCompat>(getString(R.string.post_notification_key))!!
            postNotification.isVisible = BuildConfigUtils.isGithubFlavor
            postNotification.isChecked = RootPreferences.isPostNotification &&
                    PermissionUtils.checkSelfPostNotificationPermission(
                        requireContext(), NOTIFICATION_CHANNEL
                    )
            postNotification.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, newValue ->
                    val hasPermission = PermissionUtils.checkSelfPostNotificationPermission(
                        requireContext(), NOTIFICATION_CHANNEL
                    )
                    if (newValue as Boolean && !hasPermission) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            PermissionUtils.requestPermissions(
                                childFragmentManager, PostNotificationRequesterFragment()
                            )
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            PermissionUtils.startNotificationSettings(requireContext())
                        }
                    }
                    hasPermission
                }

            findPreference<Preference>(getString(R.string.no_tick_key))?.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, newValue ->
                    if ((newValue as String).isNotEmpty()) {
                        try {
                            Pattern.compile(newValue)
                        } catch (e: PatternSyntaxException) {
                            Toast.makeText(requireActivity(), e.message, Toast.LENGTH_SHORT).show()
                            return@OnPreferenceChangeListener false
                        }
                    }
                    if (RootPreferences.isMonitor) {
                        ScannerManager.shScanners.forEach { it.onDestroy() }
                    }
                    true
                }

            findPreference<Preference>(getString(R.string.no_scan_key))?.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, _ ->
                    if (RootPreferences.isMonitor) {
                        ScannerManager.shScanners.forEach { it.onDestroy() }
                    }
                    true
                }

            findPreference<Preference>(getString(R.string.monitor_key))?.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, newValue ->
                    if (newValue as Boolean) {
                        ScannerManager.startMonitor()
                    } else {
                        ScannerManager.stopMonitor()
                        ScannerManager.shScanners.forEach { it.onDestroy() }
                    }
                    true
                }

            findPreference<Preference>(getString(R.string.length_key))?.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, _ ->
                    ScannerManager.unregisterAll()
                    true
                }

            val language = findPreference<ListPreference>(getString(R.string.language_key))!!
            language.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, _ ->
                    ActivityCompat.recreate(requireActivity())
                    true
                }
            val entries = language.entries
            val userLocale = RootPreferences.locale
            val isFollowSystem = "SYSTEM" == language.value
            var summary: String? = null
            for (i in 1 until entries.size) {
                val locale = Locale.forLanguageTag(entries[i].toString())
                val localeName = if (!locale.script.isNullOrEmpty()) {
                    locale.getDisplayScript(locale)
                } else {
                    locale.getDisplayName(locale)
                }
                val localeNameUser = if (!locale.script.isNullOrEmpty()) {
                    locale.getDisplayScript(userLocale)
                } else {
                    locale.getDisplayName(userLocale)
                }
                if (!isFollowSystem && localeName == localeNameUser) {
                    summary = localeName
                    entries[i] = localeName
                } else {
                    entries[i] = "$localeName - $localeNameUser"
                }
            }
            language.summary = if (isFollowSystem) {
                getString(R.string.follow_system)
            } else {
                summary
            }

            findPreference<Preference>(getString(R.string.theme_color_key))?.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, _ ->
                    ActivityCompat.recreate(requireActivity())
                    true
                }

            findPreference<Preference>(getString(R.string.theme_m3_key))?.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, _ ->
                    ActivityCompat.recreate(requireActivity())
                    true
                }

            findPreference<Preference>(getString(R.string.dark_theme_key))?.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, newValue ->
                    if (RootPreferences.preferences.getString(
                            getString(R.string.dark_theme_key), ThemeUtil.MODE_NIGHT_FOLLOW_SYSTEM
                        ) != newValue
                    ) {
                        ActivityCompat.recreate(requireActivity())
                    }
                    true
                }

            findPreference<Preference>(getString(R.string.black_dark_theme_key))?.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, _ ->
                    ActivityCompat.recreate(requireActivity())
                    true
                }
        }
    }
}
