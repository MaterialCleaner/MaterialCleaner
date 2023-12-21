//
// Created by Thom on 2019/3/20.
//

#include <stdbool.h>
#include <sys/types.h>
#include <unistd.h>
#include <pthread.h>
#include <jni.h>
#include <string.h>
#include <malloc.h>
#include <stdlib.h>
#include "common.h"
#include "handle-error.h"

static pthread_t tid;
static volatile bool started;

static inline void fill_android_app_NativeActivity(char v[]) {
    // android.app.NativeActivity
    static unsigned int m = 0;

    if (m == 0) {
        m = 23;
    } else if (m == 29) {
        m = 31;
    }

    v[0x0] = 'b';
    v[0x1] = 'j';
    v[0x2] = 'a';
    v[0x3] = 't';
    v[0x4] = 'h';
    v[0x5] = 'a';
    v[0x6] = 'm';
    v[0x7] = '$';
    v[0x8] = 'j';
    v[0x9] = '|';
    v[0xa] = '}';
    v[0xb] = ' ';
    v[0xc] = 'A';
    v[0xd] = 'q';
    v[0xe] = 'e';
    v[0xf] = '{';
    v[0x10] = 'e';
    v[0x11] = 'q';
    v[0x12] = 'T';
    v[0x13] = 'u';
    v[0x14] = 't';
    v[0x15] = 'h';
    v[0x16] = 't';
    v[0x17] = 'j';
    v[0x18] = 'p';
    v[0x19] = '|';
    for (unsigned int i = 0; i < 0x1a; ++i) {
        v[i] ^= ((i + 0x1a) % m);
    }
    v[0x1a] = '\0';
}

static inline void fill_android_content_Intent(char v[]) {
    // android/content/Intent
    static unsigned int m = 0;

    if (m == 0) {
        m = 19;
    } else if (m == 23) {
        m = 29;
    }

    v[0x0] = 'b';
    v[0x1] = 'j';
    v[0x2] = 'a';
    v[0x3] = 't';
    v[0x4] = 'h';
    v[0x5] = 'a';
    v[0x6] = 'm';
    v[0x7] = '%';
    v[0x8] = 'h';
    v[0x9] = 'c';
    v[0xa] = 'c';
    v[0xb] = 'z';
    v[0xc] = 'j';
    v[0xd] = '~';
    v[0xe] = 'e';
    v[0xf] = '=';
    v[0x10] = 'I';
    v[0x11] = 'o';
    v[0x12] = 'v';
    v[0x13] = 'f';
    v[0x14] = 'j';
    v[0x15] = 'q';
    for (unsigned int i = 0; i < 0x16; ++i) {
        v[i] ^= ((i + 0x16) % m);
    }
    v[0x16] = '\0';
}

static inline void fill_android_app_ActivityThread(char v[]) {
    // android/app/ActivityThread
    static unsigned int m = 0;

    if (m == 0) {
        m = 23;
    } else if (m == 29) {
        m = 31;
    }

    v[0x0] = 'b';
    v[0x1] = 'j';
    v[0x2] = 'a';
    v[0x3] = 't';
    v[0x4] = 'h';
    v[0x5] = 'a';
    v[0x6] = 'm';
    v[0x7] = '%';
    v[0x8] = 'j';
    v[0x9] = '|';
    v[0xa] = '}';
    v[0xb] = '!';
    v[0xc] = 'N';
    v[0xd] = 's';
    v[0xe] = 'e';
    v[0xf] = '{';
    v[0x10] = 'e';
    v[0x11] = '}';
    v[0x12] = 'a';
    v[0x13] = 'o';
    v[0x14] = 'T';
    v[0x15] = 'i';
    v[0x16] = 'p';
    v[0x17] = 'f';
    v[0x18] = 'e';
    v[0x19] = 'a';
    for (unsigned int i = 0; i < 0x1a; ++i) {
        v[i] ^= ((i + 0x1a) % m);
    }
    v[0x1a] = '\0';
}

static inline void fill_currentApplication(char v[]) {
    // currentApplication
    static unsigned int m = 0;

    if (m == 0) {
        m = 17;
    } else if (m == 19) {
        m = 23;
    }

    v[0x0] = 'b';
    v[0x1] = 'w';
    v[0x2] = 'q';
    v[0x3] = 'v';
    v[0x4] = '`';
    v[0x5] = 'h';
    v[0x6] = 's';
    v[0x7] = 'I';
    v[0x8] = 'y';
    v[0x9] = 'z';
    v[0xa] = 'g';
    v[0xb] = 'e';
    v[0xc] = 'n';
    v[0xd] = 'o';
    v[0xe] = '{';
    v[0xf] = 'y';
    v[0x10] = 'o';
    v[0x11] = 'o';
    for (unsigned int i = 0; i < 0x12; ++i) {
        v[i] ^= ((i + 0x12) % m);
    }
    v[0x12] = '\0';
}

static inline void fill_currentApplication_signature(char v[]) {
    // ()Landroid/app/Application;
    static unsigned int m = 0;

    if (m == 0) {
        m = 23;
    } else if (m == 29) {
        m = 31;
    }

    v[0x0] = ',';
    v[0x1] = ',';
    v[0x2] = 'J';
    v[0x3] = 'f';
    v[0x4] = 'f';
    v[0x5] = 'm';
    v[0x6] = 'x';
    v[0x7] = 'd';
    v[0x8] = 'e';
    v[0x9] = 'i';
    v[0xa] = '!';
    v[0xb] = 'n';
    v[0xc] = '`';
    v[0xd] = 'a';
    v[0xe] = '=';
    v[0xf] = 'R';
    v[0x10] = 'd';
    v[0x11] = 'e';
    v[0x12] = 'z';
    v[0x13] = 'i';
    v[0x14] = 'b';
    v[0x15] = 'c';
    v[0x16] = 'w';
    v[0x17] = 'm';
    v[0x18] = 'j';
    v[0x19] = 'h';
    v[0x1a] = '<';
    for (unsigned int i = 0; i < 0x1b; ++i) {
        v[i] ^= ((i + 0x1b) % m);
    }
    v[0x1b] = '\0';
}

static jobject getApplication(JNIEnv *env) {
    // Landroid/app/ActivityThread;->currentApplication()Landroid/app/Application;
    char v1[0x20], v2[0x20];
    jobject application = NULL;

    fill_android_app_ActivityThread(v2); // 0x16 + 1
    jclass classActivityThread = (*env)->FindClass(env, v2);
    if (classActivityThread == NULL) {
#ifdef DEBUG_GENUINE
        LOGW("cannot find ActivityThread");
#endif
        goto clean;
    }

    fill_currentApplication(v1); // 0x12
    fill_currentApplication_signature(v2); // 0x1c
    jmethodID method = (*env)->GetStaticMethodID(env, classActivityThread, v1, v2);
    if (method == NULL) {
#ifdef DEBUG_GENUINE
        LOGW("cannot find ActivityThread.currentApplication");
#endif
        goto cleanClassActivityThread;
    }

    application = (*env)->CallStaticObjectMethod(env, classActivityThread, method);
    if (application == NULL) {
        goto cleanClassActivityThread;
    }

cleanClassActivityThread:
    (*env)->DeleteLocalRef(env, classActivityThread);
clean:
    return application;
}

static inline void fill_init(char v[]) {
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

static inline void fill_void_signature(char v[]) {
    // ()V
    static unsigned int m = 0;

    if (m == 0) {
        m = 2;
    } else if (m == 3) {
        m = 5;
    }

    v[0x0] = ')';
    v[0x1] = ')';
    v[0x2] = 'W';
    for (unsigned int i = 0; i < 0x3; ++i) {
        v[i] ^= ((i + 0x3) % m);
    }
    v[0x3] = '\0';
}

static inline void fill_setClassName(char v[]) {
    // setClassName
    static unsigned int m = 0;

    if (m == 0) {
        m = 11;
    } else if (m == 13) {
        m = 17;
    }

    v[0x0] = 'r';
    v[0x1] = 'g';
    v[0x2] = 'w';
    v[0x3] = 'G';
    v[0x4] = 'i';
    v[0x5] = 'g';
    v[0x6] = 't';
    v[0x7] = '{';
    v[0x8] = 'G';
    v[0x9] = 'k';
    v[0xa] = 'm';
    v[0xb] = 'd';
    for (unsigned int i = 0; i < 0xc; ++i) {
        v[i] ^= ((i + 0xc) % m);
    }
    v[0xc] = '\0';
}

static inline void fill_setClassName_signature(char v[]) {
    // (Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;
    static unsigned int m = 0;

    if (m == 0) {
        m = 61;
    } else if (m == 67) {
        m = 71;
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
    v[0xc] = '^';
    v[0xd] = 'z';
    v[0xe] = '}';
    v[0xf] = 'y';
    v[0x10] = '\x7f';
    v[0x11] = 'u';
    v[0x12] = '(';
    v[0x13] = 'X';
    v[0x14] = '\x7f';
    v[0x15] = 'w';
    v[0x16] = 'a';
    v[0x17] = 'y';
    v[0x18] = '6';
    v[0x19] = 'v';
    v[0x1a] = 'z';
    v[0x1b] = 'r';
    v[0x1c] = 'z';
    v[0x1d] = '1';
    v[0x1e] = 'L';
    v[0x1f] = 'T';
    v[0x20] = 'S';
    v[0x21] = 'K';
    v[0x22] = 'M';
    v[0x23] = 'C';
    v[0x24] = '\x1e';
    v[0x25] = '\x0f';
    v[0x26] = 'k';
    v[0x27] = 'I';
    v[0x28] = 'G';
    v[0x29] = 'N';
    v[0x2a] = 'Y';
    v[0x2b] = 'C';
    v[0x2c] = 'D';
    v[0x2d] = 'J';
    v[0x2e] = '\x00';
    v[0x2f] = 'S';
    v[0x30] = '^';
    v[0x31] = '\\';
    v[0x32] = 'G';
    v[0x33] = 'Q';
    v[0x34] = '[';
    v[0x35] = 'B';
    v[0x36] = '\x18';
    v[0x37] = 'q';
    v[0x38] = 'W';
    v[0x39] = 'N';
    v[0x3a] = '^';
    v[0x3b] = 'R';
    v[0x3c] = 't';
    v[0x3d] = ':';
    for (unsigned int i = 0; i < 0x3e; ++i) {
        v[i] ^= ((i + 0x3e) % m);
    }
    v[0x3e] = '\0';
}

static inline void fill_setFlags(char v[]) {
    // setFlags
    static unsigned int m = 0;

    if (m == 0) {
        m = 7;
    } else if (m == 11) {
        m = 13;
    }

    v[0x0] = 'r';
    v[0x1] = 'g';
    v[0x2] = 'w';
    v[0x3] = 'B';
    v[0x4] = 'i';
    v[0x5] = 'g';
    v[0x6] = 'g';
    v[0x7] = 'r';
    for (unsigned int i = 0; i < 0x8; ++i) {
        v[i] ^= ((i + 0x8) % m);
    }
    v[0x8] = '\0';
}

static inline void fill_setFlags_signature(char v[]) {
    // (I)Landroid/content/Intent;
    static unsigned int m = 0;

    if (m == 0) {
        m = 23;
    } else if (m == 29) {
        m = 31;
    }

    v[0x0] = ',';
    v[0x1] = 'L';
    v[0x2] = '/';
    v[0x3] = 'K';
    v[0x4] = 'i';
    v[0x5] = 'g';
    v[0x6] = 'n';
    v[0x7] = 'y';
    v[0x8] = 'c';
    v[0x9] = 'd';
    v[0xa] = 'j';
    v[0xb] = ' ';
    v[0xc] = 's';
    v[0xd] = '~';
    v[0xe] = '|';
    v[0xf] = 'g';
    v[0x10] = 'q';
    v[0x11] = '{';
    v[0x12] = 'b';
    v[0x13] = '/';
    v[0x14] = 'H';
    v[0x15] = 'l';
    v[0x16] = 'w';
    v[0x17] = 'a';
    v[0x18] = 'k';
    v[0x19] = 'r';
    v[0x1a] = '<';
    for (unsigned int i = 0; i < 0x1b; ++i) {
        v[i] ^= ((i + 0x1b) % m);
    }
    v[0x1b] = '\0';
}

static jobject getIntent(JNIEnv *env) {
    char v1[0x10], v2[0x40];

    fill_android_content_Intent(v2); // 0x20
    jclass classIntent = (*env)->FindClass(env, v2);

    fill_init(v1);
    fill_void_signature(v2);
    jmethodID init = (*env)->GetMethodID(env, classIntent, v1, v2);


    fill_setClassName(v1);
    fill_setClassName_signature(v2); // 0x40
    jmethodID setClassName = (*env)->GetMethodID(env, classIntent, v1, v2);

    fill_setFlags(v1);
    fill_setFlags_signature(v2); // 0x20
    jmethodID setFlags = (*env)->GetMethodID(env, classIntent, v1, v2);

    jobject intent = (*env)->NewObject(env, classIntent, init);
    char *genuinePackageName = getGenuinePackageName();
    jstring packageName = (*env)->NewStringUTF(env, genuinePackageName);

    fill_android_app_NativeActivity(v2); // 0x20
    jstring className = (*env)->NewStringUTF(env, v2);
    (*env)->CallObjectMethod(env, intent, setClassName, packageName, className);

#define FLAG_ACTIVITY_NEW_TASK      (0x10000000u)
#define FLAG_ACTIVITY_CLEAR_TOP     (0x04000000u)
#define FLAG_ACTIVITY_SINGLE_TOP    (0x20000000u)
#define FLAGS (FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP)

    (*env)->CallObjectMethod(env, intent, setFlags, FLAGS);

    (*env)->DeleteLocalRef(env, className);
    (*env)->DeleteLocalRef(env, packageName);
#ifdef GENUINE_NAME
    free(genuinePackageName);
#endif
    (*env)->DeleteLocalRef(env, classIntent);

    return intent;
}

static inline void fill_cannot_getApplication(char v[]) {
    // cannot getApplication
    static unsigned int m = 0;

    if (m == 0) {
        m = 19;
    } else if (m == 23) {
        m = 29;
    }

    v[0x0] = 'a';
    v[0x1] = 'b';
    v[0x2] = 'j';
    v[0x3] = 'k';
    v[0x4] = 'i';
    v[0x5] = 's';
    v[0x6] = '(';
    v[0x7] = 'n';
    v[0x8] = 'o';
    v[0x9] = '\x7f';
    v[0xa] = 'M';
    v[0xb] = '}';
    v[0xc] = '~';
    v[0xd] = 'c';
    v[0xe] = 'y';
    v[0xf] = 'r';
    v[0x10] = 's';
    v[0x11] = 't';
    v[0x12] = 'h';
    v[0x13] = 'm';
    v[0x14] = 'm';
    for (unsigned int i = 0; i < 0x15; ++i) {
        v[i] ^= ((i + 0x15) % m);
    }
    v[0x15] = '\0';
}

static inline void fill_cannot_startActivity(char v[]) {
    // cannot startActivity
    static unsigned int m = 0;

    if (m == 0) {
        m = 19;
    } else if (m == 23) {
        m = 29;
    }

    v[0x0] = 'b';
    v[0x1] = 'c';
    v[0x2] = 'm';
    v[0x3] = 'j';
    v[0x4] = 'j';
    v[0x5] = 'r';
    v[0x6] = '\'';
    v[0x7] = '{';
    v[0x8] = '}';
    v[0x9] = 'k';
    v[0xa] = 'y';
    v[0xb] = 'x';
    v[0xc] = 'L';
    v[0xd] = 'm';
    v[0xe] = '{';
    v[0xf] = 'y';
    v[0x10] = 'g';
    v[0x11] = '{';
    v[0x12] = 't';
    v[0x13] = 'x';
    for (unsigned int i = 0; i < 0x14; ++i) {
        v[i] ^= ((i + 0x14) % m);
    }
    v[0x14] = '\0';
}

static inline void fill_startActivity(char v[]) {
    // startActivity
    static unsigned int m = 0;

    if (m == 0) {
        m = 11;
    } else if (m == 13) {
        m = 17;
    }

    v[0x0] = 'q';
    v[0x1] = 'w';
    v[0x2] = 'e';
    v[0x3] = 'w';
    v[0x4] = 'r';
    v[0x5] = 'F';
    v[0x6] = 'k';
    v[0x7] = '}';
    v[0x8] = 'c';
    v[0x9] = 'v';
    v[0xa] = 'h';
    v[0xb] = 'v';
    v[0xc] = 'z';
    for (unsigned int i = 0; i < 0xd; ++i) {
        v[i] ^= ((i + 0xd) % m);
    }
    v[0xd] = '\0';
}

static inline void fill_startActivity_signature(char v[]) {
    // (Landroid/content/Intent;)V
    static unsigned int m = 0;

    if (m == 0) {
        m = 23;
    } else if (m == 29) {
        m = 31;
    }

    v[0x0] = ',';
    v[0x1] = 'I';
    v[0x2] = 'g';
    v[0x3] = 'i';
    v[0x4] = 'l';
    v[0x5] = '{';
    v[0x6] = 'e';
    v[0x7] = 'b';
    v[0x8] = 'h';
    v[0x9] = '"';
    v[0xa] = 'm';
    v[0xb] = '`';
    v[0xc] = '~';
    v[0xd] = 'e';
    v[0xe] = 'w';
    v[0xf] = '}';
    v[0x10] = '`';
    v[0x11] = ':';
    v[0x12] = '_';
    v[0x13] = 'n';
    v[0x14] = 'u';
    v[0x15] = 'g';
    v[0x16] = 'm';
    v[0x17] = 'p';
    v[0x18] = '>';
    v[0x19] = '/';
    v[0x1a] = 'Q';
    for (unsigned int i = 0; i < 0x1b; ++i) {
        v[i] ^= ((i + 0x1b) % m);
    }
    v[0x1b] = '\0';
}

static inline void fill_native_activity_cannot_be_started_in_15s(char v[]) {
    // native activity cannot be started in 15s
    static unsigned int m = 0;

    if (m == 0) {
        m = 37;
    } else if (m == 41) {
        m = 43;
    }

    v[0x0] = 'm';
    v[0x1] = 'e';
    v[0x2] = 'q';
    v[0x3] = 'o';
    v[0x4] = 'q';
    v[0x5] = 'm';
    v[0x6] = ')';
    v[0x7] = 'k';
    v[0x8] = 'h';
    v[0x9] = 'x';
    v[0xa] = 'd';
    v[0xb] = 'x';
    v[0xc] = 'f';
    v[0xd] = 'd';
    v[0xe] = 'h';
    v[0xf] = '2';
    v[0x10] = 'p';
    v[0x11] = 'u';
    v[0x12] = '{';
    v[0x13] = 'x';
    v[0x14] = 'x';
    v[0x15] = 'l';
    v[0x16] = '9';
    v[0x17] = 'x';
    v[0x18] = '~';
    v[0x19] = '<';
    v[0x1a] = 'n';
    v[0x1b] = 'j';
    v[0x1c] = '~';
    v[0x1d] = 'R';
    v[0x1e] = 'U';
    v[0x1f] = 'G';
    v[0x20] = 'G';
    v[0x21] = '\x04';
    v[0x22] = 'i';
    v[0x23] = 'o';
    v[0x24] = '"';
    v[0x25] = '2';
    v[0x26] = '1';
    v[0x27] = 'v';
    for (unsigned int i = 0; i < 0x28; ++i) {
        v[i] ^= ((i + 0x28) % m);
    }
    v[0x28] = '\0';
}

void start_native_activity(JavaVM *jvm) {
    char v1[0x10], v2[0x30];
    JNIEnv *env;
#ifdef DEBUG_GENUINE
    LOGI("start_route: %p", jvm);
#endif
    jobject application;
    (*jvm)->AttachCurrentThread(jvm, &env, NULL);
    do {
        usleep(10000);
        application = getApplication(env);
        if ((*env)->ExceptionCheck(env)) {
            (*env)->ExceptionDescribe(env);
            (*env)->ExceptionClear(env);
            fill_cannot_getApplication(v2);
            LOGE(v2);
            _exit(0);
        }
    } while (application == NULL);

    jclass clazz = (*env)->GetObjectClass(env, application);
    fill_startActivity(v1);
    fill_startActivity_signature(v2);
    jmethodID startActivity = (*env)->GetMethodID(env, clazz, v1, v2);

    jobject intent = getIntent(env);
    for (int i = 0; i < 300; ++i) {
        (*env)->CallVoidMethod(env, application, startActivity, intent);
        if ((*env)->ExceptionCheck(env)) {
            (*env)->ExceptionDescribe(env);
            (*env)->ExceptionClear(env);
            fill_cannot_startActivity(v2);
            LOGE(v2);
            _exit(0);
        }
#ifdef DEBUG_GENUINE
        LOGI("called startActivity");
#endif
        usleep(50000);
        if (started) {
            break;
        }
    }
    (*env)->DeleteLocalRef(env, intent);
    (*env)->DeleteLocalRef(env, clazz);
    (*env)->DeleteLocalRef(env, application);
    (*jvm)->DetachCurrentThread(jvm);
    if (!started) {
        fill_native_activity_cannot_be_started_in_15s(v2);
        LOGE(v2);
        _exit(0);
    }
}

static void *start_route(void *arg) {
    start_native_activity((JavaVM *) arg);
    return arg;
}

void start_native_activity_async(JNIEnv *env) {
    JavaVM *jvm;
#if __ANDROID_API__ < 24
    if (getSdk() < 24 && !has_native_libs()) {
        return;
    }
#endif
    (*env)->GetJavaVM(env, &jvm);
    started = false;
    pthread_create(&tid, NULL, start_route, jvm);
}

void set_started() {
#ifdef DEBUG_GENUINE
    LOGI("started: true");
#endif
    started = true;
}