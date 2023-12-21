package me.gm.cleaner.server;

import me.gm.cleaner.browser.IRootFileService;
import me.gm.cleaner.browser.IRootWorkerService;
import me.gm.cleaner.model.BulkCursor;
import me.gm.cleaner.model.FileModel;
import me.gm.cleaner.model.FileSystemEvent;
import me.gm.cleaner.model.PackageStatus;
import me.gm.cleaner.model.ParceledListSlice;
import me.gm.cleaner.server.IFileChangeObserver;

interface ICleanerService {

    // server info
    int getServerVersion() = 0;

    int getServerException() = 1;

    int getServerPid() = 2;

    int getZygiskModuleVersion() = 3;

    // system info
    ParceledListSlice<PackageInfo> getInstalledPackages(int flags) = 10;

    PackageInfo getPackageInfo(String packageName, int flags) = 11;

    int getPackagePermission(in ApplicationInfo appInfo, String permissionName, boolean isRuntime) = 12;

    void setPackagePermission(in ApplicationInfo appInfo, String permissionName, boolean isRuntime, int userId, boolean grant) = 13;

    boolean isFuseBpfEnabled() = 14;

    // mount
    PackageStatus getPackageStatus(String packageName, int flags) = 20;

    Map<String, PackageStatus> getSrPackagesStatus(int flags) = 21;

    List<String> getMountedDirs() = 23;

    boolean isInMagiskDenyList(String packageName) = 24;

    void notifyPreferencesChanged() = 30;

    void notifySrChanged() = 31;

    void notifyReadOnlyChanged() = 32;

    void remount(in String[] packageNames) = 33;

    // filesystem record
    void registerFileChangeObserver(in IFileChangeObserver observer) = 40;

    void unregisterFileChangeObserver(in IFileChangeObserver observer) = 41;

    void pruneRecords(long method, in String[] packageNames, boolean isHideAppSpecificStorage, String queryText) = 42;

    BulkCursor<FileSystemEvent> queryAllRecords(boolean isHideAppSpecificStorage, String queryText) = 43;

    ParceledListSlice<FileSystemEvent> queryDistinctRecordsInclude(in String[] packageNames) = 44;

    int countRecordsInclude(in String[] packageNames) = 45;

    int databaseCount() = 46;

    // file service
    IRootFileService newRootFileService() = 50;

    IRootWorkerService newRootWorkerService() = 51;

    // TODO: refactor with the function above
    FileModel createFileModel(String path) = 54;

    ParceledListSlice<FileModel> listFiles(String path) = 55;

    boolean move(String from, String to) = 52;

    boolean copy(String from, String to) = 53;

    // zygisk
    void setDenyList(in String[] packageNames) = 60;

    List<String> getDenyList() = 61;

    void switchSpecificAppsOwner(in String[] packageNames) = 62;

    void switchAllAppsOwner() = 63;

    // purchase
    void syncCertificates(in List<String> certificates) = 70;

    void syncSignatures(in List<String> signatures) = 71;

    void exit() = 100;
}
