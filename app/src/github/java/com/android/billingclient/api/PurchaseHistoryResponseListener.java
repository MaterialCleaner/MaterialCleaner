package com.android.billingclient.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public interface PurchaseHistoryResponseListener {

    void onPurchaseHistoryResponse(@NonNull BillingResult var1, @Nullable List<PurchaseHistoryRecord> var2);
}
