package com.android.billingclient.api;

import androidx.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

public class BillingFlowParams {
    @NonNull
    public static final String EXTRA_PARAM_KEY_ACCOUNT_ID = "accountId";

    @NonNull
    public static Builder newBuilder() {
        throw new RuntimeException("Stub!");
    }

    public static class SubscriptionUpdateParams {

        @NonNull
        public static Builder newBuilder() {
            throw new RuntimeException("Stub!");
        }

        @Retention(RetentionPolicy.SOURCE)
        public @interface ReplacementMode {
            int UNKNOWN_REPLACEMENT_MODE = 0;
            int WITH_TIME_PRORATION = 1;
            int CHARGE_PRORATED_PRICE = 2;
            int WITHOUT_PRORATION = 3;
            int CHARGE_FULL_PRICE = 5;
            int DEFERRED = 6;
        }

        public static class Builder {

            @NonNull
            public Builder setOldPurchaseToken(@NonNull String purchaseToken) {
                throw new RuntimeException("Stub!");
            }

            /**
             * @deprecated
             */
            @Deprecated
            @NonNull
            public Builder setOldSkuPurchaseToken(@NonNull String purchaseToken) {
                throw new RuntimeException("Stub!");
            }

            @NonNull
            public Builder setOriginalExternalTransactionId(@NonNull String externalTransactionId) {
                throw new RuntimeException("Stub!");
            }

            /**
             * @deprecated
             */
            @Deprecated
            @NonNull
            public Builder setReplaceProrationMode(int replaceSkusProrationMode) {
                throw new RuntimeException("Stub!");
            }

            /**
             * @deprecated
             */
            @Deprecated
            @NonNull
            public Builder setReplaceSkusProrationMode(int replaceSkusProrationMode) {
                throw new RuntimeException("Stub!");
            }

            @NonNull
            public Builder setSubscriptionReplacementMode(int subscriptionReplacementMode) {
                throw new RuntimeException("Stub!");
            }

            @NonNull
            public SubscriptionUpdateParams build() {
                throw new RuntimeException("Stub!");
            }
        }
    }

    public static class Builder {

        @NonNull
        public Builder setIsOfferPersonalized(boolean isOfferPersonalized) {
            throw new RuntimeException("Stub!");
        }

        @NonNull
        public Builder setObfuscatedAccountId(@NonNull String obfuscatedAccountid) {
            throw new RuntimeException("Stub!");
        }

        @NonNull
        public Builder setObfuscatedProfileId(@NonNull String obfuscatedProfileId) {
            throw new RuntimeException("Stub!");
        }

        @NonNull
        public Builder setProductDetailsParamsList(@NonNull List<ProductDetailsParams> productDetailsParamsList) {
            throw new RuntimeException("Stub!");
        }

        /**
         * @deprecated
         */
        @Deprecated
        @NonNull
        public Builder setSkuDetails(@NonNull SkuDetails skuDetails) {
            throw new RuntimeException("Stub!");
        }

        @NonNull
        public Builder setSubscriptionUpdateParams(@NonNull SubscriptionUpdateParams subscriptionUpdateParams) {
            throw new RuntimeException("Stub!");
        }

        @NonNull
        public BillingFlowParams build() {
            throw new RuntimeException("Stub!");
        }
    }

    public static final class ProductDetailsParams {

        @NonNull
        public static Builder newBuilder() {
            throw new RuntimeException("Stub!");
        }

        public static class Builder {

            @NonNull
            public Builder setOfferToken(@NonNull String offerToken) {
                throw new RuntimeException("Stub!");
            }

            @NonNull
            public Builder setProductDetails(@NonNull ProductDetails productDetails) {
                throw new RuntimeException("Stub!");
            }

            @NonNull
            public ProductDetailsParams build() {
                throw new RuntimeException("Stub!");
            }
        }
    }

    /**
     * @deprecated
     */
    @Deprecated
    @Retention(RetentionPolicy.SOURCE)
    public @interface ProrationMode {
        int UNKNOWN_SUBSCRIPTION_UPGRADE_DOWNGRADE_POLICY = 0;
        int IMMEDIATE_WITH_TIME_PRORATION = 1;
        int IMMEDIATE_AND_CHARGE_PRORATED_PRICE = 2;
        int IMMEDIATE_WITHOUT_PRORATION = 3;
        int DEFERRED = 4;
        int IMMEDIATE_AND_CHARGE_FULL_PRICE = 5;
    }
}
