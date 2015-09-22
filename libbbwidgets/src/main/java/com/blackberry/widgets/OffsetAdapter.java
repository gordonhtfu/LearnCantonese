
package com.blackberry.widgets;

import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;

public class OffsetAdapter extends WrappedAdapter {

    private int mIndexOffset = 0;
    private int mCount = -1;

    /**
     * Constructor.
     */
    public OffsetAdapter() {
        super();
    }

    /**
     * @param wrappedAdapter The wrapped {@link Adapter}
     */
    public OffsetAdapter(Adapter wrappedAdapter) {
        this(wrappedAdapter, 0, -1);
    }

    /**
     * @param wrappedAdapter The wrapped {@link Adapter}
     * @param indexOffset the offset to use into the underlying {@link Adapter}
     * @param count the maximum number of items to use from the underlying
     *            {@link Adapter}. A negative value means use all items from the
     *            underlying {@link Adapter}.
     * @see #setIndexOffset(int)
     * @see #setCount(int)
     */
    public OffsetAdapter(Adapter wrappedAdapter, int indexOffset, int count) {
        super(wrappedAdapter);
        setIndexOffset(indexOffset);
        setCount(count);
    }

    @Override
    public int getCount() {
        int superCount = super.getCount();
        if (mCount < 0) {
            return superCount;
        }
        if (mIndexOffset >= superCount) {
            return 0;
        }
        if (mCount + mIndexOffset > superCount) {
            return mCount + mIndexOffset - superCount;
        }
        return mCount;
    }

    @Override
    public Object getItem(int position) {
        return super.getItem(position + mIndexOffset);
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position + mIndexOffset);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return super.getView(position + mIndexOffset, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return super.getDropDownView(position + mIndexOffset, convertView, parent);
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position + mIndexOffset);
    }

    @Override
    public boolean isEnabled(int position) {
        return super.isEnabled(position + mIndexOffset);
    }

    /**
     * @return the indexOffset
     */
    public int getIndexOffset() {
        return mIndexOffset;
    }

    /**
     * @param indexOffset the offset to use into the underlying {@link Adapter}
     */
    public void setIndexOffset(int indexOffset) {
        if (indexOffset < 0) {
            throw new IllegalArgumentException("indexOffset must be positive");
        }
        mIndexOffset = indexOffset;
    }

    /**
     * Sets the maximum number of items to use from the underlying
     * {@link Adapter}. At most count items will be used starting at
     * {@link #getIndexOffset()}.
     * 
     * @param count the maximum number of items to use from the underlying
     *            {@link Adapter}. A negative value means use all items from the
     *            underlying {@link Adapter}.
     */
    public void setCount(int count) {
        mCount = count;
    }

}
