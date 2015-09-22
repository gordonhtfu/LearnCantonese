/*
 * Copyright (C) 2013 Google Inc.
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

import com.blackberry.common.content.CursorCreator;
import com.blackberry.email.Conversation;
import com.blackberry.email.ui.ConversationUpdater;
import com.blackberry.email.ui.HtmlMessage;
import com.blackberry.email.ui.browse.MessageCursor.ConversationController;
import com.blackberry.message.service.MessageAttachmentValue;
import com.blackberry.message.service.MessageBodyValue;
import com.blackberry.message.service.MessageValue;
import com.blackberry.provider.MessageContract;
import com.google.common.base.Objects;

import android.content.AsyncQueryHandler;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.Html;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.util.Linkify;

import java.util.List;
import java.util.regex.Pattern;

/**
 * A message created as part of a conversation view. Sometimes, like during star/unstar, it's
 * handy to have the owning {@link com.blackberry.email.Conversation} for context.
 *
 * <p>This class must remain separate from the {@link MessageCursor} from whence it came,
 * because cursors can be closed by their Loaders at any time. The
 * {@link ConversationController} intermediate is used to obtain the currently opened cursor.
 *
 * <p>(N.B. This is a {@link android.os.Parcelable}, so try not to add non-transient fields here.
 * Parcelable state belongs either in {@link com.blackberry.email.Message} or
 * {@link com.blackberry.email.ui.ConversationViewState.MessageViewState}. The
 * assumption is that this class never needs the state of its extra context saved.)
 */
public final class ConversationMessage extends MessageValue implements HtmlMessage{
    private static Pattern INLINE_IMAGE_PATTERN = Pattern.compile("<img\\s+[^>]*src=",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    // TODO: remove these.  They're just here to keep existing code compiling until we figure out how to handle these features
    public Uri attachmentListUri = null;
    public String attachmentsJson = null;
    public Uri eventIntentUri = null;

    public boolean mAlwaysShowImages = false;
    public String mSnippet;

    public boolean shouldShowImagePrompt() {
        return !mAlwaysShowImages && embedsExternalResources();
    }

    private transient ConversationController mController;

    private ConversationMessage(Cursor cursor) {
        super(cursor);
    }

    public void setController(ConversationController controller) {
        mController = controller;
    }

    public Conversation getConversation() {
        return mController != null ? mController.getConversation() : null;
    }

    /**
     * Returns a hash code based on this message's identity, contents and current state.
     * This is a separate method from hashCode() to allow for an instance of this class to be
     * a functional key in a hash-based data structure.
     *
     */
    public int getStateHashCode() {
        return Objects.hashCode(mId, mState, getAttachmentsStateHashCode());
    }

    private int getAttachmentsStateHashCode() {
        int hash = 0;
        for (MessageAttachmentValue a : getAttachments()) {
            hash += a.mId;
        }
        return hash;
    }

    public boolean isConversationStarred() {
        final MessageCursor c = mController.getMessageCursor();
        return c != null && c.isConversationStarred();
    }

    public void star(boolean newStarred) {
        final ConversationUpdater listController = mController.getListController();
        if (listController != null) {
            listController.starMessage(this, newStarred);
        }
    }


    @Override
    public void restoreMessageBodies(Context context) {
        super.restoreMessageBodies(context);
        final List<MessageBodyValue> bodies = getBodies();
        if (!bodies.isEmpty()) {
            // Only one body
            MessageBodyValue body = bodies.get(0);
            if (body.mType == MessageContract.MessageBody.Type.HTML) {
                mHtml = body.getBytesAsString();
                mSnippet = MessageHeaderView.makeSnippet(mHtml);
            } else {
                mText = body.getBytesAsString();
                mSnippet = mText.length() > 100 ? mText.substring(0, 100) : mText;
            }
        }
    }

    /**
     * Public object that knows how to construct Messages given Cursors.
     */
    public static final CursorCreator<ConversationMessage> FACTORY =
            new CursorCreator<ConversationMessage>() {
                @Override
                public ConversationMessage createFromCursor(Cursor c) {
                    return new ConversationMessage(c);
                }

                @Override
                public String toString() {
                    return "ConversationMessage CursorCreator";
                }
            };


    @Override
    public long getId() {
        return mId;
    }

    @Override
    public String getBodyAsHtml() {
        String body = "";
        if (!TextUtils.isEmpty(mHtml)) {
            body = mHtml;
        } else if (!TextUtils.isEmpty(mText)) {
            final SpannableString spannable = new SpannableString(mText);
            Linkify.addLinks(spannable, Linkify.EMAIL_ADDRESSES);
            body = Html.toHtml(spannable);
        }
        return body;
    }

    @Override
    public boolean embedsExternalResources() {
        return !TextUtils.isEmpty(mHtml) && INLINE_IMAGE_PATTERN.matcher(mHtml).find();
    }

    public void markAlwaysShowImages(AsyncQueryHandler handler, int token, Object cookie) {
        // TODO no support yet
    }

    public boolean isFlaggedCalendarInvite() {
        // TODO no support yet
        return false;
    }

}
