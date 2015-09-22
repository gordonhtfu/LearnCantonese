
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
 * Value object for an account.
 */
public class AccountValue implements Parcelable {

    /**
     * Used as the ID if this account attribute has not yet been saved in the
     * account CP.
     */
    public static final int NOT_SAVED = -1;

    /**
     * Creates a new instance of the account with <code>id</code>, populated
     * with values from the account CP. Excludes account attributes.
     * 
     * @param context the application context
     * @param id the ID to restore
     * @return the new instance, or <code>null</code> if an account with that id
     *         could not be found.
     */
    public static AccountValue restoreAccountWithId(Context context, long id) {
        AccountValue message = null;
        Uri uri = ContentUris.withAppendedId(
                AccountContract.Account.CONTENT_URI, id);
        Cursor cursor = context.getContentResolver().query(uri,
                AccountContract.Account.DEFAULT_PROJECTION,
                null, null, null);

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    message = new AccountValue(cursor);
                }
            } finally {
                cursor.close();
            }
        }

        return message;
    }

    /**
     * Creates a new instance of the account with <code>id</code>, populated
     * with values and account attributes from the account CP.
     * 
     * @param context the application context
     * @param id the ID of the account to restore
     * @return
     */
    public static AccountValue restoreAccountWithIdIncludingAttributes(
            Context context, long id) {

        AccountValue account = restoreAccountWithId(context, id);
        account.restoreAccountAttributes(context);

        return account;
    }

    /**
     * Supports Parcelable.
     */
    public static final Parcelable.Creator<AccountValue> CREATOR = new Parcelable.Creator<AccountValue>() {
        @Override
        public AccountValue createFromParcel(Parcel in) {
            return new AccountValue(in);
        }

        @Override
        public AccountValue[] newArray(int size) {
            return new AccountValue[size];
        }
    };

    /**
     * The local id.
     */
    public long mId = NOT_SAVED; // assume not saved to start

    /**
     * The display name (user defined).
     */
    public String mDisplayName;

    /**
     * The account name, which may correspond to its name in AccountManager.
     */
    public String mName;

    /**
     * The account type, which may correspond to its type in AccountManager.
     */
    public String mType;

    /**
     * The content provider authority.
     */
    public String mContentProviderAuthority;

    /**
     * The account status.
     */
    public int mStatus;

    /**
     * The capabilities that this account supports.
     */
    public long mCapabilities;

    /**
     * The install package name (will be determined at runtime and override if
     * set by calling app).
     */
    public String mPackageName;

    /**
     * The name of the application that this account belongs to.
     */
    public String mApplicationName;

    /**
     * The ID of the account icon resource (e.g., R.drawable.ic_account).
     */
    public int mAccountIcon;

    /**
     * The description.
     */
    public String mDescription;

    /**
     * The source ID for the account. If underlying application uses their own
     * account they can add it for easy mapping.
     */
    public long mLocalAccountId;

    /**
     * The list of account attributes.
     */
    private List<AccountAttributeValue> mAttributes;

    /**
     * Default constructor.
     */
    public AccountValue() {
        mAttributes = new ArrayList<AccountAttributeValue>();
    }

    /**
     * Constructs a new account from a cursor.
     * 
     * @param cursor the cursor
     */
    public AccountValue(Cursor cursor) {
        this();
        setValues(cursor);
    }

    /**
     * Supports Parcelable.
     */
    public AccountValue(Parcel in) {
        setValues(ContentValues.CREATOR.createFromParcel(in));
        mAttributes = new ArrayList<AccountAttributeValue>();
        in.readList(mAttributes, AccountAttributeValue.class.getClassLoader());
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
     * Adds an account attribute.
     * 
     * @param attribute the attribute to add
     */
    public void add(AccountAttributeValue attribute) {
        mAttributes.add(attribute);
    }

    /**
     * Adds a new list of account attributes.
     * 
     * @param attributes the account attributes to add
     */
    public void addAccountAttributes(List<AccountAttributeValue> attributes) {
        mAttributes.addAll(attributes);
    }

    /**
     * Removes all of the account attributes.
     */
    public void clearAccountAttributes() {
        mAttributes.clear();
    }

    /**
     * Gets all of the account attributes related to this account.
     * 
     * @return the attribute list
     */
    public List<AccountAttributeValue> getAccountAttributes() {
        return mAttributes;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        toContentValues(false).writeToParcel(dest, flags);
        dest.writeList(mAttributes);
    }

    /**
     * Converts the account to content values. Does not include the account
     * attributes list.
     * 
     * @param excludeId <code>true</code> if the mId should not be included in
     *            the values set. This is useful when doing inserts/updates as
     *            the mId will part of the uri.
     * @return the content values
     */
    public ContentValues toContentValues(boolean excludeId) {

        ContentValues values = new ContentValues();

        if (!excludeId) {
            values.put(AccountContract.Account._ID, mId);
        }

        values.put(AccountContract.Account.DISPLAY_NAME, mDisplayName);
        values.put(AccountContract.Account.NAME, mName);
        values.put(AccountContract.Account.TYPE, mType);
        values.put(AccountContract.Account.CONTENT_PROVIDER_AUTHORITY,
                mContentProviderAuthority);
        values.put(AccountContract.Account.STATUS, mStatus);
        values.put(AccountContract.Account.CAPABILITIES, mCapabilities);
        values.put(AccountContract.Account.PACKAGE_NAME, mPackageName);
        values.put(AccountContract.Account.APPLICATION_NAME, mApplicationName);
        values.put(AccountContract.Account.ACCOUNT_ICON, mAccountIcon);
        values.put(AccountContract.Account.DESCRIPTION, mDescription);
        values.put(AccountContract.Account.LOCAL_ACCOUNT_ID, mLocalAccountId);

        return values;
    }

    /**
     * Sets the data from this object into <code>values</code>. It is possible
     * that some values are missing, depending on the projection that was used
     * to populate this object. Check for <code>nulls</code> in the result.
     * 
     * @param values the content values
     */
    public void setValues(ContentValues values) {

        if (values.containsKey(AccountContract.Account._ID)) {
            mId = values.getAsLong(AccountContract.Account._ID);
        }

        if (values.containsKey(AccountContract.Account.STATUS)) {
            mStatus = values.getAsInteger(AccountContract.Account.STATUS);
        }

        if (values.containsKey(AccountContract.Account.CAPABILITIES)) {
            mCapabilities = values
                    .getAsLong(AccountContract.Account.CAPABILITIES);
        }

        if (values.containsKey(AccountContract.Account.ACCOUNT_ICON)) {
            mAccountIcon = values
                    .getAsInteger(AccountContract.Account.ACCOUNT_ICON);
        }

        if (values.containsKey(AccountContract.Account.LOCAL_ACCOUNT_ID)) {
            mLocalAccountId = values
                    .getAsLong(AccountContract.Account.LOCAL_ACCOUNT_ID);
        }

        mDisplayName = values.getAsString(AccountContract.Account.DISPLAY_NAME);
        mName = values.getAsString(AccountContract.Account.NAME);
        mType = values.getAsString(AccountContract.Account.TYPE);
        mContentProviderAuthority = values
                .getAsString(AccountContract.Account.CONTENT_PROVIDER_AUTHORITY);
        mPackageName = values.getAsString(AccountContract.Account.PACKAGE_NAME);
        mApplicationName = values
                .getAsString(AccountContract.Account.APPLICATION_NAME);
        mDescription = values.getAsString(AccountContract.Account.DESCRIPTION);
    }

    /**
     * Sets account values based on the cursor contents. Not all members may be
     * set as this will be dependent on the query projection and therefore null
     * and value checks are recommended.
     * 
     * @param cursor the cursor
     */
    public void setValues(Cursor cursor) {

        ContentValues values = new ContentValues();

        DatabaseUtils.cursorLongToContentValuesIfPresent(cursor, values,
                AccountContract.Account._ID);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values,
                AccountContract.Account.DISPLAY_NAME);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values,
                AccountContract.Account.NAME);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values,
                AccountContract.Account.TYPE);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values,
                AccountContract.Account.CONTENT_PROVIDER_AUTHORITY);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values,
                AccountContract.Account.PACKAGE_NAME);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values,
                AccountContract.Account.APPLICATION_NAME);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values,
                AccountContract.Account.DESCRIPTION);
        DatabaseUtils.cursorIntToContentValuesIfPresent(cursor, values,
                AccountContract.Account.STATUS);
        DatabaseUtils.cursorLongToContentValuesIfPresent(cursor, values,
                AccountContract.Account.CAPABILITIES);
        DatabaseUtils.cursorIntToContentValuesIfPresent(cursor, values,
                AccountContract.Account.ACCOUNT_ICON);
        DatabaseUtils.cursorLongToContentValuesIfPresent(cursor, values,
                AccountContract.Account.LOCAL_ACCOUNT_ID);

        setValues(values);
    }

    /**
     * Helper method to insert a new account into the account CP based on
     * current data values.
     * 
     * @param context the application context
     * @return the uri of the new account
     */
    public Uri save(Context context) {
        if (mId > 0) {
            throw new UnsupportedOperationException();
        }
        Uri res = context.getContentResolver().insert(
                AccountContract.Account.CONTENT_URI,
                toContentValues(true));
        mId = Long.parseLong(res.getPathSegments().get(1));
        return res;
    }

    /**
     * Load the account attributes for this account.
     * 
     * @param context the application context
     */
    public void restoreAccountAttributes(Context context) {
        addAccountAttributes(AccountAttributeValue.restoreWithAccountId(
                context, mId));
    }

    /**
     * Checks whether the given capability is supported.
     *
     * @param capability the capability being checked
     * @return true, if the capability is supported
     */
    public boolean getCapabilitySupport(long capability) {
        return (capability & mCapabilities) > 0;
    }

    /**
     * Sets whether the given capabilities is supported.
     *
     * @param supported whether the capability is supported
     * @param capability the capability being set
     */
    public void setCapabilitySupport(boolean supported, long capability) {
        if (supported) {
            mCapabilities |= capability;
        } else {
            mCapabilities &= ~capability;
        }
    }
}
