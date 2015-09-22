package com.blackberry.widgets.tagview;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;

import com.blackberry.widgets.R;

/**
 * A one-row horizontal list of Views.
 * <p/>
 * As many views as can be fit on this one row will be visible. The remainder will be hidden.
 *
 * @author tallen
 */
public class RelatedTagListView extends ViewGroup {
    /**
     * A listener to register for clicks to items in this ListView
     */
    OnItemClickListener mItemClickListener;
    /**
     * The internal listener attached to any child View in this ListView
     */
    ViewOnClickListener mViewClickListener = new ViewOnClickListener();
    /**
     * The Adapter providing items for this ListView
     */
    private Adapter mAdapter = null;
    /**
     * The listener registered for change notifications on #mAdapter
     */
    private AdapterDataSetObserver mObserver = new AdapterDataSetObserver();
    /**
     * The number of children which will be laid out
     */
    private int mChildrenToLayout = 0;
    /**
     * The horizontal spacing attribute to be used between child Views
     */
    private int mHorizontalSpacing = 0;

    /**
     * @param context The context
     */
    public RelatedTagListView(Context context) {
        this(context, null);
    }

    /**
     * @param context The context
     * @param attrs   The xml attributes
     */
    public RelatedTagListView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FlowLayout);
        try {
            mHorizontalSpacing = a.getDimensionPixelSize(R.styleable
                    .RelatedTagListView_horizontalSpacing, 0);
        } finally {
            a.recycle();
        }
    }

    /**
     * @return The adapter currently used to display data in this ListView.
     * @see #setAdapter(Adapter)
     */
    public Adapter getAdapter() {
        return mAdapter;
    }

    /**
     * Sets the data behind this ListView.
     *
     * @param adapter The ListAdapter which is responsible for maintaining the data backing this
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
     * Call this when we need to update the ListView (ie when the Adapter data has changed)
     */
    private void updateTagListView() {
        removeAllViews();
        if (mAdapter == null) {
            return;
        }
        for (int i = 0; i < mAdapter.getCount(); i += 1) {
            View v = mAdapter.getView(i, null, this);
            v.setOnClickListener(mViewClickListener);
            addView(v);
        }
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
     * Get the measured width of the child including margins.
     *
     * @param child The child to measure
     * @return The measured width of the child, including left and right margins.
     */
    private int getMeasuredWidthWithMargins(View child) {
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        return child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
    }

    /**
     * Get the measured height of the child including margins.
     *
     * @param child The child to measure
     * @return The measured height of the child, including top and bottom margins.
     */
    private int getMeasuredHeightWithMargins(View child) {
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        return child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int finalWidth = MeasureSpec.getSize(widthMeasureSpec);

        int widthLessRightPadding = finalWidth - getPaddingRight();
        int currentRowWidth = getPaddingLeft();
        int currentLeft;
        int currentHeight = getPaddingTop();

        mChildrenToLayout = getChildCount();

        for (int i = 0; i < getChildCount(); i += 1) {
            View child = getChildAt(i);

            if (child.getVisibility() == View.GONE) {
                continue;
            }

            measureChildWithMargins(child, widthMeasureSpec, currentRowWidth, heightMeasureSpec,
                    currentHeight);
            currentLeft = currentRowWidth;
            currentRowWidth += getMeasuredWidthWithMargins(child);
            currentHeight = Math.max(currentHeight,
                    getPaddingTop() + getMeasuredHeightWithMargins(child));

            if (currentRowWidth >= widthLessRightPadding) {
                mChildrenToLayout = i;
                break;
            } else {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                lp.x = currentLeft;
                lp.y = getPaddingTop();
                currentRowWidth += mHorizontalSpacing;
            }
        }

        setMeasuredDimension(finalWidth + getPaddingRight(), currentHeight + getPaddingBottom());
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childrenToLayout = Math.min(mChildrenToLayout, getChildCount());

        for (int i = 0; i < childrenToLayout; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == View.GONE) {
                continue;
            }
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            child.layout(lp.x, lp.y, lp.x + child.getMeasuredWidth(),
                    lp.y + child.getMeasuredHeight());
        }
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    /**
     * The layout parameters to be attached to all children of this layout.
     */
    public static class LayoutParams extends MarginLayoutParams {
        /**
         * The x location to layout the view.
         *
         * @see #onMeasure(int, int)
         * @see #onLayout(boolean, int, int, int, int)
         */
        int x;
        /**
         * The y location to layout the view.
         *
         * @see #onMeasure(int, int)
         * @see #onLayout(boolean, int, int, int, int)
         */
        int y;

        /**
         * @param c     The context
         * @param attrs The xml attributes
         */
        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        /**
         * @param width  The width to use
         * @param height The height to use
         * @see {@link android.view.ViewGroup.MarginLayoutParams}
         */
        public LayoutParams(int width, int height) {
            super(width, height);
        }

        /**
         * @param source The layout params to copy from
         */
        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }

        /**
         * @param source The layout params to copy from
         */
        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }

    /**
     * A class used to listen for changes to the Adapter and update the ListView
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
            if (mItemClickListener != null) {
                int childIndex = indexOfChild(v);
                mItemClickListener.onItemClick(v,
                        new OnItemClickListener.ItemClickEvent(childIndex,
                                mAdapter.getItem(childIndex)));
            }
        }
    }
}
