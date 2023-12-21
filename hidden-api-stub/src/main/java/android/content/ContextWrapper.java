package android.content;

import android.content.res.Resources;
import android.os.Handler;
import android.os.UserHandle;

import androidx.annotation.Nullable;

import java.io.File;

public class ContextWrapper extends Context {
    public ContextWrapper(Context base) {
        throw new RuntimeException("Stub!");
    }

    protected void attachBaseContext(Context base) {
        throw new RuntimeException("Stub!");
    }

    public Context getBaseContext() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public Resources getResources() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public SharedPreferences getSharedPreferences(String var1, int var2) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public File getFilesDir() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public File getExternalFilesDir(@Nullable String type) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public File getDatabasePath(String name) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public Intent registerReceiverAsUser(BroadcastReceiver receiver, UserHandle user, IntentFilter filter, String broadcastPermission, Handler scheduler) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public Context createDeviceProtectedStorageContext() {
        throw new RuntimeException("Stub!");
    }
}
