#ifndef PROCINFO_H
#define PROCINFO_H

namespace ProcInfo {
    jstring read_cmdline(JNIEnv *env, jclass clazz, jint pid);

    jint read_uid(JNIEnv *env, jclass clazz, jint pid);

    jintArray check_mounts(JNIEnv *env, jclass clazz, jint pid, jobjectArray jtargets);
}

#endif // PROCINFO_H
