package com.android.billingclient.api;

import androidx.annotation.NonNull;

import java.util.List;

public interface ProductDetailsResponseListener {

    void onProductDetailsResponse(@NonNull BillingResult var1, @NonNull List<ProductDetails> var2);
}
