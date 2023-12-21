package android.os;

import android.annotation.NonNull;

public class UserHandle {

    @NonNull
    public static UserHandle ALL;

    public UserHandle(int h) {
        throw new RuntimeException("STUB");
    }

    public int getIdentifier() {
        throw new RuntimeException("STUB");
    }

    public static boolean isIsolated(int uid) {
        throw new RuntimeException("STUB");
    }
}
