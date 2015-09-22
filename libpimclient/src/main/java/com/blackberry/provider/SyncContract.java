package com.blackberry.provider;

import android.content.ContentUris;
import android.net.Uri;

public class SyncContract {

    /**
     * Constant for items that have not been saved.
     */
    public static final int NOT_SAVED = -1;

    /**
     * An optional URI parameter for insert, update, or delete queries that
     * allows the caller to specify that it is a sync adapter. The default value
     * is false. If true, the content provider will not mark the DIRTY flag to
     * true when modifying fields.
     */
    public static final String CALLER_IS_SYNCADAPTER = "caller_is_syncadapter";

    /**
     * Generates a content uri with sync parameters.
     *
     * @param uri Base content uri
     * @param fromSyncAdapter Indicates if the request if from a sync adapter
     * @return the generated content uri
     */
    public static Uri generateContentUri(Uri uri, boolean fromSyncAdapter) {
        return generateContentUri(uri, NOT_SAVED, fromSyncAdapter);
    }

    /**
     * Generates a content uri with object id and sync parameters.
     *
     * @param uri Base content uri
     * @param id Object id to integrate with the content uri
     * @param fromSyncAdapter Indicates if the request if from a sync adapter
     * @return the request if from a sync adapter
     */
    public static Uri generateContentUri(Uri uri, long id, boolean fromSyncAdapter) {
        // add object id
        if (id != SyncContract.NOT_SAVED) {
            uri = ContentUris.withAppendedId(uri, id);
        }

        // add sync adapter parameter
        if (fromSyncAdapter) {
            uri = uri.buildUpon()
                    .appendQueryParameter(SyncContract.CALLER_IS_SYNCADAPTER, "true")
                    .build();
        }

        return uri;
    }

    public interface SyncColumns {
        // server id (string) for the remote item
        public static final String SERVER_ID = "syncServerId";
        // source's timestamp (long) for this item
        public static final String SERVER_TIMESTAMP = "syncServerTimeStamp";
        // Used to indicate that local, unsynced, changes are present.
        public static final String DIRTY = "dirty";
        // Whether the row has been deleted but not synced to the server
        public static final String DELETED = "deleted";
    }

}
