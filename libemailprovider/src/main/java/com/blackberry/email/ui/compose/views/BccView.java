/**
 * Copyright (c) 2011, Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.blackberry.email.ui.compose.views;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import com.blackberry.lib.emailprovider.R;

public class BccView extends RelativeLayout {

    private final View mBcc;

    public BccView(Context context) {
        this(context, null);
    }

    public BccView(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public BccView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        LayoutInflater.from(context).inflate(R.layout.bcc_view, this);
        mBcc = findViewById(R.id.bcc_content);
    }

    public void show(boolean animate, boolean showBcc) {
        mBcc.setVisibility(showBcc ? View.VISIBLE : View.GONE);
        if (showBcc) {
            mBcc.requestFocus();
            mBcc.requestLayout();
        }
        if (animate) {
            doAnimate();
        } else {
            if (showBcc) {
                mBcc.setAlpha(1);
            }
            requestLayout();
        }
    }

    private void doAnimate() {
        Resources res = getResources();
        // Then, have cc/ bcc fade in
        int fadeDuration = res.getInteger(R.integer.fadein_cc_bcc_dur);
        ObjectAnimator bccAnimator = ObjectAnimator.ofFloat(mBcc, "alpha", 0, 1);
        bccAnimator.setDuration(fadeDuration);

        Animator fadeAnimation = bccAnimator;
        fadeAnimation.start();
    }

    /**
     * @return whether the BCC field is visible
     */
    public boolean isBccVisible() {
        return mBcc.getVisibility() == View.VISIBLE;
    }
}
