package com.android.billingclient.api;

import androidx.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class InAppMessageResult {

    public int getResponseCode() {
        throw new RuntimeException("Stub!");
    }

    @Nullable
    public String getPurchaseToken() {
        throw new RuntimeException("Stub!");
    }

    public InAppMessageResult(int var1, @Nullable String var2) {
        throw new RuntimeException("Stub!");
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface InAppMessageResponseCode {
        int NO_ACTION_NEEDED = 0;
        int SUBSCRIPTION_STATUS_UPDATED = 1;
    }
}
