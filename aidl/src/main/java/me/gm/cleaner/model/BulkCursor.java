/*
 * Copyright (C) 2011 The Android Open Source Project
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

package me.gm.cleaner.model;

import android.database.Cursor;
import android.os.BadParcelableException;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.function.Function;

/**
 * Transfer a large list of Parcelable objects across an IPC.  Splits into
 * multiple transactions if needed.
 *
 * @see BaseParceledListSlice
 */
public class BulkCursor<T extends Parcelable> extends BaseBulkCursor<T> {

    private static final String TAG = "BulkCursor";

    // Cache of previously looked up CREATOR.createFromParcel() methods for
    // particular classes.  Keys are the names of the classes, values are
    // Method objects.
    private static final HashMap<ClassLoader, HashMap<String, Creator<?>>>
            CREATORS = new HashMap<>();

    static {
        putCreator(FileSystemEvent.class.getName(), FileSystemEvent.CREATOR);
    }

    public static void putCreator(String name, Creator<?> creator) {
        HashMap<String, Creator<?>> map = CREATORS.get(null);
        if (map == null) {
            map = new HashMap<>();
            CREATORS.put(null, map);
        }
        map.put(name, creator);
    }

    public BulkCursor(Cursor cursor, Function<Cursor, T> creator) {
        super(cursor, creator);
    }

    private BulkCursor(Parcel in, ClassLoader loader) {
        super(in, loader);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    protected void writeElement(T parcelable, Parcel dest, int callFlags) {
        parcelable.writeToParcel(dest, callFlags);
    }

    @Override
    protected void writeParcelableCreator(T parcelable, Parcel dest) {
        String name = parcelable.getClass().getName();
        dest.writeString(name);
    }

    @Override
    protected Creator<?> readParcelableCreator(Parcel from, ClassLoader loader) {
        String name = from.readString();
        if (name == null) {
            return null;
        }
        Creator<?> creator;
        synchronized (CREATORS) {
            HashMap<String, Creator<?>> map = CREATORS.get(loader);
            if (map == null) {
                map = new HashMap<>();
                CREATORS.put(loader, map);
            }
            creator = map.get(name);
            if (creator == null) {
                try {
                    // If loader == null, explicitly emulate Class.forName(String) "caller
                    // classloader" behavior.
                    ClassLoader parcelableClassLoader =
                            (loader == null ? getClass().getClassLoader() : loader);
                    // Avoid initializing the Parcelable class until we know it implements
                    // Parcelable and has the necessary CREATOR field. http://b/1171613.
                    Class<?> parcelableClass = Class.forName(name, false /* initialize */,
                            parcelableClassLoader);
                    if (!Parcelable.class.isAssignableFrom(parcelableClass)) {
                        throw new BadParcelableException("Parcelable protocol requires subclassing "
                                + "from Parcelable on class " + name);
                    }
                    Field f = parcelableClass.getField("CREATOR");
                    if ((f.getModifiers() & Modifier.STATIC) == 0) {
                        throw new BadParcelableException("Parcelable protocol requires "
                                + "the CREATOR object to be static on class " + name);
                    }
                    Class<?> creatorType = f.getType();
                    if (!Creator.class.isAssignableFrom(creatorType)) {
                        // Fail before calling Field.get(), not after, to avoid initializing
                        // parcelableClass unnecessarily.
                        throw new BadParcelableException("Parcelable protocol requires a "
                                + "Parcelable.Creator object called "
                                + "CREATOR on class " + name);
                    }
                    creator = (Creator<?>) f.get(null);
                } catch (IllegalAccessException e) {
                    Log.e(TAG, "Illegal access when unmarshalling: " + name, e);
                    throw new BadParcelableException(
                            "IllegalAccessException when unmarshalling: " + name);
                } catch (ClassNotFoundException e) {
                    Log.e(TAG, "Class not found when unmarshalling: " + name, e);
                    throw new BadParcelableException(
                            "ClassNotFoundException when unmarshalling: " + name);
                } catch (NoSuchFieldException e) {
                    throw new BadParcelableException("Parcelable protocol requires a "
                            + "Parcelable.Creator object called "
                            + "CREATOR on class " + name);
                }
                if (creator == null) {
                    throw new BadParcelableException("Parcelable protocol requires a "
                            + "non-null Parcelable.Creator object called "
                            + "CREATOR on class " + name);
                }

                map.put(name, creator);
            }
        }

        return creator;
    }

    public static final ClassLoaderCreator<BulkCursor> CREATOR =
            new ClassLoaderCreator<BulkCursor>() {
                public BulkCursor createFromParcel(Parcel in) {
                    return new BulkCursor(in, null);
                }

                @Override
                public BulkCursor createFromParcel(Parcel in, ClassLoader loader) {
                    return new BulkCursor(in, loader);
                }

                @Override
                public BulkCursor[] newArray(int size) {
                    return new BulkCursor[size];
                }
            };
}
