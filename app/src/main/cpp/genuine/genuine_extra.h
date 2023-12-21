#ifndef GENUINE_EXTRA_H
#define GENUINE_EXTRA_H

#include <jni.h>
#include <stdbool.h>

#ifndef NELEM
#define NELEM(x) (sizeof(x) / sizeof((x)[0]))
#endif

#ifdef __cplusplus
extern "C" {
#endif

jint JNI_OnLoad_Extra(JNIEnv *env, jclass clazz);

#ifdef __cplusplus
}
#endif

#endif //GENUINE_EXTRA_H
