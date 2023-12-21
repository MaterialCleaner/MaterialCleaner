package android.content;

import android.annotation.Nullable;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Handler;
import android.os.UserHandle;

import java.io.File;

public abstract class Context {
    /**
     * @deprecated
     */
    @Deprecated
    public static final int MODE_MULTI_PROCESS = 4;
    public static final int MODE_NO_LOCALIZED_COLLATORS = 16;
    public static final int MODE_PRIVATE = 0;

    public Context() {
        throw new RuntimeException("Stub!");
    }

    public abstract Resources getResources();

    public abstract SharedPreferences getSharedPreferences(String var1, int var2);

    public abstract File getFilesDir();

    public abstract File getExternalFilesDir(@Nullable String type);

    public abstract File getDatabasePath(String name);

    public abstract Intent registerReceiverAsUser(
            BroadcastReceiver receiver,
            UserHandle user,
            IntentFilter filter,
            String broadcastPermission,
            Handler scheduler
    );

    public Context createPackageContextAsUser(String packageName, int flags, UserHandle user)
            throws PackageManager.NameNotFoundException {
        throw new RuntimeException();
    }

    public abstract Context createDeviceProtectedStorageContext();
}
