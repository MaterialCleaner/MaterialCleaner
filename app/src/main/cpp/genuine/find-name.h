#pragma once

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

char *findObjectArrayName(JNIEnv *env, jobject clazz);

char *findStaticMapName(JNIEnv *env, jobject clazz);

char *findVoidStringName(JNIEnv *env, jclass clazz);

#ifdef __cplusplus
}
#endif
