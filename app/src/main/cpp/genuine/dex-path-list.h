//
// Created by Thom on 2022/5/3.
//

#ifndef BREVENT_DEX_PATH_LIST_H
#define BREVENT_DEX_PATH_LIST_H

#include <stdbool.h>
#include "jni.h"

#ifdef __cplusplus
extern "C" {
#endif

bool hasAbnormalClassLoader(JNIEnv *env, jclass baseDexClassLoader, jobject object, bool *hasInMemoryDex);

#ifdef __cplusplus
}
#endif

#endif //BREVENT_DEX_PATH_LIST_H
