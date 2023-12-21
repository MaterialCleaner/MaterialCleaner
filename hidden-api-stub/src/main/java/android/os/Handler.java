package android.os;

import android.annotation.NonNull;

public class Handler {
    public Handler() {
        throw new RuntimeException("STUB");
    }

    public Handler(@NonNull Looper looper) {
        throw new RuntimeException("STUB");
    }

    public final boolean post(@NonNull Runnable r) {
        throw new RuntimeException("STUB");
    }

    public final boolean postDelayed(@NonNull Runnable r, long delayMillis) {
        throw new RuntimeException("STUB");
    }

    public final Looper getLooper() {
        throw new RuntimeException("STUB");
    }
}
