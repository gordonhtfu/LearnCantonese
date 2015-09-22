package com.blackberry.task;

import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import com.blackberry.provider.SyncContract;
import com.blackberry.task.provider.TaskContract;
import com.blackberry.task.provider.TaskContract.TaskColumns;

/** The Task object. */
public class Task implements TaskColumns {

    public static final String TABLE_NAME = "Tasks";
    public static final String URI_SUFFIX = "task";
    public static final Uri CONTENT_URI =
            Uri.parse("content://" + TaskContract.AUTHORITY + "/" + URI_SUFFIX);

    public static final String[] PROJECTION_ID = new String[] { Task._ID };

    public static final int COLUMN_ID_FOR_PROJECTION_ALL = 0;
    public static final int COLUMN_SERVER_ID_FOR_PROJECTION_ALL = 1;
    public static final int COLUMN_SERVER_TIMESTAMP_FOR_PROJECTION_ALL = 2;
    public static final int COLUMN_DIRTY_FOR_PROJECTION_ALL = 3;
    public static final int COLUMN_DELETE_FOR_PROJECTION_ALL = 4;
    public static final int COLUMN_ACCOUNT_KEY_FOR_PROJECTION_ALL = 5;
    public static final int COLUMN_MAILBOX_KEY_FOR_PROJECTION_ALL = 6;
    public static final int COLUMN_SUBJECT_FOR_PROJECTION_ALL = 7;
    public static final int COLUMN_IMPORTANCE_FOR_PROJECTION_ALL = 8;
    public static final int COLUMN_UTC_START_DATE_FOR_PROJECTION_ALL = 9;
    public static final int COLUMN_START_DATE_FOR_PROJECTION_ALL = 10;
    public static final int COLUMN_UTC_DUE_DATE_FOR_PROJECTION_ALL = 11;
    public static final int COLUMN_DUE_DATE_FOR_PROJECTION_ALL = 12;
    public static final int COLUMN_COMPLETE_FOR_PROJECTION_ALL = 13;
    public static final int COLUMN_COMPLETED_DATE_FOR_PROJECTION_ALL = 14;
    public static final int COLUMN_SENSITIVITY_FOR_PROJECTION_ALL = 15;
    public static final int COLUMN_REMINDER_SET_FOR_PROJECTION_ALL = 16;
    public static final int COLUMN_REMINDER_DATE_FOR_PROJECTION_ALL = 17;
    public static final int COLUMN_BODY_FOR_PROJECTION_ALL = 18;

    public static final String[] PROJECTION_ALL = new String[] {
        BaseColumns._ID, // 0
        TaskColumns.SERVER_ID, // 1
        TaskColumns.SERVER_TIMESTAMP, // 2
        TaskColumns.DIRTY, // 3
        TaskColumns.DELETED, // 4
        TaskColumns.ACCOUNT_KEY, // 5
        TaskColumns.MAILBOX_KEY, // 6
        TaskColumns.SUBJECT, // 7
        TaskColumns.IMPORTANCE, // 8
        TaskColumns.UTC_START_DATE, // 9
        TaskColumns.START_DATE, // 10
        TaskColumns.UTC_DUE_DATE, // 11
        TaskColumns.DUE_DATE, // 12
        TaskColumns.COMPLETE, // 13
        TaskColumns.COMPLETED_DATE, // 14
        TaskColumns.SENSITIVITY, // 15
        TaskColumns.REMINDER_SET, // 16
        TaskColumns.REMINDER_DATE, // 17
        TaskColumns.BODY // 18
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
    public int mImportance;
    public long mUtcStartDate;
    public long mStartDate;
    public long mUtcDueDate;
    public long mDueDate;
    public boolean mComplete = false;
    public long mCompletedDate;
    public int mSensitivity;
    public boolean mReminderSet = false;
    public long mReminderDate;
    public String mBody;

    /** Task constructor. */
    public Task() {
        mBaseUri =  CONTENT_URI;
    }

    /**
     * Task to content provider operation.
     *
     * @param fromSyncAdapter Set to true if the operation is from sync adapter.
     *
     * @return the content provider operation
     */
    public ContentProviderOperation toOperation(boolean fromSyncAdapter) {
        boolean isNew = !isSaved();

        ContentProviderOperation.Builder b;
        if (isNew) {
            b = ContentProviderOperation.newInsert(fromSyncAdapter
                    ? SyncContract.generateContentUri(mBaseUri, true)
                    : mBaseUri);
        } else {
            b = ContentProviderOperation.newUpdate(fromSyncAdapter
                    ? SyncContract.generateContentUri(mBaseUri, mId, true)
                    : ContentUris.withAppendedId(mBaseUri, mId));
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
     * Task to content values.
     *
     * @return the content values
     */
    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();

        values.put(TaskColumns.SERVER_ID, mServerId);
        values.put(TaskColumns.SERVER_TIMESTAMP, mServerTimeStamp);
        values.put(TaskColumns.DIRTY, mDirty);
        values.put(TaskColumns.DELETED, mDeleted);
        values.put(TaskColumns.ACCOUNT_KEY, mAccountKey);
        values.put(TaskColumns.MAILBOX_KEY, mMailboxKey);
        values.put(TaskColumns.SUBJECT, mSubject);
        values.put(TaskColumns.IMPORTANCE, mImportance);
        values.put(TaskColumns.UTC_START_DATE, mUtcStartDate);
        values.put(TaskColumns.START_DATE, mStartDate);
        values.put(TaskColumns.UTC_DUE_DATE, mUtcDueDate);
        values.put(TaskColumns.DUE_DATE, mDueDate);
        values.put(TaskColumns.COMPLETE, mComplete);
        values.put(TaskColumns.COMPLETED_DATE, mCompletedDate);
        values.put(TaskColumns.SENSITIVITY, mSensitivity);
        values.put(TaskColumns.REMINDER_SET, mReminderSet);
        values.put(TaskColumns.REMINDER_DATE, mReminderDate);
        values.put(TaskColumns.BODY, mBody);

        return values;
    }

    /**
     * Restore task from cursor.
     *
     * @param cursor the cursor
     * @return the task
     */
    public static Task restore(Cursor cursor) {
        Task task = new Task();
        task.mId = cursor.getLong(COLUMN_ID_FOR_PROJECTION_ALL);
        task.mServerId = cursor.getString(COLUMN_SERVER_ID_FOR_PROJECTION_ALL);
        task.mServerTimeStamp = cursor.getLong(COLUMN_SERVER_TIMESTAMP_FOR_PROJECTION_ALL);
        task.mDirty = cursor.getInt(COLUMN_DIRTY_FOR_PROJECTION_ALL) == 1;
        task.mDeleted = cursor.getInt(COLUMN_DELETE_FOR_PROJECTION_ALL) == 1;
        task.mAccountKey = cursor.getLong(COLUMN_ACCOUNT_KEY_FOR_PROJECTION_ALL);
        task.mMailboxKey = cursor.getLong(COLUMN_MAILBOX_KEY_FOR_PROJECTION_ALL);
        task.mSubject = cursor.getString(COLUMN_SUBJECT_FOR_PROJECTION_ALL);
        task.mImportance = cursor.getInt(COLUMN_IMPORTANCE_FOR_PROJECTION_ALL);
        task.mUtcStartDate = cursor.getLong(COLUMN_UTC_START_DATE_FOR_PROJECTION_ALL);
        task.mStartDate = cursor.getLong(COLUMN_START_DATE_FOR_PROJECTION_ALL);
        task.mUtcDueDate = cursor.getLong(COLUMN_UTC_DUE_DATE_FOR_PROJECTION_ALL);
        task.mDueDate = cursor.getLong(COLUMN_DUE_DATE_FOR_PROJECTION_ALL);
        task.mComplete = cursor.getInt(COLUMN_COMPLETE_FOR_PROJECTION_ALL) == 1;
        task.mCompletedDate = cursor.getLong(COLUMN_COMPLETED_DATE_FOR_PROJECTION_ALL);
        task.mSensitivity = cursor.getInt(COLUMN_SENSITIVITY_FOR_PROJECTION_ALL);
        task.mReminderSet = cursor.getInt(COLUMN_REMINDER_SET_FOR_PROJECTION_ALL) == 1;
        task.mReminderDate = cursor.getLong(COLUMN_REMINDER_DATE_FOR_PROJECTION_ALL);
        task.mBody = cursor.getString(COLUMN_BODY_FOR_PROJECTION_ALL);
        return task;
    }
}