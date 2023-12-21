package me.gm.cleaner.app

import android.content.Context
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.ConfigurationCompat
import androidx.core.view.WindowCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.color.DynamicColors
import me.gm.cleaner.R
import me.gm.cleaner.dao.RootPreferences
import me.gm.cleaner.settings.theme.ThemeUtil

abstract class BaseActivity : AppCompatActivity() {
    private var themeKey: String = ""

    override fun attachBaseContext(newBase: Context) {
        val configuration = newBase.resources.configuration
        configuration.setLocale(RootPreferences.locale)
        AppCompatDelegate.setDefaultNightMode(ThemeUtil.getDarkTheme())
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (RootPreferences.material3) {
            setTheme(R.style.Base_AppTheme_Material3)
            DynamicColors.applyToActivityIfAvailable(this)
        } else {
            setTheme(R.style.Base_AppTheme)
            theme.applyStyle(ThemeUtil.getColorThemeStyleRes(), true)
        }
        theme.applyStyle(ThemeUtil.getNightThemeStyleRes(this), true)
        themeKey = computeThemeKey()
        super.onCreate(savedInstanceState)
        // Turn off the decor fitting system windows, which allows us to handle insets,
        // including IME animations
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    override fun setContentView(view: View) {
        super.setContentView(view)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
    }

    override fun onResume() {
        if (ConfigurationCompat.getLocales(resources.configuration)[0] != RootPreferences.locale) {
            recreate()
        } else if (themeKey != computeThemeKey()) {
            themeKey = computeThemeKey()
            recreate()
        } else {
            val themeResource = ContextThemeWrapper::class.java.getDeclaredMethod("getThemeResId")
                .invoke(this)
            if (themeResource == R.style.Base_AppTheme && RootPreferences.material3 ||
                themeResource == R.style.Base_AppTheme_Material3 && !RootPreferences.material3
            ) {
                recreate()
            }
        }
        super.onResume()
    }

    private fun computeThemeKey(): String =
        ThemeUtil.getColorTheme() + ThemeUtil.getNightTheme(this)

    override fun onSupportNavigateUp(): Boolean {
        if (!super.onSupportNavigateUp()) {
            finish()
        }
        return true
    }
}
