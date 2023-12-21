package me.gm.cleaner.util

import me.gm.cleaner.BuildConfig

private const val FLAVOR_GITHUB: String = "github"
private const val FLAVOR_GOOGLEPLAY: String = "googleplay"

object BuildConfigUtils {
    val isGithubFlavor: Boolean
        get() = isGithubFlavor(BuildConfig.FLAVOR)

    fun isGithubFlavor(flavor: String): Boolean = flavor == FLAVOR_GITHUB

    val isGoogleplayFlavor: Boolean
        get() = isGoogleplayFlavor(BuildConfig.FLAVOR)

    fun isGoogleplayFlavor(flavor: String): Boolean = flavor == FLAVOR_GOOGLEPLAY
}
