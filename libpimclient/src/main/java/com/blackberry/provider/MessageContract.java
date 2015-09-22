
package com.blackberry.provider;

import android.net.Uri;
import android.provider.BaseColumns;

import com.blackberry.message.service.IMessagingService;

public class MessageContract {

    public static String AUTHORITY = "com.blackberry.message.provider";
    public static String NOTIFY_AUTHORITY = "com.blackberry.message.notifier";
    public static Uri LISTITEM_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/"
            + ListItemContract.URI_SUFFIX);

    /**
     * An optional insert, update or delete URI parameter that allows the caller
     * to specify that it is a sync adapter. The default value is false which means 
     * any calls to delete will just mark the row dirty/deleted ( i.e the row still exists) and if
     * any modification occurs the row will be marked as dirty.  When true , the clearing of the 
     * dirty flag is done and in the case of a delete, the row will be removed. 
     * 
     * @see Uri.Builder#appendQueryParameter(java.lang.String, java.lang.String)
     */
    public static final String CALLER_IS_SYNCADAPTER = "caller_is_syncadapter";

    /**
     * Base or common Columns
     */
    protected interface BaseMessageColumns {
        /**
         * This column contains the long long account id registered with the
         * AccountProvider
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String ACCOUNT_ID = "account_id";
    }

    /**
     * Names of columns representing fields in a Message.
     */
    protected interface MessageColumns extends BaseColumns, BaseMessageColumns {

        /**
         * String that contains conversation Id for this message belongs to
         * example :
         * content://com.blackberry.emailservices.provider/conversation/1
         */
        public static final String CONVERSATION_ID = "conversation_id";

        /**
         * The id of the Folder. Column name.
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String FOLDER_ID = "folder_id";

        /**
         * This string column contains the subject string of a inbox item -
         * example subject of a message
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String SUBJECT = "subject";

        /**
         * This string column contains the name of the Sender
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String SENDER = "sender";

        /**
         * This long column contains the timestamp example timestamp of received
         * of a message
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String TIMESTAMP = "timestamp";

        /**
         * long column State of message item - see Message.state
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String STATE = "state";

        /**
         * Attachment count on the message
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String ATTACHMENT_COUNT = "attachment_count";

    }

    /**
     * Generic columns for use by sync adapters. The specific functions of these
     * columns are private to the sync adapter. Other clients of the API should
     * not attempt to either read or write this column.
     * 
     * @see Message
     * @see Folder
     */
    protected interface BaseSyncColumns {

        /** Generic column for use by sync adapters. */
        public static final String SYNC_DATA1 = "sync1";
        /** Generic column for use by sync adapters. */
        public static final String SYNC_DATA2 = "sync2";
        /** Generic column for use by sync adapters. */
        public static final String SYNC_DATA3 = "sync3";
        /** Generic column for use by sync adapters. */
        public static final String SYNC_DATA4 = "sync4";
        /** Generic column for use by sync adapters. */
        public static final String SYNC_DATA5 = "sync5";
    }

    /**
     * Columns that appear when each row of a table belongs to a specific
     * account, including sync information that an account may need.
     * 
     * @see Message
     * @see Folder
     */
    protected interface SyncColumns extends BaseSyncColumns {
        /**
         * The unique remote or servicer side ID for a row assigned by the sync
         * source. NULL if the row has never been synced. This is used as a
         * reference id for exceptions along with {@link BaseColumns#_ID}.
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String REMOTE_ID = "remote_id";

        /**
         * Used to indicate that local, unsynced, changes are present.
         * <P>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String DIRTY = "dirty";

        /**
         * Whether the row has been deleted but not synced to the server. A
         * deleted row should be ignored.
         * <P>
         * Type: INTEGER (boolean)
         * </P>
         */
        public static final String DELETED = "deleted";

    }

    /**
     * Columns that appear when each row of a table belongs to a specific
     * account, including sync information that an account may need.
     */
    protected interface FolderSyncColumns extends SyncColumns {
        /**
         * The unique ID for a parent row assigned by the source. NULL if the
         * row has never been synced. This is used as a reference id for
         * exceptions along with {@link BaseColumns#_ID}.
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String PARENT_REMOTE_ID = "parent_remote_id";

        /**
         * I have added this for now as it is used by our Email SAMS
         * <P>
         * TYPE: integer
         * </P
         * ?
         */
        public static final String FLAGS = "flags";
    }

    protected interface EntityColumns {
        /**
         * The unique uri (need to get Chris T def for this)
         * <P>
         * Type: TEXT
         * </P>
         */
        public static String ENTITY_URI = "entity_uri";

        /**
         * The mimeType of this entity
         * <P>
         * Type: TEXT
         * </P>
         */
        public static String MIME_TYPE = "mime_type";

    }

    /**
     * These states are common to Message and Conversation
     */
    protected interface BaseState {
        public static final long NO_STATE             = 0;
        public static final long DRAFT                = 1L << 0;
        public static final long INBOUND              = 1L << 1;
        public static final long PENDING              = 1L << 2;
        public static final long SENDING              = 1L << 3;
        public static final long SENT                 = 1L << 4;
        public static final long ERROR                = 1L << 5;
        public static final long READ                 = 1L << 6;
        public static final long UNREAD               = 1L << 7;
        public static final long FILED                = 1L << 8;
        public static final long PRIORITY             = 1L << 9;
        public static final long LOW_IMPORTANCE       = 1L << 10;
        public static final long HIGH_IMPORTANCE      = 1L << 11;
        public static final long MEETING_INVITE       = 1L << 12;
    };

    public static final class Message implements MessageColumns, EntityColumns, SyncColumns {

        public static final String URI_SUFFIX = "message";
        public static final Uri CONTENT_URI = Uri
                .parse("content://" + AUTHORITY + "/" + URI_SUFFIX);
        public static final Uri CONTENT_NOTIFIER_URI = Uri.parse("content://" + NOTIFY_AUTHORITY
                + "/" + URI_SUFFIX);

        public static final String[] DEFAULT_PROJECTION = {
                Message._ID,
                Message.ENTITY_URI,
                Message.ACCOUNT_ID,
                Message.CONVERSATION_ID,
                Message.FOLDER_ID,
                Message.SUBJECT,
                Message.SENDER,
                Message.MIME_TYPE,
                Message.TIMESTAMP,
                Message.STATE,
                Message.ATTACHMENT_COUNT,
                Message.REMOTE_ID,
                Message.DIRTY,
                Message.DELETED

        };

        public static final class State implements BaseState {
            public static final long FLAGGED                         = 1L << 24;
            public static final long REPLIED_TO                      = 1L << 25;
            public static final long REPLIED_TO_ALL                  = 1L << 26;
            public static final long FORWARDED                       = 1L << 27;
            public static final long LEVEL_ONE_PRIORITY              = 1L << 28;
            public static final long HIDDEN                          = 1L << 29;

            // these are from emailprovider I have included them for now
            public static final long MEETING_CANCEL                  = 1L << 30;
            public static final long TYPE_REPLY                      = 1L << 31;
            public static final long TYPE_FORWARD                    = 1L << 32;
            public static final long TYPE_ORIGINAL                   = 1L << 33;
            public static final long FLAG_TYPE_REPLY_ALL             = 1L << 34;

            /**
             * If set, the outgoing message should *not* include the quoted
             * original message.
             */
            public static final long FLAG_NOT_INCLUDE_QUOTED_TEXT    = 1L << 35;

            public static final long FLAG_INCOMING_MEETING_INVITE    = 1L << 36;
            public static final long FLAG_INCOMING_MEETING_CANCEL    = 1L << 37;

            // (e.g. invites TO others)
            public static final long FLAG_OUTGOING_MEETING_INVITE    = 1L << 38;
            public static final long FLAG_OUTGOING_MEETING_CANCEL    = 1L << 39;
            public static final long FLAG_OUTGOING_MEETING_ACCEPT    = 1L << 40;
            public static final long FLAG_OUTGOING_MEETING_DECLINE   = 1L << 41;
            public static final long FLAG_OUTGOING_MEETING_TENTATIVE = 1L << 42;
            public static final long DRAFT_INFO_APPEND_REF_MESSAGE   = 1L << 43;
            public static final long OUTGOING_MESSAGE                = 1L << 44 ;

            public static final long FLAG_INCOMING_MEETING_MASK =
                    FLAG_INCOMING_MEETING_INVITE | FLAG_INCOMING_MEETING_CANCEL;
   
            public static final long FLAG_OUTGOING_MEETING_MASK =
                    FLAG_OUTGOING_MEETING_INVITE | FLAG_OUTGOING_MEETING_CANCEL |
                            FLAG_OUTGOING_MEETING_ACCEPT | FLAG_OUTGOING_MEETING_DECLINE |
                            FLAG_OUTGOING_MEETING_TENTATIVE;
            public static final long FLAG_OUTGOING_MEETING_REQUEST_MASK =
                    FLAG_OUTGOING_MEETING_INVITE | FLAG_OUTGOING_MEETING_CANCEL;

        };

        private Message() {
        }

    }

    /**
     * Columns for the MessageContact
     */
    protected interface MessageContactColumns extends BaseColumns, BaseMessageColumns {
        public static final String NAME = "name";
        public static final String ADDRESS = "address";
        public static final String ADDRESS_TYPE = "address_type";
        public static final String FIELD_TYPE = "field_type";

        /**
         * The row id of the Message.
         * <P>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String MESSAGE_ID = "message_id";
    }

    public static final class MessageContact implements MessageContactColumns, EntityColumns {
        public static final String URI_SUFFIX = "messagecontact";
        public static final Uri CONTENT_URI = Uri
                .parse("content://" + AUTHORITY + "/" + URI_SUFFIX);
        public static final Uri CONTENT_NOTIFIER_URI = Uri.parse("content://" + NOTIFY_AUTHORITY
                + "/" + URI_SUFFIX);

        public static final String[] DEFAULT_PROJECTION = {
            MessageContactColumns._ID,
            MessageContactColumns.NAME,
            MessageContactColumns.ADDRESS,
            MessageContactColumns.ADDRESS_TYPE,
            MessageContactColumns.FIELD_TYPE,
            MessageContactColumns.MESSAGE_ID
        };

        public static final class AddrType {
            public static final int EMAIL = 0;
            public static final int PHONE = 1;
            public static final int PIN = 2;
        }

        public static final class FieldType {
            public static final int TO = 0;
            public static final int FROM = 1;
            public static final int CC = 2;
            public static final int BCC = 3;
            public static final int REPLY_TO = 4;
        }

    }

    /**
     * Columns for MessageAttachment
     */
    protected interface MessageAttachmentColumns extends BaseColumns, BaseMessageColumns {

        /**
         * Name of the attachment
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String NAME = "name";

        /**
         * 
         */
        public static final String URI = "uri";

        /**
         * The current size of the Attachment
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String SIZE = "size";

        /**
         * The current state of the Attachment
         * <P>
         * Type: INTEGER (see MessageAttachment.State)
         * </P>
         */
        public static final String STATE = "state";

        /**
         * The row id of the Message.
         * <P>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String MESSAGE_ID = "message_id";

    }

    public static final class MessageAttachment implements MessageAttachmentColumns, EntityColumns {
        public static final String URI_SUFFIX = "messageattachment";
        public static final Uri CONTENT_URI = Uri
                .parse("content://" + AUTHORITY + "/" + URI_SUFFIX);
        public static final Uri CONTENT_NOTIFIER_URI = Uri.parse("content://" + NOTIFY_AUTHORITY
                + "/" + URI_SUFFIX);

        public static final String[] DEFAULT_PROJECTION = {
                MessageAttachment._ID,
                MessageAttachment.NAME,
                MessageAttachment.MIME_TYPE,
                MessageAttachment.URI,
                MessageAttachment.MESSAGE_ID,
                MessageAttachment.ACCOUNT_ID,
                MessageAttachment.SIZE,
                MessageAttachment.STATE
        };

        public static final class State {
            public static final int DOWNLOADED = 1 << 0;
            public static final int DOWNLOADING = 1 << 1;
        }

    }

    /**
     * @ MessageBody Contract
     */
    protected interface MessageBodyColumns extends BaseColumns, BaseMessageColumns , SyncColumns {
        public static final String TYPE = "type";
        public static final String PATH = "path";
        public static final String STATE = "state";
        // Message Body data/handle
        public static final String DATA = "data";

        /**
         * The row id of the Message.
         * <P>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String MESSAGE_ID = "message_id";
    }

    public static final class MessageBody implements MessageBodyColumns, EntityColumns {
        public static final String URI_SUFFIX = "messagebody";
        public static final Uri CONTENT_URI = Uri
                .parse("content://" + AUTHORITY + "/" + URI_SUFFIX);
        public static final Uri CONTENT_NOTIFIER_URI = Uri.parse("content://" + NOTIFY_AUTHORITY
                + "/" + URI_SUFFIX);

        public static final String[] DEFAULT_PROJECTION = {
            MessageBodyColumns._ID,
            MessageBodyColumns.TYPE,
            MessageBodyColumns.PATH,
            MessageBodyColumns.DATA,
            MessageBodyColumns.STATE,
            MessageBodyColumns.ACCOUNT_ID,
            MessageBodyColumns.MESSAGE_ID
        };

        public static final class Type {
            public static final int HTML = 0;
            public static final int TEXT = 1;
        }

        public static final class State {
            public static final int DOWNLOADED = 1 << 0;
        }

    }

    /**
     * Columns for Folder
     */
    protected interface FolderColumns extends BaseColumns, BaseMessageColumns {

        /**
         * The id of the Parent Folder. Column name.
         * <P>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String PARENT_ID = "parent_id";

        /**
         * This string column contains the human visible name for the folder.
         * example : Inbox,Drafts
         */
        public static final String NAME = "name";

        /**
         * This String column any description for the Folder
         */
        public static final String DESCRIPTION = "description";

        /**
         * The long column see FolderType
         */
        public static final String TYPE = "type";

        /**
         * This long column contains current sync status of the folder; some
         * combination of the SyncStatus bits defined above
         */
        public static final String SYNC_STATE = "state";

        /**
         * This long column represents the capabilities of the Folder , see
         * FolderCapabilities
         */
        public static final String CAPABILITIES = "configuration";

    }

    public static final class Folder implements FolderColumns, FolderSyncColumns, EntityColumns {

        public static final String URI_SUFFIX = "folder";
        public static final Uri CONTENT_URI = Uri
                .parse("content://" + AUTHORITY + "/" + URI_SUFFIX);
        public static final Uri CONTENT_NOTIFIER_URI = Uri.parse("content://" + NOTIFY_AUTHORITY
                + "/" + URI_SUFFIX);

        public static int NO_PARENT_FOLDER = -1;

        public static final String[] DEFAULT_PROJECTION = {
                Folder._ID,
                Folder.ENTITY_URI,
                Folder.ACCOUNT_ID,
                Folder.PARENT_ID,
                Folder.NAME,
                Folder.DESCRIPTION,
                Folder.TYPE,
                Folder.SYNC_STATE,
                Folder.CAPABILITIES,
                Folder.REMOTE_ID,
                Folder.PARENT_REMOTE_ID
        };

        public static final String[] ALL_PROJECTION = {
            Folder._ID,
            Folder.ENTITY_URI,
            Folder.ACCOUNT_ID,
            Folder.PARENT_ID,
            Folder.NAME,
            Folder.DESCRIPTION,
            Folder.TYPE,
            Folder.SYNC_STATE,
            Folder.CAPABILITIES,
            Folder.REMOTE_ID,
            Folder.PARENT_REMOTE_ID,
            Folder.FLAGS,
            Folder.SYNC_DATA1,
            Folder.SYNC_DATA2,
            Folder.SYNC_DATA3,
            Folder.SYNC_DATA4,
            Folder.SYNC_DATA5,
            Folder.DIRTY,
            Folder.DELETED
        };

        public static final class Capabilities {
            public static final int SYNCABLE =          1 << 0;
            public static final int AUTO_SYNC_ENABLED = 1 << 1;
            public static final int DELETE =            1 << 2;
            public static final int RENAME =            1 << 3;
            public static final int MOVE =              1 << 4;
            public static final int ADD_CHILD =         1 << 5;
            public static final int ALLOWS_CONTENT =    1 << 6;
            public static final int EMPTY_COMMAND =     1 << 7;
        }

        public static final class State {
            /* Indicates if folder is syncing */
            public static final int SYNCING = 1 << 0;
        };

        public static final class Type {
            /** Personal Folder */
            public static final int PERSONAL = 0;
            /** A system defined inbox */
            public static final int INBOX = 1;
            /** A system defined Draft . */
            public static final int DRAFT = 2;
            /** A system defined folder containing mails <b>to be</b> sent */
            public static final int OUTBOX = 3;
            /** A system defined folder containing sent mails */
            public static final int SENT = 4;
            /** A system defined trash folder */
            public static final int TRASH = 5;
            /** A system defined spam folder */
            public static final int SPAM = 6;
            /** A system defined starred folder */
            public static final int STARRED = 7;
            /** Parent-only mailbox; does not hold any mail */
            public static final int TYPE_PARENT = 8;
            /** Any other system label */
            public static final int OTHER_PROVIDER_FOLDER = 9;
        }

        private Folder() {
        }

    }

    /**
     * @author Conversation
     */

    public interface ConversationColumns extends BaseColumns {
        /**
         * This string column contains the URI of the conversation.  Example :
         * content://com.blackberry.message.provider/conversation/1
         */
        public static final String CONVERSATION_ID = "conversation_id";

        /**
         * This column contains the long long account id registered with the
         * AccountProvider
         */
        public static final String ACCOUNT_ID = "account_id";

        /**
         * This string column contains the user assigned name for the conversation.
         */
        public static final String NAME = "name";

        /**
         * The state of the conversation
         */
        public static final String STATE = "state";

        /**
         * The active participants of the conversation
         */
        public static final String PARTICIPANTS = "participants";

        /**
         * The summary of the body of the last message in the conversation
         */
        public static final String SUMMARY = "summary";

        /**
         * This long column contains the timestamp of the last message
         */
        public static final String LAST_MESSAGE_TIMESTAMP = "last_message_timestamp";

        /**
         * long column State of Conversation item - see Message.State
         */
        public static final String LAST_MESSAGE_STATE = "last_message_state";

        /**
         * Source URI of the latest message in the conversation, example :
         * content://com.blackberry.message.provider/message/215
         */
        public static final String LAST_MESSAGE_ID = "last_message_id";

        /**
         * Integer column containing the number of unread messages in the conversation.
         */
        public static final String UNREAD_COUNT = "unread_count";

        /**
         * Integer column containing the number of draft messages in the conversation.
         */
        public static final String DRAFT_COUNT = "draft_count";

        /**
         * Integer column containing the number of sent messages in the conversation.
         */
        public static final String SENT_COUNT = "sent_count";

        /**
         * Integer column containing the number of errored messages in the conversation.
         */
        public static final String ERROR_COUNT = "error_count";

        /**
         * Integer column containing the number of filed messages in the conversation.
         */
        public static final String FILED_COUNT = "filed_count";

        /**
         * Integer column containing the number of inbound messages in the conversation.
         */
        public static final String INBOUND_COUNT = "inbound_count";

        /**
         * Integer column containing the number of flagged messages in the conversation.
         */
        public static final String FLAGGED_COUNT = "flagged_count";

        /**
         * Integer column containing the number of high importance messages in the conversation.
         */
        public static final String HIGH_IMPORTANCE_COUNT = "high_importance_count";

        /**
         * Integer column containing the number of low importance messages in the conversation.
         */
        public static final String LOW_IMPORTANCE_COUNT = "low_importance_count";

        /**
         * Integer column containing the number of meeting invites in the conversation.
         */
        public static final String MEETING_INVITE_COUNT = "meeting_invite_count";

        /**
         * Integer column containing the total number of messages in the conversation.
         */
        public static final String TOTAL_MESSAGE_COUNT = "total_message_count";

        /**
         * Integer column containing the total number of attachments in the conversation.
         */
        public static final String TOTAL_ATTACHMENT_COUNT = "total_attachment_count";

    }

    public static final class Conversation implements ConversationColumns {

        public static final String URI_SUFFIX = "conversations";
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + URI_SUFFIX);
        public static final Uri CONTENT_NOTIFIER_URI = Uri.parse("content://" + NOTIFY_AUTHORITY
                + "/" + URI_SUFFIX);

        public static final String[] DEFAULT_PROJECTION = {
                Conversation._ID,
                Conversation.CONVERSATION_ID,
                Conversation.ACCOUNT_ID,
                Conversation.NAME,
                Conversation.STATE,
                Conversation.PARTICIPANTS,
                Conversation.SUMMARY,
                Conversation.LAST_MESSAGE_TIMESTAMP,
                Conversation.LAST_MESSAGE_STATE,
                Conversation.LAST_MESSAGE_ID,
                Conversation.UNREAD_COUNT,
                Conversation.DRAFT_COUNT,
                Conversation.SENT_COUNT,
                Conversation.ERROR_COUNT,
                Conversation.FILED_COUNT,
                Conversation.INBOUND_COUNT,
                Conversation.FLAGGED_COUNT,
                Conversation.HIGH_IMPORTANCE_COUNT,
                Conversation.LOW_IMPORTANCE_COUNT,
                Conversation.MEETING_INVITE_COUNT,
                Conversation.TOTAL_MESSAGE_COUNT,
                Conversation.TOTAL_ATTACHMENT_COUNT
        };

        public static final class State implements BaseState {
            public static final long ATTACHMENT           = 1L << 24;
            public static final long SINGLE_FOLLOWUPFLAG  = 1L << 25;
            public static final long MULTI_FOLLOWUPFLAG   = 1L << 26;
        };

        private Conversation() {
        }

    }

    /**
     * Actions to be performed by {@link IMessagingService} through bulkActionMessage method.
     */
    public static interface MessagingBulkAction {
        public static final int DELETE = 1;
        public static final int MARK_READ = 2;
        public static final int MARK_UNREAD = 3;
    }
}
