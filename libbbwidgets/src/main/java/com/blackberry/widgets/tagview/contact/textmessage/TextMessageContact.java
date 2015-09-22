package com.blackberry.widgets.tagview.contact.textmessage;

import android.text.TextUtils;

import com.blackberry.widgets.tagview.contact.Contact;

/**
 * A class wrapped around Android's Contact data
 * <p/>
 * The idea of this class is an internal data object can be shared among many TextMessageContact
 * objects. The one data point kept from being shared is the {@link #getActiveDataItem()}. This
 * allows one copy of all of the Contact's data (name, email addresses, etc) but multiple
 * TextMessageContact objects each able to have a different active data items.
 */
public class TextMessageContact extends Contact {
    /**
     * The currently active data item
     */
    private ContactDataItem mActiveDataItem = null;

    /**
     * Default constructor
     */
    public TextMessageContact() {
        super();
    }

    /**
     * @param contactDetails The contact details to share
     */
    public TextMessageContact(ContactDetails contactDetails) {
        super(contactDetails);
    }

    @Override
    public String getLabel() {
        String result = super.getLabel();
        if (!TextUtils.isEmpty(result)) {
            return result;
        }

        ContactDataItem activeDataItem = getActiveDataItem();
        if (activeDataItem != null) {
            return activeDataItem.getValue();
        }

        return "";
    }

    /**
     * @return The currently active data item (email address, phone number) index.
     * @see #getActiveDataItem()
     * @see #setActiveDataItemIndex(int)
     */
    public int getActiveDataItemIndex() {
        if (mActiveDataItem != null) {
            int result = getPhoneNumbers().indexOf(mActiveDataItem);
            if (result >= 0) {
                return result;
            }
            result = getEmailAddresses().indexOf(mActiveDataItem);
            if (result >= 0) {
                return getPhoneNumbers().size() + result;
            }
        }
        return -1;
    }

    /**
     * Set the currently active data item (email address, phone number) index.
     * <p/>
     * Passing a value outside the allowed range of the combined list returned by {@link
     * #getPhoneNumbers()} and {@link #getEmailAddresses()} will deselect any active data item.
     *
     * @param index The new index
     * @see #getActiveDataItem()
     * @see #getActiveDataItemIndex()
     */
    public void setActiveDataItemIndex(int index) {
        if (index < 0) {
            mActiveDataItem = null;
//            notifyOnObjectChangedListener();
            return;
        }
        int realListIndex = index;
        if (realListIndex >= getPhoneNumbers().size()) {
            realListIndex -= getPhoneNumbers().size();
            if (realListIndex >= getEmailAddresses().size()) {
                mActiveDataItem = null;
//                notifyOnObjectChangedListener();
                return;
            }
        }

        mActiveDataItem = getDataItem(index);
//        notifyOnObjectChangedListener();
    }

    /**
     * Convenience method to get the active email address directly
     *
     * @return The selected EmailAddress object or null if none selected
     * @see #getActiveDataItemIndex()
     * @see #setActiveDataItemIndex(int)
     */
    public ContactDataItem getActiveDataItem() {
        if (mActiveDataItem == null) {
            return null;
        }
        if (!getPhoneNumbers().contains(mActiveDataItem)
                && !getEmailAddresses().contains(mActiveDataItem)) {
            return null;
        }
        return mActiveDataItem;
    }

    /**
     * Get a data item at a specified index. The index is of a union of the phone number list and
     * the email address list (in that order).
     *
     * @param index The index of the data item to get
     * @return The data item at the requested index
     */
    ContactDataItem getDataItem(int index) {
        int realListIndex = index;
        if (realListIndex >= getPhoneNumbers().size()) {
            realListIndex -= getPhoneNumbers().size();
            if (realListIndex >= getEmailAddresses().size()) {
                return null;
            }
            return getEmailAddresses().get(realListIndex);
        }
        return getPhoneNumbers().get(realListIndex);
    }

    /**
     * @return The total count of data items (phone numbers + email addresses)
     */
    int getDataItemCount() {
        return getPhoneNumbers().size() + getEmailAddresses().size();
    }

    @Override
    public boolean isValid() {
        ContactDataItem activeDataItem = getActiveDataItem();
        if (activeDataItem != null) {
            return activeDataItem.isValid();
        }
        return false;
    }

    /**
     * @return Whether or not there is an active data item
     */
    private boolean hasActiveDataItem() {
        if (mActiveDataItem == null) {
            return false;
        }
        if (!getPhoneNumbers().contains(mActiveDataItem) &&
                !getEmailAddresses().contains(mActiveDataItem)) {
            return false;
        }
        return true;
    }
}