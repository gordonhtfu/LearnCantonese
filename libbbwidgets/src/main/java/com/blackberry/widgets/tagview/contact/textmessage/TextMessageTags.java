
package com.blackberry.widgets.tagview.contact.textmessage;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;

import com.blackberry.widgets.tagview.contact.BaseContactTags;
import com.blackberry.widgets.tagview.contact.Contact;
import com.blackberry.widgets.tagview.internal.contact.ContactsHelper;

import java.util.Collection;

/**
 * @author tallen
 *         <p/>
 *         This class is a specialized version of
 *         {@link com.blackberry.widgets.tagview.contact .BaseContactTags} for
 *         use in text messaging clients.
 */
public class TextMessageTags extends BaseContactTags<TextMessageContact> {

    // This *HAS* to be a static class or we leak a Context when we rotate
    // because the ContactsHelper instance would keep a reference to this
    // EmailTags object.
    private static final ContactsHelper.OnContactMatched mOnContactMatched = new ContactsHelper.OnContactMatched() {
        @Override
        public Contact onContactMatched(Contact.ContactDetails contactDetails,
                Contact.ContactDataItem matchedDataItem) {
            TextMessageContact contact = new TextMessageContact(contactDetails);
            if (matchedDataItem != null) {
                contact.setActiveDataItemIndex(contact.getDataItemCount() - 1);
            } else {
                contact.setActiveDataItemIndex(0);
            }

            return contact;
        }

        @Override
        public Contact onContactMatched(String inputText) {
            TextMessageContact contact = new TextMessageContact();
            Contact.EmailAddress emailAddress = new Contact.EmailAddress(inputText);
            if (emailAddress.isValid()) {
                contact.getEmailAddresses().add(emailAddress);
            } else {
                Contact.PhoneNumber phoneNumber = new Contact.PhoneNumber();
                phoneNumber.setValue(inputText);
                contact.getPhoneNumbers().add(phoneNumber);
            }
            contact.setActiveDataItemIndex(0);
            return contact;
        }
    };

    /**
     * @param context The context
     */
    public TextMessageTags(Context context) {
        this(context, null);
    }

    /**
     * @param context The context
     * @param attrs The xml attributes
     */
    public TextMessageTags(Context context, AttributeSet attrs) {
        super(context, attrs, TextMessageTag.class, TextMessageContact.class);

        setSelectExtraEmailAddresses(true);
        setSelectExtraPhoneNumbers(true);

        getContactsHelper().setOnContactMatched(mOnContactMatched);

        setCompletionsAdapter(new TextMessageCompletionsAdapter(context));
        setRelatedTagsAdapter(null);
    }

    @Override
    protected TextMessageContact createTagDataItem(CharSequence inputText) {
        // try for an exact match first.
        Contact c = getContactsHelper().fetchContact(inputText.toString());
        if (c != null) {
            return (TextMessageContact) c;
        }
        // fall back to a best-match
        return (TextMessageContact) getContactsHelper().createContact(inputText.toString());
    }

    /**
     * Add the item to the end of the current list of items. The Content
     * Provider is queried to find additional contact data based on the item.
     *
     * @param item The item to add
     */
    public void addItem(String item) {
        addTag(createTagDataItem(item));
    }

    /**
     * Add all items to the end of the current list of items. The Content
     * Provider is queried to find additional contact data based on each item.
     *
     * @param items The items to add
     */
    public void addAllItems(Collection<String> items) {
        for (String item : items) {
            addItem(item);
        }
    }

    /**
     * Add all items to the end of the current list of items. The Content
     * Provider is queried to find additional contact data based on each item.
     *
     * @param items The items to add
     */
    public void addAllItems(String... items) {
        for (String item : items) {
            addItem(item);
        }
    }

    @Override
    protected Contact getContact(Uri contactUri) {
        TextMessageContact contact = (TextMessageContact) super.getContact(contactUri);
        contact.setActiveDataItemIndex(0);
        return contact;
    }
}
