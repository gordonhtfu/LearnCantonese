
package com.blackberry.widgets.tagview.internal.contact;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.text.TextUtils;

import com.blackberry.analytics.provider.AnalyticsContract;
import com.blackberry.analytics.provider.AnalyticsContract.AnalyticsContact;
import com.blackberry.analytics.recent.RecentContactContract;
import com.blackberry.widgets.tagview.contact.BaseContactTags;
import com.blackberry.widgets.tagview.contact.Contact;

import java.util.ArrayList;
import java.util.List;

/**
 * A helper class for querying contact providers
 */
public class ContactsHelper {
    /**
     * The context
     */
    private final Context mContext;
    /**
     * Whether or not we need to cancel the query.
     *
     * @see #isCancelled()
     * @see #cancel()
     */
    private boolean mCancelled = false;
    /**
     * The listener registered to be called when a contact has been matched
     *
     * @see #getOnContactMatched()
     * @see #setOnContactMatched(com.blackberry.widgets.tagview.internal.contact.ContactsHelper
     *      .OnContactMatched)
     */
    private OnContactMatched mOnContactMatched;
    /**
     * The listener registered to be called when an email address is required to
     * be tested for internal/external status.
     *
     * @see #getOnEmailAddressIsExternalListener()
     * @see #setOnEmailAddressIsExternalListener(com.blackberry.widgets.tagview.contact.BaseContactTags
     *      .OnEmailAddressIsExternalListener)
     */
    private BaseContactTags.OnEmailAddressIsExternalListener mOnEmailAddressIsExternalListener;
    /**
     * Whether or not to select the extra email addresses when querying a
     * contact.
     */
    private boolean mSelectExtraEmailAddresses;
    /**
     * Whether or not to select the extra phone numbers when querying a contact.
     */
    private boolean mSelectExtraPhoneNumbers;

    /**
     * The maximum number of contacts to return from a query.
     */
    private int mContactLimit = 20;

    /**
     * @param context The context
     */
    public ContactsHelper(Context context) {
        // Keep the Application Context as this object will be kept around on an
        // orientation change and we don't want to leak an Activity Context.
        mContext = context.getApplicationContext();
    }

    /**
     * @return Whether or not the query has been cancelled.
     * @see #cancel()
     */
    public boolean isCancelled() {
        return mCancelled;
    }

    /**
     * Cancel the current operation
     *
     * @see #isCancelled()
     */
    public void cancel() {
        mCancelled = true;
    }

    /**
     * @return The listener registered to be told when a contact has been
     *         matched.
     * @see #setOnContactMatched(com.blackberry.widgets.tagview.internal.contact.ContactsHelper
     *      .OnContactMatched)
     */
    public OnContactMatched getOnContactMatched() {
        return mOnContactMatched;
    }

    /**
     * Register a listener to be told when a contact has been matched. This MUST
     * be set to a non-null value before calling any methods on this object.
     *
     * @param onContactMatched The listener to register.
     * @see #getOnContactMatched()
     */
    public void setOnContactMatched(OnContactMatched onContactMatched) {
        mOnContactMatched = onContactMatched;
    }

    /**
     * @return The listener registered
     * @see #setOnEmailAddressIsExternalListener(com.blackberry.widgets.tagview.contact.BaseContactTags
     *      .OnEmailAddressIsExternalListener)
     */
    public BaseContactTags.OnEmailAddressIsExternalListener getOnEmailAddressIsExternalListener() {
        return mOnEmailAddressIsExternalListener;
    }

    /**
     * @param onEmailAddressIsExternalListener The listener to register
     * @see #getOnEmailAddressIsExternalListener()
     */
    public void setOnEmailAddressIsExternalListener(
            BaseContactTags.OnEmailAddressIsExternalListener
            onEmailAddressIsExternalListener) {
        mOnEmailAddressIsExternalListener = onEmailAddressIsExternalListener;
    }

    /**
     * Selecting extra email addresses means for every contact that has a match,
     * all email addresses will be queried and added to the Contact instead of
     * only those email addresses which match.
     * <p/>
     * For instance say there exists a contact named "John Doe" with email
     * addresses 'jdoe@foo.com' and 'iamcool@bar.com'. Searching for 'John' will
     * give a Contact object with both email addresses (since the name matches).
     * Searching for 'foo' will give a Contact with only jdoe@foo.com in the
     * email addresses list. Turning on this feature will also add
     * iamcool@bar.com to the list of email addresses even though it doesn't
     * match.
     *
     * @return Whether or not this class will select extra email addresses
     * @see #setSelectExtraEmailAddresses(boolean)
     */
    public boolean getSelectExtraEmailAddresses() {
        return mSelectExtraEmailAddresses;
    }

    /**
     * @param selectExtraEmailAddresses Whether or not to select extra
     *            (non-matching) email addresses.
     */
    public void setSelectExtraEmailAddresses(boolean selectExtraEmailAddresses) {
        mSelectExtraEmailAddresses = selectExtraEmailAddresses;
    }

    /**
     * Selecting extra phone numbers means for every contact that has a match,
     * all phone numbers will be queried and added to the Contact instead of
     * only those phone numbers which match.
     * <p/>
     * For instance say there exists a contact named "John Doe" with phone
     * numbers '519-555-1234' and '905-555-9876'. Searching for 'John' will give
     * a Contact object with both phone numbers (since the name matches).
     * Searching for '519' will give a Contact with only 519-555-1234 in the
     * phone numbers list. Turning on this feature will also add 905-555-9876 to
     * the list of phone numbers even though it doesn't match.
     *
     * @return Whether or not this class will select extra phone numbers
     * @see #setSelectExtraPhoneNumbers(boolean)
     */
    public boolean getSelectExtraPhoneNumbers() {
        return mSelectExtraPhoneNumbers;
    }

    /**
     * @param selectExtraPhoneNumbers Whether or not to select extra
     *            (non-matching) phone numbers.
     */
    public void setSelectExtraPhoneNumbers(boolean selectExtraPhoneNumbers) {
        mSelectExtraPhoneNumbers = selectExtraPhoneNumbers;
    }

    private Uri generateLimitedUri(Uri baseUri, String pattern, String limitKey) {
        Uri.Builder builder = baseUri
                .buildUpon()
                .appendPath(pattern)
                .appendQueryParameter(limitKey, String.valueOf(mContactLimit));
        return builder.build();
    }

    private Cursor matchEmailAddressesWithAnalytics(String pattern) {
        ContentResolver cr = mContext.getContentResolver();
        String[] PROJECTION = new String[] {
                RecentContactContract.RecentContact.CONTACT_ID,
                RecentContactContract.RecentContact.CONTACT_LOOKUP_KEY,
                RecentContactContract.RecentContact.DISPLAY_NAME,
                RecentContactContract.RecentContact.ADDRESS,
                RecentContactContract.RecentContact.ADDRESS_TYPE,
                RecentContactContract.RecentContact.ADDRESS_TYPE_LABEL,
                RecentContactContract.RecentContact.PHOTO_THUMBNAIL_URI
        };
        Uri filterUri = generateLimitedUri(
                RecentContactContract.RecentContact.RECENT_EMAIL_FILTER_URI,
                pattern.trim(), AnalyticsContract.PARAMETER_LIMIT);
        return cr.query(filterUri, PROJECTION, null, null, null);
    }

    private Cursor matchEmailAddressesWithAndroid(String pattern) {
        ContentResolver cr = mContext.getContentResolver();
        String[] PROJECTION;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            PROJECTION = new String[] {
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.LOOKUP_KEY,
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Email.DATA,
                    ContactsContract.CommonDataKinds.Email.TYPE,
                    ContactsContract.CommonDataKinds.Email.LABEL,
                    ContactsContract.Contacts.PHOTO_THUMBNAIL_URI
            };
        } else {
            PROJECTION = new String[] {
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.LOOKUP_KEY,
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Email.DATA,
                    ContactsContract.CommonDataKinds.Email.TYPE,
                    ContactsContract.CommonDataKinds.Email.LABEL
            };
        }
        Uri filterUri = generateLimitedUri(
                ContactsContract.CommonDataKinds.Email.CONTENT_FILTER_URI,
                pattern.trim(), ContactsContract.LIMIT_PARAM_KEY);
        return cr.query(filterUri, PROJECTION, null, null, null);
    }

    /**
     * Generate a list of contacts which match pattern based on the standard
     * android email filter.
     *
     * @param pattern The string pattern to match
     * @return A list of contacts which match pattern
     * @see android.provider.ContactsContract.CommonDataKinds.Email#CONTENT_FILTER_URI
     */
    public List<Contact> matchEmailAddresses(String pattern) {
        checkForListeners();
        ContactListBuilder contactListBuilder = new ContactListBuilder();
        Cursor cur = null;
        try {
            int lookupKeyIndex;
            int displayNameIndex;
            int photoIndex;
            int idIndex;
            int emailAddressIndex;
            int emailTypeIndex;
            int emailLabelIndex;
            cur = matchEmailAddressesWithAnalytics(pattern);
            if (cur != null) {
                lookupKeyIndex = cur
                        .getColumnIndex(RecentContactContract.RecentContact.CONTACT_LOOKUP_KEY);
                displayNameIndex = cur
                        .getColumnIndex(RecentContactContract.RecentContact.DISPLAY_NAME);
                photoIndex = cur
                        .getColumnIndex(RecentContactContract.RecentContact.PHOTO_THUMBNAIL_URI);
                idIndex = cur.getColumnIndex(RecentContactContract.RecentContact.CONTACT_ID);
                emailAddressIndex = cur.getColumnIndex(RecentContactContract.RecentContact.ADDRESS);
                emailTypeIndex = cur
                        .getColumnIndex(RecentContactContract.RecentContact.ADDRESS_TYPE);
                emailLabelIndex = cur
                        .getColumnIndex(RecentContactContract.RecentContact.ADDRESS_TYPE_LABEL);
            } else {
                cur = matchEmailAddressesWithAndroid(pattern);
                lookupKeyIndex = cur.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY);
                displayNameIndex = cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                photoIndex = cur.getColumnIndex(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI);
                idIndex = cur.getColumnIndex(ContactsContract.Contacts._ID);
                emailAddressIndex = cur.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA);
                emailTypeIndex = cur.getColumnIndex(ContactsContract.CommonDataKinds.Email.TYPE);
                emailLabelIndex = cur.getColumnIndex(ContactsContract.CommonDataKinds.Email.LABEL);
            }
            if (cur.getCount() > 0) {
                contactListBuilder.setCapacity(cur.getCount() + 1);
                Contact contact;
                Contact.EmailAddress emailAddress;
                while (cur.moveToNext()) {
                    if (isCancelled()) {
                        return null;
                    }
                    String lookupKey = cur.getString(lookupKeyIndex);
                    Contact.ContactDetails contactDetails = getContactDetails(contactListBuilder,
                            lookupKey, cur, lookupKeyIndex, displayNameIndex, photoIndex, idIndex);
                    emailAddress = new Contact.EmailAddress();
                    emailAddress.setValue(cur.getString(emailAddressIndex));
                    emailAddress.setType(cur.getInt(emailTypeIndex));
                    emailAddress.setLabel(cur.getString(emailLabelIndex));
                    contactDetails.getEmailAddresses().add(emailAddress);
                    contact = mOnContactMatched.onContactMatched(contactDetails, emailAddress);
                    contact.setContactsHelper(this);
                    if (getOnEmailAddressIsExternalListener() != null) {
                        emailAddress.setExternal(getOnEmailAddressIsExternalListener()
                                .isExternal(contact, emailAddress));
                    }
                    contactListBuilder.add(contact);
                    // get out once we have enough contacts.
                    if (contactListBuilder.getContacts().size() >= mContactLimit) {
                        break;
                    }
                }
            }
        } catch (SecurityException ex) {
        } finally {
            if (cur != null) {
                cur.close();
            }
        }

        // fillWithExtraContactData(contactListBuilder.getContacts());

        return contactListBuilder.getContacts();
    }

    private Cursor matchPhoneNumbersWithAnalytics(String pattern) {
        return null;
    }

    private Cursor matchPhoneNumbersWithAndroid(String pattern) {
        ContentResolver cr = mContext.getContentResolver();
        String[] PROJECTION;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            PROJECTION = new String[] {
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.LOOKUP_KEY,
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.DATA,
                    ContactsContract.CommonDataKinds.Phone.TYPE,
                    ContactsContract.CommonDataKinds.Phone.LABEL,
                    ContactsContract.Contacts.PHOTO_THUMBNAIL_URI
            };
        } else {
            PROJECTION = new String[] {
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.LOOKUP_KEY,
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.DATA,
                    ContactsContract.CommonDataKinds.Phone.TYPE,
                    ContactsContract.CommonDataKinds.Phone.LABEL
            };
        }
        Uri filterUri = generateLimitedUri(
                ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI,
                pattern.trim(), ContactsContract.LIMIT_PARAM_KEY);
        return cr.query(filterUri, PROJECTION, null, null, null);
    }

    /**
     * Generate a list of contacts which match pattern based on the standard
     * android phone filter.
     *
     * @param pattern The string pattern to match
     * @return A list of contacts which match pattern
     * @see android.provider.ContactsContract.CommonDataKinds.Phone#CONTENT_FILTER_URI
     */
    public List<Contact> matchPhoneNumbers(String pattern) {
        checkForListeners();
        ContactListBuilder contactListBuilder = new ContactListBuilder();
        Cursor cur = null;
        try {
            int lookupKeyIndex;
            int displayNameIndex;
            int photoIndex;
            int idIndex;
            int numberIndex;
            int numberTypeIndex;
            int numberLabelIndex;
            cur = matchPhoneNumbersWithAnalytics(pattern);
            if (cur != null) {
                lookupKeyIndex = cur
                        .getColumnIndex(RecentContactContract.RecentContact.CONTACT_LOOKUP_KEY);
                displayNameIndex = cur
                        .getColumnIndex(RecentContactContract.RecentContact.DISPLAY_NAME);
                photoIndex = cur
                        .getColumnIndex(RecentContactContract.RecentContact.PHOTO_THUMBNAIL_URI);
                idIndex = cur.getColumnIndex(RecentContactContract.RecentContact.CONTACT_ID);
                numberIndex = cur.getColumnIndex(RecentContactContract.RecentContact.ADDRESS);
                numberTypeIndex = cur
                        .getColumnIndex(RecentContactContract.RecentContact.ADDRESS_TYPE);
                numberLabelIndex = cur
                        .getColumnIndex(RecentContactContract.RecentContact.ADDRESS_TYPE_LABEL);
            } else {
                cur = matchPhoneNumbersWithAndroid(pattern);
                lookupKeyIndex = cur.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY);
                displayNameIndex = cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                photoIndex = cur.getColumnIndex(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI);
                idIndex = cur.getColumnIndex(ContactsContract.Contacts._ID);
                numberIndex = cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DATA);
                numberTypeIndex = cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE);
                numberLabelIndex = cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL);
            }
            if (cur.getCount() > 0) {
                contactListBuilder.setCapacity(cur.getCount() + 1);
                Contact contact;
                Contact.PhoneNumber phoneNumber;
                while (cur.moveToNext()) {
                    if (isCancelled()) {
                        return null;
                    }
                    String lookupKey = cur.getString(lookupKeyIndex);
                    Contact.ContactDetails contactDetails = getContactDetails(contactListBuilder,
                            lookupKey, cur, lookupKeyIndex, displayNameIndex, photoIndex, idIndex);
                    phoneNumber = new Contact.PhoneNumber();
                    phoneNumber.setValue(cur.getString(numberIndex));
                    phoneNumber.setType(cur.getInt(numberTypeIndex));
                    phoneNumber.setLabel(cur.getString(numberLabelIndex));
                    contactDetails.getPhoneNumbers().add(phoneNumber);
                    contact = mOnContactMatched.onContactMatched(contactDetails, phoneNumber);
                    contact.setContactsHelper(this);
                    contactListBuilder.add(contact);
                    // get out once we have enough contacts.
                    if (contactListBuilder.getContacts().size() >= mContactLimit) {
                        break;
                    }
                }
            }
        } catch (SecurityException ex) {
        } finally {
            if (cur != null) {
                cur.close();
            }
        }

        // fillWithExtraContactData(contactListBuilder.getContacts());

        return contactListBuilder.getContacts();
    }

    /**
     * Fetch details for a contact given a Uri from the standard Android
     * Contacts Content Provider.
     *
     * @param contactUri The contact Uri to fetch details of. MUST be from the
     *            standard Android Contacts Content Provider!.
     * @return The created contact or null if the contact cannot be found
     */
    public Contact fetchContact(Uri contactUri) {
        checkForListeners();

        String[] PROJECTION;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            PROJECTION = new String[] {
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.LOOKUP_KEY,
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.Contacts.PHOTO_THUMBNAIL_URI
            };
        } else {
            PROJECTION = new String[] {
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.LOOKUP_KEY,
                    ContactsContract.Contacts.DISPLAY_NAME
            };
        }
        Cursor cursor = mContext.getContentResolver().query(contactUri, PROJECTION, null, null,
                null);
        int lookupKeyIndex = cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY);
        int displayNameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
        int photoIndex = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI);
        int idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID);
        try {
            if (cursor.moveToFirst()) {
                Contact.ContactDetails contactDetails = new Contact.ContactDetails();
                fillContactDetails(cursor, lookupKeyIndex, displayNameIndex, photoIndex, idIndex,
                        contactDetails);
                // fillWithExtraContactData(contactDetails);
                Contact contact = mOnContactMatched.onContactMatched(contactDetails, null);
                contact.setContactsHelper(this);
                return contact;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * Create a contact based on an input string. For instance the input string
     * could be an email, phone number, etc. The Content Providers will be
     * queried to try to find an exact match for the given inputText. Currently
     * it looks to match based on email addresses and phone numbers.
     * <p/>
     * For a partial match see {@link #createContact(String)}.
     *
     * @param dataItem The text to be converted into a contact. Could be an
     *            email or a phone number.
     * @return A contact representing inputText
     */
    public Contact fetchContact(String dataItem) {
        // TODO: AVEN-1239- don't do a filter match, match exactly in this case
        return createContact(dataItem);
    }

    /**
     * Create a contact based on an input string. For instance the input string
     * could be an email, phone number, etc. The Content Providers will be
     * queried to try to find a partial match for the given inputText. Currently
     * it looks to match based on email addresses and phone numbers.
     * <p/>
     * For an exact match see {@link #fetchContact(String)}
     *
     * @param inputText The text to be converted into a contact
     * @return A contact representing inputText
     */
    public Contact createContact(String inputText) {
        checkForListeners();

        // TODO: This can be done SO much more efficiently...
        List<Contact> contacts = matchEmailAddresses(inputText);
        if (contacts.size() > 0) {
            return contacts.get(0);
        }
        contacts = matchPhoneNumbers(inputText);
        if (contacts.size() > 0) {
            return contacts.get(0);
        }

        Contact contact = mOnContactMatched.onContactMatched(inputText);
        contact.setContactsHelper(this);
        return contact;
    }

    /**
     * @param contacts The contacts to fill with extra data (phone numbers,
     *            email, etc).
     */
    private void fillWithExtraContactData(List<Contact> contacts) {
        List<Contact> uniqueContacts = new ArrayList<Contact>(contacts.size());

        for (Contact contact : contacts) {
            if (!uniqueContacts.contains(contact)) {
                uniqueContacts.add(contact);
            }
        }

        fillWithExtraEmailAddresses(uniqueContacts);
        fillWithExtraPhoneNumbers(uniqueContacts);
    }

    /**
     * @param contactDetails The contact details to fill with extra data (phone
     *            numbers, email, etc).
     */
    public void fillWithExtraContactData(Contact.ContactDetails contactDetails) {
        if (!TextUtils.isEmpty(contactDetails.getLookupKey())) {
            fillWithExtraEmailAddresses(contactDetails);
            fillWithExtraPhoneNumbers(contactDetails);
        }
    }

    /**
     * If {@link #mSelectExtraEmailAddresses} is true loop through contacts and
     * fetch all email addresses that aren't already in their list.
     *
     * @param contacts The contacts which were matched
     */
    private void fillWithExtraEmailAddresses(List<Contact> contacts) {
        if (mSelectExtraEmailAddresses) {
            ContentResolver cr = mContext.getContentResolver();
            String[] PROJECTION;
            PROJECTION = new String[] {
                    ContactsContract.Contacts._ID,
                    ContactsContract.CommonDataKinds.Email.DATA,
                    ContactsContract.CommonDataKinds.Email.TYPE,
                    ContactsContract.CommonDataKinds.Email.LABEL
            };
            String filter = ContactsContract.Contacts.LOOKUP_KEY + " = ?";
            String[] filterArgs = {
                    ""
            };
            Contact.EmailAddress emailAddress;
            for (Contact contact : contacts) {
                filterArgs[0] = contact.getLookupKey();
                Cursor cur = cr.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                        PROJECTION, filter, filterArgs, null);
                try {
                    if (cur.getCount() > 0) {
                        while (cur.moveToNext()) {
                            emailAddress = new Contact.EmailAddress();
                            emailAddress.setValue(cur.getString(cur.getColumnIndex(
                                    ContactsContract.CommonDataKinds.Email.DATA)));
                            emailAddress.setType(cur.getInt(cur.getColumnIndex(
                                    ContactsContract.CommonDataKinds.Email.TYPE)));
                            emailAddress.setLabel(cur.getString(cur.getColumnIndex(
                                    ContactsContract.CommonDataKinds.Email.LABEL)));
                            if (getOnEmailAddressIsExternalListener() != null) {
                                emailAddress.setExternal(getOnEmailAddressIsExternalListener()
                                        .isExternal(contact, emailAddress));
                            }
                            if (!contact.getEmailAddresses().contains(emailAddress)) {
                                contact.getEmailAddresses().add(emailAddress);
                            }
                        }
                    }
                } finally {
                    if (cur != null) {
                        cur.close();
                    }
                }
            }
        }
    }

    /**
     * If {@link #mSelectExtraEmailAddresses} is true fetch all email addresses
     * that aren't already in the contact's list.
     *
     * @param contactDetails The contact details to fill
     */
    private void fillWithExtraEmailAddresses(Contact.ContactDetails contactDetails) {
        if (mSelectExtraEmailAddresses) {
            ContentResolver cr = mContext.getContentResolver();
            String[] PROJECTION;
            PROJECTION = new String[] {
                    ContactsContract.Contacts._ID,
                    ContactsContract.CommonDataKinds.Email.DATA,
                    ContactsContract.CommonDataKinds.Email.TYPE,
                    ContactsContract.CommonDataKinds.Email.LABEL
            };
            String filter = ContactsContract.Contacts.LOOKUP_KEY + " = ?";
            String[] filterArgs = {
                    contactDetails.getLookupKey()
            };
            Contact.EmailAddress emailAddress;
            Cursor cur = cr.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                    PROJECTION, filter, filterArgs, null);
            try {
                if (cur.getCount() > 0) {
                    while (cur.moveToNext()) {
                        emailAddress = new Contact.EmailAddress();
                        emailAddress.setValue(cur.getString(cur.getColumnIndex(
                                ContactsContract.CommonDataKinds.Email.DATA)));
                        emailAddress.setType(cur.getInt(cur.getColumnIndex(
                                ContactsContract.CommonDataKinds.Email.TYPE)));
                        emailAddress.setLabel(cur.getString(cur.getColumnIndex(
                                ContactsContract.CommonDataKinds.Email.LABEL)));
                        if (getOnEmailAddressIsExternalListener() != null) {
                            emailAddress.setExternal(getOnEmailAddressIsExternalListener()
                                    .isExternal(new Contact(contactDetails), emailAddress));
                        }
                        if (!contactDetails.getEmailAddresses().contains(emailAddress)) {
                            contactDetails.getEmailAddresses().add(emailAddress);
                        }
                    }
                }
            } finally {
                if (cur != null) {
                    cur.close();
                }
            }
        }
    }

    /**
     * If {@link #mSelectExtraPhoneNumbers} is true loop through contacts and
     * fetch all phone numbers that aren't already in their list.
     *
     * @param contacts The contacts which were matched
     */
    private void fillWithExtraPhoneNumbers(List<Contact> contacts) {
        if (mSelectExtraPhoneNumbers) {
            ContentResolver cr = mContext.getContentResolver();
            String[] PROJECTION;
            PROJECTION = new String[] {
                    ContactsContract.Contacts._ID,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.TYPE,
                    ContactsContract.CommonDataKinds.Phone.LABEL
            };
            String filter = ContactsContract.Contacts.LOOKUP_KEY + " = ?";
            String[] filterArgs = {
                    ""
            };
            Contact.PhoneNumber phoneNumber;
            for (Contact contact : contacts) {
                filterArgs[0] = contact.getLookupKey();
                Cursor cur = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        PROJECTION, filter, filterArgs, null);
                try {
                    if (cur.getCount() > 0) {
                        while (cur.moveToNext()) {
                            phoneNumber = new Contact.PhoneNumber();
                            phoneNumber.setValue(cur.getString(cur.getColumnIndex(
                                    ContactsContract.CommonDataKinds.Phone.NUMBER)));
                            phoneNumber.setType(cur.getInt(cur.getColumnIndex(
                                    ContactsContract.CommonDataKinds.Phone.TYPE)));
                            phoneNumber.setLabel(cur.getString(cur.getColumnIndex(
                                    ContactsContract.CommonDataKinds.Phone.LABEL)));
                            if (!contact.getPhoneNumbers().contains(phoneNumber)) {
                                contact.getPhoneNumbers().add(phoneNumber);
                            }
                        }
                    }
                } finally {
                    if (cur != null) {
                        cur.close();
                    }
                }
            }
        }
    }

    /**
     * If {@link #mSelectExtraPhoneNumbers} is true fetch all phone numbers that
     * aren't already in the contact's list.
     *
     * @param contactDetails The contact details to fill
     */
    private void fillWithExtraPhoneNumbers(Contact.ContactDetails contactDetails) {
        if (mSelectExtraPhoneNumbers) {
            ContentResolver cr = mContext.getContentResolver();
            String[] PROJECTION;
            PROJECTION = new String[] {
                    ContactsContract.Contacts._ID,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.TYPE,
                    ContactsContract.CommonDataKinds.Phone.LABEL
            };
            String filter = ContactsContract.Contacts.LOOKUP_KEY + " = ?";
            String[] filterArgs = {
                    contactDetails.getLookupKey()
            };
            Contact.PhoneNumber phoneNumber;
            Cursor cur = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    PROJECTION, filter, filterArgs, null);
            try {
                if (cur.getCount() > 0) {
                    while (cur.moveToNext()) {
                        phoneNumber = new Contact.PhoneNumber();
                        phoneNumber.setValue(cur.getString(cur.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.NUMBER)));
                        phoneNumber.setType(cur.getInt(cur.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.TYPE)));
                        phoneNumber.setLabel(cur.getString(cur.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.LABEL)));
                        if (!contactDetails.getPhoneNumbers().contains(phoneNumber)) {
                            contactDetails.getPhoneNumbers().add(phoneNumber);
                        }
                    }
                }
            } finally {
                if (cur != null) {
                    cur.close();
                }
            }
        }
    }

    /**
     * @param contactListBuilder The builder to use as cache
     * @param lookupKey The lookup key for the contact
     * @param cursor The cursor containing the data. It should be pre-positioned
     *            at the row to read.
     * @return The ContactDetails for the contact
     */
    private Contact.ContactDetails getContactDetails(ContactListBuilder contactListBuilder,
            String lookupKey, Cursor cursor, int lookupKeyIndex, int displayNameIndex,
            int photoIndex, int idIndex) {
        Contact.ContactDetails contactDetails = contactListBuilder.findContactDetails
                (lookupKey);
        if (TextUtils.isEmpty(contactDetails.getLookupKey())) {
            fillContactDetails(cursor, lookupKeyIndex, displayNameIndex, photoIndex, idIndex,
                    contactDetails);
        }
        return contactDetails;
    }

    /**
     * @param cursor The cursor containing the data. It should be pre-positioned
     *            at the row to read.
     * @param contactDetails The
     *            {@link com.blackberry.widgets.tagview.contact.Contact.ContactDetails}
     *            to fill with data.
     */
    private void fillContactDetails(Cursor cursor, int lookupKeyIndex, int displayNameIndex,
            int photoIndex, int idIndex, Contact.ContactDetails contactDetails) {
        contactDetails.setLookupKey(cursor.getString(lookupKeyIndex));
        contactDetails.setName(cursor.getString(displayNameIndex));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            String s = cursor.getString(photoIndex);
            if (s != null) {
                contactDetails.setPhotoUri(s);
            }
        } else if (idIndex >= 0) {
            contactDetails.setPhotoUri(getPhotoUriFromContactId(cursor.getLong(idIndex)));
        }
    }

    /**
     * @param contactId The contact Id to query
     * @return The Uri pointing to the photo to use for the contact
     */
    private Uri getPhotoUriFromContactId(long contactId) {
        Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI,
                contactId);
        return Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
    }

    /**
     * Check if the listener is registered. If not, throw an exception as it is
     * required.
     */
    private void checkForListeners() {
        if (mOnContactMatched == null) {
            throw new IllegalStateException("An OnContactMatched listener MUST be registered");
        }
    }

    /**
     * An interface used as a callback when a contact has been matched
     */
    public interface OnContactMatched {
        /**
         * Called when one of the methods of this class has matched a contact.
         *
         * @param contactDetails The
         *            {@link com.blackberry.widgets.tagview.contact.Contact.ContactDetails}
         *            for the matched contact.
         * @param matchedDataItem The
         *            {@link com.blackberry.widgets.tagview.contact.Contact.ContactDataItem}
         *            for the matched contact. This may be NULL.
         * @return A newly created {@link Contact} representing the information
         *         passed in
         */
        Contact onContactMatched(Contact.ContactDetails contactDetails,
                Contact.ContactDataItem matchedDataItem);

        /**
         * This method is called when a Contact is requested to be created with
         * the only input being a string.
         *
         * @param inputText The text to be converted into a contact
         * @return A contact representing inputText.
         */
        Contact onContactMatched(String inputText);
    }
}
