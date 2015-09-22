package com.blackberry.email.provider.contract;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.blackberry.email.provider.contract.EmailContent.MailboxColumns;
import com.blackberry.email.utils.Utility;

public class VirtualMailbox extends EmailContent implements MailboxColumns, Parcelable {

    public static final String TABLE_NAME = "VirtualMailbox";

    public static Uri CONTENT_URI;

    public static void initVirtualMailbox() {
        CONTENT_URI = Uri.parse(EmailContent.CONTENT_URI + "/virtualMailbox");
    }

    public long mAccountKey;
    public int mType;

    public static final int CONTENT_ID_COLUMN = 0;
    public static final int CONTENT_ACCOUNT_KEY_COLUMN = 1;
    public static final int CONTENT_TYPE_COLUMN = 2;

    /**
     * <em>NOTE</em>: If fields are added or removed, the method {@link #getHashes()}
     * MUST be updated.
     */
    public static final String[] CONTENT_PROJECTION = new String[] {
            RECORD_ID, VirtualMailboxColumns.ACCOUNT_KEY, VirtualMailboxColumns.TYPE
    };

    public static final String WHERE_TYPE_AND_ACCOUNT_KEY =
            VirtualMailboxColumns.TYPE + "=? and " + MailboxColumns.ACCOUNT_KEY + "=?";

    public static final String[] MAILBOX_TYPE_PROJECTION = new String[] {
            VirtualMailboxColumns.TYPE
            };
    public static final int MAILBOX_TYPE_TYPE_COLUMN = 0;

    /**
     * Projection to use when reading {@link VirtualMailboxColumns#ACCOUNT_KEY}
     * for a mailbox.
     */
    public static final String[] ACCOUNT_KEY_PROJECTION = {
        VirtualMailboxColumns.ACCOUNT_KEY
    };
    public static final int ACCOUNT_KEY_PROJECTION_ACCOUNT_KEY_COLUMN = 0;

    public static final long NO_VIRTUAL_MAILBOX = -1;

    public VirtualMailbox() {
        mBaseUri = CONTENT_URI;
    }

    /**
     * Builds a new virtual mailbox with "typical" settings for a system
     * mailbox, such as a local "Drafts" mailbox. This is useful for protocols
     * like POP3 or IMAP who don't have certain local system mailboxes synced
     * with the server. Note: the mailbox is not persisted - clients must call
     * {@link #save} themselves.
     */
    public static VirtualMailbox newVirtualMailbox(Context context, long accountId, int mailboxType) {
        VirtualMailbox mailbox = new VirtualMailbox();
        mailbox.mAccountKey = accountId;
        mailbox.mType = mailboxType;
        return mailbox;
    }

    @Override
    public void restore(Cursor cursor) {
        mBaseUri = CONTENT_URI;
        mId = cursor.getLong(CONTENT_ID_COLUMN);
        mAccountKey = cursor.getLong(CONTENT_ACCOUNT_KEY_COLUMN);
        mType = cursor.getInt(CONTENT_TYPE_COLUMN);
    }

    @Override
    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put(MailboxColumns.ACCOUNT_KEY, mAccountKey);
        values.put(MailboxColumns.TYPE, mType);
        return values;
    }

    /**
     * Get the id for a virtual mailbox of a given type in an account.
     * 
     * @param context the caller's context, used to get a ContentResolver
     * @param accountId the id of the account to be queried
     * @param type the mailbox type
     * @return The id of the virtual mailbox, or
     *         {@link VirtualMailbox#NO_VIRTUAL_MAILBOX} if it doesn't exist
     */
    public static long getVirtualMailboxId(final Context context, long accountId, int type) {

        String[] bindArguments = new String[] {
                Integer.toString(type), Long.toString(accountId)
        };

        return Utility.getFirstRowLong(context, VirtualMailbox.CONTENT_URI,
                ID_PROJECTION, WHERE_TYPE_AND_ACCOUNT_KEY,
                bindArguments, null, ID_PROJECTION_COLUMN,
                NO_VIRTUAL_MAILBOX);
    }

    /**
     * Get the account id for a virtual mailbox.
     * 
     * @param context the caller's context, used to get a ContentResolver
     * @param mailboxId The id of the mailbox we're interested in, as a
     *            {@link String}.
     * @return The account id for the mailbox, or {@link Account#NO_ACCOUNT} if
     *         the mailbox doesn't exist.
     */
    public static long getVirtualMailboxAccountId(final Context context, long mailboxId) {
        Uri url = ContentUris.withAppendedId(VirtualMailbox.CONTENT_URI, mailboxId);
        return Utility.getFirstRowLong(context, url, ACCOUNT_KEY_PROJECTION, null, null, null,
                ACCOUNT_KEY_PROJECTION_ACCOUNT_KEY_COLUMN, Account.NO_ACCOUNT);
    }

    /**
     * Determines if there is a virtual mailbox with the given ID.
     * 
     * @param context the caller's context, used to get a ContentResolver
     * @param mailboxId The id of the mailbox we're interested in, as a
     *            {@link String}.
     * @return <code>true</code> if the virtual mailbox with this ID exists,
     *         <code>false</code> otherwise
     */
    public static boolean isVirtualMailbox(final Context context, long mailboxId) {
        Uri url = ContentUris.withAppendedId(VirtualMailbox.CONTENT_URI, mailboxId);
        return (Utility.getFirstRowLong(context, url, ID_PROJECTION, null, null, null,
                ID_PROJECTION_COLUMN, NO_VIRTUAL_MAILBOX) != NO_VIRTUAL_MAILBOX);
    }

    /**
     * Get the account id for a virtual mailbox.
     * 
     * @param context the caller's context, used to get a ContentResolver
     * @param mailboxId The id of the mailbox we're interested in, as a
     *            {@link String}.
     * @return The type for the mailbox, or {@link Mailbox#TYPE_NONE} if the
     *         mailbox doesn't exist.
     */
    public static int getVirtualMailboxType(final Context context, long mailboxId) {
        Uri url = ContentUris.withAppendedId(VirtualMailbox.CONTENT_URI, mailboxId);
        return Utility.getFirstRowInt(context, url, MAILBOX_TYPE_PROJECTION,
                null, null, null, MAILBOX_TYPE_TYPE_COLUMN, Mailbox.TYPE_NONE);
    }

    /**
     * Get the id for a combined mailbox of a given type
     * 
     * @param type the mailbox type for the combined mailbox
     * @return the id, as a String
     */
    public static String combinedMailboxId(final Context context, int type) {
        return Long
                .toString(getVirtualMailboxId(context,
                        Account.ACCOUNT_ID_COMBINED_VIEW, type));
    }

    /**
     * Determins if this mailbox ID represents one of the combined virtual
     * mailboxes.
     * 
     * @param context the caller's context, used to get a ContentResolver
     * @param mailboxId The id of the mailbox we're interested in, as a
     *            {@link String}.
     * @return <code>true</code> if this is a combined mailbox,
     *         <code>false</code> otherwise
     */
    public static boolean isCombinedMailbox(final Context context, long mailboxId) {
        return getVirtualMailboxAccountId(context, mailboxId) == Account.ACCOUNT_ID_COMBINED_VIEW;
    }

    /**
     * Returns a set of hashes that can identify this mailbox. These can be used to
     * determine if any of the fields have been modified.
     */
    public Object[] getHashes() {
        Object[] hash = new Object[CONTENT_PROJECTION.length];

        hash[CONTENT_ID_COLUMN]
             = mId;
        hash[CONTENT_ACCOUNT_KEY_COLUMN]
                = mAccountKey;
        hash[CONTENT_TYPE_COLUMN]
                = mType;
        return hash;
    }

    // Parcelable
    @Override
    public int describeContents() {
        return 0;
    }

    // Parcelable
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mBaseUri, flags);
        dest.writeLong(mId);
        dest.writeLong(mAccountKey);
        dest.writeInt(mType);
    }

    public VirtualMailbox(Parcel in) {
        mBaseUri = in.readParcelable(null);
        mId = in.readLong();
        mAccountKey = in.readLong();
        mType = in.readInt();
    }

    public static final Parcelable.Creator<VirtualMailbox> CREATOR = new Parcelable.Creator<VirtualMailbox>() {
        @Override
        public VirtualMailbox createFromParcel(Parcel source) {
            return new VirtualMailbox(source);
        }

        @Override
        public VirtualMailbox[] newArray(int size) {
            return new VirtualMailbox[size];
        }
    };

    @Override
    public String toString() {
        return "[Mailbox " + mId + ": " + mType + ", " + mAccountKey + "]";
    }

    /**
     * Gets the correct authority for a mailbox.
     * @param mailboxType The type of the mailbox we're interested in.
     * @return The authority for the mailbox we're interested in.
     */
    public static String getAuthority(final int mailboxType) {
        return Mailbox.getAuthority(mailboxType);
    }
}
