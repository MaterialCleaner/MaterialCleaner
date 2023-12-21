package me.gm.cleaner.server;

import android.content.Intent;
import android.os.RemoteException;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import api.SystemService;
import me.gm.cleaner.dao.MountRules;
import me.gm.cleaner.dao.PurchaseVerification;
import me.gm.cleaner.dao.ServicePreferences;
import me.gm.cleaner.server.observer.ActivityManagerLogsObserver;
import me.gm.cleaner.server.observer.BaseProcessObserver;
import me.gm.cleaner.server.observer.FileSystemObserver;
import me.gm.cleaner.server.observer.ObserverManager;
import me.gm.cleaner.util.FileUtils;

public class CleanerServerCallback extends ICleanerServerCallback.Stub {
    private final CleanerServer mServer;

    public CleanerServerCallback(final CleanerServer cleanerServer) {
        mServer = cleanerServer;
    }

    @Override
    public boolean waitMount(String packageName, int pid, int uid) throws RemoteException {
        final var ob = ObserverManager.fastGetObserver(ActivityManagerLogsObserver.class);
        if (ob == null) {
            return false;
        }
        return ob.waitMount(packageName, pid, uid);
    }

    @Override
    public void onFileSystemEvent(long timeMillis, String packageName, String path, int flags)
            throws RemoteException {
        final var ob = ObserverManager.fastGetObserver(FileSystemObserver.class);
        if (ob == null) {
            return;
        }
        ob.onEvent(timeMillis, packageName, path, flags);
    }

    // For insert
    @Override
    public String getMountedPath(final String packageName, String path, final int type) {
        try {
            path = new File(path).getCanonicalPath();
        } catch (final IOException ignored) {
        }
        final var userId = FileUtils.INSTANCE.extractUserIdFromPath(path, 0);
        final var ruleZipped = ServicePreferences.INSTANCE
                .getPackageSrZipped(packageName, userId);
        final var mountedPath = new MountRules(ruleZipped).getMountedPath(path);
        if (!path.equals(mountedPath) &&
                FileUtils.INSTANCE.isKnownAppDirPaths(mountedPath, packageName) &&
                !ServicePreferences.INSTANCE.getDenylist().contains(packageName) &&
                !new File(mountedPath).isDirectory()) {
            final var finalPath = path;
            mServer.broadcastIntent(broadcastIntent -> {
                broadcastIntent
                        .setAction(ServerConstants.ACTION_REDIRECTED_TO_INTERNAL)
                        .putExtra(Intent.EXTRA_PACKAGE_NAME,
                                SystemService.getPackageInfoNoThrow(packageName, 0, 0))
                        .putExtra(Intent.EXTRA_TEXT, mountedPath)
                        .setType(String.valueOf(type));
                if (PurchaseVerification.INSTANCE.isLoosePro()) {
                    broadcastIntent.putExtra(Intent.EXTRA_STREAM,
                            FileUtils.INSTANCE.getPathAsUser(finalPath, 0));
                }
            });
        }
        return mountedPath;
    }

    // For query
    private final Map<String, Map<String, String>> mPackageNameToMountedPathToPath =
            Collections.synchronizedMap(new HashMap<>());

    @Override
    public boolean setQueriedPaths(final String packageName, final List<String> paths) {
        if (ServicePreferences.INSTANCE.getDenylist().contains(packageName)) {
            return false;
        }
        final var userId = FileUtils.INSTANCE.extractUserIdFromPath(paths.get(0), 0);
        final var ruleZipped = ServicePreferences.INSTANCE
                .getPackageSrZipped(packageName, userId);
        final var mountRules = new MountRules(ruleZipped);
        for (final var path : paths) {
            final var mountedPath = mountRules.getMountedPath(path);
            if (!mountedPath.equals(path)) {
                if (ServicePreferences.INSTANCE.getRecordExternalAppSpecificStorage()) {
                    final var mountedPathToPath = mPackageNameToMountedPathToPath
                            .compute(packageName, (key, oldValue) -> oldValue == null ?
                                    new ConcurrentHashMap<>() : oldValue);
                    mountedPathToPath.put(mountedPath, path);
                    new File(mountedPath).mkdirs();
                } else if (ServicePreferences.INSTANCE.getAggressivelyPromptForReadingMediaFiles()) {
                    mServer.broadcastIntent(broadcastIntent -> {
                        broadcastIntent
                                .setAction(ServerConstants.ACTION_MEDIA_NOT_FOUND)
                                .putExtra(Intent.EXTRA_PACKAGE_NAME,
                                        SystemService.getPackageInfoNoThrow(packageName, 0, 0))
                                .putExtra(Intent.EXTRA_TEXT, path)
                                .setType(Intent.EXTRA_SUBJECT);
                        if (PurchaseVerification.INSTANCE.isLoosePro()) {
                            broadcastIntent.putExtra(Intent.EXTRA_STREAM,
                                    FileUtils.INSTANCE.getPathAsUser(path, 0));
                        }
                    });
                    return false;
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean onMaybeAccessQueriedPaths(final String packageName, final String mountedPath) {
        final var mountedPathToPath = mPackageNameToMountedPathToPath.get(packageName);
        if (mountedPathToPath == null) {
            return false;
        }
        final var path = mountedPathToPath.get(mountedPath);
        if (path == null) {
            return false;
        }
        if (FileUtils.INSTANCE.startsWith(FileUtils.INSTANCE.getAndroidDataDir(), path) &&
                !FileUtils.INSTANCE.isKnownAppDirPaths(path, packageName)) {
            return false;
        }
        if (!ServicePreferences.INSTANCE.getDenylist().contains(packageName)) {
            mServer.broadcastIntent(broadcastIntent -> {
                broadcastIntent
                        .setAction(ServerConstants.ACTION_MEDIA_NOT_FOUND)
                        .putExtra(Intent.EXTRA_PACKAGE_NAME,
                                SystemService.getPackageInfoNoThrow(packageName, 0, 0))
                        .putExtra(Intent.EXTRA_TEXT, path);
                if (PurchaseVerification.INSTANCE.isLoosePro()) {
                    broadcastIntent.putExtra(Intent.EXTRA_STREAM,
                            FileUtils.INSTANCE.getPathAsUser(path, 0));
                }
            });
        }
        return true;
    }

    @Override
    public void onReleaseQueriedPaths(final String packageName) {
        final var mountedPathToPath = mPackageNameToMountedPathToPath.remove(packageName);
        if (mountedPathToPath != null) {
            for (final var mountedPath : mountedPathToPath.keySet()) {
                rmdirSafe(mountedPath);
            }
        }
    }

    public void releaseAll() {
        for (final var mountedPathToPath : mPackageNameToMountedPathToPath.values()) {
            for (final var mountedPath : mountedPathToPath.keySet()) {
                rmdirSafe(mountedPath);
            }
        }
    }

    private void rmdirSafe(final String dir) {
        final var observer = ObserverManager.INSTANCE.getObserver(BaseProcessObserver.class);
        if (observer != null) {
            final var mountedDirs = observer.getMountedDirs();
            rmdirRecursively(dir, new HashSet<>(mountedDirs));
        }
    }

    private void rmdirRecursively(final String dir, final Set<String> exceptions) {
        if (dir == null || exceptions.contains(dir)) {
            return;
        }
        final var parent = new File(dir).getParent();
        if (FileUtils.INSTANCE.rm_dir(dir) == 0) {
            rmdirRecursively(parent, exceptions);
        }
    }
}
