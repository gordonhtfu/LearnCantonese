package com.blackberry.note.provider;

import java.util.Arrays;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Bundle;
import android.util.SparseArray;

import com.blackberry.common.content.ProjectionMap;
import com.blackberry.note.Note;
import com.blackberry.pimbase.provider.PIMContentProviderBase;
import com.blackberry.provider.ListItemContract;
import com.blackberry.provider.SyncContract;

/**
 * Contents provider for notes.
 */
public class NoteProvider extends PIMContentProviderBase {


    public static final String NOTE_MIMETYPE
            = "vnd.android.cursor.dir/vnd.blackberry.note";
    public static final String NOTE_ID_MIMETYPE
            = "vnd.android.cursor.item/vnd.blackberry.note";
    public static final String NOTE_ATTR_MIMETYPE
            = "vnd.android.cursor.dir/vnd.blackberry.note.attr";
    public static final String NOTE_ATTR_ID_MIMETYPE
            = "vnd.android.cursor.item/vnd.blackberry.note.attr";
    public static final String NOTES_LIST_ITEM_MIMETYPE
            = "vnd.android.cursor.dir/vnd.blackberry.notes.list";
    public static final String NOTES_LIST_ITEM_ID_MIMETYPE
            = "vnd.android.cursor.item/vnd.blackberry.notes.list";

    protected static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    private static Uri INTEGRITY_CHECK_URI;
    private static final int NOTE_BASE = 0;
    private static final int NOTE = NOTE_BASE;
    private static final int NOTE_ID = NOTE_BASE + 1;
    private static final int NOTES_LIST_ITEM = NOTE_BASE + 2;
    private static final int NOTES_LIST_ITEM_ID = NOTE_BASE + 3;

    private static final String DATABASE_NAME = "NoteProvider.db";
    private static final String NULL_COLUMN_HACK_VALUE = "foo";
    private NoteProviderDBHelper.DatabaseHelper mDbHelper;

    private static final int BASE_SHIFT = 12;  // 12 bits to the base type: 0, 0x1000, 0x2000, etc.
    private static final SparseArray<String> TABLE_NAMES;

    private static final ProjectionMap NOTES_LIST_PROJECTION_MAP;

    private static final String NOT_DELETED_SELECTION = NoteContract.NoteColumns.DELETED + " != 1";

    static {
        SparseArray<String> array = new SparseArray<String>(11);
        array.put(NOTE_BASE >> BASE_SHIFT, Note.TABLE_NAME);
        TABLE_NAMES = array;

        NOTES_LIST_PROJECTION_MAP = ProjectionMap.builder()
            .add(ListItemContract.ListItemColumns._ID,
                NoteContract.NoteColumns._ID)
            .add(ListItemContract.ListItemColumns.ACCOUNT_ID,
                NoteContract.NoteColumns.ACCOUNT_KEY)
            .add(ListItemContract.ListItemColumns.MIME_TYPE,
                "'" + NOTE_ID_MIMETYPE + "'")
            .add(ListItemContract.ListItemColumns.DUID,
                NoteContract.NoteColumns._ID)
            .add(ListItemContract.ListItemColumns.URI,
                "'" + NoteContract.CONTENT_URI_WITH_SUFFIX + "/'||" + NoteContract.NoteColumns._ID)
            .add(ListItemContract.ListItemColumns.PRIMARY_TEXT,
                NoteContract.NoteColumns.SUBJECT)
            .add(ListItemContract.ListItemColumns.SECONDARY_TEXT,
                NoteContract.NoteColumns.BODY)
            .add(ListItemContract.ListItemColumns.TERTIARY_TEXT,
                "''")
            .add(ListItemContract.ListItemColumns.TIMESTAMP,
                NoteContract.NoteColumns.LAST_MODIFIED_DATE)
            .add(ListItemContract.ListItemColumns.STATE,
                "''")
            .add(ListItemContract.ListItemColumns.GROUP_ID,
                "''")
            .build();
    }

    private static void init(Context context) {
        synchronized (URI_MATCHER) {
            if (INTEGRITY_CHECK_URI != null) {
                return;
            }

            INTEGRITY_CHECK_URI = Uri.parse("content://" + NoteContract.AUTHORITY
                    + "/integrityCheck");
            // All notes
            URI_MATCHER.addURI(NoteContract.AUTHORITY, "note", NOTE);
            // Specific note by id
            URI_MATCHER.addURI(NoteContract.AUTHORITY, "note/#", NOTE_ID);
            // All note list items
            URI_MATCHER.addURI(NoteContract.AUTHORITY, ListItemContract.URI_SUFFIX,
                    NOTES_LIST_ITEM);
            // Specific note list item by id
            URI_MATCHER.addURI(NoteContract.AUTHORITY, ListItemContract.URI_SUFFIX
                    + "/#", NOTES_LIST_ITEM_ID);
        }
    }

    //--------------
    // PIM Overrides
    //--------------
    @Override
    public void pimShutdown() {
        closeAllDatabases();
        mDbHelper = null;
    }

    @Override
    protected void initializeDatabaseHelpers() {
        mDbHelper = new NoteProviderDBHelper.DatabaseHelper(getContext(), DATABASE_NAME);
    }

    @Override
    protected SQLiteOpenHelper[] getDatabaseHelpers(boolean includeAttachedDbs) {
        return new SQLiteOpenHelper[] { mDbHelper };
    }

    @Override
    protected void closeAllDatabases() {
        if (mDbHelper != null) {
            mDbHelper.close();
        }
    }

    @Override
    protected SQLiteDatabase getWritableDatabase() {
        return mDbHelper.getWritableDatabase();
    }

    @Override
    protected SQLiteDatabase getReadableDatabase() {
        return mDbHelper.getReadableDatabase();
    }

    @Override
    public boolean pimOnCreate() {
        init(getContext());
        return true;
    }

    @Override
    public Cursor pimQuery(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        Cursor cursor = null;
        //need to handle a null projection
        if (projection == null) {
            return cursor;
        }

        int match;
        try {
            match = findMatch(uri, "query");
        } catch (IllegalArgumentException e) {
            return null;
        }

        // Filter out deleted items if we aren't being called from a sync adapter.
        if (!isUriFromSyncAdapter(uri)) {
            if (!Arrays.asList(projection).contains(NoteContract.NoteColumns.DELETED)) {
                int len = projection.length;
                projection = Arrays.copyOf(projection, len + 1);
                projection[len] = NoteContract.NoteColumns.DELETED;
            }

            selection = whereWith(NOT_DELETED_SELECTION, selection);
        }

        SQLiteDatabase db = getWritableDatabase();

        // using this table match idea for now, may change when
        // adding validation/sql injection protection
        int table = match >> BASE_SHIFT;
        String tableName = TABLE_NAMES.valueAt(table);
        String id;
        switch(match) {
            case NOTE:
                cursor = db.query(tableName, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            case NOTE_ID:
                id = uri.getPathSegments().get(1);
                cursor = db.query(tableName, projection, whereWithId(id, selection),
                        selectionArgs, null, null, sortOrder);
                break;
            case NOTES_LIST_ITEM:
                cursor = queryNotesListItem(db, tableName, projection, selection,
                        selectionArgs, sortOrder);
                break;
            case NOTES_LIST_ITEM_ID:
                id = uri.getLastPathSegment();
                cursor = queryNotesListItem(db, tableName, projection, whereWithId(id, selection),
                        selectionArgs, sortOrder);
                break;
            default:
                cursor = null;
        }

        if (cursor != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        int match = findMatch(uri, "getType");
        switch (match) {
            case NOTE:
                return NOTE_MIMETYPE;
            case NOTE_ID:
                return NOTE_ID_MIMETYPE;
            case NOTES_LIST_ITEM:
                return NOTES_LIST_ITEM_MIMETYPE;
            case NOTES_LIST_ITEM_ID:
                return NOTES_LIST_ITEM_ID_MIMETYPE;
            default:
                return null;
        }
    }

    @Override
    public Uri pimInsert(Uri uri, ContentValues values) {
        int match = findMatch(uri, "insert");
        Uri resultUri = null;
        SQLiteDatabase db = getWritableDatabase();
        long longId = -1;
        switch (match) {
            case NOTE:
                longId = db.insert(Note.TABLE_NAME, NULL_COLUMN_HACK_VALUE, values);
                resultUri = ContentUris.withAppendedId(uri, longId);
               break;
            default:
                return null;
        }

        if (longId != -1) {
            notify(uri, null);
            notify(NoteListItemContract.CONTENT_URI, null);
        }

        return resultUri;
    }

    private int deleteFromDB(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = getWritableDatabase();

        int match = findMatch(uri, "delete");
        String id = "0";
        int result = 0;
        int table = match >> BASE_SHIFT;
        String tableName = TABLE_NAMES.valueAt(table);

        switch (match) {
            case NOTE_ID:
                id = uri.getPathSegments().get(1);
                result = db.delete(tableName, whereWithId(id, selection), selectionArgs);
                break;
            case NOTE:
                if (selection != null && selectionArgs != null) {
                    result = db.delete(tableName, selection, selectionArgs);
                }
                break;
            default:
                break;
        }

        if (result > 0) {
            notify(uri, null);
            notify(NoteListItemContract.CONTENT_URI, id);
        }

        return result;
    }

    @Override
    public int pimDelete(Uri uri, String selection, String[] selectionArgs) {
        int result = 0;

        if (isUriFromSyncAdapter(uri)) {
            result = deleteFromDB(uri, selection, selectionArgs);
        } else {
            // Update 'delete' flag
            ContentValues values = new ContentValues();
            values.put(NoteContract.NoteColumns.DELETED, 1);
            result = pimUpdate(uri, values, selection, selectionArgs);
        }

        return result;
    }

    @Override
    public int pimUpdate(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int match = findMatch(uri, "update");
        int result = 0;
        SQLiteDatabase db = getWritableDatabase();

        int table = match >> BASE_SHIFT;
        String tableName = TABLE_NAMES.valueAt(table);
        String id = "0";
        switch (match) {
            case NOTE_ID:
                markDirtyIfCalledFromSyncAdapter(uri, values);
                id = uri.getPathSegments().get(1);
                result = db.update(tableName, values, whereWithId(id, selection), selectionArgs);
                break;
            default:
                break;
        }

        if (result > 0) {
            notify(uri, null);
            notify(NoteListItemContract.CONTENT_URI, id);
        }

        return result;
    }

    private static boolean isUriFromSyncAdapter(Uri uri) {
        String callerIsSyncAdapter = uri.getQueryParameter(SyncContract.CALLER_IS_SYNCADAPTER);
        return (callerIsSyncAdapter != null && callerIsSyncAdapter.equals("true"));
    }

    private static void markDirtyIfCalledFromSyncAdapter(Uri uri, ContentValues values) {
        if (!isUriFromSyncAdapter(uri)) {
            values.put(NoteContract.NoteColumns.DIRTY, 1);
        }
    }

    protected void notify(Uri uri, String id) {
        final Uri notifyUri = (id != null) ? uri.buildUpon().appendPath(id).build() : uri;
        notifyChange(notifyUri, null);
    }

    protected static int findMatch(Uri uri, String methodName) {
        int match = URI_MATCHER.match(uri);
        if (match < 0) {
            throw new IllegalArgumentException("Unknown uri: " + uri);
        }
        return match;
    }

    /**
     * Gets the list item cursor.
     *
     * @param database The SQL database of the content provider
     * @param projection The list of columns to put into the cursor.
     * @param selection A selection criteria to apply when filtering rows.
     * @param selectionArgs The values to replace arguments on selection.
     * @param sortOrder How the rows in the cursor should be sorted.
     * @return a Cursor or null
     */
    private Cursor queryNotesListItem(SQLiteDatabase database,  String tableName,
            String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        String[] notesProjection = new String[projection.length];
        int i = 0;
        for (String p : projection) {
            notesProjection[i++] = NOTES_LIST_PROJECTION_MAP.get(p);
        }
        Cursor result = database.query(tableName, notesProjection, selection,
                selectionArgs, null, null, sortOrder);

        return result;
    }

    /*
     * Call a provider-defined method. This can be used to implement interfaces that are cheaper
     * and/or unnatural for a table-like model.
     *
     * TODO: Add relevant calls here
     */

    @Override
    protected Bundle pimCall(String method, String arg, Bundle extras) {
        return null;
    }
}
