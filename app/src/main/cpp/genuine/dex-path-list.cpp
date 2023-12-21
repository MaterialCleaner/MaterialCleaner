//
// Created by Thom on 2022/5/3.
//

#include <string.h>
#include <stdlib.h>
#include "dex-path-list.h"
#include "classloader.h"
#include "common.h"

static inline void fill_java_util_Arrays(char v[]) {
    // java/util/Arrays
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
    v[0x5] = '}';
    v[0x6] = '}';
    v[0x7] = 'c';
    v[0x8] = 'g';
    v[0x9] = '#';
    v[0xa] = 'A';
    v[0xb] = 's';
    v[0xc] = 'p';
    v[0xd] = 'b';
    v[0xe] = '}';
    v[0xf] = 'v';
    for (unsigned int i = 0; i < 0x10; ++i) {
        v[i] ^= ((i + 0x10) % m);
    }
    v[0x10] = '\0';
}

static inline void fill_dexElements(char v[]) {
    // dexElements
    static unsigned int m = 0;

    if (m == 0) {
        m = 7;
    } else if (m == 11) {
        m = 13;
    }

    v[0x0] = '`';
    v[0x1] = '`';
    v[0x2] = '~';
    v[0x3] = 'E';
    v[0x4] = 'm';
    v[0x5] = 'g';
    v[0x6] = 'n';
    v[0x7] = 'a';
    v[0x8] = 'k';
    v[0x9] = 'r';
    v[0xa] = 's';
    for (unsigned int i = 0; i < 0xb; ++i) {
        v[i] ^= ((i + 0xb) % m);
    }
    v[0xb] = '\0';
}

static inline void fill_dexElements_signature(char v[]) {
    // [Ldalvik/system/DexPathList$Element;
    static unsigned int m = 0;

    if (m == 0) {
        m = 31;
    } else if (m == 37) {
        m = 41;
    }

    v[0x0] = '^';
    v[0x1] = 'J';
    v[0x2] = 'c';
    v[0x3] = 'i';
    v[0x4] = 'e';
    v[0x5] = '|';
    v[0x6] = 'b';
    v[0x7] = 'g';
    v[0x8] = '"';
    v[0x9] = '}';
    v[0xa] = 'v';
    v[0xb] = 'c';
    v[0xc] = 'e';
    v[0xd] = 'w';
    v[0xe] = '~';
    v[0xf] = ';';
    v[0x10] = 'Q';
    v[0x11] = 's';
    v[0x12] = 'o';
    v[0x13] = 'H';
    v[0x14] = 'x';
    v[0x15] = 'n';
    v[0x16] = 's';
    v[0x17] = 'P';
    v[0x18] = 't';
    v[0x19] = 'm';
    v[0x1a] = 't';
    v[0x1b] = '%';
    v[0x1c] = 'G';
    v[0x1d] = 'o';
    v[0x1e] = 'a';
    v[0x1f] = 'h';
    v[0x20] = 'c';
    v[0x21] = 'i';
    v[0x22] = '|';
    v[0x23] = '2';
    for (unsigned int i = 0; i < 0x24; ++i) {
        v[i] ^= ((i + 0x24) % m);
    }
    v[0x24] = '\0';
}

static inline void fill_InMemoryDexFile(char v[]) {
    // InMemoryDexFile
    static unsigned int m = 0;

    if (m == 0) {
        m = 13;
    } else if (m == 17) {
        m = 19;
    }

    v[0x0] = 'K';
    v[0x1] = 'm';
    v[0x2] = 'I';
    v[0x3] = '`';
    v[0x4] = 'k';
    v[0x5] = 'h';
    v[0x6] = 'z';
    v[0x7] = 'p';
    v[0x8] = 'N';
    v[0x9] = 'n';
    v[0xa] = 't';
    v[0xb] = 'F';
    v[0xc] = 'h';
    v[0xd] = 'n';
    v[0xe] = 'f';
    for (unsigned int i = 0; i < 0xf; ++i) {
        v[i] ^= ((i + 0xf) % m);
    }
    v[0xf] = '\0';
}

static inline void fill__apk(char v[]) {
    // .apk
    static unsigned int m = 0;

    if (m == 0) {
        m = 3;
    } else if (m == 5) {
        m = 7;
    }

    v[0x0] = '/';
    v[0x1] = 'c';
    v[0x2] = 'p';
    v[0x3] = 'j';
    for (unsigned int i = 0; i < 0x4; ++i) {
        v[i] ^= ((i + 0x4) % m);
    }
    v[0x4] = '\0';
}

static inline void fill__data_app_(char v[]) {
    // /data/app/
    static unsigned int m = 0;

    if (m == 0) {
        m = 7;
    } else if (m == 11) {
        m = 13;
    }

    v[0x0] = ',';
    v[0x1] = '`';
    v[0x2] = 'd';
    v[0x3] = 'r';
    v[0x4] = 'a';
    v[0x5] = '.';
    v[0x6] = 'c';
    v[0x7] = 's';
    v[0x8] = 't';
    v[0x9] = '*';
    for (unsigned int i = 0; i < 0xa; ++i) {
        v[i] ^= ((i + 0xa) % m);
    }
    v[0xa] = '\0';
}

static bool checkAbnormal(const char *check, bool dex, bool *hasInMemoryDex) {
    char v[0x10];
    fill_InMemoryDexFile(v);
    if (dex && strstr(check, v)) {
        if (hasInMemoryDex) {
            *hasInMemoryDex = true;
        }
    }
    fill__data_app_(v);
    if (!strstr(check, v)) {
        return false;
    }
    fill__apk(v);
    bool abnormal = false;
    if (strstr(check, v)) {
        char *packageName = getGenuinePackageName();
        if (!strstr(check, packageName)) {
            LOGW(check);
            abnormal = true;
        }
#ifdef GENUINE_NAME
        free(packageName);
#endif
    }
    return abnormal;
}

static inline void fill_nativeLibraryDirectories(char v[]) {
    // nativeLibraryDirectories
    static unsigned int m = 0;

    if (m == 0) {
        m = 23;
    } else if (m == 29) {
        m = 31;
    }

    v[0x0] = 'o';
    v[0x1] = 'c';
    v[0x2] = 'w';
    v[0x3] = 'm';
    v[0x4] = 's';
    v[0x5] = 'c';
    v[0x6] = 'K';
    v[0x7] = 'a';
    v[0x8] = 'k';
    v[0x9] = 'x';
    v[0xa] = 'j';
    v[0xb] = '~';
    v[0xc] = 't';
    v[0xd] = 'J';
    v[0xe] = 'f';
    v[0xf] = 'b';
    v[0x10] = 't';
    v[0x11] = 'q';
    v[0x12] = 'g';
    v[0x13] = '{';
    v[0x14] = 'g';
    v[0x15] = '\x7f';
    v[0x16] = 'e';
    v[0x17] = 'r';
    for (unsigned int i = 0; i < 0x18; ++i) {
        v[i] ^= ((i + 0x18) % m);
    }
    v[0x18] = '\0';
}

static inline void fill_nativeLibraryDirectories_signature(char v[]) {
    // Ljava/util/List;
    static unsigned int m = 0;

    if (m == 0) {
        m = 13;
    } else if (m == 17) {
        m = 19;
    }

    v[0x0] = 'O';
    v[0x1] = 'n';
    v[0x2] = 'd';
    v[0x3] = 'p';
    v[0x4] = 'f';
    v[0x5] = '\'';
    v[0x6] = '|';
    v[0x7] = '~';
    v[0x8] = 'b';
    v[0x9] = '`';
    v[0xa] = '/';
    v[0xb] = 'M';
    v[0xc] = 'k';
    v[0xd] = 'p';
    v[0xe] = 'p';
    v[0xf] = '>';
    for (unsigned int i = 0; i < 0x10; ++i) {
        v[i] ^= ((i + 0x10) % m);
    }
    v[0x10] = '\0';
}

static inline void fill_java_util_List(char v[]) {
    // java/util/List
    static unsigned int m = 0;

    if (m == 0) {
        m = 13;
    } else if (m == 17) {
        m = 19;
    }

    v[0x0] = 'k';
    v[0x1] = 'c';
    v[0x2] = 'u';
    v[0x3] = 'e';
    v[0x4] = '*';
    v[0x5] = 's';
    v[0x6] = 's';
    v[0x7] = 'a';
    v[0x8] = 'e';
    v[0x9] = '%';
    v[0xa] = 'G';
    v[0xb] = 'e';
    v[0xc] = 's';
    v[0xd] = 'u';
    for (unsigned int i = 0; i < 0xe; ++i) {
        v[i] ^= ((i + 0xe) % m);
    }
    v[0xe] = '\0';
}

static inline void fill_toString(char v[]) {
    // toString
    static unsigned int m = 0;

    if (m == 0) {
        m = 7;
    } else if (m == 11) {
        m = 13;
    }

    v[0x0] = 'u';
    v[0x1] = 'm';
    v[0x2] = 'P';
    v[0x3] = 'p';
    v[0x4] = 'w';
    v[0x5] = 'o';
    v[0x6] = 'n';
    v[0x7] = 'f';
    for (unsigned int i = 0; i < 0x8; ++i) {
        v[i] ^= ((i + 0x8) % m);
    }
    v[0x8] = '\0';
}

static inline void fill_Arrays_toString_signature(char v[]) {
    // ([Ljava/lang/Object;)Ljava/lang/String;
    static unsigned int m = 0;

    if (m == 0) {
        m = 37;
    } else if (m == 41) {
        m = 43;
    }

    v[0x0] = '*';
    v[0x1] = 'X';
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
    v[0xd] = '@';
    v[0xe] = 'r';
    v[0xf] = '{';
    v[0x10] = 'w';
    v[0x11] = 'p';
    v[0x12] = '`';
    v[0x13] = '.';
    v[0x14] = '?';
    v[0x15] = '[';
    v[0x16] = 'r';
    v[0x17] = 'x';
    v[0x18] = 'l';
    v[0x19] = 'z';
    v[0x1a] = '3';
    v[0x1b] = 'q';
    v[0x1c] = '\x7f';
    v[0x1d] = 'q';
    v[0x1e] = 'G';
    v[0x1f] = '\x0e';
    v[0x20] = 'q';
    v[0x21] = 'W';
    v[0x22] = 'V';
    v[0x23] = 'i';
    v[0x24] = 'o';
    v[0x25] = 'e';
    v[0x26] = '8';
    for (unsigned int i = 0; i < 0x27; ++i) {
        v[i] ^= ((i + 0x27) % m);
    }
    v[0x27] = '\0';
}

static inline void fill_toArray(char v[]) {
    // toArray
    static unsigned int m = 0;

    if (m == 0) {
        m = 5;
    } else if (m == 7) {
        m = 11;
    }

    v[0x0] = 'v';
    v[0x1] = 'l';
    v[0x2] = 'E';
    v[0x3] = 'r';
    v[0x4] = 's';
    v[0x5] = 'c';
    v[0x6] = 'z';
    for (unsigned int i = 0; i < 0x7; ++i) {
        v[i] ^= ((i + 0x7) % m);
    }
    v[0x7] = '\0';
}

static inline void fill_Arrays_toArray_signature(char v[]) {
    // ()[Ljava/lang/Object;
    static unsigned int m = 0;

    if (m == 0) {
        m = 19;
    } else if (m == 23) {
        m = 29;
    }

    v[0x0] = '*';
    v[0x1] = '*';
    v[0x2] = '_';
    v[0x3] = 'I';
    v[0x4] = 'l';
    v[0x5] = 'f';
    v[0x6] = '~';
    v[0x7] = 'h';
    v[0x8] = '%';
    v[0x9] = 'g';
    v[0xa] = 'm';
    v[0xb] = 'c';
    v[0xc] = 'i';
    v[0xd] = ' ';
    v[0xe] = '_';
    v[0xf] = 's';
    v[0x10] = 'x';
    v[0x11] = 'e';
    v[0x12] = 'b';
    v[0x13] = 'v';
    v[0x14] = '8';
    for (unsigned int i = 0; i < 0x15; ++i) {
        v[i] ^= ((i + 0x15) % m);
    }
    v[0x15] = '\0';
}

static inline void fill_isEmpty(char v[]) {
    // isEmpty
    static unsigned int m = 0;

    if (m == 0) {
        m = 5;
    } else if (m == 7) {
        m = 11;
    }

    v[0x0] = 'k';
    v[0x1] = 'p';
    v[0x2] = 'A';
    v[0x3] = 'm';
    v[0x4] = 'q';
    v[0x5] = 'v';
    v[0x6] = 'z';
    for (unsigned int i = 0; i < 0x7; ++i) {
        v[i] ^= ((i + 0x7) % m);
    }
    v[0x7] = '\0';
}

static inline void fill_Arrays_isEmpty_signature(char v[]) {
    // ()Z
    static unsigned int m = 0;

    if (m == 0) {
        m = 2;
    } else if (m == 3) {
        m = 5;
    }

    v[0x0] = ')';
    v[0x1] = ')';
    v[0x2] = '[';
    for (unsigned int i = 0; i < 0x3; ++i) {
        v[i] ^= ((i + 0x3) % m);
    }
    v[0x3] = '\0';
}

static inline void fill_pathList(char v[]) {
    // pathList
    static unsigned int m = 0;

    if (m == 0) {
        m = 7;
    } else if (m == 11) {
        m = 13;
    }

    v[0x0] = 'q';
    v[0x1] = 'c';
    v[0x2] = 'w';
    v[0x3] = 'l';
    v[0x4] = 'I';
    v[0x5] = 'o';
    v[0x6] = 's';
    v[0x7] = 'u';
    for (unsigned int i = 0; i < 0x8; ++i) {
        v[i] ^= ((i + 0x8) % m);
    }
    v[0x8] = '\0';
}

static inline void fill_pathList_signature(char v[]) {
    // Ldalvik/system/DexPathList;
    static unsigned int m = 0;

    if (m == 0) {
        m = 23;
    } else if (m == 29) {
        m = 31;
    }

    v[0x0] = 'H';
    v[0x1] = 'a';
    v[0x2] = 'g';
    v[0x3] = 'k';
    v[0x4] = '~';
    v[0x5] = '`';
    v[0x6] = 'a';
    v[0x7] = '$';
    v[0x8] = '\x7f';
    v[0x9] = 't';
    v[0xa] = '}';
    v[0xb] = '{';
    v[0xc] = 'u';
    v[0xd] = '|';
    v[0xe] = '=';
    v[0xf] = 'W';
    v[0x10] = 'q';
    v[0x11] = 'm';
    v[0x12] = 'F';
    v[0x13] = 'a';
    v[0x14] = 'u';
    v[0x15] = 'j';
    v[0x16] = 'O';
    v[0x17] = 'm';
    v[0x18] = 'v';
    v[0x19] = 'r';
    v[0x1a] = '<';
    for (unsigned int i = 0; i < 0x1b; ++i) {
        v[i] ^= ((i + 0x1b) % m);
    }
    v[0x1b] = '\0';
}

static bool showDexPathList(JNIEnv *env, jobject dexPathList, bool *hasInMemoryDex) {
    bool abnormal;
    jobject nativeLibraryDirectories;
    char v[0x40], v2[0x40];
    jclass classDexPathList = env->GetObjectClass(dexPathList);
    fill_dexElements(v);
    fill_dexElements_signature(v2);
    jfieldID fieldDexElements = env->GetFieldID(classDexPathList, v, v2);
    if (env->ExceptionCheck()) {
#ifdef DEBUG
        env->ExceptionDescribe();
#endif
        env->ExceptionClear();
    }
#ifdef DEBUG
    LOGI("fieldDexElements: %p", fieldDexElements);
#endif
    fill_nativeLibraryDirectories(v);
    fill_nativeLibraryDirectories_signature(v2);
    jfieldID fieldNativeLibraryDirectories = env->GetFieldID(classDexPathList, v, v2);
    if (env->ExceptionCheck()) {
#ifdef DEBUG
        env->ExceptionDescribe();
#endif
        env->ExceptionClear();
    }
#ifdef DEBUG
    LOGI("fieldNativeLibraryDirectories: %p", fieldNativeLibraryDirectories);
#endif
    fill_java_util_Arrays(v);
    jclass classArrays = env->FindClass(v);
    fill_toString(v);
    fill_Arrays_toString_signature(v2);
    jmethodID methodArraysToString = env->GetStaticMethodID(classArrays, v, v2);
    fill_java_util_List(v);
    jclass classList = env->FindClass(v);
    fill_toArray(v);
    fill_Arrays_toArray_signature(v2);
    jmethodID methodListToArray = env->GetMethodID(classList, v, v2);
    fill_isEmpty(v);
    fill_Arrays_isEmpty_signature(v2);
    jmethodID methodListIsEmpty = env->GetMethodID(classList, v, v2);
    if (fieldDexElements == nullptr) {
        auto dexPathListAsString = (jstring) env->CallNonvirtualObjectMethod(dexPathList, classDexPathList, methodObjectToString);
        const char *dexPathListAsChar = env->GetStringUTFChars(dexPathListAsString, nullptr);
#ifdef DEBUG
        LOGI("dexPathList: %s", dexPathListAsChar);
#endif
        abnormal = checkAbnormal(dexPathListAsChar, true, hasInMemoryDex);
        env->ReleaseStringUTFChars(dexPathListAsString, dexPathListAsChar);
        env->DeleteLocalRef(dexPathListAsString);
    } else {
        auto dexElements = (jobjectArray) env->GetObjectField(dexPathList, fieldDexElements);
        auto dexElementsAsString = (jstring) env->CallStaticObjectMethod(classArrays, methodArraysToString, dexElements);
        const char *dexElementsAsChar = env->GetStringUTFChars(dexElementsAsString, nullptr);
#ifdef DEBUG
        LOGI("dexElements: %s", dexElementsAsChar);
#endif
        abnormal = checkAbnormal(dexElementsAsChar, true, hasInMemoryDex);
        env->ReleaseStringUTFChars(dexElementsAsString, dexElementsAsChar);
        env->DeleteLocalRef(dexElementsAsString);
        env->DeleteLocalRef(dexElements);
    }
    if (fieldNativeLibraryDirectories == nullptr) {
        goto clean;
    }
    nativeLibraryDirectories = env->GetObjectField(dexPathList, fieldNativeLibraryDirectories);
    if (nativeLibraryDirectories == nullptr) {
        goto clean;
    }
    if (!env->CallBooleanMethod(nativeLibraryDirectories, methodListIsEmpty)) {
        jobject nativeLibraryDirectoriesAsArray = env->CallObjectMethod(nativeLibraryDirectories, methodListToArray);
        auto nativeLibraryDirectoriesAsString = (jstring) env->CallStaticObjectMethod(classArrays, methodArraysToString, nativeLibraryDirectoriesAsArray);
        const char *nativeLibraryDirectoriesAsChar = env->GetStringUTFChars(nativeLibraryDirectoriesAsString, nullptr);
#ifdef DEBUG
        LOGI("nativeLibraryDirectories: %s", nativeLibraryDirectoriesAsChar);
#endif
        abnormal |= checkAbnormal(nativeLibraryDirectoriesAsChar, false, hasInMemoryDex);
        env->ReleaseStringUTFChars(nativeLibraryDirectoriesAsString, nativeLibraryDirectoriesAsChar);
        env->DeleteLocalRef(nativeLibraryDirectoriesAsString);
        env->DeleteLocalRef(nativeLibraryDirectoriesAsArray);
    }
    env->DeleteLocalRef(nativeLibraryDirectories);
clean:
    env->DeleteLocalRef(classList);
    env->DeleteLocalRef(classArrays);
    env->DeleteLocalRef(classDexPathList);
    return abnormal;
}

bool hasAbnormalClassLoader(JNIEnv *env, jclass baseDexClassLoader, jobject object, bool *hasInMemoryDex) {
    bool abnormal;
    char v[0x40], v2[0x40];
    if (hasInMemoryDex) {
        *hasInMemoryDex = false;
    }
    jclass clazz = env->GetObjectClass(object);
    debug(env, "baseDexClassLoader: %s", baseDexClassLoader);
    fill_pathList(v);
    fill_pathList_signature(v2);
    jfieldID fieldPathList = env->GetFieldID(baseDexClassLoader, v, v2);
#ifdef DEBUG
    LOGI("fieldPathList: %p", fieldPathList);
#endif
    if (env->ExceptionCheck()) {
#ifdef DEBUG
        env->ExceptionDescribe();
#endif
        env->ExceptionClear();
    }
    if (fieldPathList != nullptr) {
        jobject pathList = env->GetObjectField(object, fieldPathList);
        abnormal = showDexPathList(env, pathList, hasInMemoryDex);
        env->DeleteLocalRef(pathList);
    } else {
        auto classLoaderAsString = (jstring) env->CallNonvirtualObjectMethod(object, baseDexClassLoader, methodObjectToString);
        const char *classLoaderAsChar = env->GetStringUTFChars(classLoaderAsString, nullptr);
        abnormal = checkAbnormal(classLoaderAsChar, true, hasInMemoryDex);
        env->ReleaseStringUTFChars(classLoaderAsString, classLoaderAsChar);
        env->DeleteLocalRef(classLoaderAsString);
    }
    env->DeleteLocalRef(clazz);
    return abnormal;
}