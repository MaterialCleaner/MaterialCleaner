package me.gm.cleaner.util

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.CallSuper
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commitNow
import me.gm.cleaner.BuildConfig
import me.gm.cleaner.app.BaseFragment

object PermissionUtils {

    fun containsStoragePermissions(pi: PackageInfo): Boolean = pi.requestedPermissions?.run {
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.R ||
                pi.applicationInfo.targetSdkVersion < Build.VERSION_CODES.R) &&
                (contains(Manifest.permission.READ_EXTERNAL_STORAGE) ||
                        contains(Manifest.permission.WRITE_EXTERNAL_STORAGE)) ||
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                contains(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
    } == true

    fun checkSelfStoragePermissions(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.R && ContextCompat.checkSelfPermission(
            context, Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED ||
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()

    fun checkSelfPostNotificationPermission(context: Context, vararg channelIds: String): Boolean =
        NotificationManagerCompat.from(context).run {
            areNotificationsEnabled() && (Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && channelIds.all { channelId ->
                getNotificationChannel(channelId)?.importance != NotificationManager.IMPORTANCE_NONE
            })
        }

    fun NotificationManagerCompat.notifySafe(
        context: Context, id: Int, notification: Notification
    ) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        notify(id, notification)
    }

    private const val TAG: String = "PermissionUtils"
    fun requestPermissions(
        fragmentManager: FragmentManager, requesterFragment: RequesterFragment
    ) {
        fragmentManager.commitNow {
            val existingFragment = fragmentManager.findFragmentByTag(TAG)
            if (existingFragment != null) {
                remove(existingFragment)
            }
            add(requesterFragment, TAG)
        }
        requesterFragment.dispatchRequestPermissions(requesterFragment.requiredPermissions, null)
    }

    fun startDetailsSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }

    fun startNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, context.applicationInfo.uid)
            putExtra("app_package", context.packageName)
            putExtra("app_uid", context.applicationInfo.uid)
        }
        context.startActivity(intent)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun startManageAllFilesAccessPermission(context: Context) {
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
        }
        context.startActivity(intent)
    }
}

/**
 * 4 status:
 * granted
 * showRationale
 * neverAsked
 * permanentlyDenied
 */
abstract class RequesterFragment : BaseFragment() {
    open val requiredPermissions: Array<String> = emptyArray()
    private lateinit var requestMultiplePermissions: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestMultiplePermissions =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
                // We should dispatch by ourselves rather than call dispatchRequestPermissions(),
                // or we'll stick in infinite recursion.
                val granted = result.filterValues { it }.keys
                if (granted.isNotEmpty()) {
                    onRequestPermissionsSuccess(granted, savedInstanceState)
                }
                val denied = result.keys - granted
                if (denied.isNotEmpty()) {
                    val shouldShowRationale = denied.filterTo(mutableSetOf()) {
                        ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), it)
                    }
                    val permanentlyDenied = denied - shouldShowRationale
                    onRequestPermissionsFailure(
                        shouldShowRationale, permanentlyDenied, true, savedInstanceState
                    )
                }
            }
    }

    @CallSuper
    open fun dispatchRequestPermissions(permissions: Array<String>, savedInstanceState: Bundle?) {
        val granted = permissions.filterTo(mutableSetOf()) {
            ActivityCompat.checkSelfPermission(requireContext(), it) ==
                    PackageManager.PERMISSION_GRANTED
        }
        if (granted.isNotEmpty()) {
            onRequestPermissionsSuccess(granted, savedInstanceState)
        }
        if (permissions.size > granted.size) {
            val denied = permissions.toSet() - granted
            val shouldShowRationale = denied.filterTo(mutableSetOf()) {
                ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), it)
            }
            if (shouldShowRationale.isNotEmpty()) {
                onRequestPermissionsFailure(
                    shouldShowRationale, emptySet(), false, savedInstanceState
                )
            } else {
                // Permissions that are never asked and that are permanently denied
                // can't be distinguished unless we actually request.
                onRequestPermissions(denied.toTypedArray(), savedInstanceState)
            }
        }
    }

    protected open fun onRequestPermissions(
        permissions: Array<String>, savedInstanceState: Bundle?
    ) {
        requestMultiplePermissions.launch(permissions)
    }

    protected open fun onRequestPermissionsSuccess(
        permissions: Set<String>, savedInstanceState: Bundle?
    ) {
    }

    protected open fun onRequestPermissionsFailure(
        shouldShowRationale: Set<String>, permanentlyDenied: Set<String>, haveAskedUser: Boolean,
        savedInstanceState: Bundle?
    ) {
        if (shouldShowRationale.isNotEmpty()) {
            onRequestPermissions(shouldShowRationale.toTypedArray(), savedInstanceState)
        } else if (permanentlyDenied.isNotEmpty()) {
            PermissionUtils.startDetailsSettings(requireContext())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requestMultiplePermissions.unregister()
    }
}
