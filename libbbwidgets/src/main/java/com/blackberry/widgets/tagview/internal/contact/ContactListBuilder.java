
package com.blackberry.widgets.tagview.internal.contact;

import android.text.TextUtils;

import com.blackberry.widgets.tagview.contact.Contact;

import java.util.ArrayList;
import java.util.List;

/**
 * A helper class used to ensure ContactDetails are being shared across all
 * Contact objects with the same
 * {@link com.blackberry.widgets.tagview.contact.Contact#getLookupKey()}.
 */
public class ContactListBuilder {
    /**
     * The list of contacts
     */
    ArrayList<Contact> mContacts = new ArrayList<Contact>(0);

    /**
     * Call this method if you know how many contacts will be created. This
     * saves the internal ArrayList from constantly rebuilding itself
     *
     * @param capacity The capacity to set on the internal list.
     */
    public void setCapacity(int capacity) {
        mContacts.ensureCapacity(capacity);
    }

    /**
     * @param lookupKey The lookup key to find details of
     * @return A
     *         {@link com.blackberry.widgets.tagview.contact.Contact.ContactDetails}
     *         object to be shared by all Contact objects with the given
     *         lookupKey
     */
    public Contact.ContactDetails findContactDetails(String lookupKey) {
        if (!TextUtils.isEmpty(lookupKey)) {
            for (Contact contact : mContacts) {
                if (TextUtils.equals(lookupKey, contact.getLookupKey())) {
                    return contact.getContactDetails();
                }
            }
        }
        return new Contact.ContactDetails();
    }

    /**
     * Add a contact to the list
     *
     * @param contact The contact to add
     */
    public void add(Contact contact) {
        mContacts.add(contact);
    }

    /**
     * @return The list of Contact objects.
     */
    List<Contact> getContacts() {
        return mContacts;
    }
}
