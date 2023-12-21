package me.gm.cleaner.client;

import android.os.IBinder;
import android.os.ServiceManager;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.Map;

import me.gm.cleaner.server.BuildConfig;

public class SystemServiceDeathRecipient implements IBinder.DeathRecipient {

    private final IBinder binder;

    public SystemServiceDeathRecipient(IBinder binder) {
        this.binder = binder;
    }

    @Override
    public void binderDied() {
        Log.w(BuildConfig.LIBRARY_PACKAGE_NAME, "The system died.");
        try {
            //noinspection JavaReflectionMemberAccess
            Field field = ServiceManager.class.getDeclaredField("sServiceManager");
            field.setAccessible(true);
            field.set(null, null);

            //noinspection JavaReflectionMemberAccess
            field = ServiceManager.class.getDeclaredField("sCache");
            field.setAccessible(true);
            Object sCache = field.get(null);
            if (sCache instanceof Map) {
                ((Map<?, ?>) sCache).clear();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        binder.unlinkToDeath(this, 0);
    }
}
