package me.gm.cleaner.util;

import androidx.annotation.Keep;

import java.lang.reflect.Member;

/**
 * Created by thom on 2018/10/31.
 */
@Keep
public class Genuine {

    /* check true, return defined VERSION */
    public static final int CHECK_TRUE = 0;

    /* cannot make sure check result */
    public static final int CHECK_FALSE = 1;

    /* fake signature */
    public static final int CHECK_FAKE = 2;

    /* third party apk loaded */
    public static final int CHECK_OVERLAY = 3;

    /* odex is tampered */
    public static final int CHECK_ODEX = 4;

    /* third party dex loaded */
    public static final int CHECK_DEX = 5;

    /* binder proxy */
    public static final int CHECK_PROXY = 6;

    /* cannot check */
    public static final int CHECK_ERROR = 7;

    /* fatal hook */
    public static final int CHECK_FATAL = 8;

    /* noapk */
    public static final int CHECK_NOAPK = 9;

    private Genuine() {
    }

    private static native Object invoke(Member m, int i, Object a, Object t, Object[] as) throws Throwable;

    /* refer CHECK_XXX */
    public static native int version();

}
