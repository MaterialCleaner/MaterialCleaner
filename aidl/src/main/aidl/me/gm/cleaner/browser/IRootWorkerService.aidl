package me.gm.cleaner.browser;

import me.gm.cleaner.browser.IProgressListener;

interface IRootWorkerService {

    void cancelWork(String uuid);

    void delete(String uuid, in IProgressListener listener, String file);

    void copy(String uuid, in IProgressListener listener, String source, String target);

    void move(String uuid, in IProgressListener listener, String source, String target);

    boolean snapshot(String uuid, in IProgressListener listener, String pendingSnapshotFile, String path);
}
