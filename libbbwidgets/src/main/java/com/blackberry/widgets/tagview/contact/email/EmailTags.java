
package com.blackberry.widgets.tagview.contact.email;

import android.content.Context;
import android.net.Uri;
import android.text.InputType;
import android.text.util.Rfc822Token;
import android.text.util.Rfc822Tokenizer;
import android.util.AttributeSet;
import android.widget.EditText;

import com.blackberry.widgets.tagview.BaseTag;
import com.blackberry.widgets.tagview.contact.BaseContactTags;
import com.blackberry.widgets.tagview.contact.Contact;
import com.blackberry.widgets.tagview.contact.ContactTag;
import com.blackberry.widgets.tagview.internal.contact.ContactsHelper;

import java.util.Collection;
import java.util.List;

/**
 * @author tallen
 *         <p/>
 *         This class is a specialized version of
 *         {@link com.blackberry.widgets.tagview.contact .BaseContactTags} for
 *         use in email clients.
 */
public class EmailTags extends BaseContactTags<EmailContact> {

    // This *HAS* to be a static class or we leak a Context when we rotate
    // because the ContactsHelper instance would keep a reference to this
    // EmailTags object.
    private static final ContactsHelper.OnContactMatched mOnContactMatched = new ContactsHelper.OnContactMatched() {
        @Override
        public Contact onContactMatched(Contact.ContactDetails contactDetails, Contact
                .ContactDataItem matchedDataItem) {
            EmailContact contact = new EmailContact(contactDetails);
            if (matchedDataItem != null) {
                contact.setActiveEmailAddressIndex(
                        contactDetails.getEmailAddresses().indexOf(matchedDataItem));
            } else {
                contact.setActiveEmailAddressIndex(0);
            }

            return contact;
        }

        @Override
        public Contact onContactMatched(String inputText) {
            return new EmailContact(inputText);
        }
    };

    /**
     * @param context The context
     */
    public EmailTags(Context context) {
        this(context, null);
    }

    /**
     * @param context The context
     * @param attrs The xml attributes
     */
    public EmailTags(Context context, AttributeSet attrs) {
        super(context, attrs, EmailTag.class, EmailContact.class);

        setSelectExtraEmailAddresses(true);
        setSelectExtraPhoneNumbers(true);

        getContactsHelper().setOnContactMatched(mOnContactMatched);

        setCompletionsAdapter(new EmailCompletionsAdapter(context));
        setRelatedTagsAdapter(new EmailRelatedAdapter());
    }

    @Override
    protected EmailContact createTagDataItem(CharSequence inputText) {
        // try for an exact match first.
        Contact c = getContactsHelper().fetchContact(inputText.toString());
        if (c != null) {
            return (EmailContact) c;
        }
        // fall back to a best-match
        return (EmailContact) getContactsHelper().createContact(inputText.toString());
    }

    // @Override
    // protected void onEditTextCreated(EditText editText) {
    // super.onEditTextCreated(editText);
    // editText.setInputType(InputType.TYPE_CLASS_TEXT
    // | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
    // }

    /**
     * Add the item to the end of the current list of items. The Content
     * Provider is queried to find additional contact data based on the email
     * address. If no contact is found, information from the input string is
     * used if available. The email address will be parsed by
     * {@link Rfc822Tokenizer#tokenize(CharSequence)} so any acceptable input
     * there is also acceptable here.
     *
     * @param emailAddress The item to add
     * @deprecated Use {@link #addEmailAddresses(String)} instead.
     */
    @Deprecated
    public void addEmailAddress(String emailAddress) {
        addEmailAddresses(emailAddress);
    }

    /**
     * Add the items to the end of the current list of items. The Content
     * Provider is queried to find additional contact data based on the email
     * addresses. If no contact is found, information from the input string is
     * used if available. The input string will be parsed by
     * {@link Rfc822Tokenizer#tokenize(CharSequence)} so any acceptable input
     * there is also acceptable here.
     *
     * @param emailAddresses The item to add
     */
    public void addEmailAddresses(String emailAddresses) {
        Rfc822Token[] tokens = Rfc822Tokenizer.tokenize(emailAddresses);
        for (Rfc822Token token : tokens) {
            EmailContact c = createTagDataItem(token.getAddress());
            // If no contact was found, use the name if available from the input
            // string.
            if (!c.isContactValid() && (token.getName() != null)) {
                c.setName(token.getName());
            }
            addTag(c);
        }
    }

    /**
     * Add all items to the end of the current list of items. The Content
     * Provider is queried to find additional contact data based on each email
     * address. If no contact is found, information from the input strings are
     * used if available. The email addresses will be parsed by
     * {@link Rfc822Tokenizer#tokenize(CharSequence)} so any acceptable input
     * there is also acceptable here.
     *
     * @param emailAddresses The items to add
     */
    public void addAllEmailAddresses(Collection<String> emailAddresses) {
        for (String emailAddress : emailAddresses) {
            addEmailAddresses(emailAddress);
        }
    }

    /**
     * Add all items to the end of the current list of items. The Content
     * Provider is queried to find additional contact data based on each email
     * address. If no contact is found, information from the input strings are
     * used if available. The email addresses will be parsed by
     * {@link Rfc822Tokenizer#tokenize(CharSequence)} so any acceptable input
     * there is also acceptable here.
     *
     * @param emailAddresses The items to add
     */
    public void addAllEmailAddresses(String... emailAddresses) {
        for (String emailAddress : emailAddresses) {
            addEmailAddresses(emailAddress);
        }
    }

    @Override
    protected Contact getContact(Uri contactUri) {
        EmailContact contact = (EmailContact) super.getContact(contactUri);
        contact.setActiveEmailAddressIndex(0);
        return contact;
    }
}
