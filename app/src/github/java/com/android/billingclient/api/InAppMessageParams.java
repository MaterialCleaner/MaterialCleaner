package com.android.billingclient.api;

import androidx.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class InAppMessageParams {

    @NonNull
    public static Builder newBuilder() {
        throw new RuntimeException("Stub!");
    }

    public static final class Builder {

        @NonNull
        public Builder addAllInAppMessageCategoriesToShow() {
            throw new RuntimeException("Stub!");
        }

        @NonNull
        public Builder addInAppMessageCategoryToShow(int inAppMessageCategoryId) {
            throw new RuntimeException("Stub!");
        }

        @NonNull
        public InAppMessageParams build() {
            throw new RuntimeException("Stub!");
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface InAppMessageCategoryId {
        int UNKNOWN_IN_APP_MESSAGE_CATEGORY_ID = 0;
        int TRANSACTIONAL = 2;
    }
}
