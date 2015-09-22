
package com.blackberry.widgets.tagview.contact.textmessage;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
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
 * A customized expanded area for a
 * {@link com.blackberry.widgets.tagview.contact.textmessage .TextMessageContact}
 */
public class TextMessageContactExpandedArea extends ContactExpandedArea {
    /**
     * The
     * {@link com.blackberry.widgets.tagview.contact.textmessage.TextMessageContact}
     * represented
     */
    private TextMessageContact mTextMessageContact;
    /**
     * The adapter used to fill the expanded area
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
            Contact.ContactDataItem activeItem = mTextMessageContact.getActiveDataItem();
            if (activeItem instanceof Contact.EmailAddress) {
                bundle.putString(ContactsContract.Intents.Insert.EMAIL, activeItem.getValue());
            } else {
                bundle.putString(ContactsContract.Intents.Insert.PHONE, activeItem.getValue());
            }
            bundle.putString(ContactsContract.Intents.Insert.NAME, mTextMessageContact.getName());
            activityHelper.setOnContactAdded(new ActivityHelper.OnContactAdded() {
                @Override
                public void onContactAdded(Uri contactUri) {
                    Contact newContact = mTextMessageContact.getContactsHelper().fetchContact(
                            contactUri);
                    mTextMessageContact.setContactDetails(newContact.getContactDetails());
                    // this getEmailAddresses causes the TextMessageContact to
                    // lazy-load the data
                    mTextMessageContact.getEmailAddresses();
                    mTextMessageContact.setActiveDataItemIndex(0);
                    mAdapter.notifyDataSetChanged();
                }
            });
            activityHelper.addContact(getContext(), bundle);
        }
    };

    /**
     * @param context The context
     */
    public TextMessageContactExpandedArea(Context context) {
        this(context, null);
    }

    /**
     * @param context The context
     * @param attrs The xml attributes
     */
    public TextMessageContactExpandedArea(Context context, AttributeSet attrs) {
        super(context, attrs);

        setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (mTextMessageContact.getActiveDataItemIndex() != i) {
                    mTextMessageContact.setActiveDataItemIndex(i);
                    mAdapter.notifyDataSetChanged();
                }
            }
        });

        setAdapter(mAdapter);
    }

    @Override
    public void setContact(Contact contact) {
        if (!(contact instanceof TextMessageContact)) {
            throw new IllegalArgumentException("contact must be an TextMessageContact");
        }
        mTextMessageContact = (TextMessageContact) contact;
        super.setContact(contact);
    }

    /**
     * The adapter to use to fill the ListView in the expanded area
     */
    private class ExpandedAreaAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            if (mTextMessageContact != null) {
                if (isReadOnly()) {
                    return 1;
                }
                return mTextMessageContact.getDataItemCount();
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
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            TextMessageContactListItem item = null;
            if (mTextMessageContact != null) {
                if ((view != null) && (view instanceof ListItem)) {
                    item = (TextMessageContactListItem) view;
                } else {
                    item = new TextMessageContactListItem(viewGroup.getContext());
                    item.getRightImageView().setOnClickListener(mOnAddContactClickListener);
                }
                item.setAllowAddContactButton(true);
                item.setSwapImageViews(true);
                if (isReadOnly()) {
                    item.setContact(mTextMessageContact);
                    item.setSoftFocus(true);
                    item.getDeleteImageView().setVisibility(View.GONE);
                    item.getDeleteImageView().setOnClickListener(null);
                } else {
                    item.setContact(mTextMessageContact, mTextMessageContact.getDataItem(i));
                    // highlight only the one that is active
                    item.setSoftFocus(i == mTextMessageContact.getActiveDataItemIndex());
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
