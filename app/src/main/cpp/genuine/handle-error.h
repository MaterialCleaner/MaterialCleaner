//
// Created by Thom on 2019/3/20.
//

#ifndef BREVENT_HANDLE_ERROR_H
#define BREVENT_HANDLE_ERROR_H

#include <stdbool.h>
#include <jni.h>

void start_native_activity(JavaVM *jvm);

void start_native_activity_async(JNIEnv *env);

void set_started();

#endif //BREVENT_HANDLE_ERROR_H
