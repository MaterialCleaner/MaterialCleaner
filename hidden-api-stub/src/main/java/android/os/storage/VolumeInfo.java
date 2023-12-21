/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.os.storage;

import android.annotation.NonNull;
import android.annotation.Nullable;

import java.util.Locale;

public class VolumeInfo {
    public static final int TYPE_EMULATED = 2;

    public static final int STATE_UNMOUNTED = 0;
    public static final int STATE_CHECKING = 1;
    public static final int STATE_MOUNTED = 2;

    /**
     * vold state
     */
    public final String id;
    public final int type;
    public final DiskInfo disk;
    public final String partGuid;
    public int mountFlags;
    public int mountUserId;
    public int state;
    public String fsType;
    public String fsUuid;
    public String fsLabel;
    public String path;
    public String internalPath;

    public VolumeInfo(VolumeInfo volumeInfo) {
        this.id = volumeInfo.id;
        this.type = volumeInfo.type;
        this.disk = volumeInfo.disk;
        this.partGuid = volumeInfo.partGuid;
        this.mountFlags = volumeInfo.mountFlags;
        this.mountUserId = volumeInfo.mountUserId;
        this.state = volumeInfo.state;
        this.fsType = volumeInfo.fsType;
        this.fsUuid = volumeInfo.fsUuid;
        this.fsLabel = volumeInfo.fsLabel;
        this.path = volumeInfo.path;
        this.internalPath = volumeInfo.internalPath;
    }

    public @NonNull String getId() {
        return id;
    }

    public int getType() {
        return type;
    }

    public int getState() {
        return state;
    }

    public @Nullable String getFsUuid() {
        return fsUuid;
    }

    public @Nullable String getNormalizedFsUuid() {
        return fsUuid != null ? fsUuid.toLowerCase(Locale.US) : null;
    }

    public int getMountUserId() {
        return mountUserId;
    }
}
