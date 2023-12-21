package me.gm.cleaner.server;

import android.os.storage.VolumeInfo;

import me.gm.cleaner.server.observer.EmulatedStorageMountObserver;
import me.gm.cleaner.server.observer.IEmulatedStorageEventListener;
import me.gm.cleaner.server.observer.ObserverManager;

public class EmulatedStorageEventListenerImpl implements IEmulatedStorageEventListener {
    private final CleanerServer mServer;

    public EmulatedStorageEventListenerImpl(final CleanerServer server) {
        mServer = server;
    }

    public void start() {
        final var observer = ObserverManager.INSTANCE.getObserver(EmulatedStorageMountObserver.class);
        if (observer != null) {
            observer.registerListener(this);
        }
    }

    @Override
    public void onEmulatedStorageMounted(final VolumeInfo vol, final boolean isPrimary,
                                         final boolean isJustMounted) {
        mServer.onEmulatedStorageMounted(vol, isPrimary, isJustMounted);
    }

    @Override
    public void onEmulatedStorageUnmounted(final VolumeInfo vol) {
        mServer.onEmulatedStorageUnmounted(vol);
    }
}
