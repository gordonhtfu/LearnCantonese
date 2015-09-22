package com.blackberry.widgets.tagview;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.WrapperListAdapter;

import com.blackberry.widgets.R;

/**
 * @author tallen
 *         <p/>
 *         This class is the section of BaseTags that shows completions. Currently it contains only
 *         a ListView however that could change in the future so it should not be limited to
 *         extending ListView.
 */
public class CompletionsListView extends LinearLayout {

    private ListView mListView;

    /**
     * @param context The context
     */
    public CompletionsListView(Context context) {
        this(context, null);
    }

    /**
     * @param context The context
     * @param attrs   The xml attributes
     */
    public CompletionsListView(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.completions_listview, this, true);

        mListView = (ListView) findViewById(R.id.clListView);
    }

    /**
     * Returns the adapter currently in use in this ListView. The returned adapter might not be the
     * same adapter passed to {@link #setAdapter(ListAdapter)} but might be a {@link
     * WrapperListAdapter}.
     *
     * @return The adapter currently used to display data in this ListView.
     * @see #setAdapter(ListAdapter)
     */
    public ListAdapter getAdapter() {
        return mListView.getAdapter();
    }

    /**
     * Sets the data behind this ListView.
     * <p/>
     * The adapter passed to this method may be wrapped by a {@link WrapperListAdapter}, depending
     * on the ListView features currently in use. For instance, adding headers and/or footers will
     * cause the adapter to be wrapped.
     *
     * @param adapter The ListAdapter which is responsible for maintaining the data backing this
     *                list and for producing a view to represent an item in that data set.
     * @see #getAdapter()
     */
    public void setAdapter(ListAdapter adapter) {
        mListView.setAdapter(adapter);
    }

    /**
     * Get the ListView used by this View
     *
     * @return The ListView used by this View
     * @see ListView
     */
    public ListView getListView() {
        return mListView;
    }

    @Override
    protected void dispatchSaveInstanceState(SparseArray<Parcelable> container) {
        // DO NOT call the super. This blocks the child Views from being saved by the default
        // implementation (which is bad for compound views). Save their instance state ourselves.
        //super.dispatchSaveInstanceState(container);

        super.dispatchFreezeSelfOnly(container);
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        // DO NOT call the super. This blocks the child Views from being saved by the default
        // implementation (which is bad for compound views). Save their instance state ourselves.
        //super.dispatchRestoreInstanceState(container);

        super.dispatchThawSelfOnly(container);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState ss = new SavedState(super.onSaveInstanceState());

        ss.mListViewSaveData = mListView.onSaveInstanceState();

        return ss;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        mListView.onRestoreInstanceState(ss.mListViewSaveData);
    }

    /**
     * Used for saving state
     */
    static class SavedState extends BaseSavedState {
        private Parcelable mListViewSaveData = null;

        /**
         * @param superState The saved-state Parcelable from the super class.
         */
        SavedState(Parcelable superState) {
            super(superState);
        }

        /**
         * @param in The parcel to unpack.
         */
        SavedState(Parcel in) {
            super(in);
            mListViewSaveData = in.readParcelable(null);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeParcelable(mListViewSaveData, 0);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}
