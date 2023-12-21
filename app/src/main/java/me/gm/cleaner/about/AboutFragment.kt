package me.gm.cleaner.about

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.danielstone.materialaboutlibrary.ConvenienceBuilder
import com.danielstone.materialaboutlibrary.MaterialAboutFragment
import com.danielstone.materialaboutlibrary.items.MaterialAboutActionItem
import com.danielstone.materialaboutlibrary.items.MaterialAboutTitleItem
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard
import com.danielstone.materialaboutlibrary.model.MaterialAboutList
import me.gm.cleaner.R
import me.gm.cleaner.util.BuildConfigUtils
import me.gm.cleaner.util.collatorComparator
import me.gm.cleaner.util.colorBackground
import me.gm.cleaner.util.fitsSystemWindowInsets
import me.gm.cleaner.util.fixEdgeEffect
import me.gm.cleaner.util.overScrollIfContentScrollsPersistent

class AboutFragment : MaterialAboutFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val rootView = super.onCreateView(inflater, container, savedInstanceState)
        val list = rootView!!.findViewById<RecyclerView>(R.id.mal_recyclerview)
        list.fixEdgeEffect()
        list.overScrollIfContentScrollsPersistent()
        list.fitsSystemWindowInsets()
        list.adapter!!.stateRestorationPolicy =
            RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        return rootView
    }

    override fun getMaterialAboutList(context: Context): MaterialAboutList {
        val appCard = MaterialAboutCard.Builder()
            .cardColor(context.colorBackground)
            .addItem(
                MaterialAboutTitleItem.Builder()
                    .text(R.string.app_name)
                    .desc(R.string.slogan)
                    .icon(R.drawable.ic_launcher_round)
                    .build()
            )
            .addItem(
                ConvenienceBuilder.createVersionActionItem(
                    context,
                    AppCompatResources.getDrawable(context, R.drawable.outline_info_24),
                    getString(R.string.version),
                    false
                )
            )
            .addItem(
                MaterialAboutActionItem.Builder()
                    .text(R.string.privacy_policy)
                    .icon(R.drawable.outline_privacy_tip_24)
                    .setOnClickAction(
                        ConvenienceBuilder.createWebViewDialogOnClickAction(
                            context,
                            getString(R.string.privacy_policy),
                            "https://materialcleaner.github.io/MaterialCleaner/privacypolicy",
                            true,
                            false
                        )
                    )
                    .build()
            )
            .build()

        val authorCard = MaterialAboutCard.Builder()
            .cardColor(context.colorBackground)
            .title(R.string.developer)
            .addItem(
                MaterialAboutActionItem.Builder()
                    .text("GM")
                    .icon(R.drawable.avator)
                    .setOnClickAction(
                        ConvenienceBuilder.createWebsiteOnClickAction(
                            context, "https://github.com/GuhDoy".toUri()
                        )
                    )
                    .build()
            )
            .build()

        val licenses = listOfNotNull(
            // app
            license("https://github.com/abseil/abseil-cpp", Apache2),
            license("https://github.com/androidx/androidx", Apache2),
            license("https://github.com/brevent/genuine", BYSA4),
            license("https://github.com/coil-kt/coil", Apache2),
            license("https://github.com/daniel-stoneuk/material-about-library", Apache2),
            license("https://github.com/facebook/zstd", `3BSD`),
            license("https://github.com/google/flatbuffers", Apache2),
            license("https://github.com/google/guava", Apache2),
            license("https://github.com/JetBrains/kotlin", Apache2),
            license("https://github.com/Kotlin/kotlinx.coroutines", Apache2),
            license("https://github.com/LSPosed/AndroidHiddenApiBypass", Apache2),
            license("https://github.com/ltttttttttttt/ComposeViews", Apache2),
            license("https://github.com/lz4/lz4", `2BSD`),
            license("https://github.com/material-components/material-components-android", Apache2),
            if (BuildConfigUtils.isGithubFlavor) {
                license("https://github.com/microsoft/appcenter-sdk-android", MIT)
            } else {
                null
            },
            license("https://github.com/nanihadesuka/LazyColumnScrollbar", MIT),
            license("https://github.com/RikkaApps/RikkaX", MIT),
            license("https://github.com/takisoft/preferencex-android", Apache2),
            license("https://github.com/tensorflow/tensorflow", Apache2),
            license("https://github.com/topjohnwu/libsu", Apache2),
            license("https://github.com/zhanghai/AndroidFastScroll", Apache2),
            license("https://github.com/zhanghai/AppIconLoader", Apache2),
            // module
            license("https://github.com/topjohnwu/zygisk-module-sample", `0BSD`),
        ).sortedWith(collatorComparator { it.text.toString() })
        val licensesCard = MaterialAboutCard.Builder()
            .cardColor(context.colorBackground)
            .title(R.string.licenses)
            .addItems(licenses)
            .build()

        return MaterialAboutList.Builder()
            .addCard(appCard)
            .addCard(authorCard)
            .addCard(licensesCard)
            .build()
    }

    fun license(url: String, license: String?): MaterialAboutActionItem {
        val split = url.split('/')
        val size = split.size
        val name = split[size - 1]
        return MaterialAboutActionItem.Builder()
            .text(name)
            .subText(license)
            .showIcon(false)
            .setOnClickAction(
                ConvenienceBuilder.createWebsiteOnClickAction(context, url.toUri())
            )
            .build()
    }

    companion object {
        private const val Apache2: String = "Apache License 2.0"
        private const val MIT: String = "MIT License"
        private const val LGPL3: String = "GNU Lesser General Public License v3.0"
        private const val BYSA4: String = "Attribution-ShareAlike 4.0 International"
        private const val `0BSD`: String = "Zero-Clause BSD"
        private const val `2BSD`: String = "The 2-Clause BSD License"
        private const val `3BSD`: String = "The 3-Clause BSD License"
    }
}
