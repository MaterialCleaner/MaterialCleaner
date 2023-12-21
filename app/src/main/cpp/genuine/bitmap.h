//
// Created by Thom on 2019/3/22.
//

#ifndef BREVENT_BITMAP_H
#define BREVENT_BITMAP_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

jobject asBitmap(JNIEnv *env, int width, jstring label);

#ifdef __cplusplus
}
#endif

#endif //BREVENT_BITMAP_H
