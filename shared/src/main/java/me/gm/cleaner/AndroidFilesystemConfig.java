package me.gm.cleaner;

import java.util.Locale;

public class AndroidFilesystemConfig {
    public static final int AID_APP_START = 10000; /* first app user */
    public static final int AID_APP_END = 19999; /* last app user */

    public static final int AID_CACHE_GID_START = 20000; /* start of gids for apps to mark cached data */
    public static final int AID_CACHE_GID_END = 29999; /* end of gids for apps to mark cached data */

    public static final int AID_EXT_GID_START = 30000; /* start of gids for apps to mark external data */
    public static final int AID_EXT_GID_END = 39999; /* end of gids for apps to mark external data */

    public static final int AID_EXT_CACHE_GID_START = 40000; /* start of gids for apps to mark external cached data */
    public static final int AID_EXT_CACHE_GID_END = 49999; /* end of gids for apps to mark external cached data */

    public static final int AID_SHARED_GID_START = 50000; /* start of gids for apps in each user to share */
    public static final int AID_SHARED_GID_END = 59999; /* end of gids for apps in each user to share */

    /* use the ranges below to determine whether a process is isolated */
    public static final int AID_ISOLATED_START = 90000; /* start of uids for fully isolated sandboxed processes */
    public static final int AID_ISOLATED_END = 99999; /* end of uids for fully isolated sandboxed processes */

    public static final int AID_USER_OFFSET = 100000; /* offset for uid ranges for each user */

    /*
     * @see https://android.googlesource.com/platform/bionic/+/android10-release/libc/bionic/grp_grd.cpp
     *      print_app_name_from_gid()
     */
    public static String getAppPrincipalName(int uid) {
        final var userid = uid / AID_USER_OFFSET;
        final var appid = uid % AID_USER_OFFSET;
        if (appid > AID_ISOLATED_START) {
            return String.format(Locale.ENGLISH, "u%d_i%d", userid, appid - AID_ISOLATED_START);
        } else if (userid == 0 && appid >= AID_SHARED_GID_START && appid <= AID_SHARED_GID_END) {
            return String.format(Locale.ENGLISH, "all_a%d", appid - AID_SHARED_GID_START);
        } else if (appid >= AID_EXT_CACHE_GID_START && appid <= AID_EXT_CACHE_GID_END) {
            return String.format(Locale.ENGLISH, "u%d_a%d_ext_cache", userid, appid - AID_EXT_CACHE_GID_START);
        } else if (appid >= AID_EXT_GID_START && appid <= AID_EXT_GID_END) {
            return String.format(Locale.ENGLISH, "u%d_a%d_ext", userid, appid - AID_EXT_GID_START);
        } else if (appid >= AID_CACHE_GID_START && appid <= AID_CACHE_GID_END) {
            return String.format(Locale.ENGLISH, "u%d_a%d_cache", userid, appid - AID_CACHE_GID_START);
        } else {
            return String.format(Locale.ENGLISH, "u%d_a%d", userid, appid - AID_APP_START);
        }
    }
}
