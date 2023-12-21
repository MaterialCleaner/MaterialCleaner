package me.gm.cleaner.app

import android.app.Application
import android.content.Context
import android.os.Build
import coil.Coil
import coil.ImageLoader
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import com.topjohnwu.superuser.Shell
import me.gm.cleaner.BuildConfig
import me.gm.cleaner.R
import me.gm.cleaner.dao.AppLabelCache
import me.gm.cleaner.dao.ManualToolsPreferences
import me.gm.cleaner.dao.RootPreferences
import me.gm.cleaner.dao.ServiceMoreOptionsPreferences
import me.gm.cleaner.dao.ServicePreferences
import me.gm.cleaner.util.LogUtils
import me.zhanghai.android.appiconloader.coil.AppIconFetcher
import me.zhanghai.android.appiconloader.coil.AppIconKeyer
import org.lsposed.hiddenapibypass.HiddenApiBypass

class App : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HiddenApiBypass.addHiddenApiExemptions("")
        }
        System.loadLibrary(String(charArrayOf('c', 'l', 'e', 'a', 'n', 'e', 'r')))
    }

    override fun onCreate() {
        super.onCreate()
        if (!BuildConfig.DEBUG && !AppCenter.isConfigured()) {
            AppCenter.start(
                this, "d835c6bf-b1ab-422b-b4c8-4c50087facfc",
                Analytics::class.java, Crashes::class.java
            )
        }
        LogUtils.init(this)
        AppLabelCache.init(this)
        val dpsContext = createDeviceProtectedStorageContext()
        RootPreferences.init(dpsContext)
        ManualToolsPreferences.init(this, dpsContext)
        ServicePreferences.init(dpsContext)
        ServiceMoreOptionsPreferences.init(dpsContext)
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_MOUNT_MASTER)
                .setTimeout(10)
        )
        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .components {
                    add(AppIconKeyer())
                    val iconSize = resources.getDimensionPixelSize(R.dimen.icon_size)
                    add(AppIconFetcher.Factory(iconSize, false, this@App))
                }
                .build()
        )
    }
}
