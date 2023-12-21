package me.gm.cleaner.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

public class FileModel implements Parcelable {
    public String path;
    public boolean isDirectory;
    public boolean isFile;

    public static final Creator<FileModel> CREATOR = new Creator<>() {
        @Override
        public FileModel createFromParcel(final Parcel source) {
            return new FileModel(source);
        }

        @Override
        public FileModel[] newArray(final int size) {
            return new FileModel[size];
        }
    };

    public FileModel(final Path path) {
        this.path = path.toString();
        try {
            final var attrs = Files.readAttributes(
                    path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            this.isDirectory = attrs.isDirectory();
            this.isFile = attrs.isRegularFile();
        } catch (final IOException e) {
            this.isDirectory = false;
            this.isFile = false;
        }
    }

    public FileModel(final String path, final boolean isDirectory, final boolean isFile) {
        this.path = path;
        this.isDirectory = isDirectory;
        this.isFile = isFile;
    }

    private FileModel(final Parcel source) {
        path = source.readString();
        isDirectory = source.readInt() != 0;
        isFile = source.readInt() != 0;
    }

    public boolean exists() {
        return isDirectory || isFile;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeString(path);
        dest.writeInt(isDirectory ? 1 : 0);
        dest.writeInt(isFile ? 1 : 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, isDirectory, isFile);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final FileModel that = (FileModel) o;
        return Objects.equals(path, that.path) &&
                isDirectory == that.isDirectory &&
                isFile == that.isFile;
    }
}
