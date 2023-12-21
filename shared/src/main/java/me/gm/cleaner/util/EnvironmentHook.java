package me.gm.cleaner.util;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.IBinder;
import android.os.storage.StorageVolume;
import android.util.ArrayMap;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

@SuppressWarnings("JavaReflectionMemberAccess")
@SuppressLint({"PrivateApi", "DiscouragedPrivateApi"})
public class EnvironmentHook {
    private File mTarget;

    private EnvironmentHook() {
    }

    public EnvironmentHook(File target) {
        mTarget = target;
    }

    public void hook() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // 通过 getService("mount"); 得到一个新的 IBinder 对象 rawBinder
                Class<?> serviceManager = Class.forName("android.os.ServiceManager");
                IBinder rawBinder = (IBinder) serviceManager
                        .getDeclaredMethod("getService", String.class)
                        .invoke(null, "mount");
                // 动态代理 rawBinder 中的 queryLocalInterface 方法，使这个方法返回替换后的 IStorageManager
                IBinder binder = (IBinder) Proxy.newProxyInstance(
                        getClass().getClassLoader(),
                        new Class<?>[]{IBinder.class},
                        new IBinderHandler(rawBinder)
                );
                // 用准备好的 IBinder 对象替换 ServiceManager 中缓存的 IBinder 对象
                Field field = serviceManager.getDeclaredField("sCache");
                field.setAccessible(true);
                ArrayMap<String, IBinder> map = (ArrayMap<String, IBinder>) field.get(null);
                map.put("mount", binder);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException |
                NoSuchFieldException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private class IBinderHandler implements InvocationHandler {
        private final IBinder mBase;
        private Class<?> mIin;

        public IBinderHandler(IBinder rawBinder) {
            mBase = rawBinder;
            try {
                mIin = Class.forName("android.os.storage.IStorageManager");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("queryLocalInterface".equals(method.getName())) {
                return Proxy.newProxyInstance(
                        mBase.getClass().getClassLoader(),
                        new Class<?>[]{mIin},
                        new IStorageManagerHandler(mBase)
                );
            }
            return method.invoke(mBase, args);
        }
    }

    private class IStorageManagerHandler implements InvocationHandler {
        private Object/* IStorageManager */ mBase;

        public IStorageManagerHandler(IBinder base) {
            try {
                mBase = Class.forName("android.os.storage.IStorageManager$Stub")
                        .getDeclaredMethod("asInterface", IBinder.class)
                        .invoke(null, base);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("getVolumeList".equals(method.getName())) {
                StorageVolume[] volumes = (StorageVolume[]) method.invoke(mBase, args);
                Field field = volumes[0].getClass().getDeclaredField("mPath");
                field.setAccessible(true);
                field.set(volumes[0], mTarget);
                return volumes;
            }
            return method.invoke(mBase, args);
        }
    }
}
