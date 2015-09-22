/*
 * Copyright (C) 2012 The Android Open Source Project
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
 */

package com.blackberry.email.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.blackberry.common.utils.LogUtils;
import com.blackberry.email.provider.contract.Account;
import com.blackberry.email.provider.contract.EmailContent;
import com.blackberry.email.provider.contract.EmailContent.AccountColumns;
import com.blackberry.email.provider.contract.EmailContent.Attachment;
import com.blackberry.email.provider.contract.EmailContent.AttachmentColumns;
import com.blackberry.email.provider.contract.EmailContent.Body;
import com.blackberry.email.provider.contract.EmailContent.BodyColumns;
import com.blackberry.email.provider.contract.EmailContent.HostAuthColumns;
import com.blackberry.email.provider.contract.EmailContent.MailboxColumns;
import com.blackberry.email.provider.contract.EmailContent.Message;
import com.blackberry.email.provider.contract.EmailContent.MessageColumns;
import com.blackberry.email.provider.contract.EmailContent.PolicyColumns;
import com.blackberry.email.provider.contract.EmailContent.QuickResponseColumns;
import com.blackberry.email.provider.contract.EmailContent.SyncColumns;
import com.blackberry.email.provider.contract.EmailContent.VirtualMailboxColumns;
import com.blackberry.email.provider.contract.HostAuth;
import com.blackberry.email.provider.contract.Mailbox;
import com.blackberry.email.provider.contract.MessageChangeLogTable;
import com.blackberry.email.provider.contract.MessageMove;
import com.blackberry.email.provider.contract.MessageStateChange;
import com.blackberry.email.provider.contract.Policy;
import com.blackberry.email.provider.contract.QuickResponse;
import com.blackberry.email.provider.contract.VirtualMailbox;
import com.blackberry.email.service.LegacyPolicySet;
import com.blackberry.lib.emailprovider.R;
import com.google.common.annotations.VisibleForTesting;

public final class DBHelper {
    private static final String TAG = "EmailProvider";

    // Any changes to the database format *must* include update-in-place code.
    // Version 1: Took version 124 from android source
    public static final int DATABASE_VERSION = 1;

    // Any changes to the database format *must* include update-in-place code.
    // Version 1: took version 8 from Android source
    public static final int BODY_DATABASE_VERSION = 1;

    private static final String TRIGGER_MAILBOX_DELETE =
        "create trigger mailbox_delete before delete on " + Mailbox.TABLE_NAME +
        " begin" +
        " delete from " + Message.TABLE_NAME +
        "  where " + MessageColumns.MAILBOX_KEY + "=old." + EmailContent.RECORD_ID +
        "; delete from " + Message.UPDATED_TABLE_NAME +
        "  where " + MessageColumns.MAILBOX_KEY + "=old." + EmailContent.RECORD_ID +
        "; delete from " + Message.DELETED_TABLE_NAME +
        "  where " + MessageColumns.MAILBOX_KEY + "=old." + EmailContent.RECORD_ID +
        "; end";

    private static final String TRIGGER_ACCOUNT_DELETE =
        "create trigger account_delete before delete on " + Account.TABLE_NAME +
        " begin delete from " + Mailbox.TABLE_NAME +
        " where " + MailboxColumns.ACCOUNT_KEY + "=old." + EmailContent.RECORD_ID +
        "; delete from " + HostAuth.TABLE_NAME +
        " where " + EmailContent.RECORD_ID + "=old." + AccountColumns.HOST_AUTH_KEY_RECV +
        "; delete from " + HostAuth.TABLE_NAME +
        " where " + EmailContent.RECORD_ID + "=old." + AccountColumns.HOST_AUTH_KEY_SEND +
        "; delete from " + Policy.TABLE_NAME +
        " where " + EmailContent.RECORD_ID + "=old." + AccountColumns.POLICY_KEY +
        "; end";

    /*
     * Internal helper method for index creation.
     * Example:
     * "create index message_" + MessageColumns.FLAG_READ
     * + " on " + Message.TABLE_NAME + " (" + MessageColumns.FLAG_READ + ");"
     */
    /* package */
    static String createIndex(String tableName, String columnName) {
        return "create index " + tableName.toLowerCase() + '_' + columnName
            + " on " + tableName + " (" + columnName + ");";
    }

    static void createVirtualMailboxTriggers(final SQLiteDatabase db) {
        // Insert an account
        db.execSQL("create trigger virtual_mailbox_insert after insert on " + Account.TABLE_NAME +
                " begin insert into " + VirtualMailbox.TABLE_NAME +
                " (" + VirtualMailboxColumns.ACCOUNT_KEY + ", " + VirtualMailboxColumns.TYPE + ")" +
                " values (" + "new." + EmailContent.RECORD_ID + ", " + Mailbox.TYPE_STARRED + ")" +
                "; insert into " + VirtualMailbox.TABLE_NAME +
                " (" + VirtualMailboxColumns.ACCOUNT_KEY + ", " + VirtualMailboxColumns.TYPE + ")" +
                " values (" + "new." + EmailContent.RECORD_ID + ", " + Mailbox.TYPE_UNREAD + ")" +
                "; end");

        // Delete an account
        db.execSQL("create trigger virtual_mailbox_delete before delete on " + Account.TABLE_NAME +
                " begin delete from " + VirtualMailbox.TABLE_NAME +
                " where " + VirtualMailboxColumns.ACCOUNT_KEY + "=old." + EmailContent.RECORD_ID +
                "; end");
    }

    static void createMessageCountTriggers(final SQLiteDatabase db) {
        // Insert a message.
        db.execSQL("create trigger message_count_message_insert after insert on " +
                Message.TABLE_NAME +
                " begin update " + Mailbox.TABLE_NAME + " set " + MailboxColumns.MESSAGE_COUNT +
                '=' + MailboxColumns.MESSAGE_COUNT + "+1" +
                "  where " + EmailContent.RECORD_ID + "=NEW." + MessageColumns.MAILBOX_KEY +
                "; end");

        // Delete a message.
        db.execSQL("create trigger message_count_message_delete after delete on " +
                Message.TABLE_NAME +
                " begin update " + Mailbox.TABLE_NAME + " set " + MailboxColumns.MESSAGE_COUNT +
                '=' + MailboxColumns.MESSAGE_COUNT + "-1" +
                "  where " + EmailContent.RECORD_ID + "=OLD." + MessageColumns.MAILBOX_KEY +
                "; end");

        // Change a message's mailbox.
        db.execSQL("create trigger message_count_message_move after update of " +
                MessageColumns.MAILBOX_KEY + " on " + Message.TABLE_NAME +
                " begin update " + Mailbox.TABLE_NAME + " set " + MailboxColumns.MESSAGE_COUNT +
                '=' + MailboxColumns.MESSAGE_COUNT + "-1" +
                "  where " + EmailContent.RECORD_ID + "=OLD." + MessageColumns.MAILBOX_KEY +
                "; update " + Mailbox.TABLE_NAME + " set " + MailboxColumns.MESSAGE_COUNT +
                '=' + MailboxColumns.MESSAGE_COUNT + "+1" +
                " where " + EmailContent.RECORD_ID + "=NEW." + MessageColumns.MAILBOX_KEY +
                "; end");
    }

    static void dropDeleteDuplicateMessagesTrigger(final SQLiteDatabase db) {
        db.execSQL("drop trigger message_delete_duplicates_on_insert");
    }

    /**
     * Add a trigger to delete duplicate server side messages before insertion.
     * This should delete any messages older messages that have the same serverId and account as
     * the new message, if:
     *    Neither message is in a SEARCH type mailbox, and
     *    The new message's mailbox's account is an exchange account.
     *
     * Here is the plain text of this sql:
     *   create trigger message_delete_duplicates_on_insert before insert on
     *   Message for each row when new.syncServerId is not null and
     *    (select type from Mailbox where _id=new.mailboxKey) != 8 and
     *    (select HostAuth.protocol from HostAuth, Account where
     *       new.accountKey=account._id and account.hostAuthKeyRecv=hostAuth._id) = 'gEas'
     *   begin delete from Message where new.syncServerId=syncSeverId and
     *   new.accountKey=accountKey and
     *    (select Mailbox.type from Mailbox where _id=mailboxKey) != 8; end
     */
    static void createDeleteDuplicateMessagesTrigger(final Context context,
            final SQLiteDatabase db) {
        db.execSQL("create trigger message_delete_duplicates_on_insert before insert on "
                + Message.TABLE_NAME + " for each row when new." + SyncColumns.SERVER_ID
                + " is not null and "
                + "(select " + MailboxColumns.TYPE + " from " + Mailbox.TABLE_NAME
                + " where " + MailboxColumns.ID + "=new."
                + MessageColumns.MAILBOX_KEY + ")!=" + Mailbox.TYPE_SEARCH
                + " and (select "
                + HostAuth.TABLE_NAME + "." + HostAuthColumns.PROTOCOL + " from "
                + HostAuth.TABLE_NAME + "," + Account.TABLE_NAME
                + " where new." + MessageColumns.ACCOUNT_KEY
                + "=" + Account.TABLE_NAME + "." + AccountColumns.ID
                + " and " + Account.TABLE_NAME + "." + AccountColumns.HOST_AUTH_KEY_RECV
                + "=" + HostAuth.TABLE_NAME + "." + HostAuthColumns.ID
                + ")='" + context.getString(R.string.protocol_eas) + "'"
                + " begin delete from " + Message.TABLE_NAME + " where new."
                + SyncColumns.SERVER_ID + "=" + SyncColumns.SERVER_ID + " and new."
                + MessageColumns.ACCOUNT_KEY + "=" + MessageColumns.ACCOUNT_KEY
                + " and (select " + Mailbox.TABLE_NAME + "." + MailboxColumns.TYPE + " from "
                + Mailbox.TABLE_NAME + " where " + MailboxColumns.ID + "="
                + MessageColumns.MAILBOX_KEY + ")!=" + Mailbox.TYPE_SEARCH +"; end");
    }

    static void createMessageTable(Context context, SQLiteDatabase db) {
        String messageColumns = MessageColumns.DISPLAY_NAME + " text, "
            + MessageColumns.TIMESTAMP + " integer, "
            + MessageColumns.SUBJECT + " text, "
            + MessageColumns.FLAG_READ + " integer, "
            + MessageColumns.FLAG_LOADED + " integer, "
            + MessageColumns.FLAG_FAVORITE + " integer, "
            + MessageColumns.FLAG_ATTACHMENT + " integer, "
            + MessageColumns.FLAGS + " integer, "
            + MessageColumns.DRAFT_INFO + " integer, "
            + MessageColumns.MESSAGE_ID + " text, "
            + MessageColumns.MAILBOX_KEY + " integer, "
            + MessageColumns.ACCOUNT_KEY + " integer, "
            + MessageColumns.FROM_LIST + " text, "
            + MessageColumns.TO_LIST + " text, "
            + MessageColumns.CC_LIST + " text, "
            + MessageColumns.BCC_LIST + " text, "
            + MessageColumns.REPLY_TO_LIST + " text, "
            + MessageColumns.MEETING_INFO + " text, "
            + MessageColumns.SNIPPET + " text, "
            + MessageColumns.PROTOCOL_SEARCH_INFO + " text, "
            + MessageColumns.THREAD_TOPIC + " text, "
            + MessageColumns.SYNC_DATA + " text, "
            + MessageColumns.FLAG_SEEN + " integer, "
            + MessageColumns.MAIN_MAILBOX_KEY + " integer, "
            + MessageColumns.FLAG_INCLUDED + " integer"
            + ");";

        // This String and the following String MUST have the same columns, except for the type
        // of those columns!
        String createString = " (" + EmailContent.RECORD_ID + " integer primary key autoincrement, "
            + SyncColumns.SERVER_ID + " text, "
            + SyncColumns.SERVER_TIMESTAMP + " integer, "
            + messageColumns;

        // For the updated and deleted tables, the id is assigned, but we do want to keep track
        // of the ORDER of updates using an autoincrement primary key.  We use the DATA column
        // at this point; it has no other function
        String altCreateString = " (" + EmailContent.RECORD_ID + " integer unique, "
            + SyncColumns.SERVER_ID + " text, "
            + SyncColumns.SERVER_TIMESTAMP + " integer, "
            + messageColumns;

        // The three tables have the same schema
        db.execSQL("create table " + Message.TABLE_NAME + createString);
        db.execSQL("create table " + Message.UPDATED_TABLE_NAME + altCreateString);
        db.execSQL("create table " + Message.DELETED_TABLE_NAME + altCreateString);

        String indexColumns[] = {
            MessageColumns.TIMESTAMP,
            MessageColumns.FLAG_READ,
            MessageColumns.FLAG_LOADED,
            MessageColumns.MAILBOX_KEY,
            SyncColumns.SERVER_ID
        };

        for (String columnName : indexColumns) {
            db.execSQL(createIndex(Message.TABLE_NAME, columnName));
        }

        // Deleting a Message deletes all associated Attachments
        // Deleting the associated Body cannot be done in a trigger, because the Body is stored
        // in a separate database, and trigger cannot operate on attached databases.
        db.execSQL("create trigger message_delete before delete on " + Message.TABLE_NAME +
                " begin delete from " + Attachment.TABLE_NAME +
                "  where " + AttachmentColumns.MESSAGE_KEY + "=old." + EmailContent.RECORD_ID +
                "; end");

        // Add triggers to keep unread count accurate per mailbox

        // NOTE: SQLite's before triggers are not safe when recursive triggers are involved.
        // Use caution when changing them.

        // Insert a message; if flagRead is zero, add to the unread count of the message's mailbox
        db.execSQL("create trigger unread_message_insert before insert on " + Message.TABLE_NAME +
                " when NEW." + MessageColumns.FLAG_READ + "=0 AND NEW." + MessageColumns.FLAG_INCLUDED +"=1" +
                " begin update " + Mailbox.TABLE_NAME + " set " + MailboxColumns.UNREAD_COUNT +
                '=' + MailboxColumns.UNREAD_COUNT + "+1" +
                "  where " + EmailContent.RECORD_ID + "=NEW." + MessageColumns.MAILBOX_KEY +
                "; end");

        db.execSQL("create trigger unread_message_update before update on " + Message.TABLE_NAME +
                " when OLD." + MessageColumns.FLAG_INCLUDED + "=0" +
                " begin update " + Mailbox.TABLE_NAME + " set " + MailboxColumns.UNREAD_COUNT +
                '=' + MailboxColumns.UNREAD_COUNT + "+1" +
                "  where " + EmailContent.RECORD_ID + "=OLD." + MessageColumns.MAILBOX_KEY +
                "; end");
        
        // Delete a message; if flagRead is zero, decrement the unread count of the msg's mailbox
        db.execSQL("create trigger unread_message_delete before delete on " + Message.TABLE_NAME +
                " when OLD." + MessageColumns.FLAG_READ + "=0 AND OLD." + MessageColumns.FLAG_INCLUDED +"=1" +
                " begin update " + Mailbox.TABLE_NAME + " set " + MailboxColumns.UNREAD_COUNT +
                '=' + MailboxColumns.UNREAD_COUNT + "-1" +
                "  where " + EmailContent.RECORD_ID + "=OLD." + MessageColumns.MAILBOX_KEY +
                "; end");

        // Change a message's mailbox
        db.execSQL("create trigger unread_message_move before update of " +
                MessageColumns.MAILBOX_KEY + " on " + Message.TABLE_NAME +
                " when OLD." + MessageColumns.FLAG_READ + "=0" +
                " begin update " + Mailbox.TABLE_NAME + " set " + MailboxColumns.UNREAD_COUNT +
                '=' + MailboxColumns.UNREAD_COUNT + "-1" +
                "  where " + EmailContent.RECORD_ID + "=OLD." + MessageColumns.MAILBOX_KEY +
                "; update " + Mailbox.TABLE_NAME + " set " + MailboxColumns.UNREAD_COUNT +
                '=' + MailboxColumns.UNREAD_COUNT + "+1" +
                " where " + EmailContent.RECORD_ID + "=NEW." + MessageColumns.MAILBOX_KEY +
                "; end");

        // Change a message's read state
        db.execSQL("create trigger unread_message_read before update of " +
                MessageColumns.FLAG_READ + " on " + Message.TABLE_NAME +
                " when OLD." + MessageColumns.FLAG_READ + "!=NEW." + MessageColumns.FLAG_READ +
                " begin update " + Mailbox.TABLE_NAME + " set " + MailboxColumns.UNREAD_COUNT +
                '=' + MailboxColumns.UNREAD_COUNT + "+ case OLD." + MessageColumns.FLAG_READ +
                " when 0 then -1 else 1 end" +
                "  where " + EmailContent.RECORD_ID + "=OLD." + MessageColumns.MAILBOX_KEY +
                "; end");

        // Add triggers to maintain message_count.
        createMessageCountTriggers(db);
        createDeleteDuplicateMessagesTrigger(context, db);
    }

    static void resetMessageTable(Context context, SQLiteDatabase db,
            int oldVersion, int newVersion) {
        try {
            db.execSQL("drop table " + Message.TABLE_NAME);
            db.execSQL("drop table " + Message.UPDATED_TABLE_NAME);
            db.execSQL("drop table " + Message.DELETED_TABLE_NAME);
        } catch (SQLException e) {
        }
        createMessageTable(context, db);
    }

    /**
     * Common columns for all {@link MessageChangeLogTable} tables.
     */
    private static String MESSAGE_CHANGE_LOG_COLUMNS =
            MessageChangeLogTable.ID + " integer primary key autoincrement, "
            + MessageChangeLogTable.MESSAGE_KEY + " integer, "
            + MessageChangeLogTable.SERVER_ID + " text, "
            + MessageChangeLogTable.ACCOUNT_KEY + " integer, "
            + MessageChangeLogTable.STATUS + " integer, ";

    /**
     * Create indices common to all {@link MessageChangeLogTable} tables.
     * @param db The {@link SQLiteDatabase}.
     * @param tableName The name of this particular table.
     */
    private static void createMessageChangeLogTableIndices(final SQLiteDatabase db,
            final String tableName) {
        db.execSQL(createIndex(tableName, MessageChangeLogTable.MESSAGE_KEY));
        db.execSQL(createIndex(tableName, MessageChangeLogTable.ACCOUNT_KEY));
    }

    /**
     * Create triggers common to all {@link MessageChangeLogTable} tables.
     * @param db The {@link SQLiteDatabase}.
     * @param tableName The name of this particular table.
     */
    private static void createMessageChangeLogTableTriggers(final SQLiteDatabase db,
            final String tableName) {
        // Trigger to delete from the change log when a message is deleted.
        db.execSQL("create trigger " + tableName + "_delete_message before delete on "
                + Message.TABLE_NAME + " for each row begin delete from " + tableName
                + " where " + MessageChangeLogTable.MESSAGE_KEY + "=old." + MessageColumns.ID
                + "; end");

        // Trigger to delete from the change log when an account is deleted.
        db.execSQL("create trigger " + tableName + "_delete_account before delete on "
                + Account.TABLE_NAME + " for each row begin delete from " + tableName
                + " where " + MessageChangeLogTable.ACCOUNT_KEY + "=old." + AccountColumns.ID
                + "; end");
    }

    /**
     * Create the MessageMove table.
     * @param db The {@link SQLiteDatabase}.
     */
    private static void createMessageMoveTable(final SQLiteDatabase db) {
        db.execSQL("create table " + MessageMove.TABLE_NAME + " ("
                + MESSAGE_CHANGE_LOG_COLUMNS
                + MessageMove.SRC_FOLDER_KEY + " integer, "
                + MessageMove.DST_FOLDER_KEY + " integer, "
                + MessageMove.SRC_FOLDER_SERVER_ID + " text, "
                + MessageMove.DST_FOLDER_SERVER_ID + " text);");

        createMessageChangeLogTableIndices(db, MessageMove.TABLE_NAME);
        createMessageChangeLogTableTriggers(db, MessageMove.TABLE_NAME);
    }

    /**
     * Create the MessageStateChange table.
     * @param db The {@link SQLiteDatabase}.
     */
    private static void createMessageStateChangeTable(final SQLiteDatabase db) {
        db.execSQL("create table " + MessageStateChange.TABLE_NAME + " ("
                + MESSAGE_CHANGE_LOG_COLUMNS
                + MessageStateChange.OLD_FLAG_READ + " integer, "
                + MessageStateChange.NEW_FLAG_READ + " integer, "
                + MessageStateChange.OLD_FLAG_FAVORITE + " integer, "
                + MessageStateChange.NEW_FLAG_FAVORITE + " integer);");

        createMessageChangeLogTableIndices(db, MessageStateChange.TABLE_NAME);
        createMessageChangeLogTableTriggers(db, MessageStateChange.TABLE_NAME);
    }

    @SuppressWarnings("deprecation")
    static void createAccountTable(SQLiteDatabase db) {
        String s = " (" + EmailContent.RECORD_ID + " integer primary key , "
            + AccountColumns.DISPLAY_NAME + " text, "
            + AccountColumns.EMAIL_ADDRESS + " text, "
            + AccountColumns.SYNC_KEY + " text, "
            + AccountColumns.SYNC_LOOKBACK + " integer, "
            + AccountColumns.SYNC_INTERVAL + " text, "
            + AccountColumns.HOST_AUTH_KEY_RECV + " integer, "
            + AccountColumns.HOST_AUTH_KEY_SEND + " integer, "
            + AccountColumns.FLAGS + " integer, "
            + AccountColumns.IS_DEFAULT + " integer, "
            + AccountColumns.COMPATIBILITY_UUID + " text, "
            + AccountColumns.SENDER_NAME + " text, "
            + AccountColumns.RINGTONE_URI + " text, "
            + AccountColumns.PROTOCOL_VERSION + " text, "
            + AccountColumns.NEW_MESSAGE_COUNT + " integer, "
            + AccountColumns.SECURITY_FLAGS + " integer, "
            + AccountColumns.SECURITY_SYNC_KEY + " text, "
            + AccountColumns.SIGNATURE + " text, "
            + AccountColumns.POLICY_KEY + " integer, "
            + AccountColumns.PING_DURATION + " integer, "
            + AccountColumns.PIM_ACCOUNT_ID + " integer"
            + ");";
        db.execSQL("create table " + Account.TABLE_NAME + s);
        // Deleting an account deletes associated Mailboxes and HostAuth's
        db.execSQL(TRIGGER_ACCOUNT_DELETE);

        // Insert and delete VirtualMailboxes associated with accounts
        createVirtualMailboxTriggers(db);
    }

    static void resetAccountTable(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            db.execSQL("drop table " +  Account.TABLE_NAME);
        } catch (SQLException e) {
        }
        createAccountTable(db);
    }

    static void createPolicyTable(SQLiteDatabase db) {
        String s = " (" + EmailContent.RECORD_ID + " integer primary key autoincrement, "
            + PolicyColumns.PASSWORD_MODE + " integer, "
            + PolicyColumns.PASSWORD_MIN_LENGTH + " integer, "
            + PolicyColumns.PASSWORD_EXPIRATION_DAYS + " integer, "
            + PolicyColumns.PASSWORD_HISTORY + " integer, "
            + PolicyColumns.PASSWORD_COMPLEX_CHARS + " integer, "
            + PolicyColumns.PASSWORD_MAX_FAILS + " integer, "
            + PolicyColumns.MAX_SCREEN_LOCK_TIME + " integer, "
            + PolicyColumns.REQUIRE_REMOTE_WIPE + " integer, "
            + PolicyColumns.REQUIRE_ENCRYPTION + " integer, "
            + PolicyColumns.REQUIRE_ENCRYPTION_EXTERNAL + " integer, "
            + PolicyColumns.REQUIRE_MANUAL_SYNC_WHEN_ROAMING + " integer, "
            + PolicyColumns.DONT_ALLOW_CAMERA + " integer, "
            + PolicyColumns.DONT_ALLOW_ATTACHMENTS + " integer, "
            + PolicyColumns.DONT_ALLOW_HTML + " integer, "
            + PolicyColumns.MAX_ATTACHMENT_SIZE + " integer, "
            + PolicyColumns.MAX_TEXT_TRUNCATION_SIZE + " integer, "
            + PolicyColumns.MAX_HTML_TRUNCATION_SIZE + " integer, "
            + PolicyColumns.MAX_EMAIL_LOOKBACK + " integer, "
            + PolicyColumns.MAX_CALENDAR_LOOKBACK + " integer, "
            + PolicyColumns.PASSWORD_RECOVERY_ENABLED + " integer, "
            + PolicyColumns.PROTOCOL_POLICIES_ENFORCED + " text, "
            + PolicyColumns.PROTOCOL_POLICIES_UNSUPPORTED + " text"
            + ");";
        db.execSQL("create table " + Policy.TABLE_NAME + s);
    }

    static void createHostAuthTable(SQLiteDatabase db) {
        String s = " (" + EmailContent.RECORD_ID + " integer primary key autoincrement, "
            + HostAuthColumns.PROTOCOL + " text, "
            + HostAuthColumns.ADDRESS + " text, "
            + HostAuthColumns.PORT + " integer, "
            + HostAuthColumns.FLAGS + " integer, "
            + HostAuthColumns.LOGIN + " text, "
            + HostAuthColumns.PASSWORD + " text, "
            + HostAuthColumns.DOMAIN + " text, "
            + HostAuthColumns.ACCOUNT_KEY + " integer,"
            + HostAuthColumns.CLIENT_CERT_ALIAS + " text,"
            + HostAuthColumns.SERVER_CERT + " blob"
            + ");";
        db.execSQL("create table " + HostAuth.TABLE_NAME + s);
    }

    static void resetHostAuthTable(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            db.execSQL("drop table " + HostAuth.TABLE_NAME);
        } catch (SQLException e) {
        }
        createHostAuthTable(db);
    }

    static void createMailboxTable(SQLiteDatabase db) {
        String s = " (" + EmailContent.RECORD_ID + " integer primary key autoincrement, "
            + MailboxColumns.DISPLAY_NAME + " text, "
            + MailboxColumns.SERVER_ID + " text, "
            + MailboxColumns.PARENT_SERVER_ID + " text, "
            + MailboxColumns.PARENT_KEY + " integer, "
            + MailboxColumns.ACCOUNT_KEY + " integer, "
            + MailboxColumns.TYPE + " integer, "
            + MailboxColumns.DELIMITER + " integer, "
            + MailboxColumns.SYNC_KEY + " text, "
            + MailboxColumns.SYNC_LOOKBACK + " integer, "
            + MailboxColumns.SYNC_INTERVAL + " integer, "
            + MailboxColumns.SYNC_TIME + " integer, "
            + MailboxColumns.UNREAD_COUNT + " integer, "
            + MailboxColumns.FLAG_VISIBLE + " integer, "
            + MailboxColumns.FLAGS + " integer, "
            + MailboxColumns.VISIBLE_LIMIT + " integer, "
            + MailboxColumns.SYNC_STATUS + " text, "
            + MailboxColumns.MESSAGE_COUNT + " integer not null default 0, "
            + MailboxColumns.LAST_TOUCHED_TIME + " integer default 0, "
            + MailboxColumns.UI_SYNC_STATUS + " integer default 0, "
            + MailboxColumns.UI_LAST_SYNC_RESULT + " integer default 0, "
            + MailboxColumns.LAST_NOTIFIED_MESSAGE_KEY + " integer not null default 0, "
            + MailboxColumns.LAST_NOTIFIED_MESSAGE_COUNT + " integer not null default 0, "
            + MailboxColumns.TOTAL_COUNT + " integer, "
            + MailboxColumns.HIERARCHICAL_NAME + " text, "
            + MailboxColumns.LAST_FULL_SYNC_TIME + " integer"
            + ");";
        db.execSQL("create table " + Mailbox.TABLE_NAME + s);
        db.execSQL("create index mailbox_" + MailboxColumns.SERVER_ID
                + " on " + Mailbox.TABLE_NAME + " (" + MailboxColumns.SERVER_ID + ")");
        db.execSQL("create index mailbox_" + MailboxColumns.ACCOUNT_KEY
                + " on " + Mailbox.TABLE_NAME + " (" + MailboxColumns.ACCOUNT_KEY + ")");
        // Deleting a Mailbox deletes associated Messages in all three tables
        db.execSQL(TRIGGER_MAILBOX_DELETE);
    }

    static void resetMailboxTable(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            db.execSQL("drop table " + Mailbox.TABLE_NAME);
        } catch (SQLException e) {
        }
        createMailboxTable(db);
    }
    
    static void createVirtualMailboxTable(SQLiteDatabase db) {
        String s = " (" + EmailContent.RECORD_ID + " integer primary key autoincrement, "
            + VirtualMailboxColumns.ACCOUNT_KEY + " integer, "
            + VirtualMailboxColumns.TYPE + " integer"
            + ");";
        db.execSQL("create table " + VirtualMailbox.TABLE_NAME + s);
        db.execSQL("create index virtual_mailbox_" + VirtualMailboxColumns.ACCOUNT_KEY
                + " on " + VirtualMailbox.TABLE_NAME + " (" + VirtualMailboxColumns.ACCOUNT_KEY + ")");

        // insert the virtual mailboxes for the combined account
        db.execSQL("insert into " + VirtualMailbox.TABLE_NAME +
                " (" + VirtualMailboxColumns.ACCOUNT_KEY + ", " + VirtualMailboxColumns.TYPE + ")" +
                " values (" + Account.ACCOUNT_ID_COMBINED_VIEW + ", " + Mailbox.TYPE_INBOX + ")");
        db.execSQL("insert into " + VirtualMailbox.TABLE_NAME +
                " (" + VirtualMailboxColumns.ACCOUNT_KEY + ", " + VirtualMailboxColumns.TYPE + ")" +
                " values (" + Account.ACCOUNT_ID_COMBINED_VIEW + ", " + Mailbox.TYPE_STARRED + ")");
        db.execSQL("insert into " + VirtualMailbox.TABLE_NAME +
                " (" + VirtualMailboxColumns.ACCOUNT_KEY + ", " + VirtualMailboxColumns.TYPE + ")" +
                " values (" + Account.ACCOUNT_ID_COMBINED_VIEW + ", " + Mailbox.TYPE_UNREAD + ")");
    }

    static void resetVirtualMailboxTable(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            db.execSQL("drop table " + VirtualMailbox.TABLE_NAME);
        } catch (SQLException e) {
        }
        createVirtualMailboxTable(db);
    }

    static void createAttachmentTable(SQLiteDatabase db) {
        String s = " (" + EmailContent.RECORD_ID + " integer primary key autoincrement, "
            + AttachmentColumns.FILENAME + " text, "
            + AttachmentColumns.MIME_TYPE + " text, "
            + AttachmentColumns.SIZE + " integer, "
            + AttachmentColumns.CONTENT_ID + " text, "
            + AttachmentColumns.CONTENT_URI + " text, "
            + AttachmentColumns.MESSAGE_KEY + " integer, "
            + AttachmentColumns.LOCATION + " text, "
            + AttachmentColumns.ENCODING + " text, "
            + AttachmentColumns.CONTENT + " text, "
            + AttachmentColumns.FLAGS + " integer, "
            + AttachmentColumns.CONTENT_BYTES + " blob, "
            + AttachmentColumns.ACCOUNT_KEY + " integer, "
            + AttachmentColumns.UI_STATE + " integer, "
            + AttachmentColumns.UI_DESTINATION + " integer, "
            + AttachmentColumns.UI_DOWNLOADED_SIZE + " integer, "
            + AttachmentColumns.CACHED_FILE + " text"
            + ");";
        db.execSQL("create table " + Attachment.TABLE_NAME + s);
        db.execSQL(createIndex(Attachment.TABLE_NAME, AttachmentColumns.MESSAGE_KEY));
    }

    static void resetAttachmentTable(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            db.execSQL("drop table " + Attachment.TABLE_NAME);
        } catch (SQLException e) {
        }
        createAttachmentTable(db);
    }

    static void createQuickResponseTable(SQLiteDatabase db) {
        String s = " (" + EmailContent.RECORD_ID + " integer primary key autoincrement, "
                + QuickResponseColumns.TEXT + " text, "
                + QuickResponseColumns.ACCOUNT_KEY + " integer"
                + ");";
        db.execSQL("create table " + QuickResponse.TABLE_NAME + s);
    }

    static void createBodyTable(SQLiteDatabase db) {
        String s = " (" + EmailContent.RECORD_ID + " integer primary key autoincrement, "
            + BodyColumns.MESSAGE_KEY + " integer, "
            + BodyColumns.HTML_CONTENT + " text, "
            + BodyColumns.TEXT_CONTENT + " text, "
            + BodyColumns.HTML_REPLY + " text, "
            + BodyColumns.TEXT_REPLY + " text, "
            + BodyColumns.SOURCE_MESSAGE_KEY + " text, "
            + BodyColumns.INTRO_TEXT + " text, "
            + BodyColumns.QUOTED_TEXT_START_POS + " integer"
            + ");";
        db.execSQL("create table " + Body.TABLE_NAME + s);
        db.execSQL(createIndex(Body.TABLE_NAME, BodyColumns.MESSAGE_KEY));
    }

    static void upgradeBodyTable(SQLiteDatabase db, int oldVersion, int newVersion) {
        LogUtils.i(LogUtils.TAG, "Upgrading Body database from %d to %d db=", oldVersion, newVersion, db.getPath());
        // Nothing to upgrade yet - will ship with version 1 (assumption)
    }

    protected static class BodyDatabaseHelper extends SQLiteOpenHelper {
        BodyDatabaseHelper(Context context, String name) {
            super(context, name, null, BODY_DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            LogUtils.d(TAG, "Creating EmailProviderBody database");
            createBodyTable(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            upgradeBodyTable(db, oldVersion, newVersion);
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
        }
    }

    /** Counts the number of messages in each mailbox, and updates the message count column. */
    @VisibleForTesting
    static void recalculateMessageCount(SQLiteDatabase db) {
        db.execSQL("update " + Mailbox.TABLE_NAME + " set " + MailboxColumns.MESSAGE_COUNT +
                "= (select count(*) from " + Message.TABLE_NAME +
                " where " + Message.MAILBOX_KEY + " = " +
                    Mailbox.TABLE_NAME + "." + EmailContent.RECORD_ID + ")");
    }

    protected static class DatabaseHelper extends SQLiteOpenHelper {
        Context mContext;

        DatabaseHelper(Context context, String name) {
            super(context, name, null, DATABASE_VERSION);
            mContext = context;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            LogUtils.d(TAG, "Creating EmailProvider database");
            // Create all tables here; each class has its own method
            createMessageTable(mContext, db);
            createAttachmentTable(db);
            createMailboxTable(db);
            createVirtualMailboxTable(db);
            createHostAuthTable(db);
            createAccountTable(db);
            createMessageMoveTable(db);
            createMessageStateChangeTable(db);
            createPolicyTable(db);
            createQuickResponseTable(db);
        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion == 101 && newVersion == 100) {
                LogUtils.d(TAG, "Downgrade from v101 to v100");
            } else {
                super.onDowngrade(db, oldVersion, newVersion);
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            LogUtils.i(LogUtils.TAG, "Upgrading from %d to %d db=", oldVersion, newVersion, db.getPath());
            // Nothing to upgrade yet, will ship with version 1 (assumption)
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            try {
                // Cleanup some nasty records
                db.execSQL("DELETE FROM " + Account.TABLE_NAME
                        + " WHERE " + AccountColumns.DISPLAY_NAME + " ISNULL;");
                db.execSQL("DELETE FROM " + HostAuth.TABLE_NAME
                        + " WHERE " + HostAuthColumns.PROTOCOL + " ISNULL;");
            } catch (SQLException e) {
                // Shouldn't be needed unless we're debugging and interrupt the process
                LogUtils.e(TAG, e, "Exception cleaning EmailProvider.db");
            }
        }
    }

    @VisibleForTesting
    @SuppressWarnings("deprecation")
    static void convertPolicyFlagsToPolicyTable(SQLiteDatabase db) {
        Cursor c = db.query(Account.TABLE_NAME,
                new String[] {EmailContent.RECORD_ID /*0*/, AccountColumns.SECURITY_FLAGS /*1*/},
                AccountColumns.SECURITY_FLAGS + ">0", null, null, null, null);
        try {
            ContentValues cv = new ContentValues();
            String[] args = new String[1];
            while (c.moveToNext()) {
                long securityFlags = c.getLong(1 /*SECURITY_FLAGS*/);
                Policy policy = LegacyPolicySet.flagsToPolicy(securityFlags);
                long policyId = db.insert(Policy.TABLE_NAME, null, policy.toContentValues());
                cv.put(AccountColumns.POLICY_KEY, policyId);
                cv.putNull(AccountColumns.SECURITY_FLAGS);
                args[0] = Long.toString(c.getLong(0 /*RECORD_ID*/));
                db.update(Account.TABLE_NAME, cv, EmailContent.RECORD_ID + "=?", args);
            }
        } finally {
            c.close();
        }
    }
}
