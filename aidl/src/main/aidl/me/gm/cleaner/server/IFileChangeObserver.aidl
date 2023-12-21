package me.gm.cleaner.server;

oneway interface IFileChangeObserver {
    void onEvent(long timeMillis, String packageName, String path, int flags, boolean isAppSpecificStorage);
}
