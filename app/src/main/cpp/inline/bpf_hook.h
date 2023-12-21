#pragma once

#include <jni.h>
#include <set>
#include <string>
#include "native_api.h"

namespace bpf_hook {

    extern std::set<std::string> mountPoint;

    extern bool recordExternalAppSpecificStorage;

    void Hook(void *handle, HookFunType hook_func);

    void setMountPoint(JNIEnv *env, jclass clazz, jobjectArray value);

    void setRecordExternalAppSpecificStorage(JNIEnv *env, jclass clazz, jboolean value);
}  // namespace bpf_hook
