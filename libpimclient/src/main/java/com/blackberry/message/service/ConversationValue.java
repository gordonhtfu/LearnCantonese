
package com.blackberry.message.service;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.Parcel;
import android.os.Parcelable;

import com.blackberry.provider.MessageContract.Conversation;

public class ConversationValue implements Parcelable {

    /**
     * row id of the conversation
     */
    public long mId = -1;

    /**
     * public id of the conversation
     */
    public String mConversationId;

    /**
     * account id the conversation belongs to
     */
    public long mAccountId;

    /**
     * user assigned name give to the conversation
     */
    public String mName;

    /**
     * id of the last message in the conversation
     */
    public long mLastMessageId = -1;

    /**
     * timestamp of the last message in the conversation
     */
    public long mLastMessageTimestamp;

    /**
     * state of the last message in the conversation
     */
    public long mLastMessageState;

    /**
     * Count of unread messages in the conversation
     */
    public long mUnreadCount;

    /**
     * Count of draft messages in the conversation
     */
    public long mDraftCount;

    /**
     * Count of messages in the sent state in the conversation
     */
    public long mSentCount;

    /**
     * Count of errored messages in the conversation
     */
    public long mErrorCount;

    /**
     * Count of filed messages in the conversation
     */
    public long mFiledCount;

    /**
     * Count of inbound messages in the conversation
     */
    public long mInboundCount;

    /**
     * Count of flagged messages in the conversation
     */
    public long mFlaggedCount;

    /**
     * Count of high importance messages in the conversation
     */
    public long mHighImportanceCount;

    /**
     * Count of low importance messages in the conversation
     */
    public long mLowImportanceCount;

    /**
     * Count of meeting invites in the conversation
     */
    public long mMeetingInviteCount;

    /**
     * Count of all messages in the conversation
     */
    public long mTotalMessageCount;

    /**
     * Count of all attachments in the conversation
     */
    public long mTotalAttachmentCount;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        toContentValues(false).writeToParcel(dest, flags);
    }

    public ConversationValue() {
    }

    public ConversationValue(Cursor cursor) {
        setValues(cursor);
    }

    /**
     * Supports Parcelable
     */
    public ConversationValue(Parcel in) {
        setValues(ContentValues.CREATOR.createFromParcel(in));
    }

    /**
     * Supports Parcelable
     */
    public static final Parcelable.Creator<ConversationValue> CREATOR = new Parcelable.Creator<ConversationValue>() {
        @Override
        public ConversationValue createFromParcel(Parcel in) {
            return new ConversationValue(in);
        }

        @Override
        public ConversationValue[] newArray(int size) {
            return new ConversationValue[size];
        }
    };

    /**
     * Converts ConversationValue members into ContentValaues
     * 
     * @return
     */
    public ContentValues toContentValues(boolean excludeId) {
        ContentValues values = new ContentValues();
        
        if (!excludeId) {
            values.put(Conversation._ID, mId);
        }
       
        values.put(Conversation.CONVERSATION_ID, mConversationId);
        values.put(Conversation.ACCOUNT_ID, mAccountId);
        values.put(Conversation.NAME, mName);
        values.put(Conversation.LAST_MESSAGE_ID, mLastMessageId);
        values.put(Conversation.LAST_MESSAGE_TIMESTAMP, mLastMessageTimestamp);
        values.put(Conversation.LAST_MESSAGE_STATE, mLastMessageState);
        values.put(Conversation.UNREAD_COUNT, mUnreadCount);
        values.put(Conversation.DRAFT_COUNT, mDraftCount);
        values.put(Conversation.SENT_COUNT, mSentCount);
        values.put(Conversation.ERROR_COUNT, mErrorCount);
        values.put(Conversation.FILED_COUNT, mFiledCount);
        values.put(Conversation.SENT_COUNT, mSentCount);
        values.put(Conversation.INBOUND_COUNT, mInboundCount);
        values.put(Conversation.FLAGGED_COUNT, mFlaggedCount);
        values.put(Conversation.HIGH_IMPORTANCE_COUNT, mHighImportanceCount);
        values.put(Conversation.LOW_IMPORTANCE_COUNT, mLowImportanceCount);
        values.put(Conversation.MEETING_INVITE_COUNT, mMeetingInviteCount);
        values.put(Conversation.TOTAL_MESSAGE_COUNT, mTotalMessageCount);
        values.put(Conversation.TOTAL_ATTACHMENT_COUNT, mTotalAttachmentCount);

        return values;
    }

    /**
     * Sets ConversationValue data members from ContentValues
     * 
     * @param values
     */
    public void setValues(ContentValues values) {
        mId = values.getAsLong(Conversation._ID);

        mConversationId = values.getAsString(Conversation.CONVERSATION_ID);
        mAccountId = values.getAsLong(Conversation.ACCOUNT_ID);
        mName = values.getAsString(Conversation.NAME);
        if (values.containsKey(Conversation.LAST_MESSAGE_ID)) {
            mLastMessageId = values.getAsLong(Conversation.LAST_MESSAGE_ID);
        }
        mLastMessageTimestamp = values.getAsLong(Conversation.LAST_MESSAGE_TIMESTAMP);
        mLastMessageState = values.getAsLong(Conversation.LAST_MESSAGE_STATE);
        mUnreadCount = values.getAsLong(Conversation.UNREAD_COUNT);
        mDraftCount = values.getAsLong(Conversation.DRAFT_COUNT);
        mSentCount = values.getAsLong(Conversation.SENT_COUNT);
        mErrorCount = values.getAsLong(Conversation.ERROR_COUNT);
        mFiledCount = values.getAsLong(Conversation.FILED_COUNT);
        mInboundCount = values.getAsLong(Conversation.INBOUND_COUNT);
        mFlaggedCount = values.getAsLong(Conversation.FLAGGED_COUNT);
        mHighImportanceCount = values.getAsLong(Conversation.HIGH_IMPORTANCE_COUNT);
        mLowImportanceCount = values.getAsLong(Conversation.LOW_IMPORTANCE_COUNT);
        mMeetingInviteCount = values.getAsLong(Conversation.MEETING_INVITE_COUNT);
        mTotalMessageCount = values.getAsLong(Conversation.TOTAL_MESSAGE_COUNT);
        mTotalAttachmentCount = values.getAsLong(Conversation.TOTAL_ATTACHMENT_COUNT);

    }

    /**
     * Sets Message members contained in the Cursor - Note, not all members may
     * be set as this will be dependent on the query projection and therefore
     * null and value checks are recommended
     * 
     * @param cursor
     */
    public void setValues(Cursor cursor) {

        ContentValues values = new ContentValues();

        DatabaseUtils.cursorLongToContentValuesIfPresent(cursor, values, Conversation._ID);
        DatabaseUtils.cursorLongToContentValuesIfPresent(cursor, values, Conversation.CONVERSATION_ID);
        DatabaseUtils.cursorLongToContentValuesIfPresent(cursor, values, Conversation.ACCOUNT_ID);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values, Conversation.NAME);
        DatabaseUtils.cursorLongToContentValuesIfPresent(cursor, values, Conversation.LAST_MESSAGE_ID);
        DatabaseUtils.cursorIntToContentValuesIfPresent(cursor, values, Conversation.LAST_MESSAGE_TIMESTAMP);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values, Conversation.LAST_MESSAGE_STATE);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values, Conversation.UNREAD_COUNT);
        DatabaseUtils.cursorIntToContentValuesIfPresent(cursor, values, Conversation.DRAFT_COUNT);
        DatabaseUtils.cursorIntToContentValuesIfPresent(cursor, values, Conversation.SENT_COUNT);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values, Conversation.ERROR_COUNT);
        DatabaseUtils.cursorIntToContentValuesIfPresent(cursor, values, Conversation.FILED_COUNT);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values, Conversation.INBOUND_COUNT);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values, Conversation.FLAGGED_COUNT);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values, Conversation.HIGH_IMPORTANCE_COUNT);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values, Conversation.LOW_IMPORTANCE_COUNT);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values, Conversation.MEETING_INVITE_COUNT);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values, Conversation.TOTAL_MESSAGE_COUNT);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values, Conversation.TOTAL_ATTACHMENT_COUNT);

        setValues(values);
    }

    @Override
    public String toString() {
        return "[" + mConversationId + " " + mId + "," + mAccountId + "]";
    }
}
