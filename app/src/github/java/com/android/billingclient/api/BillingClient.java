package com.android.billingclient.api;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public abstract class BillingClient {

    @AnyThread
    public abstract int getConnectionState();

    @AnyThread
    @NonNull
    public static Builder newBuilder(@NonNull Context context) {
        throw new RuntimeException("Stub!");
    }

    @AnyThread
    @NonNull
    public abstract BillingResult isFeatureSupported(@NonNull String var1);

    @UiThread
    @NonNull
    public abstract BillingResult launchBillingFlow(@NonNull Activity var1, @NonNull BillingFlowParams var2);

    @UiThread
    @NonNull
    public abstract BillingResult showInAppMessages(@NonNull Activity var1, @NonNull InAppMessageParams var2, @NonNull InAppMessageResponseListener var3);

    @AnyThread
    public abstract void acknowledgePurchase(@NonNull AcknowledgePurchaseParams var1, @NonNull AcknowledgePurchaseResponseListener var2);

    @AnyThread
    public abstract void consumeAsync(@NonNull ConsumeParams var1, @NonNull ConsumeResponseListener var2);

    @AnyThread
    public abstract void endConnection();

    @AnyThread
    public abstract void queryProductDetailsAsync(@NonNull QueryProductDetailsParams var1, @NonNull ProductDetailsResponseListener var2);

    @AnyThread
    public abstract void queryPurchaseHistoryAsync(@NonNull QueryPurchaseHistoryParams var1, @NonNull PurchaseHistoryResponseListener var2);

    /**
     * @deprecated
     */
    @Deprecated
    @AnyThread
    public abstract void queryPurchaseHistoryAsync(@NonNull String var1, @NonNull PurchaseHistoryResponseListener var2);

    @AnyThread
    public abstract void queryPurchasesAsync(@NonNull QueryPurchasesParams var1, @NonNull PurchasesResponseListener var2);

    /**
     * @deprecated
     */
    @Deprecated
    @AnyThread
    public abstract void queryPurchasesAsync(@NonNull String var1, @NonNull PurchasesResponseListener var2);

    /**
     * @deprecated
     */
    @Deprecated
    @AnyThread
    public abstract void querySkuDetailsAsync(@NonNull SkuDetailsParams var1, @NonNull SkuDetailsResponseListener var2);

    @AnyThread
    public abstract void startConnection(@NonNull BillingClientStateListener var1);

    @AnyThread
    public abstract boolean isReady();

    @AnyThread
    public static final class Builder {

        @NonNull
        public Builder enableAlternativeBilling(@NonNull AlternativeBillingListener alternativeBillingListener) {
            throw new RuntimeException("Stub!");
        }

        @NonNull
        public Builder enablePendingPurchases() {
            throw new RuntimeException("Stub!");
        }

        @NonNull
        public Builder setListener(@NonNull PurchasesUpdatedListener listener) {
            throw new RuntimeException("Stub!");
        }

        @NonNull
        public BillingClient build() {
            throw new RuntimeException("Stub!");
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface ConnectionState {
        int DISCONNECTED = 0;
        int CONNECTING = 1;
        int CONNECTED = 2;
        int CLOSED = 3;
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface BillingResponseCode {
        /**
         * @deprecated
         */
        @Deprecated
        int SERVICE_TIMEOUT = -3;
        int FEATURE_NOT_SUPPORTED = -2;
        int SERVICE_DISCONNECTED = -1;
        int OK = 0;
        int USER_CANCELED = 1;
        int SERVICE_UNAVAILABLE = 2;
        int BILLING_UNAVAILABLE = 3;
        int ITEM_UNAVAILABLE = 4;
        int DEVELOPER_ERROR = 5;
        int ERROR = 6;
        int ITEM_ALREADY_OWNED = 7;
        int ITEM_NOT_OWNED = 8;
        int NETWORK_ERROR = 12;
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface FeatureType {
        @NonNull
        String SUBSCRIPTIONS = "subscriptions";
        @NonNull
        String SUBSCRIPTIONS_UPDATE = "subscriptionsUpdate";
        @NonNull
        String PRICE_CHANGE_CONFIRMATION = "priceChangeConfirmation";
        @NonNull
        String IN_APP_MESSAGING = "bbb";
        @NonNull
        String PRODUCT_DETAILS = "fff";
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface ProductType {
        @NonNull
        String INAPP = "inapp";
        @NonNull
        String SUBS = "subs";
    }

    /**
     * @deprecated
     */
    @Retention(RetentionPolicy.SOURCE)
    @Deprecated
    public @interface SkuType {
        @NonNull
        String INAPP = "inapp";
        @NonNull
        String SUBS = "subs";
    }
}
