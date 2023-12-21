//
// Created by Thom on 2019/2/16.
//

#include <jni.h>
#include <fcntl.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include "plt.h"
#include "classloader.h"
#include "common.h"
#include "anti-xposed.h"
#include "epic.h"
#include "art.h"
#include "hash.h"
#include "dex-path-list.h"
#include "inline.h"
#include "xposed-nop.h"

jmethodID methodObjectToString;
jmethodID methodClassGetClassLoader;
jmethodID methodClassGetName;
jmethodID methodClassLoaderLoadClass;
jmethodID methodClassIsArray;

static jclass xposedClassXcMethodHook;
static jobject xposedNoHookGlobal;
static uint64_t xposedNoHookO;
static bool xposedNoHookOSet;

#ifndef NO_CHECK_XPOSED
static void inline fill_NewLocalRef(char v[]) {
    // _ZN3art9JNIEnvExt11NewLocalRefEPNS_6mirror6ObjectE
    static unsigned int m = 0;

    if (m == 0) {
        m = 47;
    } else if (m == 53) {
        m = 59;
    }

    v[0x0] = '\\';
    v[0x1] = '^';
    v[0x2] = 'K';
    v[0x3] = '5';
    v[0x4] = 'f';
    v[0x5] = 'z';
    v[0x6] = '}';
    v[0x7] = '3';
    v[0x8] = 'A';
    v[0x9] = 'B';
    v[0xa] = 'D';
    v[0xb] = 'K';
    v[0xc] = 'a';
    v[0xd] = 'f';
    v[0xe] = 'T';
    v[0xf] = 'j';
    v[0x10] = 'g';
    v[0x11] = '%';
    v[0x12] = '$';
    v[0x13] = 'X';
    v[0x14] = 'r';
    v[0x15] = 'o';
    v[0x16] = 'U';
    v[0x17] = 'u';
    v[0x18] = 'x';
    v[0x19] = '}';
    v[0x1a] = 'q';
    v[0x1b] = 'L';
    v[0x1c] = 'z';
    v[0x1d] = 'F';
    v[0x1e] = 'd';
    v[0x1f] = 'r';
    v[0x20] = 'm';
    v[0x21] = 'w';
    v[0x22] = 'z';
    v[0x23] = '\x10';
    v[0x24] = 'J';
    v[0x25] = 'A';
    v[0x26] = '[';
    v[0x27] = 'X';
    v[0x28] = 'D';
    v[0x29] = '^';
    v[0x2a] = '\x1b';
    v[0x2b] = 'a';
    v[0x2c] = 'b';
    v[0x2d] = 'k';
    v[0x2e] = 'g';
    v[0x2f] = '`';
    v[0x30] = 'p';
    v[0x31] = '@';
    for (unsigned int i = 0; i < 0x32; ++i) {
        v[i] ^= ((i + 0x32) % m);
    }
    v[0x32] = '\0';
}

static inline jobject newLocalRef(JNIEnv *env, void *object) {
    static jobject (*NewLocalRef)(JNIEnv *, void *) = nullptr;
    if (object == nullptr) {
        return nullptr;
    }
    if (NewLocalRef == nullptr) {
        char v[0x40];
        fill_NewLocalRef(v);
        NewLocalRef = (jobject (*)(JNIEnv *, void *)) plt_dlsym(v, nullptr);
#ifdef DEBUG
        LOGI("NewLocalRef: %p", NewLocalRef);
#endif
    }
    if (NewLocalRef != nullptr) {
        return NewLocalRef(env, object);
    } else {
        return nullptr;
    }
}

static inline void fill_DeleteLocalRef(char v[]) {
    // _ZN3art9JNIEnvExt14DeleteLocalRefEP8_jobject
    static unsigned int m = 0;

    if (m == 0) {
        m = 43;
    } else if (m == 47) {
        m = 53;
    }

    v[0x0] = '^';
    v[0x1] = 'X';
    v[0x2] = 'M';
    v[0x3] = '7';
    v[0x4] = 'd';
    v[0x5] = 't';
    v[0x6] = 's';
    v[0x7] = '1';
    v[0x8] = 'C';
    v[0x9] = 'D';
    v[0xa] = 'B';
    v[0xb] = 'I';
    v[0xc] = 'c';
    v[0xd] = 'x';
    v[0xe] = 'J';
    v[0xf] = 'h';
    v[0x10] = 'e';
    v[0x11] = '#';
    v[0x12] = '\'';
    v[0x13] = 'P';
    v[0x14] = 'p';
    v[0x15] = 'z';
    v[0x16] = 'r';
    v[0x17] = 'l';
    v[0x18] = '|';
    v[0x19] = 'V';
    v[0x1a] = 't';
    v[0x1b] = '\x7f';
    v[0x1c] = '|';
    v[0x1d] = 'r';
    v[0x1e] = 'M';
    v[0x1f] = 'E';
    v[0x20] = 'G';
    v[0x21] = 'g';
    v[0x22] = 's';
    v[0x23] = '\x1c';
    v[0x24] = 'z';
    v[0x25] = 'L';
    v[0x26] = 'H';
    v[0x27] = 'J';
    v[0x28] = 'C';
    v[0x29] = 'O';
    v[0x2a] = 'c';
    v[0x2b] = 'u';
    for (unsigned int i = 0; i < 0x2c; ++i) {
        v[i] ^= ((i + 0x2c) % m);
    }
    v[0x2c] = '\0';
}

static void DeleteLocalRef(JNIEnv *env, jobject object) {
    static void (*DeleteLocalRef)(JNIEnv *, jobject) = nullptr;
    if (DeleteLocalRef == nullptr) {
        char v[0x30];
        fill_DeleteLocalRef(v);
        DeleteLocalRef = (void (*)(JNIEnv *, jobject)) plt_dlsym(v, nullptr);
#ifdef DEBUG
        LOGI("DeleteLocalRef: %p", DeleteLocalRef);
#endif
    }
    if (DeleteLocalRef != nullptr) {
        DeleteLocalRef(env, object);
    }
}

static void doAntiXposed(JNIEnv *env, jobject object) {
    jclass classXposedBridge = findXposedBridge(env, object);
    if (classXposedBridge == nullptr) {
        return;
    }
#ifdef DEBUG
    LOGI("doAntiXposed, classLoader: %p", object);
#endif
    disableXposedBridge(env, classXposedBridge);
#ifdef CHECK_XPOSED_EPIC
    if (doAntiEpic(env, object)) {
#ifdef DEBUG
        LOGI("antied epic");
#endif
    }
#endif
}

static inline void fill_dalvik_system_BaseDexClassLoader(char v[]) {
    // dalvik/system/BaseDexClassLoader
    static unsigned int m = 0;

    if (m == 0) {
        m = 31;
    } else if (m == 37) {
        m = 41;
    }

    v[0x0] = 'e';
    v[0x1] = 'c';
    v[0x2] = 'o';
    v[0x3] = 'r';
    v[0x4] = 'l';
    v[0x5] = 'm';
    v[0x6] = '(';
    v[0x7] = '{';
    v[0x8] = 'p';
    v[0x9] = 'y';
    v[0xa] = '\x7f';
    v[0xb] = 'i';
    v[0xc] = '`';
    v[0xd] = '!';
    v[0xe] = 'M';
    v[0xf] = 'q';
    v[0x10] = 'b';
    v[0x11] = 'w';
    v[0x12] = 'W';
    v[0x13] = 'q';
    v[0x14] = 'm';
    v[0x15] = 'U';
    v[0x16] = '{';
    v[0x17] = 'y';
    v[0x18] = 'j';
    v[0x19] = 'i';
    v[0x1a] = 'W';
    v[0x1b] = 's';
    v[0x1c] = '|';
    v[0x1d] = 'z';
    v[0x1e] = 'e';
    v[0x1f] = 's';
    for (unsigned int i = 0; i < 0x20; ++i) {
        v[i] ^= ((i + 0x20) % m);
    }
    v[0x20] = '\0';
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

#define MAX_CLASS_LOADER 100
static jobject checkedClassLoaders[MAX_CLASS_LOADER];
static int abnormalClassLoaders[MAX_CLASS_LOADER];

static void emptyAbnormalClassLoader(JNIEnv *env) {
    for (int i = 0; i < MAX_CLASS_LOADER; ++i) {
        jobject checkedClassLoader = checkedClassLoaders[i];
        if (checkedClassLoader != nullptr) {
            env->DeleteLocalRef(checkedClassLoader);
            checkedClassLoaders[i] = nullptr;
        } else {
            break;
        }
    }
}

#define CLASSLOADER_ABNORMAL      0x1
#define CLASSLOADER_IN_MEMORY_DEX 0x2
static int isAbnormalClassLoader(JNIEnv *env, jclass baseDexClassLoader, jobject classLoader, bool *hasInMemoryDex) {
    for (int i = 0; i < MAX_CLASS_LOADER; ++i) {
        jobject checkedClassLoader = checkedClassLoaders[i];
        if (checkedClassLoader == nullptr) {
            checkedClassLoaders[i] = env->NewLocalRef(classLoader);
            bool abnormal = hasAbnormalClassLoader(env, baseDexClassLoader, classLoader, hasInMemoryDex);
            int value = 0;
            if (abnormal) {
                value |= CLASSLOADER_ABNORMAL;
            }
            if (hasInMemoryDex && *hasInMemoryDex) {
                value |= CLASSLOADER_IN_MEMORY_DEX;
            }
            abnormalClassLoaders[i] = value;
            return abnormal;
        } else if (env->IsSameObject(checkedClassLoader, classLoader)) {
            int value = abnormalClassLoaders[i];
            bool abnormal = (value & CLASSLOADER_ABNORMAL) == CLASSLOADER_ABNORMAL;
            if (hasInMemoryDex) {
                *hasInMemoryDex = (value & CLASSLOADER_IN_MEMORY_DEX) == CLASSLOADER_IN_MEMORY_DEX;
            }
            return abnormal;
        }
    }
    return hasAbnormalClassLoader(env, baseDexClassLoader, classLoader, hasInMemoryDex);
}

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

static inline void fill_getClassLoader(char v[]) {
    // getClassLoader
    static unsigned int m = 0;

    if (m == 0) {
        m = 13;
    } else if (m == 17) {
        m = 19;
    }

    v[0x0] = 'f';
    v[0x1] = 'g';
    v[0x2] = 'w';
    v[0x3] = 'G';
    v[0x4] = 'i';
    v[0x5] = 'g';
    v[0x6] = 't';
    v[0x7] = '{';
    v[0x8] = 'E';
    v[0x9] = 'e';
    v[0xa] = 'j';
    v[0xb] = 'h';
    v[0xc] = 'e';
    v[0xd] = 's';
    for (unsigned int i = 0; i < 0xe; ++i) {
        v[i] ^= ((i + 0xe) % m);
    }
    v[0xe] = '\0';
}

static inline void fill_getClassLoader_signature(char v[]) {
    // ()Ljava/lang/ClassLoader;
    static unsigned int m = 0;

    if (m == 0) {
        m = 23;
    } else if (m == 29) {
        m = 31;
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
    v[0xf] = 'p';
    v[0x10] = 'a';
    v[0x11] = '`';
    v[0x12] = 'X';
    v[0x13] = 'z';
    v[0x14] = 'w';
    v[0x15] = 'd';
    v[0x16] = 'd';
    v[0x17] = 'p';
    v[0x18] = '8';
    for (unsigned int i = 0; i < 0x19; ++i) {
        v[i] ^= ((i + 0x19) % m);
    }
    v[0x19] = '\0';
}

static inline void fill_XC_MethodHook(char v[]) {
    // .XC_MethodHook
    static unsigned int m = 0;

    if (m == 0) {
        m = 13;
    } else if (m == 17) {
        m = 19;
    }

    v[0x0] = '/';
    v[0x1] = 'Z';
    v[0x2] = '@';
    v[0x3] = '[';
    v[0x4] = 'H';
    v[0x5] = 'c';
    v[0x6] = 's';
    v[0x7] = '`';
    v[0x8] = 'f';
    v[0x9] = 'n';
    v[0xa] = 'C';
    v[0xb] = 'c';
    v[0xc] = 'o';
    v[0xd] = 'j';
    for (unsigned int i = 0; i < 0xe; ++i) {
        v[i] ^= ((i + 0xe) % m);
    }
    v[0xe] = '\0';
}

static inline void fill_nop(char v[]) {
    // nop
    static unsigned int m = 0;

    if (m == 0) {
        m = 2;
    } else if (m == 3) {
        m = 5;
    }

    v[0x0] = 'o';
    v[0x1] = 'o';
    v[0x2] = 'q';
    for (unsigned int i = 0; i < 0x3; ++i) {
        v[i] ^= ((i + 0x3) % m);
    }
    v[0x3] = '\0';
}

static bool checkXcMethodHook(JNIEnv *env, jobject object) {
    bool result = false;
    if (object == nullptr) {
        return false;
    }
    jclass objectClass = env->GetObjectClass(object);
    if (objectClass == nullptr) {
        return false;
    }
    jclass objectSuperClass = env->GetSuperclass(objectClass);
    if (objectSuperClass != nullptr) {
        auto objectSuperClassNameAsString = (jstring) env->CallObjectMethod(objectSuperClass, methodClassGetName);
        const char *objectSuperClassNameAsChars = env->GetStringUTFChars(objectSuperClassNameAsString, nullptr);
        char *objectSuperClassName = strdup(objectSuperClassNameAsChars);
        env->ReleaseStringUTFChars(objectSuperClassNameAsString, objectSuperClassNameAsChars);
        env->DeleteLocalRef(objectSuperClassNameAsString);
        char *lastDot = strrchr(objectSuperClassName, '.');
        char xposedSuffix[0x20];
        fill_XC_MethodHook(xposedSuffix);
        if (strcmp(lastDot, xposedSuffix) == 0) {
            result = true;
            if (xposedClassXcMethodHook == nullptr) {
                xposedClassXcMethodHook = objectSuperClass;
                objectSuperClass = nullptr;
            }
        }
        free(objectSuperClassName);
    }
    if (objectSuperClass != nullptr) {
        env->DeleteLocalRef(objectSuperClass);
    }
    env->DeleteLocalRef(objectClass);
    return result;
}

static bool isXcMethodHook(JNIEnv *env, jclass baseDexClassLoader, jobject object) {
    bool result = false;
    jobject classLoader;
    if (object == nullptr) {
        return false;
    }
    jclass objectClass = env->GetObjectClass(object);
    if (objectClass == nullptr) {
        return false;
    }
#if defined(DEBUG_XPOSED)
    if (xposedNoHookGlobal == nullptr) {
        logObject(env, "object: %s", object);
        jclass objectSuperClass = env->GetSuperclass(objectClass);
        logObject(env, "object super class: %s", objectSuperClass);
        env->DeleteLocalRef(objectSuperClass);
    }
#endif
    classLoader = env->CallObjectMethod(objectClass, methodClassGetClassLoader);
    if (classLoader == nullptr) {
        goto cleanObjectClass;
    }
    if (env->IsInstanceOf(classLoader, baseDexClassLoader)) {
        bool hasInMemoryDex;
        bool abnormal = isAbnormalClassLoader(env, baseDexClassLoader, classLoader, &hasInMemoryDex);
#if defined(DEBUG_XPOSED)
        if (xposedNoHookGlobal == nullptr) {
            LOGI("abnormal: %d, hasInMemoryDex: %d", abnormal, hasInMemoryDex);
        }
#endif
        if (abnormal || hasInMemoryDex) {
            result = checkXcMethodHook(env, object);
        }
    }
    env->DeleteLocalRef(classLoader);
cleanObjectClass:
    if (object != objectClass) {
        env->DeleteLocalRef(objectClass);
    }
    return result;
}

static bool checkAbnormalObject(JNIEnv *env, jclass baseDexClassLoader, jobject object, bool *hooked) {
    jsize size;
    bool result = false;
    *hooked = false;
    if (object == nullptr) {
        return false;
    }
    jclass objectClass = env->GetObjectClass(object);
    if (objectClass == nullptr) {
        return false;
    }
    if (!env->CallBooleanMethod(objectClass, methodClassIsArray)) {
        if (isXcMethodHook(env, baseDexClassLoader, object)) {
            *hooked = true;
        }
        goto clean;
    }
    size = env->GetArrayLength((jobjectArray) object);
#ifdef DEBUG
    LOGI("size: %d", size);
#endif
    if (size == 1) {
        jobject element0 = env->GetObjectArrayElement((jobjectArray) object, 0);
        if (element0 == nullptr) {
            goto clean;
        }
        jclass objectClass0 = env->GetObjectClass(element0);
        debug(env, "object[0] class: %s", objectClass0);
        if (env->CallBooleanMethod(objectClass0, methodClassIsArray)) {
            jsize size0 = env->GetArrayLength((jobjectArray) element0);
#ifdef DEBUG
            LOGI("object[0] size: %d", size0);
#endif
            if (size0 <= 0) {
                goto clean;
            }
            for (int i = 0; i < size0; ++i) {
                jobject subElement = env->GetObjectArrayElement((jobjectArray) element0, i);
                if (subElement == nullptr) {
                    continue;
                }
                if (isXcMethodHook(env, baseDexClassLoader, subElement)) {
                    result = true;
                }
                env->DeleteLocalRef(subElement);
                if (result) {
                    break;
                }
            }
            if (result && xposedNoHookGlobal != nullptr) {
                for (int i = 0; i < size0; ++i) {
                    env->SetObjectArrayElement((jobjectArray) element0, i, xposedNoHookGlobal);
                }
            }
        }
    }
clean:
    if (result && xposedNoHookGlobal != nullptr) {
        char v[0x40];
        fill_java_lang_Object(v);
        jclass classObject = env->FindClass(v);
        env->SetObjectArrayElement((jobjectArray) object, 0, env->NewObjectArray(0, classObject, nullptr));
        env->DeleteLocalRef(classObject);
    }
    env->DeleteLocalRef(objectClass);
    return result;
}

class PathClassLoaderVisitor : public art::SingleRootVisitor {
public:
    PathClassLoaderVisitor(JNIEnv *env, jclass classLoader) : _env(env), _classLoader(classLoader) {
        _hasAbnormalClassLoader = false;
    }

    void VisitRoot(art::mirror::Object *root, const art::RootInfo &info ATTRIBUTE_UNUSED) {
#ifdef DEBUG
        LOGI("VisitRoot, root: %p", root);
#endif
        bool hooked = false;
        jobject object = newLocalRef(_env, root);
        if (object == nullptr) {
            return;
        }
        if (!_env->IsInstanceOf(object, _classLoader)) {
            if (xposedNoHookO && memcmp(root, &xposedNoHookO, 8) == 0) {
                return;
            }
            checkAbnormalObject(_env, _classLoader, object, &hooked);
            if (hooked) {
                if (xposedNoHookO == 0 && xposedNoHookGlobal != nullptr && _env->IsSameObject(object, xposedNoHookGlobal)) {
                    memcpy(&xposedNoHookO, root, 8);
#ifdef DEBUG_XPOSED
                    LOGI("root: %p, xposedNoHookO: %08x" ,root, xposedNoHookO);
#endif
                    xposedNoHookOSet = true;
                } else if (xposedNoHookO && memcmp(root, &xposedNoHookO, 8)) {
                    logObject(_env, "replace %s as NoHook", object);
                    memcpy(root, &xposedNoHookO, 8);
                }
            }
        } else if (add((intptr_t) root)) {
            debug(_env, "visit global object, instance: %s", object);
            bool hasInMemoryDex;
            if (isAbnormalClassLoader(_env, _classLoader, object, &hasInMemoryDex)) {
                _hasAbnormalClassLoader = true;
            }
            if (!hasInMemoryDex) {
                doAntiXposed(_env, object);
            }
        }
        DeleteLocalRef(_env, object);
    }

    bool isAbnormal() {
        return _hasAbnormalClassLoader || xposedClassXcMethodHook != nullptr;
    }

private:
    JNIEnv *_env;
    jclass _classLoader;
    bool _hasAbnormalClassLoader;
};

static inline void fill_VisitRoots(char v[]) {
    // _ZN3art9JavaVMExt10VisitRootsEPNS_11RootVisitorE
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
    v[0x7] = '1';
    v[0x8] = 'C';
    v[0x9] = 'k';
    v[0xa] = '}';
    v[0xb] = 'm';
    v[0xc] = '[';
    v[0xd] = 'C';
    v[0xe] = 'J';
    v[0xf] = 'h';
    v[0x10] = 'e';
    v[0x11] = '#';
    v[0x12] = '#';
    v[0x13] = 'B';
    v[0x14] = '|';
    v[0x15] = 'e';
    v[0x16] = '~';
    v[0x17] = 'l';
    v[0x18] = 'K';
    v[0x19] = 'u';
    v[0x1a] = 't';
    v[0x1b] = 'h';
    v[0x1c] = 'n';
    v[0x1d] = '[';
    v[0x1e] = 'O';
    v[0x1f] = 'n';
    v[0x20] = 'r';
    v[0x21] = '}';
    v[0x22] = '\x12';
    v[0x23] = '\x15';
    v[0x24] = 'w';
    v[0x25] = 'I';
    v[0x26] = 'H';
    v[0x27] = '\\';
    v[0x28] = '\x7f';
    v[0x29] = 'C';
    v[0x2a] = 'X';
    v[0x2b] = 'E';
    v[0x2c] = 'Y';
    v[0x2d] = 'A';
    v[0x2e] = 'r';
    v[0x2f] = 'D';
    for (unsigned int i = 0; i < 0x30; ++i) {
        v[i] ^= ((i + 0x30) % m);
    }
    v[0x30] = '\0';
}

static void setupXposedNoHookGlobal(JNIEnv* env) {
    jobject noHook = nullptr;
    if (xposedClassXcMethodHook != nullptr) {
        noHook = xposedNop(env, xposedClassXcMethodHook);
        env->DeleteLocalRef(xposedClassXcMethodHook);
    }
    if (noHook == nullptr) {
        char v[0x10];
        fill_nop(v);
        jclass classNoHook = env->FindClass(v);
#ifdef DEBUG_XPOSED
        logObject(env, "NoHook: %s", classNoHook);
#endif
        noHook = env->AllocObject(classNoHook);
        env->DeleteLocalRef(classNoHook);
    }
    xposedNoHookGlobal = env->NewGlobalRef(noHook);
    env->DeleteLocalRef(noHook);
#ifdef DEBUG_XPOSED
    logObject(env, "xposedNoHookGlobal: %s", xposedNoHookGlobal);
#endif
}

static bool checkGlobalRef(JNIEnv *env, jclass clazz, int *genuine) {
    char v[0x40];
    fill_VisitRoots(v);
    auto VisitRoots = (void (*)(void *, void *)) plt_dlsym(v, nullptr);
#ifdef DEBUG
    LOGI("VisitRoots: %p", VisitRoots);
#endif
    if (VisitRoots == nullptr) {
        return false;
    }
    if (isPltHooked(v, true)
        || (setRead((void *) VisitRoots) && isInlineHooked((void *) VisitRoots))) {
        if (genuine) {
            *genuine = CHECK_FATAL;
        }
    }
    JavaVM *jvm;
    env->GetJavaVM(&jvm);
    bool abnormal = false;
    xposedClassXcMethodHook = nullptr;
    xposedNoHookO = 0;
    xposedNoHookOSet = false;
    for (int i = 0; i < 0x3; ++i) {
        PathClassLoaderVisitor visitor(env, clazz);
        VisitRoots(jvm, &visitor);
        abnormal |= visitor.isAbnormal();
        if (!abnormal) {
            break;
        }
        if (xposedNoHookO) {
            if (xposedNoHookOSet) {
                xposedNoHookOSet = false;
            } else {
                break;
            }
        } else if (xposedNoHookGlobal == nullptr) {
            setupXposedNoHookGlobal(env);
        }
    }
    return abnormal;
}

static inline void fill_SweepJniWeakGlobals(char v[]) {
    // _ZN3art9JavaVMExt19SweepJniWeakGlobalsEPNS_15IsMarkedVisitorE
    static unsigned int m = 0;

    if (m == 0) {
        m = 59;
    } else if (m == 61) {
        m = 67;
    }

    v[0x0] = ']';
    v[0x1] = 'Y';
    v[0x2] = 'J';
    v[0x3] = '6';
    v[0x4] = 'g';
    v[0x5] = 'u';
    v[0x6] = '|';
    v[0x7] = '0';
    v[0x8] = '@';
    v[0x9] = 'j';
    v[0xa] = 'z';
    v[0xb] = 'l';
    v[0xc] = 'X';
    v[0xd] = 'B';
    v[0xe] = 'U';
    v[0xf] = 'i';
    v[0x10] = 'f';
    v[0x11] = '"';
    v[0x12] = '-';
    v[0x13] = 'F';
    v[0x14] = 'a';
    v[0x15] = 'r';
    v[0x16] = '}';
    v[0x17] = 'i';
    v[0x18] = 'P';
    v[0x19] = 'u';
    v[0x1a] = 'u';
    v[0x1b] = 'J';
    v[0x1c] = '{';
    v[0x1d] = '~';
    v[0x1e] = 'K';
    v[0x1f] = 'f';
    v[0x20] = 'N';
    v[0x21] = 'L';
    v[0x22] = 'F';
    v[0x23] = 'D';
    v[0x24] = 'J';
    v[0x25] = 'T';
    v[0x26] = 'm';
    v[0x27] = 'y';
    v[0x28] = 'd';
    v[0x29] = 'x';
    v[0x2a] = 's';
    v[0x2b] = '\x1c';
    v[0x2c] = '\x1b';
    v[0x2d] = 'f';
    v[0x2e] = 'C';
    v[0x2f] = '|';
    v[0x30] = 'S';
    v[0x31] = 'A';
    v[0x32] = '_';
    v[0x33] = 'P';
    v[0x34] = 'R';
    v[0x35] = 'a';
    v[0x36] = 'Q';
    v[0x37] = 'J';
    v[0x38] = 'S';
    v[0x39] = 't';
    v[0x3a] = 'n';
    v[0x3b] = 'p';
    v[0x3c] = 'F';
    for (unsigned int i = 0; i < 0x3d; ++i) {
        v[i] ^= ((i + 0x3d) % m);
    }
    v[0x3d] = '\0';
}

class WeakClassLoaderVisitor : public art::IsMarkedVisitor {
public :
    WeakClassLoaderVisitor(JNIEnv *env, jclass classLoader) : _env(env), _classLoader(classLoader) {
        _hasAbnormalClassLoader = false;
    }

    art::mirror::Object *IsMarked(art::mirror::Object *obj) override {
        jobject object = newLocalRef(_env, obj);
        if (object != nullptr) {
            if (!_env->IsInstanceOf(object, _classLoader)) {
                bool hooked;
                checkAbnormalObject(_env, _classLoader, object, &hooked);
            } else if (add((intptr_t) obj)) {
                debug(_env, "visit weak root, instance: %s", object);
                bool hasInMemoryDex;
                if (isAbnormalClassLoader(_env, _classLoader, object, &hasInMemoryDex)) {
                    _hasAbnormalClassLoader = true;
                }
                if (!hasInMemoryDex) {
                    doAntiXposed(_env, object);
                }
            }
            DeleteLocalRef(_env, object);
        }
        return obj;
    }

    bool isAbnormal() {
        return _hasAbnormalClassLoader;
    }

private:
    JNIEnv *_env;
    jclass _classLoader;
    bool _hasAbnormalClassLoader;
};

static bool checkWeakGlobalRef(JNIEnv *env, jclass clazz) {
    char v[0x40];
    fill_SweepJniWeakGlobals(v);
    auto SweepJniWeakGlobals = (void (*)(void *, void *)) plt_dlsym(v, nullptr);
#ifdef DEBUG
    LOGI("SweepJniWeakGlobals: %p", SweepJniWeakGlobals);
#endif
    if (SweepJniWeakGlobals == nullptr) {
        return false;
    }
    JavaVM *jvm;
    env->GetJavaVM(&jvm);
    WeakClassLoaderVisitor visitor(env, clazz);
    SweepJniWeakGlobals(jvm, &visitor);
    return visitor.isAbnormal();
}

static inline void fill_isArray(char v[]) {
    // isArray
    static unsigned int m = 0;

    if (m == 0) {
        m = 5;
    } else if (m == 7) {
        m = 11;
    }

    v[0x0] = 'k';
    v[0x1] = 'p';
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

static inline void fill___Z(char v[]) {
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

static inline void fill_void_string_signature(char *v) {
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

static inline void fill_java_lang_ClassLoader(char v[]) {
    // java/lang/ClassLoader
    static unsigned int m = 0;

    if (m == 0) {
        m = 19;
    } else if (m == 23) {
        m = 29;
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
    v[0xb] = 'a';
    v[0xc] = 'o';
    v[0xd] = '|';
    v[0xe] = 'c';
    v[0xf] = ']';
    v[0x10] = '}';
    v[0x11] = 'a';
    v[0x12] = 'e';
    v[0x13] = 'g';
    v[0x14] = 'q';
    for (unsigned int i = 0; i < 0x15; ++i) {
        v[i] ^= ((i + 0x15) % m);
    }
    v[0x15] = '\0';
}

static inline void fill_loadClass(char v[]) {
    // loadClass
    static unsigned int m = 0;

    if (m == 0) {
        m = 7;
    } else if (m == 11) {
        m = 13;
    }

    v[0x0] = 'n';
    v[0x1] = 'l';
    v[0x2] = 'e';
    v[0x3] = 'a';
    v[0x4] = 'E';
    v[0x5] = 'l';
    v[0x6] = '`';
    v[0x7] = 'q';
    v[0x8] = 'p';
    for (unsigned int i = 0; i < 0x9; ++i) {
        v[i] ^= ((i + 0x9) % m);
    }
    v[0x9] = '\0';
}

static inline void fill_loadClass_signature(char v[]) {
    // (Ljava/lang/String;)Ljava/lang/Class;
    static unsigned int m = 0;

    if (m == 0) {
        m = 31;
    } else if (m == 37) {
        m = 41;
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
    v[0xc] = 'A';
    v[0xd] = 'g';
    v[0xe] = 'f';
    v[0xf] = '|';
    v[0x10] = 'x';
    v[0x11] = 'p';
    v[0x12] = '#';
    v[0x13] = '0';
    v[0x14] = 'V';
    v[0x15] = 'q';
    v[0x16] = '}';
    v[0x17] = 'k';
    v[0x18] = '\x7f';
    v[0x19] = '/';
    v[0x1a] = 'm';
    v[0x1b] = 'c';
    v[0x1c] = 'm';
    v[0x1d] = 'c';
    v[0x1e] = '*';
    v[0x1f] = 'E';
    v[0x20] = 'k';
    v[0x21] = 'i';
    v[0x22] = 'z';
    v[0x23] = 'y';
    v[0x24] = '0';
    for (unsigned int i = 0; i < 0x25; ++i) {
        v[i] ^= ((i + 0x25) % m);
    }
    v[0x25] = '\0';
}

static void initStaticMethods(JNIEnv *env) {
    char v[0x40], v2[0x40];
    if (methodClassLoaderLoadClass != nullptr) {
        return;
    }
    fill_java_lang_Object(v);
    jclass classObject = env->FindClass(v);
    // Object.toString
    fill_toString(v);
    fill_void_string_signature(v2);
    methodObjectToString = env->GetMethodID(classObject, v, v2);
    env->DeleteLocalRef(classObject);

    fill_java_lang_Class(v);
    jclass classClass = env->FindClass(v);
    // Class.isArray
    fill_isArray(v);
    fill___Z(v2);
    methodClassIsArray = env->GetMethodID(classClass, v, v2);
    // Class.getClassLoader
    fill_getClassLoader(v);
    fill_getClassLoader_signature(v2);
    methodClassGetClassLoader = env->GetMethodID(classClass, v, v2);
    // Class.getName
    fill_getName(v);
    fill_void_string_signature(v2);
    methodClassGetName = env->GetMethodID(classClass, v, v2);
    env->DeleteLocalRef(classClass);

    fill_java_lang_ClassLoader(v);
    jclass classClassLoader = env->FindClass(v);
    // ClassLoader.loadClass
    fill_loadClass(v);
    fill_loadClass_signature(v2);
    methodClassLoaderLoadClass = env->GetMethodID(classClassLoader, v, v2);
    env->DeleteLocalRef(classClassLoader);
}

void logObject(JNIEnv *env, const char *format, jobject object) {
    if (object == nullptr) {
        LOGI(format, nullptr);
    } else {
        initStaticMethods(env);
        auto objectAsString = (jstring) env->CallObjectMethod(object, methodObjectToString);
        const char *objectAsChars = env->GetStringUTFChars(objectAsString, nullptr);
        LOGI(format, objectAsChars);
        env->ReleaseStringUTFChars(objectAsString, objectAsChars);
        env->DeleteLocalRef(objectAsString);
    }
}

bool checkClassLoader(JNIEnv *env, int sdk, int *genuine) {
    if (sdk < 21) {
        return false;
    }

    char v[0x40];
    fill_dalvik_system_BaseDexClassLoader(v);
    jclass clazz = env->FindClass(v);
    if (env->ExceptionCheck()) {
#ifdef DEBUG
        env->ExceptionDescribe();
#endif
        env->ExceptionClear();
    }
    if (clazz == nullptr) {
        return false;
    }
    debug(env, "BaseDexClassLoader: %s", clazz);

    initStaticMethods(env);

    bool abnormal = checkGlobalRef(env, clazz, genuine);
    abnormal |= checkWeakGlobalRef(env, clazz);
    emptyAbnormalClassLoader(env);

    clear();
    env->DeleteLocalRef(clazz);

    return abnormal;
}
#endif
