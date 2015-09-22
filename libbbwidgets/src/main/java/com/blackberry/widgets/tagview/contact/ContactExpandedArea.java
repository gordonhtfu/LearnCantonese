
package com.blackberry.widgets.tagview.contact;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.blackberry.widgets.tagview.IReadOnly;

/**
 * The standard expanded area for a Contact
 */
public class ContactExpandedArea extends ListView implements IReadOnly {
    /**
     * The {@link android.widget.Adapter} being used
     *
     * @see #setAdapter(android.widget.ListAdapter)
     */
    private BaseAdapter mAdapter;
    /**
     * The contact this expanded area represents
     *
     * @see #getContact()
     * @see #setContact(Contact)
     */
    private Contact mContact;
    /**
     * Whether or not this expanded area is read only or not
     *
     * @see #isReadOnly()
     * @see #setReadOnly(boolean)
     */
    private boolean mReadOnly = false;
    /**
     * The listener for delete clicks
     * 
     * @see #getOnDeleteClickListener()
     * @see #setOnDeleteClickListener(OnClickListener)
     */
    private View.OnClickListener mOnDeleteClickListener;

    /**
     * @param context The context
     */
    public ContactExpandedArea(Context context) {
        super(context);
    }

    /**
     * @param context The context
     * @param attrs The xml attributes
     */
    public ContactExpandedArea(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        if (adapter instanceof BaseAdapter) {
            mAdapter = (BaseAdapter) adapter;
        }
        super.setAdapter(adapter);
    }

    /**
     * @return The contact this View represents
     * @see #setContact(Contact)
     */
    public Contact getContact() {
        return mContact;
    }

    /**
     * Set the contact this View represents
     *
     * @param contact The contact this View represents
     * @see #getContact()
     */
    public void setContact(Contact contact) {
        mContact = contact;
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    /**
     * @return Whether or not this View is read only or not
     * @see #setReadOnly(boolean)
     */
    public boolean isReadOnly() {
        return mReadOnly;
    }

    /**
     * @param readOnly The read-only state to set
     * @see #isReadOnly()
     */
    public void setReadOnly(boolean readOnly) {
        this.mReadOnly = readOnly;
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Call this to force an update of the View (ie when the underlying data
     * object has changed
     */
    public void update() {
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    /**
     * @return The listener for delete clicks
     */
    public View.OnClickListener getOnDeleteClickListener() {
        return mOnDeleteClickListener;
    }

    /**
     * @param onDeleteClickListener The listener for delete clicks
     */
    public void setOnDeleteClickListener(View.OnClickListener onDeleteClickListener) {
        mOnDeleteClickListener = onDeleteClickListener;
    }
}
