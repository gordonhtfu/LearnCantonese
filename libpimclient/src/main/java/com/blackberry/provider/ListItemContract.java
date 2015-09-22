
package com.blackberry.provider;

import android.provider.BaseColumns;

public class ListItemContract {

    public static final String URI_SUFFIX = "list_item";

    public interface ListItemColumns extends BaseColumns {

        /**
         * ID of the account associated with the list item.
         */
        public static final String ACCOUNT_ID = "account_id";

        /**
         * MIME type associated with the list item.
         */
        public static final String MIME_TYPE = "mime_type";

        /**
         * Unique ID identifying this list item within its data domain (Account and MIME type).
         */
        public static final String DUID = "duid";

        /**
         * Source URI for this list item from its source content provider.
         */
        public static final String URI = "uri";

        /**
         * Primary list item text.
         */
        public static final String PRIMARY_TEXT = "primary_text";

        /**
         * Secondary list item text.
         */
        public static final String SECONDARY_TEXT = "secondary_text";

        /**
         * Tertiary list item text.
         */
        public static final String TERTIARY_TEXT = "tertiary_text";

        /**
         * Meaningful time associated with this list item. Could be time received, sent, edited etc.
         */
        public static final String TIMESTAMP = "timestamp";

        /**
         * A bit vector of state information. This information is only interpretable by the data
         * owner. Examples of state information are read/unread status and priority status.
         */
        public static final String STATE = "state";

        /**
         * ID representing a group. Temporary work around until we can support 1:many group
         * mappings.
         */
        public static final String GROUP_ID = "group_id";

        /**
         * Full projection of available fields for a list item.
         */
        public static final String[] PROJ_FULL = {
            BaseColumns._ID,
            ACCOUNT_ID,
            MIME_TYPE,
            DUID,
            URI,
            PRIMARY_TEXT,
            SECONDARY_TEXT,
            TERTIARY_TEXT,
            TIMESTAMP,
            STATE,
            GROUP_ID
        };
    }
}
