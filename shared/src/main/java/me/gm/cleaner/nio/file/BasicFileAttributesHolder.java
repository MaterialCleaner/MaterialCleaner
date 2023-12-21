package me.gm.cleaner.nio.file;

import java.nio.file.attribute.BasicFileAttributes;

public interface BasicFileAttributesHolder {

    BasicFileAttributes get();

    void invalidate();
}
