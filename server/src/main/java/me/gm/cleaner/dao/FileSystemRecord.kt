package me.gm.cleaner.dao

import android.database.Cursor
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Entity
data class FileSystemRecord(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "time_millis") val timeMillis: Long,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "path") val path: String,
    @ColumnInfo(name = "flags") val flags: Int,
    @ColumnInfo(name = "is_app_specific_storage") val isAppSpecificStorage: Boolean,
) {
    companion object {
        fun create(
            timeMillis: Long, packageName: String, path: String, flags: Int,
            isAppSpecificStorage: Boolean
        ): FileSystemRecord = FileSystemRecord(
            0, timeMillis, packageName, path, flags, isAppSpecificStorage
        )
    }
}

@Dao
interface FileSystemRecordDao {
    @Query("SELECT * FROM FileSystemRecord WHERE is_app_specific_storage IN (:isHideAppSpecificStorageArg) AND package_name NOT IN (:denylist) ORDER BY time_millis DESC")
    fun queryAll(isHideAppSpecificStorageArg: IntArray, denylist: List<String>): Cursor

    @Query("SELECT * FROM FileSystemRecord WHERE is_app_specific_storage IN (:isHideAppSpecificStorageArg) AND package_name NOT IN (:denylist) AND LOWER(path) LIKE LOWER('%' || (:queryText) || '%') ORDER BY time_millis DESC")
    fun queryText(isHideAppSpecificStorageArg: IntArray, denylist: List<String>, queryText: String)
            : Cursor

    @Query("SELECT DISTINCT package_name, path, flags FROM FileSystemRecord WHERE is_app_specific_storage IS 0 AND package_name IN (:packageNames) ORDER BY time_millis DESC")
    fun queryInclude(vararg packageNames: String): Cursor

    @Query("SELECT count(*) FROM FileSystemRecord WHERE is_app_specific_storage IS 0 AND package_name IN (:packageNames)")
    fun countInclude(vararg packageNames: String): Int

    @Query("SELECT count(*) FROM FileSystemRecord")
    fun count(): Int

    @Query("SELECT DISTINCT package_name FROM FileSystemRecord")
    fun recordedPackages(): List<String>

    @Insert
    fun insert(record: FileSystemRecord)

    @Query("UPDATE OR IGNORE FileSystemRecord SET time_millis = (:timeMillis) WHERE package_name = (:packageName) AND path = (:path) AND flags = (:flags)")
    fun update(timeMillis: Long, packageName: String, path: String, flags: Int): Int

    @Transaction
    fun upsert(record: FileSystemRecord) {
        if (update(record.timeMillis, record.packageName, record.path, record.flags) == 0) {
            insert(record)
        }
    }

    @Delete
    fun delete(record: FileSystemRecord)

    @Query("DELETE FROM FileSystemRecord WHERE is_app_specific_storage = 1")
    fun deleteAppSpecificStorageRecords()

    // Keep the latest one.
    @Query("DELETE FROM FileSystemRecord WHERE id NOT IN (SELECT MAX(id) FROM FileSystemRecord GROUP BY package_name, path, flags)")
    fun distinct()

    @Query("DELETE FROM FileSystemRecord WHERE time_millis < (:timeMillis) AND package_name IN (:packageNames)")
    fun deleteTimeBefore(timeMillis: Long, vararg packageNames: String)

    @Query("DELETE FROM FileSystemRecord WHERE id IN (:ids)")
    fun deleteAll(ids: IntArray)
}

@Database(entities = [FileSystemRecord::class], version = 2, exportSchema = false)
abstract class FileSystemRecordDatabase : RoomDatabase() {
    abstract fun fileSystemRecordDao(): FileSystemRecordDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(_db: SupportSQLiteDatabase) {
        _db.execSQL("ALTER TABLE FileSystemRecord ADD COLUMN 'is_app_specific_storage' INTEGER NOT NULL DEFAULT 1")
    }
}
