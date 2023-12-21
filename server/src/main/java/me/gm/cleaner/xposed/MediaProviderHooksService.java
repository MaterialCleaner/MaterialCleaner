package me.gm.cleaner.xposed;

import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import api.SystemService;
import me.gm.cleaner.server.BuildConfig;
import me.gm.cleaner.server.ICleanerServerCallback;
import me.gm.cleaner.server.IMediaProviderHooksService;

public class MediaProviderHooksService extends IMediaProviderHooksService.Stub {
    private final Map<String, List<String>> mPackageNameToReadOnlyPaths = new ConcurrentHashMap<>();
    private volatile ICleanerServerCallback mCleanerServerBinder = null;
    private final IBinder.DeathRecipient mCleanerServerDeathRecipient = () -> mCleanerServerBinder = null;

    public void whileAlive(Consumer<ICleanerServerCallback> c) {
        if (mCleanerServerBinder != null) {
            c.accept(mCleanerServerBinder);
        }
    }

    @Override
    public int getVersion() {
        return BuildConfig.VERSION_CODE;
    }

    @Override
    public void setCleanerServerBinder(ICleanerServerCallback iinterface) {
        mCleanerServerBinder = iinterface;
        try {
            iinterface.asBinder().linkToDeath(mCleanerServerDeathRecipient, 0);
        } catch (final RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setReadOnlyPaths(@NonNull Map<String, List> packageNameToReadOnlyPaths) {
        mPackageNameToReadOnlyPaths.clear();
        mPackageNameToReadOnlyPaths.putAll(
                (Map<String, List<String>>) (Object) packageNameToReadOnlyPaths);
    }

    private static final Pattern PATHS_HAVE_USER_ID = Pattern.compile("(?i)(^/[^/]+/[^/]+/)([0-9]+)(/.*)?");

    private String getPathAsUser(@NonNull String path, int userId) {
        final var m = PATHS_HAVE_USER_ID.matcher(path);
        if (!m.matches()) {
            return path;
        }
        final var sb = new StringBuilder();
        for (int i = 1; i <= m.groupCount(); i++) {
            final var group = m.group(i);
            if (group == null) {
                continue;
            } else if (TextUtils.isDigitsOnly(group)) {
                sb.append(userId);
            } else {
                sb.append(group);
            }
        }
        return sb.toString();
    }

    public boolean isReadOnly(@NonNull String path, int uid) {
        final var packages = SystemService.getPackagesForUidNoThrow(uid);
        if (packages.isEmpty()) {
            return false;
        }
        final var readOnlyPaths = mPackageNameToReadOnlyPaths.get(packages.get(0));
        if (readOnlyPaths == null) {
            return false;
        }
        final var pathAsUser = getPathAsUser(path, 0);
        final var parent = new File(pathAsUser).getParent();
        return readOnlyPaths.stream().anyMatch(readOnlyPath ->
                readOnlyPath.equalsIgnoreCase(pathAsUser) || readOnlyPath.equalsIgnoreCase(parent)
        );
    }

    @Override
    public void setMountPoint(List<String> value) {
        InlineHookConfig.INSTANCE.setMountPoint(value.stream().toArray(String[]::new));
    }

    @Override
    public void setRecordExternalAppSpecificStorage(boolean value) {
        InlineHookConfig.INSTANCE.setRecordExternalAppSpecificStorage(value);
    }
}
