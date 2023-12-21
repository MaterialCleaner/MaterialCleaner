package com.android.billingclient.api;

import androidx.annotation.NonNull;

import java.util.List;

public final class QueryProductDetailsParams {

    @NonNull
    public static Builder newBuilder() {
        throw new RuntimeException("Stub!");
    }

    public static class Builder {

        @NonNull
        public Builder setProductList(@NonNull List<Product> productList) {
            throw new RuntimeException("Stub!");
        }

        @NonNull
        public QueryProductDetailsParams build() {
            throw new RuntimeException("Stub!");
        }
    }

    public static class Product {

        @NonNull
        public static Builder newBuilder() {
            throw new RuntimeException("Stub!");
        }

        public static class Builder {

            @NonNull
            public Builder setProductId(@NonNull String productId) {
                throw new RuntimeException("Stub!");
            }

            @NonNull
            public Builder setProductType(@NonNull String productType) {
                throw new RuntimeException("Stub!");
            }

            @NonNull
            public Product build() {
                throw new RuntimeException("Stub!");
            }
        }
    }
}
