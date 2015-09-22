package com.blackberry.widgets.tagview.contact.textmessage;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import com.blackberry.widgets.tagview.contact.Contact;
import com.blackberry.widgets.tagview.contact.ContactCompletionsAdapter;

import java.util.List;

/**
 * @author tallen
 *         <p/>
 *         The standard Adapter used by EmailTags for generating completions. Users can subclass
 *         this and provide their own {@link com.blackberry.widgets.tagview.contact
 *         .ContactCompletionsAdapter.ContactSearch} delegate to add custom items, alternative list
 *         sorting, etc. Users who do this will also need to: <ol> <li> override {@link
 *         #getView(int, View, ViewGroup)} to provide the view to use for displaying any custom
 *         items, falling back to the super's {@link #getView(int, View, ViewGroup)} for non-custom
 *         items.</li> <li> override {@link #getItemViewType(int)} returning a unique integer >=
 *         super's {@link #getViewTypeCount()} for each type of custom item, falling back to the
 *         super's {@link #getItemViewType(int)} for non-custom items.</li> </ol>
 * @see com.blackberry.widgets.tagview.contact.email.EmailTags
 */
public class TextMessageCompletionsAdapter extends ContactCompletionsAdapter {

    /**
     * @param context The context
     */
    public TextMessageCompletionsAdapter(Context context) {
        super(context);
        setContactSearch(new TextMessageContactSearch());
    }

    @Override
    public int getItemViewType(int position) {
        Object o = getItem(position);
        if (o instanceof TextMessageContact) {
            return super.getViewTypeCount();
        }
        return super.getItemViewType(position);
    }

    @Override
    public int getViewTypeCount() {
        return super.getViewTypeCount() + 1;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Object o = getItem(position);
        if (o instanceof TextMessageContact) {
            TextMessageContact contact = (TextMessageContact) o;
            TextMessageContactListItem item;
            if ((convertView != null) && (convertView instanceof TextMessageContactListItem)) {
                item = (TextMessageContactListItem) convertView;
            } else {
                item = new TextMessageContactListItem(parent.getContext());
            }
            if (position > 0) {
                Object previousObject = getItem(position - 1);
                if (previousObject instanceof Contact) {
                    Contact previousContact = (Contact) previousObject;
                    if (TextUtils.equals(previousContact.getLookupKey(), contact.getLookupKey())) {
                        item.setTitleVisibility(View.GONE);
                    }
                }
            }
            item.setContact(contact);
            return item;
        }
        return super.getView(position, convertView, parent);
    }

    /**
     * The class to perform the searching
     */
    private class TextMessageContactSearch implements ContactSearch {
        @Override
        public List<Contact> performSearch(String pattern) {
            List<Contact> result = getContactsHelper().matchPhoneNumbers(pattern);
            result.addAll(getContactsHelper().matchEmailAddresses(pattern));
            String emailShortcut = attemptEmailShortcut(pattern);
            if (!TextUtils.isEmpty(emailShortcut)) {
                result.add(getContactsHelper().createContact(emailShortcut));
            }
            return result;
        }

        @Override
        public void cancel() {
            getContactsHelper().cancel();
        }
    }
}
