//
// Created by Thom on 2020/9/9.
//

#ifndef BREVENT_EPIC_FIELD_H
#define BREVENT_EPIC_FIELD_H

#include "common.h"
#include <jni.h>

char *findObjectArrayName(JNIEnv *env, jobject clazz);

char *findStaticMapName(JNIEnv *env, jobject clazz);

#endif //BREVENT_EPIC_FIELD_H
