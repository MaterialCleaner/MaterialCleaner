package me.gm.cleaner.server;

import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import kotlin.io.ByteStreamsKt;
import kotlin.io.ConstantsKt;
import kotlin.io.FilesKt;
import me.gm.cleaner.client.CleanerHooksClient;
import me.gm.cleaner.server.observer.BaseIntentObserver;
import me.gm.cleaner.server.observer.ObserverManager;

public class AutoLogging {
    public static final int MODE_BOOT_SHUTDOWN = 0;
    public static final int MODE_CONTINUOUSLY = 1;
    private static final File sZygiskModuleDir = new File("/data/adb/modules/zygisk_cleanerhooks");
    private static final File sMountException = new File(sZygiskModuleDir, "mount_exception");
    private static final File sDisable = new File(sZygiskModuleDir, "disable");
    public static final File logsZip = new File(sZygiskModuleDir, "logs.zip");
    private static final File sLogsDir = new File(sZygiskModuleDir, "logs");
    private static final File sProps = new File(sLogsDir, "props.txt");
    private final PackageInfo mPackageInfo;

    public AutoLogging(final PackageInfo packageInfo) {
        mPackageInfo = packageInfo;
    }

    public void maybePackageLegacyLogs() {
        try {
            if (sMountException.exists() && !logsZip.exists()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    sLogsDir.mkdirs();
                    sProps.createNewFile();
                    final var writer = new PrintWriter(sProps);
                    writer.println(BuildConfig.VERSION_CODE + " " + mPackageInfo.versionName);
                    final var props = List.of(
                            "ro.build.version.sdk",
                            "ro.build.description",
                            "ro.build.product",
                            "ro.product.manufacturer",
                            "ro.product.brand",
                            "ro.product.model",
                            "ro.build.fingerprint",
                            "ro.product.cpu.abi",
                            "ro.product.device",
                            "ro.build.version.release",
                            "ro.build.id"
                    );
                    for (final var prop : props) {
                        final var process = new ProcessBuilder("sh", "-c", "getprop " + prop).start();
                        final var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                        writer.println(prop + "=" + reader.readLine());
                        process.destroy();
                        reader.close();
                    }
                    writer.flush();
                    writer.close();
                }

                try (final var out = new ZipOutputStream(new FileOutputStream(logsZip))) {
                    final var logs = sLogsDir.listFiles();
                    if (logs != null) {
                        for (final var log : logs) {
                            final var e = new ZipEntry(FilesKt.toRelativeString(log, sLogsDir));
                            out.putNextEntry(e);
                            try (final var in = new FileInputStream(log)) {
                                ByteStreamsKt.copyTo(in, out, ConstantsKt.DEFAULT_BUFFER_SIZE);
                            }
                        }
                    }
                }
                FilesKt.deleteRecursively(sLogsDir);
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void grabLogs(final Object logName, final String cmd) {
        sLogsDir.mkdirs();
        try {
            final var logFile = new File(sLogsDir, logName.toString() + ".log");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                new ProcessBuilder("sh", "-c", cmd)
                        .redirectOutput(logFile)
                        .start();
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void registerBootShutdownReceiver(int mode) {
        switch (mode) {
            case MODE_CONTINUOUSLY -> {
                grabLogs(System.currentTimeMillis(),
                        "/system/bin/logcat -b main,system,crash");
            }
            case MODE_BOOT_SHUTDOWN -> {
                final var observer = ObserverManager.INSTANCE.getObserver(BaseIntentObserver.class);
                if (observer == null) {
                    return;
                }
                final var intentFilter = new IntentFilter(Intent.ACTION_BOOT_COMPLETED);
                intentFilter.addAction(Intent.ACTION_REBOOT);
                intentFilter.addAction(Intent.ACTION_SHUTDOWN);
                observer.registerReceiver(
                        new IIntentReceiver.Stub() {
                            @Override
                            public void performReceive(final Intent intent, final int resultCode,
                                                       final String data, final Bundle extras,
                                                       final boolean ordered, final boolean sticky,
                                                       final int sendingUser) throws RemoteException {
                                final var action = intent.getAction();
                                Log.i(BuildConfig.LIBRARY_PACKAGE_NAME, action);
                                switch (action) {
                                    case Intent.ACTION_BOOT_COMPLETED -> {
                                        grabLogs("boot",
                                                "/system/bin/logcat -d main,system,crash");
                                    }
                                    case Intent.ACTION_REBOOT, Intent.ACTION_SHUTDOWN -> {
                                        grabLogs("shutdown",
                                                "/system/bin/logcat -d main,system,crash");
                                    }
                                }
                                // For Android 14:
                                // 1. Receivers registered with `scheduleRegisteredReceiver` need to call `finishReceiver` when `assumeDelivered` is false to avoid blocking.
                                // 2. `LOCKED_BOOT_COMPLETED` is no longer `ordered` but `assumeDelivered` = false
                                if (ordered || Intent.ACTION_BOOT_COMPLETED.equals(action)) {
                                    CleanerHooksClient.whileAlive(service -> {
                                        try {
                                            service.finishReceiver(this, resultCode, data, extras,
                                                    false, intent.getFlags());
                                        } catch (RemoteException e) {
                                            e.printStackTrace();
                                        }
                                    });
                                }
                            }
                        },
                        intentFilter, 0, 0
                );
            }
        }
    }

    public void markMountException() {
        sZygiskModuleDir.mkdirs();
        try {
            sMountException.createNewFile();
            sDisable.createNewFile();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void markMounted() {
        sMountException.delete();
        sDisable.delete();
    }
}
