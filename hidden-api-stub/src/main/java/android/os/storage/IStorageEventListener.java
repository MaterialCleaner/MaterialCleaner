package android.os.storage;

import android.os.Binder;
import android.os.IBinder;

/**
 * Callback class for receiving events from StorageManagerService.
 * <p>
 * Don't change the existing transaction Ids as they could be used in the native code.
 * When adding a new method, assign the next available transaction id.
 */
public interface IStorageEventListener {
    /**
     * Detection state of USB Mass Storage has changed
     *
     * @param connected true if a UMS host is connected.
     */
    void onUsbMassStorageConnectionChanged(boolean connected) throws android.os.RemoteException;

    /**
     * Storage state has changed.
     *
     * @param path     The volume mount path.
     * @param oldState The old state of the volume.
     * @param newState The new state of the volume. Note: State is one of the
     *                 values returned by Environment.getExternalStorageState()
     */
    void onStorageStateChanged(java.lang.String path, java.lang.String oldState, java.lang.String newState) throws android.os.RemoteException;

    void onVolumeStateChanged(android.os.storage.VolumeInfo vol, int oldState, int newState) throws android.os.RemoteException;

    void onVolumeRecordChanged(android.os.storage.VolumeRecord rec) throws android.os.RemoteException;

    void onVolumeForgotten(java.lang.String fsUuid) throws android.os.RemoteException;

    void onDiskScanned(android.os.storage.DiskInfo disk, int volumeCount) throws android.os.RemoteException;

    void onDiskDestroyed(android.os.storage.DiskInfo disk) throws android.os.RemoteException;

    abstract class Stub extends Binder implements IStorageEventListener {
    }

    IBinder asBinder();
}
