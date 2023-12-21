package hidden;

import android.app.ActivityManager;
import android.app.ActivityThread;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.SystemProperties;
import android.os.UserHandle;

import androidx.annotation.RequiresApi;

import com.android.org.conscrypt.Conscrypt;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;

import kotlin.Pair;

public class HiddenApiBridge {

    public static int getActivityManager_UID_OBSERVER_ACTIVE() {
        return ActivityManager.UID_OBSERVER_ACTIVE;
    }

    public static int getActivityManager_PROCESS_STATE_UNKNOWN() {
        return ActivityManager.PROCESS_STATE_UNKNOWN;
    }

    public static UserHandle createUserHandle(int userId) {
        return new UserHandle(userId);
    }

    public static Context getSystemContext() {
        return ActivityThread.systemMain().getSystemContext();
    }

    public static int UserHandle_getIdentifier(UserHandle userHandle) {
        return userHandle.getIdentifier();
    }

    public static boolean UserHandle_isIsolated(int uid) {
        return UserHandle.isIsolated(uid);
    }

    public static int permissionToOpCode(String permission) {
        return AppOpsManager.permissionToOpCode(permission);
    }

    public static List<AppOpsManager.OpEntry> PackageOps_getOps(Object _packageOps) {
        AppOpsManager.PackageOps packageOps = (AppOpsManager.PackageOps) _packageOps;
        return packageOps.getOps();
    }

    public static int OpEntry_getOp(Object _opEntry) {
        AppOpsManager.OpEntry opEntry = (AppOpsManager.OpEntry) _opEntry;
        return opEntry.getOp();
    }

    public static int OpEntry_getMode(Object _opEntry) {
        AppOpsManager.OpEntry opEntry = (AppOpsManager.OpEntry) _opEntry;
        return opEntry.getMode();
    }

    public static List<Pair<Integer, Integer>> mapOpToMode(List<?> ops) {
        return ((List<AppOpsManager.PackageOps>) ops).stream()
                .map(HiddenApiBridge::PackageOps_getOps)
                .flatMap(Collection::stream)
                .map(opEntry -> new Pair<>(HiddenApiBridge.OpEntry_getOp(opEntry), HiddenApiBridge.OpEntry_getMode(opEntry)))
                .collect(Collectors.toList());
    }

    public static String PackageInfo_overlayTarget(PackageInfo packageInfo) {
        return packageInfo.overlayTarget;
    }

    @RequiresApi(29)
    public static byte[] Conscrypt_exportKeyingMaterial(SSLSocket socket, String label, byte[] context, int length) throws SSLException {
        return Conscrypt.exportKeyingMaterial(socket, label, context, length);
    }

    public static String SystemProperties_get(String key) {
        return SystemProperties.get(key);
    }

    public static String SystemProperties_get(String key, String defaultValue) {
        return SystemProperties.get(key, defaultValue);
    }

    public static void SystemProperties_set(String key, String defaultValue) {
        SystemProperties.set(key, defaultValue);
    }

    public static Context Context_createPackageContextAsUser(Context context, String packageName, int flags, UserHandle user)
            throws PackageManager.NameNotFoundException {
        return context.createPackageContextAsUser(packageName, flags, user);
    }

    public static boolean PackageInfo_isOverlayPackage(PackageInfo packageInfo) {
        return packageInfo.overlayTarget != null;
    }
}
