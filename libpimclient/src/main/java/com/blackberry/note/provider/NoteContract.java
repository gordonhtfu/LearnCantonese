
package com.blackberry.note.provider;

import android.net.Uri;
import android.provider.BaseColumns;

import com.blackberry.provider.SyncContract;

/**
 * A contract to access notes content provider.
 */
public final class NoteContract {
    private NoteContract() {
    }

    /**
     * This authority is used for writing to or querying from the note provider.
     * Note: This is set at first run and cannot be changed without breaking
     * apps that access the provider.
     */
    public static final String AUTHORITY = "com.blackberry.note.provider";

    /**
     * The path segment that follows the AUTHORITY, and precedes a note item ID.
     */
    public static final String URI_SUFFIX = "note";

    /**
     * The content:// style URL for the top-level note authority.
     */
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    /**
     * The content:// style URL prefix for note items. The note ID should be
     * appended to this URL to identify a particular note.
     */
    public static final Uri CONTENT_URI_WITH_SUFFIX = Uri.parse("content://"
            + NoteContract.AUTHORITY + "/" + URI_SUFFIX);

    public static final String CREATE_NOTE_ACTION = "com.blackberry.note.CREATE_NOTE";
    public static final String EDIT_NOTE_ACTION = "com.blackberry.note.EDIT_NOTE";
    public static final String REMOVE_NOTE_ACTION = "com.blackberry.note.REMOVE_NOTE";

    public static final String[] PROJECTION_ID = new String[] {
            NoteColumns._ID
    };

    public static final String[] PROJECTION_ALL = new String[] {
            NoteColumns._ID, // 0
            NoteColumns.SERVER_ID, // 1
            NoteColumns.SERVER_TIMESTAMP, // 2
            NoteColumns.DIRTY, // 3
            NoteColumns.DELETED, // 4
            NoteColumns.ACCOUNT_KEY, // 5
            NoteColumns.MAILBOX_KEY, // 6
            NoteColumns.SUBJECT, // 7
            NoteColumns.MESSAGE_CLASS, // 8
            NoteColumns.LAST_MODIFIED_DATE, // 9
            NoteColumns.BODY
    // 10
    };

    public interface NoteColumns extends BaseColumns, SyncContract.SyncColumns {
        // Foreign key to the Account holding this note
        public static final String ACCOUNT_KEY = "accountKey";
        // Foreign key to the Mailbox holding this note
        public static final String MAILBOX_KEY = "mailboxKey";
        // The Subject of the note
        public static final String SUBJECT = "subject";
        // The Message Class of the note
        public static final String MESSAGE_CLASS = "messageClass";
        // The Last Modified Date of the note
        public static final String LAST_MODIFIED_DATE = "lastModifiedDate";
        // The Body of the note
        public static final String BODY = "body";
    }
}
