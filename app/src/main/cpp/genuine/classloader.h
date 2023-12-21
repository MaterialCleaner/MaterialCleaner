//
// Created by Thom on 2019/2/16.
//

#ifndef BREVENT_CLASSLOADER_H
#define BREVENT_CLASSLOADER_H

#include <jni.h>
#include "common.h"

#ifdef __cplusplus
extern "C" {
#endif

extern jmethodID methodObjectToString;
extern jmethodID methodClassGetClassLoader;
extern jmethodID methodClassGetName;
extern jmethodID methodClassLoaderLoadClass;
extern jmethodID methodClassIsArray;

bool checkClassLoader(JNIEnv *env, int sdk, int *genuine);

void logObject(JNIEnv *env, const char *format, jobject object);

#ifdef __cplusplus
}
#endif

#endif
