/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors: Andrew Ewanchuk & Carl Cherry
 */

package com.blackberry.email.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.SparseBooleanArray;

import com.blackberry.common.Logging;
import com.blackberry.common.utils.LogUtils;
import com.blackberry.email.provider.contract.Account;
import com.blackberry.email.provider.contract.EmailContent.MailboxColumns;
import com.blackberry.email.provider.contract.ProviderUnavailableException;
import com.blackberry.lib.emailprovider.R;
import com.blackberry.message.service.FolderValue;
import com.blackberry.message.utilities.MessagingProviderUtilities;
import com.blackberry.note.provider.NoteContract;
import com.blackberry.provider.MessageContract;
import com.blackberry.provider.MessageContract.Folder;
import com.blackberry.task.provider.TaskContract;

/** Mailbox (EAS folder) utilities. */
public class FolderUtilities {

    /**
     * Sync extras key when syncing one or more mailboxes to specify how many
     * mailboxes are included in the extra.
     */
    public static final String SYNC_EXTRA_MAILBOX_COUNT = "__mailboxCount__";
    /**
     * Sync extras key pattern when syncing one or more mailboxes to specify
     * which mailbox to sync. Is intentionally private, we have helper functions
     * to set up an appropriate bundle, or read its contents.
     */
    private static final String SYNC_EXTRA_MAILBOX_ID_PATTERN = "__mailboxId%d__";
    /**
     * Sync extra key indicating that we are doing a sync of the folder
     * structure for an account.
     */
    public static final String SYNC_EXTRA_ACCOUNT_ONLY = "__account_only__";
    /**
     * Sync extra key indicating that we are only starting a ping.
     */
    public static final String SYNC_EXTRA_PUSH_ONLY = "__push_only__";

    /**
     * Sync extras key to specify that only a specific mailbox type should be
     * synced.
     */
    public static final String SYNC_EXTRA_MAILBOX_TYPE = "__mailboxType__";
    /**
     * Sync extras key when syncing a mailbox to specify how many additional
     * messages to sync.
     */
    public static final String SYNC_EXTRA_DELTA_MESSAGE_COUNT = "__deltaMessageCount__";
    /**
     * How many messages have been changed in this mailbox
     */
    public static final String SYNC_EXTRA_MESSAGE_UPDATE_COUNT = "__messageUpdateCount__";

    public static final String SYNC_EXTRA_NOOP = "__noop__";

    public static final Long NO_MAILBOX = Long.valueOf(-1);
    public static final int ID_PROJECTION_COLUMN = 0;
    public static final String[] ID_PROJECTION = new String[] {
            BaseColumns._ID
    };

    private static final String WHERE_TYPE_AND_ACCOUNT_KEY =
            MessageContract.Folder.TYPE + "=? and " + MessageContract.Folder.ACCOUNT_ID
                    + "=?";

    /**
     * Selection for mailboxes that should receive push for an account. A
     * mailbox should receive push if it has a valid, non-initial sync key and
     * is opted in for sync.
     */
    private static final String PUSH_MAILBOXES_FOR_ACCOUNT_SELECTION =
            MessageContract.Folder.SYNC_DATA3 + " is not null and "
                    + MessageContract.Folder.SYNC_DATA3 + "!='' and " +
                    MessageContract.Folder.SYNC_DATA3 + "!='0' and "
                    + MessageContract.Folder.SYNC_DATA1 +
                    "=1 and " + MessageContract.Folder.ACCOUNT_ID + "=?";

    /**
     * Selection for mailboxes that are configured for sync of a certain type
     * for an account.
     */
    private static final String SYNCING_AND_TYPE_FOR_ACCOUNT_SELECTION =
            MessageContract.Folder.SYNC_DATA1 + "=1 and " + MessageContract.Folder.TYPE + "=? and "
                    +
                    MessageContract.Folder.ACCOUNT_ID + "=?";

    /**
     * Selection for folders that say they want to sync, plus outbox, for an
     * account.
     */
    private static final String SYNC_ENABLED_FOLDERS_BY_ACCOUNT =
            Folder.SYNC_DATA1 + "=1 and " + Folder.ACCOUNT_ID + "=?";

    /** Selection by server pathname for a given account */
    public static final String PATH_AND_ACCOUNT_SELECTION =
            Folder.REMOTE_ID + "=? and " + Folder.ACCOUNT_ID + "=?";

    public static final String[] SYNC_FOLDER_CONTENT_PROJECTION = new String[] {
            Folder._ID,
            Folder.NAME,
            Folder.REMOTE_ID,
            Folder.PARENT_REMOTE_ID,
            Folder.ACCOUNT_ID,
            Folder.TYPE,
            Folder.SYNC_DATA1, // MailboxColumns.SYNC_INTERVAL,
            Folder.SYNC_DATA2, // MailboxColumns.SYNC_LOOKBACK,
            Folder.SYNC_DATA3, // MailboxColumns.SYNC_KEY,
            Folder.SYNC_DATA4, // MailboxColumns.SYNC_TIME,
            Folder.FLAGS,
            Folder.SYNC_STATE
            // MailboxColumns.FLAG_VISIBLE,
            // MailboxColumns.SYNC_STATUS,
            // MailboxColumns.PARENT_KEY,
            // MailboxColumns.LAST_TOUCHED_TIME,
            // MailboxColumns.UI_SYNC_STATUS,
            // MailboxColumns.UI_LAST_SYNC_RESULT,
            // MailboxColumns.TOTAL_COUNT,
            // MailboxColumns.HIERARCHICAL_NAME,
            // MailboxColumns.LAST_FULL_SYNC_TIME
            };

    // Custom Folder Types - should put in FolderExtendedTypes enum
    public static final int TYPE_NONE = -1;

    /** Starting point for custom email types */
    private static final int CUSTOM_EMAIL = 0x10;

    /** Generic mailbox that holds mail, sometimes user created */
    public static final int TYPE_MAIL = CUSTOM_EMAIL + 1;
    /** Search results */
    public static final int TYPE_SEARCH = CUSTOM_EMAIL + 2;
    /** All unread mail (virtual) */
    public static final int TYPE_UNREAD = CUSTOM_EMAIL + 3;

    /**
     * A generic type to indicate all Mail folders NOTE: this type is just used
     * internally and NOT saved to the database as a mailbox type. It is simply
     * used for internally processing to indicate that ALL mail folders should
     * be included in the action. (All mail folders types are below this number)
     */
    public static final int TYPE_ALL_MAIL = 0x20;

    public static final int TYPE_NOT_EMAIL = 0x40;
    public static final int TYPE_CALENDAR = 0x41;
    public static final int TYPE_CONTACTS = 0x42;
    public static final int TYPE_TASKS = 0x43;
    public static final int TYPE_UNKNOWN = 0x45;
    public static final int TYPE_NOTES = 0x47;
    public static final int TYPE_JUNK  = 0x48;
    public static final int TYPE_USER_NOTES = 0x49;

    /**
     * For each of the following folder types, we expect there to be exactly one
     * folder of that type per account. Each sync adapter must do the following:
     * 1) On initial sync: For each type that was not found from the server,
     * create a local folder. 2) On folder delete: If it's of a required type,
     * convert it to local rather than delete. 3) On folder add: If it's of a
     * required type, convert the local folder to server. 4) When adding a
     * duplicate (either initial sync or folder add): Error.
     */
    public static final int[] REQUIRED_FOLDER_TYPES = {
            MessageContract.Folder.Type.INBOX, MessageContract.Folder.Type.DRAFT,
            MessageContract.Folder.Type.OUTBOX, MessageContract.Folder.Type.SENT,
            MessageContract.Folder.Type.TRASH
    };

    // Bit field flags; each is defined below
    // Warning: Do not read these flags until POP/IMAP/EAS all populate them
    /** No flags set */
    public static final int FLAG_NONE = 0;
    /** Has children in the mailbox hierarchy */
    public static final int FLAG_HAS_CHILDREN = 1 << 0;
    /** Children are visible in the UI */
    public static final int FLAG_CHILDREN_VISIBLE = 1 << 1;
    /** cannot receive "pushed" mail */
    public static final int FLAG_CANT_PUSH = 1 << 2;
    /**
     * can hold emails (i.e. some parent mailboxes cannot themselves contain
     * mail)
     */
    public static final int FLAG_HOLDS_MAIL = 1 << 3;
    /** can be used as a target for moving messages within the account */
    public static final int FLAG_ACCEPTS_MOVED_MAIL = 1 << 4;
    /** can be used as a target for appending messages */
    public static final int FLAG_ACCEPTS_APPENDED_MAIL = 1 << 5;
    /** has user settings (sync lookback, etc.) */
    public static final int FLAG_SUPPORTS_SETTINGS = 1 << 6;
    /**
     * this is a temporary mailbox and it was created solely for remote search
     * operation result called by API
     */
    public static final int FLAG_API_REMOTE_SEARCH = 1 << 7;

    public static final long PARENT_KEY_UNINITIALIZED = 0L;

    public static final String TYPE_STRING_PERSONAL = "Personal";
    public static final String TYPE_STRING_INBOX = "Inbox";
    public static final String TYPE_STRING_OUTBOX = "Outbox";
    public static final String TYPE_STRING_DRAFTS = "Drafts";
    public static final String TYPE_STRING_TRASH = "Trash";
    public static final String TYPE_STRING_SENT = "Sent";
    public static final String TYPE_STRING_JUNK = "Junk";
    public static final String TYPE_STRING_SPAM = "Spam";
    public static final String TYPE_STRING_STARRED = "Starred";
    public static final String TYPE_STRING_EMAIL = "Email";
    public static final String TYPE_STRING_PARENT = "Parent";
    public static final String TYPE_STRING_SEARCH = "Search";
    public static final String TYPE_STRING_UNREAD = "Unread";
    public static final String TYPE_STRING_OTHER = "Other";
    public static final String TYPE_STRING_NOT_MAIL = "NotMail";
    public static final String TYPE_STRING_CALENDAR = "Calendar";
    public static final String TYPE_STRING_CONTACTS = "Contacts";
    public static final String TYPE_STRING_TASKS = "Tasks";
    public static final String TYPE_STRING_NOTES = "Notes";
    public static final String TYPE_STRING_USER_NOTES = "UserNotes";
    public static final String TYPE_STRING_UNKNOWN = "Unknown";

    /**
     * Specifies which mailbox types may be synced from server, and what the
     * default sync interval value should be. If a mailbox type is in this
     * array, then it can be synced. If the mailbox type is mapped to true in
     * this array, then new mailboxes of that type should be set to
     * automatically sync (either with the periodic poll, or with push, as
     * determined by the account's sync settings). See {@link #isSyncableType}
     * and {@link #getDefaultSyncStateForType} for how to access this data.
     */
    private static final SparseBooleanArray SYNCABLE_TYPES;
    static {
        SYNCABLE_TYPES = new SparseBooleanArray(7);
        SYNCABLE_TYPES.put(MessageContract.Folder.Type.INBOX, true);
        SYNCABLE_TYPES.put(TYPE_MAIL, false);
        SYNCABLE_TYPES.put(MessageContract.Folder.Type.DRAFT, false);
        SYNCABLE_TYPES.put(MessageContract.Folder.Type.SENT, true);
        SYNCABLE_TYPES.put(MessageContract.Folder.Type.TRASH, false);
        SYNCABLE_TYPES.put(TYPE_CALENDAR, true);
        SYNCABLE_TYPES.put(TYPE_CONTACTS, true);
        SYNCABLE_TYPES.put(TYPE_TASKS, true);
        SYNCABLE_TYPES.put(TYPE_NOTES, true);
        SYNCABLE_TYPES.put(TYPE_USER_NOTES, true);
    }

    /**
     * Check if a folder type should sync with the server by default.
     * 
     * @param mailboxType The type to check.
     * @return Whether this type should default to syncing.
     */
    public static boolean getDefaultSyncStateForType(final int folderType) {
        return SYNCABLE_TYPES.get(folderType);
    }

    /**
     * Check if a folder type can be synced with the server.
     * 
     * @param mailboxType The type to check.
     * @return Whether this type is syncable.
     */
    public static boolean isSyncableType(final int folderType) {
        return SYNCABLE_TYPES.indexOfKey(folderType) >= 0;
    }

    /**
     * Check whether this folder is syncable. It has to be both a server synced
     * mailbox, and of a syncable able.
     * 
     * @return Whether this folder is syncable.
     */
    public static boolean isSyncable(FolderValue folder) {
        return (isSyncableType(folder.mType) && folder.mRemoteId != null && folder.mRemoteId
                .length() > 0);
    }

    /**
     * Gets the correct authority for a folder.
     * 
     * @param folderType The type of the folder we're interested in.
     * @return The authority for the folder we're interested in.
     */
    public static String getAuthority(final int folderType) {
        switch (folderType) {
            case TYPE_CALENDAR:
                return CalendarContract.AUTHORITY;
            case TYPE_CONTACTS:
                return ContactsContract.AUTHORITY;
            case TYPE_TASKS:
                return TaskContract.AUTHORITY;
            case TYPE_NOTES:
            case TYPE_USER_NOTES:
                return NoteContract.AUTHORITY;
            default:
                if (isMailFolder(folderType)) {
                    return MessageContract.AUTHORITY;
                } else {
                    return "";
                }
        }
    }

    /**
     * Gets the folders to sync.
     * 
     * @param context the context
     * @param accountId the account id
     * @param folderType the folder type
     * @return the folders to sync
     */
    public ArrayList<FolderValue> getFoldersToSync(Context context,
            long accountId, int mailboxType) {
        ArrayList<FolderValue> syncableFolders = new ArrayList<FolderValue>();
        ArrayList<FolderValue> tempFolders = null;

        tempFolders = loadFoldersFromCursor(context, accountId, mailboxType, true);

        // Now we have our list - before we actually sync, let's make sure that
        // the
        // remote Calendar folder is created
        // TODO - should this be done when the Calendar folder
        // is created in the EmailProvider code???
        for (FolderValue folder : tempFolders) {
            syncableFolders.add(folder);
        }
        return syncableFolders;
    }

    /**
     * Load folders from cursor.
     * 
     * @param context the context
     * @param accountId the account id
     * @param folderType the folder type
     * @param initialSync the initial sync
     * @return the array list
     */
    private ArrayList<FolderValue> loadFoldersFromCursor(
            Context context,
            long accountId, int mailboxType, boolean initialSync) {
        ArrayList<FolderValue> syncableFolders = new ArrayList<FolderValue>();
        // Get the folders that need push notifications.
        final Cursor c;
        if (initialSync) {
            c = getFoldersForSync(context.getContentResolver(),
                    accountId);
        } else {
            if (mailboxType == TYPE_NONE) {
                c = getFoldersForPush(context.getContentResolver(), accountId);
            } else {
                c = getFoldersForSyncByType(context.getContentResolver(),
                        accountId, mailboxType);
            }
        }
        if (c != null) {
            try {
                while (c.moveToNext()) {
                    final FolderValue folder = new FolderValue(c);
                    if (isSyncableType(folder.mType)) {
                        syncableFolders.add(folder);
                    }
                }
            } finally {
                c.close();
            }
        }
        return syncableFolders;
    }

    /**
     * Gets the user enabled folder types.
     * 
     * @param account the account
     * @return the user enabled folder types
     */
    public HashSet<String> getUserEnabledFolderTypes(final android.accounts.Account account) {
        final HashSet<String> authsToSync = new HashSet<String>();
        if (ContentResolver.getSyncAutomatically(account, MessageContract.AUTHORITY)) {
            authsToSync.add(MessageContract.AUTHORITY);
        }
        if (ContentResolver.getSyncAutomatically(account, CalendarContract.AUTHORITY)) {
            authsToSync.add(CalendarContract.AUTHORITY);
        }
        if (ContentResolver.getSyncAutomatically(account, ContactsContract.AUTHORITY)) {
            authsToSync.add(ContactsContract.AUTHORITY);
        }
        if (ContentResolver.getSyncAutomatically(account, TaskContract.AUTHORITY)) {
            authsToSync.add(TaskContract.AUTHORITY);
        }
        if (ContentResolver.getSyncAutomatically(account, NoteContract.AUTHORITY)) {
            authsToSync.add(NoteContract.AUTHORITY);
        }
        return authsToSync;
    }

    /**
     * Convenience method to return the id of a given type of Folder for a given
     * Account; the common Folder types (Inbox, Outbox, Sent, Drafts, Trash, and
     * Search)
     * 
     * @param context the caller's context, used to get a ContentResolver
     * @param accountId the id of the account to be queried
     * @param type the folder type, as defined above
     * @return the id of the folder, or -1 if not found
     */
    public static Long findFolderOfType(Context context, long accountId, int type) {
        String[] bindArguments = new String[] {
                Long.toString(type), Long.toString(accountId)
        };
        return Utility.getFirstRowLong(context, MessageContract.Folder.CONTENT_URI,
                ID_PROJECTION, WHERE_TYPE_AND_ACCOUNT_KEY, bindArguments, null,
                ID_PROJECTION_COLUMN, NO_MAILBOX);
    }

    /**
     * Convenience method that returns the folder found using the method above
     */
    public static FolderValue restoreFolderOfType(Context context, long accountId, int type) {
        Long folderId = findFolderOfType(context, accountId, type);
        if (folderId != NO_MAILBOX) {

            Uri u = ContentUris.withAppendedId(MessageContract.Folder.CONTENT_URI, folderId);
            Cursor c = context.getContentResolver().query(u,
                    MessageContract.Folder.DEFAULT_PROJECTION, null, null, null);
            if (c == null)
                throw new ProviderUnavailableException();
            try {
                if (c.moveToFirst()) {
                    FolderValue folder = new FolderValue(c);
                    return folder;
                } else {
                    return null;
                }
            } finally {
                c.close();
            }

        }
        return null;
    }

    /**
     * Get the folders that should receive push updates for an account.
     * 
     * @param cr The {@link ContentResolver}.
     * @param accountId The id for the account that is pushing.
     * @return A cursor (suitable for use with {@link #restore}) with all
     *         folders we should sync.
     */
    public static Cursor getFoldersForPush(final ContentResolver cr, final long accountId) {
        return cr.query(MessageContract.Folder.CONTENT_URI, SYNC_FOLDER_CONTENT_PROJECTION,
                PUSH_MAILBOXES_FOR_ACCOUNT_SELECTION, new String[] {
                    Long.toString(accountId)
                },
                null);
    }

    /**
     * Get the folder content for an account that are configured for sync and
     * have a specific type.
     * 
     * @param accountId The id for the account that is syncing.
     * @param folderType The type of the folder we're interested in.
     * @return A cursor (with one column, containing ids) with all folder ids
     *         that match.
     */
    public static Cursor getFoldersForSyncByType(final ContentResolver cr, final long accountId,
            final int folderType) {
        return cr.query(MessageContract.Folder.CONTENT_URI, SYNC_FOLDER_CONTENT_PROJECTION,
                SYNCING_AND_TYPE_FOR_ACCOUNT_SELECTION,
                new String[] {
                        Integer.toString(folderType), Long.toString(accountId)
                }, null);
    }

    public static String getSystemFolderName(Context context, int folderType) {
        int resId = -1;
        switch (folderType) {
            case MessageContract.Folder.Type.INBOX:
                resId = R.string.mailbox_name_server_inbox;
                break;
            case MessageContract.Folder.Type.OUTBOX:
                resId = R.string.mailbox_name_server_outbox;
                break;
            case MessageContract.Folder.Type.DRAFT:
                resId = R.string.mailbox_name_server_drafts;
                break;
            case MessageContract.Folder.Type.TRASH:
                resId = R.string.mailbox_name_server_trash;
                break;
            case MessageContract.Folder.Type.SENT:
                resId = R.string.mailbox_name_server_sent;
                break;
            case TYPE_JUNK:
                resId = R.string.mailbox_name_server_junk;
                break;
            case MessageContract.Folder.Type.STARRED:
                resId = R.string.mailbox_name_server_starred;
                break;
            case TYPE_UNREAD:
                 resId = R.string.mailbox_name_server_all_unread;
                 break;
            default:
                throw new IllegalArgumentException("Illegal folder type");
        }
        return context.getString(resId);
    }

    /**
     * Gets the type string for logging
     * Should NOT be shown to the user as it's not locale specific.
     *
     * @param type the folder type
     * @return the folder type name
     */
    public static String getTypeString(int type) {
        String typeString;
        switch(type) {
        case Folder.Type.PERSONAL:
            typeString = TYPE_STRING_PERSONAL;
            break;
        case Folder.Type.INBOX:
            typeString = TYPE_STRING_INBOX;
            break;
        case Folder.Type.OUTBOX:
            typeString = TYPE_STRING_OUTBOX;
            break;
        case Folder.Type.DRAFT:
            typeString = TYPE_STRING_DRAFTS;
            break;
        case Folder.Type.TRASH:
            typeString = TYPE_STRING_TRASH;
            break;
        case Folder.Type.SENT:
            typeString = TYPE_STRING_SENT;
            break;
        case TYPE_JUNK:
            typeString = TYPE_STRING_JUNK;
            break;
        case Folder.Type.SPAM:
            typeString = TYPE_STRING_SPAM;
            break;
        case Folder.Type.STARRED:
            typeString = TYPE_STRING_STARRED;
            break;
        case TYPE_MAIL:
            typeString = TYPE_STRING_EMAIL;
            break;
        case Folder.Type.TYPE_PARENT:
            typeString = TYPE_STRING_PARENT;
            break;
        case TYPE_SEARCH:
            typeString = TYPE_STRING_SEARCH;
            break;
        case TYPE_UNREAD:
            typeString = TYPE_STRING_UNREAD;
            break;
        case Folder.Type.OTHER_PROVIDER_FOLDER:
            typeString = TYPE_STRING_OTHER;
            break;
        case TYPE_NOT_EMAIL:
            typeString = TYPE_STRING_NOT_MAIL;
            break;
        case TYPE_CALENDAR:
            typeString = TYPE_STRING_CALENDAR;
            break;
        case TYPE_CONTACTS:
            typeString = TYPE_STRING_CONTACTS;
            break;
        case TYPE_TASKS:
            typeString = TYPE_STRING_TASKS;
            break;
        case TYPE_NOTES:
            typeString = TYPE_STRING_NOTES;
            break;
        case TYPE_USER_NOTES:
            typeString = TYPE_STRING_USER_NOTES;
            break;
        default:
            typeString = TYPE_STRING_UNKNOWN + String.valueOf(type);
            break;
        }
        return typeString;
    }

    /**
     * To log string.
     *
     * @param folder the folder
     * @return a string safe enough to log
     */
    public static String toLogString(FolderValue folder) {
        if (folder.mRemoteId != null) {
            return String.format(Locale.US, "[%s %d,%s]",
                    getTypeString(folder.mType), folder.mId, folder.mRemoteId);
        } else {
            return String.format(Locale.US, "[%s %d]",
                    getTypeString(folder.mType), folder.mId);
        }
    }

    private static String formatFolderIdExtra(final int index) {
        return String.format(Locale.US, SYNC_EXTRA_MAILBOX_ID_PATTERN, index);
    }

    public static Bundle createSyncBundle(final ArrayList<Long> mailboxIds) {
        Bundle bundle = new Bundle();
        bundle.putInt(SYNC_EXTRA_MAILBOX_COUNT, mailboxIds.size());
        for (int i = 0; i < mailboxIds.size(); i++) {
            bundle.putLong(formatFolderIdExtra(i), mailboxIds.get(i));
        }
        return bundle;
    }

    public static Bundle createSyncBundle(final Long[] mailboxIds) {
        Bundle bundle = new Bundle();
        bundle.putInt(SYNC_EXTRA_MAILBOX_COUNT, mailboxIds.length);
        for (int i = 0; i < mailboxIds.length; i++) {
            bundle.putLong(formatFolderIdExtra(i), mailboxIds[i]);
        }
        return bundle;
    }

    public static Bundle createSyncBundle(final Long mailboxId) {
        Bundle bundle = new Bundle();
        bundle.putInt(SYNC_EXTRA_MAILBOX_COUNT, 1);
        bundle.putLong(formatFolderIdExtra(0), mailboxId);
        return bundle;
    }

    public static Bundle createSyncBundle(final Long mailboxId, int numChanges) {
        Bundle bundle = new Bundle();
        bundle.putInt(SYNC_EXTRA_MAILBOX_COUNT, 1);
        bundle.putInt(SYNC_EXTRA_MESSAGE_UPDATE_COUNT, numChanges);
        bundle.putLong(formatFolderIdExtra(0), mailboxId);
        return bundle;
    }

    public static Long[] getFolderIdsFromBundle(Bundle bundle) {
        final int count = bundle.getInt(SYNC_EXTRA_MAILBOX_COUNT, 0);
        if (count > 0) {
            if (bundle.getBoolean(SYNC_EXTRA_PUSH_ONLY, false)) {
                LogUtils.w(Logging.LOG_TAG, "Mailboxes specified in a push only sync");
            }
            if (bundle.getBoolean(SYNC_EXTRA_ACCOUNT_ONLY, false)) {
                LogUtils.w(Logging.LOG_TAG, "Mailboxes specified in an account only sync");
            }
            Long[] result = new Long[count];
            for (int i = 0; i < count; i++) {
                result[i] = bundle.getLong(formatFolderIdExtra(i), 0);
            }

            return result;
        } else {
            return null;
        }
    }

    /**
     * isMailFolder
     *
     * @param folderType - type of folder to check
     * @return true if Folder is a Mail folder
     */
    public static boolean isMailFolder(int folderType) {
        if (folderType > TYPE_NONE && folderType < TYPE_ALL_MAIL) {
            return true;
        }
        return false;
    }

    /**
     * Builds a new mailbox with "typical" settings for a system mailbox, such
     * as a local "Drafts" mailbox. This is useful for protocols like POP3 or
     * IMAP who don't have certain local system mailboxes synced with the
     * server. Note: the mailbox is not persisted - clients must call
     * {@link #save} themselves.
     */
    public static FolderValue newSystemFolder(Context context, long accountId, int folderType) {
        // Sync interval and flags are different based on mailbox type.
        final String syncInterval;
        final int flags;
        switch (folderType) {
            case Folder.Type.INBOX:
                flags = FLAG_HOLDS_MAIL | FLAG_ACCEPTS_MOVED_MAIL;
                syncInterval = "0";
                break;
            case Folder.Type.SENT:
            case Folder.Type.TRASH:
            case Folder.Type.DRAFT:
                flags = FLAG_HOLDS_MAIL;
                syncInterval = "0";
                break;
            case Folder.Type.OUTBOX:
                flags = FLAG_HOLDS_MAIL;
                syncInterval = Account.CHECK_INTERVAL_NEVER + "";
                break;
            default:
                throw new IllegalArgumentException("Bad mailbox type for newSystemMailbox: " +
                        folderType);
        }

        FolderValue folder = new FolderValue();
        folder.mAccountId = accountId;
        folder.mType = folderType;
        folder.mSyncData1 = syncInterval;
        // TODO: Fix how display names work.
        folder.mRemoteId = folder.mDisplayName = getSystemFolderName(context, folderType);
        folder.mParentId = Long.valueOf(-1);
        folder.mFlags = flags;
        return folder;
    }

    public static final String FOLDER_SELECTION = MessageContract.Folder._ID + "=?";

    public static void resyncFolder(
            final ContentResolver cr,
            final android.accounts.Account account,
            final Long folderId) {
        final Cursor cursor = cr.query(Folder.CONTENT_URI,
                new String[] {
                        MessageContract.Folder.TYPE,
                        MessageContract.Folder.REMOTE_ID,
                },
                MessageContract.Folder._ID + "=?",
                new String[] {
                    String.valueOf(folderId)
                },
                null);
        if (cursor == null || cursor.getCount() == 0) {
            LogUtils.w(Logging.LOG_TAG, "Folder %d not found", folderId);
            return;
        }
        try {
            cursor.moveToFirst();
            final int type = cursor.getInt(0);
            if (type >= TYPE_NOT_EMAIL) {
                throw new IllegalArgumentException(
                        String.format("Folder %d is not an Email folder", folderId));
            }
            final String serverId = cursor.getString(1);
            if (TextUtils.isEmpty(serverId)) {
                throw new IllegalArgumentException(
                        String.format("Folder %d has no remote id", folderId));
            }
            final ArrayList<ContentProviderOperation> ops =
                    new ArrayList<ContentProviderOperation>();
            ops.add(ContentProviderOperation.newDelete(MessageContract.Message.CONTENT_URI.buildUpon()
                    .appendQueryParameter(MessageContract.CALLER_IS_SYNCADAPTER,"true").build())
                    .withSelection(FOLDER_SELECTION,
                            new String[] {
                                String.valueOf(folderId)
                            })
                    .build());
            
           
            ops.add(ContentProviderOperation.newUpdate(
                    MessagingProviderUtilities.buildUri(MessageContract.Folder.CONTENT_URI, folderId, true))
                    .withValue(MessageContract.Folder.SYNC_DATA3, "0").build());// synckey
                                                                                // maps
                                                                                // to
                                                                                // SYNC_DATA3

            cr.applyBatch(MessageContract.AUTHORITY, ops);
            final Bundle extras = createSyncBundle(folderId);
            extras.putBoolean(ContentResolver.SYNC_EXTRAS_IGNORE_SETTINGS, true);
            ContentResolver.requestSync(account, MessageContract.AUTHORITY, extras);
            LogUtils.i(Logging.LOG_TAG, "requestSync resyncFolder %s, %s",
                    account.toString(), extras.toString());
        } catch (RemoteException e) {
            LogUtils.w(Logging.LOG_TAG, e, "Failed to wipe folder %d", folderId);
        } catch (OperationApplicationException e) {
            LogUtils.w(Logging.LOG_TAG, e, "Failed to wipe folder %d", folderId);
        } finally {
            cursor.close();
        }
    }

    /**
     * Get the mailbox content for an account that should sync when we do a full
     * account sync.
     * 
     * @param cr The {@link ContentResolver}.
     * @param accountId The id for the account that is pushing.
     * @return A cursor (with one column, containing ids) with all mailbox ids
     *         we should sync.
     */
    public static Cursor getFoldersForSync(final ContentResolver cr, final long accountId) {
        // We're sorting by mailbox type. The reason is that the inbox is type
        // 0, other types
        // (e.g. Calendar and Contacts) are all higher numbers. Upon initial
        // sync, we'd like to
        // sync the inbox first to improve perceived performance.
        return cr.query(Folder.CONTENT_URI, SYNC_FOLDER_CONTENT_PROJECTION,
                SYNC_ENABLED_FOLDERS_BY_ACCOUNT,
                new String[] {
                    Long.toString(accountId)
                }, Folder.TYPE + " ASC");
    }

    /**
     * Returns a Mailbox from the database, given its pathname and account id.
     * All mailbox paths for a particular account must be unique. Paths are
     * stored in the column {@link MailboxColumns#SERVER_ID} for want of yet
     * another column in the table.
     * 
     * @param context
     * @param accountId the ID of the account
     * @param path the fully qualified, remote pathname
     */
    public static FolderValue restoreFolderForPath(Context context,
            long accountId, String path) {
        Cursor c = context.getContentResolver().query(
                Folder.CONTENT_URI,
                SYNC_FOLDER_CONTENT_PROJECTION,
                PATH_AND_ACCOUNT_SELECTION,
                new String[] {
                        path, Long.toString(accountId)
                },
                null);
        try {
            FolderValue folder = null;
            if (c != null && c.moveToFirst()) {
                folder = new FolderValue(c);
            }
            return folder;
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    /**
     * @param context
     * @param accountId
     * @param path
     * @return
     */
    public static FolderValue getFolderForPath(Context context, long accountId, String path) {
        FolderValue folder = restoreFolderForPath(context, accountId, path);
        if (folder == null) {
            folder = new FolderValue();
        }
        return folder;
    }
}
