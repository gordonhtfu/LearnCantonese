
package com.blackberry.widgets.tagview.contact.email;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;

import com.blackberry.widgets.tagview.ListItem;
import com.blackberry.widgets.tagview.contact.Contact;
import com.blackberry.widgets.tagview.contact.ContactExpandedArea;
import com.blackberry.widgets.tagview.internal.activity.ActivityHelper;

/**
 * A customized expanded area for an
 * {@link com.blackberry.widgets.tagview.contact.email.EmailContact}
 */
public class EmailContactExpandedArea extends ContactExpandedArea {
    /**
     * The {@link EmailContact} represented by this expanded area
     */
    private EmailContact mEmailContact;
    /**
     * The {@link android.widget.Adapter} used to provide data to the expanded
     * area
     */
    private ExpandedAreaAdapter mAdapter = new ExpandedAreaAdapter();
    /**
     * The listener used when the add contact button is clicked
     */
    private OnClickListener mOnAddContactClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            ActivityHelper activityHelper = new ActivityHelper();
            Bundle bundle = new Bundle();
            bundle.putString(ContactsContract.Intents.Insert.EMAIL,
                    mEmailContact.getActiveEmailAddress().getValue());
            bundle.putString(ContactsContract.Intents.Insert.NAME, mEmailContact.getName());
            activityHelper.setOnContactAdded(new ActivityHelper.OnContactAdded() {
                @Override
                public void onContactAdded(Uri contactUri) {
                    Contact newContact = mEmailContact.getContactsHelper().fetchContact(contactUri);
                    mEmailContact.setContactDetails(newContact.getContactDetails());
                    // this getEmailAddresses causes the EmailContact to
                    // lazy-load the data
                    mEmailContact.getEmailAddresses();
                    mEmailContact.setActiveEmailAddressIndex(0);
                    mAdapter.notifyDataSetChanged();
                }
            });
            activityHelper.addContact(getContext(), bundle);
        }
    };

    /**
     * @param context The context
     */
    public EmailContactExpandedArea(Context context) {
        this(context, null);
    }

    /**
     * @param context The context
     * @param attrs The xml attributes
     */
    public EmailContactExpandedArea(Context context, AttributeSet attrs) {
        super(context, attrs);

        setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (mEmailContact.getActiveEmailAddressIndex() != i) {
                    mEmailContact.setActiveEmailAddressIndex(i);
                    mAdapter.notifyDataSetChanged();
                }
            }
        });

        setAdapter(mAdapter);
    }

    @Override
    public void setContact(Contact contact) {
        if (!(contact instanceof EmailContact)) {
            throw new IllegalArgumentException("contact must be an EmailContact");
        }
        mEmailContact = (EmailContact) contact;
        super.setContact(contact);
    }

    /**
     * The adapter to use to fill the ListView in the expanded area
     */
    private class ExpandedAreaAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            if (mEmailContact != null) {
                if (isReadOnly()) {
                    return 1;
                }
                return mEmailContact.getEmailAddresses().size();
            }
            return 0;
        }

        @Override
        public Object getItem(int i) {
            // Don't bother. We never use this method and it doesn't appear
            // ListView does either.
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            EmailContactListItem item = null;
            if (mEmailContact != null) {
                if ((view != null) && (view instanceof ListItem)) {
                    item = (EmailContactListItem) view;
                } else {
                    item = new EmailContactListItem(viewGroup.getContext());
                    item.getRightImageView().setOnClickListener(mOnAddContactClickListener);
                }
                item.setAllowAddContactButton(true);
                item.setSwapImageViews(true);
                if (isReadOnly()) {
                    item.setContact(mEmailContact);
                    item.setSoftFocus(true);
                    item.getDeleteImageView().setVisibility(View.GONE);
                    item.getDeleteImageView().setOnClickListener(null);
                } else {
                    item.setContact(mEmailContact, mEmailContact.getEmailAddresses().get(i));
                    // highlight only the one that is active
                    item.setSoftFocus(i == mEmailContact.getActiveEmailAddressIndex());
                    if (i == 0) {
                        item.getDeleteImageView().setVisibility(View.VISIBLE);
                        item.getDeleteImageView().setOnClickListener(getOnDeleteClickListener());
                    } else {
                        item.getDeleteImageView().setVisibility(View.GONE);
                        item.getDeleteImageView().setOnClickListener(null);
                    }
                }
                if (i > 0) {
                    item.setTitleVisibility(View.GONE);
                } else {
                    item.setTitleVisibility(View.VISIBLE);
                }
            }

            return item;
        }
    }
}
