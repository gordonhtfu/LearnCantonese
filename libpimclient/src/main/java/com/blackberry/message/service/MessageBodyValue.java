
package com.blackberry.message.service;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;

import com.blackberry.provider.MessageContract;

public class MessageBodyValue implements Parcelable {

    /**
     * row id of the MessageBody
     */
    public long mId;

    /**
     * id of the Message that this MessageBody belongs to
     */
    public long mMessageId;

    /**
     * The account id ( from AccountProvider)
     */
    public long mAccountId;
    public int mType;
    public int mState;
    public String mPath;

    /**
     * Generic SyncData Values - used by SAM
     */
    public String mSyncData1;
    public String mSyncData2;
    public String mSyncData3;
    public String mSyncData4;
    public String mSyncData5;

    // data
    public byte[] mContentBytes;
    public ParcelFileDescriptor mFd;

    public MessageBodyValue() {

    }

    public MessageBodyValue(Cursor cursor) {
        setValues(cursor);
    }

    public String getBytesAsString() {
        String value = null;

        if (mContentBytes != null) {
            value = new String(mContentBytes);
        } else {
            value = "";
        }
        return value;
    }

    public MessageBodyValue(Parcel in) {
        setValues(ContentValues.CREATOR.createFromParcel(in));
        int length = in.readInt();
        if (length >= 0) {
            mContentBytes = new byte[length];
            in.readByteArray(mContentBytes);
        }
        int exists = in.readInt();
        if (exists > 0) {
            mFd = in.readFileDescriptor();
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        toContentValues(false).writeToParcel(dest, flags);
        if (mContentBytes != null) {
            dest.writeInt(mContentBytes.length);
            dest.writeByteArray(mContentBytes);
        } else {
            dest.writeInt(-1);
        }
        if (mFd != null) {
            dest.writeInt(1);
            mFd.writeToParcel(dest, flags);
        } else {
            dest.writeInt(0);
        }
    }

    public static final Parcelable.Creator<MessageBodyValue> CREATOR = new Parcelable.Creator<MessageBodyValue>() {
        public MessageBodyValue createFromParcel(Parcel in) {
            return new MessageBodyValue(in);
        }

        public MessageBodyValue[] newArray(int size) {
            return new MessageBodyValue[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    public boolean isNew(){
        return mId < 1 ;
    }

    /**
     * Converts member data into ContentValues
     * 
     * @return
     */
    public ContentValues toContentValues(boolean excludeId) {
        ContentValues cv = new ContentValues();

        if (!excludeId) {
            cv.put(MessageContract.MessageBody._ID, mId);
        }
        cv.put(MessageContract.MessageBody.ACCOUNT_ID, mAccountId);
        cv.put(MessageContract.MessageBody.MESSAGE_ID, mMessageId);
        cv.put(MessageContract.MessageBody.STATE, mState);
        cv.put(MessageContract.MessageBody.DATA, mContentBytes);
        cv.put(MessageContract.MessageBody.PATH, mPath);
        cv.put(MessageContract.MessageBody.TYPE, mType);
        cv.put(MessageContract.MessageBody.SYNC_DATA1, mSyncData1);
        cv.put(MessageContract.MessageBody.SYNC_DATA1, mSyncData2);
        cv.put(MessageContract.MessageBody.SYNC_DATA1, mSyncData3);
        cv.put(MessageContract.MessageBody.SYNC_DATA1, mSyncData4);
        cv.put(MessageContract.MessageBody.SYNC_DATA1, mSyncData5);

        return cv;
    }

    public void setValues(ContentValues values) {
        if (values.containsKey(MessageContract.MessageBody._ID)) {
            mId = values.getAsLong(MessageContract.MessageBody._ID);
        }

        if (values.containsKey(MessageContract.MessageBody.ACCOUNT_ID)) {
            mAccountId = values.getAsLong(MessageContract.MessageBody.ACCOUNT_ID);
        }

        if (values.containsKey(MessageContract.MessageBody.MESSAGE_ID)) {
            mMessageId = values.getAsLong(MessageContract.MessageBody.MESSAGE_ID);
        }

        if (values.containsKey(MessageContract.MessageBody.STATE)) {
            mState = values.getAsInteger(MessageContract.MessageBody.STATE);
        }
        mContentBytes = values.getAsByteArray(MessageContract.MessageBody.DATA);
        if (values.containsKey(MessageContract.MessageBody.PATH)) {
            mPath = values.getAsString(MessageContract.MessageBody.PATH);
        }
        mType = values.getAsInteger(MessageContract.MessageBody.TYPE);
        mSyncData1 = values.getAsString(MessageContract.MessageBody.SYNC_DATA1);
        mSyncData2 = values.getAsString(MessageContract.MessageBody.SYNC_DATA2);
        mSyncData3 = values.getAsString(MessageContract.MessageBody.SYNC_DATA3);
        mSyncData4 = values.getAsString(MessageContract.MessageBody.SYNC_DATA4);
        mSyncData5 = values.getAsString(MessageContract.MessageBody.SYNC_DATA5);

    }

    /**
     * Set MessageBody members via a cursor
     * 
     * @param cursor
     */
    public void setValues(Cursor cursor) {
        ContentValues values = new ContentValues();

        DatabaseUtils.cursorLongToContentValuesIfPresent(cursor, values,
                MessageContract.MessageBody._ID);
        DatabaseUtils.cursorLongToContentValuesIfPresent(cursor, values,
                MessageContract.MessageBody.MESSAGE_ID);
        DatabaseUtils.cursorLongToContentValuesIfPresent(cursor, values,
                MessageContract.MessageBody.ACCOUNT_ID);

        int index = cursor.getColumnIndex(MessageContract.MessageBody.DATA);
        if (index != -1) {
            values.put(MessageContract.MessageBody.DATA, cursor.getBlob(index));
        }

        DatabaseUtils.cursorIntToContentValuesIfPresent(cursor, values,
                MessageContract.MessageBody.TYPE);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values,
                MessageContract.MessageBody.PATH);
        DatabaseUtils.cursorIntToContentValuesIfPresent(cursor, values,
                MessageContract.MessageBody.STATE);

        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values,
                MessageContract.MessageBody.SYNC_DATA1);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values,
                MessageContract.MessageBody.SYNC_DATA2);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values,
                MessageContract.MessageBody.SYNC_DATA3);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values,
                MessageContract.MessageBody.SYNC_DATA4);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values,
                MessageContract.MessageBody.SYNC_DATA5);

        setValues(values);
    }

    /**
     * Given a cursor, restore a Body from it
     * 
     * @param cursor a cursor which must NOT be null
     * @return the Body as restored from the cursor
     */
    private static MessageBodyValue restoreBodyWithCursor(Cursor cursor) {
        try {
            if (cursor.moveToFirst()) {
                MessageBodyValue body = new MessageBodyValue();
                body.setValues(cursor);
                return body;
            } else {
                return null;
            }
        } finally {
            cursor.close();
        }
    }

    public static MessageBodyValue restoreBodyWithId(Context context, long id) {
        MessageBodyValue body = null;
        Uri u = ContentUris.withAppendedId(MessageContract.MessageBody.CONTENT_URI, id);
        Cursor c = context.getContentResolver().query(u,
                MessageContract.MessageBody.DEFAULT_PROJECTION,
                null, null, null);
        if (c != null) {
            body = restoreBodyWithCursor(c);
        }

        return body;
    }

    public static MessageBodyValue restoreBodyWithMessageId(Context context, long messageId) {
        MessageBodyValue body = null;
        Cursor c = context.getContentResolver().query(MessageContract.MessageBody.CONTENT_URI,
                MessageContract.MessageBody.DEFAULT_PROJECTION,
                MessageContract.MessageBody.MESSAGE_ID + "=?",
                new String[] {
                    Long.toString(messageId)
                }, null);

        if (c != null) {
            body = restoreBodyWithCursor(c);
        }

        return body;
    }

    /**
     * Updates the Body for a messageId with the given ContentValues. If the
     * message has no body, a new body is inserted for the message. Warning: the
     * argument "values" is modified by this method, setting MESSAGE_KEY.
     */
    public static void updateBodyWithMessageId(Context context, long messageId,
            ContentValues values) {
        ContentResolver resolver = context.getContentResolver();
        MessageBodyValue body = restoreBodyWithMessageId(context, messageId);
        long bodyId = body != null ? body.mId : -1;
        values.put(MessageContract.MessageBody.MESSAGE_ID, messageId);
        if (bodyId == -1) {
            resolver.insert(MessageContract.MessageBody.CONTENT_URI, values);
        } else {
            final Uri uri = ContentUris.withAppendedId(MessageContract.MessageBody.CONTENT_URI,
                    bodyId);
            resolver.update(uri, values, null, null);
        }
    }

    /**
     * returns a give body id from a message id
     * @param context
     * @param messageId
     * @return
     */
    public static long getBodyIdFromMessageId(Context context, long messageId) {
        long id = -1;
        Cursor c = context.getContentResolver().query(MessageContract.MessageBody.CONTENT_URI,
                new String[] {
                    MessageContract.MessageBody._ID
                },
                MessageContract.MessageBody.MESSAGE_ID + "=?",
                new String[] {
                    Long.toString(messageId)
                }, null);

        try {
            if (c != null && c.moveToFirst()) {
                id = c.getLong(0);// only one column requested
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }

        return id;
    }
}
