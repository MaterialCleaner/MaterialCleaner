#include <cstring>
#include <dlfcn.h>
#include <jni.h>
#include "bpf_hook.h"
#include "logging.h"
#include "native_api.h"
#include "obfuscate.h"

static HookFunType hook_func = nullptr;

void on_library_loaded(const char *name, void *handle) {
    if (std::string(name).ends_with("/libfuse_jni.so")) {
        bpf_hook::Hook(handle, hook_func);
    }
}

extern "C" [[gnu::visibility("default")]] [[gnu::used]]
jint JNI_OnLoad(JavaVM *jvm, void *v __unused) {
    JNIEnv *env;
    if (jvm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }
    jclass clazz = env->FindClass(AY_OBFUSCATE("me/gm/cleaner/xposed/InlineHookConfig"));
    if (clazz == nullptr) {
        return JNI_ERR;
    }
    auto a = AY_OBFUSCATE("a");
    JNINativeMethod methods[] = {
            {a, AY_OBFUSCATE("([Ljava/lang/String;)V"), (void *) bpf_hook::setMountPoint},
            {a, AY_OBFUSCATE(
                        "(Z)V"),                        (void *) bpf_hook::setRecordExternalAppSpecificStorage},
    };
    if (env->RegisterNatives(clazz, methods, sizeof(methods) / sizeof(methods[0]))) {
        return JNI_ERR;
    }
    return JNI_VERSION_1_6;
}

extern "C" [[gnu::visibility("default")]] [[gnu::used]]
NativeOnModuleLoaded native_init(const NativeAPIEntries *entries) {
    hook_func = entries->hook_func;
    return on_library_loaded;
}
