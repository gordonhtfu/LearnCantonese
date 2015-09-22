package com.blackberry.analytics.provider;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.blackberry.analytics.provider.AnalyticsContract.AnalyticsContact;

/**
 * Convenience class for managing contact values in analytics.
 */
public class AnalyticsContactValue implements Parcelable {

    /**
     * Supports Parcelable.
     */
    public static final Parcelable.Creator<AnalyticsContactValue> CREATOR =
            new Parcelable.Creator<AnalyticsContactValue>() {
                @Override
                public AnalyticsContactValue createFromParcel(Parcel in) {
                    return new AnalyticsContactValue(in);
                }

                @Override
                public AnalyticsContactValue[] newArray(int size) {
                    return new AnalyticsContactValue[size];
                }
            };

    /**
     * Create a new instance from the content values.
     * 
     * @param values the content values, which must be created using
     *            {@link #toContentValues()}
     * @return the new instance
     * @see #toContentValues()
     */
    public static AnalyticsContactValue fromContentValues(ContentValues values) {

        AnalyticsContactValue result = new AnalyticsContactValue();
        result.setValues(values);

        return result;
    }

    /**
     * Creates a new instance with values from a cursor.
     * 
     * @param cursor the cursor
     * @return the new instance
     */
    public static AnalyticsContactValue fromCursor(Cursor cursor) {

        AnalyticsContactValue result = new AnalyticsContactValue();

        ContentValues values = new ContentValues();

        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values,
                AnalyticsContact.ADDRESS);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values,
                AnalyticsContact.ADDRESS_CATEGORY);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values,
                AnalyticsContact.DISPLAY_NAME);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values,
                AnalyticsContact.URI);

        result.setValues(values);

        return result;
    }

    /**
     * The address data, for example an email address or a phone number.
     */
    public String mAddress;

    /**
     * The address category, such as
     * {@link AnalyticsContact.AddressCategory#EMAIL} or
     * {@link AnalyticsContact.AddressCategory#PHONE}.
     */
    public String mAddressCategory;

    /**
     * The display name for the contact.
     */
    public String mDisplayName;

    /**
     * The entity URI of the real contact in the contacts content provider.
     */
    public String mUri;

    /**
     * Creates a new instance.
     */
    public AnalyticsContactValue() {
    }

    /**
     * Creates a new instance.
     * 
     * @param address the contact address
     * @param addressCategory the contact address category
     * @param name the contact display name, if any
     * @param uri the URI of the contact in the Contacts CP, if any
     */
    public AnalyticsContactValue(String address, String addressCategory,
            String name, String uri) {

        this();

        mAddress = address;
        mAddressCategory = addressCategory;
        mDisplayName = name;
        mUri = uri;
    }

    /**
     * Supports Parcelable.
     * 
     * @param in the parcel to create the contact from
     */
    public AnalyticsContactValue(Parcel in) {
        setValues(ContentValues.CREATOR.createFromParcel(in));
    }

    private void setValues(ContentValues values) {
        mAddress = values.getAsString(AnalyticsContact.ADDRESS);
        mAddressCategory = values
                .getAsString(AnalyticsContact.ADDRESS_CATEGORY);
        mDisplayName = values.getAsString(AnalyticsContact.DISPLAY_NAME);
        mUri = values.getAsString(AnalyticsContact.URI);
    }

    /**
     * Created a ContentValues representation of this contact.
     * 
     * @return the content values
     */
    public ContentValues toContentValues() {
        ContentValues result = new ContentValues();

        result.put(AnalyticsContact.ADDRESS, mAddress);
        result.put(AnalyticsContact.ADDRESS_CATEGORY, mAddressCategory);
        result.put(AnalyticsContact.DISPLAY_NAME, mDisplayName);
        result.put(AnalyticsContact.URI, mUri);

        return result;
    }

    @Override
    public String toString() {
        return "(" + mDisplayName + ", " + mAddress + ", "
                + mAddressCategory + ", " + mUri + ")";
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (!(o instanceof AnalyticsContactValue)) {
            return false;
        }

        AnalyticsContactValue other = (AnalyticsContactValue) o;

        return TextUtils.equals(mAddress, other.mAddress)
                && TextUtils.equals(mAddressCategory, other.mAddressCategory)
                && TextUtils.equals(mDisplayName, other.mDisplayName)
                && TextUtils.equals(mUri, other.mUri);
    }

    @Override
    public int hashCode() {
        int result = 0;

        if (mAddress != null) {
            result += mAddress.hashCode();
        }

        if (mAddressCategory != null) {
            result += mAddressCategory.hashCode();
        }

        if (mDisplayName != null) {
            result += mDisplayName.hashCode();
        }

        if (mUri != null) {
            result += mUri.hashCode();
        }

        if (result == 0) {
            result = super.hashCode();
        }

        return result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        toContentValues().writeToParcel(dest, flags);
    }
}
