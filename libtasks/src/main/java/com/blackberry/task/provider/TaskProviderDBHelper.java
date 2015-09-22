package com.blackberry.task.provider;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.blackberry.common.utils.LogUtils;
import com.blackberry.task.Task;
import com.blackberry.task.provider.TaskContract.TaskColumns;
import com.blackberry.task.utils.TaskUtils;

/**
 * A helper class to access tasks database.
 */
public final class TaskProviderDBHelper {

    // Any changes to the database format *must* include update-in-place code.
    public static final int DATABASE_VERSION = 1;
    public static final String RECORD_ID = "_id";

    private TaskProviderDBHelper() {
    }

    /*
     * Create the accounts table
     */
    static void createTaskTable(SQLiteDatabase db) {
        String s = " (" // BASE COLUMNS
                + TaskColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "

                // SYNC COLUMNS
                + TaskColumns.SERVER_ID + " TEXT, "
                + TaskColumns.SERVER_TIMESTAMP + " INTEGER, "
                + TaskColumns.DIRTY + " INTEGER DEFAULT 0, "
                + TaskColumns.DELETED + " INTEGER DEFAULT 0, "

                // TASK
                + TaskColumns.ACCOUNT_KEY + " INTEGER, "
                + TaskColumns.MAILBOX_KEY + " INTEGER, "
                + TaskColumns.SUBJECT + " TEXT, "
                + TaskColumns.IMPORTANCE + " INTEGER, "
                + TaskColumns.UTC_START_DATE + " INTEGER, "
                + TaskColumns.START_DATE + " INTEGER, "
                + TaskColumns.UTC_DUE_DATE + " INTEGER, "
                + TaskColumns.DUE_DATE + " INTEGER, "
                + TaskColumns.COMPLETE + " INTEGER, "
                + TaskColumns.COMPLETED_DATE + " INTEGER, "
                + TaskColumns.SENSITIVITY + " INTEGER, "
                + TaskColumns.REMINDER_SET + " INTEGER, "
                + TaskColumns.REMINDER_DATE + " INTEGER, "
                + TaskColumns.BODY + " TEXT"
                + ");";

        db.execSQL("create table " + Task.TABLE_NAME + s);
    }

    static void resetTaskTable(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            db.execSQL("drop table " + Task.TABLE_NAME);
        } catch (SQLException e) {
            LogUtils.e(TaskUtils.TAG, "Unable to reset task table: " + e.toString());
        }
        createTaskTable(db);
    }

    protected static class DatabaseHelper extends SQLiteOpenHelper {
        Context mContext;

        DatabaseHelper(Context context, String name) {
            super(context, name, null, DATABASE_VERSION);
            mContext = context;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.d(TaskUtils.TAG, "Creating TaskProvider database");
            // Create all tables here; each class has its own method
            createTaskTable(db);
        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
        }
    }
}