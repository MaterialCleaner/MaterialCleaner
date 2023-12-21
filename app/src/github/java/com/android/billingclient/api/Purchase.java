package com.android.billingclient.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

public class Purchase {

    public int getPurchaseState() {
        throw new RuntimeException("Stub!");
    }

    public int getQuantity() {
        throw new RuntimeException("Stub!");
    }

    public long getPurchaseTime() {
        throw new RuntimeException("Stub!");
    }

    @Nullable
    public AccountIdentifiers getAccountIdentifiers() {
        throw new RuntimeException("Stub!");
    }

    @NonNull
    public String getDeveloperPayload() {
        throw new RuntimeException("Stub!");
    }

    @Nullable
    public String getOrderId() {
        throw new RuntimeException("Stub!");
    }

    @NonNull
    public String getOriginalJson() {
        throw new RuntimeException("Stub!");
    }

    @NonNull
    public String getPackageName() {
        throw new RuntimeException("Stub!");
    }

    @NonNull
    public String getPurchaseToken() {
        throw new RuntimeException("Stub!");
    }

    @NonNull
    public String getSignature() {
        throw new RuntimeException("Stub!");
    }

    /**
     * @deprecated
     */
    @Deprecated
    @NonNull
    public ArrayList<String> getSkus() {
        throw new RuntimeException("Stub!");
    }

    @NonNull
    public List<String> getProducts() {
        throw new RuntimeException("Stub!");
    }

    public Purchase(@NonNull String jsonPurchaseInfo, @NonNull String signature) throws JSONException {
        throw new RuntimeException("Stub!");
    }

    public boolean isAcknowledged() {
        throw new RuntimeException("Stub!");
    }

    public boolean isAutoRenewing() {
        throw new RuntimeException("Stub!");
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface PurchaseState {
        int UNSPECIFIED_STATE = 0;
        int PURCHASED = 1;
        int PENDING = 2;
    }
}
