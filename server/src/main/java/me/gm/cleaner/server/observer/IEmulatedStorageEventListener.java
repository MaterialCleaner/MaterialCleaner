package me.gm.cleaner.server.observer;

import android.os.storage.VolumeInfo;

public interface IEmulatedStorageEventListener {

    void onEmulatedStorageMounted(final VolumeInfo vol, final boolean isPrimary,
                                  final boolean isJustMounted);

    void onEmulatedStorageUnmounted(final VolumeInfo vol);
}
