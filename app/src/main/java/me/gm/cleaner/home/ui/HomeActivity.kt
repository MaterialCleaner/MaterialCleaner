package me.gm.cleaner.home.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import me.gm.cleaner.app.BaseActivity
import me.gm.cleaner.dao.RootPreferences
import me.gm.cleaner.databinding.HomeActivityBinding
import me.gm.cleaner.home.scanner.ScannerManager
import me.gm.cleaner.net.maybeBuildUpdateNotification
import me.gm.cleaner.util.BuildConfigUtils
import me.gm.cleaner.util.PermissionUtils
import me.gm.cleaner.util.RequesterFragment
import me.gm.cleaner.util.hasWifiTransport
import java.lang.ref.WeakReference

abstract class HomeActivity : BaseActivity() {

    class PostNotificationRequesterFragment : RequesterFragment() {
        override val requiredPermissions: Array<String> =
            arrayOf(Manifest.permission.POST_NOTIFICATIONS)

        override fun onRequestPermissionsFailure(
            shouldShowRationale: Set<String>, permanentlyDenied: Set<String>,
            haveAskedUser: Boolean, savedInstanceState: Bundle?
        ) {
            if (shouldShowRationale.isNotEmpty() && !haveAskedUser) {
                onRequestPermissions(shouldShowRationale.toTypedArray(), savedInstanceState)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = HomeActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionUtils.requestPermissions(
                supportFragmentManager, PostNotificationRequesterFragment()
            )
        }
        if (RootPreferences.isMonitor) {
            ScannerManager.startMonitor()
        }
        ScannerManager.activityRef = WeakReference(this)
        if (savedInstanceState == null && BuildConfigUtils.isGithubFlavor &&
            RootPreferences.isPostNotification && hasWifiTransport
        ) {
            lifecycleScope.launch {
                maybeBuildUpdateNotification(this@HomeActivity)
            }
        }
    }
}
