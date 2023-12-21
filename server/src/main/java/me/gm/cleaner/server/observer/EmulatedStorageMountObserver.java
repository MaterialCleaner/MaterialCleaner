package me.gm.cleaner.server.observer;

import androidx.annotation.CallSuper;

import api.SystemService;

public class EmulatedStorageMountObserver extends BaseObserver {
    private final EmulatedStorageEventListenerAdapter mListener = new EmulatedStorageEventListenerAdapter();

    public void registerListener(final IEmulatedStorageEventListener listener) {
        mListener.registerListener(listener);
    }

    public void unregisterListener(final IEmulatedStorageEventListener listener) {
        mListener.unregisterListener(listener);
    }

    @CallSuper
    @Override
    protected void onStart() {
        super.onStart();
        SystemService.registerStorageEventListener(mListener);
    }

    @CallSuper
    @Override
    protected void onDestroy() {
        super.onDestroy();
        SystemService.unregisterStorageEventListener(mListener);
    }
}
