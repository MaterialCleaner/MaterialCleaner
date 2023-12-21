#ifndef MOUNT_H
#define MOUNT_H

#define REMOUNT_STORAGE

namespace Mount {
    jboolean bind_mount(JNIEnv *env, jclass clazz, jint pid, jint uid,
                        jboolean enableRelatime, jboolean unmountDataRestriction,
                        jboolean fuseBypass, jobjectArray jsources, jobjectArray jtargets);
}

#endif // MOUNT_H
