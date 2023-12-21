//
// Created by Thom on 2020/9/9.
//

#include "epic-field.h"
#include <string.h>

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

static inline void fill_getDeclaredFields(char v[]) {
    // getDeclaredFields
    static unsigned int m = 0;

    if (m == 0) {
        m = 13;
    } else if (m == 17) {
        m = 19;
    }

    v[0x0] = 'c';
    v[0x1] = '`';
    v[0x2] = 'r';
    v[0x3] = 'C';
    v[0x4] = 'm';
    v[0x5] = 'j';
    v[0x6] = 'f';
    v[0x7] = 'j';
    v[0x8] = '~';
    v[0x9] = 'e';
    v[0xa] = 'e';
    v[0xb] = 'D';
    v[0xc] = 'j';
    v[0xd] = 'a';
    v[0xe] = 'i';
    v[0xf] = 'b';
    v[0x10] = 't';
    for (unsigned int i = 0; i < 0x11; ++i) {
        v[i] ^= ((i + 0x11) % m);
    }
    v[0x11] = '\0';
}

static inline void fill_getDeclaredFields_signature(char v[]) {
    // ()[Ljava/lang/reflect/Field;
    static unsigned int m = 0;

    if (m == 0) {
        m = 23;
    } else if (m == 29) {
        m = 31;
    }

    v[0x0] = '-';
    v[0x1] = '/';
    v[0x2] = '\\';
    v[0x3] = 'D';
    v[0x4] = 'c';
    v[0x5] = 'k';
    v[0x6] = '}';
    v[0x7] = 'm';
    v[0x8] = '"';
    v[0x9] = 'b';
    v[0xa] = 'n';
    v[0xb] = '~';
    v[0xc] = 'v';
    v[0xd] = '=';
    v[0xe] = 'a';
    v[0xf] = 'q';
    v[0x10] = 's';
    v[0x11] = 'z';
    v[0x12] = 'e';
    v[0x13] = 'b';
    v[0x14] = 'v';
    v[0x15] = ',';
    v[0x16] = 'B';
    v[0x17] = 'l';
    v[0x18] = 'c';
    v[0x19] = 'k';
    v[0x1a] = 'l';
    v[0x1b] = '2';
    for (unsigned int i = 0; i < 0x1c; ++i) {
        v[i] ^= ((i + 0x1c) % m);
    }
    v[0x1c] = '\0';
}

static inline void fill_java_lang_reflect_Field(char v[]) {
    // java/lang/reflect/Field
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
    v[0xa] = '|';
    v[0xb] = 'j';
    v[0xc] = 'v';
    v[0xd] = '}';
    v[0xe] = 'w';
    v[0xf] = 'c';
    v[0x10] = 'u';
    v[0x11] = '-';
    v[0x12] = 'E';
    v[0x13] = 'm';
    v[0x14] = '`';
    v[0x15] = 'j';
    v[0x16] = 'c';
    for (unsigned int i = 0; i < 0x17; ++i) {
        v[i] ^= ((i + 0x17) % m);
    }
    v[0x17] = '\0';
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

static inline void fill_getType(char v[]) {
    // getType
    static unsigned int m = 0;

    if (m == 0) {
        m = 5;
    } else if (m == 7) {
        m = 11;
    }

    v[0x0] = 'e';
    v[0x1] = 'f';
    v[0x2] = 'p';
    v[0x3] = 'T';
    v[0x4] = 'x';
    v[0x5] = 'r';
    v[0x6] = 'f';
    for (unsigned int i = 0; i < 0x7; ++i) {
        v[i] ^= ((i + 0x7) % m);
    }
    v[0x7] = '\0';
}

static inline void fill_getType_signature(char v[]) {
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

static inline void fill_L_java_lang_Object(char v[]) {
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

static char *findField(JNIEnv *env, jobject clazz, int staticFlag, jclass type) {
    char v1[0x20], v2[0x20];
    char *name = NULL;

    fill_java_lang_Class(v1);
    jclass classClass = (*env)->FindClass(env, v1);

    fill_getDeclaredFields(v1);
    fill_getDeclaredFields_signature(v2);
    jmethodID getDeclaredFields = (*env)->GetMethodID(env, classClass, v1, v2);

    fill_java_lang_reflect_Field(v1);
    jclass classField = (*env)->FindClass(env, v1);

    fill_getModifiers(v1);
    fill_getModifiers_signature(v2);
    jmethodID getModifiers = (*env)->GetMethodID(env, classField, v1, v2);

    fill_getType(v1);
    fill_getType_signature(v2);
    jmethodID getType = (*env)->GetMethodID(env, classField, v1, v2);

    fill_getName(v1);
    fill_getName_signature(v2);
    jmethodID getName = (*env)->GetMethodID(env, classField, v1, v2);

    jobjectArray fields = (*env)->CallObjectMethod(env, clazz, getDeclaredFields);
    int length = (*env)->GetArrayLength(env, fields);
    for (int i = 0; i < length; ++i) {
        jobject field = (*env)->GetObjectArrayElement(env, fields, i);
        debug(env, "field: %s", field);
        int modifier = (*env)->CallIntMethod(env, field, getModifiers);
        if ((modifier & STATIC) == staticFlag
            && (*env)->IsSameObject(env, type, (*env)->CallObjectMethod(env, field, getType))) {
            jstring fieldString = (*env)->CallObjectMethod(env, field, getName);
            const char *fieldName = (*env)->GetStringUTFChars(env, fieldString, NULL);
            name = strdup(fieldName);
            (*env)->ReleaseStringUTFChars(env, fieldString, fieldName);
            (*env)->DeleteLocalRef(env, fieldString);
        }
        (*env)->DeleteLocalRef(env, field);
        if (name != NULL) {
            break;
        }
    }

    (*env)->DeleteLocalRef(env, fields);
    (*env)->DeleteLocalRef(env, classField);
    (*env)->DeleteLocalRef(env, classClass);
    return name;
}

char *findObjectArrayName(JNIEnv *env, jobject clazz) {
    char v[0x20];
    fill_L_java_lang_Object(v);
    jclass classObjectArray = (*env)->FindClass(env, v);
    char *name = findField(env, clazz, 0, classObjectArray);
    (*env)->DeleteLocalRef(env, classObjectArray);
    return name;
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

char *findStaticMapName(JNIEnv *env, jobject clazz) {
    char v[0x20];
    fill_java_util_Map(v);
    jclass classMap = (*env)->FindClass(env, v);
    char *name = findField(env, clazz, STATIC, classMap);
    (*env)->DeleteLocalRef(env, classMap);
    return name;
}
#endif
