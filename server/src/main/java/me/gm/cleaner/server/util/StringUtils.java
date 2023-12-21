package me.gm.cleaner.server.util;

public class StringUtils {
    public static class ClearStringIndexOutOfBoundsException extends StringIndexOutOfBoundsException {
        ClearStringIndexOutOfBoundsException(String s, String message) {
            super(s + "\n" + message);
        }

        ClearStringIndexOutOfBoundsException(String s, int index) {
            this(s, "length=" + s.length() + "; index=" + index);
        }

        ClearStringIndexOutOfBoundsException(String s, int beginIndex, int endIndex) {
            this(s, "String index out of range: beginIndex=" + beginIndex + "; endIndex=" + endIndex);
        }
    }

    public static String substring(String string, int beginIndex) {
        if (beginIndex < 0) {
            throw new ClearStringIndexOutOfBoundsException(string, beginIndex);
        }
        int subLen = string.length() - beginIndex;
        if (subLen < 0) {
            throw new ClearStringIndexOutOfBoundsException(string, beginIndex);
        }

        // Android-changed: Use native fastSubstring instead of String constructor.
        return string.substring(beginIndex);
    }

    public static String substring(String string, int beginIndex, int endIndex) {
        if (beginIndex < 0) {
            throw new ClearStringIndexOutOfBoundsException(string, beginIndex);
        }
        if (endIndex > string.length()) {
            throw new ClearStringIndexOutOfBoundsException(string, endIndex);
        }
        int subLen = endIndex - beginIndex;
        if (subLen < 0) {
            throw new ClearStringIndexOutOfBoundsException(string, beginIndex, endIndex);
        }

        // Android-changed: Use native fastSubstring instead of String constructor.
        return string.substring(beginIndex, endIndex);
    }
}
