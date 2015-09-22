
package com.blackberry.common.ui.list;

import android.content.Context;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.google.android.mail.common.base.Preconditions;

/**
 * A BaseAdapter that is backed by an {@link SparseArray} of arbitrary objects.
 * 
 * @param <T>
 */
public abstract class SparseArrayAdapter<T> extends BaseAdapter {

    private SparseArray<T> mData;
    private Context mContext;

    public SparseArrayAdapter(Context context) {
        this(context, null);
    }

    public SparseArrayAdapter(Context context, SparseArray<T> data) {
        mContext = context;
        setData(data);
    }

    public void setData(SparseArray<T> data) {
        mData = data;
    }

    @Override
    public int getCount() {
        if (mData != null) {
            return mData.size();
        } else {
            return 0;
        }
    }

    @Override
    public T getItem(int position) {
        if (mData != null) {
            return mData.valueAt(position);
        } else {
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        if (mData != null) {
            return mData.keyAt(position);
        } else {
            return -1;
        }
    }

    public abstract View newView(Context context, ViewGroup parent);

    public abstract void bindView(View view, Context context, T data);

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v;
        Preconditions.checkElementIndex(position, getCount());
        T item = getItem(position);
        if (convertView == null) {
            v = newView(mContext, parent);
        } else {
            v = convertView;
        }
        bindView(v, mContext, item);
        return v;
    }
}
