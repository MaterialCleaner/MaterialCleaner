package me.gm.cleaner.purchase

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.MutableStateFlow
import me.gm.cleaner.R
import me.gm.cleaner.dao.TempCodeRecords

class EventPurchaseViewModel(private val application: Application) :
    AndroidViewModel(application), PurchaseViewModel {
    private val purchaseSuccess: String by lazy { application.getString(R.string.purchase_success) }

    // For PurchaseFragment
    private val _purchasesResult: MutableLiveData<String?> = MutableLiveData(null)
    override val purchasesResult: LiveData<String?>
        get() = _purchasesResult

    override fun requestQueryPurchases() {}

    // For PreparePurchaseDialog
    private val _connectionState: MutableStateFlow<ServerConnectionState> =
        MutableStateFlow(ServerConnectionState.Connecting)
    override val connectionState: LiveData<ServerConnectionState> = _connectionState.asLiveData()

    override fun waitOrStartServerConnection(@ProductId productId: Int) {
        TempCodeRecords.advance("1.10.5")
        if (productId == 0 || productId == 1) {
            _connectionState.tryEmit(ServerConnectionState.AlreadyOwned)
            return
        }

        _connectionState.tryEmit(ServerConnectionState.Connecting)
        queryProductDetailsAndPurchases(productId)
    }

    private fun queryProductDetailsAndPurchases(@ProductId productId: Int) {
    }

    override fun launchPurchaseFlow(activity: Activity) {
        try {
        } catch (e: Throwable) {
            _purchasesResult.postValue(e.message)
        }
    }

    override fun endServerConnection() {}
}
