package me.gm.cleaner.util;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Created by thom on 2018/11/2.
 */
public class SimpleApkV2 {

    private static final byte[] APK_V2_MAGIC = {'A', 'P', 'K', ' ', 'S', 'i', 'g', ' ',
            'B', 'l', 'o', 'c', 'k', ' ', '4', '2'};

    private static int[] getApkSignV2(String path) throws IOException {
        try (
                RandomAccessFile apk = new RandomAccessFile(path, "r")
        ) {
            ByteBuffer buffer = ByteBuffer.allocate(0x10);
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            apk.seek(apk.length() - 0x6);
            apk.readFully(buffer.array(), 0x0, 0x6);
            int offset = buffer.getInt();
            if (buffer.getShort() != 0) {
                throw new UnsupportedEncodingException("no zip");
            }

            apk.seek(offset - 0x10);
            apk.readFully(buffer.array(), 0x0, 0x10);

            if (!Arrays.equals(buffer.array(), APK_V2_MAGIC)) {
                throw new UnsupportedEncodingException("no apk v2");
            }

            // Read and compare size fields
            apk.seek(offset - 0x18);
            apk.readFully(buffer.array(), 0x0, 0x8);
            buffer.rewind();
            int size = (int) buffer.getLong();

            ByteBuffer block = ByteBuffer.allocate(size + 0x8);
            block.order(ByteOrder.LITTLE_ENDIAN);
            apk.seek(offset - block.capacity());
            apk.readFully(block.array(), 0x0, block.capacity());

            if (size != block.getLong()) {
                throw new UnsupportedEncodingException("no apk v2");
            }

            while (block.remaining() > 24) {
                size = (int) block.getLong();
                if (block.getInt() == 0x7109871a) {
                    // signer-sequence length, signer length, signed data length
                    block.position(block.position() + 12);
                    size = block.getInt(); // digests-sequence length

                    // digests, certificates length
                    block.position(block.position() + size + 0x4);

                    size = block.getInt(); // certificate length
                    break;
                } else {
                    block.position(block.position() + size - 0x4);
                }
            }

            int hash = 1;
            for (int i = 0; i < size; ++i) {
                hash = 31 * hash + block.get();
            }

            return new int[]{size, hash};
        }
    }

    private static String formatName(String name) {
        int length = name.length();
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (int i = 0; i < length; ++i) {
            int c = name.charAt(i) ^ ((i + length) % 20);
            sb.append("0x");
            sb.append(Integer.toHexString(c));
            sb.append(", ");
        }
        sb.append("0x0");
        sb.append("}");
        return sb.toString();
    }

    public static void main(String[] args) throws IOException {
        System.out.format("#define GENUINE_NAME %s%n", formatName(args[0]));
        int[] sizeAndHash = getApkSignV2(args[1]);
        System.out.format("#define GENUINE_SIZE 0x%04x%n", sizeAndHash[0]);
        System.out.format("#define GENUINE_HASH 0x%04x%n", sizeAndHash[1] ^ 0x14131211);
        String extra = "\n" +
                "// #define GET_GENUINE_CLASS_NAME function_name_for_get_genuine_class_name\n" +
                "// #define GET_GENUINE_PACKAGE_NAME function_name_for_get_genuine_package_name\n" +
                "\n" +
                "/* define to turn off maps check */\n" +
                "// #define NO_CHECK_MAPS\n" +
                "\n" +
                "#ifndef NO_CHECK_MAPS\n" +
                "/* define to anti odex */\n" +
                "// #define ANTI_ODEX\n" +
                "\n" +
                "/* define to anti overlay */\n" +
                "// #define ANTI_OVERLAY\n" +
                "#endif\n" +
                "\n" +
                "/* define to check plt hook for jniRegisterNativeMethods */\n" +
                "// #define CHECK_JNI_REGISTER_NATIVE_METHODS\n" +
                "\n" +
                "/* define to turn off xposed check */\n" +
                "// #define NO_CHECK_XPOSED\n" +
                "\n" +
                "/* define to turn on xposed-epic check\n" +
                " */\n" +
                "// #define CHECK_XPOSED_EPIC\n" +
                "\n" +
                "/* genuine false handler */\n" +
                "// #define GENUINE_FALSE_CRASH\n" +
                "// #define GENUINE_FALSE_NATIVE\n" +
                "\n" +
                "/* genuine fake handler */\n" +
                "// #define GENUINE_FAKE_CRASH\n" +
                "#define GENUINE_FAKE_NATIVE\n" +
                "\n" +
                "/* genuine overlay handler */\n" +
                "// #define GENUINE_OVERLAY_CRASH\n" +
                "// #define GENUINE_OVERLAY_NATIVE\n" +
                "\n" +
                "/* genuine odex handler */\n" +
                "// #define GENUINE_ODEX_CRASH\n" +
                "// #define GENUINE_ODEX_NATIVE\n" +
                "\n" +
                "/* genuine dex handler */\n" +
                "// #define GENUINE_DEX_CRASH\n" +
                "// #define GENUINE_DEX_NATIVE\n" +
                "\n" +
                "/* genuine proxy handler */\n" +
                "// #define GENUINE_PROXY_CRASH\n" +
                "// #define GENUINE_PROXY_NATIVE\n" +
                "\n" +
                "/* genuine error handler */\n" +
                "// #define GENUINE_ERROR_CRASH\n" +
                "#define GENUINE_ERROR_NATIVE\n" +
                "\n" +
                "/* genuine fatal handler */\n" +
                "// #define GENUINE_FATAL_CRASH\n" +
                "#define GENUINE_FATAL_NATIVE\n" +
                "\n" +
                "/* genuine noapk handler */\n" +
                "// #define GENUINE_NOAPK_CRASH\n" +
                "#define GENUINE_NOAPK_NATIVE";
        System.out.print(extra);
    }

}
