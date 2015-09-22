
package com.blackberry.widgets;

import android.database.DataSetObserver;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.AbsSavedState;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListAdapter;
import android.widget.SpinnerAdapter;

import com.blackberry.widgets.tagview.ISaveInstanceState;

/**
 * A base class for an {@link Adapter} that wraps around another {@link Adapter}
 * providing extra functionality on top.
 */
public class WrappedAdapter extends BaseAdapter implements ISaveInstanceState, Filterable {
    /**
     * The {@link Adapter} that is wrapped
     *
     * @see #WrappedAdapter(android.widget.Adapter)
     * @see #setWrappedAdapter(android.widget.Adapter)
     * @see #getWrappedAdapter()
     */
    private Adapter mWrappedAdapter = null;

    /**
     * Default constructor
     */
    public WrappedAdapter() {
        super();
    }

    /**
     * @param wrappedAdapter The wrapped {@link Adapter}
     */
    public WrappedAdapter(Adapter wrappedAdapter) {
        super();
        setWrappedAdapter(wrappedAdapter);
    }

    /**
     * Set the wrapped {@link Adapter}
     *
     * @param wrappedAdapter The wrapped {@link Adapter}
     * @see #getWrappedAdapter()
     */
    public void setWrappedAdapter(Adapter wrappedAdapter) {
        mWrappedAdapter = wrappedAdapter;
        if (mWrappedAdapter != null) {
            mWrappedAdapter.registerDataSetObserver(new DataSetObserver() {
                @Override
                public void onChanged() {
                    notifyDataSetChanged();
                }

                @Override
                public void onInvalidated() {
                    notifyDataSetInvalidated();
                }
            });
        }
        notifyDataSetChanged();
    }

    /**
     * @return The currently wrapped {@link Adapter}
     * @see #setWrappedAdapter(android.widget.Adapter)
     */
    public Adapter getWrappedAdapter() {
        return mWrappedAdapter;
    }

    @Override
    public int getCount() {
        if (mWrappedAdapter != null) {
            return mWrappedAdapter.getCount();
        }
        return 0;
    }

    @Override
    public Object getItem(int i) {
        if (mWrappedAdapter != null) {
            return mWrappedAdapter.getItem(i);
        }
        return null;
    }

    @Override
    public long getItemId(int i) {
        if (mWrappedAdapter != null) {
            return mWrappedAdapter.getItemId(i);
        }
        return 0;
    }

    @Override
    public boolean hasStableIds() {
        if (mWrappedAdapter != null) {
            return mWrappedAdapter.hasStableIds();
        }
        return super.hasStableIds();
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (mWrappedAdapter != null) {
            return mWrappedAdapter.getView(i, view, viewGroup);
        }
        return null;
    }

    @Override
    public int getViewTypeCount() {
        if (mWrappedAdapter != null) {
            return mWrappedAdapter.getViewTypeCount();
        }
        return super.getViewTypeCount();
    }

    @Override
    public boolean isEmpty() {
        if (mWrappedAdapter != null) {
            return mWrappedAdapter.isEmpty();
        }
        return super.isEmpty();
    }

    @Override
    public int getItemViewType(int position) {
        if (mWrappedAdapter != null) {
            return mWrappedAdapter.getItemViewType(position);
        }
        return super.getItemViewType(position);
    }

    @Override
    public boolean areAllItemsEnabled() {
        if ((mWrappedAdapter != null) && (mWrappedAdapter instanceof ListAdapter)) {
            return ((ListAdapter) mWrappedAdapter).areAllItemsEnabled();
        }
        return false;
    }

    @Override
    public boolean isEnabled(int i) {
        if ((mWrappedAdapter != null) && (mWrappedAdapter instanceof ListAdapter)) {
            return ((ListAdapter) mWrappedAdapter).isEnabled(i);
        }
        return false;
    }

    @Override
    public View getDropDownView(int i, View view, ViewGroup viewGroup) {
        if ((mWrappedAdapter != null) && (mWrappedAdapter instanceof SpinnerAdapter)) {
            return ((SpinnerAdapter) mWrappedAdapter).getDropDownView(i, view, viewGroup);
        }
        return null;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        if (mWrappedAdapter instanceof ISaveInstanceState) {
            return new BaseSavedState(((ISaveInstanceState) mWrappedAdapter).onSaveInstanceState());
        }
        return BaseSavedState.EMPTY_STATE;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof BaseSavedState) {
            BaseSavedState ss = (BaseSavedState) state;
            if (mWrappedAdapter instanceof ISaveInstanceState) {
                ((ISaveInstanceState) mWrappedAdapter).onRestoreInstanceState(ss.getSuperState());
            }
        }
    }

    /**
     * Used for saving state
     */
    protected static class BaseSavedState extends AbsSavedState {
        /**
         * Constructor used when reading from a parcel. Reads the state of the
         * superclass.
         *
         * @param source The parcel to unpack
         */
        public BaseSavedState(Parcel source) {
            super(source);
        }

        /**
         * Constructor called by derived classes when creating their SavedState
         * objects
         *
         * @param superState The state of the superclass of this view
         */
        public BaseSavedState(Parcelable superState) {
            super(superState);
        }

        public static final Parcelable.Creator<BaseSavedState> CREATOR =
                new Parcelable.Creator<BaseSavedState>() {
                    public BaseSavedState createFromParcel(Parcel in) {
                        return new BaseSavedState(in);
                    }

                    public BaseSavedState[] newArray(int size) {
                        return new BaseSavedState[size];
                    }
                };
    }

    @Override
    public Filter getFilter() {
        if (mWrappedAdapter instanceof Filterable) {
            return ((Filterable) mWrappedAdapter).getFilter();
        }
        return null;
    }
}
