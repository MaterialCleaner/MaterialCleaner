package com.android.billingclient.api;

import androidx.annotation.NonNull;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class PurchaseHistoryRecord {

    public int getQuantity() {
        throw new RuntimeException("Stub!");
    }

    public long getPurchaseTime() {
        throw new RuntimeException("Stub!");
    }

    @NonNull
    public String getDeveloperPayload() {
        throw new RuntimeException("Stub!");
    }

    @NonNull
    public String getOriginalJson() {
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

    public PurchaseHistoryRecord(@NonNull String jsonPurchaseInfo, @NonNull String signature) throws JSONException {
        throw new RuntimeException("Stub!");
    }
}
