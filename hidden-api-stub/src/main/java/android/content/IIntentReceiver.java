package android.content;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;

public interface IIntentReceiver extends IInterface {

    void performReceive(Intent intent, int resultCode, String data, Bundle extras,
                        boolean ordered, boolean sticky, int sendingUser)
            throws android.os.RemoteException;

    abstract class Stub extends Binder implements IIntentReceiver {

        @Override
        public android.os.IBinder asBinder() {
            throw new UnsupportedOperationException();
        }

        public static IIntentSender asInterface(IBinder binder) {
            throw new UnsupportedOperationException();
        }
    }
}
