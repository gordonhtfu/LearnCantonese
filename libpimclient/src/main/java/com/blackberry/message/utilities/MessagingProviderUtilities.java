
package com.blackberry.message.utilities;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.v4.util.LongSparseArray;

import com.blackberry.message.service.FolderValue;
import com.blackberry.message.service.MessageValue;
import com.blackberry.provider.MessageContract;
import com.blackberry.provider.MessageContract.Folder;

import java.util.ArrayList;
import java.util.List;

public class MessagingProviderUtilities {

    /**
     * This projection can be used with any of the EmailContent classes, when
     * all you need is a list of id's. Use ID_PROJECTION_COLUMN to access the
     * row data.
     */
    public static final String[] ID_PROJECTION = new String[] {
            BaseColumns._ID
    };

    public static final int ID_PROJECTION_COLUMN = 0;

    /********
     * MESSAGE HELPERS *********?
     */

    /**
     * Projection for a query to get all columns necessary for an actual change.
     */
    private interface ProjectionChangeQuery {
        public static final int COLUMN_ID = 0;
        public static final int COLUMN_SERVER_ID = 2;
        public static final int COLUMN_STATE = 3;

        public static final String[] PROJECTION = new String[] {
                MessageContract.Message._ID, MessageContract.Message.REMOTE_ID,
                MessageContract.Message.STATE,
                MessageContract.Message.FOLDER_ID
                // OLD_FLAG_READ, NEW_FLAG_READ,
                // OLD_FLAG_FAVORITE, NEW_FLAG_FAVORITE
        };
    }

    // 1 need a get Dirty/Changed Message call
    /** Selection string for querying this table. */
    private static final String SELECTION_BY_ACCOUNT_KEY_AND_DIRTY_NOT_DELETED =
            MessageContract.Message.ACCOUNT_ID + "=? and " + MessageContract.Message.DIRTY
                    + "=? and " +
                    MessageContract.Message.DELETED + " = 0";

    /**
     * Gets final state changes to upsync to the server, setting the status in
     * the DB for all rows to {@link #STATUS_PROCESSING} that are being updated
     * and to {@link #STATUS_FAILED} for any old updates. Messages whose
     * sequence of changes results in a no-op are cleared from the DB without
     * any upsync.
     * 
     * @param context A {@link Context}.
     * @param accountId The account we want to update.
     * @param ignoreFavorites Whether to ignore changes to the favorites flag.
     * @param mailboxId Mailbox Id used to filter changes
     * @return The final changes to send to the server, or null if there are
     *         none.
     */
    public static List<MessageValue> getChanges(final Context context, final long accountId,
            final boolean ignoreFavorites, final long mailboxId) {

        List<MessageValue> allChanges = getChanges(context, accountId, ignoreFavorites);
        if (allChanges == null) {
            return null;
        }

        LongSparseArray<List<MessageValue>> allChangesInMapForm = convertToChangesMap(allChanges);
        if (allChangesInMapForm == null) {
            return null;
        }

        return allChangesInMapForm.get(mailboxId);
    }

    /**
     * Rearrange the changes list to a map by mailbox id.
     * 
     * @return The final changes to send to the server, or null if there are
     *         none.
     */
    public static LongSparseArray<List<MessageValue>> convertToChangesMap(
            final List<MessageValue> changes) {
        if (changes == null) {
            return null;
        }

        final LongSparseArray<List<MessageValue>> changesMap = new LongSparseArray<List<MessageValue>>();
        for (final MessageValue change : changes) {
            List<MessageValue> list = changesMap.get(change.mFolderId);
            if (list == null) {
                list = new ArrayList<MessageValue>();
                changesMap.put(change.mFolderId, list);
            }
            list.add(change);
        }
        if (changesMap.size() == 0) {
            return null;
        }
        return changesMap;
    }

    /**
     * Gets final state changes to upsync to the server, setting the status in
     * the DB for all rows to {@link #STATUS_PROCESSING} that are being updated
     * and to {@link #STATUS_FAILED} for any old updates. Messages whose
     * sequence of changes results in a no-op are cleared from the DB without
     * any upsync.
     * 
     * @param context A {@link Context}.
     * @param accountId The account we want to update.
     * @param ignoreFavorites Whether to ignore changes to the favorites flag.
     * @return The final changes to send to the server, or null if there are
     *         none.
     */
    public static List<MessageValue> getChanges(final Context context, final long accountId,
            final boolean ignoreFavorites) {
        final ContentResolver cr = context.getContentResolver();

        final String[] args = {
                accountId + "", "1"
        };

        Cursor cursor = cr.query(MessageContract.Message.CONTENT_URI,
                ProjectionChangeQuery.PROJECTION, SELECTION_BY_ACCOUNT_KEY_AND_DIRTY_NOT_DELETED,
                args, MessageContract.Message._ID + " ASC");

        if (cursor == null) {
            return null;
        }

        // WILL NEED TO ADD SOME LOGIC TO SEE IF THE FINAL STATE IS DIFFERENT
        // FROM THE STARTING STATE
        // AS NOT TO UPDATE SOMETHING ON THE SERVICER THAT REALLY HAS NOT
        // CHANGED
        LongSparseArray<MessageValue> changesMap = getChangesWithCursor(ignoreFavorites, cr, cursor);

        // Prune no-ops.
        final int count = changesMap.size();
        final long[] unchangedMessages = new long[count];
        int unchangedMessagesCount = 0;
        final ArrayList<MessageValue> changes = new ArrayList<MessageValue>(count);
        for (int i = 0; i < changesMap.size(); ++i) {
            final MessageValue change = changesMap.valueAt(i);
            // We also treat changes without a server id as a no-op.
            if ((change.mRemoteId == null || change.mRemoteId.length() == 0)) {
                unchangedMessages[unchangedMessagesCount] = change.mId;
                ++unchangedMessagesCount;
            } else {
                changes.add(change);
            }
        }
        if (unchangedMessagesCount != 0) {
            // @TODO ADD BACK
            // deleteRowsForMessages(cr, CONTENT_URI, unchangedMessages,
            // unchangedMessagesCount);
        }
        if (changes.isEmpty()) {
            return null;
        }
        return changes;
    }

    private static LongSparseArray<MessageValue> getChangesWithCursor(
            final boolean ignoreFavorites, final ContentResolver cr, final Cursor c) {
        // Collapse rows acting on the same message.
        LongSparseArray<MessageValue> changesMap = new LongSparseArray<MessageValue>();
        try {
            while (c.moveToNext()) {
                MessageValue current = new MessageValue(c);
                changesMap.put(current.mId, current);
            }
        } finally {
            c.close();
        }
        return changesMap;
    }

    private static final String WHERE_TYPE_AND_ACCOUNT_KEY =
            Folder.TYPE + "=? and " + Folder.ACCOUNT_ID + "=?";

    /**
     * Convenience method to return the id of a given type of Folder for a given
     * Account; the common Folder types (Inbox, Outbox, Sent, Drafts, Trash,
     * etc)
     * 
     * @param context the caller's context, used to get a ContentResolver
     * @param accountId the id of the account to be queried
     * @param type the folder type, as defined above
     * @return the id of the folder, or -1 if not found
     */
    public static long findFolderOfType(Context context, long accountId, int type) {
        long folderId = -1;
        String[] selectionArgs = new String[] {
                Long.toString(type), Long.toString(accountId)
        };

        Cursor c = context.getContentResolver().query(Folder.CONTENT_URI,
                ID_PROJECTION, WHERE_TYPE_AND_ACCOUNT_KEY, selectionArgs,
                null);

        try {
            if (c != null && c.moveToFirst()) {
                folderId = c.getLong(0);
            }
        } finally
        {
            if (c != null) {
                c.close();
            }
        }

        return folderId;
    }

    /**
     * @param uriBase
     * @param id
     * @param asSAM
     * @return
     */
    public static Uri buildUri(Uri uriBase, long id, boolean callerIsSyncAdapter) {
        Uri uri =
                ContentUris
                        .appendId(uriBase.buildUpon(), id).
                        appendQueryParameter(MessageContract.CALLER_IS_SYNCADAPTER,
                                callerIsSyncAdapter ? "true" : "false")
                        .build();

        return uri;
    }

    public static Uri appendSamQueryParameter(Uri uri, boolean callerIsSyncAdapter) {
        return uri.buildUpon().appendQueryParameter(MessageContract.CALLER_IS_SYNCADAPTER,
                callerIsSyncAdapter ? "true" : "false").build();

    }
}
