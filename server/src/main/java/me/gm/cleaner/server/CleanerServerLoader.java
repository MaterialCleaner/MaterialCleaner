package me.gm.cleaner.server;

import android.ddm.DdmHandleAppName;
import android.os.Build;
import android.os.Looper;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.zip.ZipFile;

import dalvik.system.InMemoryDexClassLoader;
import kotlin.io.ByteStreamsKt;
import kotlin.io.ConstantsKt;

public class CleanerServerLoader {

    public static void main(final String[] args) throws IOException, ClassNotFoundException,
            NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Log.i(BuildConfig.LIBRARY_PACKAGE_NAME,
                "Starting Cleaner server v" + BuildConfig.VERSION_CODE +
                        " on " + Build.VERSION.SDK_INT
        );
        var libClassLoader = ClassLoader.getSystemClassLoader();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final var jars = List.<String>of(
//                    "/apex/com.android.art/javalib/core-oj.jar"
            );
            for (final var jar : jars) {
                try (final var zip = new ZipFile(jar)) {
                    final var enumeration = zip.entries();
                    while (enumeration.hasMoreElements()) {
                        final var entry = enumeration.nextElement();
                        if (entry.getName().matches("classes[0-9]*?\\.dex")) {
                            final var out = new ByteArrayOutputStream((int) entry.getSize());
                            ByteStreamsKt.copyTo(
                                    zip.getInputStream(entry), out, ConstantsKt.DEFAULT_BUFFER_SIZE
                            );
                            libClassLoader = new InMemoryDexClassLoader(
                                    ByteBuffer.wrap(out.toByteArray()), libClassLoader);
                        }
                    }
                }
            }
        }
        System.loadLibrary("android");
        System.loadLibrary("compiler_rt");
        System.loadLibrary("jnigraphics");
        libClassLoader.loadClass(CleanerServerLoader.class.getName())
                .getDeclaredMethod("entry", String[].class)
                .invoke(null, (Object) args);
    }

    public static void entry(final String[] args) {
        DdmHandleAppName.setAppName("cleaner_server", 0);

        Looper.prepareMainLooper();
        new CleanerServer().onStorageManagerServiceReady();
        Looper.loop();
        throw new RuntimeException("Main thread loop unexpectedly exited");
    }
}
