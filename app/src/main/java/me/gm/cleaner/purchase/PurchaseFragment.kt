package me.gm.cleaner.purchase

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.danielstone.materialaboutlibrary.MaterialAboutFragment
import com.danielstone.materialaboutlibrary.holders.MaterialAboutItemViewHolder
import com.danielstone.materialaboutlibrary.items.MaterialAboutActionItem
import com.danielstone.materialaboutlibrary.items.MaterialAboutItem
import com.danielstone.materialaboutlibrary.items.MaterialAboutTitleItem
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard
import com.danielstone.materialaboutlibrary.model.MaterialAboutList
import com.danielstone.materialaboutlibrary.util.DefaultViewTypeManager
import com.danielstone.materialaboutlibrary.util.ViewTypeManager
import com.google.android.material.snackbar.Snackbar
import me.gm.cleaner.BuildConfig
import me.gm.cleaner.R
import me.gm.cleaner.app.InfoDialog
import me.gm.cleaner.dao.PurchaseVerification
import me.gm.cleaner.dao.TempCodeRecords
import me.gm.cleaner.purchase.ProductId.Companion.CLEANER_SERVICE
import me.gm.cleaner.purchase.ProductId.Companion.CLEANUP
import me.gm.cleaner.purchase.ProductId.Companion.PREMIUM_VERSION_SUBSCRIPTION
import me.gm.cleaner.purchase.cusomaboutlibrary.MaterialAboutButtonItem
import me.gm.cleaner.util.*

class PurchaseFragment : MaterialAboutFragment() {
    private val viewModel: PurchaseViewModel by purchaseActivityViewModels()

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val rootView = super.onCreateView(inflater, container, savedInstanceState)
        val list = rootView!!.findViewById<RecyclerView>(R.id.mal_recyclerview)
        list.fixEdgeEffect()
        list.overScrollIfContentScrollsPersistent(false)
        list.fitsSystemWindowInsets()
        list.adapter!!.stateRestorationPolicy =
            RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

        viewModel.purchasesResult.observe(viewLifecycleOwner) { result ->
            if (result != null) {
                setMaterialAboutList(getMaterialAboutList(requireActivity()))
                Snackbar.make(requireView(), result, Snackbar.LENGTH_SHORT).show()
            }
        }

        if (savedInstanceState == null) {
            TempCodeRecords.advance("1.10.5")
//            BuildConfigUtils.isGoogleplayFlavor &&
            val installerPackageName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                requireContext().packageManager.getInstallSourceInfo(BuildConfig.APPLICATION_ID).initiatingPackageName
            } else {
                requireContext().packageManager.getInstallerPackageName(BuildConfig.APPLICATION_ID)
            }
            if ("com.android.vending" != installerPackageName) {
                InfoDialog
                    .newInstance(getString(R.string.install_from_play))
                    .show(childFragmentManager, null)
            }
        }
        return rootView
    }

    override fun getViewTypeManager(): ViewTypeManager = object : DefaultViewTypeManager() {

        override fun getLayout(itemType: Int): Int {
            if (itemType == MaterialAboutButtonItem.BUTTON_ITEM) {
                return R.layout.mal_material_about_button_item
            }
            return super.getLayout(itemType)
        }

        override fun getViewHolder(itemType: Int, view: View?): MaterialAboutItemViewHolder {
            if (itemType == MaterialAboutButtonItem.BUTTON_ITEM) {
                return MaterialAboutButtonItem.getViewHolder(view)
            }
            return super.getViewHolder(itemType, view)
        }

        override fun setupItem(
            itemType: Int, holder: MaterialAboutItemViewHolder, item: MaterialAboutItem,
            context: Context
        ) {
            when (itemType) {
                ViewTypeManager.ItemType.TITLE_ITEM -> {
                    val h = holder as MaterialAboutTitleItem.MaterialAboutTitleItemViewHolder
                    val size = resources.getDimension(R.dimen.badge_size).toInt()
                    val margin = resources
                        .getDimension(com.danielstone.materialaboutlibrary.R.dimen.mal_baseline)
                        .toInt()
                    h.icon.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        width = size
                        height = size
                        marginStart = margin
                        marginEnd = margin
                    }
                }

                MaterialAboutButtonItem.BUTTON_ITEM -> {
                    MaterialAboutButtonItem.setupItem(
                        holder as MaterialAboutButtonItem.MaterialAboutButtonItemViewHolder,
                        item as MaterialAboutButtonItem,
                        context
                    )
                    return
                }
            }
            super.setupItem(itemType, holder, item, context)
        }
    }

    override fun getMaterialAboutList(context: Context): MaterialAboutList {
        val subscribeCard = MaterialAboutCard.Builder()
            .cardColor(context.colorBackground)
            .addItem(
                MaterialAboutTitleItem.Builder()
                    .text(R.string.purchase_subscribe_card_title)
                    .icon(R.drawable.outline_subscriptions_24)
                    .build()
            )
            .addItem(
                MaterialAboutActionItem.Builder()
                    .text(R.string.purchase_subscribe_card_summary)
                    .showIcon(false)
                    .build()
            )
            .apply {
                when {
                    BuildConfigUtils.isGithubFlavor -> {
                        // TODO: fix predicate
                        if (PurchaseVerification.isExpressPro) {
                            addItem(
                                MaterialAboutButtonItem.Builder()
                                    .text(R.string.subscribing)
                                    .enabled(false)
                                    .build()
                            )
                        } else {
                            addItem(
                                MaterialAboutButtonItem.Builder()
                                    .text(R.string.purchase_subscribe_card_button)
                                    .setOnClickAction {
                                        PreparePurchaseDialog
                                            .newInstance(PREMIUM_VERSION_SUBSCRIPTION)
                                            .show(childFragmentManager, null)
                                    }
                                    .build()
                            )
                        }
                    }

                    BuildConfigUtils.isGoogleplayFlavor -> addItem(
                        MaterialAboutButtonItem.Builder()
                            .text(R.string.purchase_unavailable_button)
                            .enabled(false)
                            .build()
                    )
                }
            }
            .build()

        val cleanupCard = MaterialAboutCard.Builder()
            .cardColor(context.colorBackground)
            .addItem(
                MaterialAboutTitleItem.Builder()
                    .text(R.string.purchase_cleanup_card_title)
                    .icon(R.drawable.outline_cleaning_services_24)
                    .build()
            )
            .addItem(
                MaterialAboutActionItem.Builder()
                    .text(R.string.purchase_cleanup_card_summary)
                    .showIcon(false)
                    .build()
            )
            .apply {
                when {
                    PurchaseVerification.isCleanupPro -> addItem(
                        MaterialAboutButtonItem.Builder()
                            .text(R.string.purchased)
                            .enabled(false)
                            .build()
                    )

                    BuildConfigUtils.isGithubFlavor -> addItem(
                        MaterialAboutButtonItem.Builder()
                            .text(R.string.purchase_unavailable_button)
                            .enabled(false)
                            .build()
                    )

                    BuildConfigUtils.isGoogleplayFlavor -> addItem(
                        MaterialAboutButtonItem.Builder()
                            .text(R.string.purchase)
                            .setOnClickAction {
                                PreparePurchaseDialog
                                    .newInstance(CLEANUP)
                                    .show(childFragmentManager, null)
                            }
                            .build()
                    )
                }
            }
            .build()

        val serviceCard = MaterialAboutCard.Builder()
            .cardColor(context.colorBackground)
            .addItem(
                MaterialAboutTitleItem.Builder()
                    .text(R.string.purchase_service_card_title)
                    .icon(R.drawable.outline_miscellaneous_services_24)
                    .build()
            )
            .addItem(
                MaterialAboutActionItem.Builder()
                    .text(R.string.purchase_service_card_summary)
                    .showIcon(false)
                    .setOnClickAction {
                        PremiumVersionFeaturesFragment()
                            .show(childFragmentManager, null)
                    }
                    .build()
            )
            .apply {
                when {
                    PurchaseVerification.isExpressPro -> addItem(
                        MaterialAboutButtonItem.Builder()
                            .text(R.string.purchased)
                            .enabled(false)
                            .build()
                    )

                    BuildConfigUtils.isGithubFlavor -> addItem(
                        MaterialAboutButtonItem.Builder()
                            .text(R.string.purchase_unavailable_button)
                            .enabled(false)
                            .build()
                    )

                    BuildConfigUtils.isGoogleplayFlavor -> addItem(
                        MaterialAboutButtonItem.Builder()
                            .text(R.string.purchase)
                            .setOnClickAction {
                                PreparePurchaseDialog
                                    .newInstance(CLEANER_SERVICE)
                                    .show(childFragmentManager, null)
                            }
                            .build()
                    )
                }
            }
            .build()

        return MaterialAboutList.Builder()
            .addCard(subscribeCard)
            .addCard(cleanupCard)
            .addCard(serviceCard)
            .build()
    }

    override fun onResume() {
        super.onResume()
        viewModel.requestQueryPurchases()
    }
}
