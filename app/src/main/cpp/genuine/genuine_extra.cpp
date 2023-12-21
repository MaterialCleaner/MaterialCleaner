#include "genuine_extra.h"
#include "obfuscate.h"
#include "../FileUtils.h"
#include "../Mount.h"
#include "../ProcInfo.h"

#ifdef DEBUG

#include "logging.h"

#endif

jint JNI_OnLoad_Extra(JNIEnv *env, jclass clazz) {
    if ((clazz = env->FindClass(AY_OBFUSCATE("me/gm/cleaner/util/FileUtils"))) == nullptr) {
        return JNI_ERR;
    }
    auto a = AY_OBFUSCATE("a");
    auto b = AY_OBFUSCATE("b");
    JNINativeMethod methods[] = {
            {b, AY_OBFUSCATE("(Ljava/lang/String;)I"),                     (void *) FileUtils::rm_dir},
            {a, AY_OBFUSCATE(
                        "([Ljava/lang/String;I)Z"),                        (void *) FileUtils::auto_prepare_dirs},
            {a, AY_OBFUSCATE(
                        "(Ljava/lang/String;IZ)V"),                        (void *) FileUtils::switch_owner},
//            {a, AY_OBFUSCATE(
//                        "(I)Ljava/lang/String;"),                        (void *) ProcInfo::read_cmdline},
            {b, AY_OBFUSCATE(
                        "(I)I"),                                           (void *) ProcInfo::read_uid},
            {a, AY_OBFUSCATE(
                        "(I[Ljava/lang/String;)[I"),                       (void *) ProcInfo::check_mounts},
            {a, AY_OBFUSCATE(
                        "(IIZZZ[Ljava/lang/String;[Ljava/lang/String;)Z"), (void *) Mount::bind_mount},
    };
    return env->RegisterNatives(clazz, methods, NELEM(methods));
}
