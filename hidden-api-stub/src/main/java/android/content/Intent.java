package android.content;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.Uri;

public class Intent {
    public static final String ACTION_PACKAGE_ADDED = "android.intent.action.PACKAGE_ADDED";
    public static final String ACTION_PACKAGE_REPLACED = "android.intent.action.PACKAGE_REPLACED";
    public static final String ACTION_PACKAGE_FULLY_REMOVED = "android.intent.action.PACKAGE_FULLY_REMOVED";

    public Intent(String action) {
        throw new RuntimeException();
    }

    @Nullable
    public String getAction() {
        throw new RuntimeException();
    }

    @NonNull
    public Intent setData(@Nullable Uri data) {
        throw new RuntimeException();
    }
}
