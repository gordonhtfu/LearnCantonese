package com.blackberry.widgets.tagview;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Adapter;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.blackberry.widgets.R;
import com.blackberry.widgets.tagview.ExpandableTag.OnExpandRequestListener;

/**
 * A horizontal ListView which wraps Views onto the next row if it does not fit on the current row.
 * <p/>
 * It also handles showing the expanded View when an ExpandableTag being shown in this View requests
 * it.
 *
 * @author tallen
 */
public class WrapTagListView extends FrameLayout implements IReadOnly {
    /**
     * The {@link com.blackberry.widgets.tagview.FlowLayout} which handles the horizontal line-wrapping
     */
    private ViewGroup mFlowLayout;
    /**
     * The area which is shown when an {@link com.blackberry.widgets.tagview.ExpandableTag} is expanded
     */
    private RelativeLayout mExpandedAreaLayout;
    /**
     * The {@link Adapter} to use for data items
     *
     * @see #getAdapter()
     * @see #setAdapter(android.widget.Adapter)
     */
    private Adapter mAdapter = null;
    /**
     * The listener notified when #mAdapter changes
     */
    private AdapterDataSetObserver mObserver = new AdapterDataSetObserver();
    /**
     * The listener registered to be notified when an item has been clicked
     *
     * @see #setOnItemClickListener(OnItemClickListener)
     */
    private OnItemClickListener mItemClickListener;
    /**
     * The listener used to listen for child View clicks
     */
    private ViewOnClickListener mViewClickListener = new ViewOnClickListener();
    /**
     * The listener registered with each child {@link com.blackberry.widgets.tagview.ExpandableTag} to be
     * notified when it should be expanded.
     */
    private OnExpandRequestListener mOnExpandRequestListener = new OnExpandRequestListener() {

        @Override
        public void expand(ExpandableTag tag, boolean expand) {
            expandTag(tag, expand);
        }
    };
    /**
     * The maximum number of tags to show when collapsed.
     *
     * @see #getMaxTagsWhenCollapsed()
     * @see #setMaxTagsWhenCollapsed(int)
     */
    private int mMaxTagsWhenCollapsed = 4;
    /**
     * The {@link com.blackberry.widgets.tagview.MoreTag} to show when collapsed with more than
     * #getMaxTagsWhenCollapsed() items
     */
    private MoreTag mMoreTag = null;
    /**
     * Whether or not this control is collapsed
     */
    private boolean mCollapsed = true;
    /**
     * The listener registered to be notified when the input state changes
     *
     * @see #setOnInputStateChangedListener(com.blackberry.widgets.tagview.WrapTagListView
     * .OnInputStateChangedListener)
     */
    private OnInputStateChangedListener mOnInputStateChangedListener = null;
    /**
     * The listener registered to be notified when the size of this View changes
     *
     * @see #setOnSizeChangedListener(com.blackberry.widgets.tagview.WrapTagListView.OnSizeChangedListener)
     */
    private OnSizeChangedListener mOnSizeChangedListener = null;
    /**
     * Whether or not the control is read only or not.
     *
     * @see #isReadOnly()
     * @see #setReadOnly(boolean)
     */
    private boolean mReadOnly = false;
    /**
     * Whether or not input is currently allowed or not
     *
     * @see #setInputAllowed(boolean)
     */
    private boolean mInputAllowed = false;

    /**
     * @param context The context
     */
    public WrapTagListView(Context context) {
        this(context, null);
    }

    /**
     * @param context The context
     * @param attrs   The xml attributes
     */
    public WrapTagListView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setFocusableInTouchMode(true);
        setClickable(true);

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.wrap_tag_listview, this, true);

        mFlowLayout = (ViewGroup) findViewById(R.id.wtlFlowLayout);
        mExpandedAreaLayout = (RelativeLayout) findViewById(R.id.wtlExpandedAreaLayout);

        // TODO: Can we optimize this so we aren't always listening for global focus changes?
        // Try to figure out if we can register/deregister it during only certain scenarios
        getViewTreeObserver().addOnGlobalFocusChangeListener(new ViewTreeObserver
                .OnGlobalFocusChangeListener() {
            @Override
            public void onGlobalFocusChanged(View oldFocus, View newFocus) {
                if (findFocus() == null) {
                    // No child of ours nor ourself has focus. De-select everything.
                    for (int i = 0; i < mFlowLayout.getChildCount(); i += 1) {
                        mFlowLayout.getChildAt(i).setSelected(false);
                    }
                    setCollapsed(true);
                    setInputAllowed(false);
                }
            }
        });

        init(attrs);
    }

    /**
     * Initialize the control with the given attribute set.
     *
     * @param attrs The xml attributes
     */
    private void init(AttributeSet attrs) {
        if (attrs == null) {
            return;
        }

        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.BaseTags);

        try {
            int count = a.getIndexCount();
            for (int i = 0; i < count; i++) {
                int attr = a.getIndex(i);
                if (attr == R.styleable.WrapTagListView_max_tags_when_collapsed) {
                    int value = a.getInt(attr, Integer.MIN_VALUE);
                    if (value > Integer.MIN_VALUE) {
                        setMaxTagsWhenCollapsed(value);
                    }
                } else if (attr == R.styleable.WrapTagListView_read_only) {
                    setReadOnly(a.getBoolean(attr, false));
                }
            }
        } finally {
            a.recycle();
        }
    }

    /**
     * @return The maximum number of tags to show when collapsed.
     * @see #setMaxTagsWhenCollapsed(int)
     */
    public int getMaxTagsWhenCollapsed() {
        return mMaxTagsWhenCollapsed;
    }

    /**
     * Set the maximum number of tags to show when Collapsed. The tag at position
     * maxTagsWhenCollapsed-1 will be a More tag used to expand this control showing all tags.
     * <p/>
     * The {@link com.blackberry.widgets.tagview.WrapTagListView} is considered Collapsed when it does not
     * have focus.
     *
     * @param maxTagsWhenCollapsed The maximum number of tags to show when collapsed including the
     *                             "More" tag. Setting to <=0 disables this option. The default
     *                             value is 4.
     * @see #getMaxTagsWhenCollapsed()
     */
    public void setMaxTagsWhenCollapsed(int maxTagsWhenCollapsed) {
        if (this.mMaxTagsWhenCollapsed != maxTagsWhenCollapsed) {
            this.mMaxTagsWhenCollapsed = maxTagsWhenCollapsed;
            updateTagListView();
        }
    }

    /**
     * @return The {@link Adapter} currently used to display data in this ListView.
     * @see #setAdapter(Adapter)
     */
    public Adapter getAdapter() {
        return mAdapter;
    }

    /**
     * Sets the data behind this ListView.
     *
     * @param adapter The {@link Adapter} which is responsible for maintaining the data backing this
     *                list and for producing a view to represent an item in that data set.
     * @see #getAdapter()
     */
    public void setAdapter(Adapter adapter) {
        if (mAdapter != null) {
            mAdapter.unregisterDataSetObserver(mObserver);
        }
        mAdapter = adapter;
        if (mAdapter != null) {
            mAdapter.registerDataSetObserver(mObserver);
        }
        updateTagListView();
    }

    /**
     * Set a listener for when an item in this list is clicked.
     *
     * @param listener The listener to register for item clicks
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        mItemClickListener = listener;
    }

    /**
     * Set a listener for when the input state changes
     *
     * @param listener The listener to register
     */
    public void setOnInputStateChangedListener(OnInputStateChangedListener listener) {
        this.mOnInputStateChangedListener = listener;
    }

    /**
     * Set whether or not this control is collapsed.
     *
     * @param collapsed The new collapsed value
     */
    private void setCollapsed(boolean collapsed) {
        if (mCollapsed != collapsed) {
            mCollapsed = collapsed;
            if (mCollapsed) {
                if ((mMaxTagsWhenCollapsed > 0) && (mFlowLayout.getChildCount() >
                        mMaxTagsWhenCollapsed)) {
                    updateTagListView();
                }
            } else {
                updateTagListView();
            }
        }
    }

    /**
     * Update the children of the View.
     */
    private void updateTagListView() {
//        ExpandableTag selectedTag = null;
//        clearAllChildViews();
//        int tagsToShow;
//        if ((!mCollapsed) || (mMaxTagsWhenCollapsed <= 0)) {
//            tagsToShow = mAdapter.getCount();
//        } else {
//            tagsToShow = Math.min(mAdapter.getCount(), mMaxTagsWhenCollapsed);
//        }
//        int tagsShown = 0;
//        for (int i = 0; i < mAdapter.getCount(); i += 1) {
//            View v = mAdapter.getView(i, null, mFlowLayout);
//            v.setOnClickListener(mViewClickListener);
//            if (v instanceof BaseTag) {
//                if (v instanceof ExpandableTag) {
//                    ExpandableTag tag = (ExpandableTag) v;
//                    tag.setOnExpandRequestListener(mOnExpandRequestListener);
//                    if (tag.isSelected()) {
//                        selectedTag = tag;
//                    }
//                }
//                tagsShown += 1;
//                if (tagsShown == tagsToShow) {
//                    int howManyMore = 1;
//                    for (int j = i + 1; j < mAdapter.getCount(); j += 1) {
//                        // TODO: Optimize this if we can by not having to create the Views for
//                        // hidden Tags
//                        if (mAdapter.getView(j, null, mFlowLayout) instanceof BaseTag) {
//                            howManyMore += 1;
//                        }
//                    }
//                    if (howManyMore > 1) {
//                        // We didn't add them all meaning we have to add a More tag
//                        mFlowLayout.addView(getMoreTag(howManyMore));
//                        break;
//                    }
//                }
//            }
//            if (v instanceof IReadOnly) {
//                ((IReadOnly) v).setReadOnly(mReadOnly);
//            }
//            mFlowLayout.addView(v);
//        }
//        if (selectedTag == null) {
//            expandTag(selectedTag, false);
//            setInputAllowed(true);
//            if (isFocused()) {
//                // re-request focus so the input field can gain focus (if there is one)
//                requestFocus();
//            }
//        } else {
//            expandTag(selectedTag, true);
//        }
    }

    /**
     * @param howManyMore The number of extra children that are hidden by this {@link
     *                    com.blackberry.widgets.tagview.MoreTag}
     * @return The {@link com.blackberry.widgets.tagview.MoreTag}
     */
    private MoreTag getMoreTag(int howManyMore) {
//        if (mMoreTag == null) {
//            mMoreTag = new MoreTag(getContext());
//            mMoreTag.setOnClickListener(new OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    setCollapsed(false);
//                    requestFocus();
//                }
//            });
//        }
//        mMoreTag.setHowManyMore(howManyMore);
//        return mMoreTag;
        return null;
    }

    /**
     * Remove all child views from the {@link com.blackberry.widgets.tagview.FlowLayout}
     */
    private void clearAllChildViews() {
//        for (int i = 0; i < mFlowLayout.getChildCount(); i += 1) {
//            View v = mFlowLayout.getChildAt(i);
//            if (v instanceof ExpandableTag) {
//                ExpandableTag tag = (ExpandableTag) v;
//                tag.setOnExpandRequestListener(null);
//            }
//        }
//        mFlowLayout.removeAllViews();
    }

    /**
     * Toggle the selection on a View. There is currently no multiselect so all other child views
     * are de-selected.
     *
     * @param toggleView The View to toggle the selection on
     */
    private void toggleItemselection(View toggleView) {
        if (!toggleView.isSelected()) {
            // go through and de-select everyone since we are selecting a "new"
            // View
            int toggleViewIndex = mFlowLayout.indexOfChild(toggleView);
            for (int i = 0; i < mFlowLayout.getChildCount(); i += 1) {
                if (i != toggleViewIndex) {
                    mFlowLayout.getChildAt(i).setSelected(false);
                }
            }
        }
        // and select the one we want selected at the end
        toggleView.setSelected(!toggleView.isSelected());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            // touched somewhere in the whitespace so de-select all children
            for (int i = 0; i < mFlowLayout.getChildCount(); i += 1) {
                mFlowLayout.getChildAt(i).setSelected(false);
            }
            setInputAllowed(true);
            requestFocus();
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean requestFocus(int direction, Rect previouslyFocusedRect) {
        setCollapsed(false);
        boolean hasSelectedChild = false;
        for (int i = 0; i < mFlowLayout.getChildCount(); i += 1) {
            if (mFlowLayout.getChildAt(i).isSelected()) {
                hasSelectedChild = true;
                break;
            }
        }
        boolean result = false;
        if (!hasSelectedChild) {
            setInputAllowed(true);
            result = mFlowLayout.requestFocus(direction, previouslyFocusedRect);
        }
        if (!result) {
            result = super.requestFocus(direction, previouslyFocusedRect);
        }
        return result;
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        for (int i = 0; i < mFlowLayout.getChildCount(); i += 1) {
            mFlowLayout.getChildAt(i).setSelected(false);
        }
        super.requestChildFocus(child, focused);
    }

    /**
     * Called when we need to expand or collapse a tag.
     *
     * @param tag    The tag to expand
     * @param expand Whether to expand or collapse the tag
     * @see #doExpandTag(ExpandableTag, boolean)
     */
    private void expandTag(final ExpandableTag tag, final boolean expand) {
        // Post a Runnable because it was causing issues when this was sometimes being called at
        // a time when the Android system does not allow re-laying out. This ensures it is done
        // at a safe time.
        post(new Runnable() {
            @Override
            public void run() {
                doExpandTag(tag, expand);
            }
        });
    }

    /**
     * Actually perform the tag expansion/collapse.
     *
     * @param tag    The tag to expand
     * @param expand Whether to expand or collapse the tag
     */
    private void doExpandTag(ExpandableTag tag, boolean expand) {
//        mExpandedAreaLayout.removeAllViews();
//        if (expand) {
//            View expandedArea = tag.getExpandedAreaView(mExpandedAreaLayout);
//            if (expandedArea != null) {
//                ViewGroup.MarginLayoutParams params = (MarginLayoutParams) mExpandedAreaLayout
//                        .getLayoutParams();
//                params.setMargins(params.leftMargin, tag.getBottom(),
//                        params.rightMargin, params.bottomMargin);
//                mExpandedAreaLayout.addView(expandedArea);
//                mExpandedAreaLayout.setVisibility(View.VISIBLE);
//                return;
//            }
//        }
//        mExpandedAreaLayout.setVisibility(View.GONE);
    }

    /**
     * Set the input allowed state. The {@link com.blackberry.widgets.tagview.WrapTagListView
     * .OnInputStateChangedListener} will be notified of changes.
     *
     * @param inputAllowed Whether or not input is currently allowed
     * @see #setOnInputStateChangedListener(com.blackberry.widgets.tagview.WrapTagListView
     * .OnInputStateChangedListener)
     */
    private void setInputAllowed(boolean inputAllowed) {
        if (mInputAllowed != inputAllowed) {
            mInputAllowed = inputAllowed;
            if (mOnInputStateChangedListener != null) {
                mOnInputStateChangedListener.onInputStateChanged(mInputAllowed);
            }
        }
    }

    /**
     * @return Whether or not this control is currently read only.
     */
    public boolean isReadOnly() {
        return mReadOnly;
    }

    /**
     * @param readOnly The read-only state to set
     */
    public void setReadOnly(boolean readOnly) {
        if (mReadOnly != readOnly) {
            mReadOnly = readOnly;
            for (int i = 0; i < mFlowLayout.getChildCount(); i += 1) {
                View v = mFlowLayout.getChildAt(i);
                if (v instanceof IReadOnly) {
                    ((IReadOnly) v).setReadOnly(readOnly);
                }
            }
            if (mOnInputStateChangedListener != null) {
                mOnInputStateChangedListener.onInputStateChanged(mInputAllowed);
            }
        }
    }

    /**
     * The listener used to catch change notifications from the {@link Adapter}
     */
    private class AdapterDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            updateTagListView();
        }

        @Override
        public void onInvalidated() {
            updateTagListView();
        }
    }

    /**
     * @author tallen
     *         <p/>
     *         Used to register as a click listener on child views
     */
    private class ViewOnClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            int childIndexBeforeCollapse = mFlowLayout.indexOfChild(v);
            setCollapsed(false);
            int childIndexAfterCollapse = mFlowLayout.indexOfChild(v);
            View childClicked;
            if (childIndexAfterCollapse >= 0) {
                childClicked = v;
            } else {
                childClicked = mFlowLayout.getChildAt(childIndexBeforeCollapse);
            }
            OnItemClickListener.ItemClickEvent event =
                    new OnItemClickListener.ItemClickEvent(childIndexBeforeCollapse,
                            mAdapter.getItem(childIndexBeforeCollapse));
            if (mItemClickListener != null) {
                mItemClickListener.onItemClick(childClicked, event);
            }
            if (event.getSelectItem()) {
                toggleItemselection(childClicked);
            }
            requestFocus();
            setInputAllowed(!childClicked.isSelected());
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (mOnSizeChangedListener != null) {
            mOnSizeChangedListener.onSizeChanged(w, h, oldw, oldh);
        }
        super.onSizeChanged(w, h, oldw, oldh);
    }

    /**
     * Register a listener for when this View's size changes.
     *
     * @param listener The listener to register
     * @see {@link com.blackberry.widgets.tagview.WrapTagListView.OnSizeChangedListener}
     */
    public void setOnSizeChangedListener(OnSizeChangedListener listener) {
        this.mOnSizeChangedListener = listener;
    }

    /**
     * An interface to notify listeners of size changes to this control
     *
     * @see #setOnSizeChangedListener(com.blackberry.widgets.tagview.WrapTagListView.OnSizeChangedListener)
     */
    public static interface OnSizeChangedListener {
        /**
         * Called when this control's size changes
         *
         * @param width     The new width
         * @param height    The new height
         * @param oldWidth  The old width
         * @param oldHeight The old height
         */
        void onSizeChanged(int width, int height, int oldWidth, int oldHeight);
    }

    /**
     * This interface is so users can listen for when any input controls should be shown or hidden.
     *
     * @see #setOnInputStateChangedListener(com.blackberry.widgets.tagview.WrapTagListView
     * .OnInputStateChangedListener)
     */
    public static interface OnInputStateChangedListener {
        /**
         * @param isInInputState true if this control has entered the input state or false
         *                       otherwise
         */
        void onInputStateChanged(boolean isInInputState);
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

        ss.mMaxTagsWhenCollapsed = mMaxTagsWhenCollapsed;
        ss.mCollapsed = mCollapsed;
        ss.mReadOnly = mReadOnly;

        if (mAdapter instanceof ISaveInstanceState) {
            ss.mAdapterSaveData = ((ISaveInstanceState) mAdapter).onSaveInstanceState();
        }

        SparseArray<Parcelable> viewSaveData = new SparseArray<Parcelable>(mFlowLayout
                .getChildCount());

        SparseArray<Object> childrenSaveData = new SparseArray<Object>(mFlowLayout.getChildCount());
        for (int i = 0; i < mFlowLayout.getChildCount(); i++) {
            View v = mFlowLayout.getChildAt(i);
            v.setId(i);
            v.saveHierarchyState(viewSaveData);
            Parcelable p = viewSaveData.get(i);
            childrenSaveData.append(i, p);
        }
        ss.mChildrenSaveData = childrenSaveData;

        return ss;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        setMaxTagsWhenCollapsed(ss.mMaxTagsWhenCollapsed);
        setCollapsed(ss.mCollapsed);
        setReadOnly(ss.mReadOnly);

        if ((mAdapter instanceof ISaveInstanceState) && (ss.mAdapterSaveData != null)) {
            ((ISaveInstanceState) mAdapter).onRestoreInstanceState(ss.mAdapterSaveData);
        }

        SparseArray<Parcelable> viewSaveData = new SparseArray<Parcelable>(ss.mChildrenSaveData
                .size());
        for (int i = 0; i < ss.mChildrenSaveData.size(); i++) {
            View v = mFlowLayout.getChildAt(i);
            if (v == null) {
                break;
            }
            int originalId = v.getId();
            v.setId(i);
            Parcelable p = (Parcelable) (ss.mChildrenSaveData.valueAt(i));
            viewSaveData.append(i, p);
            v.restoreHierarchyState(viewSaveData);
            v.setId(originalId);
        }
    }

    /**
     * Used for saving state
     */
    static class SavedState extends BaseSavedState {
        private int mMaxTagsWhenCollapsed = 4;
        private boolean mCollapsed = true;
        private boolean mReadOnly = false;
        private Parcelable mAdapterSaveData = null;
        private SparseArray<Object> mChildrenSaveData = null;

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
            mMaxTagsWhenCollapsed = in.readInt();
            mCollapsed = in.readByte() != 0;
            mReadOnly = in.readByte() != 0;
            if (in.readByte() != 0) {
                mAdapterSaveData = in.readParcelable(null);
            }
            mChildrenSaveData = in.readSparseArray(null);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(mMaxTagsWhenCollapsed);
            dest.writeByte((byte) (mCollapsed ? 1 : 0));
            dest.writeByte((byte) (mReadOnly ? 1 : 0));
            if (mAdapterSaveData != null) {
                dest.writeByte((byte) 1);
                dest.writeParcelable(mAdapterSaveData, 0);
            } else {
                dest.writeByte((byte) 0);
            }
            dest.writeSparseArray(mChildrenSaveData);
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
