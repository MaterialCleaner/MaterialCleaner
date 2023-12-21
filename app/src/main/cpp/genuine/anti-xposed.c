//
// Created by Thom on 2019/3/7.
//

#include <stdio.h>
#include <stdbool.h>
#include <string.h>
#include <limits.h>
#include <dlfcn.h>

#include "common.h"
#include "anti-xposed.h"
#include "plt.h"

#ifndef NELEM
#define NELEM(x) (sizeof(x) / sizeof((x)[0]))
#endif

#define likely(x) __builtin_expect(!!(x), 1)

static jclass originalXposedClass;
static jmethodID originalXposedMethod;

static inline void fill_invoke(char v[]) {
    // invoke
    static unsigned int m = 0;

    if (m == 0) {
        m = 5;
    } else if (m == 7) {
        m = 11;
    }

    v[0x0] = 'h';
    v[0x1] = 'l';
    v[0x2] = 'u';
    v[0x3] = 'k';
    v[0x4] = 'k';
    v[0x5] = 'd';
    for (unsigned int i = 0; i < 0x6; ++i) {
        v[i] ^= ((i + 0x6) % m);
    }
    v[0x6] = '\0';
}

static inline void fill_invoke_signature(char v[]) {
    // (Ljava/lang/reflect/Member;ILjava/lang/Object;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;
    static unsigned int m = 0;

    if (m == 0) {
        m = 101;
    } else if (m == 103) {
        m = 107;
    }

    v[0x0] = ')';
    v[0x1] = 'N';
    v[0x2] = 'i';
    v[0x3] = 'e';
    v[0x4] = 's';
    v[0x5] = 'g';
    v[0x6] = '(';
    v[0x7] = 'd';
    v[0x8] = 'h';
    v[0x9] = 'd';
    v[0xa] = 'l';
    v[0xb] = '#';
    v[0xc] = '\x7f';
    v[0xd] = 'k';
    v[0xe] = 'i';
    v[0xf] = '|';
    v[0x10] = 't';
    v[0x11] = 'q';
    v[0x12] = 'g';
    v[0x13] = ';';
    v[0x14] = 'X';
    v[0x15] = 's';
    v[0x16] = 'z';
    v[0x17] = 'z';
    v[0x18] = '|';
    v[0x19] = 'h';
    v[0x1a] = ' ';
    v[0x1b] = 'U';
    v[0x1c] = 'Q';
    v[0x1d] = 't';
    v[0x1e] = '~';
    v[0x1f] = 'V';
    v[0x20] = '@';
    v[0x21] = '\r';
    v[0x22] = 'O';
    v[0x23] = 'E';
    v[0x24] = 'K';
    v[0x25] = 'A';
    v[0x26] = '\x08';
    v[0x27] = 'g';
    v[0x28] = 'K';
    v[0x29] = '@';
    v[0x2a] = 'N';
    v[0x2b] = 'O';
    v[0x2c] = 'Y';
    v[0x2d] = '\x15';
    v[0x2e] = 'c';
    v[0x2f] = 'Z';
    v[0x30] = 'P';
    v[0x31] = 'D';
    v[0x32] = 'R';
    v[0x33] = '\x1b';
    v[0x34] = 'Y';
    v[0x35] = 'W';
    v[0x36] = 'Y';
    v[0x37] = '_';
    v[0x38] = '\x16';
    v[0x39] = 'u';
    v[0x3a] = 'Y';
    v[0x3b] = 'V';
    v[0x3c] = 'X';
    v[0x3d] = ']';
    v[0x3e] = 'K';
    v[0x3f] = '{';
    v[0x40] = '\x1a';
    v[0x41] = '\x0e';
    v[0x42] = ')';
    v[0x43] = '%';
    v[0x44] = '3';
    v[0x45] = '\'';
    v[0x46] = 'h';
    v[0x47] = '$';
    v[0x48] = '(';
    v[0x49] = '$';
    v[0x4a] = ',';
    v[0x4b] = 'c';
    v[0x4c] = '\x02';
    v[0x4d] = ',';
    v[0x4e] = '%';
    v[0x4f] = '5';
    v[0x50] = '2';
    v[0x51] = '&';
    v[0x52] = 'h';
    v[0x53] = '}';
    v[0x54] = '\x19';
    v[0x55] = '<';
    v[0x56] = '6';
    v[0x57] = '.';
    v[0x58] = '8';
    v[0x59] = 'u';
    v[0x5a] = '7';
    v[0x5b] = '=';
    v[0x5c] = '3';
    v[0x5d] = '9';
    v[0x5e] = 'p';
    v[0x5f] = '/';
    v[0x60] = '\x03';
    v[0x61] = '\x08';
    v[0x62] = '\x06';
    v[0x63] = '\x07';
    v[0x64] = 't';
    v[0x65] = ':';
    for (unsigned int i = 0; i < 0x66; ++i) {
        v[i] ^= ((i + 0x66) % m);
    }
    v[0x66] = '\0';
}

static inline void fill_java_lang_reflect_InvocationTargetException(char v[]) {
    // java/lang/reflect/InvocationTargetException
    static unsigned int m = 0;

    if (m == 0) {
        m = 41;
    } else if (m == 43) {
        m = 47;
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
    v[0xa] = '~';
    v[0xb] = 'h';
    v[0xc] = 'h';
    v[0xd] = 'c';
    v[0xe] = 'u';
    v[0xf] = 'r';
    v[0x10] = 'f';
    v[0x11] = '<';
    v[0x12] = ']';
    v[0x13] = '{';
    v[0x14] = '`';
    v[0x15] = 'x';
    v[0x16] = '{';
    v[0x17] = 'x';
    v[0x18] = 'n';
    v[0x19] = 'r';
    v[0x1a] = 's';
    v[0x1b] = 's';
    v[0x1c] = 'J';
    v[0x1d] = '~';
    v[0x1e] = 'R';
    v[0x1f] = 'F';
    v[0x20] = 'G';
    v[0x21] = 'W';
    v[0x22] = 'a';
    v[0x23] = ']';
    v[0x24] = 'E';
    v[0x25] = 'B';
    v[0x26] = 'X';
    v[0x27] = 't';
    v[0x28] = 'h';
    v[0x29] = 'm';
    v[0x2a] = 'm';
    for (unsigned int i = 0; i < 0x2b; ++i) {
        v[i] ^= ((i + 0x2b) % m);
    }
    v[0x2b] = '\0';
}

static inline void fill_java_lang_Throwable(char v[]) {
    // java/lang/Throwable
    static unsigned int m = 0;

    if (m == 0) {
        m = 17;
    } else if (m == 19) {
        m = 23;
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
    v[0xa] = 'X';
    v[0xb] = 'e';
    v[0xc] = '|';
    v[0xd] = '`';
    v[0xe] = 'g';
    v[0xf] = 'a';
    v[0x10] = 'c';
    v[0x11] = 'n';
    v[0x12] = 'f';
    for (unsigned int i = 0; i < 0x13; ++i) {
        v[i] ^= ((i + 0x13) % m);
    }
    v[0x13] = '\0';
}

static inline void fill_getCause(char v[]) {
    // getCause
    static unsigned int m = 0;

    if (m == 0) {
        m = 7;
    } else if (m == 11) {
        m = 13;
    }

    v[0x0] = 'f';
    v[0x1] = 'g';
    v[0x2] = 'w';
    v[0x3] = 'G';
    v[0x4] = 'd';
    v[0x5] = 's';
    v[0x6] = 's';
    v[0x7] = 'd';
    for (unsigned int i = 0; i < 0x8; ++i) {
        v[i] ^= ((i + 0x8) % m);
    }
    v[0x8] = '\0';
}

static inline void fill_getCause_signature(char v[]) {
    // ()Ljava/lang/Throwable;
    static unsigned int m = 0;

    if (m == 0) {
        m = 19;
    } else if (m == 23) {
        m = 29;
    }

    v[0x0] = ',';
    v[0x1] = ',';
    v[0x2] = 'J';
    v[0x3] = 'm';
    v[0x4] = 'i';
    v[0x5] = '\x7f';
    v[0x6] = 'k';
    v[0x7] = '$';
    v[0x8] = '`';
    v[0x9] = 'l';
    v[0xa] = '`';
    v[0xb] = 'h';
    v[0xc] = '?';
    v[0xd] = 'E';
    v[0xe] = 'z';
    v[0xf] = 'r';
    v[0x10] = 'n';
    v[0x11] = 'u';
    v[0x12] = 'b';
    v[0x13] = 'f';
    v[0x14] = 'i';
    v[0x15] = 'c';
    v[0x16] = '<';
    for (unsigned int i = 0; i < 0x17; ++i) {
        v[i] ^= ((i + 0x17) % m);
    }
    v[0x17] = '\0';
}

static jobject
invoke(JNIEnv *env, jclass clazz __unused, jobject m, jint i, jobject object __unused, jobject t,
       jobjectArray as) {
    jobject result = (*env)->CallStaticObjectMethod(env, originalXposedClass, originalXposedMethod, m, i, NULL, NULL, t, as);
    if ((*env)->ExceptionCheck(env)) {
        char v1[0x2c];
        jthrowable throwable = (*env)->ExceptionOccurred(env);
        (*env)->ExceptionClear(env);
        fill_java_lang_reflect_InvocationTargetException(v1); // v1: 0x2b
        jclass classInvokeException = (*env)->FindClass(env, v1);
        if ((*env)->IsInstanceOf(env, throwable, classInvokeException)) {
            char v2[0x18];
            fill_java_lang_Throwable(v1); // v1: 0x13
            jclass classThrowable = (*env)->FindClass(env, v1);
            fill_getCause(v1); // v1: 0x8
            fill_getCause_signature(v2); // v2: 0x17
            jmethodID getCause = (*env)->GetMethodID(env, classThrowable, v1, v2);
            jthrowable cause = (*env)->CallObjectMethod(env, throwable, getCause);
            (*env)->Throw(env, cause);
            (*env)->DeleteLocalRef(env, cause);
            (*env)->DeleteLocalRef(env, classThrowable);
        } else {
            (*env)->Throw(env, throwable);
        }
        (*env)->DeleteLocalRef(env, classInvokeException);
        (*env)->DeleteLocalRef(env, throwable);
    }
    return result;
}

static inline void fill__ZN3art6mirror9ArtMethod21xposed_callback_classE(char v[]) {
    // _ZN3art6mirror9ArtMethod21xposed_callback_classE
    static unsigned int m = 0;

    if (m == 0) {
        m = 47;
    } else if (m == 53) {
        m = 59;
    }

    v[0x0] = '^';
    v[0x1] = 'X';
    v[0x2] = 'M';
    v[0x3] = '7';
    v[0x4] = 'd';
    v[0x5] = 't';
    v[0x6] = 's';
    v[0x7] = '>';
    v[0x8] = 'd';
    v[0x9] = 'c';
    v[0xa] = 'y';
    v[0xb] = '~';
    v[0xc] = 'b';
    v[0xd] = '|';
    v[0xe] = '6';
    v[0xf] = 'Q';
    v[0x10] = 'c';
    v[0x11] = 'f';
    v[0x12] = '^';
    v[0x13] = 'q';
    v[0x14] = 'a';
    v[0x15] = '~';
    v[0x16] = 'x';
    v[0x17] = '|';
    v[0x18] = '+';
    v[0x19] = '+';
    v[0x1a] = 'c';
    v[0x1b] = 'l';
    v[0x1c] = 'r';
    v[0x1d] = 'm';
    v[0x1e] = 'z';
    v[0x1f] = 'D';
    v[0x20] = '~';
    v[0x21] = 'A';
    v[0x22] = 'B';
    v[0x23] = 'H';
    v[0x24] = 'I';
    v[0x25] = 'D';
    v[0x26] = 'F';
    v[0x27] = 'K';
    v[0x28] = 'B';
    v[0x29] = 'u';
    v[0x2a] = 'H';
    v[0x2b] = '@';
    v[0x2c] = 'L';
    v[0x2d] = ']';
    v[0x2e] = 's';
    v[0x2f] = 'D';
    for (unsigned int i = 0; i < 0x30; ++i) {
        v[i] ^= ((i + 0x30) % m);
    }
    v[0x30] = '\0';
}

static inline void fill__ZN3art9ArtMethod21xposed_callback_classE(char v[]) {
    // _ZN3art9ArtMethod21xposed_callback_classE
    static unsigned int m = 0;

    if (m == 0) {
        m = 37;
    } else if (m == 41) {
        m = 43;
    }

    v[0x0] = '[';
    v[0x1] = '_';
    v[0x2] = 'H';
    v[0x3] = '4';
    v[0x4] = 'i';
    v[0x5] = '{';
    v[0x6] = '~';
    v[0x7] = '2';
    v[0x8] = 'M';
    v[0x9] = '\x7f';
    v[0xa] = 'z';
    v[0xb] = 'B';
    v[0xc] = 'u';
    v[0xd] = 'e';
    v[0xe] = 'z';
    v[0xf] = '|';
    v[0x10] = 'p';
    v[0x11] = '\'';
    v[0x12] = '\'';
    v[0x13] = 'o';
    v[0x14] = 'h';
    v[0x15] = 'v';
    v[0x16] = 'i';
    v[0x17] = '~';
    v[0x18] = 'x';
    v[0x19] = 'B';
    v[0x1a] = '}';
    v[0x1b] = '~';
    v[0x1c] = 'L';
    v[0x1d] = 'M';
    v[0x1e] = '@';
    v[0x1f] = 'B';
    v[0x20] = 'G';
    v[0x21] = 'k';
    v[0x22] = '^';
    v[0x23] = 'a';
    v[0x24] = 'o';
    v[0x25] = 'e';
    v[0x26] = 'v';
    v[0x27] = 'u';
    v[0x28] = 'B';
    for (unsigned int i = 0; i < 0x29; ++i) {
        v[i] ^= ((i + 0x29) % m);
    }
    v[0x29] = '\0';
}

static inline void fill__ZN3art6mirror9ArtMethod22xposed_callback_methodE(char v[]) {
    // _ZN3art6mirror9ArtMethod22xposed_callback_methodE
    static unsigned int m = 0;

    if (m == 0) {
        m = 47;
    } else if (m == 53) {
        m = 59;
    }

    v[0x0] = ']';
    v[0x1] = 'Y';
    v[0x2] = 'J';
    v[0x3] = '6';
    v[0x4] = 'g';
    v[0x5] = 'u';
    v[0x6] = '|';
    v[0x7] = '?';
    v[0x8] = 'g';
    v[0x9] = 'b';
    v[0xa] = '~';
    v[0xb] = '\x7f';
    v[0xc] = 'a';
    v[0xd] = '}';
    v[0xe] = ')';
    v[0xf] = 'P';
    v[0x10] = '`';
    v[0x11] = 'g';
    v[0x12] = 'Y';
    v[0x13] = 'p';
    v[0x14] = 'b';
    v[0x15] = '\x7f';
    v[0x16] = 'w';
    v[0x17] = '}';
    v[0x18] = '(';
    v[0x19] = ')';
    v[0x1a] = 'd';
    v[0x1b] = 'm';
    v[0x1c] = 'q';
    v[0x1d] = 'l';
    v[0x1e] = 'E';
    v[0x1f] = 'E';
    v[0x20] = '}';
    v[0x21] = '@';
    v[0x22] = 'E';
    v[0x23] = 'I';
    v[0x24] = 'J';
    v[0x25] = 'E';
    v[0x26] = 'I';
    v[0x27] = 'J';
    v[0x28] = 'A';
    v[0x29] = 't';
    v[0x2a] = 'A';
    v[0x2b] = 'H';
    v[0x2c] = 'Z';
    v[0x2d] = 'h';
    v[0x2e] = 'n';
    v[0x2f] = 'f';
    v[0x30] = 'F';
    for (unsigned int i = 0; i < 0x31; ++i) {
        v[i] ^= ((i + 0x31) % m);
    }
    v[0x31] = '\0';
}

static inline void fill__ZN3art9ArtMethod22xposed_callback_methodE(char v[]) {
    // _ZN3art9ArtMethod22xposed_callback_methodE
    static unsigned int m = 0;

    if (m == 0) {
        m = 41;
    } else if (m == 43) {
        m = 47;
    }

    v[0x0] = '^';
    v[0x1] = 'X';
    v[0x2] = 'M';
    v[0x3] = '7';
    v[0x4] = 'd';
    v[0x5] = 't';
    v[0x6] = 's';
    v[0x7] = '1';
    v[0x8] = 'H';
    v[0x9] = 'x';
    v[0xa] = '\x7f';
    v[0xb] = 'A';
    v[0xc] = 'h';
    v[0xd] = 'z';
    v[0xe] = 'g';
    v[0xf] = '\x7f';
    v[0x10] = 'u';
    v[0x11] = ' ';
    v[0x12] = '!';
    v[0x13] = 'l';
    v[0x14] = 'e';
    v[0x15] = 'y';
    v[0x16] = 'd';
    v[0x17] = '}';
    v[0x18] = '}';
    v[0x19] = 'E';
    v[0x1a] = 'x';
    v[0x1b] = '}';
    v[0x1c] = 'q';
    v[0x1d] = 'r';
    v[0x1e] = '}';
    v[0x1f] = 'A';
    v[0x20] = 'B';
    v[0x21] = 'I';
    v[0x22] = '|';
    v[0x23] = 'I';
    v[0x24] = '@';
    v[0x25] = 'R';
    v[0x26] = 'O';
    v[0x27] = 'G';
    v[0x28] = 'd';
    v[0x29] = 'D';
    for (unsigned int i = 0; i < 0x2a; ++i) {
        v[i] ^= ((i + 0x2a) % m);
    }
    v[0x2a] = '\0';
}

static inline void fill_java_lang_VMClassLoader(char v[]) {
    // java/lang/VMClassLoader
    static unsigned int m = 0;

    if (m == 0) {
        m = 19;
    } else if (m == 23) {
        m = 29;
    }

    v[0x0] = 'n';
    v[0x1] = 'd';
    v[0x2] = 'p';
    v[0x3] = 'f';
    v[0x4] = '\'';
    v[0x5] = 'e';
    v[0x6] = 'k';
    v[0x7] = 'e';
    v[0x8] = 'k';
    v[0x9] = '"';
    v[0xa] = 'X';
    v[0xb] = 'B';
    v[0xc] = 'S';
    v[0xd] = '}';
    v[0xe] = 's';
    v[0xf] = 's';
    v[0x10] = 'r';
    v[0x11] = 'N';
    v[0x12] = 'l';
    v[0x13] = 'e';
    v[0x14] = 'a';
    v[0x15] = 'c';
    v[0x16] = 'u';
    for (unsigned int i = 0; i < 0x17; ++i) {
        v[i] ^= ((i + 0x17) % m);
    }
    v[0x17] = '\0';
}

static inline void fill_de_robv_android_xposed_XposedBridge(char v[]) {
    // de/robv/android/xposed/XposedBridge
    static unsigned int m = 0;

    if (m == 0) {
        m = 31;
    } else if (m == 37) {
        m = 41;
    }

    v[0x0] = '`';
    v[0x1] = '`';
    v[0x2] = ')';
    v[0x3] = 'u';
    v[0x4] = 'g';
    v[0x5] = 'k';
    v[0x6] = '|';
    v[0x7] = '$';
    v[0x8] = 'm';
    v[0x9] = 'c';
    v[0xa] = 'j';
    v[0xb] = '}';
    v[0xc] = '\x7f';
    v[0xd] = 'x';
    v[0xe] = 'v';
    v[0xf] = '<';
    v[0x10] = 'l';
    v[0x11] = 'e';
    v[0x12] = 'y';
    v[0x13] = 'd';
    v[0x14] = '}';
    v[0x15] = '}';
    v[0x16] = '5';
    v[0x17] = 'C';
    v[0x18] = 'l';
    v[0x19] = 'r';
    v[0x1a] = 'm';
    v[0x1b] = 'e';
    v[0x1c] = 'e';
    v[0x1d] = '@';
    v[0x1e] = 'q';
    v[0x1f] = 'm';
    v[0x20] = 'a';
    v[0x21] = 'a';
    v[0x22] = 'b';
    for (unsigned int i = 0; i < 0x23; ++i) {
        v[i] ^= ((i + 0x23) % m);
    }
    v[0x23] = '\0';
}

static inline void fill_findLoadedClass(char v[]) {
    // findLoadedClass
    static unsigned int m = 0;

    if (m == 0) {
        m = 13;
    } else if (m == 17) {
        m = 19;
    }

    v[0x0] = 'd';
    v[0x1] = 'j';
    v[0x2] = 'j';
    v[0x3] = 'a';
    v[0x4] = 'J';
    v[0x5] = 'h';
    v[0x6] = 'i';
    v[0x7] = 'm';
    v[0x8] = 'o';
    v[0x9] = 'o';
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

static inline void fill_findLoadedClass_signature(char v[]) {
    // (Ljava/lang/ClassLoader;Ljava/lang/String;)Ljava/lang/Class;
    static unsigned int m = 0;

    if (m == 0) {
        m = 59;
    } else if (m == 61) {
        m = 67;
    }

    v[0x0] = ')';
    v[0x1] = 'N';
    v[0x2] = 'i';
    v[0x3] = 'e';
    v[0x4] = 's';
    v[0x5] = 'g';
    v[0x6] = '(';
    v[0x7] = 'd';
    v[0x8] = 'h';
    v[0x9] = 'd';
    v[0xa] = 'l';
    v[0xb] = '#';
    v[0xc] = 'N';
    v[0xd] = 'b';
    v[0xe] = 'n';
    v[0xf] = 'c';
    v[0x10] = 'b';
    v[0x11] = '^';
    v[0x12] = '|';
    v[0x13] = 'u';
    v[0x14] = 'q';
    v[0x15] = 's';
    v[0x16] = 'e';
    v[0x17] = '#';
    v[0x18] = 'U';
    v[0x19] = 'p';
    v[0x1a] = 'z';
    v[0x1b] = 'j';
    v[0x1c] = '|';
    v[0x1d] = '1';
    v[0x1e] = 's';
    v[0x1f] = 'A';
    v[0x20] = 'O';
    v[0x21] = 'E';
    v[0x22] = '\x0c';
    v[0x23] = 'w';
    v[0x24] = 'Q';
    v[0x25] = 'T';
    v[0x26] = 'N';
    v[0x27] = 'F';
    v[0x28] = 'N';
    v[0x29] = '\x11';
    v[0x2a] = '\x02';
    v[0x2b] = '`';
    v[0x2c] = 'G';
    v[0x2d] = 'O';
    v[0x2e] = 'Y';
    v[0x2f] = 'Q';
    v[0x30] = '\x1e';
    v[0x31] = '^';
    v[0x32] = 'R';
    v[0x33] = 'Z';
    v[0x34] = 'R';
    v[0x35] = '\x19';
    v[0x36] = 't';
    v[0x37] = 'T';
    v[0x38] = 'X';
    v[0x39] = 'I';
    v[0x3a] = 's';
    v[0x3b] = ':';
    for (unsigned int i = 0; i < 0x3c; ++i) {
        v[i] ^= ((i + 0x3c) % m);
    }
    v[0x3c] = '\0';
}

static inline void fill_invokeOriginalMethodNative(char v[]) {
    // invokeOriginalMethodNative
    static unsigned int m = 0;

    if (m == 0) {
        m = 23;
    } else if (m == 29) {
        m = 31;
    }

    v[0x0] = 'j';
    v[0x1] = 'j';
    v[0x2] = 's';
    v[0x3] = 'i';
    v[0x4] = 'l';
    v[0x5] = 'm';
    v[0x6] = 'F';
    v[0x7] = 'x';
    v[0x8] = 'b';
    v[0x9] = 'k';
    v[0xa] = 'd';
    v[0xb] = '`';
    v[0xc] = 'n';
    v[0xd] = '|';
    v[0xe] = '\\';
    v[0xf] = 'w';
    v[0x10] = 'g';
    v[0x11] = '|';
    v[0x12] = 'z';
    v[0x13] = 'r';
    v[0x14] = 'N';
    v[0x15] = '`';
    v[0x16] = 'v';
    v[0x17] = 'j';
    v[0x18] = 'r';
    v[0x19] = '`';
    for (unsigned int i = 0; i < 0x1a; ++i) {
        v[i] ^= ((i + 0x1a) % m);
    }
    v[0x1a] = '\0';
}

static inline void fill_invokeOriginalMethodNative_signature(char v[]) {
    // (Ljava/lang/reflect/Member;I[Ljava/lang/Class;Ljava/lang/Class;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;
    static unsigned int m = 0;

    if (m == 0) {
        m = 113;
    } else if (m == 127) {
        m = 131;
    }

    v[0x0] = '.';
    v[0x1] = 'K';
    v[0x2] = 'b';
    v[0x3] = 'h';
    v[0x4] = '|';
    v[0x5] = 'j';
    v[0x6] = '#';
    v[0x7] = 'a';
    v[0x8] = 'o';
    v[0x9] = 'a';
    v[0xa] = 'w';
    v[0xb] = '>';
    v[0xc] = '`';
    v[0xd] = 'v';
    v[0xe] = 'r';
    v[0xf] = 'y';
    v[0x10] = 's';
    v[0x11] = 't';
    v[0x12] = 'l';
    v[0x13] = '6';
    v[0x14] = 'W';
    v[0x15] = '~';
    v[0x16] = 'q';
    v[0x17] = '\x7f';
    v[0x18] = '{';
    v[0x19] = 'm';
    v[0x1a] = '\x1b';
    v[0x1b] = 'h';
    v[0x1c] = 'y';
    v[0x1d] = 'o';
    v[0x1e] = 'N';
    v[0x1f] = 'D';
    v[0x20] = 'P';
    v[0x21] = 'F';
    v[0x22] = '\x07';
    v[0x23] = 'E';
    v[0x24] = 'K';
    v[0x25] = 'E';
    v[0x26] = 'K';
    v[0x27] = '\x02';
    v[0x28] = 'm';
    v[0x29] = 'C';
    v[0x2a] = 'Q';
    v[0x2b] = 'B';
    v[0x2c] = 'A';
    v[0x2d] = '\x08';
    v[0x2e] = 'x';
    v[0x2f] = '_';
    v[0x30] = 'W';
    v[0x31] = 'A';
    v[0x32] = 'Y';
    v[0x33] = '\x16';
    v[0x34] = 'V';
    v[0x35] = 'Z';
    v[0x36] = 'R';
    v[0x37] = 'Z';
    v[0x38] = '\x11';
    v[0x39] = '|';
    v[0x3a] = ',';
    v[0x3b] = ' ';
    v[0x3c] = '1';
    v[0x3d] = '0';
    v[0x3e] = '\x7f';
    v[0x3f] = '\t';
    v[0x40] = ',';
    v[0x41] = '&';
    v[0x42] = '>';
    v[0x43] = '(';
    v[0x44] = 'e';
    v[0x45] = '\'';
    v[0x46] = '-';
    v[0x47] = '#';
    v[0x48] = ')';
    v[0x49] = '`';
    v[0x4a] = '\x1f';
    v[0x4b] = '3';
    v[0x4c] = '8';
    v[0x4d] = '6';
    v[0x4e] = '7';
    v[0x4f] = '!';
    v[0x50] = 'm';
    v[0x51] = '\x0c';
    v[0x52] = '\x14';
    v[0x53] = '3';
    v[0x54] = ';';
    v[0x55] = '-';
    v[0x56] = '=';
    v[0x57] = 'r';
    v[0x58] = '2';
    v[0x59] = '>';
    v[0x5a] = '\x0e';
    v[0x5b] = '\x06';
    v[0x5c] = 'M';
    v[0x5d] = ',';
    v[0x5e] = '\x06';
    v[0x5f] = '\x0f';
    v[0x60] = '\x03';
    v[0x61] = '\x04';
    v[0x62] = '\x1c';
    v[0x63] = 'R';
    v[0x64] = 'C';
    v[0x65] = '\'';
    v[0x66] = '\x06';
    v[0x67] = '\x0c';
    v[0x68] = '\x18';
    v[0x69] = '\x0e';
    v[0x6a] = '_';
    v[0x6b] = 'l';
    v[0x6c] = '`';
    v[0x6d] = 'l';
    v[0x6e] = 'd';
    v[0x6f] = '+';
    v[0x70] = 'J';
    v[0x71] = 'd';
    v[0x72] = 'm';
    v[0x73] = 'm';
    v[0x74] = 'j';
    v[0x75] = '~';
    v[0x76] = '0';
    for (unsigned int i = 0; i < 0x77; ++i) {
        v[i] ^= ((i + 0x77) % m);
    }
    v[0x77] = '\0';
}


static inline void fill_handleHookedMethod(char v[]) {
    // handleHookedMethod
    static unsigned int m = 0;

    if (m == 0) {
        m = 17;
    } else if (m == 19) {
        m = 23;
    }

    v[0x0] = 'i';
    v[0x1] = 'c';
    v[0x2] = 'm';
    v[0x3] = '`';
    v[0x4] = 'i';
    v[0x5] = 'c';
    v[0x6] = 'O';
    v[0x7] = 'g';
    v[0x8] = 'f';
    v[0x9] = 'a';
    v[0xa] = 'n';
    v[0xb] = 'h';
    v[0xc] = '@';
    v[0xd] = 'k';
    v[0xe] = '{';
    v[0xf] = 'x';
    v[0x10] = 'o';
    v[0x11] = 'e';
    for (unsigned int i = 0; i < 0x12; ++i) {
        v[i] ^= ((i + 0x12) % m);
    }
    v[0x12] = '\0';
}

jboolean antiXposed(JNIEnv *env, jclass clazz, int sdk, bool *xposed) {
    jboolean result = JNI_FALSE;
    char v1[0x80], v2[0x80];

    JNINativeMethod methods[1];
    fill_invoke(v1);
    fill_invoke_signature(v2);
    methods[0].name = strdup(v1);
    methods[0].signature = strdup(v2);
    methods[0].fnPtr = invoke;

    if ((*env)->RegisterNatives(env, clazz, methods, 1) < 0) {
        return JNI_FALSE;
    }

    if (sdk < 21) {
        return JNI_TRUE;
    }

    if (likely(sdk >= 23)) {
        fill__ZN3art9ArtMethod21xposed_callback_classE(v2);
        fill__ZN3art9ArtMethod22xposed_callback_methodE(v1);
    } else {
        fill__ZN3art6mirror9ArtMethod21xposed_callback_classE(v2);
        fill__ZN3art6mirror9ArtMethod22xposed_callback_methodE(v1);
    }

    jclass *xposedCallbackClass = (jclass *) plt_dlsym(v2, NULL);
    jmethodID *xposedCallbackMethod = (jmethodID *) plt_dlsym(v1, NULL);
#ifdef DEBUG
    LOGI("xposedCallbackClass: %p, xposedCallbackMethod: %p", xposedCallbackClass, xposedCallbackMethod);
#endif
    if (xposedCallbackClass == NULL || xposedCallbackMethod == NULL) {
        return JNI_TRUE;
    }

    fill_invokeOriginalMethodNative(v1);
    fill_invokeOriginalMethodNative_signature(v2);
    originalXposedMethod = (*env)->GetStaticMethodID(env, *xposedCallbackClass, v1, v2);
    if (originalXposedMethod == NULL) {
        (*env)->ExceptionClear(env);
        goto clean;
    }
    originalXposedClass = *xposedCallbackClass;

    fill_invoke(v1);
    fill_invoke_signature(v2);
    jmethodID replace = (*env)->GetStaticMethodID(env, clazz, v1, v2);
    if (replace == NULL) {
        (*env)->ExceptionClear(env);
        goto clean;
    }

    fill_handleHookedMethod(v1);
    jmethodID hooked = (*env)->GetStaticMethodID(env, *xposedCallbackClass, v1, v2);
    if (hooked == NULL) {
        (*env)->ExceptionClear(env);
        goto clean;
    }

#ifdef DEBUG
    LOGI("callback: %p, hooked: %p", *xposedCallbackMethod, hooked);
#endif
    if (*xposedCallbackMethod == hooked) {
        *xposed = true;
        *xposedCallbackMethod = replace;
        *xposedCallbackClass = (*env)->NewGlobalRef(env, clazz);
        result = JNI_TRUE;
        goto clean;
    }

clean:
    return result;
}

static inline void fill_disableHooks(char v[]) {
    // disableHooks
    static unsigned int m = 0;

    if (m == 0) {
        m = 11;
    } else if (m == 13) {
        m = 17;
    }

    v[0x0] = 'e';
    v[0x1] = 'k';
    v[0x2] = 'p';
    v[0x3] = 'e';
    v[0x4] = 'g';
    v[0x5] = 'j';
    v[0x6] = 'b';
    v[0x7] = '@';
    v[0x8] = 'f';
    v[0x9] = 'e';
    v[0xa] = 'k';
    v[0xb] = 'r';
    for (unsigned int i = 0; i < 0xc; ++i) {
        v[i] ^= ((i + 0xc) % m);
    }
    v[0xc] = '\0';

}

static inline void fill_disableHooks_signature(char v[]) {
    // Z
    static unsigned int m = 0;

    if (m == 0) {
        m = 2;
    } else if (m == 3) {
        m = 5;
    }

    v[0x0] = '[';
    for (unsigned int i = 0; i < 0x1; ++i) {
        v[i] ^= ((i + 0x1) % m);
    }
    v[0x1] = '\0';
}

jclass findLoadedClass(JNIEnv *env, jobject classLoader, const char *name) {
    char v1[0x80], v2[0x80];
    jclass loadedClass = NULL;
    jstring nameAsString;

    fill_java_lang_VMClassLoader(v1);
    jclass vmClassLoader = (*env)->FindClass(env, v1);
    if ((*env)->ExceptionCheck(env)) {
#ifdef DEBUG
        (*env)->ExceptionDescribe(env);
#endif
        (*env)->ExceptionClear(env);
    }
    if (vmClassLoader == NULL) {
#ifdef DEBUG
        LOGI("findLoadedClass, no %s", classLoader, v1);
#endif
        goto clean;
    }

    fill_findLoadedClass(v1);
    fill_findLoadedClass_signature(v2);
    jmethodID findLoadedClass = (*env)->GetStaticMethodID(env, vmClassLoader, v1, v2);
    if ((*env)->ExceptionCheck(env)) {
#ifdef DEBUG
        (*env)->ExceptionDescribe(env);
#endif
        (*env)->ExceptionClear(env);
    }
    if (findLoadedClass == NULL) {
#ifdef DEBUG
        char v3[0x80];
        fill_java_lang_VMClassLoader(v3);
        LOGI("findLoadedClass, no %s.%s%s", v3, v1, v2);
#endif
        goto cleanVmClassLoader;
    }

    nameAsString = (*env)->NewStringUTF(env, name);
    loadedClass = (jclass) (*env)->CallStaticObjectMethod(env,
                                                          vmClassLoader,
                                                          findLoadedClass,
                                                          classLoader,
                                                          nameAsString);

    if ((*env)->ExceptionCheck(env)) {
#ifdef DEBUG
        (*env)->ExceptionDescribe(env);
#endif
        (*env)->ExceptionClear(env);
    }

#ifdef DEBUG
    LOGI("findLoadedClass, name: %s, loaded class: %s", name, loadedClass);
#endif

    (*env)->DeleteLocalRef(env, nameAsString);
cleanVmClassLoader:
    (*env)->DeleteLocalRef(env, vmClassLoader);
clean:
    return loadedClass;
}

jclass findXposedBridge(JNIEnv *env, jobject classLoader) {
    char v1[0x80];
    fill_de_robv_android_xposed_XposedBridge(v1);
    return findLoadedClass(env, classLoader, v1);
}

bool disableXposedBridge(JNIEnv *env, jclass classXposedBridge) {
    char v1[0x80], v2[0x80];
    fill_disableHooks(v1);
    fill_disableHooks_signature(v2);
    jfieldID field = (*env)->GetStaticFieldID(env, classXposedBridge, v1, v2);
    if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionClear(env);
    }
    if (field == NULL) {
        return false;
    }
    (*env)->SetStaticBooleanField(env, classXposedBridge, field, JNI_TRUE);
    if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionClear(env);
        return false;
    }
    return true;
}