package me.gm.cleaner.xposed;

import static me.gm.cleaner.xposed.MediaProviderHook.TYPE_INSERT;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.text.TextUtils;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.util.regex.Pattern;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

@RequiresApi(api = Build.VERSION_CODES.Q)
public class InsertHooker extends XC_MethodHook {
    private final static String DIRECTORY_THUMBNAILS = ".thumbnails";

    private final static int IMAGES_MEDIA = 1;
    private final static int IMAGES_MEDIA_ID = 2;
    private final static int IMAGES_MEDIA_ID_THUMBNAIL = 3;
    private final static int IMAGES_THUMBNAILS = 4;
    private final static int IMAGES_THUMBNAILS_ID = 5;

    private final static int AUDIO_MEDIA = 100;
    private final static int AUDIO_MEDIA_ID = 101;
    private final static int AUDIO_MEDIA_ID_GENRES = 102;
    private final static int AUDIO_MEDIA_ID_GENRES_ID = 103;
    private final static int AUDIO_GENRES = 106;
    private final static int AUDIO_GENRES_ID = 107;
    private final static int AUDIO_GENRES_ID_MEMBERS = 108;
    private final static int AUDIO_GENRES_ALL_MEMBERS = 109;
    private final static int AUDIO_PLAYLISTS = 110;
    private final static int AUDIO_PLAYLISTS_ID = 111;
    private final static int AUDIO_PLAYLISTS_ID_MEMBERS = 112;
    private final static int AUDIO_PLAYLISTS_ID_MEMBERS_ID = 113;
    private final static int AUDIO_ARTISTS = 114;
    private final static int AUDIO_ARTISTS_ID = 115;
    private final static int AUDIO_ALBUMS = 116;
    private final static int AUDIO_ALBUMS_ID = 117;
    private final static int AUDIO_ARTISTS_ID_ALBUMS = 118;
    private final static int AUDIO_ALBUMART = 119;
    private final static int AUDIO_ALBUMART_ID = 120;
    private final static int AUDIO_ALBUMART_FILE_ID = 121;

    private final static int VIDEO_MEDIA = 200;
    private final static int VIDEO_MEDIA_ID = 201;
    private final static int VIDEO_MEDIA_ID_THUMBNAIL = 202;
    private final static int VIDEO_THUMBNAILS = 203;
    private final static int VIDEO_THUMBNAILS_ID = 204;

    private final static int DOWNLOADS = 800;
    private final static int DOWNLOADS_ID = 801;

    private final MediaProviderHook mHook;
    private final MediaProviderHooksService mService;
    private final ClassLoader mClassLoader;

    public InsertHooker(MediaProviderHook hook, MediaProviderHooksService service, ClassLoader classLoader) {
        mHook = hook;
        mService = service;
        mClassLoader = classLoader;
    }

    private final static Pattern KNOWN_APP_DIR_PATHS = Pattern.compile(
            "(?i)(^/storage/[^/]+/(?:([0-9]+)/)?Android/(?:data|media|obb|sandbox)/)([^/]+)(/.*)?");

    private String extractPathOwnerPackageName(String path) {
        final var m = KNOWN_APP_DIR_PATHS.matcher(path);
        if (m.matches()) {
            return m.group(3);
        }
        return null;
    }

    @Override
    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        if (mHook.isFuseThread()) {
            return;
        }
        /** ARGUMENTS */
        int match;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            match = (int) param.args[2];
        } else {
            match = (int) param.args[1];
        }
        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            uri = (Uri) param.args[3];
        } else {
            uri = (Uri) param.args[2];
        }
        Bundle extras;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            extras = (Bundle) param.args[4];
        } else {
            extras = Bundle.EMPTY;
        }
        ContentValues values;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            values = (ContentValues) param.args[5];
        } else {
            values = (ContentValues) param.args[3];
        }
        int mediaType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            mediaType = (int) param.args[6];
        } else {
            mediaType = (int) param.args[4];
        }

        /** PARSE */
        final var mimeType = values.getAsString(MediaStore.MediaColumns.MIME_TYPE);
        final var wasPathEmpty = wasPathEmpty(values);
        if (wasPathEmpty) {
            // Generate path when undefined
            ensureUniqueFileColumns(param.thisObject, match, uri, values, mimeType);
        }
        final var data = values.getAsString(MediaStore.MediaColumns.DATA);
        if (wasPathEmpty) {
            // Restore to allow mkdir
            values.remove(MediaStore.MediaColumns.DATA);
        }

        /** REDIRECT */
        mService.whileAlive(service -> {
            try {
                final var mountedPath = service.getMountedPath(
                        mHook.getCallingPackage(param.thisObject), data, TYPE_INSERT);
                if (mountedPath != null && !data.equals(mountedPath) &&
                        TextUtils.isEmpty(extractPathOwnerPackageName(mountedPath))) {
                    values.put(MediaStore.MediaColumns.DATA, mountedPath);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });
    }

    private boolean wasPathEmpty(ContentValues values) {
        return !values.containsKey(MediaStore.MediaColumns.DATA)
                || values.getAsString(MediaStore.MediaColumns.DATA).isEmpty();
    }

    private void ensureUniqueFileColumns(Object mp, int match, Uri uri,
                                         ContentValues values, String mimeType) {
        var defaultPrimary = Environment.DIRECTORY_DOWNLOADS;
        String defaultSecondary = null;
        switch (match) {
            case AUDIO_MEDIA:
            case AUDIO_MEDIA_ID:
                defaultPrimary = Environment.DIRECTORY_MUSIC;
                break;
            case VIDEO_MEDIA:
            case VIDEO_MEDIA_ID:
                defaultPrimary = Environment.DIRECTORY_MOVIES;
                break;
            case IMAGES_MEDIA:
            case IMAGES_MEDIA_ID:
                defaultPrimary = Environment.DIRECTORY_PICTURES;
                break;
            case AUDIO_ALBUMART:
            case AUDIO_ALBUMART_ID:
                defaultPrimary = Environment.DIRECTORY_MUSIC;
                defaultSecondary = DIRECTORY_THUMBNAILS;
                break;
            case VIDEO_THUMBNAILS:
            case VIDEO_THUMBNAILS_ID:
                defaultPrimary = Environment.DIRECTORY_MOVIES;
                defaultSecondary = DIRECTORY_THUMBNAILS;
                break;
            case IMAGES_THUMBNAILS:
            case IMAGES_THUMBNAILS_ID:
                defaultPrimary = Environment.DIRECTORY_PICTURES;
                defaultSecondary = DIRECTORY_THUMBNAILS;
                break;
            case AUDIO_PLAYLISTS:
            case AUDIO_PLAYLISTS_ID:
                defaultPrimary = Environment.DIRECTORY_MUSIC;
                break;
            case DOWNLOADS:
            case DOWNLOADS_ID:
                defaultPrimary = Environment.DIRECTORY_DOWNLOADS;
                break;
        }
        // Give ourselves reasonable defaults when missing
        if (TextUtils.isEmpty(values.getAsString(MediaStore.MediaColumns.DISPLAY_NAME))) {
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, String.valueOf(System.currentTimeMillis()));
        }
        // Use default directories when missing
        if (TextUtils.isEmpty(values.getAsString(MediaStore.MediaColumns.RELATIVE_PATH))) {
            if (defaultSecondary != null) {
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, defaultPrimary + "/" + defaultSecondary);
            } else {
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, defaultPrimary + "/");
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            final var resolvedVolumeName = (String) XposedHelpers.callMethod(
                    mp, "resolveVolumeName", uri);
            final var volumePath = (File) XposedHelpers.callMethod(
                    mp, "getVolumePath", resolvedVolumeName);

            final var fileUtilsClass = XposedHelpers.findClass(
                    "com.android.providers.media.util.FileUtils", mClassLoader);
            final var isFuseThread = (boolean) XposedHelpers.callMethod(mp, "isFuseThread");
            XposedHelpers.callStaticMethod(fileUtilsClass, "sanitizeValues", values, !isFuseThread);
            XposedHelpers.callStaticMethod(
                    fileUtilsClass, "computeDataFromValues", values, volumePath, isFuseThread);

            var res = new File(values.getAsString(MediaStore.MediaColumns.DATA));
            res = (File) XposedHelpers.callStaticMethod(
                    fileUtilsClass, "buildUniqueFile",
                    res.getParentFile(), mimeType, res.getName());

            values.put(MediaStore.MediaColumns.DATA, res.getAbsolutePath());
        } else {
            final var resolvedVolumeName = (String) XposedHelpers.callMethod(
                    mp, "resolveVolumeName", uri);

            final var relativePath = XposedHelpers.callMethod(
                    mp, "sanitizePath",
                    values.getAsString(MediaStore.MediaColumns.RELATIVE_PATH)
            );
            final var displayName = XposedHelpers.callMethod(
                    mp, "sanitizeDisplayName",
                    values.getAsString(MediaStore.MediaColumns.DISPLAY_NAME)
            );

            var res = (File) XposedHelpers.callMethod(
                    mp, "getVolumePath", resolvedVolumeName);
            res = (File) XposedHelpers.callStaticMethod(
                    Environment.class, "buildPath", res, relativePath);
            res = (File) XposedHelpers.callStaticMethod(
                    FileUtils.class, "buildUniqueFile", res, mimeType, displayName);

            values.put(MediaStore.MediaColumns.DATA, res.getAbsolutePath());
        }
    }
}
