
package com.blackberry.widgets.tagview;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Adapter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.blackberry.widgets.R;
import com.blackberry.widgets.tagview.TagTextView.OnCreateTagDataItemRequest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A simple control which contains 3 vertically stacked sections:
 * <ol>
 * <li>Tags section: a horizontally-wrapped list of View objects</li>
 * <li>Related Tags section: a one-row horizontal list of View objects</li>
 * <li>Completions section: a vertical list of View objects</li>
 * </ol>
 * All View objects are provided by Adapters.
 *
 * @author tallen
 */
public abstract class BaseTags<T> extends LinearLayout implements IReadOnly {

    /**
     * The View showing the Title
     */
    private TextView mTitleTextView;
    /**
     * The View containing the wrapped-list of Tag Views.
     */
    private TagTextView mTagTextView;
    /**
     * The View containing the related Tag Views
     */
    private RelatedTagListView mRelatedTagListView;
    /**
     * The flags currently set
     *
     * @see #getAutoGenerateTagFlags()
     * @see #setAutoGenerateTagFlags(int)
     */
    private int mAutoGenerateTagFlags = AUTO_TAG_NONE;
    /**
     * Whether or not a child of this control has focus
     */
    private boolean mChildHasFocus = false;
    /**
     * The Soft Focus Adapter to optionally use
     */
    private SoftFocusAdapter mSoftFocusAdapter = null;
    /**
     * Whether or not to use Soft Focus
     *
     * @see #useSoftFocus()
     * @see #setUseSoftFocus(boolean)
     */
    private boolean mUseSoftFocus = true;
    /**
     * The Listener for changes to the completions adapter
     */
    private DataSetObserver mCompletionsAdapterObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            updateChildrensVisibility();
        }

        @Override
        public void onInvalidated() {
            updateChildrensVisibility();
        }
    };
    private OnTagListChanged<BaseTag> mTagTextViewTagListChanged = new OnTagListChanged<BaseTag>() {

        @Override
        public void tagRemoved(BaseTag tag) {
            if (mOnTagListChanged != null) {
                T dataItem = castToDataItem(tag.getData());
                if (dataItem != null) {
                    mOnTagListChanged.tagRemoved(dataItem);
                }
            }
        }

        @Override
        public void tagChanged(BaseTag tag) {
            if (mOnTagListChanged != null) {
                T dataItem = castToDataItem(tag.getData());
                if (dataItem != null) {
                    mOnTagListChanged.tagChanged(dataItem);
                }
            }
        }

        @Override
        public void tagAdded(BaseTag tag) {
            if (mOnTagListChanged != null) {
                T dataItem = castToDataItem(tag.getData());
                if (dataItem != null) {
                    mOnTagListChanged.tagAdded(dataItem);
                }
            }
        }
    };
    private Class<T> mDataClass;
    private OnTagListChanged<T> mOnTagListChanged;

    /**
     * Do not auto tag.
     * <p/>
     * Use with {@link #setAutoGenerateTagFlags(int)}
     */
    public static final int AUTO_TAG_NONE = 0x0000;

    /**
     * When focus is lost attempt to create a tag from the visible text
     * <p/>
     * Use with {@link #setAutoGenerateTagFlags(int)}
     */
    public static final int AUTO_TAG_ON_FOCUS_LOST = 0x0001;

    /**
     * Valid tags Use with {@link #getTagValues(int)}
     */
    public static final int TAG_VALID = 0x01;
    /**
     * Invalid tags Use with {@link #getTagValues(int)}
     */
    public static final int TAG_INVALID = 0x02;
    /**
     * Valid and Invalid tags Use with {@link #getTagValues(int)}
     */
    public static final int TAG_ALL = TAG_VALID | TAG_INVALID;

    /**
     * Subclasses should use this constructor.
     * 
     * @param context
     * @param attrs
     * @param tagClass
     * @param itemClass
     */
    protected BaseTags(Context context, AttributeSet attrs, Class<? extends BaseTag> tagClass,
            Class<T> dataClass) {
        this(context, attrs, R.layout.base_tag_view, tagClass, dataClass);
    }

    protected BaseTags(Context context, AttributeSet attrs, int layout,
            Class<? extends BaseTag> tagClass, Class<T> dataClass) {
        super(context, attrs);

        setFocusable(false);

        setOrientation(LinearLayout.VERTICAL);

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(layout, this, true);

        mTitleTextView = (TextView) findViewById(R.id.titleTextView);
        mTagTextView = (TagTextView) findViewById(R.id.btvTagTextView);

        // we have to do this so we can manually handle focus on orientation
        // changes.
        mTagTextView.setId(View.NO_ID);

        mRelatedTagListView = (RelatedTagListView) findViewById(R.id.btvRelatedTagListView);

        setTagClass(tagClass);
        mDataClass = dataClass;

        mTagTextView.setOnTagListChanged(mTagTextViewTagListChanged);
        mTagTextView.setOnCreateTagDataItemRequest(new OnCreateTagDataItemRequest() {

            @Override
            public Object createTag(CharSequence inputText) {
                return BaseTags.this.createTagDataItem(inputText);
            }
        });

        // Show/hide the related and completions based on focus.
        mRelatedTagListView.setVisibility(View.GONE);

        init(attrs);
    }

    /**
     * Initialize this View by parsing the relevant XML attributes
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
                if (attr == R.styleable.BaseTags_auto_generate_tag_flags) {
                    setAutoGenerateTagFlags(a.getInt(attr, AUTO_TAG_NONE));
                } else if (attr == R.styleable.BaseTags_max_tags_when_collapsed) {
                    int value = a.getInt(attr, Integer.MIN_VALUE);
                    if (value > Integer.MIN_VALUE) {
                        setMaxTagsWhenCollapsed(value);
                    }
                } else if (attr == R.styleable.BaseTags_read_only) {
                    setReadOnly(a.getBoolean(attr, false));
                } else if (attr == R.styleable.BaseTags_tags_title) {
                    setTitle(a.getString(attr));
                } else if (attr == R.styleable.BaseTags_use_soft_focus) {
                    setUseSoftFocus(a.getBoolean(attr, true));
                } else if (attr == R.styleable.BaseTags_drag_group) {
                    setDragGroup(a.getString(attr));
                }
            }
        } finally {
            a.recycle();
        }
    }

    /**
     * @return The adapter currently used to display data in the Related Tags
     *         section
     * @see #setRelatedTagsAdapter(Adapter)
     */
    public Adapter getRelatedTagsAdapter() {
        return mRelatedTagListView.getAdapter();
    }

    /**
     * Sets the data behind the Related Tags section
     *
     * @param adapter The Adapter which is responsible for maintaining the data
     *            backing the Related Tags section and for producing a view to
     *            represent an item in that data set
     * @see #getRelatedTagsAdapter()
     */
    public void setRelatedTagsAdapter(Adapter adapter) {
        mRelatedTagListView.setAdapter(adapter);
    }

    /**
     * @return The adapter currently used to display data in the Completions
     *         section
     * @see #setCompletionsAdapter(ListAdapter)
     */
    public ListAdapter getCompletionsAdapter() {
        return mTagTextView.getAdapter();
    }

    /**
     * Sets the data behind the Completions section
     *
     * @param adapter The Adapter which is responsible for maintaining the data
     *            backing the Tags section and for producing a view to represent
     *            an item in that data set
     * @see #getCompletionsAdapter()
     */
    public <T extends ListAdapter & Filterable> void setCompletionsAdapter(T adapter) {
        ListAdapter finalAdapter;
        if (mUseSoftFocus) {
            if (mSoftFocusAdapter == null) {
                mSoftFocusAdapter = new SoftFocusAdapter();
            }
            mSoftFocusAdapter.setWrappedAdapter(adapter);
            finalAdapter = mSoftFocusAdapter;
        } else {
            finalAdapter = adapter;
        }
        // Adapter oldAdapter = mTagTextView.getAdapter();
        // if (oldAdapter != null) {
        // oldAdapter.unregisterDataSetObserver(mCompletionsAdapterObserver);
        // }
        // if (finalAdapter != null) {
        // finalAdapter.registerDataSetObserver(mCompletionsAdapterObserver);
        // }
        mTagTextView.setAdapter(adapter);
    }

    /**
     * Returns the auto generate flags
     *
     * @return One of {@link #AUTO_TAG_NONE} or {@link #AUTO_TAG_ON_FOCUS_LOST}
     */
    public int getAutoGenerateTagFlags() {
        return mAutoGenerateTagFlags;
    }

    /**
     * Set the auto generate flags
     *
     * @param flags One of {@link #AUTO_TAG_NONE} or
     *            {@link #AUTO_TAG_ON_FOCUS_LOST}
     */
    public void setAutoGenerateTagFlags(int flags) {
        mAutoGenerateTagFlags = flags;
    }

    /**
     * @return The maximum number of tags to show when collapsed.
     * @see #setMaxTagsWhenCollapsed(int)
     */
    public int getMaxTagsWhenCollapsed() {
        return 0;// mWrapTagListView.getMaxTagsWhenCollapsed();
    }

    /**
     * Set the maximum number of tags to show when Collapsed. There will be an
     * indicator after the last visible tag showing there are hidden tags.
     * <p/>
     * The control is considered Collapsed when it does not have focus.
     *
     * @param maxTagsWhenCollapsed The maximum number of tags to show when
     *            collapsed not including the "More" tag. Setting to <=0
     *            disables this option. The default value is 4.
     * @see #getMaxTagsWhenCollapsed()
     */
    public void setMaxTagsWhenCollapsed(int maxTagsWhenCollapsed) {
        mTagTextView.setMaxTagsWhenCollapsed(maxTagsWhenCollapsed);
    }

    /**
     * @return Whether or not the control is read-only
     * @see #setReadOnly(boolean)
     */
    public boolean isReadOnly() {
        return mTagTextView.isReadOnly();
    }

    /**
     * Set the read-only state of this View.
     * <p/>
     * When read-only Tags cannot be added or removed via the UI. The API will
     * still allow adding/removing items.
     *
     * @param readOnly The read-only state to set
     * @see #isReadOnly()
     */
    public void setReadOnly(boolean readOnly) {
        mTagTextView.setReadOnly(readOnly);
        updateChildrensVisibility();
    }

    /**
     * @return Whether or not to use soft focus
     */
    public boolean useSoftFocus() {
        return mUseSoftFocus;
    }

    /**
     * Set the soft focus state.
     * <p/>
     * Soft Focus is when the first item in the Completions list is highlighted.
     * If the user presses enter with a soft focused item it will add that item
     * instead of the text in the input field
     *
     * @param useSoftFocus The soft focus state to set
     */
    public void setUseSoftFocus(boolean useSoftFocus) {
        if (this.mUseSoftFocus != useSoftFocus) {
            this.mUseSoftFocus = useSoftFocus;
            fixCompletionsAdapter();
        }
    }

    /**
     * @return The title to show in this View
     */
    public String getTitle() {
        return mTitleTextView.getText().toString();
    }

    /**
     * Set the title to show in this View
     *
     * @param title The title to show
     */
    public void setTitle(String title) {
        mTitleTextView.setText(title);
    }

    /**
     * @param listener Set the item click listener.
     */
    public void setOnTagClickListener(OnItemClickListener listener) {
        // mWrapTagListView.setOnItemClickListener(listener);
    }

    private T castToDataItem(Object o) {
        T dataItem = null;
        try {
            dataItem = mDataClass.cast(o);
        } catch (ClassCastException ex) {
        }
        return dataItem;
    }

    /**
     * Return the list of tags contained in this instance based on the given
     * validity.
     * <p/>
     * 
     * @param validity What type of tags to return. Must be one of
     *            {@link #TAG_VALID}, {@link #TAG_INVALID}, {@link #TAG_ALL}.
     * @return The list of tag values in this instance filtered by validity
     * @see #getTagValues()
     */
    public List<T> getTagValues(int validity) {
        List<BaseTag> tags = mTagTextView.getTags();

        List<T> result = new ArrayList<T>(tags.size());

        for (BaseTag tag : tags) {
            if (matchesValidity(tag, validity)) {
                T dataItem = castToDataItem(tag.getData());
                if (dataItem != null) {
                    result.add(dataItem);
                }
            }
        }
        return result;
    }

    /**
     * @return The list of all tag values in this instance.
     * @see #getTagValues(int)
     */
    public final List<T> getTagValues() {
        return getTagValues(TAG_ALL);
    }

    private boolean matchesValidity(BaseTag tag, int validity) {
        return (tag.isValid() && ((validity & TAG_VALID) == TAG_VALID))
                || (!tag.isValid() && ((validity & TAG_INVALID) == TAG_INVALID));
    }

    /**
     * Add a given item to the end of the current list of items
     *
     * @param item The item to add
     */
    public void add(T item) {
        addTag(item);
    }

    /**
     * Add a given item to the end of the current list of items.
     * 
     * @param item The data item to add
     */
    protected void addTag(Object item) {
        mTagTextView.addTag(item);
    }

    /**
     * Add all items to the end of the current list of items
     *
     * @param items The items to add
     */
    public void addAll(Collection<? extends T> items) {
        for (T item : items) {
            addTag(item);
        }
    }

    /**
     * Add all items to the end of the current list of items
     *
     * @param items The items to add
     */
    public void addAll(T... items) {
        for (T item : items) {
            addTag(item);
        }
    }

    /**
     * Insert the item into the specified position
     * <p/>
     * If it is greater than or equal to the size of the current list item will
     * be appended to the end of the list
     *
     * @param item The item to insert
     * @param index The 0-based index to insert into.
     */
    public void insert(T item, int index) {
        throw new UnsupportedOperationException("Not Implemented Yet");
    }

    /**
     * Remove the item from the list
     *
     * @param item The item to remove
     */
    public void remove(T item) {
        throw new UnsupportedOperationException("Not Implemented Yet");
    }

    /**
     * Clear all items from the list
     */
    public void clear() {
        mTagTextView.setText("");
    }

    /**
     * Set the proper completions adapter based on Soft Focus state.
     *
     * @see #setUseSoftFocus(boolean)
     */
    private void fixCompletionsAdapter() {
        if (mSoftFocusAdapter == null) {
            // setCompletionsAdapter(getCompletionsAdapter());
        } else {
            // We can safely cast this here because the only place it is ever
            // set in
            // mSoftFocusAdapter is inside setCompletionsAdapter which requires
            // a ListAdapter to
            // begin with.
            // setCompletionsAdapter((ListAdapter)
            // mSoftFocusAdapter.getWrappedAdapter());
        }
    }

    /**
     * Get the RelatedTagListView used by this View. Useful for connecting to
     * notifiers in subclasses
     *
     * @return The related View
     * @see RelatedTagListView
     */
    protected RelatedTagListView getRelatedTagListView() {
        return mRelatedTagListView;
    }

    /**
     * Called when the child focus state changes. This includes all children in
     * the tree below this View not just the direct children.
     *
     * @param childHasFocus Whether or not a child of this class has focus
     */
    protected void onChildFocusUpdate(boolean childHasFocus) {
        // if (mChildHasFocus != childHasFocus) {
        // mChildHasFocus = childHasFocus;
        // if ((!mChildHasFocus) && ((mAutoGenerateTagFlags &
        // AUTO_TAG_ON_FOCUS_LOST) ==
        // AUTO_TAG_ON_FOCUS_LOST)) {
        // createTag();
        // }
        // updateChildrensVisibility();
        // }
    }

    /**
     * Called when the visibility of the direct children should be re-set as
     * appropriate. Sub classes can override this to perform their own logic but
     * MUST call the super implementation
     */
    protected void updateChildrensVisibility() {
        if (mChildHasFocus && !isReadOnly()) {
            // mCompletionsListView.setVisibility(View.VISIBLE);
            // Adapter completionsAdapter = mCompletionsListView.getAdapter();
            // if ((completionsAdapter == null) ||
            // (completionsAdapter.getCount() == 0)) {
            // mRelatedTagListView.setVisibility(View.VISIBLE);
            // } else {
            // mRelatedTagListView.setVisibility(View.GONE);
            // }
        } else {
            // mCompletionsListView.setVisibility(View.GONE);
            mRelatedTagListView.setVisibility(View.GONE);
        }
    }

    /**
     * Clear the input text
     */
    public void clearText() {
        if (mTagTextView != null) {
            mTagTextView.clearComposingText();
        }
    }

    /**
     * Called by BaseTags when the subclass needs to attempt to create a tag
     * from a String. Subclasses can use this method to decide whether or not to
     * allow or disallow creation of a tag from a String.
     *
     * @param inputText The text to attempt to create a tag from
     * @return The data object if a tag is to be created or null otherwise
     */
    protected abstract T createTagDataItem(CharSequence inputText);

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_UP:
                // mWrapTagListView.requestFocus();
                return true;
        }
        return super.onTouchEvent(event);
    }

    /**
     * Sub-classes should call this method when a soft-focused item should be
     * clicked. For instance a sub-class with an EditText for input would call
     * this method when the Go/Enter key is pressed.
     *
     * @return true if a soft focused item was clicked or false otherwise (ie
     *         soft focus is disabled or no item has soft focus).
     * @see #useSoftFocus()
     * @see #setUseSoftFocus(boolean)
     */
    protected boolean clickSoftFocusedItem() {
        if ((mUseSoftFocus) && (mSoftFocusAdapter != null) && (mSoftFocusAdapter.getCount() > 0)) {
            // for now the first item is always soft-focused, if it exists
            // mCompletionsListView.getListView().performItemClick(null, 0, 0);
            return true;
        }
        return false;
    }

    @Override
    protected void dispatchSaveInstanceState(SparseArray<Parcelable> container) {
        // DO NOT call the super. This blocks the child Views from being saved
        // by the default
        // implementation (which is bad for compound views). Save their instance
        // state ourselves.
        // super.dispatchSaveInstanceState(container);

        super.dispatchFreezeSelfOnly(container);
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        // DO NOT call the super. This blocks the child Views from being saved
        // by the default
        // implementation (which is bad for compound views). Save their instance
        // state ourselves.
        // super.dispatchRestoreInstanceState(container);

        super.dispatchThawSelfOnly(container);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState ss = new SavedState(super.onSaveInstanceState());

        ss.mTagTextViewSaveData = mTagTextView.onSaveInstanceState();
        ss.mIsTagTextViewFocused = mTagTextView.isFocused();
        // ss.mCompletionsListViewSaveData =
        // mCompletionsListView.onSaveInstanceState();

        return ss;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        mTagTextView.onRestoreInstanceState(ss.mTagTextViewSaveData);
        if (ss.mIsTagTextViewFocused) {
            mTagTextView.requestFocus();
        }

        // mCompletionsListView.onRestoreInstanceState(ss.mCompletionsListViewSaveData);
    }

    /**
     * used for saving state
     */
    static class SavedState extends BaseSavedState {
        private Parcelable mTagTextViewSaveData = null;
        private Parcelable mCompletionsListViewSaveData = null;
        private boolean mIsTagTextViewFocused = false;

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
            mTagTextViewSaveData = in.readParcelable(null);
            mCompletionsListViewSaveData = in.readParcelable(null);
            mIsTagTextViewFocused = in.readInt() != 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeParcelable(mTagTextViewSaveData, 0);
            dest.writeParcelable(mCompletionsListViewSaveData, 0);
            dest.writeInt(mIsTagTextViewFocused ? 1 : 0);
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

    /**
     * @param tagClass The class to use to represent the objects in the list.
     */
    protected final void setTagClass(Class<? extends BaseTag> tagClass) {
        mTagTextView.setTagClass(tagClass);
    }

    public OnTagListChanged<T> getOnTagListChanged() {
        return mOnTagListChanged;
    }

    public void setOnTagListChanged(OnTagListChanged<T> onTagListChanged) {
        mOnTagListChanged = onTagListChanged;
    }

    /**
     * The drag group is a string which is used to allow drag and drop of tags
     * from different TagTextView controls. The drag group from the source
     * TagTextView must match the drag group from the destination TagTextView or
     * the drop will not be allowed.
     * 
     * @return The drag group for this control.
     * @see #setDragGroup(String)
     */
    public String getDragGroup() {
        return mTagTextView.getDragGroup();
    }

    /**
     * Sets the drag group.
     * 
     * @param dragGroup The new drag group to set.
     * @see #getDragGroup()
     */
    public void setDragGroup(String dragGroup) {
        mTagTextView.setDragGroup(dragGroup);
    }
}
