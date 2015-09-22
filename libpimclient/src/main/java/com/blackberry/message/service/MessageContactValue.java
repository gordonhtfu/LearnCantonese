
package com.blackberry.message.service;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.blackberry.provider.MessageContract;
import com.blackberry.provider.MessageContract.MessageContact;

import java.util.ArrayList;

/**
 * Value Object for message contact
 * 
 * @author vrudenko
 */
public class MessageContactValue implements Parcelable {

    /**
     * the id of this MessageContact
     */
    public long mId = -1;

    /**
     * the name of this MessageContact
     */
    public String mName;

    /**
     * the address of this MessageContact
     */
    public String mAddress;

    /**
     * the type of address (EMAIL etc)
     */
    public int mAddressType;

    /**
     * the type of field this MessageContact links to (TO/FROM/etc)
     */
    public int mFieldType;

    /**
     * the id of the message that this message contact links to
     */
    public long mMessageId;

    /**
     * the account for this MessageContact
     */
    public long mAccountId;

    public MessageContactValue() {
    }

    public MessageContactValue(Cursor cursor) {
        setValues(cursor);
    }

    public MessageContactValue(Parcel in) {
        setValues(ContentValues.CREATOR.createFromParcel(in));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        toContentValues(false).writeToParcel(dest, flags);
    }

    public ContentValues toContentValues(boolean excludeId) {
        ContentValues values = new ContentValues();

        if (!excludeId) {
            values.put(MessageContact._ID, mId);
        }

        values.put(MessageContact.NAME, mName != null ? mName : "");
        values.put(MessageContact.ADDRESS, mAddress);
        values.put(MessageContact.ADDRESS_TYPE, mAddressType);
        values.put(MessageContact.FIELD_TYPE, mFieldType);
        values.put(MessageContact.MESSAGE_ID, mMessageId);
        values.put(MessageContact.ACCOUNT_ID, mAccountId);

        return values;
    }

    public void setValues(ContentValues values) {

        mId = values.getAsLong(MessageContact._ID);
        mName = values.getAsString(MessageContact.NAME);
        mAddress = values.getAsString(MessageContact.ADDRESS);

        if (values.containsKey(MessageContact.ADDRESS_TYPE)) {
            mAddressType = values.getAsInteger(MessageContact.ADDRESS_TYPE);
        }

        if (values.containsKey(MessageContact.FIELD_TYPE)) {
            mFieldType = values.getAsInteger(MessageContact.FIELD_TYPE);
        }

        if (values.containsKey(MessageContact.MESSAGE_ID)) {
            mMessageId = values.getAsInteger(MessageContact.MESSAGE_ID);
        }

        if (values.containsKey(MessageContact.ACCOUNT_ID)) {
            mAccountId = values.getAsInteger(MessageContact.ACCOUNT_ID);
        }
    }

    public void setValues(Cursor cursor) {

        ContentValues values = new ContentValues();

        DatabaseUtils.cursorLongToContentValuesIfPresent(cursor, values,
                MessageContact._ID);
        DatabaseUtils.cursorLongToContentValuesIfPresent(cursor, values,
                MessageContact.MESSAGE_ID);
        DatabaseUtils.cursorLongToContentValuesIfPresent(cursor, values,
                MessageContact.ACCOUNT_ID);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values,
                MessageContact.NAME);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values,
                MessageContact.ADDRESS);
        DatabaseUtils.cursorIntToContentValuesIfPresent(cursor, values,
                MessageContact.ADDRESS_TYPE);
        DatabaseUtils.cursorIntToContentValuesIfPresent(cursor, values,
                MessageContact.FIELD_TYPE);

        setValues(values);
    }

    /**
     * Supports Parcelable
     */
    public static final Parcelable.Creator<MessageContactValue> CREATOR = new Parcelable.Creator<MessageContactValue>() {
        @Override
        public MessageContactValue createFromParcel(Parcel in) {
            return new MessageContactValue(in);
        }

        @Override
        public MessageContactValue[] newArray(int size) {
            return new MessageContactValue[size];
        }
    };

    public static ArrayList<MessageContactValue> restoreWithMessageId(Context context,
            long messageId) {
        ArrayList<MessageContactValue> contacts = new ArrayList<MessageContactValue>();

        Cursor c = context.getContentResolver().query(MessageContract.MessageContact.CONTENT_URI,
                MessageContract.MessageContact.DEFAULT_PROJECTION,
                MessageContract.MessageContact.MESSAGE_ID + "=?",
                new String[] {
                    Long.toString(messageId)
                }, null);

        try {
            if (c != null) {
                while (c.moveToNext()) {
                    contacts.add(new MessageContactValue(c));
                }
            }
        } finally
        {
            if (c != null) {
                c.close();
            }
        }

        return contacts;
    }

}
