package me.gm.cleaner.server.observer;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.FileObserver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

public class MagiskDenyListObserver {
    private static final FileObserver sMagiskDbObserver = new FileObserver(
            "/data/adb/magisk.db", FileObserver.MODIFY | FileObserver.CREATE) {
        @Override
        public void onEvent(final int event, @Nullable final String path) {
            synchronized (sMagiskDbObserver) {
                sMagiskDenyList = null;
                sMagiskDenySet = null;
            }
        }
    };
    private static volatile List<String> sMagiskDenyList;
    private static volatile Set<String> sMagiskDenySet;

    static {
        initialize();
    }

    public static void initialize() {
        sMagiskDbObserver.startWatching();
    }

    public static void close() {
        sMagiskDbObserver.stopWatching();
    }

    private static List<String> queryMagiskDenyList() {
        try (final SQLiteDatabase magiskDb = SQLiteDatabase.openDatabase(
                "/data/adb/magisk.db", null, SQLiteDatabase.OPEN_READONLY
        )) {
            final List<String> result = new ArrayList<>();
            try (final Cursor cursor = magiskDb.rawQuery(
                    "SELECT value FROM settings WHERE `key`=?", new String[]{"denylist"}
            )) {
                if (!cursor.moveToNext()) return result;
                final int valueIndex = cursor.getColumnIndex("value");
                if (valueIndex >= 0 && cursor.getInt(valueIndex) == 0) return result;
            }

            try (final Cursor cursor = magiskDb.rawQuery(
                    "SELECT DISTINCT package_name FROM denylist", null
            )) {
                if (cursor == null) return result;
                final int packageNameIndex = cursor.getColumnIndex("package_name");
                while (cursor.moveToNext()) {
                    result.add(cursor.getString(packageNameIndex));
                }
                return result;
            }
        } catch (final Throwable ignored) {
        }
        return Collections.emptyList();
    }

    public static List<String> getMagiskDenyList() {
        synchronized (sMagiskDbObserver) {
            if (sMagiskDenyList == null) {
                sMagiskDenyList = queryMagiskDenyList();
            }
            return sMagiskDenyList;
        }
    }

    public static boolean isInDenyList(String packageName) {
        var magiskDenySet = sMagiskDenySet;
        if (magiskDenySet == null) {
            synchronized (sMagiskDbObserver) {
                if (sMagiskDenySet == null) {
                    sMagiskDenySet = Collections.synchronizedSet(new HashSet<>(getMagiskDenyList()));
                }
                magiskDenySet = sMagiskDenySet;
            }
        }
        return magiskDenySet.contains(packageName);
    }
}
