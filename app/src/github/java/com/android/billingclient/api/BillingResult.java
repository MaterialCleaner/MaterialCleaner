package com.android.billingclient.api;

import androidx.annotation.NonNull;

public final class BillingResult {

    public int getResponseCode() {
        throw new RuntimeException("Stub!");
    }

    @NonNull
    public static Builder newBuilder() {
        throw new RuntimeException("Stub!");
    }

    @NonNull
    public String getDebugMessage() {
        throw new RuntimeException("Stub!");
    }

    public static class Builder {

        @NonNull
        public Builder setDebugMessage(@NonNull String debugMessage) {
            throw new RuntimeException("Stub!");
        }

        @NonNull
        public Builder setResponseCode(int responseCode) {
            throw new RuntimeException("Stub!");
        }

        @NonNull
        public BillingResult build() {
            throw new RuntimeException("Stub!");
        }
    }
}
