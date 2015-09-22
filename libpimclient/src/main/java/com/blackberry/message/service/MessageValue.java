
package com.blackberry.message.service;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.blackberry.provider.MessageContract;

/**
 * Value Object for message. Serves as a composition for its attributes,
 * contacts, attachments and body parts. The object can be used when structured
 * message information should be passed, i.e. sendMessage() method.
 * 
 * @author vrudenko
 */
public class MessageValue implements Parcelable {
    public static final int NOT_SAVED = -1;

    /**
     * the local id of this Message
     */
    public long mId = NOT_SAVED;// assume not saved to start

    /**
     * the entity uri of this message
     */
    public Uri mEntityUri;

    /**
     * The account id ( from AccountProvider)
     */
    public long mAccountId;

    /**
     * MimeType of the message
     */
    public String mMimeType;

    /**
     * The converstion id indication what conversation this message is in
     */
    public String mConversationId;

    /**
     * The remote id , which typically relates to the id the server gives this
     * message, used my SAM
     */
    public String mRemoteId;

    /**
     * The folder id relates to the local Folder id (FK) that this message is
     * contained in
     */
    public Long mFolderId;

    /**
     * The time the message was sent
     */
    public long mTimeStamp;

    /**
     * The subject of this message
     */
    public String mSubject;

    /**
     * Senders display name
     */
    public String mSender;

    /**
     * The current state of the Message (See MessageContract.Message.State for
     * values)
     */
    public long mState;

    /**
     * The full list of To/CC/From/BCC contacts
     */
    private List<MessageContactValue> mContacts;

    /**
     * The body of the message
     */
    private List<MessageBodyValue> mBodies;

    /**
     * The list of attachments
     */
    private List<MessageAttachmentValue> mAttachments;

    /**
     * Generic SyncData Values - used by SAM
     */
    public String mSyncData1;
    public String mSyncData2;
    public String mSyncData3;
    public String mSyncData4;
    public String mSyncData5;

    /**
     * boolean indicates that the message has been modified locally and not yet syned to server
     * 
     * NOTE: YOU MUSH ENSURE THAT THE CURSOR PROJECTION CONTAINS THE DIRTY COLUMN IN ORDER FOR THIS VALUE TO BE CORRECT
     */
    public boolean mIsDirty;

    /**
     * boolean indicating that this message has been locally deleted and that is has not been syned to server
     * NOTE: YOU MUSH ENSURE THAT THE CURSOR PROJECTION CONTAINS THE DELETED COLUMN IN ORDER FOR THIS VALUE TO BE CORRECT
     */
    public boolean mIsDeleted;

    // @TODO SEE IF WE NEED THESE _ LEFT OVER FROM PORT
    public boolean mFlagFavorite = false;
    public boolean mFlagAttachment = false;
    // public int mDraftInfo;
    public long mMainMailboxKey;
    // For now, just the start time of a meeting invite, in ms
    public String mMeetingInfo;
    // public String mSnippet;
    public String mProtocolSearchInfo;
    // public String mThreadTopic;

    // The following transient members may be used while building and
    // manipulating messages,
    // but they are NOT persisted directly by EmailProvider. See Body for
    // related fields.
    transient public String mText;
    transient public String mHtml;
    transient public String mTextReply;
    transient public String mHtmlReply;
    transient public long mSourceKey;
    transient public String mIntroText;
    transient public int mQuotedTextStartPos;

    /**
     * Default Constructor
     */
    public MessageValue() {
        mBodies = new ArrayList<MessageBodyValue>();
        mContacts = new ArrayList<MessageContactValue>();
        mAttachments = new ArrayList<MessageAttachmentValue>();
    }

    /**
     * Constructs a Message from a Cursor
     * 
     * @param cursor
     */
    public MessageValue(Cursor cursor) {
        this();
        setValues(cursor);
    }

    /**
     * Add new message contact
     * 
     * @param contact contact to add
     */
    public void add(MessageContactValue contact) {
        mContacts.add(contact);
    }

    /**
     * Adds a new list of message contacts
     * 
     * @param contacts
     */
    public void addMessageContacts(List<MessageContactValue> contacts) {
        mContacts.addAll(contacts);
    }

    public void clearMessageContacts() {
        mContacts.clear();
    }

    public void add(MessageBodyValue body) {
        body.mMessageId = mId;
        mBodies.add(body);
    }

    public void setSingleBody(MessageBodyValue body){
        mBodies.clear();
        body.mMessageId = mId;
        add(body);
    }

    /**
     * Add new message attachment
     * 
     * @param attachment attachment to add
     */
    public void add(MessageAttachmentValue attachment) {
        mAttachments.add(attachment);
    }

    /**
     * Add a list of attachments
     */
    public void addAttachments(List<MessageAttachmentValue> attachments) {
        mAttachments.addAll(attachments);
    }

    /**
     * gets all of the contacts related to this message (include From/TO/CC/BCC
     * etc)
     * 
     * @return
     */
    public List<MessageContactValue> getContacts() {
        return mContacts;
    }

    /*
     * gets all message bodies
     */
    public List<MessageBodyValue> getBodies() {
        return mBodies;
    }

    /**
     * gets all Message Contacts that have a given field type
     * 
     * @param fieldTye
     * @return
     */
    public List<MessageContactValue> getContacts(int fieldTye) {

        ArrayList<MessageContactValue> contacts = new ArrayList<MessageContactValue>();
        MessageContactValue current = null;
        for (int x = 0; x < mContacts.size(); x++) {
            current = mContacts.get(x);
            if (current.mFieldType == fieldTye) {
                contacts.add(current);
            }
        }
        return contacts;
    }

    /**
     * get all attachments for this message
     * 
     * @return
     */
    public List<MessageAttachmentValue> getAttachments() {
        return mAttachments;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        toContentValues(false).writeToParcel(dest, flags);
        dest.writeList(mContacts);
        dest.writeList(mBodies);
        dest.writeList(mAttachments);
    }

    /**
     * Supports Parcelable
     */
    public MessageValue(Parcel in) {
        setValues(ContentValues.CREATOR.createFromParcel(in));
        mContacts = new ArrayList<MessageContactValue>();
        in.readList(mContacts, MessageContactValue.class.getClassLoader());
        mBodies = new ArrayList<MessageBodyValue>();
        in.readList(mBodies, MessageBodyValue.class.getClassLoader());
        mAttachments = new ArrayList<MessageAttachmentValue>();
        in.readList(mAttachments, MessageAttachmentValue.class.getClassLoader());
    }

    /**
     * Supports Parcelable
     */
    public static final Parcelable.Creator<MessageValue> CREATOR = new Parcelable.Creator<MessageValue>() {
        @Override
        public MessageValue createFromParcel(Parcel in) {
            return new MessageValue(in);
        }

        @Override
        public MessageValue[] newArray(int size) {
            return new MessageValue[size];
        }
    };

    public boolean isNew() {
        return mId == NOT_SAVED ? true : false;
    }

    /**
     * converts message to content values
     * 
     * @param excludeId - if true the mId will not be included in the values
     *            set. This is useful when doing inserts/updates as the mId will
     *            part of the uri
     * @return contentvalues
     */
    public ContentValues toContentValues(boolean excludeId) {
        ContentValues values = new ContentValues();

        // Assign values for each row.
        if (!excludeId) {
            values.put(MessageContract.Message._ID, mId);
        }
        if (mEntityUri != null) {
            values.put(MessageContract.Message.ENTITY_URI, mEntityUri.toString());
        }
        values.put(MessageContract.Message.MIME_TYPE, mMimeType);
        values.put(MessageContract.Message.SENDER, mSender);
        values.put(MessageContract.Message.TIMESTAMP, mTimeStamp);
        values.put(MessageContract.Message.SUBJECT, mSubject);
        values.put(MessageContract.Message.STATE, mState);
        values.put(MessageContract.Message.CONVERSATION_ID, mConversationId);
        values.put(MessageContract.Message.REMOTE_ID, mRemoteId);
        values.put(MessageContract.Message.FOLDER_ID, mFolderId);
        values.put(MessageContract.Message.ACCOUNT_ID, mAccountId);
        values.put(MessageContract.Message.SYNC_DATA1, mSyncData1);
        values.put(MessageContract.Message.SYNC_DATA2, mSyncData2);
        values.put(MessageContract.Message.SYNC_DATA3, mSyncData3);
        values.put(MessageContract.Message.SYNC_DATA4, mSyncData4);
        values.put(MessageContract.Message.SYNC_DATA5, mSyncData5);
        return values;
    }

    /**
     * Sets data members contained in a ContentValue object. Note, not all
     * members may be set as this will be dependent on the client usage and
     * therefore null and value checks are recommended
     * 
     * @param values
     */
    public void setValues(ContentValues values) {
        if (values.containsKey(MessageContract.Message._ID))
            mId = values.getAsLong(MessageContract.Message._ID);
        final String messageUriString = values.getAsString(MessageContract.Message.ENTITY_URI);
        mEntityUri = !TextUtils.isEmpty(messageUriString) ? Uri.parse(messageUriString) : null;

        // Long can be null need to check
        if (values.containsKey(MessageContract.Message.ACCOUNT_ID)) {
            mAccountId = values.getAsLong(MessageContract.Message.ACCOUNT_ID);
        }
        // Long can be null need to check
        if (values.containsKey(MessageContract.Message.TIMESTAMP)) {
            mTimeStamp = values.getAsLong(MessageContract.Message.TIMESTAMP);
        }
        // Long can be null need to check
        if (values.containsKey(MessageContract.Message.FOLDER_ID)) {
            mFolderId = values.getAsLong(MessageContract.Message.FOLDER_ID);
        }

        if (values.containsKey(MessageContract.Message.STATE)) {
            mState = values.getAsInteger(MessageContract.Message.STATE);
        }

        if (values.containsKey(MessageContract.Message.DIRTY)) {
            mIsDirty = values.getAsInteger(MessageContract.Message.DIRTY) == 1 ? true : false;
        }

        if (values.containsKey(MessageContract.Message.DELETED)) {
            mIsDeleted = values.getAsInteger(MessageContract.Message.DELETED) == 1 ? true : false;
        }

        mMimeType = values.getAsString(MessageContract.Message.MIME_TYPE);
        mConversationId = values.getAsString(MessageContract.Message.CONVERSATION_ID);
        mSender = values.getAsString(MessageContract.Message.SENDER);
        mSubject = values.getAsString(MessageContract.Message.SUBJECT);
        mRemoteId = values.getAsString(MessageContract.Message.REMOTE_ID);
        mSyncData1 = values.getAsString(MessageContract.Message.SYNC_DATA1);
        mSyncData2 = values.getAsString(MessageContract.Message.SYNC_DATA2);
        mSyncData3 = values.getAsString(MessageContract.Message.SYNC_DATA3);
        mSyncData4 = values.getAsString(MessageContract.Message.SYNC_DATA4);
        mSyncData5 = values.getAsString(MessageContract.Message.SYNC_DATA5);
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

        DatabaseUtils.cursorLongToContentValuesIfPresent(cursor, values,
                MessageContract.Message._ID);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values,
                MessageContract.Message.ENTITY_URI);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values,
                MessageContract.Message.MIME_TYPE);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values,
                MessageContract.Message.CONVERSATION_ID);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values,
                MessageContract.Message.SENDER);
        DatabaseUtils.cursorLongToContentValuesIfPresent(cursor, values,
                MessageContract.Message.TIMESTAMP);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values,
                MessageContract.Message.SUBJECT);
        DatabaseUtils.cursorIntToContentValuesIfPresent(cursor, values,
                MessageContract.Message.STATE);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values,
                MessageContract.Message.REMOTE_ID);
        DatabaseUtils.cursorLongToContentValuesIfPresent(cursor, values,
                MessageContract.Message.FOLDER_ID);
        DatabaseUtils.cursorLongToContentValuesIfPresent(cursor, values,
                MessageContract.Message.ACCOUNT_ID);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values,
                MessageContract.Message.SYNC_DATA1);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values,
                MessageContract.Message.SYNC_DATA2);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values,
                MessageContract.Message.SYNC_DATA3);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values,
                MessageContract.Message.SYNC_DATA4);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values,
                MessageContract.Message.SYNC_DATA5);
        DatabaseUtils.cursorIntToContentValuesIfPresent(cursor, values,
                MessageContract.Message.DIRTY);
        DatabaseUtils.cursorIntToContentValuesIfPresent(cursor, values,
                MessageContract.Message.DELETED);

        setValues(values);
    }

    /**
     * Helper method to insert a new Message into the MCP based on current data
     * values
     * 
     * @param context
     * @return
     */
    public Uri save(Context context) {
        if (mId > 0) {
            throw new UnsupportedOperationException();
        }
        Uri res = context.getContentResolver().insert(MessageContract.Message.CONTENT_URI,
                toContentValues(true));
        mId = Long.parseLong(res.getPathSegments().get(1));
        return res;
    }

    public static MessageValue restoreMessageWithId(Context context, long id) {
        MessageValue message = null;
        Uri u = ContentUris.withAppendedId(MessageContract.Message.CONTENT_URI, id);
        Cursor c = context.getContentResolver().query(u,
                MessageContract.Message.DEFAULT_PROJECTION,
                null, null, null);

        try {
            if (c != null && c.moveToFirst()) {
                message = new MessageValue(c);
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }

        return message;
    }

    public boolean isRead() {
        // this stuff is broken - temp work around
        return (mState & MessageContract.Message.State.UNREAD) == 0;
    }

    public long getNewFlagFavorite() {
        return mState & MessageContract.Message.State.FLAGGED;

    }

    /**
     * Load the message body for this message
     * @param context
     */
    public void restoreMessageBodies(Context context) {
        mBodies.clear();
        MessageBodyValue body = MessageBodyValue.restoreBodyWithMessageId(context, mId);
        if(body != null){
            this.add(body);
        }
    }

    /**
     * Load the contacts for this message
     * @param context
     */
    public void restoreMessageContacts(Context context) {
        mContacts.clear();
        this.addMessageContacts(MessageContactValue.restoreWithMessageId(context, mId));
    }

    public static MessageValue restoreMessageWithIdIncludingContacts(Context context, long id) {
        MessageValue message = restoreMessageWithId(context, id);
        message.restoreMessageContacts(context);

        return message;
    }

    public static MessageValue retoreMessageWithIdAllParts(Context context, long id) {
        MessageValue message = restoreMessageWithIdIncludingContacts(context, id);
        MessageBodyValue body = MessageBodyValue.restoreBodyWithMessageId(context, id);

        if (body != null) {
            message.mBodies.add(body);
        }
        //@TODO ATTACHMENTS
        return message;
    }

    /**
     * Gets all of the message contact addresses of a specific type for this message.
     * @param type Type of contact. See {@link MessageContract.MessageContact.FieldType} for values.
     * @return A string array of addresses
     */
    public String[] getAddresses(int type) {
        ArrayList<String> addresses = new ArrayList<String>();
        for (MessageContactValue contact : mContacts) {
            if (contact.mFieldType == type) {
                addresses.add(contact.mAddress);
            }
        }
        return addresses.toArray(new String[addresses.size()]);
    }
}
