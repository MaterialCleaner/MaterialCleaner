//
// Created by Thom on 2019/3/20.
//

#include <android/native_activity.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <android/log.h>
#include <android/hardware_buffer.h>
#include <android/hardware_buffer_jni.h>
#include <android/surface_texture.h>
#include <android/surface_texture_jni.h>
#include <android/native_window_jni.h>
#include <android/bitmap.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <pthread.h>
#include "common.h"
#include "handle-error.h"
#include "bitmap.h"

static int repeat;

static inline int fill_unsupported_genuine_x(char v[]) {
// unsupported_genuine_?
    static unsigned int m = 0;

    if (m == 0) {
        m = 19;
    } else if (m == 23) {
        m = 29;
    }

    v[0x0] = 'w';
    v[0x1] = 'm';
    v[0x2] = 'w';
    v[0x3] = 'p';
    v[0x4] = 'v';
    v[0x5] = 'w';
    v[0x6] = 'g';
    v[0x7] = '{';
    v[0x8] = '~';
    v[0x9] = 'n';
    v[0xa] = 'h';
    v[0xb] = 'R';
    v[0xc] = 'i';
    v[0xd] = 'j';
    v[0xe] = '~';
    v[0xf] = 'd';
    v[0x10] = '{';
    v[0x11] = 'n';
    v[0x12] = 'd';
    v[0x13] = ']';
    v[0x14] = '<';
    for (unsigned int i = 0; i < 0x15; ++i) {
        v[i] ^= ((i + 0x15) % m);
    }
    v[0x15] = '\0';
    return 0x15;
}

static char asChar(int genuine) {
    if (genuine >= 0 && genuine < 10) {
        return (char) ('0' + genuine);
    } else if (genuine >= 10 && genuine < 16) {
        return (char) ('a' + genuine - 10);
    } else {
        return '\0';
    }
}

static inline void fill_getResources(char v[]) {
    // getResources
    static unsigned int m = 0;

    if (m == 0) {
        m = 11;
    } else if (m == 13) {
        m = 17;
    }

    v[0x0] = 'f';
    v[0x1] = 'g';
    v[0x2] = 'w';
    v[0x3] = 'V';
    v[0x4] = '`';
    v[0x5] = 'u';
    v[0x6] = 'h';
    v[0x7] = '}';
    v[0x8] = '{';
    v[0x9] = 'i';
    v[0xa] = 'e';
    v[0xb] = 'r';
    for (unsigned int i = 0; i < 0xc; ++i) {
        v[i] ^= ((i + 0xc) % m);
    }
    v[0xc] = '\0';
}

static inline void fill_getResources_signature(char v[]) {
    // ()Landroid/content/res/Resources;
    static unsigned int m = 0;

    if (m == 0) {
        m = 31;
    } else if (m == 37) {
        m = 41;
    }

    v[0x0] = '*';
    v[0x1] = '*';
    v[0x2] = 'H';
    v[0x3] = 'd';
    v[0x4] = 'h';
    v[0x5] = 'c';
    v[0x6] = 'z';
    v[0x7] = 'f';
    v[0x8] = 'c';
    v[0x9] = 'o';
    v[0xa] = '#';
    v[0xb] = 'n';
    v[0xc] = 'a';
    v[0xd] = 'a';
    v[0xe] = 'd';
    v[0xf] = 't';
    v[0x10] = '|';
    v[0x11] = 'g';
    v[0x12] = ';';
    v[0x13] = 'g';
    v[0x14] = 's';
    v[0x15] = 'd';
    v[0x16] = '7';
    v[0x17] = 'K';
    v[0x18] = '\x7f';
    v[0x19] = 'h';
    v[0x1a] = 's';
    v[0x1b] = 'h';
    v[0x1c] = 'l';
    v[0x1d] = 'c';
    v[0x1e] = 'd';
    v[0x1f] = 'q';
    v[0x20] = '8';
    for (unsigned int i = 0; i < 0x21; ++i) {
        v[i] ^= ((i + 0x21) % m);
    }
    v[0x21] = '\0';
}

static inline void fill_getIdentifier(char v[]) {
    // getIdentifier
    static unsigned int m = 0;

    if (m == 0) {
        m = 11;
    } else if (m == 13) {
        m = 17;
    }

    v[0x0] = 'e';
    v[0x1] = 'f';
    v[0x2] = 'p';
    v[0x3] = 'L';
    v[0x4] = 'b';
    v[0x5] = 'b';
    v[0x6] = 'f';
    v[0x7] = '}';
    v[0x8] = 'c';
    v[0x9] = 'f';
    v[0xa] = 'h';
    v[0xb] = 'g';
    v[0xc] = 'q';
    for (unsigned int i = 0; i < 0xd; ++i) {
        v[i] ^= ((i + 0xd) % m);
    }
    v[0xd] = '\0';
}

static inline void fill_getIdentifier_signature(char v[]) {
    // (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)I
    static unsigned int m = 0;

    if (m == 0) {
        m = 53;
    } else if (m == 59) {
        m = 61;
    }

    v[0x0] = ',';
    v[0x1] = 'I';
    v[0x2] = 'l';
    v[0x3] = 'f';
    v[0x4] = '~';
    v[0x5] = 'h';
    v[0x6] = '%';
    v[0x7] = 'g';
    v[0x8] = 'm';
    v[0x9] = 'c';
    v[0xa] = 'i';
    v[0xb] = ' ';
    v[0xc] = 'C';
    v[0xd] = 'e';
    v[0xe] = '`';
    v[0xf] = 'z';
    v[0x10] = 'z';
    v[0x11] = 'r';
    v[0x12] = '-';
    v[0x13] = '[';
    v[0x14] = 'r';
    v[0x15] = 'x';
    v[0x16] = 'l';
    v[0x17] = 'z';
    v[0x18] = '3';
    v[0x19] = 'q';
    v[0x1a] = '\x7f';
    v[0x1b] = 'q';
    v[0x1c] = 'G';
    v[0x1d] = '\x0e';
    v[0x1e] = 'q';
    v[0x1f] = 'W';
    v[0x20] = 'V';
    v[0x21] = 'L';
    v[0x22] = 'H';
    v[0x23] = '@';
    v[0x24] = '\x13';
    v[0x25] = 'e';
    v[0x26] = '@';
    v[0x27] = 'J';
    v[0x28] = 'Z';
    v[0x29] = 'L';
    v[0x2a] = '\x01';
    v[0x2b] = 'C';
    v[0x2c] = 'Q';
    v[0x2d] = '_';
    v[0x2e] = 'U';
    v[0x2f] = '\x1c';
    v[0x30] = 'g';
    v[0x31] = 't';
    v[0x32] = 's';
    v[0x33] = 'k';
    v[0x34] = 'm';
    v[0x35] = 'c';
    v[0x36] = '>';
    v[0x37] = '/';
    v[0x38] = 'N';
    for (unsigned int i = 0; i < 0x39; ++i) {
        v[i] ^= ((i + 0x39) % m);
    }
    v[0x39] = '\0';
}

static inline void fill_getString(char v[]) {
    // getString
    static unsigned int m = 0;

    if (m == 0) {
        m = 7;
    } else if (m == 11) {
        m = 13;
    }

    v[0x0] = 'e';
    v[0x1] = 'f';
    v[0x2] = 'p';
    v[0x3] = 'V';
    v[0x4] = 'r';
    v[0x5] = 'r';
    v[0x6] = 'h';
    v[0x7] = 'l';
    v[0x8] = 'd';
    for (unsigned int i = 0; i < 0x9; ++i) {
        v[i] ^= ((i + 0x9) % m);
    }
    v[0x9] = '\0';
}

static inline void fill_getString_signature(char v[]) {
    // (I)Ljava/lang/String;
    static unsigned int m = 0;

    if (m == 0) {
        m = 19;
    } else if (m == 23) {
        m = 29;
    }

    v[0x0] = '*';
    v[0x1] = 'J';
    v[0x2] = '-';
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
    v[0xe] = 'C';
    v[0xf] = 'e';
    v[0x10] = '`';
    v[0x11] = 'i';
    v[0x12] = 'o';
    v[0x13] = 'e';
    v[0x14] = '8';
    for (unsigned int i = 0; i < 0x15; ++i) {
        v[i] ^= ((i + 0x15) % m);
    }
    v[0x15] = '\0';
}

static inline void fill_string(char v[]) {
    // string
    static unsigned int m = 0;

    if (m == 0) {
        m = 5;
    } else if (m == 7) {
        m = 11;
    }

    v[0x0] = 'r';
    v[0x1] = 'v';
    v[0x2] = 'q';
    v[0x3] = 'm';
    v[0x4] = 'n';
    v[0x5] = 'f';
    for (unsigned int i = 0; i < 0x6; ++i) {
        v[i] ^= ((i + 0x6) % m);
    }
    v[0x6] = '\0';
}


static jobject getLabel(JNIEnv *env, jobject activity) {
    char v1[0x10], v2[0x40];

    jclass classActivity = (*env)->GetObjectClass(env, activity);

    fill_getResources(v1);
    fill_getResources_signature(v2);
    jmethodID getResources = (*env)->GetMethodID(env, classActivity, v1, v2);

    jobject resources = (*env)->CallObjectMethod(env, activity, getResources);

    jclass classResources = (*env)->GetObjectClass(env, resources);

    fill_getIdentifier(v1);
    fill_getIdentifier_signature(v2);
    jmethodID getIdentifier = (*env)->GetMethodID(env, classResources, v1, v2);

    fill_getString(v1);
    fill_getString_signature(v2);
    jmethodID getString = (*env)->GetMethodID(env, classResources, v1, v2);

    int length = fill_unsupported_genuine_x(v2);
    v2[length - 1] = asChar(getGenuine());
#if defined(DEBUG) || defined(DEBUG_GENUINE)
    LOGI("v: %s", v2);
#endif
    jstring name = (*env)->NewStringUTF(env, v2);

    fill_string(v1);
    jstring stringType = (*env)->NewStringUTF(env, v1);
    char *packageName = getGenuinePackageName();
    jstring stringPackageName = (*env)->NewStringUTF(env, packageName);
    int label = (*env)->CallIntMethod(env, resources, getIdentifier, name, stringType,
                                      stringPackageName);

    jstring string = (*env)->CallObjectMethod(env, resources, getString, label);

    (*env)->DeleteLocalRef(env, stringPackageName);
    (*env)->DeleteLocalRef(env, stringType);
    (*env)->DeleteLocalRef(env, name);
    (*env)->DeleteLocalRef(env, classResources);
    (*env)->DeleteLocalRef(env, resources);
    (*env)->DeleteLocalRef(env, classActivity);
    free(packageName);

    return string;
}

static void onNativeWindowCreated(ANativeActivity *activity, ANativeWindow *window) {
#ifdef DEBUG_GENUINE
    LOGI("onNativeWindowCreated start %p %p", activity, window);
    LOGI("window, format: %d, width: %d, height: %d", ANativeWindow_getFormat(window), ANativeWindow_getWidth(window), ANativeWindow_getHeight(window));
#endif

#define FORMAT AHARDWAREBUFFER_FORMAT_R8G8B8A8_UNORM
    if (ANativeWindow_getFormat(window) != FORMAT) {
        ANativeWindow_setBuffersGeometry(window, ANativeWindow_getWidth(window), ANativeWindow_getHeight(window), FORMAT);
    }

    ANativeWindow_Buffer buffer = {0};
    ANativeWindow_lock(window, &buffer, NULL);

#ifdef DEBUG_GENUINE
    LOGI("buffer, format: %d, width: %d, height: %d, stride: %d",
         buffer.format, buffer.width, buffer.height, buffer.stride);
#endif
    if (buffer.format != FORMAT) {
        ANativeWindow_unlockAndPost(window);
        return;
    }

    uint32_t *bits = buffer.bits;
    for (int i = 0; i < buffer.height; ++i) {
        memset(bits, 0, buffer.width * sizeof(uint32_t));
        bits += buffer.stride;
    }

    JNIEnv *env = activity->env;
    jstring label = getLabel(env, activity->clazz);
    jobject bitmap = asBitmap(env, (int) (buffer.width * 0.618), label);

    void *pixels;
    AndroidBitmapInfo info;
    AndroidBitmap_getInfo(env, bitmap, &info);
    AndroidBitmap_lockPixels(env, bitmap, &pixels);

    uint32_t *dst = buffer.bits;
    uint32_t *src = pixels;
    size_t top = (buffer.height - info.height) / 2;
    size_t left = (buffer.width - info.width) / 2;
    dst += buffer.stride * top;
    for (size_t i = 0; i < info.height; ++i) {
        dst += left;
        memcpy(dst, src, info.width * sizeof(uint32_t));
        src += info.width;
        dst += (buffer.stride - left);
    }

    AndroidBitmap_unlockPixels(env, bitmap);
    ANativeWindow_unlockAndPost(window);
#ifdef DEBUG_GENUINE
    LOGI("onNativeWindowCreated complete %p %p", activity, window);
#endif
}

static void onStart(ANativeActivity *activity __unused) {
#ifdef DEBUG_GENUINE
    LOGI("onStart %p", activity);
#endif
    set_started();
}

static void onStop(ANativeActivity *activity __unused) {
#ifdef DEBUG_GENUINE
    LOGI("onStop %p", activity);
#endif
    kill(getpid(), SIGTERM);
    _exit(0);
}

static void onResume(ANativeActivity *activity __unused) {
#ifdef DEBUG_GENUINE
    LOGI("onResume %p", activity);
#endif
}

static void onDestroy(ANativeActivity *activity __unused) {
#ifdef DEBUG_GENUINE
    LOGI("onDestroy %p", activity);
#endif
}

#ifdef DEBUG
static const char *action_name(const AInputEvent *event) {
    int action = AKeyEvent_getAction(event);
    switch (action) {
        case AKEY_EVENT_ACTION_DOWN:
            return "down";
        case AKEY_EVENT_ACTION_UP:
            return "up";
        case AKEY_EVENT_ACTION_MULTIPLE:
            return "multiple";
        default:
            return "unknown";
    }
}
#endif

static int handleEvent(AInputEvent *event) {
    switch (AInputEvent_getType(event)) {
        case AINPUT_EVENT_TYPE_KEY:
            if (AKeyEvent_getKeyCode(event) == AKEYCODE_BACK) {
#ifdef DEBUG
                LOGI("back %s", action_name(event));
#endif
                if (AKeyEvent_getAction(event) == AKEY_EVENT_ACTION_UP) {
                    ++repeat;
#ifdef DEBUG_GENUINE
                    LOGI("back, repeat: %d", repeat);
#endif
                }
                return repeat < 6;
            }
            break;
        case AINPUT_EVENT_TYPE_MOTION:
            break;
        default:
            break;
    }
    repeat = 0;
    return 0;
}

static int onInputEvent(int fd __unused, int events, void *data) {
    AInputEvent *event;
    if (events == ALOOPER_EVENT_INPUT) {
        while (AInputQueue_getEvent(data, &event) >= 0) {
            if (AInputQueue_preDispatchEvent(data, event) == 0) {
                AInputQueue_finishEvent(data, event, handleEvent(event));
            }
        }
    }
    return 1;
}

static void onInputQueueCreated(ANativeActivity *activity, AInputQueue *queue) {
#ifdef DEBUG_GENUINE
    LOGI("onInputQueueCreated");
#endif
    AInputQueue_attachLooper(queue, activity->instance, ALOOPER_POLL_CALLBACK, onInputEvent, queue);
}

static void onInputQueueDestroyed(ANativeActivity *activity __unused, AInputQueue *queue) {
#ifdef DEBUG_GENUINE
    LOGI("onInputQueueDestroyed");
#endif
    AInputQueue_detachLooper(queue);
}

JNIEXPORT void __unused ANativeActivity_onCreate(ANativeActivity *activity,
                                                 void *savedState __unused,
                                                 size_t savedStateSize __unused) {
    activity->callbacks->onStart = onStart;
    activity->callbacks->onStop = onStop;
    activity->callbacks->onResume = onResume;
    activity->callbacks->onDestroy = onDestroy;
    activity->callbacks->onInputQueueCreated = onInputQueueCreated;
    activity->callbacks->onInputQueueDestroyed = onInputQueueDestroyed;
    activity->callbacks->onNativeWindowCreated = onNativeWindowCreated;

    activity->instance = ALooper_prepare(0);
}
