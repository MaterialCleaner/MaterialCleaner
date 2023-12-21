/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.gm.cleaner.client.ui.storageredirect;

import android.content.ClipDescription;
import android.mtp.MtpConstants;
import android.provider.MediaStore.Files.FileColumns;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.Locale;
import java.util.Objects;

public class MimeUtils {
    /**
     * Variant of {@link Objects#equals(Object, Object)} but which tests with
     * case-insensitivity.
     */
    public static boolean equalIgnoreCase(@Nullable String a, @Nullable String b) {
        return (a != null) && a.equalsIgnoreCase(b);
    }

    /**
     * Variant of {@link String#startsWith(String)} but which tests with
     * case-insensitivity.
     */
    public static boolean startsWithIgnoreCase(@Nullable String target, @Nullable String other) {
        if (target == null || other == null) return false;
        if (other.length() > target.length()) return false;
        return target.regionMatches(true, 0, other, 0, other.length());
    }

    /**
     * Resolve the MIME type of the given file, returning
     * {@code application/octet-stream} if the type cannot be determined.
     */
    public static @NonNull
    String resolveMimeType(@NonNull File file) {
        final String extension = extractFileExtension(file.getPath());
        if (extension == null) return ClipDescription.MIMETYPE_UNKNOWN;

        final String mimeType = MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(extension.toLowerCase(Locale.ROOT));
        if (mimeType == null) return ClipDescription.MIMETYPE_UNKNOWN;

        return mimeType;
    }

    public static @Nullable
    String extractFileExtension(@Nullable String data) {
        if (data == null) return null;

        final int lastDot = data.lastIndexOf('.');
        if (lastDot == -1) {
            return null;
        } else {
            return data.substring(lastDot + 1);
        }
    }

    /**
     * Resolve the {@link FileColumns#MEDIA_TYPE} of the given MIME type. This
     * carefully checks for more specific types before generic ones, such as
     * treating {@code audio/mpegurl} as a playlist instead of an audio file.
     */
    public static int resolveMediaType(@NonNull String mimeType) {
        if (isPlaylistMimeType(mimeType)) {
            return FileColumns.MEDIA_TYPE_PLAYLIST;
        } else if (isSubtitleMimeType(mimeType)) {
            return FileColumns.MEDIA_TYPE_SUBTITLE;
        } else if (isAudioMimeType(mimeType)) {
            return FileColumns.MEDIA_TYPE_AUDIO;
        } else if (isVideoMimeType(mimeType)) {
            return FileColumns.MEDIA_TYPE_VIDEO;
        } else if (isImageMimeType(mimeType)) {
            return FileColumns.MEDIA_TYPE_IMAGE;
        } else if (isDocumentMimeType(mimeType)) {
            return FileColumns.MEDIA_TYPE_DOCUMENT;
        } else {
            return FileColumns.MEDIA_TYPE_NONE;
        }
    }

    /**
     * Resolve the {@link FileColumns#FORMAT} of the given MIME type. Note that
     * since this column isn't public API, we're okay only getting very rough
     * values in place, and it's not worthwhile to build out complex matching.
     */
    public static int resolveFormatCode(@Nullable String mimeType) {
        final int mediaType = resolveMediaType(mimeType);
        switch (mediaType) {
            case FileColumns.MEDIA_TYPE_AUDIO:
                return MtpConstants.FORMAT_UNDEFINED_AUDIO;
            case FileColumns.MEDIA_TYPE_VIDEO:
                return MtpConstants.FORMAT_UNDEFINED_VIDEO;
            case FileColumns.MEDIA_TYPE_IMAGE:
                return MtpConstants.FORMAT_DEFINED;
            default:
                return MtpConstants.FORMAT_UNDEFINED;
        }
    }

    public static @NonNull
    String extractPrimaryType(@NonNull String mimeType) {
        final int slash = mimeType.indexOf('/');
        if (slash == -1) {
            throw new IllegalArgumentException();
        }
        return mimeType.substring(0, slash);
    }

    public static boolean isAudioMimeType(@Nullable String mimeType) {
        if (mimeType == null) return false;
        return startsWithIgnoreCase(mimeType, "audio/");
    }

    public static boolean isVideoMimeType(@Nullable String mimeType) {
        if (mimeType == null) return false;
        return startsWithIgnoreCase(mimeType, "video/");
    }

    public static boolean isImageMimeType(@Nullable String mimeType) {
        if (mimeType == null) return false;
        return startsWithIgnoreCase(mimeType, "image/");
    }

    public static boolean isPlaylistMimeType(@Nullable String mimeType) {
        if (mimeType == null) return false;
        switch (mimeType.toLowerCase(Locale.ROOT)) {
            case "application/vnd.apple.mpegurl":
            case "application/vnd.ms-wpl":
            case "application/x-extension-smpl":
            case "application/x-mpegurl":
            case "application/xspf+xml":
            case "audio/mpegurl":
            case "audio/x-mpegurl":
            case "audio/x-scpls":
                return true;
            default:
                return false;
        }
    }

    public static boolean isSubtitleMimeType(@Nullable String mimeType) {
        if (mimeType == null) return false;
        switch (mimeType.toLowerCase(Locale.ROOT)) {
            case "application/lrc":
            case "application/smil+xml":
            case "application/ttml+xml":
            case "application/x-extension-cap":
            case "application/x-extension-srt":
            case "application/x-extension-sub":
            case "application/x-extension-vtt":
            case "application/x-subrip":
            case "text/vtt":
                return true;
            default:
                return false;
        }
    }

    public static boolean isDocumentMimeType(@Nullable String mimeType) {
        if (mimeType == null) return false;

        if (startsWithIgnoreCase(mimeType, "text/")) return true;

        switch (mimeType.toLowerCase(Locale.ROOT)) {
            case "application/epub+zip":
            case "application/msword":
            case "application/pdf":
            case "application/rtf":
            case "application/vnd.ms-excel":
            case "application/vnd.ms-excel.addin.macroenabled.12":
            case "application/vnd.ms-excel.sheet.binary.macroenabled.12":
            case "application/vnd.ms-excel.sheet.macroenabled.12":
            case "application/vnd.ms-excel.template.macroenabled.12":
            case "application/vnd.ms-powerpoint":
            case "application/vnd.ms-powerpoint.addin.macroenabled.12":
            case "application/vnd.ms-powerpoint.presentation.macroenabled.12":
            case "application/vnd.ms-powerpoint.slideshow.macroenabled.12":
            case "application/vnd.ms-powerpoint.template.macroenabled.12":
            case "application/vnd.ms-word.document.macroenabled.12":
            case "application/vnd.ms-word.template.macroenabled.12":
            case "application/vnd.oasis.opendocument.chart":
            case "application/vnd.oasis.opendocument.database":
            case "application/vnd.oasis.opendocument.formula":
            case "application/vnd.oasis.opendocument.graphics":
            case "application/vnd.oasis.opendocument.graphics-template":
            case "application/vnd.oasis.opendocument.presentation":
            case "application/vnd.oasis.opendocument.presentation-template":
            case "application/vnd.oasis.opendocument.spreadsheet":
            case "application/vnd.oasis.opendocument.spreadsheet-template":
            case "application/vnd.oasis.opendocument.text":
            case "application/vnd.oasis.opendocument.text-master":
            case "application/vnd.oasis.opendocument.text-template":
            case "application/vnd.oasis.opendocument.text-web":
            case "application/vnd.openxmlformats-officedocument.presentationml.presentation":
            case "application/vnd.openxmlformats-officedocument.presentationml.slideshow":
            case "application/vnd.openxmlformats-officedocument.presentationml.template":
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet":
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.template":
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.template":
            case "application/vnd.stardivision.calc":
            case "application/vnd.stardivision.chart":
            case "application/vnd.stardivision.draw":
            case "application/vnd.stardivision.impress":
            case "application/vnd.stardivision.impress-packed":
            case "application/vnd.stardivision.mail":
            case "application/vnd.stardivision.math":
            case "application/vnd.stardivision.writer":
            case "application/vnd.stardivision.writer-global":
            case "application/vnd.sun.xml.calc":
            case "application/vnd.sun.xml.calc.template":
            case "application/vnd.sun.xml.draw":
            case "application/vnd.sun.xml.draw.template":
            case "application/vnd.sun.xml.impress":
            case "application/vnd.sun.xml.impress.template":
            case "application/vnd.sun.xml.math":
            case "application/vnd.sun.xml.writer":
            case "application/vnd.sun.xml.writer.global":
            case "application/vnd.sun.xml.writer.template":
            case "application/x-mspublisher":
                return true;
            default:
                return false;
        }
    }
}
