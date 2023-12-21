#include <jni.h>
#include <string>
#include <vector>

#include "external/abseil-cpp/absl/strings/escaping.h"
#include "external/abseil-cpp/absl/strings/str_split.h"
#include "android-base/stringprintf.h"
#include "linux_syscall_support.h"
#include "obfs-string.h"
#include "FileUtils.h"

using android::base::StringPrintf;

#ifdef DEBUG

#include "logging.h"

#endif

namespace ProcInfo {

    jstring read_cmdline(JNIEnv *env, jclass clazz, jint pid) {
        int fd = sys_open(StringPrintf("/proc/%d/cmdline"_iobfs.c_str(), pid).c_str(), O_RDONLY, 0);

        char nice_name[PATH_MAX];
        nice_name[sys_read(fd, nice_name, sizeof(nice_name) - 1)] = 0;
        sys_close(fd);
        return env->NewStringUTF(nice_name);
    }

    jint read_uid(JNIEnv *env, jclass clazz, jint pid) {
        int fd = sys_open(StringPrintf("/proc/%d/cgroup"_iobfs.c_str(), pid).c_str(), O_RDONLY, 0);
        if (fd < 0) {
            return -3;
        }
        FILE *fp = fdopen(fd, "r"_iobfs.c_str());
        if (fp == nullptr) {
            sys_close(fd);
            return -2;
        }

        int cuid = -1, cpid;
        char line[PATH_MAX];
        while (fgets(line, PATH_MAX - 1, fp) != nullptr) {
            if (sscanf(line, "%*[^/]/uid_%d/pid_%d"_iobfs.c_str(), &cuid, &cpid) == 2) {
                break;
            }
        }
        fclose(fp);
        sys_close(fd);
        return cuid;
    }

    jintArray check_mounts(JNIEnv *env, jclass clazz, jint pid, jobjectArray jtargets) {
        int length = env->GetArrayLength(jtargets);
        if (length == 0) {
            return env->NewIntArray(0);
        }
        std::vector<std::string> mountinfo = {};
        int fd = sys_open(StringPrintf("/proc/%d/mountinfo"_iobfs.c_str(), pid).c_str(), O_RDONLY,
                          0);
        if (fd < 0) {
            return nullptr;
        }
        FILE *fp = fdopen(fd, "r"_iobfs.c_str());
        if (fp == nullptr) {
            sys_close(fd);
            return nullptr;
        }
        struct IsTarget {
            std::string _target;

            explicit IsTarget(std::string target) : _target(std::move(target)) {}

            bool operator()(const std::string &mountinfoLine) const {
                std::vector<std::string> splitLine = absl::StrSplit(mountinfoLine, ' ');
                std::string targetFromMountinfo = splitLine.at(4);
                std::string unescaped;
                absl::CUnescape(targetFromMountinfo, &unescaped);
                return absl::EqualsIgnoreCase(unescaped, _target);
            }
        };
        auto jfirstTarget = (jstring) env->GetObjectArrayElement(jtargets, 0);
        const char *firstTarget = env->GetStringUTFChars(jfirstTarget, nullptr);
        auto isFirstTarget = IsTarget(firstTarget);
        bool isFirstTargetFound = false;
        char line[PATH_MAX];
        while (fgets(line, PATH_MAX - 1, fp) != nullptr) {
            if (!isFirstTargetFound && isFirstTarget(line)) {
                isFirstTargetFound = true;
            }
            if (isFirstTargetFound) {
                mountinfo.emplace_back(line);
            }
        }
        env->ReleaseStringUTFChars(jfirstTarget, firstTarget);
        fclose(fp);
        sys_close(fd);

        std::vector<int> mounted = {};
        if (!mountinfo.empty()) {
            struct IsDeleted {
                bool operator()(const std::string &mountinfoLine) const {
                    return mountinfoLine.find("//deleted"_iobfs.c_str()) != std::string::npos;
                }
            };
            struct IsOverride {
                std::string _target;

                explicit IsOverride(std::string target) : _target(std::move(target)) {}

                bool operator()(const std::string &mountinfoLine) const {
                    std::vector<std::string> splitLine = absl::StrSplit(mountinfoLine, ' ');
                    std::string targetFromMountinfo = splitLine.at(4);
                    std::string unescaped;
                    absl::CUnescape(targetFromMountinfo, &unescaped);
                    return FileUtils::starts_with(unescaped, _target);
                }
            };
            for (int i = 0; i < mountinfo.size(); i++) {
                std::string checkLine = mountinfo.at(i);
                if (mounted.size() < length && IsDeleted()(checkLine)) {
                    mounted.emplace_back(-1);
                } else if (i == 0) {
                    // skip first element because it's already validated
                    mounted.emplace_back(i);
                } else {
                    if (mounted.size() < length) {
                        auto jtarget = (jstring) env->GetObjectArrayElement(jtargets,
                                                                            (jsize) mounted.size());
                        const char *target = env->GetStringUTFChars(jtarget, nullptr);
                        if (IsTarget(target)(checkLine)) {
                            mounted.emplace_back(i);
                            env->ReleaseStringUTFChars(jtarget, target);
                            continue;
                        }
                        env->ReleaseStringUTFChars(jtarget, target);
                    }
                    // latter mount override former mount
                    for (int j = 0; j < mounted.size(); j++) {
                        auto jtarget = (jstring) env->GetObjectArrayElement(jtargets, j);
                        const char *target = env->GetStringUTFChars(jtarget, nullptr);
                        if (IsOverride(target)(checkLine)) {
                            mounted.back() = -2;
                            env->ReleaseStringUTFChars(jtarget, target);
                            break;
                        }
                        env->ReleaseStringUTFChars(jtarget, target);
                    }
                }
            }
        }
        jintArray jmounted = env->NewIntArray((jsize) mounted.size());
        env->SetIntArrayRegion(jmounted, 0, (jsize) mounted.size(), &mounted.front());
        return jmounted;
    }
}  // namespace ProcInfo
