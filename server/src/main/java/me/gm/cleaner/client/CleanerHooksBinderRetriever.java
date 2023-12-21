package me.gm.cleaner.client;

import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;

public class CleanerHooksBinderRetriever {
    private static final String HOOKED_SERVICE_NAME = "mount";
    private static final String HOOKED_SERVICE_DESCRIPTOR = "android.os.storage.IStorageManager";
    private static final int TRANSACTION_CODE = ('_' << 24) | ('C' << 16) | ('L' << 8) | 'R';

    public static IBinder get() {
        var hookedService = ServiceManager.getService(HOOKED_SERVICE_NAME);
        if (hookedService == null) {
            return null;
        }
        var data = Parcel.obtain();
        var reply = Parcel.obtain();

        try {
            data.writeInterfaceToken(HOOKED_SERVICE_DESCRIPTOR);
            hookedService.transact(TRANSACTION_CODE, data, reply, 0);
            reply.readException();
            return reply.readStrongBinder();
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        } finally {
            data.recycle();
            reply.recycle();
        }
    }
}
