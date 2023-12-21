package me.gm.cleaner.net

import me.gm.cleaner.dao.RootPreferences
import java.util.Locale

object Website {
    const val latestReleases: String =
        "https://github.com/MaterialCleaner/MaterialCleaner/releases/latest"
    const val latestReleasesApi: String =
        "https://api.github.com/repos/MaterialCleaner/MaterialCleaner/releases/latest"
    const val wiki: String =
        "https://github.com/MaterialCleaner/MaterialCleaner/wiki"
    const val email: String =
        "greenmushroomcn@gmail.com"
    const val issues: String =
        "https://github.com/MaterialCleaner/MaterialCleaner/issues/new/choose"
    const val telegram: String =
        "https://t.me/TabSwitch"
    const val qqChannel: String =
        "https://pd.qq.com/s/bby64wnb4"
    const val appsTypeMarksRepo: String =
        "https://github.com/MaterialCleaner/AppsTypeMarks/fork"
    const val mediaProviderManager: String =
        "https://github.com/MaterialCleaner/Media-Provider-Manager"
    const val zygiskUpdateJson: String =
        "https://materialcleaner.github.io/MaterialCleaner/releases/module.json"
    val wikiInstallZygiskModule: String
        get() = LocaleAdapter(
            Locale.US to {
                "https://github.com/MaterialCleaner/MaterialCleaner/wiki/Install-Zygisk-Module"
            },
            Locale.SIMPLIFIED_CHINESE to {
                "https://github.com/MaterialCleaner/MaterialCleaner/wiki/Zygisk-%E6%A8%A1%E5%9D%97%E5%AE%89%E8%A3%85"
            }
        ).getContentForLocale(RootPreferences.locale)
}
