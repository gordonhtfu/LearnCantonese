
package com.blackberry.widgets.tagview.contact;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;

import com.blackberry.widgets.R;
import com.blackberry.widgets.tagview.BaseTag;
import com.blackberry.widgets.tagview.internal.contact.ContactsHelper;

import java.util.ArrayList;

/**
 * A class wrapped around Android's Contact data
 * <p/>
 * The idea of this class is an internal data object can be shared among many
 * Contact objects. This allows subclasses to have their own individual fields
 * (ie activeEmailAddress) but keep the same synchronized copy of the Contact's
 * data.
 */
public class Contact {
    // TODO: Fix the OnObjectChanged signals.
    // Currently only the instance that has setXYZ called on it is notified of
    // the change. The
    // ContactDetails should notify all encapsulating instances that it has
    // changed.

    /**
     * The ContactDetails shared amongst all matching Contact instances
     */
    private ContactDetails mDetails;

    /**
     * Default constructor
     */
    public Contact() {
        mDetails = new ContactDetails();
    }

    /**
     * Create a Contact linked to an existing set of contact details.
     *
     * @param contactDetails The
     *            {@link com.blackberry.widgets.tagview.contact.Contact .ContactDetails}
     *            to be linked to
     */
    public Contact(ContactDetails contactDetails) {
        this.mDetails = contactDetails;
    }

    /**
     * @return The
     *         {@link com.blackberry.widgets.tagview.contact.Contact.ContactDetails}
     *         for this instance
     * @see #setContactDetails(com.blackberry.widgets.tagview.contact.Contact.ContactDetails)
     */
    public ContactDetails getContactDetails() {
        return mDetails;
    }

    /**
     * @param contactDetails The details to set
     * @see #getContactDetails()
     */
    public void setContactDetails(ContactDetails contactDetails) {
        mDetails = contactDetails;
        // notifyOnObjectChangedListener();
    }

    /**
     * @return The lookup key for the contact. This is the unique identifier for
     *         the contact
     * @see #setLookupKey(String)
     */
    public String getLookupKey() {
        return mDetails.getLookupKey();
    }

    /**
     * Set the lookup key
     *
     * @param key The key to set
     * @see #getLookupKey()
     */
    public void setLookupKey(String key) {
        mDetails.setLookupKey(key);
        // notifyOnObjectChangedListener();
    }

    /**
     * @return The name of the contact
     * @see #setName(String)
     */
    public String getName() {
        return mDetails.getName();
    }

    /**
     * Set the name of the contact
     *
     * @param name The new name of the contact
     * @see #getName()
     */
    public void setName(String name) {
        mDetails.setName(name);
        // notifyOnObjectChangedListener();
    }

    /**
     * @return The photo Uri. If it isn't set null is returned
     * @see #setPhotoUri(android.net.Uri)
     * @see #setPhotoUri(String)
     * @see #getPhotoUri(Context context)
     */
    public Uri getPhotoUri() {
        return mDetails.getPhotoUri();
    }

    /**
     * @param context The context
     * @return The photo Uri. If it isn't set a default contact picture is
     *         returned.
     * @see #setPhotoUri(android.net.Uri)
     * @see #setPhotoUri(String)
     * @see #getPhotoUri()
     */
    public Uri getPhotoUriOrDefault(Context context) {
        Uri result = mDetails.getPhotoUri();
        if (result == null) {
            result = Uri.withAppendedPath(
                    Uri.parse("android.resource://" + context.getPackageName()),
                    String.valueOf(R.drawable.ic_contact_picture));
        }
        return result;
    }

    /**
     * @param photoUri The new photo Uri
     * @see #getPhotoUri()
     * @see #setPhotoUri(String)
     */
    public void setPhotoUri(Uri photoUri) {
        mDetails.setPhotoUri(photoUri);
        // notifyOnObjectChangedListener();
    }

    /**
     * @param photoUri The new photo Uri
     * @see #getPhotoUri()
     * @see #setPhotoUri(android.net.Uri)
     */
    public void setPhotoUri(String photoUri) {
        setPhotoUri(Uri.parse(photoUri));
    }

    /**
     * @return The string to use for displaying this contact
     */
    public String getLabel() {
        String result = mDetails.getLabel();
        if (!TextUtils.isEmpty(result)) {
            return result;
        }

        return "";
    }

    /**
     * @return The list of email addresses for this contact
     * @see #setEmailAddresses(java.util.ArrayList)
     */
    public ArrayList<EmailAddress> getEmailAddresses() {
        mDetails.lazyLoadData();
        return mDetails.getEmailAddresses();
    }

    /**
     * Set the list of email addresses for this contact
     *
     * @param emailAddresses The new list of email addresses for this contact
     * @see #getEmailAddresses()
     */
    public void setEmailAddresses(ArrayList<EmailAddress> emailAddresses) {
        mDetails.setEmailAddresses(emailAddresses);
        // notifyOnObjectChangedListener();
    }

    /**
     * @return The list of phone numbers for this contact
     * @see #setPhoneNumbers(java.util.ArrayList)
     */
    public ArrayList<PhoneNumber> getPhoneNumbers() {
        mDetails.lazyLoadData();
        return mDetails.getPhoneNumbers();
    }

    /**
     * Set the list of phone numbers for this contact
     *
     * @param phoneNumbers The new list of phone numbers for this contact
     * @see #getPhoneNumbers()
     */
    public void setPhoneNumbers(ArrayList<PhoneNumber> phoneNumbers) {
        mDetails.setPhoneNumbers(phoneNumbers);
        // notifyOnObjectChangedListener();
    }

    @Override
    public String toString() {
        return getLabel();
    }

    /**
     * @return True if the Contact is a valid contact in the Android system.
     *         This is determined by if there is a
     *         {@link com.blackberry.widgets.tagview.contact.Contact .ContactDetails#getLookupKey()}
     *         available or not.
     */
    public boolean isContactValid() {
        return !TextUtils.isEmpty(mDetails.getLookupKey());
    }

    /**
     * @return Whether or not this Contact is valid or not. Subclasses can
     *         override this to provide their own validity logic.
     */
    public boolean isValid() {
        return isContactValid();
    }

    /**
     * @return The
     *         {@link com.blackberry.widgets.tagview.internal.contact.ContactsHelper}
     *         which created this instance
     * @see #setContactsHelper(com.blackberry.widgets.tagview.internal.contact.ContactsHelper)
     */
    public ContactsHelper getContactsHelper() {
        return mDetails.getContactsHelper();
    }

    /**
     * @param contactsHelper The
     *            {@link com.blackberry.widgets.tagview.internal.contact .ContactsHelper}
     *            which created this instance
     * @see #getContactsHelper()
     */
    public void setContactsHelper(ContactsHelper contactsHelper) {
        mDetails.setContactsHelper(contactsHelper);
    }

    /**
     * The data item holding Contact information
     */
    public static abstract class ContactDataItem {
        /**
         * The value
         *
         * @see #getValue()
         * @see #setValue(String)
         */
        private String mValue;
        /**
         * The type of the value
         *
         * @see #getType()
         * @see #setType(int)
         */
        private int mType = -1;
        /**
         * The label
         *
         * @see #getLabel()
         * @see #setLabel(String)
         */
        private String mLabel;

        /**
         * Default constructor
         */
        public ContactDataItem() {
        }

        /**
         * @param value The value
         */
        public ContactDataItem(String value) {
            mValue = value;
        }

        /**
         * @return The value of the data item.
         * @see #setValue(String)
         */
        public String getValue() {
            return mValue;
        }

        /**
         * Set the value
         *
         * @param value The new value of this data item
         * @see #getValue()
         */
        public void setValue(String value) {
            mValue = value;
        }

        /**
         * @return The type of data.
         * @see #setType(int)
         */
        public int getType() {
            return mType;
        }

        /**
         * Set the type of data
         *
         * @param type The type of data
         * @see #getType()
         */
        public void setType(int type) {
            mType = type;
        }

        /**
         * @return The label when type is custom
         * @see #setLabel(String)
         */
        public String getLabel() {
            return mLabel;
        }

        /**
         * Set the label
         *
         * @param label The new label
         * @see #getLabel()
         */
        public void setLabel(String label) {
            mLabel = label;
        }

        /**
         * @return Whether or not this data item is valid or not
         */
        public boolean isValid() {
            return false;
        }

        /**
         * Get the label that should be shown for the type/label in this object
         *
         * @param resources The resources obtained from
         *            {@link android.content .Context#getResources()}
         * @return The String label to use as a description for the type of data
         *         item
         */
        public abstract String getTypeString(Resources resources);

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            ContactDataItem that = (ContactDataItem) o;

            if (mType != that.mType)
                return false;
            if (!mValue.equals(that.mValue))
                return false;
            if (mLabel != null ? !mLabel.equals(that.mLabel) : that.mLabel != null)
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = mValue.hashCode();
            result = 31 * result + mType;
            result = 31 * result + (mLabel != null ? mLabel.hashCode() : 0);
            return result;
        }
    }

    /**
     * A class wrapped around Android's Email Contact data.
     *
     * @see android.provider.ContactsContract.CommonDataKinds.Email
     */
    public static class EmailAddress extends ContactDataItem {
        /**
         * Whether or not this email address is external or not
         *
         * @see #isExternal()
         * @see #setExternal(boolean)
         */
        private boolean mIsExternal;

        /**
         * Default constructor
         */
        public EmailAddress() {
        }

        /**
         * @param emailAddress The email address
         */
        public EmailAddress(String emailAddress) {
            super(emailAddress);
        }

        @Override
        public boolean isValid() {
            return android.util.Patterns.EMAIL_ADDRESS.matcher(getValue()).matches();
        }

        @Override
        public String getTypeString(Resources resources) {
            return ContactsContract.CommonDataKinds.Email.getTypeLabel(resources, getType(),
                    getLabel()).toString();
        }

        /**
         * An external address is one that is outside of the company/group. It
         * will be shown as a different color in the UI.
         *
         * @return Whether or not this address is external.
         * @see #setExternal(boolean)
         */
        public boolean isExternal() {
            return mIsExternal;
        }

        /**
         * Set the external flag
         *
         * @param external The new external value
         * @see #isExternal()
         */
        public void setExternal(boolean external) {
            mIsExternal = external;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            if (!super.equals(o))
                return false;

            EmailAddress that = (EmailAddress) o;

            if (mIsExternal != that.mIsExternal)
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + (mIsExternal ? 1 : 0);
            return result;
        }
    }

    /**
     * A class wrapped around Android's Phone Contact data
     *
     * @see android.provider.ContactsContract.CommonDataKinds.Phone
     */
    public static class PhoneNumber extends ContactDataItem {
        @Override
        public boolean isValid() {
            // TODO: verify a phone number somehow?
            return true;
        }

        @Override
        public String getTypeString(Resources resources) {
            return ContactsContract.CommonDataKinds.Phone.getTypeLabel(resources, getType(),
                    getLabel()).toString();
        }
    }

    /**
     * The class which contains the contact details shared amongst all instances
     * of a given contact
     */
    public static class ContactDetails {
        // I would love this to be protected AND package-private but Java
        // doesn't allow that so
        // public it is

        /**
         * The lookup key for the contact
         *
         * @see #getLookupKey()
         * @see #setLookupKey(String)
         */
        private String mLookupKey = "";
        /**
         * The name for the contact
         *
         * @see #getName()
         * @see #setName(String)
         */
        private String mName = "";
        /**
         * The photo Uri for the contact
         *
         * @see #getPhotoUri()
         * @see #setPhotoUri(android.net.Uri)
         * @see #setPhotoUri(String)
         */
        private Uri mPhotoUri;
        /**
         * The list of email addresses
         *
         * @see #getEmailAddresses()
         * @see #setEmailAddresses(java.util.ArrayList)
         */
        private ArrayList<EmailAddress> mEmailAddresses = new ArrayList<EmailAddress>(0);
        /**
         * The list of phone numbers
         *
         * @see #getPhoneNumbers()
         * @see #setPhoneNumbers(java.util.ArrayList)
         */
        private ArrayList<PhoneNumber> mPhoneNumbers = new ArrayList<PhoneNumber>(0);
        /**
         * The
         * {@link com.blackberry.widgets.tagview.internal.contact.ContactsHelper}
         * which created this instance
         */
        private ContactsHelper mContactsHelper;
        /**
         * The flag for determining if all of the contact's data has been
         * lazy-loaded yet or not
         */
        private volatile boolean mNeedsLazyLoad = true;

        /**
         * @return The lookup key for the contact. This is the unique identifier
         *         for the contact
         * @see #setLookupKey(String)
         */
        public String getLookupKey() {
            return mLookupKey;
        }

        /**
         * Set the lookup key
         *
         * @param key The key to set
         * @see #getLookupKey()
         */
        public void setLookupKey(String key) {
            mLookupKey = key;
        }

        /**
         * @return The name of the contact
         * @see #setName(String)
         */
        public String getName() {
            return mName;
        }

        /**
         * Set the name of the contact
         *
         * @param name The new name of the contact
         * @see #getName()
         */
        public void setName(String name) {
            mName = name;
        }

        /**
         * @return The photo Uri of the contact
         * @see #setPhotoUri(android.net.Uri)
         * @see #setPhotoUri(String)
         */
        public Uri getPhotoUri() {
            if (TextUtils.isEmpty(mLookupKey)) {
                return null;
            }
            return mPhotoUri;
        }

        /**
         * Set the photo Uri of the contact
         *
         * @param photoUri The new photo Uri of the contact
         * @see #getPhotoUri()
         * @see #setPhotoUri(String)
         */
        public void setPhotoUri(Uri photoUri) {
            mPhotoUri = photoUri;
        }

        /**
         * Set the photo Uri of the contact
         *
         * @param photoUri The new photo Uri of the contact
         * @see #getPhotoUri()
         * @see #setPhotoUri(android.net.Uri)
         */
        public void setPhotoUri(String photoUri) {
            mPhotoUri = Uri.parse(photoUri);
        }

        /**
         * @return The string to use for displaying this contact
         */
        public String getLabel() {
            if (!TextUtils.isEmpty(mName)) {
                return mName;
            }

            return "";
        }

        /**
         * @return The list of email addresses for this contact
         * @see #setEmailAddresses(java.util.ArrayList)
         */
        public ArrayList<EmailAddress> getEmailAddresses() {
            return mEmailAddresses;
        }

        /**
         * Set the list of email addresses for this contact
         *
         * @param emailAddresses The new list of email addresses for this
         *            contact
         * @see #getEmailAddresses()
         */
        public void setEmailAddresses(ArrayList<EmailAddress> emailAddresses) {
            mEmailAddresses.clear();
            mEmailAddresses.addAll(emailAddresses);
        }

        /**
         * @return The list of phone numbers for this contact
         * @see #setPhoneNumbers(java.util.ArrayList)
         */
        public ArrayList<PhoneNumber> getPhoneNumbers() {
            return mPhoneNumbers;
        }

        /**
         * Set the list of phone numbers for this contact
         *
         * @param phoneNumbers The new list of phone numbers for this contact
         * @see #getPhoneNumbers()
         */
        public void setPhoneNumbers(ArrayList<PhoneNumber> phoneNumbers) {
            mPhoneNumbers.clear();
            mPhoneNumbers.addAll(phoneNumbers);
        }

        /**
         * @return The
         *         {@link com.blackberry.widgets.tagview.internal.contact.ContactsHelper}
         *         which created this instance
         * @see #setContactsHelper(com.blackberry.widgets.tagview.internal.contact.ContactsHelper)
         */
        public ContactsHelper getContactsHelper() {
            return mContactsHelper;
        }

        /**
         * @param contactsHelper The
         *            {@link com.blackberry.widgets.tagview.internal.contact .ContactsHelper}
         *            which created this instance
         * @see #getContactsHelper()
         */
        public void setContactsHelper(ContactsHelper contactsHelper) {
            mContactsHelper = contactsHelper;
        }

        /**
         * Lazy-load the contact's extra data.
         * <p/>
         * DO NOT call this method directly. Call {@link #lazyLoadData()}.
         */
        private synchronized void lazyLoadDataSync() {
            if (mNeedsLazyLoad) {
                mContactsHelper.fillWithExtraContactData(this);
                mNeedsLazyLoad = false;
            }
        }

        /**
         * Lazy-load the contact's extra data.
         * <p/>
         * This method is unsynchronized which is 2-5x faster than always
         * calling the synchronized version based on testing. It is safe since
         * boolean read/write is atomic and the only write is inside a
         * synchronized method.
         */
        void lazyLoadData() {
            if (mNeedsLazyLoad && (mContactsHelper != null)) {
                lazyLoadDataSync();
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            ContactDetails that = (ContactDetails) o;

            if (mNeedsLazyLoad != that.mNeedsLazyLoad) {
                lazyLoadData();
                that.lazyLoadData();
            }

            if (mLookupKey != null ? !mLookupKey.equals(that.mLookupKey) : that.mLookupKey != null)
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            // no need to lazy load, the lookup key is never lazy loaded.
            return mLookupKey != null ? mLookupKey.hashCode() : 0;
        }
    }
}
