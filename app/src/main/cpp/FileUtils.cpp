#include <libgen.h>
#include <sys/stat.h>
#include <sys/system_properties.h>
#include <unistd.h>
#include <vector>

#include "absl/strings/match.h"
#include "android-base/stringprintf.h"
#include "android_filesystem_config.h"
#include "obfs-string.h"
#include "FileUtils.h"

using android::base::StringPrintf;

namespace FileUtils {

    bool mkdirs(const char *path, mode_t mode) {
        struct stat stat{};
        if (lstat(path, &stat) == 0) {
            if (S_ISDIR(stat.st_mode)) {
                return true;
            }
            if (S_ISREG(stat.st_mode)) {
                return false;
            }
        }
        std::vector<std::string> stack = {path};
        for (;;) {
            std::string parent = dirname(stack.back().c_str());
            if (lstat(parent.c_str(), &stat) == 0) {
                if (S_ISREG(stat.st_mode)) {
                    return false;
                } else {
                    break;
                }
            }
            stack.emplace_back(parent);
        }
        std::reverse(stack.begin(), stack.end());
        for (auto &&item: stack) {
            if (!TEMP_FAILURE_RETRY(mkdir(item.c_str(), mode))) {
#ifdef DEBUG
                LOGI("mkdir: %s"_iobfs.c_str(), item.c_str());
#endif
            }
        }
        return true;
    }

    jint rm_dir(JNIEnv *env, jclass clazz, jstring jdir) {
        const char *dir = env->GetStringUTFChars(jdir, nullptr);
        const jint ret = rmdir(dir);
        env->ReleaseStringUTFChars(jdir, dir);
        return ret;
    }

    bool child_of(const std::string &parent, const std::string &child) {
        return absl::EndsWithIgnoreCase(parent, "/") && absl::StartsWithIgnoreCase(child, parent) ||
               !absl::EndsWithIgnoreCase(parent, "/") &&
               absl::StartsWithIgnoreCase(child, parent + '/');
    }

    bool starts_with(const std::string &parent, const std::string &child) {
        return absl::EqualsIgnoreCase(child, parent) || absl::EqualsIgnoreCase(parent, "/") ||
               absl::StartsWithIgnoreCase(child, parent + '/');
    }

    bool isFuse() {
        char prop[PROP_VALUE_MAX] = {0};
        __system_property_get("persist.sys.fuse"_iobfs.c_str(), prop);
        return std::string_view(prop) == "true"_iobfs.c_str();
    }

    bool prepare_dir(const char *path, mode_t mode, uid_t uid, gid_t gid) {
        if (!mkdirs(path, mode)) {
            return false;
        }
        if (TEMP_FAILURE_RETRY(chmod(path, mode)) == -1) {
#ifdef DEBUG
            LOGE("Failed to chmod(%s, %d): %s"_iobfs.c_str(), path, mode, strerror(errno));
#endif
            return false;
        }
        if (TEMP_FAILURE_RETRY(chown(path, uid, gid)) == -1) {
#ifdef DEBUG
            LOGE("Failed to chown(%s, %d, %d): %s"_iobfs.c_str(), path, uid, gid, strerror(errno));
#endif
            return false;
        }
        return true;
    }

    /// @see /system/vold/Utils.cpp PrepareAndroidDirs()
    jboolean auto_prepare_dirs(JNIEnv *env, jclass clazz, jobjectArray jdirs, jint juid) {
        const bool useSdcardFs = !isFuse();
        const uid_t user_id = juid / AID_USER_OFFSET;
        const std::string androidDataDir = StringPrintf(
                "/storage/emulated/%d/Android/data"_iobfs.c_str(), user_id);
        const std::string androidObbDir = StringPrintf(
                "/storage/emulated/%d/Android/obb"_iobfs.c_str(), user_id);
        const std::string userSource = StringPrintf("/mnt/user/%d"_iobfs.c_str(), user_id);
        const std::string storage = "/storage"_iobfs.c_str();
        // mode 0771 + sticky bit for inheriting GIDs
        const mode_t mode = S_IRWXU | S_IRWXG | S_IXOTH | S_ISGID;
        const gid_t dataGid = useSdcardFs ? AID_MEDIA_RW : AID_EXT_DATA_RW;
        const gid_t obbGid = useSdcardFs ? AID_MEDIA_RW : AID_EXT_OBB_RW;
        jboolean result = JNI_TRUE;
        for (int i = 0, length = env->GetArrayLength(jdirs); i < length; i++) {
            auto jdir = (jstring) env->GetObjectArrayElement(jdirs, i);
            const char *dir = env->GetStringUTFChars(jdir, nullptr);
            uid_t uid = AID_MEDIA_RW;
            gid_t gid = AID_MEDIA_RW;
            if (FileUtils::child_of(androidDataDir, dir)) {
                uid = juid;
                gid = dataGid;
            } else if (FileUtils::child_of(androidObbDir, dir)) {
                uid = juid;
                gid = obbGid;
            }
            result = result && prepare_dir(
                    useSdcardFs || user_id == 0 ? dir :
                    (userSource + std::string(dir).substr(storage.length(), strlen(dir))).c_str(),
                    mode, uid, gid
            );
            env->ReleaseStringUTFChars(jdir, dir);
        }
        return result;
    }

    void chownRecursively(const char *path, uid_t uid, gid_t gid) {
        DIR *pDir = opendir(path);
        if (pDir != nullptr) {
            struct dirent *file;
            while ((file = readdir(pDir)) != nullptr) {
                if (!strcmp(file->d_name, "."_iobfs.c_str()) ||
                    !strcmp(file->d_name, ".."_iobfs.c_str())) {
                    continue;
                }
                char child[PATH_MAX];
                sprintf(child, "%s/%s"_iobfs.c_str(), path, file->d_name);

                chownRecursively(child, uid, gid);
            }
            closedir(pDir);
        }
        TEMP_FAILURE_RETRY(chown(path, uid, gid));
    }

    // TODO: FixupAppDir
    void switch_owner(JNIEnv *env, jclass clazz, jstring jdir, jint juid, jboolean isPrivate) {
        const char *dir = env->GetStringUTFChars(jdir, nullptr);
        const uid_t uid = isPrivate ? juid : AID_MEDIA_RW;
        const gid_t gid = isPrivate ? AID_EXT_DATA_RW : AID_EVERYBODY;
        chownRecursively(dir, uid, gid);
        env->ReleaseStringUTFChars(jdir, dir);
    }
}  // namespace FileUtils
