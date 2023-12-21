#include <cstdio>
#include <cstdlib>
#include <unistd.h>
#include <cstring>
#include <sys/system_properties.h>
#include <cerrno>
#include "android.h"
#include "misc.h"
#include "selinux.h"
#include "cgroup.h"
#include "logging.h"

#ifdef DEBUG
#define JAVA_DEBUGGABLE
#endif

#undef LOG_TAG
#define LOG_TAG "starter"

#define perrorf(...) fprintf(stderr, __VA_ARGS__)

#define PACKAGE_NAME "me.gm.cleaner"
#define SERVER_NAME "cleaner_server"
#define SERVER_CLASS_PATH "me.gm.cleaner.server.CleanerServerLoader"
#define FAILURE_PREFIX "Failure:"

static void run_server(const char *dex_path, const char *main_class, const char *process_name) {
    if (setenv("CLASSPATH", dex_path, true)) {
        exit(1);
    }

#define ARG(v) char **v = nullptr; \
    char buf_##v[PATH_MAX]; \
    size_t v_size = 0; \
    uintptr_t v_current = 0;
#define ARG_PUSH(v, arg) v_size += sizeof(char *); \
if (v == nullptr) { \
    v = (char **) malloc(v_size); \
} else { \
    v = (char **) realloc(v, v_size);\
} \
v_current = (uintptr_t) v + v_size - sizeof(char *); \
*((char **) v_current) = arg ? strdup(arg) : nullptr;

#define ARG_END(v) ARG_PUSH(v, nullptr)

#define ARG_PUSH_FMT(v, fmt, ...) snprintf(buf_##v, PATH_MAX, fmt, __VA_ARGS__); \
    ARG_PUSH(v, buf_##v)

#ifdef JAVA_DEBUGGABLE
#define ARG_PUSH_DEBUG_ONLY(v, arg) ARG_PUSH(v, arg)
#define ARG_PUSH_DEBUG_VM_PARAMS(v) \
    if (android::GetApiLevel() >= 30) { \
        ARG_PUSH(v, "-Xcompiler-option"); \
        ARG_PUSH(v, "--debuggable"); \
        ARG_PUSH(v, "-XjdwpProvider:adbconnection"); \
        ARG_PUSH(v, "-XjdwpOptions:suspend=n,server=y"); \
    } else if (android::GetApiLevel() >= 28) { \
        ARG_PUSH(v, "-Xcompiler-option"); \
        ARG_PUSH(v, "--debuggable"); \
        ARG_PUSH(v, "-XjdwpProvider:internal"); \
        ARG_PUSH(v, "-XjdwpOptions:transport=dt_android_adb,suspend=n,server=y"); \
    } else { \
        ARG_PUSH(v, "-Xcompiler-option"); \
        ARG_PUSH(v, "--debuggable"); \
        ARG_PUSH(v, "-agentlib:jdwp=transport=dt_android_adb,suspend=n,server=y"); \
    }
#else
#define ARG_PUSH_DEBUG_VM_PARAMS(v)
#define ARG_PUSH_DEBUG_ONLY(v, arg)
#endif

    ARG(argv)
    ARG_PUSH(argv, "/system/bin/app_process")
    ARG_PUSH_FMT(argv, "-Djava.class.path=%s", dex_path)
    ARG_PUSH_DEBUG_VM_PARAMS(argv)
    ARG_PUSH(argv, "/system/bin")
    ARG_PUSH_FMT(argv, "--nice-name=%s", process_name)
    ARG_PUSH(argv, main_class)
    ARG_PUSH_DEBUG_ONLY(argv, "--debug")
    ARG_END(argv)

    if (execvp((const char *) argv[0], argv)) {
        exit(1);
    }
}

static void start_server(const char *path, const char *main_class, const char *process_name) {
    if (daemon(false, false) == 0) {
        run_server(path, main_class, process_name);
    } else {
        printf("%s Can't create daemon\n", FAILURE_PREFIX);
        fflush(stdout);
        exit(1);
    }
}

static int check_selinux(const char *s, const char *t, const char *c, const char *p) {
    int res = se::selinux_check_access(s, t, c, p, nullptr);
#ifdef DEBUG
    if (res != 0) {
        printf("info: selinux_check_access %s %s %s %s: %d\n", s, t, c, p, res);
        fflush(stdout);
    }
#endif
    return res;
}

static int switch_cgroup() {
    int s_cuid, s_cpid;
    int spid = getpid();

    if (cgroup::get_cgroup(spid, &s_cuid, &s_cpid) != 0) {
        return -1;
    }
#ifdef DEBUG
    printf("info: cgroup is /uid_%d/pid_%d\n", s_cuid, s_cpid);
    fflush(stdout);
#endif
    if (cgroup::switch_cgroup(spid, -1, -1) != 0) {
        return -1;
    }
    if (cgroup::get_cgroup(spid, &s_cuid, &s_cpid) != 0) {
        return 0;
    }
    return -1;
}

char *context = nullptr;

int main(int argc, char **argv) {
    char *apk_path = nullptr;
    for (int i = 0; i < argc; ++i) {
        if (strncmp(argv[i], "--apk=", 6) == 0) {
            apk_path = argv[i] + 6;
        }
    }

    if (getuid() != 0) {
        printf("%s Can't access root\n", FAILURE_PREFIX);
        fflush(stdout);
        exit(1);
    }

    se::init();

    chown("/data/local/tmp/cleaner_starter", 2000, 2000);
    se::setfilecon("/data/local/tmp/cleaner_starter", "u:object_r:shell_data_file:s0");
    switch_cgroup();

    if (android::GetApiLevel() >= 29) {
        switch_mnt_ns(1);
    }

    /*
    if (se::getcon(&context) == 0) {
        int res = 0;

        res |= check_selinux("u:r:untrusted_app:s0", context, "binder", "call");
        res |= check_selinux("u:r:untrusted_app:s0", context, "binder", "transfer");

        if (res != 0) {
            perrorf("fatal: the su you are using does not allow app (u:r:untrusted_app:s0) to connect to su (%s) with binder.\n",
                    context);
            exit(1);
        }
        se::freecon(context);
    }
    */

    foreach_proc([](pid_t pid) {
        if (pid == getpid()) return;

        char name[1024];
        if (get_proc_name(pid, name, 1024) != 0) return;

        if (strcmp(SERVER_NAME, name) != 0) return;

        if (TEMP_FAILURE_RETRY(kill(pid, SIGKILL))) {
            printf("%s Can't kill %d, please kill it manually or restart your phone.\n",
                   FAILURE_PREFIX, pid);
            fflush(stdout);
            exit(1);
        }
    });

    if (access(apk_path, R_OK) != 0) {
        printf("%s Can't access %s\n", FAILURE_PREFIX, apk_path);
        fflush(stdout);
        exit(1);
    }

    start_server(apk_path, SERVER_CLASS_PATH, SERVER_NAME);
    exit(0);
}
