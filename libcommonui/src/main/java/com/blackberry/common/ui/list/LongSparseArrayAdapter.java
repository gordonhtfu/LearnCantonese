
package com.blackberry.common.ui.list;

import android.content.Context;
import android.util.LongSparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

/**
 * A BaseAdapter that is backed by an {@link LongSparseArray} of arbitrary objects.
 * 
 * @param <T>
 */
public abstract class LongSparseArrayAdapter<T> extends BaseAdapter {

    private LongSparseArray<T> mData;
    private Context mContext;

    public LongSparseArrayAdapter(Context context) {
        this(context, null);
    }

    public LongSparseArrayAdapter(Context context, LongSparseArray<T> data) {
        mContext = context;
        setData(data);
    }

    public void setData(LongSparseArray<T> data) {
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
