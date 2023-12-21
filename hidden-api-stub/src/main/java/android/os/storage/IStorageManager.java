package android.os.storage;

import android.os.Binder;
import android.os.IBinder;

/**
 * WARNING! Update IMountService.h and IMountService.cpp if you change this
 * file. In particular, the transaction ids below must match the
 * _TRANSACTION enum in IMountService.cpp
 *
 * @hide - Applications should use android.os.storage.StorageManager to access
 * storage functions.
 */
public interface IStorageManager extends android.os.IInterface {
    /**
     * Registers an IStorageEventListener for receiving async notifications.
     */
    void registerListener(android.os.storage.IStorageEventListener listener) throws android.os.RemoteException;

    /**
     * Unregisters an IStorageEventListener
     */
    void unregisterListener(android.os.storage.IStorageEventListener listener) throws android.os.RemoteException;

    /**
     * Checks whether the specified Opaque Binary Blob (OBB) is mounted
     * somewhere.
     */
    boolean isObbMounted(java.lang.String rawPath) throws android.os.RemoteException;

    /**
     * Gets the path to the mounted Opaque Binary Blob (OBB).
     */
    java.lang.String getMountedObbPath(java.lang.String rawPath) throws android.os.RemoteException;

    android.os.storage.VolumeInfo[] getVolumes(int flags) throws android.os.RemoteException;

    abstract class Stub extends Binder implements IStorageManager {

        public static IStorageManager asInterface(IBinder obj) {
            throw new RuntimeException("STUB");
        }
    }
}
