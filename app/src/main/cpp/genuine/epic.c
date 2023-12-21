#include "epic.h"
#include "anti-xposed.h"
#include "epic-method.h"
#include "epic-field.h"
#include <stdlib.h>

#ifdef CHECK_XPOSED_EPIC

static inline void fill_me_weishu_epic_art2_EpicNative(char v[]) {
    // me/weishu/epic/art2/EpicNative
    static unsigned int m = 0;

    if (m == 0) {
        m = 29;
    } else if (m == 31) {
        m = 37;
    }

    v[0x0] = 'l';
    v[0x1] = 'g';
    v[0x2] = ',';
    v[0x3] = 's';
    v[0x4] = '`';
    v[0x5] = 'o';
    v[0x6] = 't';
    v[0x7] = '`';
    v[0x8] = '|';
    v[0x9] = '%';
    v[0xa] = 'n';
    v[0xb] = '|';
    v[0xc] = 'd';
    v[0xd] = 'm';
    v[0xe] = ' ';
    v[0xf] = 'q';
    v[0x10] = 'c';
    v[0x11] = 'f';
    v[0x12] = '!';
    v[0x13] = ';';
    v[0x14] = 'P';
    v[0x15] = 'f';
    v[0x16] = '~';
    v[0x17] = '{';
    v[0x18] = 'W';
    v[0x19] = '{';
    v[0x1a] = 'o';
    v[0x1b] = 'u';
    v[0x1c] = 'v';
    v[0x1d] = 'd';
    for (unsigned int i = 0; i < 0x1e; ++i) {
        v[i] ^= ((i + 0x1e) % m);
    }
    v[0x1e] = '\0';
}

static inline void fill_me_weishu_epic_art2_EpicBridge(char v[]) {
    // me/weishu/epic/art2/EpicBridge
    static unsigned int m = 0;

    if (m == 0) {
        m = 29;
    } else if (m == 31) {
        m = 37;
    }

    v[0x0] = 'l';
    v[0x1] = 'g';
    v[0x2] = ',';
    v[0x3] = 's';
    v[0x4] = '`';
    v[0x5] = 'o';
    v[0x6] = 't';
    v[0x7] = '`';
    v[0x8] = '|';
    v[0x9] = '%';
    v[0xa] = 'n';
    v[0xb] = '|';
    v[0xc] = 'd';
    v[0xd] = 'm';
    v[0xe] = ' ';
    v[0xf] = 'q';
    v[0x10] = 'c';
    v[0x11] = 'f';
    v[0x12] = '!';
    v[0x13] = ';';
    v[0x14] = 'P';
    v[0x15] = 'f';
    v[0x16] = '~';
    v[0x17] = '{';
    v[0x18] = '[';
    v[0x19] = 'h';
    v[0x1a] = 'r';
    v[0x1b] = 'x';
    v[0x1c] = 'g';
    v[0x1d] = 'd';
    for (unsigned int i = 0; i < 0x1e; ++i) {
        v[i] ^= ((i + 0x1e) % m);
    }
    v[0x1e] = '\0';
}

static inline void fill_hookedMethodCallbacks(char v[]) {
    // hookedMethodCallbacks
    static unsigned int m = 0;

    if (m == 0) {
        m = 19;
    } else if (m == 23) {
        m = 29;
    }

    v[0x0] = 'j';
    v[0x1] = 'l';
    v[0x2] = 'k';
    v[0x3] = 'n';
    v[0x4] = 'c';
    v[0x5] = 'c';
    v[0x6] = 'E';
    v[0x7] = 'l';
    v[0x8] = '~';
    v[0x9] = 'c';
    v[0xa] = 'c';
    v[0xb] = 'i';
    v[0xc] = 'M';
    v[0xd] = 'n';
    v[0xe] = '|';
    v[0xf] = '}';
    v[0x10] = 'p';
    v[0x11] = 'a';
    v[0x12] = 'b';
    v[0x13] = 'i';
    v[0x14] = 'p';
    for (unsigned int i = 0; i < 0x15; ++i) {
        v[i] ^= ((i + 0x15) % m);
    }
    v[0x15] = '\0';
}

static inline void fill_hookedMethodCallbacks_signature(char v[]) {
    // Ljava/util/Map;
    static unsigned int m = 0;

    if (m == 0) {
        m = 13;
    } else if (m == 17) {
        m = 19;
    }

    v[0x0] = 'N';
    v[0x1] = 'i';
    v[0x2] = 'e';
    v[0x3] = 's';
    v[0x4] = 'g';
    v[0x5] = '(';
    v[0x6] = '}';
    v[0x7] = '}';
    v[0x8] = 'c';
    v[0x9] = 'g';
    v[0xa] = '#';
    v[0xb] = 'M';
    v[0xc] = '`';
    v[0xd] = 'r';
    v[0xe] = '8';
    for (unsigned int i = 0; i < 0xf; ++i) {
        v[i] ^= ((i + 0xf) % m);
    }
    v[0xf] = '\0';
}

static inline void fill_java_util_Map(char v[]) {
    // java/util/Map
    static unsigned int m = 0;

    if (m == 0) {
        m = 11;
    } else if (m == 13) {
        m = 17;
    }

    v[0x0] = 'h';
    v[0x1] = 'b';
    v[0x2] = 'r';
    v[0x3] = 'd';
    v[0x4] = ')';
    v[0x5] = 'r';
    v[0x6] = '|';
    v[0x7] = '`';
    v[0x8] = 'f';
    v[0x9] = '/';
    v[0xa] = 'L';
    v[0xb] = 'c';
    v[0xc] = 's';
    for (unsigned int i = 0; i < 0xd; ++i) {
        v[i] ^= ((i + 0xd) % m);
    }
    v[0xd] = '\0';
}

static inline void fill_values(char v[]) {
    // values
    static unsigned int m = 0;

    if (m == 0) {
        m = 5;
    } else if (m == 7) {
        m = 11;
    }

    v[0x0] = 'w';
    v[0x1] = 'c';
    v[0x2] = 'o';
    v[0x3] = 'q';
    v[0x4] = 'e';
    v[0x5] = 'r';
    for (unsigned int i = 0; i < 0x6; ++i) {
        v[i] ^= ((i + 0x6) % m);
    }
    v[0x6] = '\0';
}

static inline void fill_values_signature(char v[]) {
    // ()Ljava/util/Collection;
    static unsigned int m = 0;

    if (m == 0) {
        m = 23;
    } else if (m == 29) {
        m = 31;
    }

    v[0x0] = ')';
    v[0x1] = '+';
    v[0x2] = 'O';
    v[0x3] = 'n';
    v[0x4] = 'd';
    v[0x5] = 'p';
    v[0x6] = 'f';
    v[0x7] = '\'';
    v[0x8] = '|';
    v[0x9] = '~';
    v[0xa] = 'b';
    v[0xb] = '`';
    v[0xc] = '"';
    v[0xd] = 'M';
    v[0xe] = '`';
    v[0xf] = '|';
    v[0x10] = '}';
    v[0x11] = 'w';
    v[0x12] = 'p';
    v[0x13] = '`';
    v[0x14] = '|';
    v[0x15] = 'y';
    v[0x16] = 'n';
    v[0x17] = ':';
    for (unsigned int i = 0; i < 0x18; ++i) {
        v[i] ^= ((i + 0x18) % m);
    }
    v[0x18] = '\0';
}

static inline void fill_java_util_Collection(char v[]) {
    // java/util/Collection
    static unsigned int m = 0;

    if (m == 0) {
        m = 19;
    } else if (m == 23) {
        m = 29;
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
    v[0xa] = 'H';
    v[0xb] = 'c';
    v[0xc] = 'a';
    v[0xd] = 'b';
    v[0xe] = 'j';
    v[0xf] = 's';
    v[0x10] = 'e';
    v[0x11] = '{';
    v[0x12] = 'o';
    v[0x13] = 'o';
    for (unsigned int i = 0; i < 0x14; ++i) {
        v[i] ^= ((i + 0x14) % m);
    }
    v[0x14] = '\0';
}

static inline void fill_iterator(char v[]) {
    // iterator
    static unsigned int m = 0;

    if (m == 0) {
        m = 7;
    } else if (m == 11) {
        m = 13;
    }

    v[0x0] = 'h';
    v[0x1] = 'v';
    v[0x2] = 'f';
    v[0x3] = 'v';
    v[0x4] = 'd';
    v[0x5] = 'r';
    v[0x6] = 'o';
    v[0x7] = 's';
    for (unsigned int i = 0; i < 0x8; ++i) {
        v[i] ^= ((i + 0x8) % m);
    }
    v[0x8] = '\0';
}

static inline void fill_iterator_signature(char v[]) {
    // ()Ljava/util/Iterator;
    static unsigned int m = 0;

    if (m == 0) {
        m = 19;
    } else if (m == 23) {
        m = 29;
    }

    v[0x0] = '+';
    v[0x1] = '-';
    v[0x2] = 'I';
    v[0x3] = 'l';
    v[0x4] = 'f';
    v[0x5] = '~';
    v[0x6] = 'h';
    v[0x7] = '%';
    v[0x8] = '~';
    v[0x9] = 'x';
    v[0xa] = 'd';
    v[0xb] = 'b';
    v[0xc] = ' ';
    v[0xd] = 'Y';
    v[0xe] = 'e';
    v[0xf] = 'w';
    v[0x10] = 'r';
    v[0x11] = '`';
    v[0x12] = 'v';
    v[0x13] = 'l';
    v[0x14] = 'v';
    v[0x15] = '>';
    for (unsigned int i = 0; i < 0x16; ++i) {
        v[i] ^= ((i + 0x16) % m);
    }
    v[0x16] = '\0';
}

static inline void fill_java_util_Iterator(char v[]) {
    // java/util/Iterator
    static unsigned int m = 0;

    if (m == 0) {
        m = 17;
    } else if (m == 19) {
        m = 23;
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
    v[0xa] = 'B';
    v[0xb] = 'x';
    v[0xc] = 'h';
    v[0xd] = '|';
    v[0xe] = 'n';
    v[0xf] = 'd';
    v[0x10] = 'o';
    v[0x11] = 's';
    for (unsigned int i = 0; i < 0x12; ++i) {
        v[i] ^= ((i + 0x12) % m);
    }
    v[0x12] = '\0';
}

static inline void fill_hasNext(char v[]) {
    // hasNext
    static unsigned int m = 0;

    if (m == 0) {
        m = 5;
    } else if (m == 7) {
        m = 11;
    }

    v[0x0] = 'j';
    v[0x1] = 'b';
    v[0x2] = 'w';
    v[0x3] = 'N';
    v[0x4] = 'd';
    v[0x5] = 'z';
    v[0x6] = 'w';
    for (unsigned int i = 0; i < 0x7; ++i) {
        v[i] ^= ((i + 0x7) % m);
    }
    v[0x7] = '\0';
}

static inline void fill_hasNext_signature(char v[]) {
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

static inline void fill_next(char v[]) {
    // next
    static unsigned int m = 0;

    if (m == 0) {
        m = 3;
    } else if (m == 5) {
        m = 7;
    }

    v[0x0] = 'o';
    v[0x1] = 'g';
    v[0x2] = 'x';
    v[0x3] = 'u';
    for (unsigned int i = 0; i < 0x4; ++i) {
        v[i] ^= ((i + 0x4) % m);
    }
    v[0x4] = '\0';
}

static inline void fill_next_signature(char v[]) {
    // ()Ljava/lang/Object;
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
    v[0xd] = 'A';
    v[0xe] = 'm';
    v[0xf] = 'z';
    v[0x10] = 't';
    v[0x11] = 'q';
    v[0x12] = 't';
    v[0x13] = ':';
    for (unsigned int i = 0; i < 0x14; ++i) {
        v[i] ^= ((i + 0x14) % m);
    }
    v[0x14] = '\0';
}

static inline void fill_signature(char v[]) {
    // [Ljava/lang/Object;
    static unsigned int m = 0;

    if (m == 0) {
        m = 17;
    } else if (m == 19) {
        m = 23;
    }

    v[0x0] = 'Y';
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
    v[0xc] = 'A';
    v[0xd] = 'm';
    v[0xe] = 'z';
    v[0xf] = 'e';
    v[0x10] = 'b';
    v[0x11] = 'v';
    v[0x12] = '8';
    for (unsigned int i = 0; i < 0x13; ++i) {
        v[i] ^= ((i + 0x13) % m);
    }
    v[0x13] = '\0';
}

static inline void fill_java_lang_Object(char v[]) {
    // java/lang/Object
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
    v[0xa] = 'O';
    v[0xb] = 'c';
    v[0xc] = 'h';
    v[0xd] = 'f';
    v[0xe] = 'g';
    v[0xf] = 'q';
    for (unsigned int i = 0; i < 0x10; ++i) {
        v[i] ^= ((i + 0x10) % m);
    }
    v[0x10] = '\0';
}

static jfieldID findMapField(JNIEnv *env, jclass classDexposedBridge) {
    char signature[0x20];

    fill_hookedMethodCallbacks_signature(signature);
    const char *mapName = findStaticMapName(env, classDexposedBridge);
    if (mapName != NULL) {
#ifdef DEBUG
        LOGI("mapName: %s", mapName);
#endif
        return (*env)->GetStaticFieldID(env, classDexposedBridge, mapName, signature);
    } else {
        char name[0x20];
        fill_hookedMethodCallbacks(name); // 0x16
        jfieldID field = (*env)->GetStaticFieldID(env, classDexposedBridge, mapName, signature);
        if ((*env)->ExceptionCheck(env)) {
            (*env)->ExceptionClear(env);
        }
        if (field == NULL) {
            debug(env, "cannot find hookedMethodCallbacks in %s", classDexposedBridge);
        }
        return field;
    }
}

static inline bool doAntiEpicClass(JNIEnv *env, jclass classDexposedBridge) {
    char v1[0x20], v2[0x20];
    bool antied = false;

    jfieldID field = findMapField(env, classDexposedBridge);
    if (field == NULL) {
#ifdef DEBUG
        LOGI("cannot find static map");
#endif
        return false;
    }
    jobject map = (*env)->GetStaticObjectField(env, classDexposedBridge, field);
    debug(env, "hooks: %s", map);

    fill_java_util_Map(v1); // 0xe
    jclass classMap = (*env)->FindClass(env, v1);
    if (map == NULL || !(*env)->IsInstanceOf(env, map, classMap)) {
        return false;
    }
    fill_values(v1); // 0x7
    fill_values_signature(v2); // 0x19
    jmethodID method = (*env)->GetMethodID(env, classMap, v1, v2);
    jobject values = (*env)->CallObjectMethod(env, map, method);

    fill_java_util_Collection(v1); // 0x15
    jclass classCollection = (*env)->FindClass(env, v1);
    fill_iterator(v1); // 0x9
    fill_iterator_signature(v2); // 0x17
    method = (*env)->GetMethodID(env, classCollection, v1, v2);
    jobject iterator = (*env)->CallObjectMethod(env, values, method);

    fill_java_util_Iterator(v1); // 0x13
    jclass classIterator = (*env)->FindClass(env, v1);
    fill_hasNext(v1); // 0x8
    fill_hasNext_signature(v2); // 0x4
    jmethodID hasNext = (*env)->GetMethodID(env, classIterator, v1, v2);
    fill_next(v1); // 0x5
    fill_next_signature(v2); // 0x15
    jmethodID next = (*env)->GetMethodID(env, classIterator, v1, v2);

    fill_java_lang_Object(v1);
    jclass classObject = (*env)->FindClass(env, v1);
    jobject emptyArray = (*env)->NewObjectArray(env, 0, classObject, NULL);
    jfieldID fieldElements = NULL;
    while ((*env)->CallBooleanMethod(env, iterator, hasNext)) {
        jobject hook = (*env)->CallObjectMethod(env, iterator, next);
        debug(env, "hook value: %s", hook);
        if (hook == NULL) {
            continue;
        }
        if (fieldElements == NULL) {
            jobject hookClass = (*env)->GetObjectClass(env, hook);
            char *name = findObjectArrayName(env, hookClass);
            if (name == NULL) {
                (*env)->DeleteLocalRef(env, hookClass);
                break;
            }
            fill_signature(v1);
            fieldElements = (*env)->GetFieldID(env, hookClass, name, v1);
            free(name);
            (*env)->DeleteLocalRef(env, hookClass);
        }
        (*env)->SetObjectField(env, hook, fieldElements, emptyArray);
        if ((*env)->ExceptionCheck(env)) {
            (*env)->ExceptionClear(env);
        } else if (!antied) {
            antied = true;
        }
        (*env)->DeleteLocalRef(env, hook);
    }

    (*env)->DeleteLocalRef(env, classIterator);
    (*env)->DeleteLocalRef(env, iterator);
    (*env)->DeleteLocalRef(env, classCollection);
    (*env)->DeleteLocalRef(env, values);
    (*env)->DeleteLocalRef(env, classMap);
    (*env)->DeleteLocalRef(env, map);

    return antied;
}

static inline void fill_de_robv_android_xposed_DexposedBridge(char v[]) {
    // de/robv/android/xposed/DexposedBridge
    static unsigned int m = 0;

    if (m == 0) {
        m = 31;
    } else if (m == 37) {
        m = 41;
    }

    v[0x0] = 'b';
    v[0x1] = 'b';
    v[0x2] = '\'';
    v[0x3] = '{';
    v[0x4] = 'e';
    v[0x5] = 'i';
    v[0x6] = 'z';
    v[0x7] = '"';
    v[0x8] = 'o';
    v[0x9] = 'a';
    v[0xa] = 't';
    v[0xb] = 'c';
    v[0xc] = '}';
    v[0xd] = 'z';
    v[0xe] = 'p';
    v[0xf] = ':';
    v[0x10] = 'n';
    v[0x11] = 'g';
    v[0x12] = 'w';
    v[0x13] = 'j';
    v[0x14] = '\x7f';
    v[0x15] = '\x7f';
    v[0x16] = '3';
    v[0x17] = 'Y';
    v[0x18] = '{';
    v[0x19] = 'x';
    v[0x1a] = 'q';
    v[0x1b] = 'm';
    v[0x1c] = 'p';
    v[0x1d] = 'a';
    v[0x1e] = 'a';
    v[0x1f] = 'D';
    v[0x20] = 'u';
    v[0x21] = 'a';
    v[0x22] = 'm';
    v[0x23] = 'm';
    v[0x24] = 'n';
    for (unsigned int i = 0; i < 0x25; ++i) {
        v[i] ^= ((i + 0x25) % m);
    }
    v[0x25] = '\0';
}

static bool doAntiEpicCommon(JNIEnv *env, jobject classLoader, const char *name) {
    bool antied = false;
    jclass classDexposedBridge = findLoadedClass(env, classLoader, name);
    if (classDexposedBridge != NULL) {
#ifdef DEBUG
        LOGI("found %s", name);
#endif
        antied = doAntiEpicClass(env, (jclass) classDexposedBridge);
        (*env)->DeleteLocalRef(env, classDexposedBridge);
    }
    return antied;
}

bool doAntiEpic(JNIEnv *env, jobject classLoader) {
    char v1[0x80];
    bool antied = false;

    debug(env, "doAntiEpic, classLoader: %s", classLoader);

    fill_me_weishu_epic_art2_EpicNative(v1);
    jclass epicNative = findLoadedClass(env, classLoader, v1);
    if (epicNative != NULL) {
        char *bridgeClass = findVoidStringName(env, epicNative);
#ifdef DEBUG
        LOGI("bridge class: %s", bridgeClass);
#endif
        if (bridgeClass != NULL) {
            antied |= doAntiEpicCommon(env, classLoader, bridgeClass);
            free(bridgeClass);
        }
        (*env)->DeleteLocalRef(env, epicNative);
    }
    if (!antied) {
        fill_me_weishu_epic_art2_EpicBridge(v1);
        antied |= doAntiEpicCommon(env, classLoader, v1);
    }
    if (!antied) {
        fill_de_robv_android_xposed_DexposedBridge(v1);
        antied |= doAntiEpicCommon(env, classLoader, v1);
    }
    if (antied) {
        clearHandler(env, getSdk());
    }

    return antied;
}

#endif

static inline void fill_java_lang_Thread(char v[]) {
    // java/lang/Thread
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
    v[0xa] = 'T';
    v[0xb] = 'i';
    v[0xc] = 'p';
    v[0xd] = 'f';
    v[0xe] = 'e';
    v[0xf] = 'a';
    for (unsigned int i = 0; i < 0x10; ++i) {
        v[i] ^= ((i + 0x10) % m);
    }
    v[0x10] = '\0';
}

static inline void fill_setUncaughtExceptionPreHandler(char v[]) {
    // setUncaughtExceptionPreHandler
    static unsigned int m = 0;

    if (m == 0) {
        m = 29;
    } else if (m == 31) {
        m = 37;
    }

    v[0x0] = 'r';
    v[0x1] = 'g';
    v[0x2] = 'w';
    v[0x3] = 'Q';
    v[0x4] = 'k';
    v[0x5] = 'e';
    v[0x6] = 'f';
    v[0x7] = '}';
    v[0x8] = 'n';
    v[0x9] = 'b';
    v[0xa] = '\x7f';
    v[0xb] = 'I';
    v[0xc] = 'u';
    v[0xd] = 'm';
    v[0xe] = 'j';
    v[0xf] = '`';
    v[0x10] = 'e';
    v[0x11] = '{';
    v[0x12] = '|';
    v[0x13] = 'z';
    v[0x14] = 'E';
    v[0x15] = 'd';
    v[0x16] = 'r';
    v[0x17] = 'P';
    v[0x18] = 'x';
    v[0x19] = 't';
    v[0x1a] = '\x7f';
    v[0x1b] = 'p';
    v[0x1c] = 'e';
    v[0x1d] = 's';
    for (unsigned int i = 0; i < 0x1e; ++i) {
        v[i] ^= ((i + 0x1e) % m);
    }
    v[0x1e] = '\0';
}

static inline void fill_setUncaughtExceptionHandler_signature(char v[]) {
    // (Ljava/lang/Thread$UncaughtExceptionHandler;)V
    static unsigned int m = 0;

    if (m == 0) {
        m = 43;
    } else if (m == 47) {
        m = 53;
    }

    v[0x0] = '+';
    v[0x1] = 'H';
    v[0x2] = 'o';
    v[0x3] = 'g';
    v[0x4] = 'q';
    v[0x5] = 'i';
    v[0x6] = '&';
    v[0x7] = 'f';
    v[0x8] = 'j';
    v[0x9] = 'b';
    v[0xa] = 'j';
    v[0xb] = '!';
    v[0xc] = '[';
    v[0xd] = 'x';
    v[0xe] = 'c';
    v[0xf] = 'w';
    v[0x10] = 'r';
    v[0x11] = 'p';
    v[0x12] = '1';
    v[0x13] = 'C';
    v[0x14] = 'y';
    v[0x15] = '{';
    v[0x16] = 'x';
    v[0x17] = 'o';
    v[0x18] = '|';
    v[0x19] = 't';
    v[0x1a] = 'i';
    v[0x1b] = '[';
    v[0x1c] = 'g';
    v[0x1d] = 'C';
    v[0x1e] = 'D';
    v[0x1f] = 'R';
    v[0x20] = 'W';
    v[0x21] = 'M';
    v[0x22] = 'J';
    v[0x23] = 'H';
    v[0x24] = 'o';
    v[0x25] = 'I';
    v[0x26] = 'G';
    v[0x27] = 'N';
    v[0x28] = 'l';
    v[0x29] = 'd';
    v[0x2a] = 'p';
    v[0x2b] = '8';
    v[0x2c] = '-';
    v[0x2d] = 'S';
    for (unsigned int i = 0; i < 0x2e; ++i) {
        v[i] ^= ((i + 0x2e) % m);
    }
    v[0x2e] = '\0';
}

void clearHandler(JNIEnv *env, int sdk) {
    char v1[0x1f], v2[0x2f];
    if (sdk < 26) {
        return;
    }
    fill_java_lang_Thread(v1); // 0x10 + 1
    jclass clazz = (*env)->FindClass(env, v1);
    fill_setUncaughtExceptionPreHandler(v1); // 0x1e + 1
    fill_setUncaughtExceptionHandler_signature(v2); // 0x2e + 1
    jmethodID method = (*env)->GetStaticMethodID(env, clazz, v1, v2);
    if (method == NULL) {
        (*env)->ExceptionClear(env);
    } else {
        (*env)->CallStaticVoidMethod(env, clazz, method, NULL);
    }
    (*env)->DeleteLocalRef(env, clazz);
}
