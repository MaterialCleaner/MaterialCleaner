package android.content.pm;

import androidx.annotation.RequiresApi;

public class ApplicationInfo {
    public int uid;
    public int flags;
    public String sourceDir;
    public String processName;
    public String credentialProtectedDataDir;
    public String[] resourceDirs;
    @RequiresApi(31)
    public String[] overlayPaths;
}
