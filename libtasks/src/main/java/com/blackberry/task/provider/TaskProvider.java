package com.blackberry.task.provider;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Bundle;
import android.util.SparseArray;

import com.blackberry.common.content.ProjectionMap;
import com.blackberry.pimbase.provider.PIMContentProviderBase;
import com.blackberry.provider.ListItemContract;
import com.blackberry.provider.SyncContract;
import com.blackberry.task.Task;
import com.blackberry.task.provider.TaskListItemContract.TasksListItemStates;
import com.blackberry.task.provider.TaskProviderDBHelper.DatabaseHelper;

/**
 * Contents provider for tasks.
 */
public class TaskProvider extends PIMContentProviderBase {


    public static final String TASK_MIMETYPE
    = "vnd.android.cursor.dir/vnd.blackberry.task";
    public static final String TASK_ID_MIMETYPE
    = "vnd.android.cursor.item/vnd.blackberry.task";
    public static final String TASK_ATTR_MIMETYPE
    = "vnd.android.cursor.dir/vnd.blackberry.task.attr";
    public static final String TASK_ATTR_ID_MIMETYPE
    = "vnd.android.cursor.item/vnd.blackberry.task.attr";
    public static final String TASKS_LIST_ITEM_MIMETYPE
    = "vnd.android.cursor.dir/vnd.blackberry.tasks.list";
    public static final String TASKS_LIST_ITEM_ID_MIMETYPE
    = "vnd.android.cursor.item/vnd.blackberry.tasks.list";

    protected static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    private static Uri INTEGRITY_CHECK_URI;
    private static final int TASK_BASE = 0;
    private static final int TASK = TASK_BASE;
    private static final int TASK_ID = TASK_BASE + 1;
    private static final int TASKS_LIST_ITEM = TASK_BASE + 2;
    private static final int TASKS_LIST_ITEM_ID = TASK_BASE + 3;

    private static final String DATABASE_NAME = "TaskProvider.db";
    private static final String NULL_COLUMN_HACK_VALUE = "foo";
    private TaskProviderDBHelper.DatabaseHelper mDbHelper;

    private static final int BASE_SHIFT = 12;  // 12 bits to the base type: 0, 0x1000, 0x2000, etc.
    private static final SparseArray<String> TABLE_NAMES;

    private static final ProjectionMap TASKS_LIST_PROJECTION_MAP;

    private static final String NOT_DELETED_SELECTION = TaskContract.TaskColumns.DELETED + " != 1";

    static {
        SparseArray<String> array = new SparseArray<String>(11);
        array.put(TASK_BASE >> BASE_SHIFT, Task.TABLE_NAME);
        TABLE_NAMES = array;

        // If the SQL statement for getting the state is getting more complicated,
        // consider using a CursorWrapper that does the mapping.
        String state = String.format(Locale.US,
                "(CASE WHEN IFNULL(%s, 0)!=0 THEN %d ELSE 0 END)"      // complete
                + " | (CASE WHEN IFNULL(%s, 0)!=0 THEN %d ELSE 0 END)" // reminder
                + " | (CASE WHEN IFNULL(%s, 1)=2 THEN %d ELSE 0 END)"  // high importance
                + " | (CASE WHEN IFNULL(%s, 1)=0 THEN %d ELSE 0 END)", // low importance
                TaskContract.TaskColumns.COMPLETE, TasksListItemStates.COMPLETE,
                TaskContract.TaskColumns.REMINDER_SET, TasksListItemStates.REMINDER_SET,
                TaskContract.TaskColumns.IMPORTANCE, TasksListItemStates.HIGH_IMPORTANCE,
                TaskContract.TaskColumns.IMPORTANCE, TasksListItemStates.LOW_IMPORTANCE);

        TASKS_LIST_PROJECTION_MAP = ProjectionMap.builder()
                .add(ListItemContract.ListItemColumns._ID,
                        TaskContract.TaskColumns._ID)
                .add(ListItemContract.ListItemColumns.ACCOUNT_ID,
                        TaskContract.TaskColumns.ACCOUNT_KEY)
                .add(ListItemContract.ListItemColumns.MIME_TYPE,
                        "'" + TASK_ID_MIMETYPE + "'")
                .add(ListItemContract.ListItemColumns.DUID,
                        TaskContract.TaskColumns._ID)
                .add(ListItemContract.ListItemColumns.URI,
                        "'" + TaskContract.CONTENT_URI + "/task/'||" + TaskContract.TaskColumns._ID)
                .add(ListItemContract.ListItemColumns.PRIMARY_TEXT,
                        TaskContract.TaskColumns.SUBJECT)
                .add(ListItemContract.ListItemColumns.SECONDARY_TEXT,
                        TaskContract.TaskColumns.BODY)
                .add(ListItemContract.ListItemColumns.TERTIARY_TEXT,
                        "''")
                .add(ListItemContract.ListItemColumns.TIMESTAMP,
                        TaskContract.TaskColumns.UTC_DUE_DATE)
                .add(ListItemContract.ListItemColumns.STATE, state)
                .add(ListItemContract.ListItemColumns.GROUP_ID,
                        "''")
                .build();
    }

    private static void init(Context context) {
        synchronized (URI_MATCHER) {
            if (INTEGRITY_CHECK_URI != null) {
                return;
            }

            INTEGRITY_CHECK_URI = Uri.parse("content://" + TaskContract.AUTHORITY
                    + "/integrityCheck");
            // All tasks
            URI_MATCHER.addURI(TaskContract.AUTHORITY, "task", TASK);
            // Specific task by id
            URI_MATCHER.addURI(TaskContract.AUTHORITY, "task/#", TASK_ID);
            // All task list items
            URI_MATCHER.addURI(TaskContract.AUTHORITY, ListItemContract.URI_SUFFIX,
                    TASKS_LIST_ITEM);
            // Specific task list item by id
            URI_MATCHER.addURI(TaskContract.AUTHORITY, ListItemContract.URI_SUFFIX
                    + "/#", TASKS_LIST_ITEM_ID);
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
        mDbHelper = new TaskProviderDBHelper.DatabaseHelper(getContext(), DATABASE_NAME);
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
            if (!Arrays.asList(projection).contains(TaskContract.TaskColumns.DELETED)) {
                int len = projection.length;
                projection = Arrays.copyOf(projection, len + 1);
                projection[len] = TaskContract.TaskColumns.DELETED;
            }

            selection = whereWith(NOT_DELETED_SELECTION, selection);
        }

        SQLiteDatabase db = getWritableDatabase();

        // using this table match idea for now, may change when
        // adding validation/sql injection protection
        int table = match >> BASE_SHIFT;
        String tableName = TABLE_NAMES.valueAt(table);
        String id;
        switch (match) {
            case TASK:
                cursor = db.query(tableName, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            case TASK_ID:
                id = uri.getPathSegments().get(1);
                cursor = db.query(tableName, projection, whereWithId(id, selection),
                        selectionArgs, null, null, sortOrder);
                break;
            case TASKS_LIST_ITEM:
                cursor = queryTasksListItem(db, tableName, projection, selection,
                        selectionArgs, sortOrder);
                break;
            case TASKS_LIST_ITEM_ID:
                id = uri.getLastPathSegment();
                cursor = queryTasksListItem(db, tableName, projection, whereWithId(id, selection),
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
            case TASK:
                return TASK_MIMETYPE;
            case TASK_ID:
                return TASK_ID_MIMETYPE;
            case TASKS_LIST_ITEM:
                return TASKS_LIST_ITEM_MIMETYPE;
            case TASKS_LIST_ITEM_ID:
                return TASKS_LIST_ITEM_ID_MIMETYPE;
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
            case TASK:
                updateAllLocalDates(uri, values);
                longId = db.insert(Task.TABLE_NAME, NULL_COLUMN_HACK_VALUE, values);
                resultUri = ContentUris.withAppendedId(uri, longId);
                break;
            default:
                return null;
        }

        if (longId != -1) {
            notify(uri, null);
            notify(TaskListItemContract.CONTENT_URI, null);
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
            case TASK_ID:
                id = uri.getPathSegments().get(1);
                result = db.delete(tableName, whereWithId(id, selection), selectionArgs);
                break;
            case TASK:
                if (selection != null && selectionArgs != null) {
                    result = db.delete(tableName, selection, selectionArgs);
                }
                break;
            default:
                break;
        }

        if (result > 0) {
            notify(uri, null);
            notify(TaskListItemContract.CONTENT_URI, id);
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
            values.put(TaskContract.TaskColumns.DELETED, 1);
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
            case TASK_ID:
                markDirtyIfCalledFromSyncAdapter(uri, values);
                updateAllLocalDates(uri, values);
                id = uri.getPathSegments().get(1);
                result = db.update(tableName, values, whereWithId(id, selection), selectionArgs);
                break;
            default:
                break;
        }

        if (result > 0) {
            notify(uri, null);
            notify(TaskListItemContract.CONTENT_URI, id);
        }

        return result;
    }

    private static boolean isUriFromSyncAdapter(Uri uri) {
        String callerIsSyncAdapter = uri.getQueryParameter(SyncContract.CALLER_IS_SYNCADAPTER);
        return (callerIsSyncAdapter != null && callerIsSyncAdapter.equals("true"));
    }

    private static void markDirtyIfCalledFromSyncAdapter(Uri uri, ContentValues values) {
        if (!isUriFromSyncAdapter(uri)) {
            values.put(TaskContract.TaskColumns.DIRTY, 1);
        }
    }

    private static void updateAllLocalDates(Uri uri, ContentValues values) {
        if (!isUriFromSyncAdapter(uri)) {
            updateLocalDate(values, TaskContract.TaskColumns.UTC_DUE_DATE,
                    TaskContract.TaskColumns.DUE_DATE);
            updateLocalDate(values, TaskContract.TaskColumns.UTC_START_DATE,
                    TaskContract.TaskColumns.START_DATE);
        }
    }

    /**
     * Updates the local date column based on UTC date column.
     * This function is used to update the values of DUE_DATE and START_DATE based on
     * the values of UTC_DUE_DATE and UTC_START_DATE.
     * The client, such as UI application, needs to update UTC_DUE_DATE and UTC_START_DATE.
     * Tasks CP will update DUE_DATE and START_DATE accordingly.
     *
     * @param values the content values.
     * @param utcDateColumn the column name for the UTC date.
     * @param localDateColumn the column name for the local date.
     */
    private static void updateLocalDate(ContentValues values, String utcDateColumn,
            String localDateColumn) {
        TimeZone timeZone = TimeZone.getDefault();
        Long utcDate = values.getAsLong(utcDateColumn);
        if (utcDate != null && utcDate.longValue() > 0) {
            long localDate = utcDate + timeZone.getRawOffset();
            values.put(localDateColumn, localDate);
        }
    }

    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentProviderResult[] results = super.applyBatch(operations);
            db.setTransactionSuccessful();
            return results;
        } finally {
            db.endTransaction();
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
    private Cursor queryTasksListItem(SQLiteDatabase database,  String tableName,
            String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        String[] tasksProjection = new String[projection.length];
        int i = 0;
        for (String p : projection) {
            tasksProjection[i++] = TASKS_LIST_PROJECTION_MAP.get(p);
        }
        Cursor result = database.query(tableName, tasksProjection, selection,
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
