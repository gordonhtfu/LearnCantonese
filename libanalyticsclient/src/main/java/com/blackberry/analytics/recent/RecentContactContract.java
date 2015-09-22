package com.blackberry.analytics.recent;

import android.net.Uri;
import android.provider.BaseColumns;

import com.blackberry.analytics.provider.AnalyticsContract;
import com.blackberry.analytics.provider.AnalyticsContract.AnalyticsContact.AddressCategory;

/**
 * Defines the contract for recent contacts, which include contacts referenced
 * in messages sent from or received by accounts in the system as well as
 * contacts in the Android contacts content provider.
 */
public class RecentContactContract {

    /**
     * The contract for recent contacts.
     */
    public static final class RecentContact implements BaseColumns {

        public static final String URI_SUFFIX = "contact";
        public static final String RECENT_SUFFIX = "recent";
        public static final String FILTER_SUFFIX = "filter";
        public static final String LOOKUP_SUFFIX = "lookup";

        /**
         * The content:// style URL for recent contact lookup using a filter.
         * The filter is applied to display names as well as email addresses.
         * The filter argument should be passed as an additional path segment
         * after this URI.
         * <p>
         * The query in the following example will return
         * "Robert Parr (bob@incredibles.com)" as well as
         * "Bob Parr (incredible@android.com)".
         * 
         * <pre>
         * Uri uri = Uri.withAppendedPath(RecentContact.RECENT_EMAIL_FILTER_URI,
         *         Uri.encode(&quot;bob&quot;));
         * Cursor c = getContentResolver().query(uri,
         *         new String[] { RecentContact.DISPLAY_NAME, RecentContact.ADDRESS },
         *         null, null, null);
         * </pre>
         * 
         * </p>
         */
        // TODO review the authority; for now we're using the analytics
        // authority but it might be more appropriate to use an authority
        // related to the contacts domain
        public static final Uri RECENT_EMAIL_FILTER_URI = Uri.parse("content://"
                + AnalyticsContract.AUTHORITY + "/"
                + URI_SUFFIX + "/"
                + RECENT_SUFFIX + "/"
                + FILTER_SUFFIX + "/"
                + AddressCategory.EMAIL);

        /**
         * The content:// style URI for recent contact lookup using an email
         * address. Append the email address you want to lookup to this URI and
         * query it to perform a lookup.
         * 
         * <pre>
         * Uri lookupUri = Uri.withAppendedPath(RecentContact.RECENT_EMAIL_LOOKUP_URI,
         *         Uri.encode(emailAddress));
         * </pre>
         */
        // TODO review the authority; for now we're using the analytics
        // authority but it might be more appropriate to use an authority
        // related to the contacts domain
        public static final Uri RECENT_EMAIL_LOOKUP_URI = Uri
                .parse("content://"
                        + AnalyticsContract.AUTHORITY + "/"
                        + URI_SUFFIX + "/"
                        + RECENT_SUFFIX + "/"
                        + LOOKUP_SUFFIX + "/"
                        + AddressCategory.EMAIL);

        /**
         * The display name for the contact.
         * <p>
         * TYPE: STRING
         * </p>
         */
        public static final String DISPLAY_NAME = "display_name";

        /**
         * The address data, for example an email address or a phone number.
         * <p>
         * TYPE: STRING
         * </p>
         */
        public static final String ADDRESS = "address";

        /**
         * The address catgory, such as {@link AddressCategory#EMAIL} or
         * {@link AddressCategory#PHONE}.
         * <p>
         * TYPE: STRING
         * </p>
         */
        public static final String ADDRESS_CATEGORY = "address_category";

        /**
         * Optional type of the address, for example
         * {@link android.provider.ContactsContract.CommonDataKinds.Email#TYPE_HOME}
         * or
         * {@link android.provider.ContactsContract.CommonDataKinds.Email#TYPE_WORK}
         * .
         * <p>
         * TYPE: INT
         * </p>
         */
        public static final String ADDRESS_TYPE = "address_type";

        /**
         * Optional label used to display in the UI when the address type is
         * {@link android.provider.ContactsContract.CommonDataKinds.BaseTypes#TYPE_CUSTOM}
         * .
         * <p>
         * TYPE: STRING
         * </p>
         */
        public static final String ADDRESS_TYPE_LABEL = "address_type_label";

        /**
         * Optional URI of the thumbnail-sized photo, if any.
         * <p>
         * TYPE: STRING
         * </p>
         */
        public static final String PHOTO_THUMBNAIL_URI = "photo_uri";

        /**
         * Optional lookup key for the contact in the Android contact content
         * provider, if any.
         * <p>
         * TYPE: STRING
         * </p>
         */
        public static final String CONTACT_LOOKUP_KEY = "contact_lookup_key";

        /**
         * Optional ID for the contact in the Android contact content provider,
         * if any.
         * <p>
         * TYPE: LONG
         * </p>
         */
        public static final String CONTACT_ID = "contact_id";

        /**
         * Optional ID for the contact address in the Android contact content
         * provider, if any.
         * <p>
         * TYPE: LONG
         * </p>
         */
        public static final String ADDRESS_ID = "address_id";

        /**
         * Optional type of data used to produce the display name for a contact,
         * defined in
         * {@link android.provider.ContactsContract.DisplayNameSources}, if any.
         * <p>
         * TYPE: INT
         * </p>
         */
        public static final String DISPLAY_NAME_SOURCE = "display_name_source";

        /**
         * Hidden flag - is the recent contact hidden or visible. 1=hidden, 0=visible.
         * <P>
         * Type: INT (boolean)
         * </P>
         */
        public static final String HIDDEN = "hidden";

        /**
         * The time stamp when the contact was most recently referenced in
         * an email.
         * <P>
         * Type: INT
         * </P>
         */
        public static final String TIMESTAMP = "timestamp";

        /**
         * The frecency score for this contact.
         * <p>
         * TYPE: INT
         * </p>
         */
        public static final String SCORE = "frecency_score";

        /**
         * This utility class cannot be instantiated.
         */
        private RecentContact() {
        }
    }
}
