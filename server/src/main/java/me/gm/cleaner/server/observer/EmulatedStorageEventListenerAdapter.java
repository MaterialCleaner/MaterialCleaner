package me.gm.cleaner.server.observer;

import android.os.Build;
import android.os.storage.VolumeInfo;
import android.util.Log;

import androidx.annotation.CallSuper;

import java.util.concurrent.CopyOnWriteArrayList;

import api.SystemService;
import hidden.StorageEventListenerAdapter;
import me.gm.cleaner.server.BuildConfig;

public class EmulatedStorageEventListenerAdapter extends StorageEventListenerAdapter {
    public static volatile boolean isPrimaryEmulatedStorageMounted = false;
    private final CopyOnWriteArrayList<IEmulatedStorageEventListener> mListeners = new CopyOnWriteArrayList<>();

    public static int getMountUserId(final VolumeInfo vol) {
        if (vol.type != VolumeInfo.TYPE_EMULATED) {
            return -1;
        }
        final var mountUserId = vol.mountUserId;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q && mountUserId == -10000) {
            return 0;
        }
        return mountUserId;
    }

    @CallSuper
    @Override
    public void onVolumeStateChanged(final VolumeInfo vol, final int oldState, final int newState) {
        if (vol.type == VolumeInfo.TYPE_EMULATED) {
            final var isPrimary = getMountUserId(vol) == 0;
            if (newState == VolumeInfo.STATE_MOUNTED) {
                if (isPrimary) {
                    isPrimaryEmulatedStorageMounted = true;
                }
                for (final var l : mListeners) {
                    l.onEmulatedStorageMounted(vol, isPrimary, true);
                }
            } else if (isPrimary) {
                isPrimaryEmulatedStorageMounted = false;
                for (final var l : mListeners) {
                    l.onEmulatedStorageUnmounted(vol);
                }
            }
        }
        Log.i(BuildConfig.LIBRARY_PACKAGE_NAME, String.valueOf(vol));
    }

    private void dispatchEventsForMountedVolumes(final IEmulatedStorageEventListener listener) {
        for (final var vol : SystemService.getVolumes(0)) {
            if (vol.type == VolumeInfo.TYPE_EMULATED) {
                final var isPrimary = getMountUserId(vol) == 0;
                if (isPrimary) {
                    isPrimaryEmulatedStorageMounted = true;
                }
                listener.onEmulatedStorageMounted(vol, isPrimary, false);
            }
            Log.i(BuildConfig.LIBRARY_PACKAGE_NAME, String.valueOf(vol));
        }
    }

    public void registerListener(final IEmulatedStorageEventListener listener) {
        mListeners.add(listener);
        dispatchEventsForMountedVolumes(listener);
    }

    public void unregisterListener(final IEmulatedStorageEventListener listener) {
        mListeners.remove(listener);
    }
}
