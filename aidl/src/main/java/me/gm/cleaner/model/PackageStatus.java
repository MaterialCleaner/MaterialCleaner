package me.gm.cleaner.model;

import android.os.Parcel;
import android.os.Parcelable;

public class PackageStatus implements Parcelable {
    public int[] pids;
    public int[] pidFlags;
    public int[] userIds;

    public static final int GET_FROM_ALL_PROCESS = 0;
    public static final int GET_FROM_RECORDS = 1;

    public static final int PID_FLAG_MOUNTED = 1 << 0;
    public static final int PID_FLAG_STARTUP_AWARE = 1 << 1;
    public static final int PID_FLAG_DELETED = 1 << 2;
    public static final int PID_FLAG_OVERRIDE = 1 << 3;
    public static final int PID_FLAG_MKDIR_FAILED = 1 << 4;
    public static final int PID_FLAG_UNKNOWN = 1 << 5;
    public static final int PID_FLAG_MOUNT_FAILED = 1 << 6;

    public static final Creator<PackageStatus> CREATOR = new Creator<>() {
        @Override
        public PackageStatus createFromParcel(Parcel source) {
            return new PackageStatus(source);
        }

        @Override
        public PackageStatus[] newArray(int size) {
            return new PackageStatus[size];
        }
    };

    public PackageStatus() {
    }

    private PackageStatus(Parcel source) {
        pids = source.createIntArray();
        pidFlags = source.createIntArray();
        userIds = source.createIntArray();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeIntArray(pids);
        dest.writeIntArray(pidFlags);
        dest.writeIntArray(userIds);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
