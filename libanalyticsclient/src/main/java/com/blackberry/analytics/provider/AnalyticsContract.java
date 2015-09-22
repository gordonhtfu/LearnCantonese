
package com.blackberry.analytics.provider;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import com.blackberry.analytics.intent.AnalyticsIntent;
import com.blackberry.analytics.provider.AnalyticsContract.AnalyticsContact.AddressCategory;
import com.blackberry.analytics.provider.AnalyticsContract.AnalyticsContact.SortOrder;

/**
 * Defines contants that help applications work with the analytics content URIs,
 * column names, projections, etc.
 */
public final class AnalyticsContract {

    public static final String AUTHORITY = "com.blackberry.analytics";
    public static final String NOTIFY_AUTHORITY = "com.blackberry.analytics.notifier";

    public static final String RECENT_SUFFIX = "recent";

    public static final String PARAMETER_LIMIT = "limit";

    /**
     * The contract for contacts in analytics.
     */
    public static final class AnalyticsContact implements BaseColumns {
        public static final String URI_SUFFIX = "contact";
        public static final Uri CONTENT_URI = Uri
                .parse("content://" + AUTHORITY + "/" + URI_SUFFIX);
        public static final Uri CONTENT_NOTIFIER_URI = Uri.parse("content://"
                + NOTIFY_AUTHORITY
                + "/" + URI_SUFFIX);
        public static final Uri RECENT_URI = Uri.parse("content://" + AUTHORITY
                + "/"
                + URI_SUFFIX + "/" + RECENT_SUFFIX);

        /**
         * The display name for the contact. May be null.
         * <p>
         * TYPE: TEXT
         * </p>
         */
        public static final String DISPLAY_NAME = "display_name";

        /**
         * The address data, for example an email address or a phone number.
         * Used as the key to match contacts in the Android contact content
         * provider. Must not be null.
         * <p>
         * TYPE: TEXT
         * </p>
         */
        public static final String ADDRESS = "address";

        /**
         * The address category, such as {@link AddressCategory#EMAIL} or
         * {@link AddressCategory#PHONE}. Must not be null.
         * <p>
         * TYPE: TEXT
         * </p>
         */
        public static final String ADDRESS_CATEGORY = "address_category";

        /**
         * The entity URI of the real contact in the contacts content provider.
         * Will be null if this is a pseudo contact.
         * <p>
         * TYPE: TEXT
         * </p>
         */
        public static final String URI = "uri";

        public static final String[] DEFAULT_PROJECTION = {
                DISPLAY_NAME,
                ADDRESS,
                ADDRESS_CATEGORY,
        };

        /**
         * The address category constants.
         */
        public static final class AddressCategory {
            public static final String EMAIL = "email";
            public static final String PHONE = "phone";
            public static final String PIN = "pin";

            private AddressCategory() {
                // This utility class cannot be instantiated.
            }
        }

        /**
         * The sort order constants for analytics contacts.
         */
        public static final class SortOrder {
            public static final String FRECENCY = ComponentUse.SCORE + " DESC";

            private SortOrder() {
                // This utility class cannot be instantiated
            }
        }

        /**
         * Prevents instance creation.
         */
        private AnalyticsContact() {
            // cannot be instantiated
        }
    }

    /**
     * Contract for Component Use in Analytics.
     */
    public static final class ComponentUse implements BaseColumns {
        public static final String URI_SUFFIX = "componentuse";
        public static final Uri CONTENT_URI = Uri
                .parse("content://" + AUTHORITY + "/" + URI_SUFFIX);
        public static final Uri CONTENT_NOTIFIER_URI = Uri.parse("content://" + NOTIFY_AUTHORITY
                + "/"
                + URI_SUFFIX);

        /**
         * Used to combine or split multiple contact ids.
         */
        public static final String CONTACT_ID_SEPARATOR = ";";

        /**
         * Value used to indicate that there is no contact for a component use.
         */
        public static final long CONTACT_ID_NONE = -1;

        /**
         * Value used to indicate that there is no account for a component use.
         */
        public static final long ACCOUNT_ID_NONE = -1;

        /**
         * The action column.
         * <p>
         * TYPE: TEXT
         * </p>
         */
        public static final String ACTION = "action";

        /**
         * The mime type column.
         * <p>
         * TYPE: TEXT
         * </p>
         */
        public static final String MIME_TYPE = "mime_type";

        /**
         * The activity component name column. Made up of "package/class name".
         * <p>
         * TYPE: TEXT
         * </p>
         */
        public static final String COMPONENT = "component";

        /**
         * The activity account id which is the id of the account in the PIM
         * AccountProvider. {@link #ACCOUNT_ID_NONE} if there is no account.
         * <p>
         * TYPE: INTEGER
         * </p>
         */
        public static final String ACCOUNT_ID = "account_id";

        /**
         * The activity contact id which is the id of the contact in the
         * analytics contact table. -1 if no contact.
         * <p>
         * TYPE: INTEGER
         * </p>
         */
        public static final String CONTACT_ID = "contact_id";

        /**
         * The display name for the contact. May be null. Contact name, address
         * and category should all exist or all be null.
         * <p>
         * TYPE: TEXT
         * </p>
         */
        public static final String CONTACT_NAME = "contact_name";

        /**
         * The address data, for example an email address or a phone number.
         * Used as the key to match contacts in the Android contact content
         * provider. May be null. Contact name, address and category should all
         * exist or all be null.
         * <p>
         * TYPE: TEXT
         * </p>
         */
        public static final String CONTACT_ADDRESS = "contact_address";

        /**
         * The contact address category, such as {@link AddressCategory#EMAIL}
         * or {@link AddressCategory#PHONE}. May be null. Contact name, address
         * and category should all exist or all be null.
         * <p>
         * TYPE: TEXT
         * </p>
         */
        public static final String CONTACT_CATEGORY = "contact_category";

        /**
         * The contact thumbnail photo uri from the official contact. May be
         * null. Column may not exist in results if no photo URIs needed or
         * available for a given query.
         * <p>
         * TYPE: TEXT
         * </p>
         */
        public static final String PHOTO_THUMBNAIL_URI = "photo_thumbnail_uri";

        /**
         * The weighted score for this {component, account, contact} within the
         * {action, mimeType}.
         * <p>
         * TYPE: INTEGER
         * </p>
         */
        public static final String SCORE = "frecency_score";

        /**
         * List of columns to retrieve in the query.
         */
        public static final String[] DEFAULT_PROJECTION = {
                COMPONENT, ACCOUNT_ID, CONTACT_NAME, CONTACT_ADDRESS,
                CONTACT_CATEGORY, PHOTO_THUMBNAIL_URI, SCORE
        };

        /**
         * Where clause.
         */
        public static final String KEY_SELECTION = String.format("%s = ? and %s = ?", ACTION,
                MIME_TYPE);
        public static final int ACTION_INDEX = 0;
        public static final int MIME_TYPE_INDEX = 1;

        /**
         * Return the frecency score data for an action and mime type.
         * 
         * @param context the current application context
         * @param action the user action desired (such as Intent.ACTION_SEND)
         * @param mimeType the type of data being acted upon (such as "image/*")
         * @return the cursor containing the frecency score data
         */
        public static Cursor getScores(Context context, String action, String mimeType) {
            final ContentResolver resolver = context.getContentResolver();
            final String[] selectionArgs = {
                    action, mimeType
            };
            return resolver.query(CONTENT_URI, DEFAULT_PROJECTION, KEY_SELECTION, selectionArgs,
                    SortOrder.FRECENCY);
        }

        /**
         * Send a broadcast to record the choice rather than direct calls above
         * to content provider. Database updating is done asynchronously. The
         * client call will complete very quickly.
         * 
         * @param context application context for sending broadcast
         * @param action the action for which choice is being recorded
         * @param mimeType the data type for which choice is being recorded
         * @param component the application activity the user chose
         */
        public static void sendBroadcastComponentUse(Context context, String action,
                String mimeType, String component) {
            Intent intent = new Intent();
            intent.setAction(AnalyticsIntent.RECORD_ACTION);
            final String[] componentUseData = {
                    action, mimeType, component
            };
            intent.putExtra("componentUseData", componentUseData);

            // Allow this intent to activate apps when they are first installed
            // or after they have been force stopped (e.g., analytics)
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);

            context.sendBroadcast(intent);
        }

        /**
         * Send a broadcast to record additional information - account and/or
         * contacts - about the choice previously made. Database updating is
         * done asynchronously. The client call will complete very quickly.
         * 
         * @param context application context for sending broadcast
         * @param action the action for which choice is being recorded
         * @param mimeType the data type for which choice is being recorded
         * @param component the application activity the user chose
         * @param accountId the ID of the account being used by this activity.
         *            May be {@link ComponentUse#ACCOUNT_ID_NONE} if there is no
         *            account.
         */
        public static void sendBroadcastComponentUse(Context context, String action,
                String mimeType, String component, long accountId) {
            Intent intent = new Intent();
            intent.setAction(AnalyticsIntent.RECORD_ACTION);
            final String[] componentUseData = {
                    action, mimeType, component
            };
            intent.putExtra("componentUseData", componentUseData);
            intent.putExtra("account", accountId);

            // Allow this intent to activate apps when they are first installed
            // or after they have been force stopped (e.g., analytics)
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);

            context.sendBroadcast(intent);
        }

        private ComponentUse() {
            // cannot be instantiated
        }
    }

    private AnalyticsContract() {
        // This utility class cannot be instantiated
    }

    /**
     * Contract for user created message prioritization rules in Analytics
     */
    public static final class UserCreatedRule implements BaseColumns {
        public static final String URI_SUFFIX = "usercreatedrule";
        public static final Uri CONTENT_URI = Uri.parse("content://"
                + AUTHORITY + "/" + URI_SUFFIX);
        public static final Uri CONTENT_NOTIFIER_URI = Uri.parse("content://"
                + NOTIFY_AUTHORITY + "/" + URI_SUFFIX);

        /**
         * The name of the rule as set by the User
         * <p>
         * TYPE: TEXT
         * </p>
         */
        public static final String NAME = "name";

        /**
         * Flag indicating whether rule is enabled
         * <p>
         * TYPE: INTEGER
         * </p>
         */
        public static final String ENABLED = "enabled";

        /**
         * Unique identifier for the account the rule applies too.
         * <p>
         * TYPE: INTEGER
         * </p>
         */
        public static final String ACCOUNT_ID = "account_id";
        
        /**
         * User readable account name
         * <p>
         * TYPE: TEXT
         * </p>
         */
        public static final String ACCOUNT_NAME = "account_name";

        /**
         * Flag indicating whether the rule is visible.
         * <p>
         * TYPE: INTEGER
         * </p>
         */
        public static final String VISIBLE = "visible";

        /**
         * Flag indicating whether the rule is a Level 1 rule
         * <p>
         * TYPE: INTEGER
         * </p>
         */
        public static final String IS_LEVEL_1 = "is_level_1";

        /**
         * Address of the sender the rule applies to. May be null.
         * <p>
         * TYPE: TEXT
         * </p>
         */
        public static final String SENDER = "sender";

        /**
         * Address of the recipient the rule applies to. May be null.
         * <p>
         * TYPE: TEXT
         * </p>
         */
        public static final String RECIPIENT = "recipient";

        /**
         * String to match against the subject of the message. May be null
         * <p>
         * TYPE: TEXT
         * </p>
         */
        public static final String SUBJECT = "subject";

        /**
         * Flag representing the importance the rule will match against. May be
         * null.
         * <p>
         * TYPE: INTEGER
         * </p>
         */
        public static final String IMPORTANCE = "importance";

        /**
         * Flag indicating whether rule applies to messages sent directly to the
         * recipient. May be null.
         * <p>
         * TYPE: INTEGER
         * </p>
         */
        public static final String SENT_DIRECTLY_TO_ME = "sent_directly_to_me";

        /**
         * Flag indicating whether rule applies to messages where user was cc'd.
         * May be null.
         * <p>
         * TYPE: INTEGER
         * </p>
         */
        public static final String CC_TO_ME = "cc_to_me";

        /**
         * Flag indicating whether rule applies to enterprise messages. May be
         * null.
         * <p>
         * TYPE: INTEGER
         * </p>
         */
        public static final String ENTERPRISE = "enterprise";

        /**
         * List of columns to retrieve in the default query.
         */
        public static final String[] DEFAULT_PROJECTION = {
                NAME, ENABLED, ACCOUNT_ID, ACCOUNT_NAME, VISIBLE, IS_LEVEL_1,
                SENDER, RECIPIENT, SUBJECT, IMPORTANCE, SENT_DIRECTLY_TO_ME,
                CC_TO_ME, ENTERPRISE
        };

        /**
         * Default id project with name and account id
         */
        public static final String[] ID_PROJECTION = {
                BaseColumns._ID, NAME, ACCOUNT_ID, ACCOUNT_NAME
        };
        
        /**
         * List of columns to retrieve in the default query.
         */
        public static final String[] SENDER_PROJECTION = {
                NAME, ACCOUNT_ID, ACCOUNT_NAME, SENDER
        };
        
        /**
         * List of columns to retrieve.
         */
        public static final String[] NAME_PROJECTION = {
                NAME, ACCOUNT_ID, ACCOUNT_NAME, ENABLED
        };
        
        /**
         * List of columns to retrieve including _ID.
         */
        public static final String[] ID_NAME_PROJECTION = {
                BaseColumns._ID, NAME, ACCOUNT_ID, ACCOUNT_NAME, ENABLED
        };
        
        /**
         * Where clause.
         */
        public static final String RECIPIENT_SELECTION = String.format("%s = ?", RECIPIENT);
        public static final int RECIPIENT_INDEX = 0;
        public static final String NAME_SELECTION = String.format("%s = ?", NAME);
        public static final String SENDER_SELECTION = String.format("%s = ? AND %s = ? AND %s = ? AND %s = ?", NAME, ACCOUNT_ID, 
                ACCOUNT_NAME, SENDER);

        private UserCreatedRule() {
            // cannot be instantiated
        }
    }
}
