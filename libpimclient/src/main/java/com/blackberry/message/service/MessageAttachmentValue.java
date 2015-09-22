
package com.blackberry.message.service;

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

/**
 * Value Object for message attachment
 * 
 * @author vrudenko
 */
public class MessageAttachmentValue implements Parcelable {

    public static final String ATTACHMENT_PROVIDER_LEGACY_URI_PREFIX =
            "content://com.blackberry.email.attachmentprovider";

    public static final String CACHED_FILE_QUERY_PARAM = "filePath";

    // Instruct Rfc822Output to 1) not use Content-Disposition and 2) use
    // multipart/alternative
    // with this attachment. This is only valid if there is one and only one
    // attachment and
    // that attachment has this flag set
    public static final int FLAG_ICS_ALTERNATIVE_PART = 1 << 0;
    // Indicate that this attachment has been requested for downloading by the
    // user; this is
    // the highest priority for attachment downloading
    public static final int FLAG_DOWNLOAD_USER_REQUEST = 1 << 1;
    // Indicate that this attachment needs to be downloaded as part of an
    // outgoing forwarded
    // message
    public static final int FLAG_DOWNLOAD_FORWARD = 1 << 2;
    // Indicates that the attachment download failed in a non-recoverable manner
    public static final int FLAG_DOWNLOAD_FAILED = 1 << 3;
    // Allow "room" for some additional download-related flags here
    // Indicates that the attachment will be smart-forwarded
    public static final int FLAG_SMART_FORWARD = 1 << 8;
    // Indicates that the attachment cannot be forwarded due to a policy
    // restriction
    public static final int FLAG_POLICY_DISALLOWS_DOWNLOAD = 1 << 9;
    // Indicates that this is a dummy placeholder attachment.
    public static final int FLAG_DUMMY_ATTACHMENT = 1 << 10;

    public long mId;
    public String mFileName;
    public String mMimeType;
    public long mSize;
    public long mMessageId;
    public long mAccountId;
    public int mState;
    private String mContentUri;

    // from emailcontent provider
    public int mFlags;
    public String mContentId;

    private String mCachedFileUri;
    public String mLocation;
    public String mEncoding;
    public String mContent; // Not currently used

    public byte[] mContentBytes;

    public int mUiDestination;
    public int mUiDownloadedSize;

    // data, if opened already
    private ParcelFileDescriptor mFd;

    public MessageAttachmentValue() {
    }

    public MessageAttachmentValue(Cursor cursor) {
        setValues(cursor);
    }

    /**
     * Set file descriptor of the attachment
     * 
     * @param parcelFileDescriptor descriptor to set
     */
    public void setFileDescriptor(ParcelFileDescriptor parcelFileDescriptor) {
        mFd = parcelFileDescriptor;
    }

    /**
     * Get file descriptor of the attachment
     * 
     * @return file descriptor if set, NULL otherwise
     */
    public ParcelFileDescriptor getFileDescriptor() {
        return mFd;
    }

    public MessageAttachmentValue(Parcel in) {
        setValues(ContentValues.CREATOR.createFromParcel(in));
        int exists = in.readInt();
        if (exists > 0) {
            mFd = ParcelFileDescriptor.CREATOR.createFromParcel(in);
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        toContentValues(false).writeToParcel(dest, flags);
        if (mFd != null) {
            dest.writeInt(1);
            mFd.writeToParcel(dest, flags);
        } else {
            dest.writeInt(0);
        }
    }

    public static final Parcelable.Creator<MessageAttachmentValue> CREATOR = new Parcelable.Creator<MessageAttachmentValue>() {
        public MessageAttachmentValue createFromParcel(Parcel in) {
            return new MessageAttachmentValue(in);
        }

        public MessageAttachmentValue[] newArray(int size) {
            return new MessageAttachmentValue[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    public ContentValues toContentValues(boolean excludeId) {
        ContentValues values = new ContentValues();

        if (!excludeId) {
            values.put(MessageContract.MessageAttachment._ID, mId);
        }
        values.put(MessageContract.MessageAttachment.NAME, mFileName);
        values.put(MessageContract.MessageAttachment.MIME_TYPE, mMimeType);
        values.put(MessageContract.MessageAttachment.SIZE, mSize);
        values.put(MessageContract.MessageAttachment.ACCOUNT_ID, mAccountId);
        values.put(MessageContract.MessageAttachment.MESSAGE_ID, mMessageId);
        values.put(MessageContract.MessageAttachment.STATE, mState);
        values.put(MessageContract.MessageAttachment.URI, mContentUri);

        return values;
    }

    public void setValues(ContentValues values) {
        mId = values.getAsLong(MessageContract.MessageAttachment._ID);
        mFileName = values.getAsString(MessageContract.MessageAttachment.NAME);
        mMimeType = values.getAsString(MessageContract.MessageAttachment.MIME_TYPE);
        mContentUri = values.getAsString(MessageContract.MessageAttachment.URI);

        if (values.containsKey(MessageContract.MessageAttachment.SIZE)) {
            mSize = values.getAsLong(MessageContract.MessageAttachment.SIZE);
        }

        if (values.containsKey(MessageContract.MessageAttachment.ACCOUNT_ID)) {
            mAccountId = values.getAsLong(MessageContract.MessageAttachment.ACCOUNT_ID);
        }

        if (values.containsKey(MessageContract.MessageAttachment.MESSAGE_ID)) {
            mMessageId = values.getAsLong(MessageContract.MessageAttachment.MESSAGE_ID);
        }

        if (values.containsKey(MessageContract.MessageAttachment.STATE)) {
            mState = values.getAsInteger(MessageContract.MessageAttachment.STATE);
        }
    }

    public void setValues(Cursor cursor) {
        ContentValues values = new ContentValues();
        DatabaseUtils.cursorLongToContentValuesIfPresent(cursor, values,
                MessageContract.MessageAttachment._ID);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values,
                MessageContract.MessageAttachment.NAME);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values,
                MessageContract.MessageAttachment.MIME_TYPE);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values,
                MessageContract.MessageAttachment.URI);
        DatabaseUtils.cursorIntToContentValuesIfPresent(cursor, values,
                MessageContract.MessageAttachment.SIZE);
        DatabaseUtils.cursorLongToContentValuesIfPresent(cursor, values,
                MessageContract.MessageAttachment.ACCOUNT_ID);
        DatabaseUtils.cursorLongToContentValuesIfPresent(cursor, values,
                MessageContract.MessageAttachment.MESSAGE_ID);

        // left over from emailprovider -- need to see if we need these
        // mContentId = cursor.getString(CONTENT_CONTENT_ID_COLUMN);
        // mContentUri = cursor.getString(CONTENT_CONTENT_URI_COLUMN);
        // mCachedFileUri = cursor.getString(CONTENT_CACHED_FILE_COLUMN);
        // mMessageKey = cursor.getLong(CONTENT_MESSAGE_ID_COLUMN);
        // mLocation = cursor.getString(CONTENT_LOCATION_COLUMN);
        // mEncoding = cursor.getString(CONTENT_ENCODING_COLUMN);
        // mContent = cursor.getString(CONTENT_CONTENT_COLUMN);
        // mFlags = cursor.getInt(CONTENT_FLAGS_COLUMN);
        // mContentBytes = cursor.getBlob(CONTENT_CONTENT_BYTES_COLUMN);

        // mUiState = cursor.getInt(CONTENT_UI_STATE_COLUMN);
        // mUiDestination = cursor.getInt(CONTENT_UI_DESTINATION_COLUMN);
        // mUiDownloadedSize = cursor.getInt(CONTENT_UI_DOWNLOADED_SIZE_COLUMN);

        setValues(values);
    }

    public void setCachedFileUri(String cachedFile) {
        mCachedFileUri = cachedFile;
    }

    public String getCachedFileUri() {
        return mCachedFileUri;
    }

    public void setContentUri(String contentUri) {
        mContentUri = contentUri;
    }

    public String getContentUri() {
        return mContentUri;
        // If we're not using the legacy prefix and the uri IS, we need to
        // modify it
        // if (!Attachment.sUsingLegacyPrefix &&
        // mContentUri.startsWith(ATTACHMENT_PROVIDER_LEGACY_URI_PREFIX)) {
        // // In an upgrade scenario, we may still have legacy attachment Uri's
        // // Skip past content://
        // int prefix = mContentUri.indexOf('/', 10);
        // if (prefix > 0) {
        // // Create a proper uri string using the actual provider
        // return ATTACHMENT_PROVIDER_URI_PREFIX + "/" +
        // mContentUri.substring(prefix);
        // } else {
        // LogUtils.e("Attachment", "Improper contentUri format: " +
        // mContentUri);
        // // Belt & suspenders; can't really happen
        // return mContentUri;
        // }
        // } else {
        // return mContentUri;
        // }
    }

    /**
     * Restore all the Attachments of a message given its messageId
     */
    public static MessageAttachmentValue[] restoreAttachmentsWithMessageId(Context context,
            long messageId) {
        MessageAttachmentValue[] attachments = null;
        Uri uri = ContentUris.withAppendedId(MessageContract.MessageAttachment.CONTENT_URI,
                messageId);
        Cursor c = context.getContentResolver().query(uri,
                MessageContract.MessageAttachment.DEFAULT_PROJECTION,
                null, null, null);
        try {

            if (c != null) {
                int count = c.getCount();
                attachments = new MessageAttachmentValue[count];
                for (int i = 0; i < count; ++i) {
                    c.moveToNext();
                    MessageAttachmentValue attach = new MessageAttachmentValue(c);
                    attachments[i] = attach;
                }

            }else{
                attachments = new MessageAttachmentValue[0];
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }

        return attachments;
    }

    /**
     * @param context
     * @param id
     * @return
     */
    public static MessageAttachmentValue restoreWithId(Context context, long id) {
        MessageAttachmentValue att = null;
        Uri u = ContentUris.withAppendedId(MessageContract.MessageAttachment.CONTENT_URI, id);
        Cursor c = context.getContentResolver().query(u,
                MessageContract.MessageAttachment.DEFAULT_PROJECTION,
                null, null, null);
        try {
            if (c != null && c.moveToNext()) {
                att = new MessageAttachmentValue(c);
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }

        return att;
    }

}
