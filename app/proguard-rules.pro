-repackageclasses "me.gm.cleaner"
-allowaccessmodification
-overloadaggressively

# field name and method name
-obfuscationdictionary dictionary-class.txt
# class name
-classobfuscationdictionary dictionary-method.txt

-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}
-keepclasseswithmembers,includedescriptorclasses class * {
    native <methods>;
}

-keepclassmembers class * extends me.gm.cleaner.home.scanner.BaseScanner {
    <init>(me.gm.cleaner.home.StaticScanner);
}

-keep class me.gm.cleaner.xposed.XposedInit

-keepclassmembers class hidden.ProcessObserverAdapter {
    <methods>;
}
-keepclassmembers class hidden.StorageEventListenerAdapter {
    <methods>;
}
-keepclassmembers class hidden.UidObserverAdapter {
    <methods>;
}

-keep class me.gm.cleaner.server.CleanerServerLoader {
    public static void main(java.lang.String[]);
    public static void entry(java.lang.String[]);
}

-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
	public static void check*(...);
	public static void throw*(...);
}

# Please add these rules to your existing keep rules in order to suppress warnings.
# This is generated automatically by the Android Gradle plugin.
-dontwarn android.app.ActivityManagerNative
-dontwarn android.app.ActivityThread$ApplicationThread
-dontwarn android.app.ActivityThread
-dontwarn android.app.AppOpsManager$OpEntry
-dontwarn android.app.AppOpsManager$PackageOps
-dontwarn android.app.ContentProviderHolder
-dontwarn android.app.ContextImpl
-dontwarn android.app.IActivityManager$Stub
-dontwarn android.app.IActivityManager
-dontwarn android.app.IApplicationThread
-dontwarn android.app.IProcessObserver$Stub
-dontwarn android.app.IProcessObserver
-dontwarn android.app.IUidObserver$Stub
-dontwarn android.app.IUidObserver
-dontwarn android.app.ProfilerInfo
-dontwarn android.content.IContentProvider
-dontwarn android.content.pm.ILauncherApps$Stub
-dontwarn android.content.pm.ILauncherApps
-dontwarn android.content.pm.IPackageManager$Stub
-dontwarn android.content.pm.IPackageManager
-dontwarn android.content.pm.ParceledListSlice
-dontwarn android.content.pm.UserInfo
-dontwarn android.ddm.DdmHandleAppName
-dontwarn android.os.IUserManager$Stub
-dontwarn android.os.IUserManager
-dontwarn android.os.ServiceManager
-dontwarn android.os.storage.IStorageManager$Stub
-dontwarn android.os.storage.IStorageManager
-dontwarn android.os.storage.IStorageEventListener$Stub
-dontwarn android.os.storage.IStorageEventListener
-dontwarn android.os.storage.VolumeInfo
-dontwarn android.os.storage.DiskInfo
-dontwarn android.os.storage.VolumeRecord
-dontwarn android.permission.IPermissionManager$Stub
-dontwarn android.permission.IPermissionManager
-dontwarn com.android.internal.app.IAppOpsService$Stub
-dontwarn com.android.internal.app.IAppOpsService
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE
