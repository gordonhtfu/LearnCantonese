
package com.blackberry.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public final class AccountContract {
    // put Intent const here
    public static final String CREATE_ACCOUNT_ACTION = "com.blackberry.account.CREATE_ACCOUNT";
    public static final String EDIT_ACCOUNT_ACTION = "com.blackberry.account.EDIT_ACCOUNT";
    public static final String REMOVE_ACCOUNT_ACTION = "com.blackberry.account.REMOVE_ACCOUNT";
    
    // account type constants
    public static final String EMAIL_ACCOUNT_TYPE_PREFIX = "com.blackberry.email";
    public static final String EAS_ACCOUNT_TYPE = "com.blackberry.eas";

    // put extra Keys here

    /**
     * This authority is used for writing to or querying from the account
     * provider. Note: This is set at first run and cannot be changed without
     * breaking apps that access the provider.
     */
    public static final String AUTHORITY = "com.blackberry.account.provider";
    public static final String NOTIFY_AUTHORITY = "com.blackberry.acount.notifier";

    /**
     * The content:// style URL for the top-level account authority
     */
    public static Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    protected interface AccountColumns extends BaseColumns {
        // The display name of the account (user-settable)
        public static final String DISPLAY_NAME = "display_name";
        // The authority for the message item content provider
        public static final String CONTENT_PROVIDER_AUTHORITY = "cp_authority";
        // type of account - may link to AccountManager type
        public static final String TYPE = "type";
        // name of account - may link to AccountManager name
        public static final String NAME = "name";
        // status of account
        public static final String STATUS = "status";
        // capabilities that this account supports
        public static final String CAPABILITIES = "capabilities";
        // description of account
        public static final String DESCRIPTION = "description";
        // application name that account belongs to
        public static final String APPLICATION_NAME = "application_name";
        public static final String ACCOUNT_ICON = "application_icon";
        // install package name (will be determined at runtime and override if
        // set by calling app
        public static final String PACKAGE_NAME = "package_name";
        // if underlying application uses their own account they can add it for
        // easy mapping
        public static final String LOCAL_ACCOUNT_ID = "local_acct_id";
    }

    protected interface AccountAttributeColumns extends BaseColumns {
        public static final String PIM_TYPE = "pim_type";
        public static final String ATTR_NAME = "name";
        public static final String ATTR_VALUE = "value";
        public static final String ACCOUNT_KEY = "account_key";
    }

    public static final class Account implements AccountColumns {
        public static final String TABLE_NAME = "Accounts";
        public static final String URI_SUFFIX = "account";

        // List of provider mime-types
        public static String ACCOUNT_MIMETYPE = "vnd.android.cursor.dir/vnd.blackberry.account";
        public static String ACCOUNT_ITEM_MIMETYPE = "vnd.android.cursor.item/vnd.blackberry.account";

        // status values flag
        public static final int FLAG_STATUS_INACTIVE = 1 << 0;
        public static final int FLAG_STATUS_ACTIVE = 1 << 1;
        public static final int FLAG_STATUS_PENDING_CREATION = 1 << 2;
        public static final int FLAG_STATUS_PENDING_DELETION = 1 << 3;

        /**
         * Account Capabilities constants.
         * Current high bit in use: 27
         */
        public static final long SUPPORTS_CALENDARS = 1 << 0;
        public static final long SUPPORTS_CONTACTS = 1 << 1;
        public static final long SUPPORTS_MESSAGES = 1 << 2;
        public static final long SUPPORTS_TASKS = 1 << 3;
        public static final long SUPPORTS_MEMOS = 1 << 4;
        public static final long SUPPORTS_NOTES = 1 << 5;
        public static final long SUPPORTS_OUT_OF_OFFICE = 1 << 6;
        public static final long SUPPORTS_OUT_OF_OFFICE_EXTERNAL_MESSAGES = 1 << 7;
        public static final long SUPPORTS_OUT_OF_OFFICE_SCHEDULE_DATE = 1 << 8;
        public static final long SUPPORTS_OUT_OF_OFFICE_SCHEDULE_TIME = 1 << 9;
        public static final long SUPPORTS_OUT_OF_OFFICE_EVENT = 1 << 10;
        public static final long SUPPORTS_SMART_MESSAGES = 1 << 11;
        public static final long SUPPORTS_EMPTY_FOLDER = 1 << 12;
        public static final long SUPPORTS_TRASH_SYNC = 1 << 13;
        public static final long SUPPORTS_DRAFT_SYNC = 1 << 14;
        public static final long SUPPORTS_REMOTE_FOLDER_CONTENT_SEARCH = 1 << 15;
        public static final long SUPPORTS_REMOTE_GAL_SEARCH = 1 << 16;
        public static final long SUPPORTS_FREE_BUSY = 1 << 17;
        public static final long SUPPORTS_EXCEPTION_PARTICIPANTS = 1 << 18;
        public static final long SUPPORTS_MEETING_PARTICIPANTS = 1 << 19;
        public static final long SUPPORTS_RECURRENCE_COUNT = 1 << 20;
        public static final long SUPPORTS_EDIT_EVENT_TYPE = 1 << 21;
        public static final long SUPPORTS_CALENDAR_AUTO_SCHEDULE = 1 << 22;
        public static final long SUPPORTS_DUE_TIME = 1 << 23;
        public static final long SUPPORTS_TASKS_FOLDER_MANAGEMENT = 1 << 24;
        public static final long SUPPORTS_NOTES_FOLDER_MANAGEMENT = 1 << 25;
        public static final long SUPPORTS_MESSAGES_FOLDER_MANAGEMENT = 1 << 26;
        public static final long SUPPORTS_COMPOSE = 1 << 27;

        public static final Uri CONTENT_URI = Uri
                .parse("content://" + AUTHORITY + "/" + URI_SUFFIX);

        public static final String[] DEFAULT_PROJECTION = {
                Account._ID,
                Account.ACCOUNT_ICON,
                Account.APPLICATION_NAME,
                Account.CAPABILITIES,
                Account.CONTENT_PROVIDER_AUTHORITY,
                Account.DESCRIPTION,
                Account.DISPLAY_NAME,
                Account.LOCAL_ACCOUNT_ID,
                Account.NAME,
                Account.PACKAGE_NAME,
                Account.STATUS,
                Account.TYPE
        };

    }

    public static final class AccountAttribute implements AccountAttributeColumns {
        public static final String TABLE_NAME = "AccountAttributes";
        public static final String URI_SUFFIX = "attribute";

        public static String ACCOUNT_ATTR_MIMETYPE = "vnd.android.cursor.dir/vnd.blackberry.account.attr";
        public static String ACCOUNT_ATTR_ITEM_MIMETYPE = "vnd.android.cursor.item/vnd.blackberry.account.attr";

        // Some defined PIM TYPES
        public static final String PIM_TYPE_MESSAGE = "Message";
        public static final String PIM_TYPE_CONTACTS = "Contacts";
        public static final String PIM_TYPE_CALENDAR = "Calendar";
        public static final String PIM_TYPE_NOTES = "Notes";
        public static final String PIM_TYPE_OTHER = "Other";

        // some defined ATT_NAMES
        public static final String ATT_NAME_CONTENT_PROVIDER_AUTHORITY = "ContentProviderAuthority";

        /**
         * Attribute key that defines messaging service package name
         */
        public static final String ATT_NAME_MESSAGING_SERVICE_PACKAGE = "MessagingServicePackage";
        /**
         * Attribute key that defines messaging service class name
         */
        public static final String ATT_NAME_MESSAGING_SERVICE_CLASS = "MessagingServiceClass";

        public static final Uri CONTENT_URI = Uri
                .parse("content://" + AUTHORITY + "/" + URI_SUFFIX);

        public static final String[] DEFAULT_PROJECTION = {
                AccountAttribute._ID,
                AccountAttribute.ACCOUNT_KEY,
                AccountAttribute.ATTR_NAME,
                AccountAttribute.ATTR_VALUE,
                AccountAttribute.PIM_TYPE
        };
    }
}
