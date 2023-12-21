package me.gm.cleaner.io;

import android.os.FileObserver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.TimeUnit;

import me.gm.cleaner.util.FileUtils;

public class RecursiveFileListener {

    //  /proc/sys/fs/inotify/max_user_instances 初始化 ifd 的数量限制
    //  /proc/sys/fs/inotify/max_queued_events ifd 文件队列长度限制
    //  /proc/sys/fs/inotify/max_user_watches 注册监听目录的数量限制
    //  既然是文件描述符，当然也受 /etc/security/limits.conf 和 /proc/sys/fs/file-max 限制
    //  https://www.jianshu.com/p/46b2bfad3d61

    Map<String, SingleFileListener> dirMap = new ConcurrentHashMap<>(1024 * 8);
    int mMask;
    boolean flagListen;
    ExecutorService singleThread;
    boolean flagSend;
    boolean flagFork;
    int number;
    File mFile;
    ForkJoinPool forkJoinPool;
    Set<File> mExceptionalDirs = new HashSet<>();
    List<String> mTargets;
    OnFileChangeListener mOnFileChangeListener;

    public RecursiveFileListener(File file, int mask) {
        mFile = file;
        mMask = mask;
        number = 0;
        flagListen = false;
        flagSend = true;
        flagFork = true;
    }

    public RecursiveFileListener startListen() {
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "20");
        forkJoinPool = ForkJoinPool.commonPool();
        singleThread = Executors.newSingleThreadExecutor();
        singleThread.execute(() -> {
            forkJoinPool.execute(new WalkFileTree(mFile));
            try {
                TimeUnit.MILLISECONDS.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            forkJoinPool.awaitQuiescence(3000, TimeUnit.MILLISECONDS);
            flagListen = true;
        });

        final WeakReference<RecursiveFileListener> weakReference = new WeakReference<>(this);
        singleThread.execute(() -> {
            if (dirMap.size() > 8192) {
                weakReference.get().stopListen();
            }
        });
        return this;
    }

    public boolean isAlive() {
        return dirMap.values().stream().allMatch(singleFileListener -> singleFileListener.isAlive);
    }

    public void stopListen() {
        Iterator<Map.Entry<String, SingleFileListener>> iterator = dirMap.entrySet().iterator();
        while (iterator.hasNext()) {
            iterator.next().getValue().stopWatching();
            iterator.remove();
        }
        if (singleThread != null) singleThread.shutdown();
    }

    public RecursiveFileListener addExceptionalPaths(Collection<? extends File> exceptionalDirs) {
        mExceptionalDirs.addAll(exceptionalDirs);
        return this;
    }

    public RecursiveFileListener setTargets(Collection<? extends String> targets) {
        mTargets = new ArrayList<>(targets);
        return this;
    }

    public RecursiveFileListener setOnFileChangeListener(OnFileChangeListener l) {
        mOnFileChangeListener = l;
        return this;
    }

    public interface OnFileChangeListener {
        void onEvent(int event, @NonNull String path, boolean isDir);
    }

    class SingleFileListener extends FileObserver {
        private static final int DIR = 0x40000000;
        public boolean isAlive = false;
        private final String mPath;

        SingleFileListener(String path, int mask) {
            super(path, mask);
            mPath = path;
        }

        @Override
        public void onEvent(int event, @Nullable String path) {
            if (!flagListen || path == null) {
                return;
            }
            path = mPath + File.separator + path;
            boolean isDir = (event & DIR) != 0;
            mOnFileChangeListener.onEvent(event, path, isDir);
            if (isDir) {
                switch (event & FileObserver.ALL_EVENTS) {
                    case FileObserver.CREATE:
                        SingleFileListener singleFileListener = new SingleFileListener(path, mMask);
                        singleFileListener.startWatching();
                        dirMap.put(path, singleFileListener);

                        var ls = new File(path).listFiles();
                        if (ls != null) {
                            for (var file : ls) {
                                if (file.isDirectory()) {
                                    onEvent(event, file.getPath());
                                } else {
                                    mOnFileChangeListener.onEvent(event, file.getPath(), false);
                                }
                            }
                        }
                        break;
                    case FileObserver.MOVED_TO:
                        forkJoinPool.execute(new WalkFileTree(new File(path)));
                        break;
                    case FileObserver.DELETE:
                    case FileObserver.DELETE_SELF:
                        SingleFileListener singleFileListener2 = dirMap.get(path);
                        if (singleFileListener2 != null) {
                            singleFileListener2.stopWatching();
                            dirMap.remove(path);
                        }
                        break;
                    case FileObserver.MOVED_FROM:
                        Iterator<Map.Entry<String, SingleFileListener>> iterator = dirMap.entrySet().iterator();
                        Map.Entry<String, SingleFileListener> entry;
                        while (iterator.hasNext()) {
                            entry = iterator.next();
                            String key = entry.getKey();
                            if (key.equals(path) || (key.startsWith(path + File.separator))) {
                                entry.getValue().stopWatching();
                                iterator.remove();
                            }
                        }
                        break;
                }
            }
        }

        @Override
        public void startWatching() {
            super.startWatching();
            isAlive = true;
        }

        @Override
        public void stopWatching() {
            super.stopWatching();
            isAlive = false;
        }
    }

    class WalkFileTree extends RecursiveAction {
        File mFile;

        WalkFileTree(File file) {
            mFile = file;
        }

        @Override
        protected void compute() {
            if (mExceptionalDirs.contains(mFile)) {
                return;
            }
            File[] parents = mFile.listFiles();
            if (parents == null) {
                return;
            }
            for (File parent : parents) {
                if (flagFork && parent.canRead() && parent.isDirectory()) {
                    if (mTargets == null) {
                        new WalkFileTree(parent).fork();
                    } else {
                        if (mTargets.stream().anyMatch(target -> FileUtils.INSTANCE.startsWith(parent, target))) {
                            new WalkFileTree(parent).fork();
                        }
                    }
                }
            }
            String path = mFile.getPath();
            SingleFileListener singleFileListener = new SingleFileListener(path, mMask);
            singleFileListener.startWatching();
            dirMap.put(path, singleFileListener);
        }
    }
}
