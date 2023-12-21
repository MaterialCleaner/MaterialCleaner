/*
  FUSE: Filesystem in Userspace
  Copyright (C) 2001-2007  Miklos Szeredi <miklos@szeredi.hu>

  This program can be distributed under the terms of the GNU LGPLv2.
  See the file COPYING.LIB.
*/

#ifndef FUSE_LOWLEVEL_H_
#define FUSE_LOWLEVEL_H_

/** @file
 *
 * Low level API
 *
 * IMPORTANT: you should define FUSE_USE_VERSION before including this
 * header.  To use the newest API define it to 35 (recommended for any
 * new application).
 */

#include <utime.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/statvfs.h>
#include <sys/uio.h>

#ifdef __cplusplus
extern "C" {
#endif

/* ----------------------------------------------------------- *
 * Miscellaneous definitions				       *
 * ----------------------------------------------------------- */

/** The node ID of the root inode */
#define FUSE_ROOT_ID 1

/** Inode number type */
typedef uint64_t fuse_ino_t;

/** Request pointer type */
typedef struct fuse_req *fuse_req_t;

/**
 * Session
 *
 * This provides hooks for processing requests, and exiting
 */
struct fuse_session;

/** Directory entry parameters supplied to fuse_reply_entry() */
struct fuse_entry_param {
    /** Unique inode number
     *
     * In lookup, zero means negative entry (from version 2.5)
     * Returning ENOENT also means negative entry, but by setting zero
     * ino the kernel may cache negative entries for entry_timeout
     * seconds.
     */
    fuse_ino_t ino;

    /** Generation number for this entry.
     *
     * If the file system will be exported over NFS, the
     * ino/generation pairs need to be unique over the file
     * system's lifetime (rather than just the mount time). So if
     * the file system reuses an inode after it has been deleted,
     * it must assign a new, previously unused generation number
     * to the inode at the same time.
     *
     */
    uint64_t generation;

    /** Inode attributes.
     *
     * Even if attr_timeout == 0, attr must be correct. For example,
     * for open(), FUSE uses attr.st_size from lookup() to determine
     * how many bytes to request. If this value is not correct,
     * incorrect data will be returned.
     */
    struct stat attr;

    /** Validity timeout (in seconds) for inode attributes. If
        attributes only change as a result of requests that come
        through the kernel, this should be set to a very large
        value. */
    double attr_timeout;

    /** Validity timeout (in seconds) for the name. If directory
        entries are changed/deleted only as a result of requests
        that come through the kernel, this should be set to a very
        large value. */
    double entry_timeout;
    uint64_t backing_action;
    uint64_t backing_fd;
    uint64_t bpf_action;
    uint64_t bpf_fd;
};

/**
 * Additional context associated with requests.
 *
 * Note that the reported client uid, gid and pid may be zero in some
 * situations. For example, if the FUSE file system is running in a
 * PID or user namespace but then accessed from outside the namespace,
 * there is no valid uid/pid/gid that could be reported.
 */
struct fuse_ctx {
    /** User ID of the calling process */
    uid_t uid;

    /** Group ID of the calling process */
    gid_t gid;

    /** Thread ID of the calling process */
    pid_t pid;

    /** Umask of the calling process */
    mode_t umask;
};

struct fuse_forget_data {
    fuse_ino_t ino;
    uint64_t nlookup;
};

/* 'to_set' flags in setattr */
#define FUSE_SET_ATTR_MODE    (1 << 0)
#define FUSE_SET_ATTR_UID    (1 << 1)
#define FUSE_SET_ATTR_GID    (1 << 2)
#define FUSE_SET_ATTR_SIZE    (1 << 3)
#define FUSE_SET_ATTR_ATIME    (1 << 4)
#define FUSE_SET_ATTR_MTIME    (1 << 5)
#define FUSE_SET_ATTR_ATIME_NOW    (1 << 7)
#define FUSE_SET_ATTR_MTIME_NOW    (1 << 8)
#define FUSE_SET_ATTR_CTIME    (1 << 10)

/* ----------------------------------------------------------- *
 * structs from fuse_kernel.h                                  *
 * ----------------------------------------------------------- */
struct fuse_entry_out;
struct fuse_entry_bpf_out;

/* ----------------------------------------------------------- *
 * Request methods and replies				       *
 * ----------------------------------------------------------- */

/**
 * Low level filesystem operations
 *
 * Most of the methods (with the exception of init and destroy)
 * receive a request handle (fuse_req_t) as their first argument.
 * This handle must be passed to one of the specified reply functions.
 *
 * This may be done inside the method invocation, or after the call
 * has returned.  The request handle is valid until one of the reply
 * functions is called.
 *
 * Other pointer arguments (name, fuse_file_info, etc) are not valid
 * after the call has returned, so if they are needed later, their
 * contents have to be copied.
 *
 * In general, all methods are expected to perform any necessary
 * permission checking. However, a filesystem may delegate this task
 * to the kernel by passing the `default_permissions` mount option to
 * `fuse_session_new()`. In this case, methods will only be called if
 * the kernel's permission check has succeeded.
 *
 * The filesystem sometimes needs to handle a return value of -ENOENT
 * from the reply function, which means, that the request was
 * interrupted, and the reply discarded.  For example if
 * fuse_reply_open() return -ENOENT means, that the release method for
 * this file will not be called.
 */
struct fuse_lowlevel_ops {
    /**
     * Initialize filesystem
     *
     * This function is called when libfuse establishes
     * communication with the FUSE kernel module. The file system
     * should use this module to inspect and/or modify the
     * connection parameters provided in the `conn` structure.
     *
     * Note that some parameters may be overwritten by options
     * passed to fuse_session_new() which take precedence over the
     * values set in this handler.
     *
     * There's no reply to this function
     *
     * @param userdata the user data passed to fuse_session_new()
     */
    void (*init)(void *userdata, struct fuse_conn_info *conn);

    /**
     * Clean up filesystem.
     *
     * Called on filesystem exit. When this method is called, the
     * connection to the kernel may be gone already, so that eg. calls
     * to fuse_lowlevel_notify_* will fail.
     *
     * There's no reply to this function
     *
     * @param userdata the user data passed to fuse_session_new()
     */
    void (*destroy)(void *userdata);

    /**
     * Look up a directory entry by name and get its attributes.
     *
     * Valid replies:
     *   fuse_reply_entry
     *   fuse_reply_err
     *
     * @param req request handle
     * @param parent inode number of the parent directory
     * @param name the name to look up
     */
    void (*lookup)(fuse_req_t req, fuse_ino_t parent, const char *name);

    /**
     * post filter a lookup
     *
     * Valid replies:
     *   fuse_reply_entry
     *   fuse_reply_err
     *
     * @param req request handle
     * @param parent inode number of the parent directory
     * @param error_in the error, or 0, of the lookup
     * @param name the name that was looked up
     * @param feo the fuse entry out struct from the lookup
     * @param febo the fuse entry bpf out struct from the lookup
     */
    void (*lookup_postfilter)(fuse_req_t req, fuse_ino_t parent,
                              uint32_t error_in, const char *name,
                              struct fuse_entry_out *feo,
                              struct fuse_entry_bpf_out *febo);

    /**
     * Forget about an inode
     *
     * This function is called when the kernel removes an inode
     * from its internal caches.
     *
     * The inode's lookup count increases by one for every call to
     * fuse_reply_entry and fuse_reply_create. The nlookup parameter
     * indicates by how much the lookup count should be decreased.
     *
     * Inodes with a non-zero lookup count may receive request from
     * the kernel even after calls to unlink, rmdir or (when
     * overwriting an existing file) rename. Filesystems must handle
     * such requests properly and it is recommended to defer removal
     * of the inode until the lookup count reaches zero. Calls to
     * unlink, rmdir or rename will be followed closely by forget
     * unless the file or directory is open, in which case the
     * kernel issues forget only after the release or releasedir
     * calls.
     *
     * Note that if a file system will be exported over NFS the
     * inodes lifetime must extend even beyond forget. See the
     * generation field in struct fuse_entry_param above.
     *
     * On unmount the lookup count for all inodes implicitly drops
     * to zero. It is not guaranteed that the file system will
     * receive corresponding forget messages for the affected
     * inodes.
     *
     * Valid replies:
     *   fuse_reply_none
     *
     * @param req request handle
     * @param ino the inode number
     * @param nlookup the number of lookups to forget
     */
    void (*forget)(fuse_req_t req, fuse_ino_t ino, uint64_t nlookup);

    /**
     * Get file attributes.
     *
     * If writeback caching is enabled, the kernel may have a
     * better idea of a file's length than the FUSE file system
     * (eg if there has been a write that extended the file size,
     * but that has not yet been passed to the filesystem.n
     *
     * In this case, the st_size value provided by the file system
     * will be ignored.
     *
     * Valid replies:
     *   fuse_reply_attr
     *   fuse_reply_err
     *
     * @param req request handle
     * @param ino the inode number
     * @param fi for future use, currently always NULL
     */
    void (*getattr)(fuse_req_t req, fuse_ino_t ino,
                    struct fuse_file_info *fi);

    /**
     * Set file attributes
     *
     * In the 'attr' argument only members indicated by the 'to_set'
     * bitmask contain valid values.  Other members contain undefined
     * values.
     *
     * Unless FUSE_CAP_HANDLE_KILLPRIV is disabled, this method is
     * expected to reset the setuid and setgid bits if the file
     * size or owner is being changed.
     *
     * If the setattr was invoked from the ftruncate() system call
     * under Linux kernel versions 2.6.15 or later, the fi->fh will
     * contain the value set by the open method or will be undefined
     * if the open method didn't set any value.  Otherwise (not
     * ftruncate call, or kernel version earlier than 2.6.15) the fi
     * parameter will be NULL.
     *
     * Valid replies:
     *   fuse_reply_attr
     *   fuse_reply_err
     *
     * @param req request handle
     * @param ino the inode number
     * @param attr the attributes
     * @param to_set bit mask of attributes which should be set
     * @param fi file information, or NULL
     */
    void (*setattr)(fuse_req_t req, fuse_ino_t ino, struct stat *attr,
                    int to_set, struct fuse_file_info *fi);

    /**
     * Read symbolic link
     *
     * Valid replies:
     *   fuse_reply_readlink
     *   fuse_reply_err
     *
     * @param req request handle
     * @param ino the inode number
     */
    void (*readlink)(fuse_req_t req, fuse_ino_t ino);

    /**
 * Return canonical path for inotify
 *
 * Valid replies:
 *   fuse_reply_canonical_path
 *   fuse_reply_err
 *
 * @param req request handle
 * @param ino the inode number
 */
    void (*canonical_path)(fuse_req_t req, fuse_ino_t ino);

    /**
     * Create file node
     *
     * Create a regular file, character device, block device, fifo or
     * socket node.
     *
     * Valid replies:
     *   fuse_reply_entry
     *   fuse_reply_err
     *
     * @param req request handle
     * @param parent inode number of the parent directory
     * @param name to create
     * @param mode file type and mode with which to create the new file
     * @param rdev the device number (only valid if created file is a device)
     */
    void (*mknod)(fuse_req_t req, fuse_ino_t parent, const char *name,
                  mode_t mode, dev_t rdev);

    /**
     * Create a directory
     *
     * Valid replies:
     *   fuse_reply_entry
     *   fuse_reply_err
     *
     * @param req request handle
     * @param parent inode number of the parent directory
     * @param name to create
     * @param mode with which to create the new file
     */
    void (*mkdir)(fuse_req_t req, fuse_ino_t parent, const char *name,
                  mode_t mode);

    /**
     * Remove a file
     *
     * If the file's inode's lookup count is non-zero, the file
     * system is expected to postpone any removal of the inode
     * until the lookup count reaches zero (see description of the
     * forget function).
     *
     * Valid replies:
     *   fuse_reply_err
     *
     * @param req request handle
     * @param parent inode number of the parent directory
     * @param name to remove
     */
    void (*unlink)(fuse_req_t req, fuse_ino_t parent, const char *name);

    /**
     * Remove a directory
     *
     * If the directory's inode's lookup count is non-zero, the
     * file system is expected to postpone any removal of the
     * inode until the lookup count reaches zero (see description
     * of the forget function).
     *
     * Valid replies:
     *   fuse_reply_err
     *
     * @param req request handle
     * @param parent inode number of the parent directory
     * @param name to remove
     */
    void (*rmdir)(fuse_req_t req, fuse_ino_t parent, const char *name);

    /**
     * Create a symbolic link
     *
     * Valid replies:
     *   fuse_reply_entry
     *   fuse_reply_err
     *
     * @param req request handle
     * @param link the contents of the symbolic link
     * @param parent inode number of the parent directory
     * @param name to create
     */
    void (*symlink)(fuse_req_t req, const char *link, fuse_ino_t parent,
                    const char *name);

    /** Rename a file
     *
     * If the target exists it should be atomically replaced. If
     * the target's inode's lookup count is non-zero, the file
     * system is expected to postpone any removal of the inode
     * until the lookup count reaches zero (see description of the
     * forget function).
     *
     * If this request is answered with an error code of ENOSYS, this is
     * treated as a permanent failure with error code EINVAL, i.e. all
     * future bmap requests will fail with EINVAL without being
     * send to the filesystem process.
     *
     * *flags* may be `RENAME_EXCHANGE` or `RENAME_NOREPLACE`. If
     * RENAME_NOREPLACE is specified, the filesystem must not
     * overwrite *newname* if it exists and return an error
     * instead. If `RENAME_EXCHANGE` is specified, the filesystem
     * must atomically exchange the two files, i.e. both must
     * exist and neither may be deleted.
     *
     * Valid replies:
     *   fuse_reply_err
     *
     * @param req request handle
     * @param parent inode number of the old parent directory
     * @param name old name
     * @param newparent inode number of the new parent directory
     * @param newname new name
     */
    void (*rename)(fuse_req_t req, fuse_ino_t parent, const char *name,
                   fuse_ino_t newparent, const char *newname,
                   unsigned int flags);

    /**
     * Create a hard link
     *
     * Valid replies:
     *   fuse_reply_entry
     *   fuse_reply_err
     *
     * @param req request handle
     * @param ino the old inode number
     * @param newparent inode number of the new parent directory
     * @param newname new name to create
     */
    void (*link)(fuse_req_t req, fuse_ino_t ino, fuse_ino_t newparent,
                 const char *newname);

    /**
     * Open a file
     *
     * Open flags are available in fi->flags. The following rules
     * apply.
     *
     *  - Creation (O_CREAT, O_EXCL, O_NOCTTY) flags will be
     *    filtered out / handled by the kernel.
     *
     *  - Access modes (O_RDONLY, O_WRONLY, O_RDWR) should be used
     *    by the filesystem to check if the operation is
     *    permitted.  If the ``-o default_permissions`` mount
     *    option is given, this check is already done by the
     *    kernel before calling open() and may thus be omitted by
     *    the filesystem.
     *
     *  - When writeback caching is enabled, the kernel may send
     *    read requests even for files opened with O_WRONLY. The
     *    filesystem should be prepared to handle this.
     *
     *  - When writeback caching is disabled, the filesystem is
     *    expected to properly handle the O_APPEND flag and ensure
     *    that each write is appending to the end of the file.
     *
         *  - When writeback caching is enabled, the kernel will
     *    handle O_APPEND. However, unless all changes to the file
     *    come through the kernel this will not work reliably. The
     *    filesystem should thus either ignore the O_APPEND flag
     *    (and let the kernel handle it), or return an error
     *    (indicating that reliably O_APPEND is not available).
     *
     * Filesystem may store an arbitrary file handle (pointer,
     * index, etc) in fi->fh, and use this in other all other file
     * operations (read, write, flush, release, fsync).
     *
     * Filesystem may also implement stateless file I/O and not store
     * anything in fi->fh.
     *
     * There are also some flags (direct_io, keep_cache) which the
     * filesystem may set in fi, to change the way the file is opened.
     * See fuse_file_info structure in <fuse_common.h> for more details.
     *
     * If this request is answered with an error code of ENOSYS
     * and FUSE_CAP_NO_OPEN_SUPPORT is set in
     * `fuse_conn_info.capable`, this is treated as success and
     * future calls to open and release will also succeed without being
     * sent to the filesystem process.
     *
     * Valid replies:
     *   fuse_reply_open
     *   fuse_reply_err
     *
     * @param req request handle
     * @param ino the inode number
     * @param fi file information
     */
    void (*open)(fuse_req_t req, fuse_ino_t ino,
                 struct fuse_file_info *fi);

    /**
     * Read data
     *
     * Read should send exactly the number of bytes requested except
     * on EOF or error, otherwise the rest of the data will be
     * substituted with zeroes.  An exception to this is when the file
     * has been opened in 'direct_io' mode, in which case the return
     * value of the read system call will reflect the return value of
     * this operation.
     *
     * fi->fh will contain the value set by the open method, or will
     * be undefined if the open method didn't set any value.
     *
     * Valid replies:
     *   fuse_reply_buf
     *   fuse_reply_iov
     *   fuse_reply_data
     *   fuse_reply_err
     *
     * @param req request handle
     * @param ino the inode number
     * @param size number of bytes to read
     * @param off offset to read from
     * @param fi file information
     */
    void (*read)(fuse_req_t req, fuse_ino_t ino, size_t size, off_t off,
                 struct fuse_file_info *fi);

    /**
     * Write data
     *
     * Write should return exactly the number of bytes requested
     * except on error.  An exception to this is when the file has
     * been opened in 'direct_io' mode, in which case the return value
     * of the write system call will reflect the return value of this
     * operation.
     *
     * Unless FUSE_CAP_HANDLE_KILLPRIV is disabled, this method is
     * expected to reset the setuid and setgid bits.
     *
     * fi->fh will contain the value set by the open method, or will
     * be undefined if the open method didn't set any value.
     *
     * Valid replies:
     *   fuse_reply_write
     *   fuse_reply_err
     *
     * @param req request handle
     * @param ino the inode number
     * @param buf data to write
     * @param size number of bytes to write
     * @param off offset to write to
     * @param fi file information
     */
    void (*write)(fuse_req_t req, fuse_ino_t ino, const char *buf,
                  size_t size, off_t off, struct fuse_file_info *fi);

    /**
     * Flush method
     *
     * This is called on each close() of the opened file.
     *
     * Since file descriptors can be duplicated (dup, dup2, fork), for
     * one open call there may be many flush calls.
     *
     * Filesystems shouldn't assume that flush will always be called
     * after some writes, or that if will be called at all.
     *
     * fi->fh will contain the value set by the open method, or will
     * be undefined if the open method didn't set any value.
     *
     * NOTE: the name of the method is misleading, since (unlike
     * fsync) the filesystem is not forced to flush pending writes.
     * One reason to flush data is if the filesystem wants to return
     * write errors during close.  However, such use is non-portable
     * because POSIX does not require [close] to wait for delayed I/O to
     * complete.
     *
     * If the filesystem supports file locking operations (setlk,
     * getlk) it should remove all locks belonging to 'fi->owner'.
     *
     * If this request is answered with an error code of ENOSYS,
     * this is treated as success and future calls to flush() will
     * succeed automatically without being send to the filesystem
     * process.
     *
     * Valid replies:
     *   fuse_reply_err
     *
     * @param req request handle
     * @param ino the inode number
     * @param fi file information
     *
     * [close]: http://pubs.opengroup.org/onlinepubs/9699919799/functions/close.html
     */
    void (*flush)(fuse_req_t req, fuse_ino_t ino,
                  struct fuse_file_info *fi);

    /**
     * Release an open file
     *
     * Release is called when there are no more references to an open
     * file: all file descriptors are closed and all memory mappings
     * are unmapped.
     *
     * For every open call there will be exactly one release call (unless
     * the filesystem is force-unmounted).
     *
     * The filesystem may reply with an error, but error values are
     * not returned to close() or munmap() which triggered the
     * release.
     *
     * fi->fh will contain the value set by the open method, or will
     * be undefined if the open method didn't set any value.
     * fi->flags will contain the same flags as for open.
     *
     * Valid replies:
     *   fuse_reply_err
     *
     * @param req request handle
     * @param ino the inode number
     * @param fi file information
     */
    void (*release)(fuse_req_t req, fuse_ino_t ino,
                    struct fuse_file_info *fi);

    /**
     * Synchronize file contents
     *
     * If the datasync parameter is non-zero, then only the user data
     * should be flushed, not the meta data.
     *
     * If this request is answered with an error code of ENOSYS,
     * this is treated as success and future calls to fsync() will
     * succeed automatically without being send to the filesystem
     * process.
     *
     * Valid replies:
     *   fuse_reply_err
     *
     * @param req request handle
     * @param ino the inode number
     * @param datasync flag indicating if only data should be flushed
     * @param fi file information
     */
    void (*fsync)(fuse_req_t req, fuse_ino_t ino, int datasync,
                  struct fuse_file_info *fi);

    /**
     * Open a directory
     *
     * Filesystem may store an arbitrary file handle (pointer, index,
     * etc) in fi->fh, and use this in other all other directory
     * stream operations (readdir, releasedir, fsyncdir).
     *
     * If this request is answered with an error code of ENOSYS and
     * FUSE_CAP_NO_OPENDIR_SUPPORT is set in `fuse_conn_info.capable`,
     * this is treated as success and future calls to opendir and
     * releasedir will also succeed without being sent to the filesystem
     * process. In addition, the kernel will cache readdir results
     * as if opendir returned FOPEN_KEEP_CACHE | FOPEN_CACHE_DIR.
     *
     * Valid replies:
     *   fuse_reply_open
     *   fuse_reply_err
     *
     * @param req request handle
     * @param ino the inode number
     * @param fi file information
     */
    void (*opendir)(fuse_req_t req, fuse_ino_t ino,
                    struct fuse_file_info *fi);

    /**
     * Read directory
     *
     * Send a buffer filled using fuse_add_direntry(), with size not
     * exceeding the requested size.  Send an empty buffer on end of
     * stream.
     *
     * fi->fh will contain the value set by the opendir method, or
     * will be undefined if the opendir method didn't set any value.
     *
     * Returning a directory entry from readdir() does not affect
     * its lookup count.
     *
         * If off_t is non-zero, then it will correspond to one of the off_t
     * values that was previously returned by readdir() for the same
     * directory handle. In this case, readdir() should skip over entries
     * coming before the position defined by the off_t value. If entries
     * are added or removed while the directory handle is open, the filesystem
     * may still include the entries that have been removed, and may not
     * report the entries that have been created. However, addition or
     * removal of entries must never cause readdir() to skip over unrelated
     * entries or to report them more than once. This means
     * that off_t can not be a simple index that enumerates the entries
     * that have been returned but must contain sufficient information to
     * uniquely determine the next directory entry to return even when the
     * set of entries is changing.
     *
     * The function does not have to report the '.' and '..'
     * entries, but is allowed to do so. Note that, if readdir does
     * not return '.' or '..', they will not be implicitly returned,
     * and this behavior is observable by the caller.
     *
     * Valid replies:
     *   fuse_reply_buf
     *   fuse_reply_data
     *   fuse_reply_err
     *
     * @param req request handle
     * @param ino the inode number
     * @param size maximum number of bytes to send
     * @param off offset to continue reading the directory stream
     * @param fi file information
     */
    void (*readdir)(fuse_req_t req, fuse_ino_t ino, size_t size, off_t off,
                    struct fuse_file_info *fi);

    /**
     * Read directory postfilter
     *
     * Valid replies:
     *   fuse_reply_buf
     *   fuse_reply_data
     *   fuse_reply_err
     *
     * @param req request handle
     * @param ino the inode number
     * @param error_in the error from the readdir
     * @param off_in offset to continue reading the directory stream before backing
     * @param off_out offset to continue reading the directory stream after backing
     * @param size_out length in bytes of dirents
     * @param dirents array of dirents read by backing
     * @param fi file information
     */
    void (*readdirpostfilter)(fuse_req_t req, fuse_ino_t ino, uint32_t error_in,
                              off_t off_in, off_t off_out, size_t size_out,
                              const void *dirents, struct fuse_file_info *fi);

    /**
     * Release an open directory
     *
     * For every opendir call there will be exactly one releasedir
     * call (unless the filesystem is force-unmounted).
     *
     * fi->fh will contain the value set by the opendir method, or
     * will be undefined if the opendir method didn't set any value.
     *
     * Valid replies:
     *   fuse_reply_err
     *
     * @param req request handle
     * @param ino the inode number
     * @param fi file information
     */
    void (*releasedir)(fuse_req_t req, fuse_ino_t ino,
                       struct fuse_file_info *fi);

    /**
     * Synchronize directory contents
     *
     * If the datasync parameter is non-zero, then only the directory
     * contents should be flushed, not the meta data.
     *
     * fi->fh will contain the value set by the opendir method, or
     * will be undefined if the opendir method didn't set any value.
     *
     * If this request is answered with an error code of ENOSYS,
     * this is treated as success and future calls to fsyncdir() will
     * succeed automatically without being send to the filesystem
     * process.
     *
     * Valid replies:
     *   fuse_reply_err
     *
     * @param req request handle
     * @param ino the inode number
     * @param datasync flag indicating if only data should be flushed
     * @param fi file information
     */
    void (*fsyncdir)(fuse_req_t req, fuse_ino_t ino, int datasync,
                     struct fuse_file_info *fi);

    /**
     * Get file system statistics
     *
     * Valid replies:
     *   fuse_reply_statfs
     *   fuse_reply_err
     *
     * @param req request handle
     * @param ino the inode number, zero means "undefined"
     */
    void (*statfs)(fuse_req_t req, fuse_ino_t ino);

    /**
     * Set an extended attribute
     *
     * If this request is answered with an error code of ENOSYS, this is
     * treated as a permanent failure with error code EOPNOTSUPP, i.e. all
     * future setxattr() requests will fail with EOPNOTSUPP without being
     * send to the filesystem process.
     *
     * Valid replies:
     *   fuse_reply_err
     */
    void (*setxattr)(fuse_req_t req, fuse_ino_t ino, const char *name,
                     const char *value, size_t size, int flags);

    /**
     * Get an extended attribute
     *
     * If size is zero, the size of the value should be sent with
     * fuse_reply_xattr.
     *
     * If the size is non-zero, and the value fits in the buffer, the
     * value should be sent with fuse_reply_buf.
     *
     * If the size is too small for the value, the ERANGE error should
     * be sent.
     *
     * If this request is answered with an error code of ENOSYS, this is
     * treated as a permanent failure with error code EOPNOTSUPP, i.e. all
     * future getxattr() requests will fail with EOPNOTSUPP without being
     * send to the filesystem process.
     *
     * Valid replies:
     *   fuse_reply_buf
     *   fuse_reply_data
     *   fuse_reply_xattr
     *   fuse_reply_err
     *
     * @param req request handle
     * @param ino the inode number
     * @param name of the extended attribute
     * @param size maximum size of the value to send
     */
    void (*getxattr)(fuse_req_t req, fuse_ino_t ino, const char *name,
                     size_t size);

    /**
     * List extended attribute names
     *
     * If size is zero, the total size of the attribute list should be
     * sent with fuse_reply_xattr.
     *
     * If the size is non-zero, and the null character separated
     * attribute list fits in the buffer, the list should be sent with
     * fuse_reply_buf.
     *
     * If the size is too small for the list, the ERANGE error should
     * be sent.
     *
     * If this request is answered with an error code of ENOSYS, this is
     * treated as a permanent failure with error code EOPNOTSUPP, i.e. all
     * future listxattr() requests will fail with EOPNOTSUPP without being
     * send to the filesystem process.
     *
     * Valid replies:
     *   fuse_reply_buf
     *   fuse_reply_data
     *   fuse_reply_xattr
     *   fuse_reply_err
     *
     * @param req request handle
     * @param ino the inode number
     * @param size maximum size of the list to send
     */
    void (*listxattr)(fuse_req_t req, fuse_ino_t ino, size_t size);

    /**
     * Remove an extended attribute
     *
     * If this request is answered with an error code of ENOSYS, this is
     * treated as a permanent failure with error code EOPNOTSUPP, i.e. all
     * future removexattr() requests will fail with EOPNOTSUPP without being
     * send to the filesystem process.
     *
     * Valid replies:
     *   fuse_reply_err
     *
     * @param req request handle
     * @param ino the inode number
     * @param name of the extended attribute
     */
    void (*removexattr)(fuse_req_t req, fuse_ino_t ino, const char *name);

    /**
     * Check file access permissions
     *
     * This will be called for the access() and chdir() system
     * calls.  If the 'default_permissions' mount option is given,
     * this method is not called.
     *
     * This method is not called under Linux kernel versions 2.4.x
     *
     * If this request is answered with an error code of ENOSYS, this is
     * treated as a permanent success, i.e. this and all future access()
     * requests will succeed without being send to the filesystem process.
     *
     * Valid replies:
     *   fuse_reply_err
     *
     * @param req request handle
     * @param ino the inode number
     * @param mask requested access mode
     */
    void (*access)(fuse_req_t req, fuse_ino_t ino, int mask);

    /**
     * Create and open a file
     *
     * If the file does not exist, first create it with the specified
     * mode, and then open it.
     *
     * See the description of the open handler for more
     * information.
     *
     * If this method is not implemented or under Linux kernel
     * versions earlier than 2.6.15, the mknod() and open() methods
     * will be called instead.
     *
     * If this request is answered with an error code of ENOSYS, the handler
     * is treated as not implemented (i.e., for this and future requests the
     * mknod() and open() handlers will be called instead).
     *
     * Valid replies:
     *   fuse_reply_create
     *   fuse_reply_err
     *
     * @param req request handle
     * @param parent inode number of the parent directory
     * @param name to create
     * @param mode file type and mode with which to create the new file
     * @param fi file information
     */
    void (*create)(fuse_req_t req, fuse_ino_t parent, const char *name,
                   mode_t mode, struct fuse_file_info *fi);

    /**
     * Test for a POSIX file lock
     *
     * Valid replies:
     *   fuse_reply_lock
     *   fuse_reply_err
     *
     * @param req request handle
     * @param ino the inode number
     * @param fi file information
     * @param lock the region/type to test
     */
    void (*getlk)(fuse_req_t req, fuse_ino_t ino,
                  struct fuse_file_info *fi, struct flock *lock);

    /**
     * Acquire, modify or release a POSIX file lock
     *
     * For POSIX threads (NPTL) there's a 1-1 relation between pid and
     * owner, but otherwise this is not always the case.  For checking
     * lock ownership, 'fi->owner' must be used.  The l_pid field in
     * 'struct flock' should only be used to fill in this field in
     * getlk().
     *
     * Note: if the locking methods are not implemented, the kernel
     * will still allow file locking to work locally.  Hence these are
     * only interesting for network filesystems and similar.
     *
     * Valid replies:
     *   fuse_reply_err
     *
     * @param req request handle
     * @param ino the inode number
     * @param fi file information
     * @param lock the region/type to set
     * @param sleep locking operation may sleep
     */
    void (*setlk)(fuse_req_t req, fuse_ino_t ino,
                  struct fuse_file_info *fi,
                  struct flock *lock, int sleep);

    /**
     * Map block index within file to block index within device
     *
     * Note: This makes sense only for block device backed filesystems
     * mounted with the 'blkdev' option
     *
     * If this request is answered with an error code of ENOSYS, this is
     * treated as a permanent failure, i.e. all future bmap() requests will
     * fail with the same error code without being send to the filesystem
     * process.
     *
     * Valid replies:
     *   fuse_reply_bmap
     *   fuse_reply_err
     *
     * @param req request handle
     * @param ino the inode number
     * @param blocksize unit of block index
     * @param idx block index within file
     */
    void (*bmap)(fuse_req_t req, fuse_ino_t ino, size_t blocksize,
                 uint64_t idx);

#if FUSE_USE_VERSION < 35

    void (*ioctl)(fuse_req_t req, fuse_ino_t ino, int cmd,
                  void *arg, struct fuse_file_info *fi, unsigned flags,
                  const void *in_buf, size_t in_bufsz, size_t out_bufsz);

#else
    /**
     * Ioctl
     *
     * Note: For unrestricted ioctls (not allowed for FUSE
     * servers), data in and out areas can be discovered by giving
     * iovs and setting FUSE_IOCTL_RETRY in *flags*.  For
     * restricted ioctls, kernel prepares in/out data area
     * according to the information encoded in cmd.
     *
     * Valid replies:
     *   fuse_reply_ioctl_retry
     *   fuse_reply_ioctl
     *   fuse_reply_ioctl_iov
     *   fuse_reply_err
     *
     * @param req request handle
     * @param ino the inode number
     * @param cmd ioctl command
     * @param arg ioctl argument
     * @param fi file information
     * @param flags for FUSE_IOCTL_* flags
     * @param in_buf data fetched from the caller
     * @param in_bufsz number of fetched bytes
     * @param out_bufsz maximum size of output data
     *
     * Note : the unsigned long request submitted by the application
     * is truncated to 32 bits.
     */
    void (*ioctl) (fuse_req_t req, fuse_ino_t ino, unsigned int cmd,
               void *arg, struct fuse_file_info *fi, unsigned flags,
               const void *in_buf, size_t in_bufsz, size_t out_bufsz);
#endif

    /**
     * Poll for IO readiness
     *
     * Note: If ph is non-NULL, the client should notify
     * when IO readiness events occur by calling
     * fuse_lowlevel_notify_poll() with the specified ph.
     *
     * Regardless of the number of times poll with a non-NULL ph
     * is received, single notification is enough to clear all.
     * Notifying more times incurs overhead but doesn't harm
     * correctness.
     *
     * The callee is responsible for destroying ph with
     * fuse_pollhandle_destroy() when no longer in use.
     *
     * If this request is answered with an error code of ENOSYS, this is
     * treated as success (with a kernel-defined default poll-mask) and
     * future calls to pull() will succeed the same way without being send
     * to the filesystem process.
     *
     * Valid replies:
     *   fuse_reply_poll
     *   fuse_reply_err
     *
     * @param req request handle
     * @param ino the inode number
     * @param fi file information
     * @param ph poll handle to be used for notification
     */
    void (*poll)(fuse_req_t req, fuse_ino_t ino, struct fuse_file_info *fi,
                 struct fuse_pollhandle *ph);

    /**
     * Write data made available in a buffer
     *
     * This is a more generic version of the ->write() method.  If
     * FUSE_CAP_SPLICE_READ is set in fuse_conn_info.want and the
     * kernel supports splicing from the fuse device, then the
     * data will be made available in pipe for supporting zero
     * copy data transfer.
     *
     * buf->count is guaranteed to be one (and thus buf->idx is
     * always zero). The write_buf handler must ensure that
     * bufv->off is correctly updated (reflecting the number of
     * bytes read from bufv->buf[0]).
     *
     * Unless FUSE_CAP_HANDLE_KILLPRIV is disabled, this method is
     * expected to reset the setuid and setgid bits.
     *
     * Valid replies:
     *   fuse_reply_write
     *   fuse_reply_err
     *
     * @param req request handle
     * @param ino the inode number
     * @param bufv buffer containing the data
     * @param off offset to write to
     * @param fi file information
     */
    void (*write_buf)(fuse_req_t req, fuse_ino_t ino,
                      struct fuse_bufvec *bufv, off_t off,
                      struct fuse_file_info *fi);

    /**
     * Callback function for the retrieve request
     *
     * Valid replies:
     *	fuse_reply_none
     *
     * @param req request handle
     * @param cookie user data supplied to fuse_lowlevel_notify_retrieve()
     * @param ino the inode number supplied to fuse_lowlevel_notify_retrieve()
     * @param offset the offset supplied to fuse_lowlevel_notify_retrieve()
     * @param bufv the buffer containing the returned data
     */
    void (*retrieve_reply)(fuse_req_t req, void *cookie, fuse_ino_t ino,
                           off_t offset, struct fuse_bufvec *bufv);

    /**
     * Forget about multiple inodes
     *
     * See description of the forget function for more
     * information.
     *
     * Valid replies:
     *   fuse_reply_none
     *
     * @param req request handle
     */
    void (*forget_multi)(fuse_req_t req, size_t count,
                         struct fuse_forget_data *forgets);

    /**
     * Acquire, modify or release a BSD file lock
     *
     * Note: if the locking methods are not implemented, the kernel
     * will still allow file locking to work locally.  Hence these are
     * only interesting for network filesystems and similar.
     *
     * Valid replies:
     *   fuse_reply_err
     *
     * @param req request handle
     * @param ino the inode number
     * @param fi file information
     * @param op the locking operation, see flock(2)
     */
    void (*flock)(fuse_req_t req, fuse_ino_t ino,
                  struct fuse_file_info *fi, int op);

    /**
     * Allocate requested space. If this function returns success then
     * subsequent writes to the specified range shall not fail due to the lack
     * of free space on the file system storage media.
     *
     * If this request is answered with an error code of ENOSYS, this is
     * treated as a permanent failure with error code EOPNOTSUPP, i.e. all
     * future fallocate() requests will fail with EOPNOTSUPP without being
     * send to the filesystem process.
     *
     * Valid replies:
     *   fuse_reply_err
     *
     * @param req request handle
     * @param ino the inode number
     * @param offset starting point for allocated region
     * @param length size of allocated region
     * @param mode determines the operation to be performed on the given range,
     *             see fallocate(2)
     */
    void (*fallocate)(fuse_req_t req, fuse_ino_t ino, int mode,
                      off_t offset, off_t length, struct fuse_file_info *fi);

    /**
     * Read directory with attributes
     *
     * Send a buffer filled using fuse_add_direntry_plus(), with size not
     * exceeding the requested size.  Send an empty buffer on end of
     * stream.
     *
     * fi->fh will contain the value set by the opendir method, or
     * will be undefined if the opendir method didn't set any value.
     *
     * In contrast to readdir() (which does not affect the lookup counts),
     * the lookup count of every entry returned by readdirplus(), except "."
     * and "..", is incremented by one.
     *
     * Valid replies:
     *   fuse_reply_buf
     *   fuse_reply_data
     *   fuse_reply_err
     *
     * @param req request handle
     * @param ino the inode number
     * @param size maximum number of bytes to send
     * @param off offset to continue reading the directory stream
     * @param fi file information
     */
    void (*readdirplus)(fuse_req_t req, fuse_ino_t ino, size_t size, off_t off,
                        struct fuse_file_info *fi);

    /**
     * Copy a range of data from one file to another
     *
     * Performs an optimized copy between two file descriptors without the
     * additional cost of transferring data through the FUSE kernel module
     * to user space (glibc) and then back into the FUSE filesystem again.
     *
     * In case this method is not implemented, glibc falls back to reading
     * data from the source and writing to the destination. Effectively
     * doing an inefficient copy of the data.
     *
     * If this request is answered with an error code of ENOSYS, this is
     * treated as a permanent failure with error code EOPNOTSUPP, i.e. all
     * future copy_file_range() requests will fail with EOPNOTSUPP without
     * being send to the filesystem process.
     *
     * Valid replies:
     *   fuse_reply_write
     *   fuse_reply_err
     *
     * @param req request handle
     * @param ino_in the inode number or the source file
     * @param off_in starting point from were the data should be read
     * @param fi_in file information of the source file
     * @param ino_out the inode number or the destination file
     * @param off_out starting point where the data should be written
     * @param fi_out file information of the destination file
     * @param len maximum size of the data to copy
     * @param flags passed along with the copy_file_range() syscall
     */
    void (*copy_file_range)(fuse_req_t req, fuse_ino_t ino_in,
                            off_t off_in, struct fuse_file_info *fi_in,
                            fuse_ino_t ino_out, off_t off_out,
                            struct fuse_file_info *fi_out, size_t len,
                            int flags);

    /**
     * Find next data or hole after the specified offset
     *
     * If this request is answered with an error code of ENOSYS, this is
     * treated as a permanent failure, i.e. all future lseek() requests will
     * fail with the same error code without being send to the filesystem
     * process.
     *
     * Valid replies:
     *   fuse_reply_lseek
     *   fuse_reply_err
     *
     * @param req request handle
     * @param ino the inode number
     * @param off offset to start search from
     * @param whence either SEEK_DATA or SEEK_HOLE
     * @param fi file information
     */
    void (*lseek)(fuse_req_t req, fuse_ino_t ino, off_t off, int whence,
                  struct fuse_file_info *fi);
};

/* ----------------------------------------------------------- *
 * Utility functions					       *
 * ----------------------------------------------------------- */

/**
 * Get the userdata from the request
 *
 * @param req request handle
 * @return the user data passed to fuse_session_new()
 */
void *fuse_req_userdata(fuse_req_t req);

/**
 * Get the context from the request
 *
 * The pointer returned by this function will only be valid for the
 * request's lifetime
 *
 * @param req request handle
 * @return the context structure
 */
const struct fuse_ctx *fuse_req_ctx(fuse_req_t req);

/**
 * Callback function for an interrupt
 *
 * @param req interrupted request
 * @param data user data
 */
typedef void (*fuse_interrupt_func_t)(fuse_req_t req, void *data);

#ifdef __cplusplus
}
#endif

#endif /* FUSE_LOWLEVEL_H_ */
