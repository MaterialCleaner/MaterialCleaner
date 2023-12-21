package me.gm.cleaner.xposed;

import android.content.ContentProvider;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.FileObserver;
import android.os.RemoteException;
import android.system.OsConstants;
import android.util.SparseLongArray;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import me.gm.cleaner.util.SystemPropertiesUtils;

public class MediaProviderHook {
    static final int TYPE_CONNECTION = 1;
    static final int TYPE_INSERT = 2;
    private static final int DIR = 0x40000000;
    private static final String SCAN_FILE_CALL = "scan_file";
    private final MediaProviderHooksService mService;
    private final ClassLoader mClassLoader;
    private final Class<?> mMediaProviderClass;
    final SparseLongArray mQueryRecord = new SparseLongArray();

    public MediaProviderHook(MediaProviderHooksService service,
                             ClassLoader classLoader, Class<?> mediaProviderClass) {
        mService = service;
        mClassLoader = classLoader;
        mMediaProviderClass = mediaProviderClass;
        initMediaProviderHook();
    }

    boolean isFuseThread() {
        try {
            final var fuseDaemonCls = XposedHelpers.findClass(
                    "com.android.providers.media.fuse.FuseDaemon", mClassLoader);
            return (boolean) XposedHelpers.callStaticMethod(
                    fuseDaemonCls, "native_is_fuse_thread");
        } catch (final XposedHelpers.ClassNotFoundError e) {
            return false;
        }
    }

    String getCallingPackage(final Object mp) {
        final var threadLocal = (ThreadLocal<?>) XposedHelpers.getObjectField(mp, "mCallingIdentity");
        return (String) XposedHelpers.callMethod(threadLocal.get(), "getPackageName");
    }

    private void initMediaProviderHook() {
        initMediaScannerHook();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            for (final var method : mMediaProviderClass.getDeclaredMethods()) {
                if (method.getName().equals("insertFile")) {
                    XposedBridge.hookMethod(method, new InsertHooker(this, mService, mClassLoader));
                }
            }
        }
        if (SystemPropertiesUtils.getBoolean("persist.sys.fuse", false)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Method queryMethod;
                try {
                    queryMethod = XposedHelpers.findMethodExact(mMediaProviderClass, "query",
                            Uri.class, String[].class, Bundle.class, CancellationSignal.class, boolean.class);
                } catch (final NoSuchMethodError e) {
                    queryMethod = XposedHelpers.findMethodExact(mMediaProviderClass, "query",
                            Uri.class, String[].class, Bundle.class, CancellationSignal.class);
                }
                XposedBridge.hookMethod(queryMethod, new QueryHooker(this, mService, mClassLoader));
            }
            initFuseDaemonHook();
        }
    }

    private void initMediaScannerHook() {
        final var callMethod = XposedHelpers.findMethodExact(
                mMediaProviderClass, "call", String.class, String.class, Bundle.class);
        XposedBridge.hookMethod(callMethod, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(final XC_MethodHook.MethodHookParam param) throws Throwable {
                final var method = (String) param.args[0];
                final var arg = (String) param.args[1];
                final var extras = (Bundle) param.args[2];
                if (!SCAN_FILE_CALL.equals(method)) {
                    return;
                }
                final File file;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (arg == null) {
                        return;
                    }
                    file = new File(arg);
                } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                    final var uri = (Uri) extras.getParcelable(Intent.EXTRA_STREAM);
                    if (uri == null) {
                        return;
                    }
                    file = new File(uri.getPath());
                } else {
                    return;
                }
                final var callingPackage = ((ContentProvider) param.thisObject).getCallingPackage();
                mService.whileAlive(service -> {
                    try {
                        final var mountedPath = service.getMountedPath(
                                callingPackage, file.getPath(), TYPE_CONNECTION);
                        if (mountedPath != null && !file.getPath().equals(mountedPath)) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                param.args[1] = mountedPath;
                            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                                extras.putParcelable(Intent.EXTRA_STREAM, Uri.parse(mountedPath));
                            }
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                });
            }
        });
    }

    static final int DIRECTORY_ACCESS_FOR_READ = 1;
    static final int DIRECTORY_ACCESS_FOR_WRITE = 2;
    static final int DIRECTORY_ACCESS_FOR_CREATE = 3;
    static final int DIRECTORY_ACCESS_FOR_DELETE = 4;

    private void initFuseDaemonHook() {
        // FileSystemEvent
        final var insertFileIfNecessaryForFuseMethod = XposedHelpers.findMethodExact(
                mMediaProviderClass, "insertFileIfNecessaryForFuse", String.class, int.class);
        XposedBridge.hookMethod(insertFileIfNecessaryForFuseMethod, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                final var path = (String) param.args[0];
                final var uid = (int) param.args[1];
                final var packageName = getCallingPackageName(param.thisObject, uid);
                dispatchFileSystemEvent(packageName, path, FileObserver.CREATE);
                if (mService.isReadOnly(path, uid)) {
                    param.setResult(OsConstants.EPERM);
                }
            }
        });
        Method deleteFileForFuseMethod;
        try {
            deleteFileForFuseMethod = XposedHelpers.findMethodExact(mMediaProviderClass,
                    "deleteFileForFuse", String.class, int.class);
        } catch (final NoSuchMethodError e) {
            // Hyper OS (Android 14)
            deleteFileForFuseMethod = XposedHelpers.findMethodExact(mMediaProviderClass,
                    "deleteFileForFuse", String.class, int.class, int.class);
        }
        XposedBridge.hookMethod(deleteFileForFuseMethod, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                final var path = (String) param.args[0];
                final var uid = (int) param.args[1];
                final var packageName = getCallingPackageName(param.thisObject, uid);
                dispatchFileSystemEvent(packageName, path, FileObserver.DELETE);
                if (mService.isReadOnly(path, uid)) {
                    param.setResult(OsConstants.EPERM);
                }
            }
        });
        final var renameForFuseMethod = XposedHelpers.findMethodExact(
                mMediaProviderClass, "renameForFuse", String.class, String.class, int.class);
        XposedBridge.hookMethod(renameForFuseMethod, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                final var oldPath = (String) param.args[0];
                final var newPath = (String) param.args[1];
                final var uid = (int) param.args[2];
                final var packageName = getCallingPackageName(param.thisObject, uid);
                var isDir = 0;
                if (!new File(oldPath).isFile()) {
                    isDir = DIR;
                }
                dispatchFileSystemEvent(packageName, oldPath, FileObserver.MOVED_FROM | isDir);
                dispatchFileSystemEvent(packageName, newPath, FileObserver.MOVED_TO | isDir);
                if (mService.isReadOnly(oldPath, uid) || mService.isReadOnly(newPath, uid)) {
                    param.setResult(OsConstants.EPERM);
                }
            }
        });
        try {
            // Android 12+
            final var isDirAccessAllowedForFuseMethod = XposedHelpers.findMethodExact(
                    mMediaProviderClass, "isDirAccessAllowedForFuse",
                    String.class, int.class, int.class);
            XposedBridge.hookMethod(isDirAccessAllowedForFuseMethod, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    final var path = (String) param.args[0];
                    final var uid = (int) param.args[1];
                    final var accessType = (int) param.args[2];
                    if (accessType == DIRECTORY_ACCESS_FOR_CREATE ||
                            accessType == DIRECTORY_ACCESS_FOR_DELETE) {
                        final var packageName = getCallingPackageName(param.thisObject, uid);
                        dispatchFileSystemEvent(packageName, path,
                                (accessType == DIRECTORY_ACCESS_FOR_CREATE ?
                                        FileObserver.CREATE : FileObserver.DELETE) | DIR);
                    }
                    if (accessType != DIRECTORY_ACCESS_FOR_READ && mService.isReadOnly(path, uid)) {
                        param.setResult(OsConstants.EPERM);
                    }
                }
            });
        } catch (final NoSuchMethodError e) {
            final var isDirectoryCreationOrDeletionAllowedForFuseMethod = XposedHelpers.findMethodExact(
                    mMediaProviderClass, "isDirectoryCreationOrDeletionAllowedForFuse",
                    String.class, int.class, boolean.class);
            XposedBridge.hookMethod(isDirectoryCreationOrDeletionAllowedForFuseMethod, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    final var path = (String) param.args[0];
                    final var uid = (int) param.args[1];
                    final var forCreate = (boolean) param.args[2];
                    final var packageName = getCallingPackageName(param.thisObject, uid);
                    dispatchFileSystemEvent(packageName, path,
                            (forCreate ? FileObserver.CREATE : FileObserver.DELETE) | DIR);
                    if (mService.isReadOnly(path, uid)) {
                        param.setResult(OsConstants.EPERM);
                    }
                }
            });
        }
        // coworker of QueryHooker
        final var isUidAllowedAccessToDataOrObbPathForFuseMethod = XposedHelpers.findMethodExact(
                mMediaProviderClass, "isUidAllowedAccessToDataOrObbPathForFuse",
                int.class, String.class);
        XposedBridge.hookMethod(isUidAllowedAccessToDataOrObbPathForFuseMethod, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                mService.whileAlive(service -> {
                    try {
                        final var uid = (int) param.args[0];
                        final var path = (String) param.args[1];
                        final int i;
                        synchronized (mQueryRecord) {
                            final var size = mQueryRecord.size();
                            if (size == 0) {
                                return;
                            }
                            i = mQueryRecord.indexOfKey(uid);
                            if (i < 0) {
                                // gc
                                if (size > 1) {
                                    final var indicesToRemove = new ArrayList<Integer>();
                                    for (var j = 0; j < size; j++) {
                                        final var key = mQueryRecord.keyAt(j);
                                        final var value = mQueryRecord.valueAt(j);
                                        if (TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - value) > 5) {
                                            final var packageName = getCallingPackageName(param.thisObject, key);
                                            service.onReleaseQueriedPaths(packageName);
                                            indicesToRemove.add(j);
                                        }
                                    }
                                    final var iterator = indicesToRemove.listIterator(indicesToRemove.size());
                                    while (iterator.hasPrevious()) {
                                        final int index = iterator.previous();
                                        mQueryRecord.removeAt(index);
                                    }
                                }
                                return;
                            }
                        }
                        final var packageName = getCallingPackageName(param.thisObject, uid);
                        if (service.onMaybeAccessQueriedPaths(packageName, path)) {
                            synchronized (mQueryRecord) {
                                mQueryRecord.removeAt(i);
                            }
                            service.onReleaseQueriedPaths(packageName);
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                });
            }
        });
    }

    private String getCallingPackageName(final Object mp, final int uid) {
        final var localCallingIdentity = XposedHelpers.callMethod(
                mp, "getCachedCallingIdentityForFuse", uid);
        return (String) XposedHelpers.callMethod(localCallingIdentity, "getPackageName");
    }

    private void dispatchFileSystemEvent(final String packageName, final String path, final int flags) {
        mService.whileAlive(service -> {
            try {
                service.onFileSystemEvent(System.currentTimeMillis(), packageName, path, flags);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });
    }
}
