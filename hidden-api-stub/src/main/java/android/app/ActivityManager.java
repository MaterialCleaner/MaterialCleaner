package android.app;

public class ActivityManager {

    public static int UID_OBSERVER_ACTIVE;

    public static int PROCESS_STATE_UNKNOWN;

    public static class RunningAppProcessInfo {
        public int pid;
        public String[] pkgList;
        public String processName;
        public int uid;

        public static int procStateToImportance(int procState) {
            throw new RuntimeException("STUB");
        }
    }
}
