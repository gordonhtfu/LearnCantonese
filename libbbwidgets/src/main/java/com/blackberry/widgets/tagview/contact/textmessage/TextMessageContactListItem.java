package com.blackberry.widgets.tagview.contact.textmessage;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.blackberry.widgets.tagview.contact.Contact;
import com.blackberry.widgets.tagview.contact.ContactListItem;

/**
 * The custom {@link com.blackberry.widgets.tagview.ListItem} representing a {@link
 * com.blackberry.widgets.tagview.contact.textmessage.TextMessageContact}
 */
public class TextMessageContactListItem extends ContactListItem {
    /**
     * The {@link com.blackberry.widgets.tagview.contact.Contact.ContactDataItem} to show
     *
     * @see #setContact(com.blackberry.widgets.tagview.contact.Contact)
     * @see #setContact(TextMessageContact, com.blackberry.widgets.tagview.contact.Contact.ContactDataItem)
     */
    private Contact.ContactDataItem mDataItemToShow = null;

    /**
     * @param context The context
     */
    public TextMessageContactListItem(Context context) {
        this(context, null);
    }

    /**
     * @param context The context
     * @param attrs   The xml attribute
     */
    public TextMessageContactListItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Set the contact, showing the currently active data item
     *
     * @param contact The contact to represent
     */
    @Override
    public void setContact(Contact contact) {
        if (!(contact instanceof TextMessageContact)) {
            throw new IllegalArgumentException("contact must be an TextMessageContact");
        }
        mDataItemToShow = ((TextMessageContact) contact).getActiveDataItem();
        super.setContact(contact);
    }

    /**
     * Set the contact, showing the provided data item
     *
     * @param contact  The contact to represent
     * @param dataItem The data item to show
     */
    public void setContact(TextMessageContact contact, Contact.ContactDataItem dataItem) {
        mDataItemToShow = dataItem;
        super.setContact(contact);
    }

    @Override
    protected String createTitle() {
        TextMessageContact contact = (TextMessageContact) getContact();
        if (!TextUtils.isEmpty(contact.getName())) {
            return contact.getName();
        }
        if (mDataItemToShow != null) {
            return mDataItemToShow.getValue();
        }
        return super.createTitle();
    }

    @Override
    protected String createDescription() {
        TextMessageContact contact = (TextMessageContact) getContact();
        if ((mDataItemToShow != null) && (!TextUtils.isEmpty(contact.getName()))) {
            return mDataItemToShow.getValue();
        }
        return super.createDescription();
    }

    @Override
    protected String createStatus() {
        if (mDataItemToShow != null) {
            return mDataItemToShow.getTypeString(getContext().getResources());
        }
        return super.createStatus();
    }
}
