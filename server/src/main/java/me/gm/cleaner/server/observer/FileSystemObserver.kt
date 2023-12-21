package me.gm.cleaner.server.observer

import android.annotation.SuppressLint
import android.database.Cursor
import androidx.annotation.IntDef
import androidx.room.Room
import api.SystemService
import me.gm.cleaner.dao.FileSystemRecord.Companion.create
import me.gm.cleaner.dao.FileSystemRecordDao
import me.gm.cleaner.dao.FileSystemRecordDatabase
import me.gm.cleaner.dao.MIGRATION_1_2
import me.gm.cleaner.dao.PurchaseVerification
import me.gm.cleaner.dao.ServicePreferences
import me.gm.cleaner.server.CleanerServer
import me.gm.cleaner.server.observer.PruneMethod.Companion.DELETE_ALL
import me.gm.cleaner.server.observer.PruneMethod.Companion.DELETE_APP_SPECIFIC
import me.gm.cleaner.server.observer.PruneMethod.Companion.DISTINCT
import me.gm.cleaner.server.observer.PruneMethod.Companion.QUERIED
import me.gm.cleaner.server.observer.PruneMethod.Companion.UNINSTALLED
import me.gm.cleaner.util.FileUtils
import java.io.File

class FileSystemObserver(private val server: CleanerServer) : BaseObserver(), ZygiskObserver {
    private val database: FileSystemRecordDatabase
    private val dao: FileSystemRecordDao

    /*  java.lang.SecurityException: Unable to find app for caller android.app.IApplicationThread$Stub$Proxy@4073f87 (pid=20560) when getting content provider settings
        at android.os.Parcel.createExceptionOrNull(Parcel.java:2425)
        at android.os.Parcel.createException(Parcel.java:2409)
        at android.os.Parcel.readException(Parcel.java:2392)
        at android.os.Parcel.readException(Parcel.java:2334)
        at android.app.IActivityManager$Stub$Proxy.getContentProvider(IActivityManager.java:6039)
        at android.app.ActivityThread.acquireProvider(ActivityThread.java:7204)
        at android.app.ContextImpl$ApplicationContentResolver.acquireProvider(ContextImpl.java:3343)
        at android.content.ContentResolver.acquireProvider(ContentResolver.java:2510)
        at android.provider.Settings$ContentProviderHolder.getProvider(Settings.java:2737)
        at android.provider.Settings$NameValueCache.getStringForUser(Settings.java:2910)
        at android.provider.Settings$Global.getStringForUser(Settings.java:15075)
        at android.provider.Settings$Global.getString(Settings.java:15063)
        at android.database.sqlite.SQLiteCompatibilityWalFlags.initIfNeeded(SQLiteCompatibilityWalFlags.java:105)
        at android.database.sqlite.SQLiteCompatibilityWalFlags.isLegacyCompatibilityWalEnabled(SQLiteCompatibilityWalFlags.java:57)
        at android.database.sqlite.SQLiteDatabase.<init>(SQLiteDatabase.java:321)
        at android.database.sqlite.SQLiteDatabase.openDatabase(SQLiteDatabase.java:762)
        at android.database.sqlite.SQLiteDatabase.openDatabase(SQLiteDatabase.java:752)
        at android.database.sqlite.SQLiteOpenHelper.getDatabaseLocked(SQLiteOpenHelper.java:373)
        at android.database.sqlite.SQLiteOpenHelper.getWritableDatabase(SQLiteOpenHelper.java:316)
        at androidx.sqlite.db.framework.FrameworkSQLiteOpenHelper$OpenHelper.getWritableSupportDatabase(FrameworkSQLiteOpenHelper.java:151)
        at androidx.sqlite.db.framework.FrameworkSQLiteOpenHelper.getWritableDatabase(FrameworkSQLiteOpenHelper.java:112)
        at androidx.room.RoomDatabase.inTransaction(RoomDatabase.java:706)
        at androidx.room.RoomDatabase.assertNotSuspendingTransaction(RoomDatabase.java:483)
        at me.gm.cleaner.dao.FileSystemRecordDao_Impl.insert(FileSystemRecordDao_Impl.java:66)
        at me.gm.cleaner.server.observer.FileSystemObserver$1.onEvent(FileSystemObserver.java:32)
        at me.gm.cleaner.server.IFileSystemObserver$Stub.onTransact(IFileSystemObserver.java:67)
        at android.os.Binder.execTransactInternal(Binder.java:1187)
        at android.os.Binder.execTransact(Binder.java:1146)
     */
    @SuppressLint("PrivateApi", "SoonBlockedPrivateApi")
    private fun fixSecurityException() {
        try {
            val cls = Class.forName("android.database.sqlite.SQLiteCompatibilityWalFlags")
            val field = cls.getDeclaredField("sInitialized")
            field.isAccessible = true
            field[null] = true
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
    }

    init {
        fixSecurityException()
        val dpsContext = server.createDeviceProtectedStorageContext()
        database = Room
            .databaseBuilder(
                dpsContext,
                FileSystemRecordDatabase::class.java,
                DATABASE_NAME
            )
            .addMigrations(MIGRATION_1_2)
            .build()
        dao = database.fileSystemRecordDao()
    }

    private fun isAppSpecificStorage(packageName: String, path: String): Boolean =
        FileUtils.startsWith(
            FileUtils.buildExternalStorageAppDataDirs(packageName),
            FileUtils.getPathAsUser(path, 0)
        )

    private fun onVerifiedEvent(
        timeMillis: Long, packageName: String, path: String, flags: Int
    ) {
        val isAppSpecificStorage = isAppSpecificStorage(packageName, path)
        recordEvent(timeMillis, packageName, path, flags, isAppSpecificStorage)
    }

    private fun recordEvent(
        timeMillis: Long, packageName: String, path: String, flags: Int,
        isAppSpecificStorage: Boolean
    ) {
        if (ServicePreferences.upsert) {
            dao.upsert(create(timeMillis, packageName, path, flags, isAppSpecificStorage))
        } else {
            dao.insert(create(timeMillis, packageName, path, flags, isAppSpecificStorage))
        }
        server.cleanerService.dispatchFileChange(
            timeMillis, packageName, path, flags, isAppSpecificStorage
        )
    }

    fun onEvent(timeMillis: Long, packageName: String, path: String, flags: Int) {
        if (ServicePreferences.recordSharedStorage) {
            if (FileUtils.childOf(FileUtils.externalStorageDirParent, path)) {
                onVerifiedEvent(timeMillis, packageName, path, flags)
            } else {
                runCatching {
                    val canonicalPath = File(path).canonicalPath
                    if (FileUtils.childOf(FileUtils.externalStorageDirParent, canonicalPath)
                    ) {
                        onVerifiedEvent(timeMillis, packageName, canonicalPath, flags)
                    }
                }
            }
        }
    }

    fun queryAllRecords(isHideAppSpecificStorage: Boolean, queryText: String?): Cursor {
        val isHideAppSpecificStorageArg = if (isHideAppSpecificStorage) {
            intArrayOf(0)
        } else {
            intArrayOf(0, 1)
        }
        return if (queryText.isNullOrBlank()) {
            dao.queryAll(isHideAppSpecificStorageArg, ServicePreferences.denylist)
        } else {
            dao.queryText(isHideAppSpecificStorageArg, ServicePreferences.denylist, queryText)
        }
    }

    fun queryDistinctRecordsInclude(vararg packageNames: String): Cursor =
        dao.queryInclude(*packageNames)

    fun countRecordsInclude(vararg packageNames: String): Int = dao.countInclude(*packageNames)

    fun databaseCount(): Int = dao.count()

    fun prune(
        method: Long, packageNames: Array<String>?,
        isHideAppSpecificStorage: Boolean, queryText: String?
    ) {
        if (method.toInt() != DELETE_ALL && !PurchaseVerification.isLoosePro) {
            return
        }
        when (method.toInt()) {
            DELETE_ALL -> database.clearAllTables()
            DELETE_APP_SPECIFIC -> dao.deleteAppSpecificStorageRecords()
            UNINSTALLED -> {
                val uninstalledApps = dao.recordedPackages().toSet() -
                        SystemService.getInstalledPackagesFromAllUsersNoThrow(0)
                            .map { it.packageName }.toSet() + ServicePreferences.denylist
                dao.deleteTimeBefore(System.currentTimeMillis(), *uninstalledApps.toTypedArray())
            }

            DISTINCT -> dao.distinct()
            QUERIED -> {
                val cursor = queryAllRecords(isHideAppSpecificStorage, queryText)
                val ids = ArrayList<Int>(cursor.count)
                while (cursor.moveToNext()) {
                    ids += cursor.getInt(0)
                }
                dao.deleteAll(ids.toIntArray())
            }
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        database.close()
    }

    companion object {
        const val DATABASE_NAME: String = "filesystem.db"
    }
}

@IntDef(value = [DELETE_ALL, DELETE_APP_SPECIFIC, UNINSTALLED, DISTINCT, QUERIED])
@Retention(AnnotationRetention.SOURCE)
annotation class PruneMethod {
    companion object {
        const val DELETE_ALL: Int = 0
        const val DELETE_APP_SPECIFIC: Int = -1
        const val UNINSTALLED: Int = -2
        const val DISTINCT: Int = -3
        const val QUERIED: Int = -4
    }
}
