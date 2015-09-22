package com.blackberry.note;

import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import com.blackberry.note.provider.NoteContract;
import com.blackberry.note.provider.NoteContract.NoteColumns;


public class Note implements NoteColumns {

    public static final String TABLE_NAME = "Notes";
    public static final String URI_SUFFIX = "note";
    public static final Uri CONTENT_URI =
            Uri.parse("content://" + NoteContract.AUTHORITY + "/" + URI_SUFFIX);

    public static final String[] PROJECTION_ID = new String[] { Note._ID };

    public static final int COLUMN_ID_FOR_PROJECTION_ALL = 0;
    public static final int COLUMN_SERVER_ID_FOR_PROJECTION_ALL = 1;
    public static final int COLUMN_SERVER_TIMESTAMP_FOR_PROJECTION_ALL = 2;
    public static final int COLUMN_DIRTY_FOR_PROJECTION_ALL = 3;
    public static final int COLUMN_DELETE_FOR_PROJECTION_ALL = 4;
    public static final int COLUMN_ACCOUNT_KEY_FOR_PROJECTION_ALL = 5;
    public static final int COLUMN_MAILBOX_KEY_FOR_PROJECTION_ALL = 6;
    public static final int COLUMN_SUBJECT_FOR_PROJECTION_ALL = 7;
    public static final int COLUMN_MESSAGE_CLASS_FOR_PROJECTION_ALL = 8;
    public static final int COLUMN_LAST_MODIFIED_DATE_FOR_PROJECTION_ALL = 9;
    public static final int COLUMN_BODY_FOR_PROJECTION_ALL = 10;

    public static final String[] PROJECTION_ALL = new String[] {
        BaseColumns._ID, // 0
        NoteColumns.SERVER_ID, // 1
        NoteColumns.SERVER_TIMESTAMP, // 2
        NoteColumns.DIRTY, // 3
        NoteColumns.DELETED, // 4
        NoteColumns.ACCOUNT_KEY, // 5
        NoteColumns.MAILBOX_KEY, // 6
        NoteColumns.SUBJECT, // 7
        NoteColumns.MESSAGE_CLASS, // 8
        NoteColumns.LAST_MODIFIED_DATE, // 9
        NoteColumns.BODY // 10
    };

    // Newly created objects get this id
    public static final int NOT_SAVED = -1;

    // The base Uri that this piece of content came from
    public Uri mBaseUri;

    public long mId = NOT_SAVED;
    public String mServerId;
    public long mServerTimeStamp;
    public boolean mDirty = false;
    public boolean mDeleted = false;
    public long mAccountKey;
    public long mMailboxKey;
    public String mSubject;
    public String mMessageClass;
    public long mLastModifiedDate;  // ms since epoch
    public String mBody;
    // TODO: Add category support

    public Note() {
        mBaseUri =  CONTENT_URI;
    }

    /**
     * Note to content provider operation.
     *
     * @return the content provider operation
     */
    public ContentProviderOperation toOperation() {
        boolean isNew = !isSaved();

        ContentProviderOperation.Builder b;
        if (isNew) {
            b = ContentProviderOperation.newInsert(mBaseUri);
        } else {
            b = ContentProviderOperation.newUpdate(ContentUris.withAppendedId(mBaseUri, mId));
        }

        b.withValues(toContentValues());

        return b.build();
    }

    /**
     * Checks if is saved.
     *
     * @return true, if is saved
     */
    public boolean isSaved() {
        return mId != NOT_SAVED;
    }

    /**
     * Note to content values.
     *
     * @return the content values
     */
    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();

        values.put(NoteColumns.SERVER_ID, mServerId);
        values.put(NoteColumns.SERVER_TIMESTAMP, mServerTimeStamp);
        values.put(NoteColumns.DIRTY, mDirty);
        values.put(NoteColumns.DELETED, mDeleted);
        values.put(NoteColumns.ACCOUNT_KEY, mAccountKey);
        values.put(NoteColumns.MAILBOX_KEY, mMailboxKey);
        values.put(NoteColumns.SUBJECT, mSubject);
        values.put(NoteColumns.MESSAGE_CLASS, mMessageClass);
        values.put(NoteColumns.LAST_MODIFIED_DATE, mLastModifiedDate);
        values.put(NoteColumns.BODY, mBody);

        return values;
    }

    /**
     * Restore note from cursor.
     *
     * @param cursor the cursor
     * @return the note
     */
    public static Note restore(Cursor cursor) {
        Note note = new Note();
        note.mId = cursor.getLong(COLUMN_ID_FOR_PROJECTION_ALL);
        note.mServerId = cursor.getString(COLUMN_SERVER_ID_FOR_PROJECTION_ALL);
        note.mServerTimeStamp = cursor.getLong(COLUMN_SERVER_TIMESTAMP_FOR_PROJECTION_ALL);
        note.mDirty = cursor.getInt(COLUMN_DIRTY_FOR_PROJECTION_ALL) == 1;
        note.mDeleted = cursor.getInt(COLUMN_DELETE_FOR_PROJECTION_ALL) == 1;
        note.mAccountKey = cursor.getLong(COLUMN_ACCOUNT_KEY_FOR_PROJECTION_ALL);
        note.mMailboxKey = cursor.getLong(COLUMN_MAILBOX_KEY_FOR_PROJECTION_ALL);
        note.mSubject = cursor.getString(COLUMN_SUBJECT_FOR_PROJECTION_ALL);
        note.mMessageClass = cursor.getString(COLUMN_MESSAGE_CLASS_FOR_PROJECTION_ALL);
        note.mLastModifiedDate = cursor.getLong(COLUMN_LAST_MODIFIED_DATE_FOR_PROJECTION_ALL);
        note.mBody = cursor.getString(COLUMN_BODY_FOR_PROJECTION_ALL);
        return note;
    }

}
