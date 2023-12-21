#ifndef EPIC_H
#define EPIC_H

#include <jni.h>
#include <stdbool.h>
#include "common.h"

#ifdef __cplusplus
extern "C" {
#endif

#ifdef CHECK_XPOSED_EPIC

bool doAntiEpic(JNIEnv *env, jobject classLoader);

#endif

void clearHandler(JNIEnv *env, int sdk);

#ifdef __cplusplus
}
#endif

#endif