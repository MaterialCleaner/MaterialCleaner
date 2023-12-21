package me.gm.cleaner.xposed;

import android.content.ContentProvider;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.ProviderInfo;
import android.os.RemoteException;
import android.provider.MediaStore;

import java.nio.file.Path;
import java.nio.file.Paths;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import me.gm.cleaner.client.CleanerHooksBinderRetriever;
import me.gm.cleaner.server.ICleanerHooksService;
import me.gm.cleaner.util.LibUtils;
import me.gm.cleaner.util.PathKt;

public class XposedInit implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    private final MediaProviderHooksService mediaProviderHooksService = new MediaProviderHooksService();
    private String modulePath;

    private void onMediaProviderLoaded(LoadPackageParam lpparam, Context context) {
        try {
            final var mediaProviderClass = XposedHelpers.findClass(
                    "com.android.providers.media.MediaProvider", lpparam.classLoader
            );
            var binder = CleanerHooksBinderRetriever.get();
            ICleanerHooksService service = ICleanerHooksService.Stub.asInterface(binder);
            service.setMediaProviderBinder(mediaProviderHooksService);
            new MediaProviderHook(mediaProviderHooksService, lpparam.classLoader, mediaProviderClass);
        } catch (XposedHelpers.ClassNotFoundError | RemoteException ignored) {
        }
    }

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) {
        if ((lpparam.appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
            // MediaProvider must be a system app.
            return;
        }
        try {
            System.loadLibrary("inline");
        } catch (UnsatisfiedLinkError e) {
            LibUtils.loadLibrary(
                    LibUtils.findLibSourceDir(
                            PathKt.listDirectoryEntriesSafe(Paths.get(modulePath).getParent())
                                    .stream()
                                    .map(Path::toString)
                                    .toArray(String[]::new)),
                    "inline");
        }
        XposedHelpers.findAndHookMethod(ContentProvider.class, "attachInfo",
                Context.class, ProviderInfo.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        final var context = (Context) param.args[0];
                        final var providerInfo = (ProviderInfo) param.args[1];

                        if (MediaStore.AUTHORITY.equals(providerInfo.authority)) {
                            onMediaProviderLoaded(lpparam, context);
                        }
                    }
                });
    }

    @Override
    public void initZygote(final IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {
        modulePath = startupParam.modulePath;
    }
}
