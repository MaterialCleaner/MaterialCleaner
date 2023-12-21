package me.gm.cleaner.browser;

import me.gm.cleaner.model.ParceledException;

interface IProgressListener {

    void onProgress(float progress);

    void onException(in ParceledException exception);
}
