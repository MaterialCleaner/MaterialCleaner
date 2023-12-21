package android.os;

public interface IVold extends IInterface {

    abstract class Stub extends Binder implements IVold {

        public static IVold asInterface(IBinder obj) {
            throw new RuntimeException("STUB");
        }
    }
}
