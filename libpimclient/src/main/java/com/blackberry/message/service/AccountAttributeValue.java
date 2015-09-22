
package com.blackberry.message.service;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.blackberry.provider.AccountContract;

import java.util.ArrayList;
import java.util.List;

/**
 * Value object for an account attribute.
 */
public class AccountAttributeValue implements Parcelable {

    /**
     * Used as the ID if this account attribute has not yet been saved in the
     * account CP.
     */
    public static final int NOT_SAVED = -1;

    /**
     * Creates a new instance for the account attribute with <code>id</code>,
     * populated with values from the account CP.
     * 
     * @param context the application context
     * @param id the ID to restore
     * @return the new instance, or <code>null</code> if an account attribute
     *         with that id could not be found.
     */
    public static AccountAttributeValue restoreAccountAttributeWithId(
            Context context, long id) {

        AccountAttributeValue attribute = null;

        Uri uri = ContentUris.withAppendedId(
                AccountContract.AccountAttribute.CONTENT_URI, id);
        Cursor cursor = context.getContentResolver().query(uri,
                AccountContract.AccountAttribute.DEFAULT_PROJECTION,
                null, null, null);

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    attribute = new AccountAttributeValue(cursor);
                }
            } finally {
                cursor.close();
            }
        }

        return attribute;
    }

    /**
     * Creates a list of new instances that are associated with the
     * <code>accountId</code> in the account CP.
     * 
     * @param context the application context
     * @param accountId the account ID whose attributes are to be restored
     * @return the list of new account attribute instances. May be empty if no
     *         attributes were found for this account in the CP.
     */
    public static List<AccountAttributeValue> restoreWithAccountId(
            Context context,
            long accountId) {

        ArrayList<AccountAttributeValue> attributes = new ArrayList<AccountAttributeValue>();

        Cursor cursor = context.getContentResolver().query(
                AccountContract.AccountAttribute.CONTENT_URI,
                AccountContract.AccountAttribute.DEFAULT_PROJECTION,
                AccountContract.AccountAttribute.ACCOUNT_KEY + "=?",
                new String[] {
                Long.toString(accountId)
                }, null);

        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    attributes.add(new AccountAttributeValue(cursor));
                }
            } finally {
                cursor.close();
            }
        }

        return attributes;
    }

    /**
     * Supports Parcelable
     */
    public static final Parcelable.Creator<AccountAttributeValue> CREATOR = new Parcelable.Creator<AccountAttributeValue>() {
        @Override
        public AccountAttributeValue createFromParcel(Parcel in) {
            return new AccountAttributeValue(in);
        }

        @Override
        public AccountAttributeValue[] newArray(int size) {
            return new AccountAttributeValue[size];
        }
    };

    /**
     * The local id of this account attribute.
     */
    public long mId = NOT_SAVED; // assume not saved to start

    /**
     * The attribute type.
     */
    public String mPimType;

    /**
     * The attribute name.
     */
    public String mName;

    /**
     * The attribute value.
     */
    public byte[] mValue;

    /**
     * The attribute account ID.
     */
    public long mAccountKey;

    /**
     * Default constructor.
     */
    public AccountAttributeValue() {
    }

    /**
     * Constructs an account attribute from a cursor.
     * 
     * @param cursor the cursor
     */
    public AccountAttributeValue(Cursor cursor) {
        this();
        setValues(cursor);
    }

    /**
     * Supports Parcelable
     */
    public AccountAttributeValue(Parcel in) {
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

    /**
     * Answers whether or not this account attribute has been saved in the CP.
     * 
     * @return <code>true</code> if the attribute has been saved,
     *         <code>false</code> otherwise
     */
    public boolean isNew() {
        return mId == NOT_SAVED;
    }

    /**
     * Converts this account attribute to its content values representation.
     * 
     * @param excludeId <code>true</code> if the mId should be excluded from the
     *            resulting values set. This is useful when doing
     *            inserts/updates as the mId will part of the uri.
     * @return the content values
     */
    public ContentValues toContentValues(boolean excludeId) {
        ContentValues values = new ContentValues();

        if (!excludeId) {
            values.put(AccountContract.AccountAttribute._ID, mId);
        }

        values.put(AccountContract.AccountAttribute.ACCOUNT_KEY, mAccountKey);
        values.put(AccountContract.AccountAttribute.ATTR_NAME, mName);
        values.put(AccountContract.AccountAttribute.ATTR_VALUE, mValue);
        values.put(AccountContract.AccountAttribute.PIM_TYPE, mPimType);

        return values;
    }

    /**
     * Sets the data from this object into <code>values</code>. It is possible
     * that some values are missing, depending on the projection that was used
     * to populate this object. Check for <code>nulls</code> in the result.
     * 
     * @param values
     */
    public void setValues(ContentValues values) {

        if (values.containsKey(AccountContract.AccountAttribute._ID)) {
            mId = values.getAsLong(AccountContract.AccountAttribute._ID);
        }

        if (values.containsKey(AccountContract.AccountAttribute.ACCOUNT_KEY)) {
            mAccountKey = values
                    .getAsLong(AccountContract.AccountAttribute.ACCOUNT_KEY);
        }

        if (values.containsKey(AccountContract.AccountAttribute.ATTR_VALUE)) {
            mValue = values
                    .getAsByteArray(AccountContract.AccountAttribute.ATTR_VALUE);
        }

        mName = values.getAsString(AccountContract.AccountAttribute.ATTR_NAME);
        mPimType = values
                .getAsString(AccountContract.AccountAttribute.PIM_TYPE);
    }

    /**
     * Sets account attribute values based on the cursor contents. Not all
     * members may be set as this will be dependent on the query projection and
     * therefore null and value checks are recommended.
     * 
     * @param cursor the cursor
     */
    public void setValues(Cursor cursor) {

        ContentValues values = new ContentValues();

        DatabaseUtils.cursorLongToContentValuesIfPresent(cursor, values,
                AccountContract.AccountAttribute._ID);
        DatabaseUtils.cursorLongToContentValuesIfPresent(cursor, values,
                AccountContract.AccountAttribute.ACCOUNT_KEY);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values,
                AccountContract.AccountAttribute.ATTR_NAME);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values,
                AccountContract.AccountAttribute.PIM_TYPE);

        int valueIndex = cursor
                .getColumnIndex(AccountContract.AccountAttribute.ATTR_VALUE);

        if (valueIndex != -1) {
            values.put(AccountContract.AccountAttribute.ATTR_VALUE,
                    cursor.getBlob(valueIndex));
        }

        setValues(values);
    }

    /**
     * Helper method to insert a new account attribute into the account CP based
     * on current data values.
     * 
     * @param context the application context
     * @return the URI of the new account attribute
     */
    public Uri save(Context context) {

        if (mId > 0) {
            throw new UnsupportedOperationException();
        }

        Uri uri = context.getContentResolver().insert(
                AccountContract.AccountAttribute.CONTENT_URI,
                toContentValues(true));

        mId = ContentUris.parseId(uri);

        return uri;
    }
}
