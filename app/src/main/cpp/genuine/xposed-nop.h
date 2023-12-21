//
// Created by Thom on 2022/5/6.
//

#ifndef BREVENT_XPOSED_NOP_H
#define BREVENT_XPOSED_NOP_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

jobject xposedNop(JNIEnv *env, jclass xposedClassXcMethodHook);

#ifdef __cplusplus
}
#endif

#endif //BREVENT_XPOSED_NOP_H
