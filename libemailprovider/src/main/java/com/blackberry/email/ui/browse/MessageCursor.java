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

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import com.blackberry.common.content.ObjectCursor;
import com.blackberry.email.Account;
import com.blackberry.email.Attachment;
import com.blackberry.email.Conversation;
import com.blackberry.email.provider.UIProvider.CursorExtraKeys;
import com.blackberry.email.provider.UIProvider.CursorStatus;
import com.blackberry.email.ui.ConversationUpdater;
import com.blackberry.message.service.MessageAttachmentValue;
import com.blackberry.provider.MessageContract;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * MessageCursor contains the messages within a conversation; the public methods within should
 * only be called by the UI thread, as cursor position isn't guaranteed to be maintained
 */
public class MessageCursor extends ObjectCursor<ConversationMessage> {
    /**
     * The current controller that this cursor can use to reference the owning {@link Conversation},
     * and a current {@link ConversationUpdater}. Since this cursor will survive a rotation, but
     * the controller does not, whatever the new controller is MUST update this reference before
     * using this cursor.
     */
    private ConversationController mController;

    private Integer mStatus;

    public interface ConversationController {
        Conversation getConversation();
        ConversationUpdater getListController();
        MessageCursor getMessageCursor();
        Account getAccount();
    }

    public MessageCursor(Cursor inner) {
        super(inner, ConversationMessage.FACTORY);
    }

    public void setController(ConversationController controller) {
        mController = controller;
    }

    public ConversationMessage getMessage() {
        final ConversationMessage m = getModel();
        // ALWAYS set up each ConversationMessage with the latest controller.
        // Rotation invalidates everything except this Cursor, its Loader and the cached Messages,
        // so if we want to continue using them after rotate, we have to ensure their controller
        // references always point to the current controller.
        m.setController(mController);
        return m;
    }

    // Is the conversation starred?
    public boolean isConversationStarred() {
        return false;
    }


    public boolean isConversationRead() {
        int pos = -1;
        while (moveToPosition(++pos)) {
            if (!getMessage().isRead()) {
                return false;
            }
        }
        return true;
    }

    public void markMessagesRead() {
        int pos = -1;
        while (moveToPosition(++pos)) {
            long newState = getMessage().mState;
            newState &= ~MessageContract.Message.State.UNREAD;
            newState |= MessageContract.Message.State.READ;
            getMessage().mState = newState;
        }
    }

    public int getStateHashCode() {
        return getStateHashCode(0);
    }

    /**
     * Calculate a hash code that compactly summarizes the state of the messages in this cursor,
     * with respect to the way the messages are displayed in conversation view. This is not a
     * general-purpose hash code. When the state hash codes of a new cursor differs from the
     * existing cursor's hash code, the conversation view will re-render from scratch.
     *
     * @param exceptLast optional number of messages to exclude iterating through at the end of the
     * cursor. pass zero to iterate through all messages (or use {@link #getStateHashCode()}).
     * @return state hash code of the selected messages in this cursor
     */
    public int getStateHashCode(int exceptLast) {
        int hashCode = 17;
        int pos = -1;
        final int stopAt = getCount() - exceptLast;
        while (moveToPosition(++pos) && pos < stopAt) {
            hashCode = 31 * hashCode + getMessage().getStateHashCode();
        }
        return hashCode;
    }

    public int getStatus() {
        if (mStatus != null) {
            return mStatus;
        }

        mStatus = CursorStatus.LOADED;
        final Bundle extras = getExtras();
        if (extras != null && extras.containsKey(CursorExtraKeys.EXTRA_STATUS)) {
            mStatus = extras.getInt(CursorExtraKeys.EXTRA_STATUS);
        }
        return mStatus;
    }

    /**
     * Returns true if the cursor is fully loaded. Returns false if the cursor is expected to get
     * new messages.
     * @return
     */
    public boolean isLoaded() {
        return !CursorStatus.isWaitingForResults(getStatus());
    }

    public String getDebugDump() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("conv='%s' status=%d messages:\n",
                mController.getConversation(), getStatus()));
        int pos = -1;
        while (moveToPosition(++pos)) {
            final ConversationMessage m = getMessage();
            final List<Uri> attUris = Lists.newArrayList();
            for (MessageAttachmentValue a : m.getAttachments()) {
                attUris.add(Uri.parse(a.getContentUri()));
            }
            sb.append(String.format(
                    "[Message #%d hash=%s uri=%s id=%s serverId=%s from='%s'" +
                    " state=%s attUris=%s]\n",
                    pos, m.getStateHashCode(), m.mEntityUri, m.mId, m.mRemoteId, m.mSender, 
                    m.mState, attUris));
        }
        return sb.toString();
    }

}