
package com.blackberry.widgets.tagview.contact;

import android.content.Context;
import android.net.Uri;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ListAdapter;

import com.blackberry.widgets.R;
import com.blackberry.widgets.tagview.BaseTag;
import com.blackberry.widgets.tagview.BaseTags;
import com.blackberry.widgets.tagview.OnItemClickListener;
import com.blackberry.widgets.tagview.internal.activity.ActivityHelper;
import com.blackberry.widgets.tagview.internal.contact.ContactsHelper;

import java.security.InvalidParameterException;

/**
 * The base Tags control for all Contact-style Tags Views
 */
public abstract class BaseContactTags<T extends Contact> extends BaseTags<T> {
    /**
     * The
     * {@link com.blackberry.widgets.tagview.contact.ContactCompletionsAdapter}
     * to use for completions
     *
     * @see #setCompletionsAdapter(android.widget.ListAdapter)
     */
    private ContactCompletionsAdapter mCompletionsAdapter;
    /**
     * The {@link com.blackberry.widgets.tagview.contact.ContactRelatedAdapter}
     * to use for related contacts
     *
     * @see #setRelatedTagsAdapter(android.widget.Adapter)
     */
    private ContactRelatedAdapter mRelatedAdapter;
    /**
     * The View used as the contact picker
     */
    private ImageButton mContactPickerButton;

    /**
     * The listener that handles a click on a Related Tag
     */
    private OnItemClickListener mRelatedItemClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(View view, ItemClickEvent event) {
            // mTagAdapter.add(mRelatedAdapter.getItem(event.getPosition()));
            // getTagListView().requestFocus();
        }
    };
    /**
     * The helper object used for contact provider queries.
     */
    private ContactsHelper mContactsHelper;

    /**
     * @param context
     * @param attrs
     * @param tagClass
     * @param itemClass
     */
    public BaseContactTags(Context context, AttributeSet attrs,
            Class<? extends ContactTag> tagClass, Class<T> dataClass) {
        this(context, attrs, R.layout.contact_tag_view, tagClass, dataClass);
    }

    /**
     * @param context The context
     * @param attrs The xml attributes
     * @param layout
     * @param tagClass
     * @param itemClass
     */
    public BaseContactTags(Context context, AttributeSet attrs, int layout,
            Class<? extends ContactTag> tagClass, Class<T> dataClass) {
        super(context, attrs, layout, tagClass, dataClass);

        mContactsHelper = new ContactsHelper(context);

        setCompletionsAdapter(new ContactCompletionsAdapter(context));
        setRelatedTagsAdapter(new ContactRelatedAdapter());

        getRelatedTagListView().setOnItemClickListener(mRelatedItemClickListener);

        mContactPickerButton = (ImageButton) findViewById(R.id.ctvAddContactButton);
        setContactPickerVisibility();
        mContactPickerButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityHelper activityHelper = new ActivityHelper();
                activityHelper.setOnContactPicked(new ActivityHelper.OnContactPicked() {
                    @Override
                    public void onContactPicked(Uri contactUri) {
                        addTag(getContact(contactUri));
                    }
                });
                activityHelper.showContactPicker(getContext(), true);
            }
        });
    }

    @Override
    public <T extends ListAdapter & Filterable> void setCompletionsAdapter(T adapter) {
        if ((adapter != null) && (!(adapter instanceof ContactCompletionsAdapter))) {
            throw new InvalidParameterException(
                    "Completions Adapter must extend ContactCompletionsAdapter");
        }
        mCompletionsAdapter = (ContactCompletionsAdapter) adapter;
        mCompletionsAdapter.setContactsHelper(mContactsHelper);
        super.setCompletionsAdapter(adapter);
    }

    @Override
    public void setRelatedTagsAdapter(Adapter adapter) {
        if (adapter != null) {
            if (!(adapter instanceof ContactRelatedAdapter)) {
                throw new InvalidParameterException(
                        "Related Tag Adapter must extend ContactRelatedAdapter");
            }
            // ((ContactRelatedAdapter) adapter).setTagAdapter(mTagAdapter);
        }
        mRelatedAdapter = (ContactRelatedAdapter) adapter;
        super.setRelatedTagsAdapter(adapter);
    }

    /**
     * Register a listener called to query whether an email address is internal
     * or external.
     *
     * @param listener The listener to register
     */
    public void setOnEmailAddressIsExternalListener(OnEmailAddressIsExternalListener listener) {
        mContactsHelper.setOnEmailAddressIsExternalListener(listener);
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
        return mContactsHelper.getSelectExtraEmailAddresses();
    }

    /**
     * @param selectExtraEmailAddresses Whether or not to select all extra
     *            unmatching email addresses.
     * @see #getSelectExtraEmailAddresses()
     */
    public void setSelectExtraEmailAddresses(boolean selectExtraEmailAddresses) {
        mContactsHelper.setSelectExtraEmailAddresses(selectExtraEmailAddresses);
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
        return mContactsHelper.getSelectExtraPhoneNumbers();
    }

    /**
     * @param selectExtraPhoneNumbers Whether or not to select all extra
     *            unmatching phone numbers
     * @see #getSelectExtraPhoneNumbers()
     */
    public void setSelectExtraPhoneNumbers(boolean selectExtraPhoneNumbers) {
        mContactsHelper.setSelectExtraPhoneNumbers(selectExtraPhoneNumbers);
    }

    /**
     * @return The contacts helper object in use
     */
    protected ContactsHelper getContactsHelper() {
        return mContactsHelper;
    }

    // @Override
    // protected void onEditTextCreated(EditText editText) {
    // super.onEditTextCreated(editText);
    // editText.setImeOptions(EditorInfo.IME_ACTION_GO);
    // editText.setInputType(InputType.TYPE_CLASS_TEXT |
    // InputType.TYPE_TEXT_FLAG_CAP_WORDS);
    // editText.addTextChangedListener(mTextWatcher);
    // }

    /**
     * Fetch contact details given a Uri.
     *
     * @param contactUri The Uri of the contact
     */
    protected Contact getContact(Uri contactUri) {
        return mContactsHelper.fetchContact(contactUri);
    }

    /**
     * An interface which is called to determine if an email address is internal
     * or external
     */
    public static interface OnEmailAddressIsExternalListener {
        /**
         * @param contact The contact to check
         * @param emailAddress The email address to check
         * @return true if the contact's email address is external or false
         *         otherwise
         */
        boolean isExternal(Contact contact, Contact.EmailAddress emailAddress);

        /**
         * @param emailAddress The email address to check
         * @return true if the email address is external or false otherwise
         */
        boolean isExternal(String emailAddress);
    }

    /**
     * @author tallen
     *         <p/>
     *         A simple domain comparison class to determine if email addresses
     *         are internal or external.
     *         <p/>
     *         Internal for this class is defined as the email address ending
     *         with one of the domain strings passed in via the constructor.
     *         <p/>
     *         For example passing "blackberry.com" will mean abc@blackberry.com
     *         and def@ghi.blackberry.com will be considered Internal while
     *         jkl@blackberry.net will be considered External.
     */
    public static class DomainsOnEmailAddressIsExternalListener implements
            OnEmailAddressIsExternalListener {
        String[] mDomains;

        /**
         * @param domains The list of domains considered internal
         */
        public DomainsOnEmailAddressIsExternalListener(String[] domains) {
            if (domains != null) {
                mDomains = domains;
            } else {
                mDomains = new String[0];
            }
        }

        @Override
        public boolean isExternal(Contact contact,
                Contact.EmailAddress emailAddress) {
            if (emailAddress != null) {
                return !checkEmailAddressForInternal(emailAddress.getValue());
            } else {
                return false;
            }
        }

        @Override
        public boolean isExternal(String emailAddress) {
            return !checkEmailAddressForInternal(emailAddress);
        }

        /**
         * Check if the emailAddress is internal. In this case internal is
         * defined by ending with one of the strings in the domain array.
         *
         * @param emailAddress The email address to check
         * @return True if emailAddress is internal or false otherwise.
         */
        private boolean checkEmailAddressForInternal(String emailAddress) {
            for (String domain : mDomains) {
                if (emailAddress.endsWith(domain)) {
                    return true;
                }
            }
            return false;
        }
    }
    
    private void setContactPickerVisibility() {
        if (mContactPickerButton != null) {
            if (isReadOnly()) {
                mContactPickerButton.setVisibility(GONE);
            } else {
                mContactPickerButton.setVisibility(VISIBLE);
            }
        }
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        super.setReadOnly(readOnly);
        setContactPickerVisibility();
    }
}
