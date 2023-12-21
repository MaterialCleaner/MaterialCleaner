package com.android.billingclient.api;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * @deprecated
 */
@Deprecated
public class SkuDetailsParams {

    @NonNull
    public static Builder newBuilder() {
        throw new RuntimeException("Stub!");
    }

    @NonNull
    public String getSkuType() {
        throw new RuntimeException("Stub!");
    }

    @NonNull
    public List<String> getSkusList() {
        throw new RuntimeException("Stub!");
    }

    public static class Builder {

        @NonNull
        public Builder setSkusList(@NonNull List<String> skusList) {
            throw new RuntimeException("Stub!");
        }

        @NonNull
        public Builder setType(@NonNull String type) {
            throw new RuntimeException("Stub!");
        }

        @NonNull
        public SkuDetailsParams build() {
            throw new RuntimeException("Stub!");
        }
    }
}
