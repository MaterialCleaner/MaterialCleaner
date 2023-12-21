package me.gm.cleaner.xposed;

import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.RemoteException;
import android.provider.MediaStore.Files.FileColumns;
import android.util.ArraySet;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Function;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class QueryHooker extends XC_MethodHook {
    private static final String INCLUDED_DEFAULT_DIRECTORIES = "android:included-default-directories";
    private static final int TYPE_QUERY = 0;
    private final MediaProviderHook mHook;
    private final MediaProviderHooksService mService;
    private final ClassLoader mClassLoader;

    public QueryHooker(MediaProviderHook hook, MediaProviderHooksService service, ClassLoader classLoader) {
        mHook = hook;
        mService = service;
        mClassLoader = classLoader;
    }

    @Override
    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        if (mHook.isFuseThread()) {
            return;
        }
        /** ARGUMENTS */
        final var uri = (Uri) param.args[0];
        final var projection = (String[]) param.args[1];
        final Bundle queryArgs;
        if (param.args[2] == null) {
            queryArgs = Bundle.EMPTY;
        } else {
            queryArgs = (Bundle) param.args[2];
        }
        final var signal = (CancellationSignal) param.args[3];

        final var callingPackage = mHook.getCallingPackage(param.thisObject);
        if ("com.android.providers.media".equals(callingPackage) ||
                "com.android.providers.media.module".equals(callingPackage)) {
            // Scanning files and internal queries.
            return;
        }

        /** PARSE */
        final var query = new Bundle(queryArgs);
        query.remove(INCLUDED_DEFAULT_DIRECTORIES);
        final var honoredArgs = new ArraySet<String>();
        final var databaseUtilsClass = XposedHelpers.findClass(
                "com.android.providers.media.util.DatabaseUtils", mClassLoader
        );
        XposedHelpers.callStaticMethod(
                databaseUtilsClass, "resolveQueryArgs", query, new Consumer<String>() {
                    @Override
                    public void accept(String s) {
                        honoredArgs.add(s);
                    }
                }, new Function<String, String>() {
                    @Override
                    public String apply(String s) {
                        return (String) XposedHelpers.callMethod(param.thisObject, "ensureCustomCollator", s);
                    }
                }
        );

        final var targetSdkVersion = (int) XposedHelpers.callMethod(
                param.thisObject, "getCallingPackageTargetSdkVersion");
        final var allowHidden = (boolean) XposedHelpers.callMethod(
                param.thisObject, "isCallingPackageAllowedHidden");
        final var table = (int) XposedHelpers.callMethod(param.thisObject, "matchUri", uri, allowHidden);

        final var dataProjection = new String[]{FileColumns.DATA};
        final var helper = XposedHelpers.callMethod(param.thisObject, "getDatabaseForUri", uri);
        final var qb = XposedHelpers.callMethod(param.thisObject, "getQueryBuilder",
                TYPE_QUERY, table, uri, query, new Consumer<String>() {
                    @Override
                    public void accept(String s) {
                        honoredArgs.add(s);
                    }
                });

        if (targetSdkVersion < Build.VERSION_CODES.R) {
            // Some apps are abusing "ORDER BY" clauses to inject "LIMIT"
            // clauses; gracefully lift them out.
            XposedHelpers.callStaticMethod(databaseUtilsClass, "recoverAbusiveSortOrder", query);

            // Some apps are abusing the Uri query parameters to inject LIMIT
            // clauses; gracefully lift them out.
            XposedHelpers.callStaticMethod(databaseUtilsClass, "recoverAbusiveLimit", uri, query);
        }

        if (targetSdkVersion < Build.VERSION_CODES.Q) {
            // Some apps are abusing the "WHERE" clause by injecting "GROUP BY"
            // clauses; gracefully lift them out.
            XposedHelpers.callStaticMethod(databaseUtilsClass, "recoverAbusiveSelection", query);
        }

        final Cursor c;
        try {
            c = (Cursor) XposedHelpers.callMethod(
                    qb, "query", helper, dataProjection, query, signal);
        } catch (XposedHelpers.InvocationTargetError e) {
            // IllegalArgumentException that thrown from the media provider. Nothing I can do.
            return;
        }
        if (c.getCount() == 0) {
            // querying nothing.
            c.close();
            return;
        }
        final var dataColumn = c.getColumnIndex(FileColumns.DATA);
        if (dataColumn == -1) {
            return;
        }
        final var data = new ArrayList<String>();
        while (c.moveToNext()) {
            data.add(c.getString(dataColumn));
        }
        c.close();

        /** RECORD */
        final var threadLocal = (ThreadLocal<?>) XposedHelpers.getObjectField(
                param.thisObject, "mCallingIdentity");
        final var uid = (int) XposedHelpers.getObjectField(threadLocal.get(), "uid");
        synchronized (mHook.mQueryRecord) {
            mHook.mQueryRecord.put(uid, System.currentTimeMillis());
            mService.whileAlive(service -> {
                try {
                    service.setQueriedPaths(callingPackage, data);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
