//
// Created by Thom on 2022/5/6.
//

#include <zlib.h>
#include <string.h>
#include <stdlib.h>
#include <time.h>
#include "xposed-nop.h"
#include "classloader.h"
#include "common.h"

#ifdef DEBUG_XPOSED
#define debugObject logObject
#else
#define debugObject(...) do {} while(0)
#endif

static inline void fill_dalvik_system_InMemoryDexClassLoader(char v[]) {
    // dalvik/system/InMemoryDexClassLoader
    static unsigned int m = 0;

    if (m == 0) {
        m = 31;
    } else if (m == 37) {
        m = 41;
    }

    v[0x0] = 'a';
    v[0x1] = 'g';
    v[0x2] = 'k';
    v[0x3] = '~';
    v[0x4] = '`';
    v[0x5] = 'a';
    v[0x6] = '$';
    v[0x7] = '\x7f';
    v[0x8] = 't';
    v[0x9] = '}';
    v[0xa] = '{';
    v[0xb] = 'u';
    v[0xc] = '|';
    v[0xd] = '=';
    v[0xe] = 'Z';
    v[0xf] = 'z';
    v[0x10] = 'X';
    v[0x11] = 's';
    v[0x12] = 'z';
    v[0x13] = 'w';
    v[0x14] = 'k';
    v[0x15] = 'c';
    v[0x16] = '_';
    v[0x17] = 'y';
    v[0x18] = 'e';
    v[0x19] = ']';
    v[0x1a] = 'l';
    v[0x1b] = '`';
    v[0x1c] = 'q';
    v[0x1d] = 'p';
    v[0x1e] = 'H';
    v[0x1f] = 'j';
    v[0x20] = 'g';
    v[0x21] = 'c';
    v[0x22] = 'm';
    v[0x23] = '{';
    for (unsigned int i = 0; i < 0x24; ++i) {
        v[i] ^= ((i + 0x24) % m);
    }
    v[0x24] = '\0';
}

static inline void fill_InMemoryDexClassLoader_init(char v[]) {
    // <init>
    static unsigned int m = 0;

    if (m == 0) {
        m = 5;
    } else if (m == 7) {
        m = 11;
    }

    v[0x0] = '=';
    v[0x1] = 'k';
    v[0x2] = 'm';
    v[0x3] = 'm';
    v[0x4] = 't';
    v[0x5] = '?';
    for (unsigned int i = 0; i < 0x6; ++i) {
        v[i] ^= ((i + 0x6) % m);
    }
    v[0x6] = '\0';
}

static inline void fill_InMemoryDexClassLoader_init_signature(char v[]) {
    // (Ljava/nio/ByteBuffer;Ljava/lang/ClassLoader;)V
    static unsigned int m = 0;

    if (m == 0) {
        m = 43;
    } else if (m == 47) {
        m = 53;
    }

    v[0x0] = ',';
    v[0x1] = 'I';
    v[0x2] = 'l';
    v[0x3] = 'f';
    v[0x4] = '~';
    v[0x5] = 'h';
    v[0x6] = '%';
    v[0x7] = 'e';
    v[0x8] = 'e';
    v[0x9] = 'b';
    v[0xa] = '!';
    v[0xb] = 'M';
    v[0xc] = 'i';
    v[0xd] = 'e';
    v[0xe] = 'w';
    v[0xf] = 'Q';
    v[0x10] = 'a';
    v[0x11] = 's';
    v[0x12] = 'p';
    v[0x13] = 'r';
    v[0x14] = 'j';
    v[0x15] = '"';
    v[0x16] = 'V';
    v[0x17] = 'q';
    v[0x18] = '}';
    v[0x19] = 'k';
    v[0x1a] = '\x7f';
    v[0x1b] = '0';
    v[0x1c] = 'L';
    v[0x1d] = '@';
    v[0x1e] = 'L';
    v[0x1f] = 'D';
    v[0x20] = '\x0b';
    v[0x21] = 'f';
    v[0x22] = 'J';
    v[0x23] = 'F';
    v[0x24] = '[';
    v[0x25] = 'Z';
    v[0x26] = 'f';
    v[0x27] = 'o';
    v[0x28] = '`';
    v[0x29] = 'f';
    v[0x2a] = 'f';
    v[0x2b] = 'v';
    v[0x2c] = '>';
    v[0x2d] = '/';
    v[0x2e] = 'Q';
    for (unsigned int i = 0; i < 0x2f; ++i) {
        v[i] ^= ((i + 0x2f) % m);
    }
    v[0x2f] = '\0';
}

static inline void fill_NoHook(char v[]) {
    // $$Hook
    static unsigned int m = 0;

    if (m == 0) {
        m = 5;
    } else if (m == 7) {
        m = 11;
    }

    v[0x0] = '%';
    v[0x1] = '&';
    v[0x2] = 'K';
    v[0x3] = 'k';
    v[0x4] = 'o';
    v[0x5] = 'j';
    for (unsigned int i = 0; i < 0x6; ++i) {
        v[i] ^= ((i + 0x6) % m);
    }
    v[0x6] = '\0';
}

static inline void fill_hookMethod_template_signature(char v[]) {
    // (Ljava/lang/reflect/Member;L%s;)L%s$Unhook;
    static unsigned int m = 0;

    if (m == 0) {
        m = 41;
    } else if (m == 43) {
        m = 47;
    }

    v[0x0] = '*';
    v[0x1] = 'O';
    v[0x2] = 'n';
    v[0x3] = 'd';
    v[0x4] = 'p';
    v[0x5] = 'f';
    v[0x6] = '\'';
    v[0x7] = 'e';
    v[0x8] = 'k';
    v[0x9] = 'e';
    v[0xa] = 'k';
    v[0xb] = '"';
    v[0xc] = '|';
    v[0xd] = 'j';
    v[0xe] = 'v';
    v[0xf] = '}';
    v[0x10] = 'w';
    v[0x11] = 'p';
    v[0x12] = '`';
    v[0x13] = ':';
    v[0x14] = '[';
    v[0x15] = 'r';
    v[0x16] = 'u';
    v[0x17] = '{';
    v[0x18] = '\x7f';
    v[0x19] = 'i';
    v[0x1a] = '\'';
    v[0x1b] = 'Q';
    v[0x1c] = ';';
    v[0x1d] = 'l';
    v[0x1e] = '\x1b';
    v[0x1f] = '\x08';
    v[0x20] = 'n';
    v[0x21] = '\x06';
    v[0x22] = 'W';
    v[0x23] = '\x01';
    v[0x24] = 's';
    v[0x25] = 'I';
    v[0x26] = '@';
    v[0x27] = 'o';
    v[0x28] = 'n';
    v[0x29] = 'i';
    v[0x2a] = '8';
    for (unsigned int i = 0; i < 0x2b; ++i) {
        v[i] ^= ((i + 0x2b) % m);
    }
    v[0x2b] = '\0';
}

static inline void fill_xposedBridge_template(char v[]) {
    // %s/XposedBridge
    static unsigned int m = 0;

    if (m == 0) {
        m = 13;
    } else if (m == 17) {
        m = 19;
    }

    v[0x0] = '\'';
    v[0x1] = 'p';
    v[0x2] = '+';
    v[0x3] = ']';
    v[0x4] = 'v';
    v[0x5] = 'h';
    v[0x6] = '{';
    v[0x7] = 'l';
    v[0x8] = 'n';
    v[0x9] = 'I';
    v[0xa] = '~';
    v[0xb] = 'i';
    v[0xc] = 'e';
    v[0xd] = 'e';
    v[0xe] = 'f';
    for (unsigned int i = 0; i < 0xf; ++i) {
        v[i] ^= ((i + 0xf) % m);
    }
    v[0xf] = '\0';
}

static inline void fill_hookMethod(char v[]) {
    // hookMethod
    static unsigned int m = 0;

    if (m == 0) {
        m = 7;
    } else if (m == 11) {
        m = 13;
    }

    v[0x0] = 'k';
    v[0x1] = 'k';
    v[0x2] = 'j';
    v[0x3] = 'm';
    v[0x4] = 'M';
    v[0x5] = 'd';
    v[0x6] = 'v';
    v[0x7] = 'k';
    v[0x8] = 'k';
    v[0x9] = 'a';
    for (unsigned int i = 0; i < 0xa; ++i) {
        v[i] ^= ((i + 0xa) % m);
    }
    v[0xa] = '\0';
}

jobject xposedNop(JNIEnv *env, jclass xposedClassXcMethodHook) {
    jobject noHook = nullptr;
    if (getSdk() < __ANDROID_API_O__) {
        return noHook;
    }
#ifdef DEBUG_XPOSED
        LOGI("methodClassGetName: %p", methodClassGetName);
#endif
    debugObject(env, "xposedClassXcMethodHook: %s", xposedClassXcMethodHook);
    auto xposedClassNameAsString = (jstring) env->CallObjectMethod(xposedClassXcMethodHook, methodClassGetName);
    debugObject(env, "xposedClassNameAsString: %s", xposedClassNameAsString);
    const char *xposedClassNameAsChars = env->GetStringUTFChars(xposedClassNameAsString, nullptr);
    char *xposedClassName = strdup(xposedClassNameAsChars);
#ifdef DEBUG_XPOSED
    LOGI("xposedClassName: %s", xposedClassName);
#endif
    env->ReleaseStringUTFChars(xposedClassNameAsString, xposedClassNameAsChars);
    env->DeleteLocalRef(xposedClassNameAsString);
    uint8_t bytes[] = {
            0x7b, 0x7a, 0x67, 0x15, 0x2f, 0x2c, 0x2a, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f,
            0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f,
            0xbb, 0x1e, 0x1f, 0x1f, 0x6f, 0x1f, 0x1f, 0x1f, 0x67, 0x49, 0x2b, 0x0d, 0x1f, 0x1f, 0x1f, 0x1f,
            0x1f, 0x1f, 0x1f, 0x1f, 0x37, 0x1e, 0x1f, 0x1f, 0x1b, 0x1f, 0x1f, 0x1f, 0x6f, 0x1f, 0x1f, 0x1f,
            0x1c, 0x1f, 0x1f, 0x1f, 0x9f, 0x1f, 0x1f, 0x1f, 0x1e, 0x1f, 0x1f, 0x1f, 0x93, 0x1f, 0x1f, 0x1f,
            0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1d, 0x1f, 0x1f, 0x1f, 0x87, 0x1f, 0x1f, 0x1f,
            0x1e, 0x1f, 0x1f, 0x1f, 0xb7, 0x1f, 0x1f, 0x1f, 0xc3, 0x1f, 0x1f, 0x1f, 0xd7, 0x1f, 0x1f, 0x1f,
            0xff, 0x1f, 0x1f, 0x1f, 0xf7, 0x1f, 0x1f, 0x1f, 0xed, 0x1f, 0x1f, 0x1f, 0x05, 0x1e, 0x1f, 0x1f,
            0x1e, 0x1f, 0x1f, 0x1f, 0x1d, 0x1f, 0x1f, 0x1f, 0x1c, 0x1f, 0x1f, 0x1f, 0x1c, 0x1f, 0x1f, 0x1f,
            0x1d, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f,
            0x1e, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1e, 0x1f, 0x1f, 0x1f,
            0x1e, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0xe0, 0xe0, 0xe0, 0xe0, 0x1f, 0x1f, 0x1f, 0x1f,
            0x02, 0x1e, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1e, 0x1f, 0x1e, 0x1f, 0x1e, 0x1f, 0x1f, 0x1f,
            0x1f, 0x1f, 0x1f, 0x1f, 0x1b, 0x1f, 0x1f, 0x1f, 0x6f, 0x0f, 0x1e, 0x1f, 0x1f, 0x1f, 0x11, 0x1f,
            0x19, 0x23, 0x76, 0x71, 0x76, 0x6b, 0x21, 0x1f, 0x17, 0x53, 0x51, 0x70, 0x57, 0x70, 0x70, 0x74,
            0x24, 0x1f, 0x39, 0x53, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f,
            0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f,
            0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x24, 0x1f, 0x1e, 0x49, 0x1f, 0x1f, 0x1f, 0x1e,
            0x1f, 0x1f, 0x9e, 0x9f, 0x1b, 0xd7, 0x1e, 0x1f, 0x15, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f,
            0x1e, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1e, 0x1f, 0x1f, 0x1f, 0x1b, 0x1f, 0x1f, 0x1f,
            0x6f, 0x1f, 0x1f, 0x1f, 0x1d, 0x1f, 0x1f, 0x1f, 0x1c, 0x1f, 0x1f, 0x1f, 0x9f, 0x1f, 0x1f, 0x1f,
            0x1c, 0x1f, 0x1f, 0x1f, 0x1e, 0x1f, 0x1f, 0x1f, 0x93, 0x1f, 0x1f, 0x1f, 0x1a, 0x1f, 0x1f, 0x1f,
            0x1d, 0x1f, 0x1f, 0x1f, 0x87, 0x1f, 0x1f, 0x1f, 0x19, 0x1f, 0x1f, 0x1f, 0x1e, 0x1f, 0x1f, 0x1f,
            0xb7, 0x1f, 0x1f, 0x1f, 0x1e, 0x3f, 0x1f, 0x1f, 0x1e, 0x1f, 0x1f, 0x1f, 0xd7, 0x1f, 0x1f, 0x1f,
            0x1d, 0x3f, 0x1f, 0x1f, 0x1b, 0x1f, 0x1f, 0x1f, 0xff, 0x1f, 0x1f, 0x1f, 0x1f, 0x3f, 0x1f, 0x1f,
            0x1e, 0x1f, 0x1f, 0x1f, 0x02, 0x1e, 0x1f, 0x1f, 0x1f, 0x0f, 0x1f, 0x1f, 0x1e, 0x1f, 0x1f, 0x1f,
            0x37, 0x1e, 0x1f, 0x1f
    };
    for (size_t i = 0; i < sizeof(bytes); ++i) {
        bytes[i] ^= 0x1f;
    }
    char *x = xposedClassName;
    while (*x) {
        if (*x == '.') {
            *x = '/';
        }
        ++x;
    }
    if (x - xposedClassName != 36) {
        goto clean;
    }
    {
#ifdef DEBUG_XPOSED
        LOGI("xposedClassName with /: %s, dex length: %lu", xposedClassName, sizeof(bytes));
#endif
        char v[0x40], v2[0x40];
        fill_NoHook(v);
        memcpy(bytes + 0xea, v, 0x6);
        jstring classNoHookAsString = env->NewStringUTF(v);
        memcpy(bytes + 0xf4, xposedClassName, x - xposedClassName);
        uint32_t adler_checksum;
        adler_checksum = adler32(0L, Z_NULL, 0);
        adler_checksum = adler32(adler_checksum, bytes + 32, sizeof(bytes) - 32);
        memcpy(bytes + 28, &adler_checksum, 0x4);
        struct timespec tv;
        clock_gettime(CLOCK_REALTIME, &tv);
        memcpy(bytes + 12, &tv, sizeof(tv) > 20 ? 20 : sizeof(tv));
        adler_checksum = adler32(0L, Z_NULL, 0);
        adler_checksum = adler32(adler_checksum, bytes + 12, sizeof(bytes) - 12);
        memcpy(bytes + 8, &adler_checksum, 0x4);
        auto byteBuffer = env->NewDirectByteBuffer(bytes, sizeof(bytes));
        debugObject(env, "byteBuffer: %s", byteBuffer);
        auto classLoader = env->CallObjectMethod(xposedClassXcMethodHook, methodClassGetClassLoader);
        debugObject(env, "classLoader: %s", classLoader);
        fill_dalvik_system_InMemoryDexClassLoader(v);
        jclass classInMemoryDexClassLoader = env->FindClass(v);
        debugObject(env, "InMemoryDexClassLoader: %s", classInMemoryDexClassLoader);
        fill_InMemoryDexClassLoader_init(v);
        fill_InMemoryDexClassLoader_init_signature(v2);
        jmethodID methodInMemoryDexClassLoaderInit = env->GetMethodID(classInMemoryDexClassLoader, v, v2);
#ifdef DEBUG_XPOSED
        LOGI("methodInMemoryDexClassLoaderInit: %p", methodInMemoryDexClassLoaderInit);
#endif
        jobject classNoHookClassLoader = env->NewObject(classInMemoryDexClassLoader, methodInMemoryDexClassLoaderInit, byteBuffer, classLoader);
        debugObject(env, "classNoHookClassLoader: %s", classNoHookClassLoader);
        debugObject(env, "classNoHookAsString: %s", classNoHookAsString);
        jclass classNoHook = (jclass) env->CallObjectMethod(classNoHookClassLoader, methodClassLoaderLoadClass, classNoHookAsString);
        debugObject(env, "classNoHook: %s", classNoHook);
        noHook = env->AllocObject(classNoHook);
        debugObject(env, "noHook: %s", noHook);
        if (methodNop != nullptr) {
            char signature[0x80];
            fill_hookMethod_template_signature(v);
            sprintf(signature, v, xposedClassName, xposedClassName);
            char *lastSlash = strrchr(xposedClassName, '/');
            *lastSlash = '\0';
            char name[0x40];
            fill_xposedBridge_template(v);
            sprintf(name, v, xposedClassName);
#ifdef DEBUG_XPOSED
            LOGI("name: %s, signature: %s", name, signature);
#endif
            jstring classXposedBridgeAsString = env->NewStringUTF(name);
            jclass classXposedBridge = (jclass) env->CallObjectMethod(classLoader, methodClassLoaderLoadClass, classXposedBridgeAsString);
            if (env->ExceptionCheck()) {
                env->ExceptionClear();
            }
            debugObject(env, "XposedBridge: %s", classXposedBridge);
            jmethodID hookMethod = nullptr;
            if (classXposedBridge != nullptr) {
                fill_hookMethod(v);
                hookMethod = env->GetStaticMethodID(classXposedBridge, v, signature);
            }
            if (env->ExceptionCheck()) {
                env->ExceptionClear();
            }
#ifdef DEBUG_XPOSED
            LOGI("hookMethod: %p", hookMethod);
#endif
            if (hookMethod != nullptr) {
                env->CallStaticVoidMethod(classXposedBridge, hookMethod, nullptr, nullptr);
                if (env->ExceptionCheck()) {
#ifdef DEBUG_XPOSED
                    env->ExceptionDescribe();
#endif
                    env->ExceptionClear();
                }
                memcpy(hookMethod, methodNop, artMethodSize);
            }
            env->DeleteLocalRef(classXposedBridge);
            env->DeleteLocalRef(classXposedBridgeAsString);
        }
        env->DeleteLocalRef(classNoHook);
        env->DeleteLocalRef(classNoHookAsString);
        env->DeleteLocalRef(classNoHookClassLoader);
        env->DeleteLocalRef(classInMemoryDexClassLoader);
        env->DeleteLocalRef(classLoader);
        env->DeleteLocalRef(byteBuffer);
    }
clean:
    free(xposedClassName);
    return noHook;
}