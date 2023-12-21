package com.android.billingclient.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

public final class ProductDetails {

    @Nullable
    public OneTimePurchaseOfferDetails getOneTimePurchaseOfferDetails() {
        throw new RuntimeException("Stub!");
    }

    @NonNull
    public String getDescription() {
        throw new RuntimeException("Stub!");
    }

    @NonNull
    public String getName() {
        throw new RuntimeException("Stub!");
    }

    @NonNull
    public String getProductId() {
        throw new RuntimeException("Stub!");
    }

    @NonNull
    public String getProductType() {
        throw new RuntimeException("Stub!");
    }

    @NonNull
    public String getTitle() {
        throw new RuntimeException("Stub!");
    }

    @Nullable
    public List<SubscriptionOfferDetails> getSubscriptionOfferDetails() {
        throw new RuntimeException("Stub!");
    }

    ProductDetails(String var1) throws JSONException {
        throw new RuntimeException("Stub!");
    }

    public static final class SubscriptionOfferDetails {

        @NonNull
        public PricingPhases getPricingPhases() {
            throw new RuntimeException("Stub!");
        }

        @NonNull
        public String getBasePlanId() {
            throw new RuntimeException("Stub!");
        }

        @Nullable
        public String getOfferId() {
            throw new RuntimeException("Stub!");
        }

        @NonNull
        public String getOfferToken() {
            throw new RuntimeException("Stub!");
        }

        @NonNull
        public List<String> getOfferTags() {
            throw new RuntimeException("Stub!");
        }

        SubscriptionOfferDetails(JSONObject var1) throws JSONException {
            throw new RuntimeException("Stub!");
        }
    }

    public static final class OneTimePurchaseOfferDetails {

        public long getPriceAmountMicros() {
            throw new RuntimeException("Stub!");
        }

        @NonNull
        public String getFormattedPrice() {
            throw new RuntimeException("Stub!");
        }

        @NonNull
        public String getPriceCurrencyCode() {
            throw new RuntimeException("Stub!");
        }

        OneTimePurchaseOfferDetails(JSONObject var1) throws JSONException {
            throw new RuntimeException("Stub!");
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface RecurrenceMode {
        int INFINITE_RECURRING = 1;
        int FINITE_RECURRING = 2;
        int NON_RECURRING = 3;
    }

    public static final class PricingPhase {

        public int getBillingCycleCount() {
            throw new RuntimeException("Stub!");
        }

        public int getRecurrenceMode() {
            throw new RuntimeException("Stub!");
        }

        public long getPriceAmountMicros() {
            throw new RuntimeException("Stub!");
        }

        @NonNull
        public String getBillingPeriod() {
            throw new RuntimeException("Stub!");
        }

        @NonNull
        public String getFormattedPrice() {
            throw new RuntimeException("Stub!");
        }

        @NonNull
        public String getPriceCurrencyCode() {
            throw new RuntimeException("Stub!");
        }

        PricingPhase(JSONObject var1) {
            throw new RuntimeException("Stub!");
        }
    }

    public static class PricingPhases {

        @NonNull
        public List<PricingPhase> getPricingPhaseList() {
            throw new RuntimeException("Stub!");
        }

        PricingPhases(JSONArray var1) {
            throw new RuntimeException("Stub!");
        }
    }
}
