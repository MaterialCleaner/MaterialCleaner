package me.gm.cleaner.server.observer;

import static me.gm.cleaner.AndroidFilesystemConfig.AID_APP_START;

import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import api.SystemService;
import me.gm.cleaner.AndroidFilesystemConfig;
import me.gm.cleaner.dao.ServicePreferences;
import me.gm.cleaner.util.FileUtils;

public class PackageInfoMapper {
    private static SparseArray<String> sUidToSrPackageNames;
    private static SparseArray<String> sUidToPackageNames;
    private static SparseArray<Map<String, String>> sSharedUserIdEnabledUidToProcessNamesToPackageName;
    private static Map<String, String> sProcessNameToSystemSrPackageNames;
    private static Map<String, String> sProcessNameToSystemPackageNames;
    private static Map<String, Integer> sLogFormatAppPrincipalNamesToUid;

    public synchronized static void invalidate() {
        sUidToSrPackageNames = null;
        sUidToPackageNames = null;
        sSharedUserIdEnabledUidToProcessNamesToPackageName = null;
        sProcessNameToSystemSrPackageNames = null;
        sProcessNameToSystemPackageNames = null;
        sLogFormatAppPrincipalNamesToUid = null;
    }

    private static void ensurePackageInfoMaps() {
        if (sUidToSrPackageNames == null) {
            synchronized (PackageInfoMapper.class) {
                if (sUidToSrPackageNames == null) {
                    sUidToSrPackageNames = new SparseArray<>();
                    sUidToPackageNames = new SparseArray<>();
                    sSharedUserIdEnabledUidToProcessNamesToPackageName = new SparseArray<>();
                    sProcessNameToSystemSrPackageNames = new ArrayMap<>();
                    sProcessNameToSystemPackageNames = new ArrayMap<>();
                    sLogFormatAppPrincipalNamesToUid = new ArrayMap<>();
                } else {
                    return;
                }

                final var srPackages = ServicePreferences.INSTANCE.getSrPackages();
                for (final var userId : SystemService.getUserIdsNoThrow()) {
                    for (final var pi : SystemService.getInstalledPackagesNoThrow(0, userId)) {
                        final var uid = pi.applicationInfo.uid;
                        final var packageName = pi.packageName;
                        final var processNames = getProcessNames(packageName, userId);
                        if (FileUtils.INSTANCE.toAppId(uid) >= AID_APP_START) {
                            // As for user apps, we use uid to figure out its packageName.
                            // But if the app enabled sharedUserId, we use processName.
                            if (TextUtils.isEmpty(pi.sharedUserId)) {
                                if (srPackages.contains(packageName)) {
                                    sUidToSrPackageNames.put(uid, packageName);
                                } else {
                                    sUidToPackageNames.put(uid, packageName);
                                }
                            } else if (srPackages.contains(packageName)) {
                                var processNameToPackageName = sSharedUserIdEnabledUidToProcessNamesToPackageName.get(uid);
                                if (processNameToPackageName == null) {
                                    processNameToPackageName = new ArrayMap<>(processNames.size());
                                }
                                for (final var processName : processNames) {
                                    processNameToPackageName.put(processName, packageName);
                                }
                                sSharedUserIdEnabledUidToProcessNamesToPackageName.put(uid, processNameToPackageName);
                            }
                        } else {
                            // As for system apps, we use processName to figure out its packageName.
                            if (srPackages.contains(packageName)) {
                                processNames.forEach(processName ->
                                        sProcessNameToSystemSrPackageNames.put(processName, packageName));
                            } else {
                                processNames.forEach(processName ->
                                        sProcessNameToSystemPackageNames.put(processName, packageName));
                            }
                        }
                        // LogcatObserver doesn't provide standard appPrincipalName, so we have to do a workaround.
                        if (srPackages.contains(packageName)) {
                            final var logFormatAppPrincipalName = AndroidFilesystemConfig
                                    .getAppPrincipalName(uid)
                                    .replace("_", "");
                            sLogFormatAppPrincipalNamesToUid.put(logFormatAppPrincipalName, uid);
                        }
                    }
                }
            }
        }
    }

    public static String getSrPackageName(int uid, String processName) {
        ensurePackageInfoMaps();
        if (FileUtils.INSTANCE.toAppId(uid) >= AID_APP_START) {
            final var processNameToPackageName = sSharedUserIdEnabledUidToProcessNamesToPackageName.get(uid);
            if (processNameToPackageName == null) {
                // user app
                return sUidToSrPackageNames.get(uid);
            } else {
                // sharedUserId enabled user app
                return processNameToPackageName.get(processName);
            }
        } else {
            // system app
            return sProcessNameToSystemSrPackageNames.get(processName);
        }
    }

    // LogcatObserver: logFormatAppPrincipalName -> uid -> srPackageName
    public static int getUid(String logFormatAppPrincipalName) {
        if (TextUtils.isDigitsOnly(logFormatAppPrincipalName)) {
            return Integer.parseInt(logFormatAppPrincipalName);
        }
        ensurePackageInfoMaps();
        final var uid = sLogFormatAppPrincipalNamesToUid.get(logFormatAppPrincipalName);
        return uid != null ? uid : -1;
    }

    // getPackageStatus(), remountAll(): pid -> read_uid -> packageName
    public static String getPackageName(int uid, String processName) {
        ensurePackageInfoMaps();
        final var srPackageName = getSrPackageName(uid, processName);
        if (srPackageName != null) return srPackageName;
        if (FileUtils.INSTANCE.toAppId(uid) >= AID_APP_START) {
            final var processNameToPackageName = sSharedUserIdEnabledUidToProcessNamesToPackageName.get(uid);
            if (processNameToPackageName == null) {
                // user app
                return sUidToPackageNames.get(uid);
            } else {
                // sharedUserId enabled user app
                return processNameToPackageName.get(processName);
            }
        } else {
            // system app
            return sProcessNameToSystemPackageNames.get(processName);
        }
    }

    private static List<String> getProcessNames(String packageName, int userId) {
        final var processNames = new ArrayList<String>();
        final var pi = SystemService.getPackageInfoNoThrow(packageName,
                PackageManager.GET_ACTIVITIES |
                        PackageManager.GET_RECEIVERS |
                        PackageManager.GET_SERVICES |
                        PackageManager.GET_PROVIDERS, userId);
        if (pi != null) {
            for (final var components : new ComponentInfo[][]{
                    pi.activities, pi.receivers, pi.services, pi.providers}) {
                if (components != null) {
                    for (final var component : components) {
                        processNames.add(component.processName);
                    }
                }
            }
        }
        return processNames;
    }
}
