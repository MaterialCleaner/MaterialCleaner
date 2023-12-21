package me.gm.cleaner.server;

import static hidden.HiddenApiBridge.Context_createPackageContextAsUser;
import static hidden.HiddenApiBridge.createUserHandle;

import android.annotation.SuppressLint;
import android.app.ActivityThread;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.storage.VolumeInfo;
import android.util.Log;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import api.SystemService;
import me.gm.cleaner.client.CleanerHooksClient;
import me.gm.cleaner.dao.SecurityHelper;
import me.gm.cleaner.dao.ServicePreferences;
import me.gm.cleaner.server.observer.BaseProcessObserver;
import me.gm.cleaner.server.observer.EmulatedStorageEventListenerAdapter;
import me.gm.cleaner.server.observer.ObserverManager;
import me.gm.cleaner.util.FileUtils;
import me.gm.cleaner.util.LibUtils;

public class CleanerServer extends ContextWrapper {
    public final Handler handler = new Handler(Looper.getMainLooper());
    public CleanerService cleanerService;
    public final PackageInfo packageInfo;
    // System service lifecycle
    final PackageReceiver mPackageReceiver;
    final AutoLogging mAutoLogging;
    final CleanerServerCallback mCleanerServerCallback;

    private Context createPackageContext(final String packageName) {
        try {
            return Context_createPackageContextAsUser(
                    ActivityThread.systemMain().getSystemContext(),
                    packageName,
                    Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY,
                    createUserHandle(0));
        } catch (final PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void waitSystemService(final String name) {
        while (ServiceManager.getService(name) == null) {
            try {
                Log.i(BuildConfig.LIBRARY_PACKAGE_NAME, "waitSystemService " + name);
                Thread.sleep(1000);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void waitSystemServices() {
        for (final var name : List.of(
                "activity", "package", Context.USER_SERVICE, Context.APP_OPS_SERVICE, "mount"
        )) {
            waitSystemService(name);
        }
    }

    private void sendBinderToManger(final Binder binder) {
        for (final var userId : SystemService.getUserIdsNoThrow()) {
            BinderSender.sendBinderToManger(binder, userId);
        }
    }

    @SuppressLint("UnsafeDynamicallyLoadedCode")
    public CleanerServer() {
        super(null);
        waitSystemServices();
        packageInfo = SystemService.getPackageInfoNoThrow(ServerConstants.APPLICATION_ID, 0, 0);
        if (packageInfo == null) {
            throw new RuntimeException("Failed to getPackageInfo");
        }
        attachBaseContext(createPackageContext(ServerConstants.APPLICATION_ID));
        LibUtils.loadLibrary(LibUtils.getLibSourceDir(packageInfo.applicationInfo), "cleaner");
        SecurityHelper.INSTANCE.warmUpJcaProviders();
        final var dpsContext = new ContextWrapper(createDeviceProtectedStorageContext()) {
            @Override
            public Context getApplicationContext() {
                return this;
            }
        };
        SecurityHelper.INSTANCE.init(dpsContext);
        ServicePreferences.INSTANCE.init(dpsContext);
        mPackageReceiver = new PackageReceiver(this);
        mAutoLogging = new AutoLogging(packageInfo);
        mCleanerServerCallback = new CleanerServerCallback(this);
        if (ServicePreferences.INSTANCE.getAutoLogging()) {
            mAutoLogging.maybePackageLegacyLogs();
        }
        cleanerService = new CleanerService(this, packageInfo.applicationInfo.uid);
        Log.i(BuildConfig.LIBRARY_PACKAGE_NAME, "Cleaner server v" + BuildConfig.VERSION_CODE + " started");
    }

    @Override
    public Context getApplicationContext() {
        return this;
    }

    public void onStorageManagerServiceReady() {
        CleanerHooksClient.INSTANCE.onStart(this);
        ObserverManager.INSTANCE.startAllObservers(this);
        mPackageReceiver.registerPackageReceiver();
        CleanerHooksClient.whileAlive(service -> {
            if (ServicePreferences.INSTANCE.getAutoLogging()) {
                mAutoLogging.markMountException();
                mAutoLogging.registerBootShutdownReceiver(AutoLogging.MODE_BOOT_SHUTDOWN);
            }
            try {
                service.setCleanerServerBinder(mCleanerServerCallback);
                CleanerHooksClient.syncSrPackages(service);
                CleanerHooksClient.syncReadOnlyPaths(service);
                CleanerHooksClient.syncMountPoint(service);
                CleanerHooksClient.syncRecordExternalAppSpecificStorage(service);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        });
        new EmulatedStorageEventListenerImpl(this).start();
        BinderSender.register(cleanerService);
        sendBinderToManger(cleanerService);
    }

    public void onEmulatedStorageMounted(final VolumeInfo vol, final boolean isPrimary,
                                         final boolean isJustMounted) {
        // these things should be done as soon as possible
        if (isPrimary) {
            FileUtils.INSTANCE.setExternalStorageDir(new File(vol.path, String.valueOf(0)));
        }
        final var observer = ObserverManager.INSTANCE.getObserver(BaseProcessObserver.class);
        if (observer != null) {
            final var mountUserId = EmulatedStorageEventListenerAdapter.getMountUserId(vol);
            observer.getMountedEmulatedStorage().add(mountUserId);
            if (isJustMounted) {
                if (isPrimary) {
                    observer.remountAll();
                } else {
                    observer.remountAllWithCheck();
                }
            } else if (isPrimary) {
                observer.recordAll();
            }
        }
        // leisurely do remaining things
        if (isPrimary) {
            CleanerHooksClient.whileAlive(service -> {
                if (ServicePreferences.INSTANCE.getAutoLogging()) {
                    mAutoLogging.markMounted();
                }
            });
            if (observer != null && observer.isFuseBpfEnabled()) {
                new Thread(() -> cleanerService.switchSpecificAppsOwner(
                        ServicePreferences.INSTANCE.getSrPackages().stream().toArray(String[]::new)
                )).start();
            }
        }
    }

    public void onEmulatedStorageUnmounted(final VolumeInfo vol) {
        final var observer = ObserverManager.INSTANCE.getObserver(BaseProcessObserver.class);
        if (observer != null) {
            final var mountUserId = EmulatedStorageEventListenerAdapter.getMountUserId(vol);
            observer.getMountedEmulatedStorage().remove(mountUserId);
        }
    }

    private void broadcastIntentDelayed(final Consumer<Intent> callback, long delayMillis) {
        final var extra = new Bundle(1);
        extra.putBinder(ServerConstants.EXTRA_BINDER, cleanerService);
        @SuppressLint("WrongConstant") final var intent = new Intent()
                .setClassName(this, ServerConstants.RECEIVER_ACTIVITY_NAME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                .setPackage(ServerConstants.APPLICATION_ID)
                .putExtra(Intent.EXTRA_RESTRICTIONS_BUNDLE, extra);
        callback.accept(intent);
        Objects.requireNonNull(intent.getAction());
        handler.postDelayed(() -> SystemService.startActivityNoThrow(intent, null, 0), delayMillis);
    }

    public void broadcastIntentDelayed(final Consumer<Intent> callback) {
        broadcastIntentDelayed(callback, 2000);
    }

    public void broadcastIntent(final Consumer<Intent> callback) {
        broadcastIntentDelayed(callback, 0);
    }

    public void onDestroy() {
        CleanerHooksClient.INSTANCE.onDestroy();
        ObserverManager.INSTANCE.stopAllObservers();
        mCleanerServerCallback.releaseAll();
    }
}
