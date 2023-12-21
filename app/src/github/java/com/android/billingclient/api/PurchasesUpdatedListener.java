package com.android.billingclient.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public interface PurchasesUpdatedListener {

    void onPurchasesUpdated(@NonNull BillingResult var1, @Nullable List<Purchase> var2);
}
