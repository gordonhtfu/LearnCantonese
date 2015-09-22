
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

import java.util.ArrayList;

public class FolderValue implements Parcelable {

    /**
     * this local row id of this folder
     */
    public Long mId = Long.valueOf(-1);

    /**
     * Display name of Folder
     */
    public String mDisplayName;

    /**
     * Text about this folder
     */
    public String mDescription;
    /**
     * The remote id , which typically relates to the id the server gives this
     * folder, used my SAM
     */
    public String mRemoteId;

    /**
     * The parent remote id , which typically relates to the parent id the
     * server gives this folder, used my SAM
     */
    public String mParentRemoteId;

    /**
     * The id relates to the local parent Folder id (FK) that this Folder is
     * contained in
     */
    public Long mParentId;

    /**
     * The account id ( from AccountProvider)
     */
    public long mAccountId;

    /**
     * The type of this folder see MessageContract.Folder.Type (inbox/outbox
     * etc)
     */
    public int mType;

    /**
     * Generic SyncData Values - used by SAM
     */
    public String mSyncData1;
    public String mSyncData2;
    public String mSyncData3;
    public String mSyncData4;
    public String mSyncData5;

    /**
     * 
     */
    public int mFlags;

    /**
     * 
     */
    public String mSyncState;

    /**
     * 
     */
    public int mCapabilities;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        toContentValues(false).writeToParcel(dest, flags);
    }

    public FolderValue() {
    }

    public FolderValue(Cursor cursor) {
        setValues(cursor);
    }

    /**
     * Supports Parcelable
     */
    public FolderValue(Parcel in) {
        setValues(ContentValues.CREATOR.createFromParcel(in));
    }

    /**
     * Supports Parcelable
     */
    public static final Parcelable.Creator<FolderValue> CREATOR = new Parcelable.Creator<FolderValue>() {
        @Override
        public FolderValue createFromParcel(Parcel in) {
            return new FolderValue(in);
        }

        @Override
        public FolderValue[] newArray(int size) {
            return new FolderValue[size];
        }
    };

    /**
     * Converts Folder members into ContentValaues
     * 
     * @return
     */
    public ContentValues toContentValues(boolean excludeId) {
        ContentValues values = new ContentValues();
        // Assign values for each row.
        
        // Assign values for each row.
        if (!excludeId) {
            values.put(MessageContract.Folder._ID, mId);
        }
       
        values.put(MessageContract.Folder.NAME, mDisplayName);
        values.put(MessageContract.Folder.DESCRIPTION, mDescription);
        values.put(MessageContract.Folder.TYPE, mType);
        values.put(MessageContract.Folder.ACCOUNT_ID, mAccountId);
        values.put(MessageContract.Folder.PARENT_ID, mParentId);
        values.put(MessageContract.Folder.REMOTE_ID, mRemoteId);
        values.put(MessageContract.Folder.PARENT_REMOTE_ID, mParentRemoteId);
        values.put(MessageContract.Folder.CAPABILITIES, mCapabilities);
        values.put(MessageContract.Folder.FLAGS, mFlags);
        values.put(MessageContract.Folder.SYNC_STATE, mSyncState);
        values.put(MessageContract.Folder.SYNC_DATA1, mSyncData1);
        values.put(MessageContract.Folder.SYNC_DATA2, mSyncData2);
        values.put(MessageContract.Folder.SYNC_DATA3, mSyncData3);
        values.put(MessageContract.Folder.SYNC_DATA4, mSyncData4);
        values.put(MessageContract.Folder.SYNC_DATA5, mSyncData5);
        return values;
    }

    /**
     * Sets Folder data members from ContentValues
     * 
     * @param values
     */
    public void setValues(ContentValues values) {
        mId = values.getAsLong(MessageContract.Folder._ID);

        mDisplayName = values.getAsString(MessageContract.Folder.NAME);
        mDescription = values.getAsString(MessageContract.Folder.DESCRIPTION);
        mRemoteId = values.getAsString(MessageContract.Folder.REMOTE_ID);
        mParentRemoteId = values.getAsString(MessageContract.Folder.PARENT_REMOTE_ID);
        mSyncData1 = values.getAsString(MessageContract.Folder.SYNC_DATA1);
        mSyncData2 = values.getAsString(MessageContract.Folder.SYNC_DATA2);
        mSyncData3 = values.getAsString(MessageContract.Folder.SYNC_DATA3);
        mSyncData4 = values.getAsString(MessageContract.Folder.SYNC_DATA4);
        mSyncData5 = values.getAsString(MessageContract.Folder.SYNC_DATA5);

        if (values.containsKey(MessageContract.Folder.PARENT_ID)) {
            mParentId = values.getAsLong(MessageContract.Folder.PARENT_ID);
        }

        if (values.containsKey(MessageContract.Folder.ACCOUNT_ID)) {
            mAccountId = values.getAsLong(MessageContract.Folder.ACCOUNT_ID);
        }

        if (values.containsKey(MessageContract.Folder.TYPE)) {
            mType = values.getAsInteger(MessageContract.Folder.TYPE);
        }

        if (values.containsKey(MessageContract.Folder.FLAGS)) {
            mFlags = values.getAsInteger(MessageContract.Folder.FLAGS);
        }

        if (values.containsKey(MessageContract.Folder.SYNC_STATE)) {
            mSyncState = values.getAsString(MessageContract.Folder.SYNC_STATE);
        }

        if (values.containsKey(MessageContract.Folder.CAPABILITIES)) {
            mCapabilities = values.getAsInteger(MessageContract.Folder.CAPABILITIES);
        }

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
                MessageContract.Folder._ID);
        DatabaseUtils.cursorLongToContentValuesIfPresent(cursor, values,
                MessageContract.Folder.ACCOUNT_ID);
        DatabaseUtils.cursorLongToContentValuesIfPresent(cursor, values,
                MessageContract.Folder.PARENT_ID);
        DatabaseUtils.cursorIntToContentValuesIfPresent(cursor, values,
                MessageContract.Folder.TYPE);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values,
                MessageContract.Folder.NAME);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values,
                MessageContract.Folder.DESCRIPTION);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values,
                MessageContract.Folder.REMOTE_ID);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values,
                MessageContract.Folder.PARENT_REMOTE_ID);
        DatabaseUtils.cursorIntToContentValuesIfPresent(cursor, values,
                MessageContract.Folder.FLAGS);
        DatabaseUtils.cursorIntToContentValuesIfPresent(cursor, values,
                MessageContract.Folder.CAPABILITIES);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values,
                MessageContract.Folder.SYNC_STATE);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values,
                MessageContract.Folder.SYNC_DATA1);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values,
                MessageContract.Folder.SYNC_DATA2);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values,
                MessageContract.Folder.SYNC_DATA3);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values,
                MessageContract.Folder.SYNC_DATA4);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values,
                MessageContract.Folder.SYNC_DATA5);

        setValues(values);
    }

    public static FolderValue restoreFolderWithId(Context context, Long id) {
        FolderValue folderRet = null;
        Uri u = ContentUris.withAppendedId(MessageContract.Folder.CONTENT_URI, id);
        Cursor c = context.getContentResolver().query(u, MessageContract.Folder.ALL_PROJECTION,
                null, null, null);

        try {
            if (c != null && c.moveToFirst()) {
                folderRet = new FolderValue(c);
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }

        return folderRet;
    }

    public static ArrayList<FolderValue> restoreFolders(Context context, Long[] ids) {
        ArrayList<FolderValue> folders = new ArrayList<FolderValue>();
        if (ids != null && (ids.length > 0)) {
            for (Long id : ids) {
                FolderValue folder = restoreFolderWithId(context, id);
                if (folder != null) {
                    folders.add(folder);
                }
            }
        }
        return folders;
    }

    /**
     * Helper method to insert a new folder int the MCP based on current data
     * values
     * 
     * @param context
     * @return
     */
    public Uri save(Context context) {
        if (isSaved()) {
            throw new UnsupportedOperationException();
        }
        Uri res = context.getContentResolver().insert(MessageContract.Folder.CONTENT_URI,
                toContentValues(true));
        mId = Long.parseLong(res.getPathSegments().get(1));
        return res;
    }

    public boolean isSaved(){
        return mId > 0 ;
    }
}
