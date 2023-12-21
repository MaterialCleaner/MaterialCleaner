package me.gm.cleaner.server;

import static hidden.HiddenApiBridge.PackageInfo_isOverlayPackage;
import static hidden.HiddenApiBridge.UserHandle_isIsolated;
import static me.gm.cleaner.model.PackageStatus.GET_FROM_ALL_PROCESS;
import static me.gm.cleaner.model.PackageStatus.GET_FROM_RECORDS;

import android.annotation.SuppressLint;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.AppOpsManager;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.MatrixCursor;
import android.os.Binder;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.system.Os;

import com.google.common.collect.ArrayListMultimap;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import api.SystemService;
import hidden.HiddenApiBridge;
import kotlin.collections.ArraysKt;
import kotlin.io.path.PathsKt;
import me.gm.cleaner.browser.IRootFileService;
import me.gm.cleaner.browser.IRootWorkerService;
import me.gm.cleaner.client.CleanerHooksClient;
import me.gm.cleaner.dao.PurchaseVerification;
import me.gm.cleaner.dao.ServicePreferences;
import me.gm.cleaner.model.BulkCursor;
import me.gm.cleaner.model.FileModel;
import me.gm.cleaner.model.FileSystemEvent;
import me.gm.cleaner.model.PackageStatus;
import me.gm.cleaner.model.ParceledListSlice;
import me.gm.cleaner.nio.RootFileService;
import me.gm.cleaner.nio.RootWorkerService;
import me.gm.cleaner.server.observer.ActivityManagerLogsObserver;
import me.gm.cleaner.server.observer.BaseProcessObserver;
import me.gm.cleaner.server.observer.EmulatedStorageEventListenerAdapter;
import me.gm.cleaner.server.observer.EmulatedStorageMountObserver;
import me.gm.cleaner.server.observer.FileSystemObserver;
import me.gm.cleaner.server.observer.MagiskDenyListObserver;
import me.gm.cleaner.server.observer.ObserverManager;
import me.gm.cleaner.server.observer.PackageInfoMapper;
import me.gm.cleaner.util.FileUtils;

public class CleanerService extends ICleanerService.Stub {
    private final CleanerServer mServer;
    private final int mManagerAid;
    private final RemoteCallbackList<IFileChangeObserver> mFileChangeObservers = new RemoteCallbackList<>();

    public CleanerService(final CleanerServer service, final int uid) {
        mServer = service;
        mManagerAid = uid;
    }

    private void enforceManager(final Object func) {
        final var callingPid = Binder.getCallingPid();
        final var callingUid = Binder.getCallingUid();
        if (callingPid == Os.getpid() || FileUtils.INSTANCE.toAppId(callingUid) == mManagerAid) {
            return;
        }
        throw new SecurityException(String.valueOf(func));
    }

    @Override
    public int getServerVersion() {
        if (FileUtils.INSTANCE.toAppId(Binder.getCallingUid()) != mManagerAid) {
            return 0;
        }
        return BuildConfig.VERSION_CODE;
    }

    @Override
    public int getServerException() {
        final var observers = ObserverManager.INSTANCE.getObservers();
        if (observers.isEmpty() && !CleanerHooksClient.pingBinder()) {
            return 4;
        }
        if (CleanerHooksClient.INSTANCE.getZygiskNeedUpgrade()) {
            return 1;
        }
        if (CleanerHooksClient.INSTANCE.getNeedEnableInLsp()) {
            return 7;
        }
        for (final var observer : observers) {
            if (observer instanceof final ActivityManagerLogsObserver activityManagerObserver) {
                if (activityManagerObserver.isLogcatShutdown()) {
                    return 2;
                }
                if (!activityManagerObserver.hasAmStart()) {
                    return 3;
                }
            } else if (observer instanceof EmulatedStorageMountObserver) {
                if (!EmulatedStorageEventListenerAdapter.isPrimaryEmulatedStorageMounted) {
                    return 6;
                }
            }
        }
        if (AutoLogging.logsZip.exists()) {
            return 5;
        }
        return 0;
    }

    @Override
    public int getServerPid() {
        return Process.myPid();
    }

    @Override
    public int getZygiskModuleVersion() {
        return CleanerHooksClient.INSTANCE.getZygiskModuleVersion();
    }

    @Override
    public ParceledListSlice<PackageInfo> getInstalledPackages(final int flags) {
        enforceManager(BuildConfig.DEBUG ? "getInstalledPackages" : 10);
        final var res = new ArrayList<>(SystemService.getInstalledPackagesFromAllUsersNoThrow(flags));
        res.removeIf(pi -> (pi.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0 &&
                PackageInfo_isOverlayPackage(pi));
        return new ParceledListSlice<>(res);
    }

    @Override
    public PackageInfo getPackageInfo(@Nonnull final String packageName, final int flags) {
        enforceManager(BuildConfig.DEBUG ? "getPackageInfo" : 11);
        return SystemService.getPackageInfoNoThrow(packageName, flags, 0);
    }

    private int getOpMode(final ApplicationInfo appInfo, final String permissionName)
            throws RemoteException {
        final var opCode = HiddenApiBridge.permissionToOpCode(permissionName);
        final List<?> opToMode;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            opToMode = SystemService.getUidOps(appInfo.uid, new int[]{opCode});
        } else {
            opToMode = SystemService.getOpsForPackage(appInfo.uid, appInfo.packageName, new int[]{opCode});
        }
        if (opToMode == null) {
            return AppOpsManager.MODE_ERRORED;
        }
        return HiddenApiBridge.mapOpToMode(opToMode).get(0).getSecond();
    }

    @Override
    public int getPackagePermission(final ApplicationInfo appInfo, final String permissionName,
                                    final boolean isRuntime) throws RemoteException {
        enforceManager(BuildConfig.DEBUG ? "getPackagePermission" : 12);
        var runtimeResult = PackageManager.PERMISSION_DENIED;
        if (isRuntime) {
            runtimeResult = SystemService.checkPermission(permissionName, appInfo.uid);
            if (runtimeResult == PackageManager.PERMISSION_DENIED) {
                return runtimeResult;
            }
        }
        switch (getOpMode(appInfo, permissionName)) {
            case AppOpsManager.MODE_ALLOWED:
                return PackageManager.PERMISSION_GRANTED;
            case AppOpsManager.MODE_IGNORED:
                return AppOpsManager.MODE_IGNORED;
            case AppOpsManager.MODE_DEFAULT:
            case AppOpsManager.MODE_ERRORED:
                if (isRuntime) {
                    return runtimeResult;
                }
            default:
                return PackageManager.PERMISSION_DENIED;
        }
    }

    @Override
    public void setPackagePermission(final ApplicationInfo appInfo, final String permissionName,
                                     final boolean isRuntime, final int userId, final boolean grant)
            throws RemoteException {
        enforceManager(BuildConfig.DEBUG ? "setPackagePermission" : 13);
        if (isRuntime) {
            if (grant) {
                SystemService.grantRuntimePermission(appInfo.packageName, permissionName, userId);
            } else {
                SystemService.revokeRuntimePermission(appInfo.packageName, permissionName, userId);
            }
        }
        final var opCode = HiddenApiBridge.permissionToOpCode(permissionName);
        final var mode = grant ? AppOpsManager.MODE_ALLOWED : AppOpsManager.MODE_IGNORED;
        SystemService.setUidMode(opCode, appInfo.uid, mode);
    }

    @Override
    public boolean isFuseBpfEnabled() {
        enforceManager(BuildConfig.DEBUG ? "isFuseBpfEnabled" : 14);
        final var observer = ObserverManager.INSTANCE.getObserver(BaseProcessObserver.class);
        return observer != null && observer.isFuseBpfEnabled();
    }

    @Override
    public PackageStatus getPackageStatus(@Nonnull final String packageName, final int flags) {
        enforceManager(BuildConfig.DEBUG ? "getPackageStatus" : 20);
        final var pids = new ArrayList<Integer>();
        final var pidFlags = new ArrayList<Integer>();
        final var userIds = new ArrayList<Integer>();

        final var observer = ObserverManager.INSTANCE.getObserver(BaseProcessObserver.class);
        if (observer == null) {
            return new PackageStatus();
        }
        final var startUpAwarePids = observer.getStartUpAwarePids(packageName);
        final var mountFailedPids = observer.getMountFailedPids();
        final var mkdir = observer.getMountedPackages().contains(packageName);
        final List<RunningAppProcessInfo> processes;
        switch (flags) {
            case GET_FROM_ALL_PROCESS:
                processes = SystemService.getRunningAppProcessesNoThrow();
                break;
            case GET_FROM_RECORDS:
                processes = SystemService.getRunningAppProcessesNoThrow().stream()
                        .filter(it -> startUpAwarePids.contains(it.pid))
                        .collect(Collectors.toList());
                break;
            default:
                processes = Collections.emptyList();
                break;
        }
        processes.stream()
                .filter(procInfo -> !UserHandle_isIsolated(FileUtils.INSTANCE.read_uid(procInfo.pid)))
                .sorted(Comparator.comparingInt(value -> value.pid))
                .forEach(procInfo ->
                        Arrays.stream(procInfo.pkgList).filter(packageName::equals).forEach(it -> {
                            pids.add(procInfo.pid);
                            final var userId = FileUtils.INSTANCE.toUserId(procInfo.uid);
                            final var targets = ServicePreferences.INSTANCE
                                    .getPackageSr(packageName, userId).getSecond();
                            final var mountedIndices = FileUtils.INSTANCE.check_mounts(
                                    procInfo.pid, targets.stream().toArray(String[]::new));
                            var pidFlag = 0;
                            if (mountedIndices == null) {
                                pidFlag |= PackageStatus.PID_FLAG_UNKNOWN;
                            } else if (Arrays.stream(mountedIndices).anyMatch(i -> i < 0)) {
                                if (ArraysKt.contains(mountedIndices, -1)) {
                                    pidFlag |= PackageStatus.PID_FLAG_DELETED;
                                }
                                if (ArraysKt.contains(mountedIndices, -2)) {
                                    pidFlag |= PackageStatus.PID_FLAG_OVERRIDE;
                                }
                            } else if (targets.size() == mountedIndices.length) {
                                pidFlag |= PackageStatus.PID_FLAG_MOUNTED;
                            }
                            if (startUpAwarePids.contains(procInfo.pid)) {
                                pidFlag |= PackageStatus.PID_FLAG_STARTUP_AWARE;
                            }
                            if (mountFailedPids.contains(procInfo.pid)) {
                                pidFlag |= PackageStatus.PID_FLAG_MOUNT_FAILED;
                            }
                            if (!mkdir) {
                                pidFlag |= PackageStatus.PID_FLAG_MKDIR_FAILED;
                            }
                            pidFlags.add(pidFlag);
                            userIds.add(userId);
                        }));

        final var packageStatus = new PackageStatus();
        packageStatus.pids = pids.stream().mapToInt(value -> value).toArray();
        packageStatus.pidFlags = pidFlags.stream().mapToInt(value -> value).toArray();
        packageStatus.userIds = userIds.stream().mapToInt(value -> value).toArray();
        return packageStatus;
    }

    @Override
    public Map<String, PackageStatus> getSrPackagesStatus(final int flags) {
        enforceManager(BuildConfig.DEBUG ? "getSrPackagesStatus" : 21);
        final ArrayListMultimap<String, Integer> pids = ArrayListMultimap.create();
        final ArrayListMultimap<String, Integer> pidFlags = ArrayListMultimap.create();
        final ArrayListMultimap<String, Integer> userIds = ArrayListMultimap.create();

        final var observer = ObserverManager.INSTANCE.getObserver(BaseProcessObserver.class);
        if (observer == null) {
            return Collections.emptyMap();
        }
        final var startUpAwarePids = observer.getAllStartUpAwarePids();
        final var mountFailedPids = observer.getMountFailedPids();
        final var mountedPackages = observer.getMountedPackages();
        final List<RunningAppProcessInfo> processes;
        switch (flags) {
            case GET_FROM_ALL_PROCESS:
                processes = SystemService.getRunningAppProcessesNoThrow();
                break;
            case GET_FROM_RECORDS:
                processes = SystemService.getRunningAppProcessesNoThrow().stream()
                        .filter(it -> startUpAwarePids.contains(it.pid))
                        .collect(Collectors.toList());
                break;
            default:
                processes = Collections.emptyList();
                break;
        }
        final var srPackages = ServicePreferences.INSTANCE.getSrPackages();
        processes.stream()
                .filter(procInfo -> !UserHandle_isIsolated(FileUtils.INSTANCE.read_uid(procInfo.pid)))
                .sorted(Comparator.comparingInt(value -> value.pid))
                .forEach(procInfo ->
                        Arrays.stream(procInfo.pkgList).filter(srPackages::contains).forEach(packageName -> {
                            pids.put(packageName, procInfo.pid);
                            final var userId = FileUtils.INSTANCE.toUserId(procInfo.uid);
                            final var targets = ServicePreferences.INSTANCE
                                    .getPackageSr(packageName, userId).getSecond();
                            final var mountedIndices = FileUtils.INSTANCE.check_mounts(
                                    procInfo.pid, targets.stream().toArray(String[]::new));
                            var pidFlag = 0;
                            if (mountedIndices == null) {
                                pidFlag |= PackageStatus.PID_FLAG_UNKNOWN;
                            } else if (Arrays.stream(mountedIndices).anyMatch(i -> i < 0)) {
                                if (ArraysKt.contains(mountedIndices, -1)) {
                                    pidFlag |= PackageStatus.PID_FLAG_DELETED;
                                }
                                if (ArraysKt.contains(mountedIndices, -2)) {
                                    pidFlag |= PackageStatus.PID_FLAG_OVERRIDE;
                                }
                            } else if (targets.size() == mountedIndices.length) {
                                pidFlag |= PackageStatus.PID_FLAG_MOUNTED;
                            }
                            if (startUpAwarePids.contains(procInfo.pid)) {
                                pidFlag |= PackageStatus.PID_FLAG_STARTUP_AWARE;
                            }
                            if (mountFailedPids.contains(procInfo.pid)) {
                                pidFlag |= PackageStatus.PID_FLAG_MOUNT_FAILED;
                            }
                            if (!mountedPackages.contains(packageName)) {
                                pidFlag |= PackageStatus.PID_FLAG_MKDIR_FAILED;
                            }
                            pidFlags.put(packageName, pidFlag);
                            userIds.put(packageName, userId);
                        }));

        final var srPackageStatus = new HashMap<String, PackageStatus>();
        pids.keySet().forEach(packageName -> {
            final var packageStatus = new PackageStatus();
            packageStatus.pids = pids.get(packageName).stream().mapToInt(value -> value).toArray();
            packageStatus.pidFlags = pidFlags.get(packageName).stream().mapToInt(value -> value).toArray();
            packageStatus.userIds = userIds.get(packageName).stream().mapToInt(value -> value).toArray();
            srPackageStatus.put(packageName, packageStatus);
        });
        return srPackageStatus;
    }

    @Override
    public List<String> getMountedDirs() {
        enforceManager(BuildConfig.DEBUG ? "getMountedDirs" : 23);
        final var observer = ObserverManager.INSTANCE.getObserver(BaseProcessObserver.class);
        if (observer == null) {
            return Collections.emptyList();
        }
        return observer.getMountedDirs();
    }

    @Override
    public boolean isInMagiskDenyList(String packageName) {
        enforceManager(BuildConfig.DEBUG ? "isInMagiskDenyList" : 24);
        return MagiskDenyListObserver.isInDenyList(packageName);
    }

    /**
     * Cautious: {@link SharedPreferences.Editor} uses {@link SharedPreferences.Editor#apply()}
     * by default, which may cause this reload method called before the preferences are written to
     * the disk and the values before writing is read.
     */
    @SuppressLint({"PrivateApi", "SoonBlockedPrivateApi"})
    @Override
    public void notifyPreferencesChanged() {
        enforceManager(BuildConfig.DEBUG ? "notifyPreferencesChanged" : 30);
        try {
            final var sps = new SharedPreferences[]{
                    ServicePreferences.INSTANCE.getPreferences()
            };
            final var spImplCls = Class.forName("android.app.SharedPreferencesImpl");
            final var method = spImplCls.getDeclaredMethod("startLoadFromDisk");
            method.setAccessible(true);
            for (final var sp : sps) {
                method.invoke(sp);
            }
        } catch (final ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
                       InvocationTargetException e) {
            e.printStackTrace();
        }
        CleanerHooksClient.whileAlive(CleanerHooksClient::syncRecordExternalAppSpecificStorage);
    }

    @Override
    public void notifySrChanged() {
        enforceManager(BuildConfig.DEBUG ? "notifySrChanged" : 31);
        ServicePreferences.INSTANCE.invalidateSrCache();
        PackageInfoMapper.invalidate();
        CleanerHooksClient.whileAlive(CleanerHooksClient::syncMountPoint);
    }

    @Override
    public void notifyReadOnlyChanged() {
        enforceManager(BuildConfig.DEBUG ? "notifyReadOnlyChanged" : 32);
        ServicePreferences.INSTANCE.invalidateReadOnlyCache();
        CleanerHooksClient.whileAlive(CleanerHooksClient::syncReadOnlyPaths);
    }

    @Override
    public void remount(@Nonnull final String[] packageNames) {
        enforceManager(BuildConfig.DEBUG ? "remount" : 33);
        final var observer = ObserverManager.INSTANCE.getObserver(BaseProcessObserver.class);
        if (observer != null) {
            if (PurchaseVerification.INSTANCE.isLoosePro()) {
                observer.remountForPackages(packageNames);
            } else {
                observer.forceStopPackages(packageNames);
            }
        }
    }

    @Override
    public void registerFileChangeObserver(final IFileChangeObserver observer) {
        enforceManager(BuildConfig.DEBUG ? "registerFileChangeObserver" : 40);
        mFileChangeObservers.register(observer);
    }

    @Override
    public void unregisterFileChangeObserver(final IFileChangeObserver observer) {
        mFileChangeObservers.unregister(observer);
    }

    public synchronized void dispatchFileChange(
            final long timeMillis, final String packageName, final String path, final int flags,
            final boolean isAppSpecificStorage) {
        int i = mFileChangeObservers.beginBroadcast();
        while (i > 0) {
            i--;
            final var observer = mFileChangeObservers.getBroadcastItem(i);
            if (observer != null) {
                try {
                    observer.onEvent(timeMillis, packageName, path, flags, isAppSpecificStorage);
                } catch (final RemoteException ignored) {
                }
            }
        }
        mFileChangeObservers.finishBroadcast();
    }

    @Override
    public void pruneRecords(final long method, @Nullable final String[] packageNames,
                             final boolean isHideAppSpecificStorage, @Nullable final String queryText) {
        enforceManager(BuildConfig.DEBUG ? "pruneRecords" : 42);
        final var observer = ObserverManager.INSTANCE.getObserver(FileSystemObserver.class);
        if (observer != null) {
            observer.prune(method, packageNames, isHideAppSpecificStorage, queryText);
        }
    }

    @Override
    public BulkCursor<FileSystemEvent> queryAllRecords(final boolean isHideAppSpecificStorage,
                                                       @Nullable final String queryText) {
        enforceManager(BuildConfig.DEBUG ? "queryAllRecords" : 43);
        final var observer = ObserverManager.INSTANCE.getObserver(FileSystemObserver.class);
        if (observer == null) {
            return new BulkCursor<>(
                    new MatrixCursor(new String[]{}),
                    cursor -> new FileSystemEvent(
                            cursor.getLong(1),
                            cursor.getString(2),
                            cursor.getString(3),
                            cursor.getInt(4)
                    )
            );
        }
        return new BulkCursor<>(
                observer.queryAllRecords(isHideAppSpecificStorage, queryText),
                cursor -> new FileSystemEvent(
                        cursor.getLong(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getInt(4)
                )
        );
    }

    @Override
    public ParceledListSlice<FileSystemEvent> queryDistinctRecordsInclude(
            @Nonnull final String[] packageNames) {
        enforceManager(BuildConfig.DEBUG ? "queryDistinctRecordsInclude" : 44);
        final var observer = ObserverManager.INSTANCE.getObserver(FileSystemObserver.class);
        if (observer == null) {
            return new ParceledListSlice<>(Collections.emptyList());
        }
        final var cursor = observer.queryDistinctRecordsInclude(packageNames);
        final var res = new ArrayList<FileSystemEvent>(cursor.getCount());
        while (cursor.moveToNext()) {
            res.add(new FileSystemEvent(
                    0,
                    cursor.getString(0),
                    cursor.getString(1),
                    cursor.getInt(2)
            ));
        }
        cursor.close();
        return new ParceledListSlice<>(res);
    }

    @Override
    public int countRecordsInclude(@Nonnull final String[] packageNames) {
        enforceManager(BuildConfig.DEBUG ? "countRecordsInclude" : 45);
        final var observer = ObserverManager.INSTANCE.getObserver(FileSystemObserver.class);
        if (observer == null) {
            return 0;
        }
        return observer.countRecordsInclude(packageNames);
    }

    @Override
    public int databaseCount() {
        enforceManager(BuildConfig.DEBUG ? "databaseSize" : 46);
        final var observer = ObserverManager.INSTANCE.getObserver(FileSystemObserver.class);
        if (observer == null) {
            return 0;
        }
        return observer.databaseCount();
    }

    @Override
    public IRootFileService newRootFileService() {
        enforceManager(BuildConfig.DEBUG ? "newRootFileService" : 50);
        return new RootFileService();
    }

    @Override
    public IRootWorkerService newRootWorkerService() {
        enforceManager(BuildConfig.DEBUG ? "newRootWorkerService" : 51);
        return new RootWorkerService();
    }

    @Override
    public FileModel createFileModel(@Nonnull final String path) {
        enforceManager(BuildConfig.DEBUG ? "createFileModel" : 50);
        return new FileModel(Paths.get(path));
    }

    @Override
    public ParceledListSlice<FileModel> listFiles(@Nonnull final String path) {
        enforceManager(BuildConfig.DEBUG ? "listFiles" : 51);
        try {
            return new ParceledListSlice<>(
                    PathsKt.listDirectoryEntries(Paths.get(path), "*").stream()
                            .map(FileModel::new)
                            .collect(Collectors.toList())
            );
        } catch (final Exception e) {
            return new ParceledListSlice<>(Collections.emptyList());
        }
    }

    @Override
    public boolean move(String from, String to) {
        enforceManager(BuildConfig.DEBUG ? "move" : 52);
        if (!PurchaseVerification.INSTANCE.isLoosePro()) {
            return true;
        }
        final var srcPath = Paths.get(from);
        final var dstPath = Paths.get(to);
        return FileUtils.INSTANCE.move(srcPath, dstPath);
    }

    @Override
    public boolean copy(final String from, final String to) {
        enforceManager(BuildConfig.DEBUG ? "copy" : 53);
        if (!PurchaseVerification.INSTANCE.isLoosePro()) {
            return true;
        }
        final var srcPath = Paths.get(from);
        final var dstPath = Paths.get(to);
        return FileUtils.INSTANCE.copy(srcPath, dstPath);
    }

    @Override
    public void setDenyList(@Nonnull final String[] packageNames) {
        enforceManager(BuildConfig.DEBUG ? "setDenyList" : 60);
        if (ServicePreferences.INSTANCE.getRecordExternalAppSpecificStorage()) {
            final var appsNewlyAddedToDenylist = new ArrayList<>(Arrays.asList(packageNames));
            appsNewlyAddedToDenylist.removeAll(ServicePreferences.INSTANCE.getDenylist());
            switchSpecificAppsOwner(appsNewlyAddedToDenylist.stream().toArray(String[]::new));
        }
        ServicePreferences.INSTANCE.setDenylist(Arrays.asList(packageNames));
    }

    @Override
    public List<String> getDenyList() {
        enforceManager(BuildConfig.DEBUG ? "getDenyList" : 61);
        return ServicePreferences.INSTANCE.getDenylist();
    }

    @Override
    public void switchSpecificAppsOwner(final String[] packageNames) {
        enforceManager(BuildConfig.DEBUG ? "switchSpecificAppsOwner" : 62);
        for (final var userId : SystemService.getUserIdsNoThrow()) {
            for (final var packageName : packageNames) {
                final var ai = SystemService.getApplicationInfoNoThrow(packageName, 0, userId);
                if (ai != null) {
                    FileUtils.INSTANCE.switch_owner(
                            FileUtils.INSTANCE.getPathAsUser(
                                    FileUtils.INSTANCE.buildExternalStorageAppDataDirs(ai.packageName).getPath(),
                                    userId
                            ),
                            ai.uid,
                            true
                    );
                }
            }
        }
    }

    @Override
    public void switchAllAppsOwner() {
        enforceManager(BuildConfig.DEBUG ? "switchAllAppsOwner" : 63);
        for (final var userId : SystemService.getUserIdsNoThrow()) {
            for (final var ai : SystemService.getInstalledApplicationsNoThrow(0, userId)) {
                FileUtils.INSTANCE.switch_owner(
                        FileUtils.INSTANCE.getPathAsUser(
                                FileUtils.INSTANCE.buildExternalStorageAppDataDirs(ai.packageName).getPath(),
                                userId
                        ),
                        ai.uid,
                        true
                );
            }
        }
    }

    @Override
    public void syncCertificates(final List<String> certificates) {
        enforceManager(BuildConfig.DEBUG ? "syncCertificates" : 70);
        ObserverManager.INSTANCE.stopAllObservers();

        mServer.onStorageManagerServiceReady();
    }

    @Override
    public void syncSignatures(final List<String> signatures) {
        enforceManager(BuildConfig.DEBUG ? "syncSignatures" : 71);
        ObserverManager.INSTANCE.stopAllObservers();

        PurchaseVerification.INSTANCE.setSignatures(new HashSet<>(signatures));

        mServer.onStorageManagerServiceReady();
    }

    @Override
    public void exit() {
        enforceManager(BuildConfig.DEBUG ? "exit" : 100);
        MagiskDenyListObserver.close();
        mServer.onDestroy();
        System.exit(0);
    }
}
