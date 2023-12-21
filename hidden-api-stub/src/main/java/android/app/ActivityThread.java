package android.app;

import android.os.IBinder;

public class ActivityThread {

    public static ActivityThread systemMain() {
        throw new RuntimeException();
    }

    public static ActivityThread currentActivityThread() {
        throw new RuntimeException();
    }

    public ApplicationThread getApplicationThread() {
        throw new RuntimeException();
    }

    public Application getApplication() {
        throw new RuntimeException();
    }

    public ContextImpl getSystemContext() {
        throw new RuntimeException();
    }

    public static class ApplicationThread extends IApplicationThread.Stub {
        @Override
        public IBinder asBinder() {
            return null;
        }
    }
}
