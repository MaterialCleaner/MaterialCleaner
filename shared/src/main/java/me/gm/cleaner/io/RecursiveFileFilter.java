package me.gm.cleaner.io;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import me.gm.cleaner.util.FileUtils;

@Deprecated
public class RecursiveFileFilter {
    public static final int DIRECTION_TOP_DOWN = 0;
    public static final int DIRECTION_BOTTOM_UP = 1;

    private final Predicate<? super File> mDirFilter;
    private final Predicate<? super File> mFileFilter;
    private final int mDirection;
    protected final Collection<File> mExceptionalPaths = new HashSet<>();
    private final Collator mCollator = Collator.getInstance();
    private final Collection<File> mFilteredFiles = new ArrayList<>();
    private OnProgressUpdateListener mListener;
    protected final LinkedList<Integer> mHierarchyFileCount = new LinkedList<>();
    private float mProgress;

    public interface OnProgressUpdateListener {
        void onProgressUpdate(float progress);
    }

    public RecursiveFileFilter(Predicate<? super File> dirFilter,
                               Predicate<? super File> fileFilter) {
        this(dirFilter, fileFilter, DIRECTION_TOP_DOWN);
    }

    public RecursiveFileFilter(Predicate<? super File> dirFilter,
                               Predicate<? super File> fileFilter, int direction) {
        mDirFilter = dirFilter;
        mFileFilter = fileFilter;
        mDirection = direction;
    }

    public RecursiveFileFilter setOnProgressUpdateListener(OnProgressUpdateListener l) {
        mListener = l;
        return this;
    }

    public RecursiveFileFilter addExceptionalPaths(Collection<? extends File> exceptionalDirs) {
        mExceptionalPaths.addAll(exceptionalDirs);
        return this;
    }

    public Collection<File> getExceptionalPaths() {
        return mExceptionalPaths;
    }

    public Collection<File> getFilteredFiles() {
        return mFilteredFiles;
    }

    public Collection<File> start(File entry) {
        if (entry.exists() && mExceptionalPaths.stream()
                .noneMatch(it -> FileUtils.INSTANCE.startsWith(it, entry))) {
            mProgress = 0F;
            if (mDirection == DIRECTION_TOP_DOWN) {
                topDown(entry);
            } else if (mDirection == DIRECTION_BOTTOM_UP) {
                bottomUp(entry);
            } else {
                throw new IllegalArgumentException("unknown direction");
            }
        }
        return getFilteredFiles();
    }

    protected ArrayList<File> listFiles(File dir) {
        var fileList = new ArrayList<File>();
        var fs = dir.listFiles();
        if (fs != null) {
            fileList.addAll(Arrays.asList(fs));
            fileList.sort((o1, o2) -> mCollator.compare(o1.getName(), o2.getName()));
        }
        return fileList;
    }

    private void topDown(File file) {
        if (file.isDirectory()) {
            if (mDirFilter != null && mDirFilter.test(file)) {
                var exceptionalPaths = mExceptionalPaths.stream()
                        .filter(child -> FileUtils.INSTANCE.startsWith(file, child))
                        .collect(Collectors.toList());
                if (exceptionalPaths.isEmpty()) {
                    mFilteredFiles.add(file);
                } else {
                    var otherFiles = new RecursiveFileFilter(
                            dir -> exceptionalPaths.stream()
                                    .noneMatch(f -> FileUtils.INSTANCE.startsWith(dir, f)),
                            f -> !exceptionalPaths.contains(f)
                    ).start(file);
                    mFilteredFiles.addAll(otherFiles);
                }
                increaseProgress();
            } else {
                var fs = listFiles(file);
                fs.removeAll(mExceptionalPaths);
                if (fs.isEmpty()) {
                    increaseProgress();
                } else {
                    mHierarchyFileCount.addLast(fs.size());
                    for (var f : fs) {
                        topDown(f);
                    }
                    mHierarchyFileCount.removeLast();
                }
            }
        } else {
            if (mFileFilter != null && mFileFilter.test(file)) {
                mFilteredFiles.add(file);
            }
            increaseProgress();
        }
    }

    private void bottomUp(File file) {
        if (file.isDirectory()) {
            var fs = listFiles(file);
            fs.removeIf(f -> mExceptionalPaths.stream()
                    .anyMatch(parent -> FileUtils.INSTANCE.startsWith(parent, f)));
            if (fs.isEmpty()) {
                increaseProgress();
            } else {
                mHierarchyFileCount.addLast(fs.size());
                for (var f : fs) {
                    bottomUp(f);
                }
                mHierarchyFileCount.removeLast();
            }
            if (mDirFilter != null && mDirFilter.test(file)) {
                mFilteredFiles.add(file);
            }
        } else {
            if (mFileFilter != null && mFileFilter.test(file)) {
                mFilteredFiles.add(file);
            }
            increaseProgress();
        }
    }

    private void increaseProgress() {
        var proportion = 1F;
        for (var count : mHierarchyFileCount) {
            proportion /= count;
        }
        mProgress += proportion;
        if (mListener != null) {
            mListener.onProgressUpdate(mProgress);
        }
    }
}
