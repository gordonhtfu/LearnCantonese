
package com.blackberry.widgets.tagview.contact.email;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.blackberry.widgets.tagview.contact.Contact;
import com.blackberry.widgets.tagview.contact.ContactListItem;

/**
 * The custom {@link com.blackberry.widgets.tagview.ListItem} representing an
 * {@link com.blackberry.widgets.tagview.contact.email.EmailContact}
 */
public class EmailContactListItem extends ContactListItem {
    /**
     * The {@link com.blackberry.widgets.tagview.contact.Contact.EmailAddress}
     * to show
     *
     * @see #setContact(com.blackberry.widgets.tagview.contact.Contact)
     * @see #setContact(EmailContact,
     *      com.blackberry.widgets.tagview.contact.Contact.EmailAddress)
     */
    private Contact.EmailAddress mEmailToShow = null;

    /**
     * @param context The context
     */
    public EmailContactListItem(Context context) {
        this(context, null);
    }

    /**
     * @param context The context
     * @param attrs The xml attributes
     */
    public EmailContactListItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Set the contact, showing the currently active email address
     *
     * @param contact The contact to represent
     */
    @Override
    public void setContact(Contact contact) {
        if (!(contact instanceof EmailContact)) {
            throw new IllegalArgumentException("contact must be an EmailContact");
        }
        mEmailToShow = ((EmailContact) contact).getActiveEmailAddress();
        super.setContact(contact);
    }

    /**
     * Set the contact, showing the provided email address
     *
     * @param contact The contact to represent
     * @param emailAddress The email address to show
     */
    public void setContact(EmailContact contact, Contact.EmailAddress emailAddress) {
        mEmailToShow = emailAddress;
        super.setContact(contact);
    }

    @Override
    protected String createTitle() {
        EmailContact contact = (EmailContact) getContact();
        if (!TextUtils.isEmpty(contact.getName())) {
            return contact.getName();
        }
        if (mEmailToShow != null) {
            return mEmailToShow.getValue();
        }
        return super.createTitle();
    }

    @Override
    protected String createDescription() {
        EmailContact contact = (EmailContact) getContact();
        if ((mEmailToShow != null) && (!TextUtils.isEmpty(contact.getName()))) {
            return mEmailToShow.getValue();
        }
        return super.createDescription();
    }

    @Override
    protected String createStatus() {
        if (mEmailToShow != null) {
            return mEmailToShow.getTypeString(getContext().getResources());
        }
        return super.createStatus();
    }
}
