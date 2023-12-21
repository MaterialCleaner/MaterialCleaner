package me.gm.cleaner.server;

import me.gm.cleaner.server.ICleanerServerCallback;

interface IMediaProviderHooksService {

    int getVersion() = 0;

    void setCleanerServerBinder(in ICleanerServerCallback iinterface) = 1;

    void setReadOnlyPaths(in Map<String, List> packageNameToReadOnlyPaths) = 10;

    void setMountPoint(in List<String> value) = 11;

    void setRecordExternalAppSpecificStorage(boolean value) = 12;
}
