
package com.blackberry.widgets.tagview.contact.email;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;

import com.blackberry.widgets.tagview.contact.Contact;
import com.blackberry.widgets.tagview.contact.ContactCompletionsAdapter;

import java.util.List;

/**
 * @author tallen
 *         <p/>
 *         The standard Adapter used by EmailTags for generating completions.
 *         Users can subclass this and provide their own
 *         {@link com.blackberry.widgets.tagview.contact .ContactCompletionsAdapter.ContactSearch}
 *         delegate to add custom items, alternative list sorting, etc. Users
 *         who do this will also need to:
 *         <ol>
 *         <li>override {@link #getView(int, View, ViewGroup)} to provide the
 *         view to use for displaying any custom items, falling back to the
 *         super's {@link #getView(int, View, ViewGroup)} for non-custom items.</li>
 *         <li>override {@link #getItemViewType(int)} returning a unique integer
 *         >= super's {@link #getViewTypeCount()} for each type of custom item,
 *         falling back to the super's {@link #getItemViewType(int)} for
 *         non-custom items.</li>
 *         </ol>
 * @see EmailTags
 */
public class EmailCompletionsAdapter extends ContactCompletionsAdapter {

    /**
     * @param context The context
     */
    public EmailCompletionsAdapter(Context context) {
        super(context);
        setContactSearch(new EmailContactSearch());
    }

    @Override
    public int getItemViewType(int position) {
        Object o = getItem(position);
        if (o instanceof EmailContact) {
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
        if (o instanceof EmailContact) {
            EmailContact contact = (EmailContact) o;
            EmailContactListItem item;
            if ((convertView != null) && (convertView instanceof EmailContactListItem)) {
                item = (EmailContactListItem) convertView;
                item.setTitleVisibility(View.VISIBLE);
            } else {
                item = new EmailContactListItem(parent.getContext());
            }
            if (position > 0) {
                Object previousObject = getItem(position - 1);
                if (previousObject instanceof Contact) {
                    Contact previousContact = (Contact) previousObject;
                    if (!TextUtils.isEmpty(previousContact.getLookupKey())
                            && TextUtils.equals(previousContact.getLookupKey(),
                                    contact.getLookupKey())) {
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
    private class EmailContactSearch implements ContactSearch {
        @Override
        public List<Contact> performSearch(String pattern) {
            List<Contact> result = getContactsHelper().matchEmailAddresses(pattern);
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
