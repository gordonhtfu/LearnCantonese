
package com.blackberry.widgets.tagview.contact.email;

import android.text.TextUtils;

import com.blackberry.widgets.tagview.contact.Contact;

/**
 * A class wrapped around Android's Contact data
 * <p/>
 * The idea of this class is an internal data object can be shared among many
 * EmailContact objects. The one data point kept from being shared is the
 * getActiveEmailAddressIndex. This allows one copy of all of the Contact's data
 * (name, email addresses, etc) but multiple EmailContact objects each able to
 * have a different active email address.
 */
public class EmailContact extends Contact {
    /**
     * The active email address
     *
     * @see #getActiveEmailAddress()
     * @see #getActiveEmailAddressIndex()
     * @see #setActiveEmailAddressIndex(int)
     */
    private EmailAddress mActiveEmailAddress = null;

    /**
     * Default constructor
     */
    public EmailContact() {
        super();
    }

    /**
     * Construct with an active email address.
     *
     * @param emailAddress The email address to add and set as active
     */
    public EmailContact(String emailAddress) {
        super();

        getEmailAddresses().add(new EmailAddress(emailAddress));
        setActiveEmailAddressIndex(0);
    }

    /**
     * Construct with a shared copy of contact details.
     *
     * @param contactDetails The contact details to share
     */
    public EmailContact(Contact.ContactDetails contactDetails) {
        super(contactDetails);
    }

    /**
     * @return The string to use for displaying this contact
     */
    @Override
    public String getLabel() {
        String result = super.getLabel();
        if (!TextUtils.isEmpty(result)) {
            return result;
        }

        EmailAddress activeEmailAddress = getActiveEmailAddress();
        if (activeEmailAddress != null) {
            return activeEmailAddress.getValue();
        }

        return "";
    }

    /**
     * @return The currently active email address index.
     */
    public int getActiveEmailAddressIndex() {
        if (mActiveEmailAddress != null) {
            return getContactDetails().getEmailAddresses().indexOf(mActiveEmailAddress);
        }
        return -1;
    }

    /**
     * Set the currently active email address index.
     * <p/>
     * Passing a value outside the allowed range of the list returned by
     * {@link #getEmailAddresses()} will deselect any active email address.
     *
     * @param index The new index
     */
    public void setActiveEmailAddressIndex(int index) {
        if ((index < 0) || (index >= getEmailAddresses().size())) {
            mActiveEmailAddress = null;
        } else {
            mActiveEmailAddress = getEmailAddresses().get(index);
        }
        // notifyOnObjectChangedListener();
    }

    /**
     * Convenience method to get the active email address directly
     *
     * @return The selected EmailAddress object or null if none selected
     */
    public EmailAddress getActiveEmailAddress() {
        if (hasActiveEmailAddress()) {
            return mActiveEmailAddress;
        }
        return null;
    }

    /**
     * @return true if there is an active email address and this active email
     *         address is itself a valid email address or false otherwise
     */
    @Override
    public boolean isValid() {
        if (mActiveEmailAddress != null) {
            return mActiveEmailAddress.isValid();
        }
        return false;
    }

    /**
     * @return true if there is an active email address or false otherwise
     */
    private boolean hasActiveEmailAddress() {
        return (mActiveEmailAddress != null)
                && getContactDetails().getEmailAddresses().contains(mActiveEmailAddress);
    }

    /**
     * Return a string in the form "Name <email@address.com>" based on the
     * currently active email address.
     * 
     * @return The display string for the active email address or the empty
     *         string if no email address is active
     */
    public String getActiveDisplayString() {
        return getDisplayString(getActiveEmailAddress());
    }

    /**
     * Return a string in the form "Name <email@address.com>" based on the email
     * address at index emailAddressIndex
     * 
     * @param emailAddressIndex The index of the email address to use
     * @return The display string for the email address or the empty string if
     *         emailAddressIndex is invalid
     */
    public String getDisplayString(int emailAddressIndex) {
        if ((emailAddressIndex < 0)
                || (emailAddressIndex >= getContactDetails().getEmailAddresses().size())) {
            return getDisplayString(null);
        }
        return getDisplayString(getContactDetails().getEmailAddresses().get(emailAddressIndex));
    }

    /**
     * Return a string in the form "Name <email@address.com>" based on the email
     * address provided. This does NOT verify that emailAddress actually exists
     * in this Contact.
     * 
     * @param emailAddress The email address to use. Should be one from this
     *            Contact's {@link #getEmailAddresses()} list but this is not
     *            enforced.
     * @return The display string for the email address or the empty string if
     *         emailAddress is null.
     */
    public String getDisplayString(EmailAddress emailAddress) {
        if (emailAddress != null) {
            return getName() + " <" + emailAddress.getValue() + ">";
        }
        return "";
    }
}
