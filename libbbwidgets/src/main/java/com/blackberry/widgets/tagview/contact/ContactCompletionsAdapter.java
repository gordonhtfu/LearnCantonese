
package com.blackberry.widgets.tagview.contact;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.blackberry.widgets.tagview.internal.contact.ContactsHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * The completion {@link android.widget.Adapter} for standard Contact objects
 */
public class ContactCompletionsAdapter extends BaseAdapter implements Filterable {
    /**
     * List of Objects so that we (or subclasses) can add custom items for
     * Lookup, space-to-symbol completion (ie "foo bar com" has the top
     * completion "foo@bar.com"), etc.
     */
    private List<Object> mCompletions = new ArrayList<Object>(0);
    /**
     * The context
     *
     * @see #getContext()
     */
    private Context mContext;
    /**
     * The task to perform the search
     */
    private ContactSearchTask mSearchTask = null;
    /**
     * The listener registered to be called when an email address needs to be
     * determined to be internal or external
     *
     * @see #getOnEmailAddressIsExternalListener()
     * @see #setOnEmailAddressIsExternalListener(com.blackberry.widgets.tagview.contact.BaseContactTags
     *      .OnEmailAddressIsExternalListener)
     */
    private BaseContactTags.OnEmailAddressIsExternalListener mIsExternalListener;
    /**
     * THe listener registered to handle contact searches
     *
     * @see #setContactSearch(com.blackberry.widgets.tagview.contact.ContactCompletionsAdapter
     *      .ContactSearch)
     */
    private ContactSearch mContactSearch;
    /**
     * The helper around contact providers.
     */
    private ContactsHelper mContactsHelper;

    /**
     * @param context The context
     */
    public ContactCompletionsAdapter(Context context) {
        mContext = context;
    }

    @Override
    public int getCount() {
        return mCompletions.size();
    }

    @Override
    public Object getItem(int position) {
        return mCompletions.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        Object o = getItem(position);
        if (o instanceof Contact) {
            return 1;
        }
        return 0;
    }

    /**
     * @param contactSearch The interface to register
     */
    protected void setContactSearch(ContactSearch contactSearch) {
        mContactSearch = contactSearch;
    }

    /**
     * @return The context
     */
    protected Context getContext() {
        return mContext;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Object o = getItem(position);
        if (o instanceof Contact) {
            Contact contact = (Contact) o;
            ContactListItem item;
            if ((convertView != null) && (convertView instanceof ContactListItem)) {
                item = (ContactListItem) convertView;
                item.setTitleVisibility(View.VISIBLE);
            } else {
                item = new ContactListItem(parent.getContext());
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
        TextView tv;
        if ((convertView != null) && (convertView instanceof TextView)) {
            tv = (TextView) convertView;
        } else {
            tv = new TextView(parent.getContext());
        }
        tv.setText(o.toString());
        return tv;
    }

    /**
     * @return The contacts helper currently being used
     */
    public ContactsHelper getContactsHelper() {
        return mContactsHelper;
    }

    /**
     * @param contactsHelper The contacts helper object to use
     */
    public void setContactsHelper(ContactsHelper contactsHelper) {
        mContactsHelper = contactsHelper;
    }

    /**
     * This method is called to start a new search.
     * <p/>
     * This must be called from the UI thread.
     *
     * @param searchString The string to search for
     */
    public void performSearch(String searchString) {
        if (mSearchTask != null) {
            mSearchTask.cancel(true);
        }
        mSearchTask = new ContactSearchTask();
        mSearchTask.execute(searchString);
    }

    /**
     * This method attempts to shortcut an input string into an email address.
     * The first space is replaced by '@' (if there isn't already an '@' symbol)
     * while all other spaces are replaced by '.'
     * <p/>
     * For instance:
     * <ul>
     * <li>'jdoe foo com' becomes 'jdoe@foo.com'</li>
     * <li>'j doe@foo com' becomes 'j.doe@foo.com'</li>
     * </ul>
     *
     * @param input The input string to attempt to shortcut
     * @return The shortcut email address or an empty string if it cannot be
     *         shortcut
     */
    protected String attemptEmailShortcut(String input) {
        input = input.trim();
        if (input.contains(" ")) {
            String result = input;
            if (!result.contains("@")) {
                result = result.replaceFirst(" ", "@");
            }
            result = result.replace(" ", ".");
            return result;
        }
        if (Patterns.EMAIL_ADDRESS.matcher(input).matches()) {
            return input;
        }
        return "";
    }

    /**
     * The task which searches for contacts.
     */
    private class ContactSearchTask extends AsyncTask<String, Void, List<Object>> {

        @Override
        protected List<Object> doInBackground(String... params) {
            String searchString = params[0];

            if (mContactSearch == null) {
                return new ArrayList<Object>(0);
            }

            List<Contact> contacts = mContactSearch.performSearch(searchString);
            List<Object> result = new ArrayList<Object>(contacts.size());

            for (Contact contact : contacts) {
                result.add(contact);
            }

            return result;
        }

        @Override
        protected void onPostExecute(List<Object> contacts) {
            super.onPostExecute(contacts);
            mSearchTask = null;
            mCompletions = contacts;
            notifyDataSetChanged();
        }

        /**
         * Cancel the task
         */
        public void cancel() {
            if (mContactSearch != null) {
                mContactSearch.cancel();
            }
            cancel(true);
        }
    }

    /**
     * An interface used in subclasses to register their search provider.
     */
    protected interface ContactSearch {
        /**
         * @param pattern The pattern to match
         * @return The list of matched contacts
         */
        List<Contact> performSearch(String pattern);

        /**
         * Cancel the search
         */
        void cancel();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults filterResults = new FilterResults();
                if ((mContactSearch == null) || (TextUtils.isEmpty(constraint))) {
                    filterResults.values = new ArrayList<Object>(0);
                    filterResults.count = 0;
                } else {
                    List<Contact> contacts = mContactSearch.performSearch(constraint.toString());
                    List<Object> result = new ArrayList<Object>(contacts.size());

                    for (Contact contact : contacts) {
                        result.add(contact);
                    }
                    filterResults.count = result.size();
                    filterResults.values = result;
                }

                return filterResults;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                mCompletions = (List<Object>) results.values;
                notifyDataSetChanged();
            }

        };
    }
}
