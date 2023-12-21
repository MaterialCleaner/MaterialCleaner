#include <dlfcn.h>
#include <libgen.h>
#include <shared_mutex>
#include <regex>
#include <sys/system_properties.h>

#include "bpf_hook.h"
#include "fuse_i.h"
#include "fuse_lowlevel.h"
#include "logging.h"
#include "obfuscate.h"

// Regex copied from FileUtils.java in MediaProvider, but without media directory.
const std::regex PATTERN_OWNED_PATH(
        "^/storage/[^/]+/(?:[0-9]+/)?Android/(?:data|obb)/([^/]+)(/?.*)?",
        std::regex_constants::icase);

static constexpr char PRIMARY_VOLUME_PREFIX[] = "/storage/emulated";

static bool isPackageOwnedPath(const std::string &path) {
    return std::regex_match(path, PATTERN_OWNED_PATH);
}

namespace bpf_hook {
    bool isFuseBpfEnabled = false;
    std::set<std::string> mountPoint = {};
    std::shared_mutex mountPointMutex;
    bool recordExternalAppSpecificStorage = false;

    bool (*old_StartsWith)(std::string_view s, std::string_view prefix);

    bool new_StartsWith(std::string_view s, std::string_view prefix) {
        if (!isFuseBpfEnabled && recordExternalAppSpecificStorage &&
            prefix == PRIMARY_VOLUME_PREFIX) {
            auto path = std::string(s);
            if (isPackageOwnedPath(path)) {
                return false;
            }
        }
        return old_StartsWith(s, prefix);
    }

    bool isMountPoint(const std::string &path) {
        if (path.starts_with(PRIMARY_VOLUME_PREFIX)) {
            std::shared_lock<std::shared_mutex> lock(mountPointMutex);
            return mountPoint.find(path) != mountPoint.end();
        }
        return false;
    }

    // hook stubs
    bool (*old_containsMount_31)(const std::string &path);

    bool new_containsMount_31(const std::string &path) {
        if (isMountPoint(path)) {
            return true;
        }
        return old_containsMount_31(path);
    }

    bool (*old_containsMount_30)(const std::string &path, const std::string &userid);

    bool new_containsMount_30(const std::string &path, const std::string &userid) {
        if (isMountPoint(path)) {
            return true;
        }
        return old_containsMount_30(path, userid);
    }

    bool (*old_IsFuseBpfEnabled)();

    bool new_IsFuseBpfEnabled() {
        isFuseBpfEnabled = old_IsFuseBpfEnabled();
        return isFuseBpfEnabled;
    }

    fuse_req_t fuse_req;

    void *(*old_fuse_req_userdata)(fuse_req_t req);

    void *new_fuse_req_userdata(fuse_req_t req) {
        fuse_req = req;
        return old_fuse_req_userdata(req);
    }

    void (*old_fuse_bpf_install)(struct fuse *fuse, struct fuse_entry_param *e,
                                 const std::string &child_path, int &backing_fd);

    void new_fuse_bpf_install(struct fuse *fuse, struct fuse_entry_param *e,
                              const std::string &child_path, int &backing_fd) {
        if (recordExternalAppSpecificStorage || fuse_req->ctx.uid == 0) {
            return;
        }
        return old_fuse_bpf_install(fuse, e, child_path, backing_fd);
    }

    static int GetApiLevel() {
        char prop[PROP_VALUE_MAX] = {0};
        __system_property_get("ro.build.version.sdk", prop);
        return atoi(prop);
    }

    static bool IsFuse() {
        char prop[PROP_VALUE_MAX] = {0};
        __system_property_get("persist.sys.fuse", prop);
        return strcmp(prop, "true") == 0;
    }

    void Hook(void *handle, HookFunType hook_func) {
        if (!IsFuse()) {
            return;
        }
        LOGE("%s", std::string(AY_OBFUSCATE("Initializing bpf_hook")).c_str());
        if (GetApiLevel() >= 31) {
            auto startsWith = dlsym(handle, AY_OBFUSCATE(
                    "_ZN7android4base10StartsWithENSt6__ndk117basic_string_viewIcNS1_11char_traitsIcEEEES5_"));
            if (startsWith != nullptr) {
                hook_func((void *) startsWith, (void *) new_StartsWith, (void **) &old_StartsWith);
            } else {
                LOGE("%s", std::string(AY_OBFUSCATE("failed to find StartsWith")).c_str());
            }
        }
        auto containsMount_31 = dlsym(handle, AY_OBFUSCATE(
                "_ZN13mediaprovider4fuse13containsMountERKNSt6__ndk112basic_stringIcNS1_11char_traitsIcEENS1_9allocatorIcEEEE"));
        if (containsMount_31 != nullptr) {
            hook_func((void *) containsMount_31, (void *) new_containsMount_31,
                      (void **) &old_containsMount_31);
        } else {
            auto containsMount_30 = dlsym(handle, AY_OBFUSCATE(
                    "_ZN13mediaprovider4fuse13containsMountERKNSt6__ndk112basic_stringIcNS1_11char_traitsIcEENS1_9allocatorIcEEEES9_"));
            if (containsMount_30 != nullptr) {
                hook_func((void *) containsMount_30, (void *) new_containsMount_30,
                          (void **) &old_containsMount_30);
            } else {
                LOGE("%s", std::string(AY_OBFUSCATE("failed to find containsMount")).c_str());
            }
        }
        auto IsFuseBpfEnabled = dlsym(handle, AY_OBFUSCATE(
                "_ZN13mediaprovider4fuse16IsFuseBpfEnabledEv"));
        if (IsFuseBpfEnabled != nullptr) {
            hook_func((void *) IsFuseBpfEnabled, (void *) new_IsFuseBpfEnabled,
                      (void **) &old_IsFuseBpfEnabled);

            return; // Deprecated
            auto fuse_req_userdata = dlsym(handle, AY_OBFUSCATE("fuse_req_userdata"));
            if (fuse_req_userdata != nullptr) {
                hook_func((void *) fuse_req_userdata, (void *) new_fuse_req_userdata,
                          (void **) &old_fuse_req_userdata);
            } else {
                LOGE("%s", std::string(AY_OBFUSCATE("failed to find fuse_req_userdata")).c_str());
            }

            auto fuse_bpf_install = dlsym(handle, AY_OBFUSCATE(
                    "_ZN13mediaprovider4fuse16fuse_bpf_installEP4fuseP16fuse_entry_paramRKNSt6__ndk112basic_stringIcNS5_11char_traitsIcEENS5_9allocatorIcEEEERi"));
            if (fuse_bpf_install != nullptr) {
                hook_func((void *) fuse_bpf_install, (void *) new_fuse_bpf_install,
                          (void **) &old_fuse_bpf_install);
            } else {
                LOGE("%s", std::string(AY_OBFUSCATE("failed to find fuse_bpf_install")).c_str());
            }
        } else {
            LOGE("%s", std::string(AY_OBFUSCATE("failed to find IsFuseBpfEnabled")).c_str());
        }
    }

    void setMountPoint(JNIEnv *env, jclass clazz, jobjectArray value) {
        std::unique_lock<std::shared_mutex> lock(mountPointMutex);
        mountPoint.clear();
        for (int i = 0, length = env->GetArrayLength(value); i < length; i++) {
            auto jpath = (jstring) env->GetObjectArrayElement(value, i);
            const char *path = env->GetStringUTFChars(jpath, nullptr);

            std::string parent = path;
            while (true) {
                if (!mountPoint.insert(parent).second) {
                    break;
                }
                parent = dirname(parent.c_str());
                if (parent == PRIMARY_VOLUME_PREFIX || parent == "/") {
                    break;
                }
            }

            env->ReleaseStringUTFChars(jpath, path);
        }
    }

    void setRecordExternalAppSpecificStorage(JNIEnv *env, jclass clazz, jboolean value) {
        recordExternalAppSpecificStorage = value;
    }
}  // namespace bpf_hook
