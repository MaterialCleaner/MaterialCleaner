package me.gm.cleaner.server;

import static me.gm.cleaner.AndroidFilesystemConfig.AID_USER_OFFSET;

import android.content.IContentProvider;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;

import java.util.HashSet;
import java.util.Set;

import api.SystemService;
import hidden.HiddenApiBridge;
import hidden.ProcessObserverAdapter;
import hidden.UidObserverAdapter;
import me.gm.cleaner.server.ktx.IContentProviderKt;

public class BinderSender {
    public static int foregroundUid;
    private static final Set<Integer> PID_SET = new HashSet<>();
    private static CleanerService sCleanerService;

    private static class ProcessObserver extends ProcessObserverAdapter {
        @Override
        public void onForegroundActivitiesChanged(final int pid, final int uid, final boolean foregroundActivities) {
            if (!foregroundActivities) {
                return;
            }
            foregroundUid = uid;
            if (PID_SET.add(pid)) {
                onActive(uid, pid);
            }
        }

        @Override
        public void onProcessDied(final int pid, final int uid) {
            PID_SET.remove(pid);
        }

        @Override
        public void onProcessStateChanged(final int pid, final int uid, final int procState) {
            if (PID_SET.add(pid)) {
                onActive(uid, pid);
            }
        }
    }

    private static class UidObserver extends UidObserverAdapter {

        @Override
        public void onUidActive(final int uid) {
            onActive(uid);
        }
    }

    private static void onActive(final int uid) {
        onActive(uid, -1);
    }

    private static void onActive(final int uid, final int pid) {
        final var packages = SystemService.getPackagesForUidNoThrow(uid);
        if (packages.isEmpty()) {
            return;
        }

        final var userId = uid / AID_USER_OFFSET;
        for (final var packageName : packages) {
            final var pi = SystemService.getPackageInfoNoThrow(packageName, PackageManager.GET_PERMISSIONS, userId);
            if (pi == null || pi.requestedPermissions == null) {
                continue;
            }

            if (pi.packageName.equals(ServerConstants.APPLICATION_ID)) {
                sendBinderToManger(sCleanerService, userId);
                return;
            }
        }
    }

    public static void register(final CleanerService cleanerService) {
        sCleanerService = cleanerService;

        try {
            SystemService.registerProcessObserver(new ProcessObserver());
        } catch (final Throwable tr) {
            tr.printStackTrace();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                SystemService.registerUidObserver(new UidObserver(),
                        HiddenApiBridge.getActivityManager_UID_OBSERVER_ACTIVE(),
                        HiddenApiBridge.getActivityManager_PROCESS_STATE_UNKNOWN(),
                        null);
            } catch (final Throwable tr) {
                tr.printStackTrace();
            }
        }
    }

    static void sendBinderToManger(final Binder binder, final int userId) {
        sendBinderToUserApp(binder, ServerConstants.APPLICATION_ID, userId);
    }

    private static void sendBinderToUserApp(final Binder binder, final String packageName, final int userId) {
        final var name = packageName + ".binder";
        IContentProvider provider = null;

        /*
         When we pass IBinder through binder (and really crossed process), the receive side (here is system_server process)
         will always get a new instance of android.os.BinderProxy.

         In the implementation of getContentProviderExternal and removeContentProviderExternal, received
         IBinder is used as the key of a HashMap. But hashCode() is not implemented by BinderProxy, so
         removeContentProviderExternal will never work.

         Luckily, we can pass null. When token is token, count will be used.
         */
        final IBinder token = null;

        try {
            provider = SystemService.getContentProviderExternal(name, userId, token, name);
            if (provider == null) return;

            final var extra = new Bundle(1);
            extra.putBinder(ServerConstants.EXTRA_BINDER, binder);

            final var reply = IContentProviderKt.callCompat(provider, null, name, "sendBinder", null, extra);
        } catch (final Throwable tr) {
            tr.printStackTrace();
        } finally {
            if (provider != null) {
                try {
                    SystemService.removeContentProviderExternal(name, token);
                } catch (final Throwable tr) {
                    tr.printStackTrace();
                }
            }
        }
    }
}
