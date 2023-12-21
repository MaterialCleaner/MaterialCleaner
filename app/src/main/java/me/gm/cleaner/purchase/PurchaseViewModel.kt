package me.gm.cleaner.purchase

import android.app.Activity
import androidx.annotation.IntDef
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.fragment.app.createViewModelLazy
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import me.gm.cleaner.purchase.ProductId.Companion.CLEANER_SERVICE
import me.gm.cleaner.purchase.ProductId.Companion.CLEANUP
import me.gm.cleaner.purchase.ProductId.Companion.PREMIUM_VERSION_SUBSCRIPTION

@MainThread
inline fun Fragment.purchaseActivityViewModels(
    noinline extrasProducer: (() -> CreationExtras)? = null,
    noinline factoryProducer: (() -> ViewModelProvider.Factory)? = null
): Lazy<PurchaseViewModel> = createViewModelLazy(
    EventPurchaseViewModel::class, { requireActivity().viewModelStore },
    { extrasProducer?.invoke() ?: requireActivity().defaultViewModelCreationExtras },
    factoryProducer ?: { requireActivity().defaultViewModelProviderFactory }
)

interface PurchaseViewModel {
    // For PurchaseFragment
    val purchasesResult: LiveData<String?>

    fun requestQueryPurchases()

    // For PreparePurchaseDialog
    val connectionState: LiveData<ServerConnectionState>

    fun waitOrStartServerConnection(@ProductId productId: Int)

    fun launchPurchaseFlow(activity: Activity)

    fun endServerConnection()
}

sealed class ServerConnectionState {
    data object Connecting : ServerConnectionState()
    data object AlreadyOwned : ServerConnectionState()
    data object GoPurchase : ServerConnectionState()
    data class RecoverableError(val message: String) : ServerConnectionState()
    data class Error(val message: String) : ServerConnectionState()
}

@IntDef(value = [PREMIUM_VERSION_SUBSCRIPTION, CLEANUP, CLEANER_SERVICE])
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
annotation class ProductId {
    companion object {
        const val PREMIUM_VERSION_SUBSCRIPTION: Int = 0
        const val CLEANUP: Int = 1
        const val CLEANER_SERVICE: Int = 2
    }
}
