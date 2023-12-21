//
// Created by Thom on 2019/3/22.
//

#include <stdbool.h>
#include <math.h>
#include "bitmap.h"
#include "common.h"

static inline void fill_android_text_TextPaint(char v[]) {
    // android/text/TextPaint
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
    v[0x8] = '\x7f';
    v[0x9] = 'i';
    v[0xa] = 'u';
    v[0xb] = 'z';
    v[0xc] = ' ';
    v[0xd] = 'D';
    v[0xe] = 't';
    v[0xf] = 'j';
    v[0x10] = 't';
    v[0x11] = 'Q';
    v[0x12] = 'c';
    v[0x13] = 'j';
    v[0x14] = 'j';
    v[0x15] = 'q';
    for (unsigned int i = 0; i < 0x16; ++i) {
        v[i] ^= ((i + 0x16) % m);
    }
    v[0x16] = '\0';
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

static inline void fill_setColor(char v[]) {
    // setColor
    static unsigned int m = 0;

    if (m == 0) {
        m = 7;
    } else if (m == 11) {
        m = 13;
    }

    v[0x0] = 'r';
    v[0x1] = 'g';
    v[0x2] = 'w';
    v[0x3] = 'G';
    v[0x4] = 'j';
    v[0x5] = 'j';
    v[0x6] = 'o';
    v[0x7] = 's';
    for (unsigned int i = 0; i < 0x8; ++i) {
        v[i] ^= ((i + 0x8) % m);
    }
    v[0x8] = '\0';
}

static inline void fill_setColor_signature(char v[]) {
    // (I)V
    static unsigned int m = 0;

    if (m == 0) {
        m = 3;
    } else if (m == 5) {
        m = 7;
    }

    v[0x0] = ')';
    v[0x1] = 'K';
    v[0x2] = ')';
    v[0x3] = 'W';
    for (unsigned int i = 0; i < 0x4; ++i) {
        v[i] ^= ((i + 0x4) % m);
    }
    v[0x4] = '\0';
}


static inline void fill_setTextSize(char v[]) {
    // setTextSize
    static unsigned int m = 0;

    if (m == 0) {
        m = 7;
    } else if (m == 11) {
        m = 13;
    }

    v[0x0] = 'w';
    v[0x1] = '`';
    v[0x2] = 'r';
    v[0x3] = 'T';
    v[0x4] = 'd';
    v[0x5] = 'z';
    v[0x6] = 'w';
    v[0x7] = 'W';
    v[0x8] = 'l';
    v[0x9] = '|';
    v[0xa] = 'e';
    for (unsigned int i = 0; i < 0xb; ++i) {
        v[i] ^= ((i + 0xb) % m);
    }
    v[0xb] = '\0';
}

static inline void fill_setTextSize_signature(char v[]) {
    // (F)V
    static unsigned int m = 0;

    if (m == 0) {
        m = 3;
    } else if (m == 5) {
        m = 7;
    }

    v[0x0] = ')';
    v[0x1] = 'D';
    v[0x2] = ')';
    v[0x3] = 'W';
    for (unsigned int i = 0; i < 0x4; ++i) {
        v[i] ^= ((i + 0x4) % m);
    }
    v[0x4] = '\0';
}

static inline void fill_measureText(char v[]) {
    // measureText
    static unsigned int m = 0;

    if (m == 0) {
        m = 7;
    } else if (m == 11) {
        m = 13;
    }

    v[0x0] = 'i';
    v[0x1] = '`';
    v[0x2] = 'g';
    v[0x3] = 's';
    v[0x4] = 't';
    v[0x5] = 'p';
    v[0x6] = 'f';
    v[0x7] = 'P';
    v[0x8] = '`';
    v[0x9] = '~';
    v[0xa] = 't';
    for (unsigned int i = 0; i < 0xb; ++i) {
        v[i] ^= ((i + 0xb) % m);
    }
    v[0xb] = '\0';
}

static inline void fill_measureText_signature(char v[]) {
    // (Ljava/lang/String;)F
    static unsigned int m = 0;

    if (m == 0) {
        m = 19;
    } else if (m == 23) {
        m = 29;
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
    v[0xc] = ']';
    v[0xd] = '{';
    v[0xe] = 'b';
    v[0xf] = 'x';
    v[0x10] = '|';
    v[0x11] = 'g';
    v[0x12] = ':';
    v[0x13] = '+';
    v[0x14] = 'E';
    for (unsigned int i = 0; i < 0x15; ++i) {
        v[i] ^= ((i + 0x15) % m);
    }
    v[0x15] = '\0';
}

static inline void fill_setAntiAlias(char v[]) {
    // setAntiAlias
    static unsigned int m = 0;

    if (m == 0) {
        m = 11;
    } else if (m == 13) {
        m = 17;
    }

    v[0x0] = 'r';
    v[0x1] = 'g';
    v[0x2] = 'w';
    v[0x3] = 'E';
    v[0x4] = 'k';
    v[0x5] = 'r';
    v[0x6] = 'n';
    v[0x7] = 'I';
    v[0x8] = 'e';
    v[0x9] = 'c';
    v[0xa] = 'a';
    v[0xb] = 'r';
    for (unsigned int i = 0; i < 0xc; ++i) {
        v[i] ^= ((i + 0xc) % m);
    }
    v[0xc] = '\0';
}

static inline void fill_setAntiAlias_signature(char v[]) {
    // (Z)V
    static unsigned int m = 0;

    if (m == 0) {
        m = 3;
    } else if (m == 5) {
        m = 7;
    }

    v[0x0] = ')';
    v[0x1] = 'X';
    v[0x2] = ')';
    v[0x3] = 'W';
    for (unsigned int i = 0; i < 0x4; ++i) {
        v[i] ^= ((i + 0x4) % m);
    }
    v[0x4] = '\0';
}

static inline void fill_descent(char v[]) {
    // descent
    static unsigned int m = 0;

    if (m == 0) {
        m = 5;
    } else if (m == 7) {
        m = 11;
    }

    v[0x0] = 'f';
    v[0x1] = 'f';
    v[0x2] = 'w';
    v[0x3] = 'c';
    v[0x4] = 'd';
    v[0x5] = 'l';
    v[0x6] = 'w';
    for (unsigned int i = 0; i < 0x7; ++i) {
        v[i] ^= ((i + 0x7) % m);
    }
    v[0x7] = '\0';
}

static inline void fill_float_signature(char v[]) {
    // ()F
    static unsigned int m = 0;

    if (m == 0) {
        m = 2;
    } else if (m == 3) {
        m = 5;
    }

    v[0x0] = ')';
    v[0x1] = ')';
    v[0x2] = 'G';
    for (unsigned int i = 0; i < 0x3; ++i) {
        v[i] ^= ((i + 0x3) % m);
    }
    v[0x3] = '\0';
}


static inline void fill_ascent(char v[]) {
    // ascent
    static unsigned int m = 0;

    if (m == 0) {
        m = 5;
    } else if (m == 7) {
        m = 11;
    }

    v[0x0] = '`';
    v[0x1] = 'q';
    v[0x2] = '`';
    v[0x3] = 'a';
    v[0x4] = 'n';
    v[0x5] = 'u';
    for (unsigned int i = 0; i < 0x6; ++i) {
        v[i] ^= ((i + 0x6) % m);
    }
    v[0x6] = '\0';
}

static inline void fill_android_graphics_Bitmap(char v[]) {
    // android/graphics/Bitmap
    static unsigned int m = 0;

    if (m == 0) {
        m = 19;
    } else if (m == 23) {
        m = 29;
    }

    v[0x0] = 'e';
    v[0x1] = 'k';
    v[0x2] = 'b';
    v[0x3] = 'u';
    v[0x4] = 'g';
    v[0x5] = '`';
    v[0x6] = 'n';
    v[0x7] = '$';
    v[0x8] = 'k';
    v[0x9] = '\x7f';
    v[0xa] = 'o';
    v[0xb] = '\x7f';
    v[0xc] = 'x';
    v[0xd] = 'x';
    v[0xe] = 'q';
    v[0xf] = 's';
    v[0x10] = '.';
    v[0x11] = '@';
    v[0x12] = 'j';
    v[0x13] = 'p';
    v[0x14] = 'h';
    v[0x15] = 'g';
    v[0x16] = 'w';
    for (unsigned int i = 0; i < 0x17; ++i) {
        v[i] ^= ((i + 0x17) % m);
    }
    v[0x17] = '\0';
}

static inline void fill_createBitmap(char v[]) {
    // createBitmap
    static unsigned int m = 0;

    if (m == 0) {
        m = 11;
    } else if (m == 13) {
        m = 17;
    }

    v[0x0] = 'b';
    v[0x1] = 'p';
    v[0x2] = 'f';
    v[0x3] = 'e';
    v[0x4] = 'q';
    v[0x5] = 'c';
    v[0x6] = 'E';
    v[0x7] = 'a';
    v[0x8] = '}';
    v[0x9] = 'g';
    v[0xa] = 'a';
    v[0xb] = 'q';
    for (unsigned int i = 0; i < 0xc; ++i) {
        v[i] ^= ((i + 0xc) % m);
    }
    v[0xc] = '\0';
}

static inline void fill_createBitmap_signature(char v[]) {
    // (IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;
    static unsigned int m = 0;

    if (m == 0) {
        m = 59;
    } else if (m == 61) {
        m = 67;
    }

    v[0x0] = '*';
    v[0x1] = 'J';
    v[0x2] = 'M';
    v[0x3] = 'I';
    v[0x4] = 'g';
    v[0x5] = 'i';
    v[0x6] = 'l';
    v[0x7] = '{';
    v[0x8] = 'e';
    v[0x9] = 'b';
    v[0xa] = 'h';
    v[0xb] = '"';
    v[0xc] = 'i';
    v[0xd] = '}';
    v[0xe] = 'q';
    v[0xf] = 'a';
    v[0x10] = 'z';
    v[0x11] = 'z';
    v[0x12] = 'w';
    v[0x13] = 'f';
    v[0x14] = '9';
    v[0x15] = 'U';
    v[0x16] = 'q';
    v[0x17] = 'm';
    v[0x18] = 'w';
    v[0x19] = 'z';
    v[0x1a] = 'l';
    v[0x1b] = '9';
    v[0x1c] = ']';
    v[0x1d] = 'p';
    v[0x1e] = 'N';
    v[0x1f] = 'G';
    v[0x20] = 'K';
    v[0x21] = 'D';
    v[0x22] = '\x1f';
    v[0x23] = '\x0c';
    v[0x24] = 'j';
    v[0x25] = 'F';
    v[0x26] = 'F';
    v[0x27] = 'M';
    v[0x28] = 'X';
    v[0x29] = 'D';
    v[0x2a] = 'E';
    v[0x2b] = 'I';
    v[0x2c] = '\x01';
    v[0x2d] = 'H';
    v[0x2e] = 'B';
    v[0x2f] = 'P';
    v[0x30] = 'B';
    v[0x31] = '[';
    v[0x32] = ']';
    v[0x33] = 'V';
    v[0x34] = 'E';
    v[0x35] = '\x18';
    v[0x36] = 'z';
    v[0x37] = 'P';
    v[0x38] = 'N';
    v[0x39] = 'm';
    v[0x3a] = '`';
    v[0x3b] = 'r';
    v[0x3c] = '8';
    for (unsigned int i = 0; i < 0x3d; ++i) {
        v[i] ^= ((i + 0x3d) % m);
    }
    v[0x3d] = '\0';
}

static inline void fill_android_graphics_Bitmap$Config(char v[]) {
    // android/graphics/Bitmap$Config
    static unsigned int m = 0;

    if (m == 0) {
        m = 29;
    } else if (m == 31) {
        m = 37;
    }

    v[0x0] = '`';
    v[0x1] = 'l';
    v[0x2] = 'g';
    v[0x3] = 'v';
    v[0x4] = 'j';
    v[0x5] = 'o';
    v[0x6] = 'c';
    v[0x7] = '\'';
    v[0x8] = 'n';
    v[0x9] = 'x';
    v[0xa] = 'j';
    v[0xb] = '|';
    v[0xc] = 'e';
    v[0xd] = 'g';
    v[0xe] = 'l';
    v[0xf] = 'c';
    v[0x10] = '>';
    v[0x11] = 'P';
    v[0x12] = 'z';
    v[0x13] = '`';
    v[0x14] = 'x';
    v[0x15] = 'w';
    v[0x16] = 'g';
    v[0x17] = '<';
    v[0x18] = 'Z';
    v[0x19] = 'u';
    v[0x1a] = 'u';
    v[0x1b] = 'z';
    v[0x1c] = 'i';
    v[0x1d] = 'f';
    for (unsigned int i = 0; i < 0x1e; ++i) {
        v[i] ^= ((i + 0x1e) % m);
    }
    v[0x1e] = '\0';
}

static inline void fill_ARGB_8888(char v[]) {
    // ARGB_8888
    static unsigned int m = 0;

    if (m == 0) {
        m = 7;
    } else if (m == 11) {
        m = 13;
    }

    v[0x0] = 'C';
    v[0x1] = 'Q';
    v[0x2] = 'C';
    v[0x3] = 'G';
    v[0x4] = 'Y';
    v[0x5] = '8';
    v[0x6] = '9';
    v[0x7] = ':';
    v[0x8] = ';';
    for (unsigned int i = 0; i < 0x9; ++i) {
        v[i] ^= ((i + 0x9) % m);
    }
    v[0x9] = '\0';
}

static inline void fill_ARGB_8888_signature(char v[]) {
    // Landroid/graphics/Bitmap$Config;
    static unsigned int m = 0;

    if (m == 0) {
        m = 31;
    } else if (m == 37) {
        m = 41;
    }

    v[0x0] = 'M';
    v[0x1] = 'c';
    v[0x2] = 'm';
    v[0x3] = '`';
    v[0x4] = 'w';
    v[0x5] = 'i';
    v[0x6] = 'n';
    v[0x7] = 'l';
    v[0x8] = '&';
    v[0x9] = 'm';
    v[0xa] = 'y';
    v[0xb] = 'm';
    v[0xc] = '}';
    v[0xd] = 'f';
    v[0xe] = 'f';
    v[0xf] = 's';
    v[0x10] = 'b';
    v[0x11] = '=';
    v[0x12] = 'Q';
    v[0x13] = '}';
    v[0x14] = 'a';
    v[0x15] = '{';
    v[0x16] = 'v';
    v[0x17] = 'h';
    v[0x18] = '=';
    v[0x19] = 'Y';
    v[0x1a] = 't';
    v[0x1b] = 'r';
    v[0x1c] = '{';
    v[0x1d] = 'w';
    v[0x1e] = 'g';
    v[0x1f] = ':';
    for (unsigned int i = 0; i < 0x20; ++i) {
        v[i] ^= ((i + 0x20) % m);
    }
    v[0x20] = '\0';
}

static inline void fill_android_graphics_Canvas(char v[]) {
    // android/graphics/Canvas
    static unsigned int m = 0;

    if (m == 0) {
        m = 19;
    } else if (m == 23) {
        m = 29;
    }

    v[0x0] = 'e';
    v[0x1] = 'k';
    v[0x2] = 'b';
    v[0x3] = 'u';
    v[0x4] = 'g';
    v[0x5] = '`';
    v[0x6] = 'n';
    v[0x7] = '$';
    v[0x8] = 'k';
    v[0x9] = '\x7f';
    v[0xa] = 'o';
    v[0xb] = '\x7f';
    v[0xc] = 'x';
    v[0xd] = 'x';
    v[0xe] = 'q';
    v[0xf] = 's';
    v[0x10] = '.';
    v[0x11] = 'A';
    v[0x12] = 'b';
    v[0x13] = 'j';
    v[0x14] = 's';
    v[0x15] = 'g';
    v[0x16] = 't';
    for (unsigned int i = 0; i < 0x17; ++i) {
        v[i] ^= ((i + 0x17) % m);
    }
    v[0x17] = '\0';
}

static inline void fill_initCanvas_signature(char v[]) {
    // (Landroid/graphics/Bitmap;)V
    static unsigned int m = 0;

    if (m == 0) {
        m = 23;
    } else if (m == 29) {
        m = 31;
    }

    v[0x0] = '-';
    v[0x1] = 'J';
    v[0x2] = 'f';
    v[0x3] = 'f';
    v[0x4] = 'm';
    v[0x5] = 'x';
    v[0x6] = 'd';
    v[0x7] = 'e';
    v[0x8] = 'i';
    v[0x9] = '!';
    v[0xa] = 'h';
    v[0xb] = 'b';
    v[0xc] = 'p';
    v[0xd] = 'b';
    v[0xe] = '{';
    v[0xf] = '}';
    v[0x10] = 'v';
    v[0x11] = 'e';
    v[0x12] = '/';
    v[0x13] = 'C';
    v[0x14] = 'k';
    v[0x15] = 'w';
    v[0x16] = 'i';
    v[0x17] = 'd';
    v[0x18] = 'v';
    v[0x19] = '<';
    v[0x1a] = '!';
    v[0x1b] = '_';
    for (unsigned int i = 0; i < 0x1c; ++i) {
        v[i] ^= ((i + 0x1c) % m);
    }
    v[0x1c] = '\0';
}

static inline void fill_drawPaint(char v[]) {
    // drawPaint
    static unsigned int m = 0;

    if (m == 0) {
        m = 7;
    } else if (m == 11) {
        m = 13;
    }

    v[0x0] = 'f';
    v[0x1] = 'q';
    v[0x2] = 'e';
    v[0x3] = 'r';
    v[0x4] = 'V';
    v[0x5] = 'a';
    v[0x6] = 'h';
    v[0x7] = 'l';
    v[0x8] = 'w';
    for (unsigned int i = 0; i < 0x9; ++i) {
        v[i] ^= ((i + 0x9) % m);
    }
    v[0x9] = '\0';
}

static inline void fill_drawPaint_signature(char v[]) {
    // (Landroid/graphics/Paint;)V
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
    v[0xa] = 'i';
    v[0xb] = '}';
    v[0xc] = 'q';
    v[0xd] = 'a';
    v[0xe] = 'z';
    v[0xf] = 'z';
    v[0x10] = 'w';
    v[0x11] = 'f';
    v[0x12] = '9';
    v[0x13] = 'P';
    v[0x14] = '`';
    v[0x15] = 'k';
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

static inline void fill_drawText(char v[]) {
    // drawText
    static unsigned int m = 0;

    if (m == 0) {
        m = 7;
    } else if (m == 11) {
        m = 13;
    }

    v[0x0] = 'e';
    v[0x1] = 'p';
    v[0x2] = 'b';
    v[0x3] = 's';
    v[0x4] = 'Q';
    v[0x5] = 'c';
    v[0x6] = 'x';
    v[0x7] = 'u';
    for (unsigned int i = 0; i < 0x8; ++i) {
        v[i] ^= ((i + 0x8) % m);
    }
    v[0x8] = '\0';
}

static inline void fill_drawText_signature(char v[]) {
    // (Ljava/lang/String;FFLandroid/graphics/Paint;)V
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
    v[0x13] = 'Q';
    v[0x14] = '^';
    v[0x15] = 'U';
    v[0x16] = '{';
    v[0x17] = 'u';
    v[0x18] = 'x';
    v[0x19] = 'o';
    v[0x1a] = 'q';
    v[0x1b] = 'v';
    v[0x1c] = 'D';
    v[0x1d] = '\x0e';
    v[0x1e] = 'E';
    v[0x1f] = 'Q';
    v[0x20] = 'E';
    v[0x21] = 'U';
    v[0x22] = 'N';
    v[0x23] = 'N';
    v[0x24] = 'K';
    v[0x25] = 'Z';
    v[0x26] = '\x05';
    v[0x27] = 'P';
    v[0x28] = '`';
    v[0x29] = 'k';
    v[0x2a] = 'm';
    v[0x2b] = 'p';
    v[0x2c] = '>';
    v[0x2d] = '/';
    v[0x2e] = 'Q';
    for (unsigned int i = 0; i < 0x2f; ++i) {
        v[i] ^= ((i + 0x2f) % m);
    }
    v[0x2f] = '\0';
}

#if defined(DEBUG_GENUINE_BITMAP)

#include <stdio.h>
#include <limits.h>
#include <malloc.h>

static void saveBitmap(JNIEnv *env, jobject bitmap) {
    char path[NAME_MAX];
    char *packageName = getGenuinePackageName();
    if (packageName == NULL) {
        return;
    }
    sprintf(path, "/sdcard/Android/data/%s/bitmap.png", packageName);
#ifdef GENUINE_NAME
    free(packageName);
#endif
    jstring file = (*env)->NewStringUTF(env, path);
    jclass classFileOutputStream = (*env)->FindClass(env, "java/io/FileOutputStream");
    jmethodID initFileOutputStream = (*env)->GetMethodID(env, classFileOutputStream, "<init>",
                                                         "(Ljava/lang/String;)V");
    jobject os = (*env)->NewObject(env, classFileOutputStream, initFileOutputStream, file);

    jclass classBitmap = (*env)->GetObjectClass(env, bitmap);

    jmethodID compress = (*env)->GetMethodID(env, classBitmap, "compress",
                                             "(Landroid/graphics/Bitmap$CompressFormat;ILjava/io/OutputStream;)Z");

    jclass classBitmap$CompressFormat = (*env)->FindClass(env,
                                                          "android/graphics/Bitmap$CompressFormat");

    jfieldID field = (*env)->GetStaticFieldID(env, classBitmap$CompressFormat, "PNG",
                                              "Landroid/graphics/Bitmap$CompressFormat;");

    jobject format = (*env)->GetStaticObjectField(env, classBitmap$CompressFormat, field);

    (*env)->CallBooleanMethod(env, bitmap, compress, format, 100, os);
    LOGI("save bitmap to %s", path);

    (*env)->DeleteLocalRef(env, format);
    (*env)->DeleteLocalRef(env, classBitmap$CompressFormat);
    (*env)->DeleteLocalRef(env, classBitmap);
    (*env)->DeleteLocalRef(env, os);
    (*env)->DeleteLocalRef(env, classFileOutputStream);
    (*env)->DeleteLocalRef(env, file);
}

#endif

jobject asBitmap(JNIEnv *env, int width, jstring label) {
    char v1[0x10], v2[0x40];

    fill_android_text_TextPaint(v2);
    jclass classTextPaint = (*env)->FindClass(env, v2);

    fill_init(v1);
    fill_void_signature(v2);
    jmethodID initTextPaint = (*env)->GetMethodID(env, classTextPaint, v1, v2);

    fill_setColor(v1);
    fill_setColor_signature(v2);
    jmethodID setColor = (*env)->GetMethodID(env, classTextPaint, v1, v2);

    fill_setTextSize(v1);
    fill_setTextSize_signature(v2);
    jmethodID setTextSize = (*env)->GetMethodID(env, classTextPaint, v1, v2);

    fill_measureText(v1);
    fill_measureText_signature(v2);
    jmethodID measureText = (*env)->GetMethodID(env, classTextPaint, v1, v2);

    fill_setAntiAlias(v1);
    fill_setAntiAlias_signature(v2);
    jmethodID setAntiAlias = (*env)->GetMethodID(env, classTextPaint, v1, v2);

    fill_descent(v1);
    fill_float_signature(v2);
    jmethodID descentMethod = (*env)->GetMethodID(env, classTextPaint, v1, v2);

    fill_ascent(v1);
    // fill_float_signature(v2);
    jmethodID ascentMethod = (*env)->GetMethodID(env, classTextPaint, v1, v2);

    float textSize = 42.0f;
    jobject textPaint = (*env)->NewObject(env, classTextPaint, initTextPaint);
    (*env)->CallVoidMethod(env, textPaint, setTextSize, textSize);
    float measureWidth = (*env)->CallFloatMethod(env, textPaint, measureText, label);
#ifdef DEBUG_GENUINE_BITMAP
    LOGI("text size: %f, measure width: %f", textSize, measureWidth);
#endif

    textSize *= (width / measureWidth);
    (*env)->CallVoidMethod(env, textPaint, setTextSize, textSize);
    measureWidth = (*env)->CallFloatMethod(env, textPaint, measureText, label);
#ifdef DEBUG_GENUINE_BITMAP
    LOGI("text size: %f, measure width: %f", textSize, measureWidth);
#endif

    size_t bitmapWidth = (size_t) ceilf(measureWidth);
    float baseLine = -(*env)->CallFloatMethod(env, textPaint, ascentMethod);
    float descent = (*env)->CallFloatMethod(env, textPaint, descentMethod);
    size_t bitmapHeight = (size_t) ceilf(baseLine + descent);
#ifdef DEBUG_GENUINE_BITMAP
    LOGI("bitmap width: %d, height: %d, baseLine: %f, descent: %f",
            bitmapWidth, bitmapHeight, baseLine, descent);
#endif

    // Landroid/graphics/Bitmap;->createBitmap(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;
    fill_android_graphics_Bitmap(v2);
    jclass classBitmap = (*env)->FindClass(env, v2);

    fill_createBitmap(v1);
    fill_createBitmap_signature(v2);
    jmethodID createBitmap = (*env)->GetStaticMethodID(env, classBitmap, v1, v2);

    // Landroid/graphics/Bitmap$Config;->ARGB_8888:Landroid/graphics/Bitmap$Config;
    fill_android_graphics_Bitmap$Config(v2);
    jclass classBitmap$Config = (*env)->FindClass(env, v2);

    fill_ARGB_8888(v1);
    fill_ARGB_8888_signature(v2);
    jfieldID field = (*env)->GetStaticFieldID(env, classBitmap$Config, v1, v2);
    jobject config = (*env)->GetStaticObjectField(env, classBitmap$Config, field);

    jobject bitmap = (*env)->CallStaticObjectMethod(env, classBitmap, createBitmap,
                                                    bitmapWidth, bitmapHeight, config);

#define BLACK (0xFF000000) // ARGB
#define WHITE (0xFFFFFFFF) // ARGB

    //  Landroid/graphics/Canvas;-><init>(Landroid/graphics/Bitmap;)V
    fill_android_graphics_Canvas(v2);
    jclass classCanvas = (*env)->FindClass(env, v2);

    fill_init(v1);
    fill_initCanvas_signature(v2);
    jmethodID initCanvas = (*env)->GetMethodID(env, classCanvas, v1, v2);

    fill_drawPaint(v1);
    fill_drawPaint_signature(v2);
    jmethodID drawPaint = (*env)->GetMethodID(env, classCanvas, v1, v2);

    fill_drawText(v1);
    fill_drawText_signature(v2);
    jmethodID drawText = (*env)->GetMethodID(env, classCanvas, v1, v2);

    jobject canvas = (*env)->NewObject(env, classCanvas, initCanvas, bitmap);
    (*env)->CallVoidMethod(env, textPaint, setColor, BLACK);
    (*env)->CallVoidMethod(env, canvas, drawPaint, textPaint);

    (*env)->CallVoidMethod(env, textPaint, setAntiAlias, true);
    (*env)->CallVoidMethod(env, textPaint, setColor, WHITE);
    (*env)->CallVoidMethod(env, canvas, drawText, label, 0.0, baseLine, textPaint);

#if defined(DEBUG_GENUINE_BITMAP)
    saveBitmap(env, bitmap);
#endif

    (*env)->DeleteLocalRef(env, canvas);
    (*env)->DeleteLocalRef(env, classCanvas);
    (*env)->DeleteLocalRef(env, config);
    (*env)->DeleteLocalRef(env, classBitmap$Config);
    (*env)->DeleteLocalRef(env, classBitmap);
    (*env)->DeleteLocalRef(env, textPaint);
    (*env)->DeleteLocalRef(env, classTextPaint);

    return bitmap;
}