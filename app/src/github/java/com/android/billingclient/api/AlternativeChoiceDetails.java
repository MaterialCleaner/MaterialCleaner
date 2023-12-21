package com.android.billingclient.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;

import java.util.List;

public final class AlternativeChoiceDetails {

    @NonNull
    public String getExternalTransactionToken() {
        throw new RuntimeException("Stub!");
    }

    @Nullable
    public String getOriginalExternalTransactionId() {
        throw new RuntimeException("Stub!");
    }

    @NonNull
    public List<Product> getProducts() {
        throw new RuntimeException("Stub!");
    }

    AlternativeChoiceDetails(String var1) throws JSONException {
        throw new RuntimeException("Stub!");
    }

    public static class Product {

        @NonNull
        public String getId() {
            throw new RuntimeException("Stub!");
        }

        @Nullable
        public String getOfferToken() {
            throw new RuntimeException("Stub!");
        }

        @NonNull
        public String getType() {
            throw new RuntimeException("Stub!");
        }
    }
}
