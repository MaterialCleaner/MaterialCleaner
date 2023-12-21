//
// Created by Thom on 2020/9/9.
//

#include "epic-method.h"
#include <stdio.h>
#include <string.h>

#define NATIVE 0x00000100
#define STATIC 0x00000008

#ifdef CHECK_XPOSED_EPIC
static inline void fill_java_lang_Class(char v[]) {
    // java/lang/Class
    static unsigned int m = 0;

    if (m == 0) {
        m = 13;
    } else if (m == 17) {
        m = 19;
    }

    v[0x0] = 'h';
    v[0x1] = 'b';
    v[0x2] = 'r';
    v[0x3] = 'd';
    v[0x4] = ')';
    v[0x5] = 'k';
    v[0x6] = 'i';
    v[0x7] = 'g';
    v[0x8] = 'm';
    v[0x9] = '$';
    v[0xa] = 'O';
    v[0xb] = 'l';
    v[0xc] = '`';
    v[0xd] = 'q';
    v[0xe] = 'p';
    for (unsigned int i = 0; i < 0xf; ++i) {
        v[i] ^= ((i + 0xf) % m);
    }
    v[0xf] = '\0';
}

static inline void fill_getDeclaredMethods(char v[]) {
    // getDeclaredMethods
    static unsigned int m = 0;

    if (m == 0) {
        m = 17;
    } else if (m == 19) {
        m = 23;
    }

    v[0x0] = 'f';
    v[0x1] = 'g';
    v[0x2] = 'w';
    v[0x3] = '@';
    v[0x4] = '`';
    v[0x5] = 'e';
    v[0x6] = 'k';
    v[0x7] = 'i';
    v[0x8] = '{';
    v[0x9] = 'o';
    v[0xa] = 'o';
    v[0xb] = 'A';
    v[0xc] = 'h';
    v[0xd] = 'z';
    v[0xe] = 'g';
    v[0xf] = '\x7f';
    v[0x10] = 'd';
    v[0x11] = 'r';
    for (unsigned int i = 0; i < 0x12; ++i) {
        v[i] ^= ((i + 0x12) % m);
    }
    v[0x12] = '\0';
}

static inline void fill_getDeclaredMethods_signature(char v[]) {
    // ()[Ljava/lang/reflect/Method;
    static unsigned int m = 0;

    if (m == 0) {
        m = 23;
    } else if (m == 29) {
        m = 31;
    }

    v[0x0] = '.';
    v[0x1] = '.';
    v[0x2] = 'S';
    v[0x3] = 'E';
    v[0x4] = '`';
    v[0x5] = 'j';
    v[0x6] = 'z';
    v[0x7] = 'l';
    v[0x8] = '!';
    v[0x9] = 'c';
    v[0xa] = 'q';
    v[0xb] = '\x7f';
    v[0xc] = 'u';
    v[0xd] = '<';
    v[0xe] = 'f';
    v[0xf] = 'p';
    v[0x10] = 'p';
    v[0x11] = 'l';
    v[0x12] = 'd';
    v[0x13] = 'a';
    v[0x14] = 'w';
    v[0x15] = '+';
    v[0x16] = 'H';
    v[0x17] = 'c';
    v[0x18] = 's';
    v[0x19] = '`';
    v[0x1a] = 'f';
    v[0x1b] = 'n';
    v[0x1c] = '0';
    for (unsigned int i = 0; i < 0x1d; ++i) {
        v[i] ^= ((i + 0x1d) % m);
    }
    v[0x1d] = '\0';
}

static inline void fill_java_lang_reflect_Method(char v[]) {
    // java/lang/reflect/Method
    static unsigned int m = 0;

    if (m == 0) {
        m = 23;
    } else if (m == 29) {
        m = 31;
    }

    v[0x0] = 'k';
    v[0x1] = 'c';
    v[0x2] = 'u';
    v[0x3] = 'e';
    v[0x4] = '*';
    v[0x5] = 'j';
    v[0x6] = 'f';
    v[0x7] = 'f';
    v[0x8] = 'n';
    v[0x9] = '%';
    v[0xa] = 'y';
    v[0xb] = 'i';
    v[0xc] = 'k';
    v[0xd] = 'b';
    v[0xe] = 'j';
    v[0xf] = 's';
    v[0x10] = 'e';
    v[0x11] = '=';
    v[0x12] = '^';
    v[0x13] = 'q';
    v[0x14] = 'a';
    v[0x15] = '~';
    v[0x16] = 'o';
    v[0x17] = 'e';
    for (unsigned int i = 0; i < 0x18; ++i) {
        v[i] ^= ((i + 0x18) % m);
    }
    v[0x18] = '\0';
}

static inline void fill_getModifiers(char v[]) {
    // getModifiers
    static unsigned int m = 0;

    if (m == 0) {
        m = 11;
    } else if (m == 13) {
        m = 17;
    }

    v[0x0] = 'f';
    v[0x1] = 'g';
    v[0x2] = 'w';
    v[0x3] = 'I';
    v[0x4] = 'j';
    v[0x5] = 'b';
    v[0x6] = 'n';
    v[0x7] = 'n';
    v[0x8] = '`';
    v[0x9] = 'o';
    v[0xa] = 'r';
    v[0xb] = 'r';
    for (unsigned int i = 0; i < 0xc; ++i) {
        v[i] ^= ((i + 0xc) % m);
    }
    v[0xc] = '\0';
}

static inline void fill_getModifiers_signature(char v[]) {
    // ()I
    static unsigned int m = 0;

    if (m == 0) {
        m = 2;
    } else if (m == 3) {
        m = 5;
    }

    v[0x0] = ')';
    v[0x1] = ')';
    v[0x2] = 'H';
    for (unsigned int i = 0; i < 0x3; ++i) {
        v[i] ^= ((i + 0x3) % m);
    }
    v[0x3] = '\0';
}

static inline void fill_getParameterTypes(char v[]) {
    // getParameterTypes
    static unsigned int m = 0;

    if (m == 0) {
        m = 13;
    } else if (m == 17) {
        m = 19;
    }

    v[0x0] = 'c';
    v[0x1] = '`';
    v[0x2] = 'r';
    v[0x3] = 'W';
    v[0x4] = 'i';
    v[0x5] = '{';
    v[0x6] = 'k';
    v[0x7] = 'f';
    v[0x8] = 'i';
    v[0x9] = 't';
    v[0xa] = 'd';
    v[0xb] = 'p';
    v[0xc] = 'W';
    v[0xd] = '}';
    v[0xe] = 'u';
    v[0xf] = 'c';
    v[0x10] = 't';
    for (unsigned int i = 0; i < 0x11; ++i) {
        v[i] ^= ((i + 0x11) % m);
    }
    v[0x11] = '\0';
}

static inline void fill_getParameterTypes_signature(char v[]) {
    // ()[Ljava/lang/Class;
    static unsigned int m = 0;

    if (m == 0) {
        m = 19;
    } else if (m == 23) {
        m = 29;
    }

    v[0x0] = ')';
    v[0x1] = '+';
    v[0x2] = 'X';
    v[0x3] = 'H';
    v[0x4] = 'o';
    v[0x5] = 'g';
    v[0x6] = 'q';
    v[0x7] = 'i';
    v[0x8] = '&';
    v[0x9] = 'f';
    v[0xa] = 'j';
    v[0xb] = 'b';
    v[0xc] = 'j';
    v[0xd] = '!';
    v[0xe] = 'L';
    v[0xf] = '|';
    v[0x10] = 'p';
    v[0x11] = 'a';
    v[0x12] = 's';
    v[0x13] = ':';
    for (unsigned int i = 0; i < 0x14; ++i) {
        v[i] ^= ((i + 0x14) % m);
    }
    v[0x14] = '\0';
}

static inline void fill_getReturnType(char v[]) {
    // getReturnType
    static unsigned int m = 0;

    if (m == 0) {
        m = 11;
    } else if (m == 13) {
        m = 17;
    }

    v[0x0] = 'e';
    v[0x1] = 'f';
    v[0x2] = 'p';
    v[0x3] = 'W';
    v[0x4] = 'c';
    v[0x5] = 's';
    v[0x6] = '}';
    v[0x7] = '{';
    v[0x8] = 'd';
    v[0x9] = 'T';
    v[0xa] = 'x';
    v[0xb] = 'r';
    v[0xc] = 'f';
    for (unsigned int i = 0; i < 0xd; ++i) {
        v[i] ^= ((i + 0xd) % m);
    }
    v[0xd] = '\0';
}

static inline void fill_getReturnType_signature(char v[]) {
    // ()Ljava/lang/Class;
    static unsigned int m = 0;

    if (m == 0) {
        m = 17;
    } else if (m == 19) {
        m = 23;
    }

    v[0x0] = '*';
    v[0x1] = '*';
    v[0x2] = 'H';
    v[0x3] = 'o';
    v[0x4] = 'g';
    v[0x5] = 'q';
    v[0x6] = 'i';
    v[0x7] = '&';
    v[0x8] = 'f';
    v[0x9] = 'j';
    v[0xa] = 'b';
    v[0xb] = 'j';
    v[0xc] = '!';
    v[0xd] = 'L';
    v[0xe] = '|';
    v[0xf] = 'a';
    v[0x10] = 'r';
    v[0x11] = 'q';
    v[0x12] = '8';
    for (unsigned int i = 0; i < 0x13; ++i) {
        v[i] ^= ((i + 0x13) % m);
    }
    v[0x13] = '\0';
}

static inline void fill_getName(char v[]) {
    // getName
    static unsigned int m = 0;

    if (m == 0) {
        m = 5;
    } else if (m == 7) {
        m = 11;
    }

    v[0x0] = 'e';
    v[0x1] = 'f';
    v[0x2] = 'p';
    v[0x3] = 'N';
    v[0x4] = '`';
    v[0x5] = 'o';
    v[0x6] = 'f';
    for (unsigned int i = 0; i < 0x7; ++i) {
        v[i] ^= ((i + 0x7) % m);
    }
    v[0x7] = '\0';
}

static inline void fill_getName_signature(char v[]) {
    // ()Ljava/lang/String;
    static unsigned int m = 0;

    if (m == 0) {
        m = 19;
    } else if (m == 23) {
        m = 29;
    }

    v[0x0] = ')';
    v[0x1] = '+';
    v[0x2] = 'O';
    v[0x3] = 'n';
    v[0x4] = 'd';
    v[0x5] = 'p';
    v[0x6] = 'f';
    v[0x7] = '\'';
    v[0x8] = 'e';
    v[0x9] = 'k';
    v[0xa] = 'e';
    v[0xb] = 'k';
    v[0xc] = '"';
    v[0xd] = ']';
    v[0xe] = '{';
    v[0xf] = 'b';
    v[0x10] = 'x';
    v[0x11] = '|';
    v[0x12] = 'g';
    v[0x13] = ':';
    for (unsigned int i = 0; i < 0x14; ++i) {
        v[i] ^= ((i + 0x14) % m);
    }
    v[0x14] = '\0';
}

static inline void fill_java_lang_String(char v[]) {
    // java/lang/String
    static unsigned int m = 0;

    if (m == 0) {
        m = 13;
    } else if (m == 17) {
        m = 19;
    }

    v[0x0] = 'i';
    v[0x1] = 'e';
    v[0x2] = 's';
    v[0x3] = 'g';
    v[0x4] = '(';
    v[0x5] = 'd';
    v[0x6] = 'h';
    v[0x7] = 'd';
    v[0x8] = 'l';
    v[0x9] = '#';
    v[0xa] = 'S';
    v[0xb] = 'u';
    v[0xc] = 'p';
    v[0xd] = 'j';
    v[0xe] = 'j';
    v[0xf] = 'b';
    for (unsigned int i = 0; i < 0x10; ++i) {
        v[i] ^= ((i + 0x10) % m);
    }
    v[0x10] = '\0';
}

static inline void fill_signature(char v[]) {
    // ()Ljava/lang/String;
    static unsigned int m = 0;

    if (m == 0) {
        m = 19;
    } else if (m == 23) {
        m = 29;
    }

    v[0x0] = ')';
    v[0x1] = '+';
    v[0x2] = 'O';
    v[0x3] = 'n';
    v[0x4] = 'd';
    v[0x5] = 'p';
    v[0x6] = 'f';
    v[0x7] = '\'';
    v[0x8] = 'e';
    v[0x9] = 'k';
    v[0xa] = 'e';
    v[0xb] = 'k';
    v[0xc] = '"';
    v[0xd] = ']';
    v[0xe] = '{';
    v[0xf] = 'b';
    v[0x10] = 'x';
    v[0x11] = '|';
    v[0x12] = 'g';
    v[0x13] = ':';
    for (unsigned int i = 0; i < 0x14; ++i) {
        v[i] ^= ((i + 0x14) % m);
    }
    v[0x14] = '\0';
}

char *findVoidStringName(JNIEnv *env, jclass clazz) {
    char v1[0x20], v2[0x20];
    char signature[0x20];
    char *name = NULL;

    fill_java_lang_Class(v1);
    jclass classClass = (*env)->FindClass(env, v1);

    fill_getDeclaredMethods(v1);
    fill_getDeclaredMethods_signature(v2);
    jmethodID getDeclaredMethods = (*env)->GetMethodID(env, classClass, v1, v2);

    fill_java_lang_reflect_Method(v1);
    jclass classMethod = (*env)->FindClass(env, v1);

    fill_getModifiers(v1);
    fill_getModifiers_signature(v2);
    jmethodID getModifiers = (*env)->GetMethodID(env, classMethod, v1, v2);

    fill_getParameterTypes(v1);
    fill_getParameterTypes_signature(v2);
    jmethodID getParameterTypes = (*env)->GetMethodID(env, classMethod, v1, v2);

    fill_getReturnType(v1);
    fill_getReturnType_signature(v2);
    jmethodID getReturnType = (*env)->GetMethodID(env, classMethod, v1, v2);

    fill_getName(v1);
    fill_getName_signature(v2);
    jmethodID getName = (*env)->GetMethodID(env, classMethod, v1, v2);

    fill_java_lang_String(v1);
    jclass classString = (*env)->FindClass(env, v1);

    jobjectArray methods = (*env)->CallObjectMethod(env, clazz, getDeclaredMethods);
    int length = (*env)->GetArrayLength(env, methods);
    fill_signature(signature);
    for (int i = 0; i < length; ++i) {
        jobject method = (*env)->GetObjectArrayElement(env, methods, i);
        debug(env, "method: %s", method);
        int modifier = (*env)->CallIntMethod(env, method, getModifiers);
        if ((modifier & (NATIVE | STATIC)) == STATIC
            && (*env)->GetArrayLength(env, (*env)->CallObjectMethod(env, method, getParameterTypes)) == 0
            && (*env)->IsSameObject(env, classString, (*env)->CallObjectMethod(env, method, getReturnType))) {
            jstring methodString = (*env)->CallObjectMethod(env, method, getName);
            const char *methodName = (*env)->GetStringUTFChars(env, methodString, NULL);
            jmethodID methodMethod = (*env)->GetStaticMethodID(env, clazz, methodName, signature);
            jstring bridgeString = (*env)->CallStaticObjectMethod(env, clazz, methodMethod);
            if ((*env)->ExceptionCheck(env)) {
                (*env)->ExceptionClear(env);
            }
            if (bridgeString != NULL) {
                const char *bridgeName = (*env)->GetStringUTFChars(env, bridgeString, NULL);
#ifdef DEBUG
                LOGI("bridgeName: %s", bridgeName);
#endif
                if (*bridgeName == 'L') {
                    name = strdup(bridgeName + 1);
                } else {
                    name = strdup(bridgeName);
                }
                (*env)->ReleaseStringUTFChars(env, bridgeString, bridgeName);
                (*env)->DeleteLocalRef(env, bridgeString);
            }
            (*env)->ReleaseStringUTFChars(env, methodString, methodName);
            (*env)->DeleteLocalRef(env, methodString);
        }
        (*env)->DeleteLocalRef(env, method);
        if (name != NULL) {
            char *x = name;
            while (*x != ';' && *x != '\0') {
                x++;
            }
            *x = '\0';
            break;
        }
    }

    (*env)->DeleteLocalRef(env, methods);
    (*env)->DeleteLocalRef(env, classString);
    (*env)->DeleteLocalRef(env, classMethod);
    (*env)->DeleteLocalRef(env, classClass);
    return name;
}
#endif
