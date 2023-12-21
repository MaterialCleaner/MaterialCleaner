package com.android.billingclient.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/**
 * @deprecated
 */
@Deprecated
public interface SkuDetailsResponseListener {

    void onSkuDetailsResponse(@NonNull BillingResult var1, @Nullable List<SkuDetails> var2);
}
