/*
  FUSE: Filesystem in Userspace
  Copyright (C) 2001-2007  Miklos Szeredi <miklos@szeredi.hu>

  This program can be distributed under the terms of the GNU LGPLv2.
  See the file COPYING.LIB
*/

#include <cstdint>
#include <pthread.h>

#include "fuse_lowlevel.h"

struct mount_opts;

struct fuse_req {
    struct fuse_session *se;
    uint64_t unique;
    int ctr;
    pthread_mutex_t lock;
    struct fuse_ctx ctx;
    struct fuse_chan *ch;
    int interrupted;
    unsigned int ioctl_64bit: 1;
    union {
        struct {
            uint64_t unique;
        } i;
        struct {
            fuse_interrupt_func_t func;
            void *data;
        } ni;
    } u;
    struct fuse_req *next;
    struct fuse_req *prev;
};
