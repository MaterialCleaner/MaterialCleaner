package hidden;

import android.os.IBinder;
import android.os.RemoteException;
import android.os.storage.DiskInfo;
import android.os.storage.IStorageEventListener;
import android.os.storage.VolumeInfo;
import android.os.storage.VolumeRecord;

public class StorageEventListenerAdapter extends IStorageEventListener.Stub {

    @Override
    public void onUsbMassStorageConnectionChanged(boolean connected) throws RemoteException {

    }

    @Override
    public void onStorageStateChanged(String path, String oldState, String newState) throws RemoteException {

    }

    @Override
    public void onVolumeStateChanged(VolumeInfo vol, int oldState, int newState) throws RemoteException {

    }

    @Override
    public void onVolumeRecordChanged(VolumeRecord rec) throws RemoteException {

    }

    @Override
    public void onVolumeForgotten(String fsUuid) throws RemoteException {

    }

    @Override
    public void onDiskScanned(DiskInfo disk, int volumeCount) throws RemoteException {

    }

    @Override
    public void onDiskDestroyed(DiskInfo disk) throws RemoteException {

    }

    @Override
    public IBinder asBinder() {
        return this;
    }
}
