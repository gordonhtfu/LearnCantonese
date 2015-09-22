/*
 * Copyright (C) 2012 Google Inc.
 * Licensed to The Android Open Source Project.
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

package com.blackberry.email.ui.browse;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.blackberry.email.Conversation;
import com.blackberry.email.Folder;
import com.blackberry.lib.emailprovider.R;
import com.blackberry.email.Settings;
import com.blackberry.email.ui.FolderDisplayer;
import com.blackberry.email.ui.browse.ConversationViewAdapter.ConversationHeaderItem;
import com.blackberry.email.ui.browse.FolderSpan.FolderSpanDimensions;
import com.blackberry.email.utils.UtilsEx;
import com.blackberry.common.utils.LogTag;
import com.blackberry.common.utils.LogUtils;

/**
 * A view for the subject and folders in the conversation view. This container
 * makes an attempt to combine subject and folders on the same horizontal line if
 * there is enough room to fit both without wrapping. If they overlap, it
 * adjusts the layout to position the folders below the subject.
 */
public class ConversationViewHeader extends LinearLayout implements OnClickListener {

    public interface ConversationViewHeaderCallbacks {
        /**
         * Called when the height of the {@link ConversationViewHeader} changes.
         *
         * @param newHeight the new height in px
         */
        void onConversationViewHeaderHeightChange(int newHeight);
    }

    private static final String LOG_TAG = LogTag.getLogTag();
    private TextView mSubjectView;
    private ConversationViewHeaderCallbacks mCallbacks;
    private ConversationAccountController mAccountController;
    private ConversationHeaderItem mHeaderItem;

    private boolean mLargeText;
    private final float mCondensedTextSize;
    private final int mCondensedTopPadding;

    /**
     * Instantiated from this layout: conversation_view_header.xml
     * @param context
     */
    public ConversationViewHeader(Context context) {
        this(context, null);
    }

    public ConversationViewHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
        mLargeText = true;
        final Resources resources = getResources();
        mCondensedTextSize =
                resources.getDimensionPixelSize(R.dimen.conversation_header_font_size_condensed);
        mCondensedTopPadding = resources.getDimensionPixelSize(
                R.dimen.conversation_header_vertical_padding_condensed);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mSubjectView = (TextView) findViewById(R.id.subject);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // If we currently have large text and we have greater than 2 lines,
        // switch to smaller text size with smaller top padding and re-measure
        if (mLargeText && mSubjectView.getLineCount() > 2) {
            mSubjectView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mCondensedTextSize);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                // start, top, end, bottom
                mSubjectView.setPaddingRelative(mSubjectView.getPaddingStart(),
                        mCondensedTopPadding, mSubjectView.getPaddingEnd(),
                        mSubjectView.getPaddingBottom());
            } else {
                mSubjectView.setPadding(mSubjectView.getPaddingLeft(),
                        mCondensedTopPadding, mSubjectView.getPaddingRight(),
                        mSubjectView.getPaddingBottom());
            }

            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    public void setCallbacks(ConversationViewHeaderCallbacks callbacks,
            ConversationAccountController accountController) {
        mCallbacks = callbacks;
        mAccountController = accountController;
    }

    public void setSubject(final String subject) {
        mSubjectView.setText(subject);
        mSubjectView.setVisibility(TextUtils.isEmpty(subject) ? GONE : VISIBLE);
    }

    public void bind(ConversationHeaderItem headerItem) {
        mHeaderItem = headerItem;
    }

    private int measureHeight() {
        ViewGroup parent = (ViewGroup) getParent();
        if (parent == null) {
            LogUtils.e(LOG_TAG, "Unable to measure height of conversation header");
            return getHeight();
        }
        final int h = UtilsEx.measureViewHeight(this, parent);
        return h;
    }

    /**
     * Update the conversation view header to reflect the updated conversation.
     */
    public void onConversationUpdated(Conversation conv) {
        if (mHeaderItem != null) {
            final int h = measureHeight();
            if (mHeaderItem.setHeight(h)) {
                mCallbacks.onConversationViewHeaderHeightChange(h);
            }
        }
    }

    @Override
    public void onClick(View v) {

    }
}
