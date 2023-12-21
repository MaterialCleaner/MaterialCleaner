package me.gm.cleaner.client

import android.os.Bundle
import com.google.android.material.color.DynamicColors
import me.gm.cleaner.R
import me.gm.cleaner.dao.RootPreferences
import me.gm.cleaner.settings.theme.ThemeUtil

class PromptActivity : AbsPromptActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        if (RootPreferences.material3) {
            setTheme(R.style.TransparentBackground_Material3)
            DynamicColors.applyToActivityIfAvailable(this)
        } else {
            setTheme(R.style.TransparentBackground)
            theme.applyStyle(ThemeUtil.getColorThemeStyleRes(), true)
        }
        theme.applyStyle(ThemeUtil.getNightThemeStyleRes(this), true)
        super.onCreate(savedInstanceState)
    }
}
