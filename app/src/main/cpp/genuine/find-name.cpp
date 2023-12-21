#include <cstring>

#include "common.h"
#include "find-name.h"
#include "obfs-string.h"

#define NATIVE 0x00000100
#define STATIC 0x00000008

static char *findField(JNIEnv *env, jobject clazz, int staticFlag, jclass type) {
    char *name = nullptr;
    jclass classClass = env->FindClass("java/lang/Class"_iobfs.c_str());
    jmethodID getDeclaredFields = env->GetMethodID(classClass, "getDeclaredFields"_iobfs.c_str(),
                                                   "()[Ljava/lang/reflect/Field;"_iobfs.c_str());
    jclass classField = env->FindClass("java/lang/reflect/Field"_iobfs.c_str());
    jmethodID getModifiers = env->GetMethodID(classField, "getModifiers"_iobfs.c_str(),
                                              "()I"_iobfs.c_str());
    jmethodID getType = env->GetMethodID(classField, "getType"_iobfs.c_str(),
                                         "()Ljava/lang/Class;"_iobfs.c_str());
    jmethodID getName = env->GetMethodID(classField, "getName"_iobfs.c_str(),
                                         "()Ljava/lang/String;"_iobfs.c_str());
    auto fields = (jobjectArray) env->CallObjectMethod(clazz, getDeclaredFields);
    int length = env->GetArrayLength(fields);
    for (int i = 0; i < length; ++i) {
        jobject field = env->GetObjectArrayElement(fields, i);
        debug(env, "field: %s", field);
        int modifier = env->CallIntMethod(field, getModifiers);
        if ((modifier & STATIC) == staticFlag
            && env->IsSameObject(type, env->CallObjectMethod(field, getType))) {
            auto fieldString = (jstring) env->CallObjectMethod(field, getName);
            const char *fieldName = env->GetStringUTFChars(fieldString, nullptr);
            name = strdup(fieldName);
            env->ReleaseStringUTFChars(fieldString, fieldName);
            env->DeleteLocalRef(fieldString);
        }
        env->DeleteLocalRef(field);
        if (name != nullptr) {
            break;
        }
    }

    env->DeleteLocalRef(fields);
    env->DeleteLocalRef(classField);
    env->DeleteLocalRef(classClass);
    return name;
}

char *findObjectArrayName(JNIEnv *env, jobject clazz) {
    jclass classObjectArray = env->FindClass("[Ljava/lang/Object;"_iobfs.c_str());
    char *name = findField(env, clazz, 0, classObjectArray);
    env->DeleteLocalRef(classObjectArray);
    return name;
}

char *findStaticMapName(JNIEnv *env, jobject clazz) {
    jclass classMap = env->FindClass("java/util/Map"_iobfs.c_str());
    char *name = findField(env, clazz, STATIC, classMap);
    env->DeleteLocalRef(classMap);
    return name;
}

char *findVoidStringName(JNIEnv *env, jclass clazz) {
    char *name = nullptr;
    jclass classClass = env->FindClass("java/lang/Class"_iobfs.c_str());
    jmethodID getDeclaredMethods = env->GetMethodID(classClass, "getDeclaredMethods"_iobfs.c_str(),
                                                    "()[Ljava/lang/reflect/Method;"_iobfs.c_str());
    jclass classMethod = env->FindClass("java/lang/reflect/Method"_iobfs.c_str());
    jmethodID getModifiers = env->GetMethodID(classMethod, "getModifiers"_iobfs.c_str(),
                                              "()I"_iobfs.c_str());
    jmethodID getParameterTypes = env->GetMethodID(classMethod, "getParameterTypes"_iobfs.c_str(),
                                                   "()[Ljava/lang/Class;"_iobfs.c_str());
    jmethodID getReturnType = env->GetMethodID(classMethod, "getReturnType"_iobfs.c_str(),
                                               "()Ljava/lang/Class;"_iobfs.c_str());
    jmethodID getName = env->GetMethodID(classMethod, "getName"_iobfs.c_str(),
                                         "()Ljava/lang/String;"_iobfs.c_str());
    jclass classString = env->FindClass("java/lang/String"_iobfs.c_str());
    auto methods = (jobjectArray) env->CallObjectMethod(clazz, getDeclaredMethods);
    int length = env->GetArrayLength(methods);
    for (int i = 0; i < length; ++i) {
        jobject method = env->GetObjectArrayElement(methods, i);
        debug(env, "method: %s", method);
        int modifier = env->CallIntMethod(method, getModifiers);
        if ((modifier & (NATIVE | STATIC)) == STATIC
            && env->GetArrayLength((jarray) env->CallObjectMethod(method, getParameterTypes)) == 0
            && env->IsSameObject(classString, env->CallObjectMethod(method, getReturnType))) {
            auto methodString = (jstring) env->CallObjectMethod(method, getName);
            const char *methodName = env->GetStringUTFChars(methodString, nullptr);
            jmethodID methodMethod = env->GetStaticMethodID(clazz, methodName,
                                                            "()Ljava/lang/String;"_iobfs.c_str());
            auto bridgeString = (jstring) env->CallStaticObjectMethod(clazz, methodMethod);
            if (env->ExceptionCheck()) {
                env->ExceptionClear();
            }
            if (bridgeString != nullptr) {
                const char *bridgeName = env->GetStringUTFChars(bridgeString, nullptr);
#ifdef DEBUG
                LOGI("bridgeName: %s", bridgeName);
#endif
                if (*bridgeName == 'L') {
                    name = strdup(bridgeName + 1);
                } else {
                    name = strdup(bridgeName);
                }
                env->ReleaseStringUTFChars(bridgeString, bridgeName);
                env->DeleteLocalRef(bridgeString);
            }
            env->ReleaseStringUTFChars(methodString, methodName);
            env->DeleteLocalRef(methodString);
        }
        env->DeleteLocalRef(method);
        if (name != nullptr) {
            char *x = name;
            while (*x != ';' && *x != '\0') {
                x++;
            }
            *x = '\0';
            break;
        }
    }

    env->DeleteLocalRef(methods);
    env->DeleteLocalRef(classString);
    env->DeleteLocalRef(classMethod);
    env->DeleteLocalRef(classClass);
    return name;
}
