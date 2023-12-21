package me.gm.cleaner.server;

import android.content.IntentFilter;
import android.content.IIntentReceiver;

import me.gm.cleaner.server.ICleanerServerCallback;
import me.gm.cleaner.server.IMediaProviderHooksService;

interface ICleanerHooksService {

    int getModuleVersion() = 0;

    void setCleanerServerBinder(in ICleanerServerCallback iinterface) = 1;

    void setMediaProviderBinder(in IMediaProviderHooksService iinterface) = 2;

    boolean hasMediaProviderBinder() = 3;

    boolean waitMount(String packageName, int pid, int uid) = 10;

    String getMountedPath(String packageName, String path, int type) = 11;

    void setSrPackages(in List<String> srPackages) = 12;

    boolean isSrPackage(in String[] packageNames) = 13;

    void registerReceiver(in IIntentReceiver receiver, in IntentFilter filter, int userId, int flags) = 20;

    void unregisterReceiver(in IIntentReceiver receiver) = 21;

    void finishReceiver(in IIntentReceiver receiver, int resultCode, String resultData, in Bundle map, boolean abortBroadcast, int flags) = 22;

    void setReadOnlyPaths(in Map<String, List> packageNameToReadOnlyPaths) = 30;

    void setMountPoint(in List<String> value) = 31;

    void setRecordExternalAppSpecificStorage(boolean value) = 32;
}
