package me.gm.cleaner;

public class SharedConstants {
    public static final String PREF_STORAGE_REDIRECT;
    public static final String READ_ONLY;
    public static final String DENY_LIST_KEY;

    static {
        PREF_STORAGE_REDIRECT = "storage_redirect";
        READ_ONLY = "read_only";
        DENY_LIST_KEY = "deny_list";
    }
}
