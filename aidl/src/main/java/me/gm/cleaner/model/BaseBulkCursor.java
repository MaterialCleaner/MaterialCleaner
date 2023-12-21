package me.gm.cleaner.model;

import static android.os.IBinder.FIRST_CALL_TRANSACTION;

import android.database.Cursor;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

abstract class BaseBulkCursor<T> implements Parcelable, Closeable {
    private static final String TAG = "BulkCursor";
    private static final int TRANSACTION_load = FIRST_CALL_TRANSACTION;
    private static final int TRANSACTION_close = FIRST_CALL_TRANSACTION + 1;
    private Cursor mCursor;
    private Function<Cursor, T> mCreator;

    private ClassLoader mLoader;
    private int mSize;
    private Creator<?> mParcelableCreator;
    private IBinder mRetriever;

    public BaseBulkCursor(Cursor cursor, Function<Cursor, T> creator) {
        mCursor = cursor;
        mCreator = creator;
    }

    BaseBulkCursor(Parcel p, ClassLoader loader) {
        mLoader = loader;
        mSize = p.readInt();
        mParcelableCreator = readParcelableCreator(p, mLoader);
        mRetriever = p.readStrongBinder();
    }

    public List<T> load(int size) {
        if (size <= 0 || mParcelableCreator == null) {
            return Collections.emptyList();
        }
        var data = Parcel.obtain();
        var reply = Parcel.obtain();
        data.writeInt(size);
        try {
            mRetriever.transact(TRANSACTION_load, data, reply, 0);
        } catch (RemoteException e) {
            Log.w(TAG, "Failure retrieving array.", e);
            return Collections.emptyList();
        }
        var replySize = reply.readInt();
        var list = new ArrayList<T>(replySize);
        Class<?> listElementClass = null;
        for (var i = 0; i < replySize; i++) {
            final var parcelable = readCreator(mParcelableCreator, reply, mLoader);
            if (listElementClass == null) {
                listElementClass = parcelable.getClass();
            } else {
                verifySameType(listElementClass, parcelable.getClass());
            }
            list.add(parcelable);
        }
        reply.recycle();
        data.recycle();
        return list;
    }

    @Override
    public void close() {
        if (mRetriever == null || !mRetriever.pingBinder()) {
            return;
        }
        var data = Parcel.obtain();
        var reply = Parcel.obtain();
        try {
            mRetriever.transact(TRANSACTION_close, data, reply, 0);
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to close.", e);
        }
        reply.recycle();
        data.recycle();
    }

    private T readCreator(Creator<?> creator, Parcel p, ClassLoader loader) {
        if (creator instanceof ClassLoaderCreator<?>) {
            ClassLoaderCreator<?> classLoaderCreator =
                    (ClassLoaderCreator<?>) creator;
            return (T) classLoaderCreator.createFromParcel(p, loader);
        }
        return (T) creator.createFromParcel(p);
    }

    private static void verifySameType(final Class<?> expected, final Class<?> actual) {
        if (!actual.equals(expected)) {
            throw new IllegalArgumentException("Can't unparcel type "
                    + (actual == null ? null : actual.getName()) + " in list of type "
                    + (expected == null ? null : expected.getName()));
        }
    }

    public int getCount() {
        return mSize;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        final var N = mCursor.getCount();
        final var callFlags = flags;
        dest.writeInt(N);
        if (N > 0 && mCursor.moveToNext()) {
            writeParcelableCreator(mCreator.apply(mCursor), dest);
            mCursor.moveToPrevious();
        } else {
            return;
        }
        Binder retriever = new Binder() {
            @Override
            protected boolean onTransact(int code, Parcel data, Parcel reply, int flags)
                    throws RemoteException {
                switch (code) {
                    case TRANSACTION_load:
                        var requestSize = data.readInt();
                        var list = new ArrayList<T>(requestSize);
                        while (list.size() < requestSize && mCursor.moveToNext()) {
                            list.add(mCreator.apply(mCursor));
                        }
                        if (!mCursor.isClosed() && mCursor.isAfterLast()) {
                            mCursor.close();
                        }
                        reply.writeInt(list.size());
                        for (var parcelable : list) {
                            writeElement(parcelable, reply, callFlags);
                        }
                        return true;
                    case TRANSACTION_close:
                        if (!mCursor.isClosed()) {
                            mCursor.close();
                        }
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
        };
        dest.writeStrongBinder(retriever);
    }

    protected abstract void writeElement(T parcelable, Parcel reply, int callFlags);

    protected abstract void writeParcelableCreator(T parcelable, Parcel dest);

    protected abstract Creator<?> readParcelableCreator(Parcel from, ClassLoader loader);
}
