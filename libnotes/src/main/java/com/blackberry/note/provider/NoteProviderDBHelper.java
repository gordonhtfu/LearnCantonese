package com.blackberry.note.provider;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.blackberry.common.utils.LogUtils;
import com.blackberry.note.Note;
import com.blackberry.note.provider.NoteContract.NoteColumns;
import com.blackberry.note.utils.NoteUtils;

/**
 * A helper class to access notes database.
 */
public final class NoteProviderDBHelper {

    // Any changes to the database format *must* include update-in-place code.
    public static final int DATABASE_VERSION = 1;
    public static final String RECORD_ID = "_id";
    private static final String MESSAGE_CLASS_DEFAULT_VALUE = "IPM.StickyNote";

    private NoteProviderDBHelper() {
    }

    /*
     * Create the accounts table
     */
    static void createNoteTable(SQLiteDatabase db) {
        String s = " (" // BASE COLUMNS
                        + NoteColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "

                        // SYNC COLUMNS
                        + NoteColumns.SERVER_ID + " TEXT, "
                        + NoteColumns.SERVER_TIMESTAMP + " INTEGER, "
                        + NoteColumns.DIRTY + " INTEGER DEFAULT 0, "
                        + NoteColumns.DELETED + " INTEGER DEFAULT 0, "

                        // NOTE
                        + NoteColumns.ACCOUNT_KEY + " INTEGER, "
                        + NoteColumns.MAILBOX_KEY + " INTEGER, "
                        + NoteColumns.SUBJECT + " TEXT, "
                        + NoteColumns.MESSAGE_CLASS + " TEXT DEFAULT '" +
                                MESSAGE_CLASS_DEFAULT_VALUE + "', "
                        + NoteColumns.LAST_MODIFIED_DATE + " INTEGER, "
                        + NoteColumns.BODY + " TEXT"
                        + ");";

        db.execSQL("create table " + Note.TABLE_NAME + s);
    }

    static void resetNoteTable(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            db.execSQL("drop table " + Note.TABLE_NAME);
        } catch (SQLException e) {
            LogUtils.e(NoteUtils.TAG, "Unable to reset note table: " + e.toString());
        }
        createNoteTable(db);
    }

    protected static class DatabaseHelper extends SQLiteOpenHelper {
        Context mContext;

       DatabaseHelper(Context context, String name) {
            super(context, name, null, DATABASE_VERSION);
            mContext = context;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.d(NoteUtils.TAG, "Creating NoteProvider database");
            // Create all tables here; each class has its own method
            createNoteTable(db);
        }

        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
        }
    }
}