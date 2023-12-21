#ifndef FILEUTILS_H
#define FILEUTILS_H

#include <dirent.h>
#include <jni.h>
#include <string>

namespace FileUtils {

    jint rm_dir(JNIEnv *env, jclass clazz, jstring jdir);

    bool starts_with(const std::string &parent, const std::string &child);

    jboolean auto_prepare_dirs(JNIEnv *env, jclass clazz, jobjectArray jdirs, jint juid);

    void switch_owner(JNIEnv *env, jclass clazz, jstring jdir, jint juid, jboolean isPrivate);
}

#endif // FILEUTILS_H
