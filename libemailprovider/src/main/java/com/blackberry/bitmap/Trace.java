/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.blackberry.bitmap;

import android.os.Build;

/**
 * Stand-in for {@link android.os.Trace}.
 */
public abstract class Trace {

    /**
     * Begins systrace tracing for a given tag. No-op on unsupported platform versions.
     *
     * @param tag systrace tag to use
     *
     * @see android.os.Trace#beginSection(String)
     */
    public static void beginSection(String tag) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            android.os.Trace.beginSection(tag);
        }
    }

    /**
     * Ends systrace tracing for the most recently begun section. No-op on unsupported platform
     * versions.
     *
     * @see android.os.Trace#endSection()
     */
    public static void endSection() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            android.os.Trace.endSection();
        }
    }

}
