package me.gm.cleaner.server;

import me.gm.cleaner.server.IMediaProviderHooksService;

interface ICleanerServerCallback {

    // For CleanerHooksService
    boolean waitMount(String packageName, int pid, int uid) = 10;

    // For MediaProviderHooksService
    void onFileSystemEvent(long timeMillis, String packageName, String path, int flags) = 11;

    String getMountedPath(String packageName, String path, int type) = 20; // specially for both

    boolean setQueriedPaths(String packageName, in List<String>paths) = 21;

    boolean onMaybeAccessQueriedPaths(String packageName, String mountedPath) = 22;

    void onReleaseQueriedPaths(String packageName) = 23;
}
