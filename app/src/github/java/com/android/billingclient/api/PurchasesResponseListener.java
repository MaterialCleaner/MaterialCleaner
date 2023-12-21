package com.android.billingclient.api;

import androidx.annotation.NonNull;

import java.util.List;

public interface PurchasesResponseListener {

    void onQueryPurchasesResponse(@NonNull BillingResult var1, @NonNull List<Purchase> var2);
}
