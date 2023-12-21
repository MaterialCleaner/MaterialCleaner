package me.gm.cleaner.browser;

import me.gm.cleaner.browser.IProgressListener;
import me.gm.cleaner.model.ParceledBasicFileAttributes;
import me.gm.cleaner.model.ParceledCopyOptions;
import me.gm.cleaner.model.ParceledListSlice;
import me.gm.cleaner.model.ParceledPath;
import me.gm.cleaner.model.UnixFileKey;

interface IRootFileService {

    ParceledBasicFileAttributes readAttributes(in IProgressListener progressListener, String file, boolean followLinks);

    void delete(in IProgressListener progressListener, String file);

    void copy(in IProgressListener progressListener, String source, String target, in ParceledCopyOptions options);

    void move(in IProgressListener progressListener, String source, String target, in ParceledCopyOptions options);

    ParceledListSlice<ParceledPath> newDirectoryStream(in IProgressListener progressListener, String dir);
}
