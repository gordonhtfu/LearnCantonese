/*
 * Copyright (C) 2013 Sergej Shafarenka, halfbit.de
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file kt in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.blackberry.common.ui.list;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;
import android.widget.AbsListView;
import android.widget.HeaderViewListAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SectionIndexer;

import com.blackberry.common.ui.BuildConfig;

/**
 * ListView, which has items that are sticky. Sticky items will scroll to the top and stay there
 * until another sticky items pushes it off screen.
 */
// bratta This class was formally know as PinnedSectionListView
public class StickySectionListView extends ListView {

    // -- inner classes

    /** List adapter to be implemented for being used with StickySectionListView adapter. */
    public interface StickySectionListAdapter extends ListAdapter {

        /**
         * This method shall return 'true' if views of given type is sticky.
         * 
         * @param viewType the view type
         * @return true, if is item view type is sticky
         */
        boolean isItemViewTypeSticky(int viewType);
    }

    /** Wrapper class for sticky section view and its position in the list. */
    static class StickySection {
        public View view;
        public int position;
        public long id;
    }

    // -- class fields

    // fields used for handling touch events
    private final Rect mTouchRect = new Rect();
    private final PointF mTouchPoint = new PointF();
    private int mTouchSlop;
    private View mTouchTarget;
    private MotionEvent mDownEvent;

    // fields used for drawing shadow under a sticky section
    private GradientDrawable mShadowDrawable;
    private int mSectionsDistanceY;
    private int mShadowHeight;

    /** Delegating listener, can be null. */
    OnScrollListener mDelegateOnScrollListener;

    /** Shadow for being recycled, can be null. */
    StickySection mRecycleSection;

    /** shadow instance with a sticky view, can be null. */
    StickySection mStickySection;

    /** Sticky view Y-translation. We use it to stick sticky view to the next section. */
    int mTranslateY;

    /** Scroll listener which does the magic. */
    private final OnScrollListener mOnScrollListener = new OnScrollListener() {

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            if (mDelegateOnScrollListener != null) { // delegate
                mDelegateOnScrollListener.onScrollStateChanged(view, scrollState);
            }
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                int totalItemCount) {

            if (mDelegateOnScrollListener != null) { // delegate
                mDelegateOnScrollListener.onScroll(view, firstVisibleItem, visibleItemCount,
                        totalItemCount);
            }

            // get expected adapter or fail fast
            ListAdapter adapter = getAdapter();
            if (adapter == null || visibleItemCount == 0) {
                return; // nothing to do
            }

            final boolean isFirstVisibleItemSection =
                    isItemViewTypeSticky(adapter, adapter.getItemViewType(firstVisibleItem));

            if (isFirstVisibleItemSection) {
                View sectionView = getChildAt(0);
                if (sectionView.getTop() == getPaddingTop()) { // view sticks to the top, no need
                    // for sticky shadow
                    destroyStickyShadow();
                } else { // section doesn't stick to the top, make sure we have a sticky shadow
                    ensureShadowForPosition(firstVisibleItem, firstVisibleItem, visibleItemCount);
                }

            } else { // section is not at the first visible position
                int sectionPosition = findCurrentSectionPosition(firstVisibleItem);
                if (sectionPosition > -1) { // we have section position
                    ensureShadowForPosition(sectionPosition, firstVisibleItem, visibleItemCount);
                } else { // there is no section for the first visible item, destroy shadow
                    destroyStickyShadow();
                }
            }
        };

    };

    /** Default change observer. */
    private final DataSetObserver mDataSetObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            removeAllViewsInLayout();
            recreateStickyShadow();
        };

        @Override
        public void onInvalidated() {
            removeAllViewsInLayout();
        }
    };

    // -- constructors

    public StickySectionListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public StickySectionListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    private void initView() {
        setOnScrollListener(mOnScrollListener);
        mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        initShadow(true);
    }

    // -- public API methods

    public void setShadowVisible(boolean visible) {
        initShadow(visible);
        if (mStickySection != null) {
            View v = mStickySection.view;
            invalidate(v.getLeft(), v.getTop(), v.getRight(), v.getBottom() + mShadowHeight);
        }
    }

    // -- sticky section drawing methods

    public void initShadow(boolean visible) {
        if (visible) {
            if (mShadowDrawable == null) {
                mShadowDrawable = new GradientDrawable(Orientation.TOP_BOTTOM, new int[] {
                        Color.parseColor("#ffe0e0e0"), Color.parseColor("#50e0e0e0"),
                        Color.parseColor("#00e0e0e0")
                });
                mShadowHeight = (int) (8 * getResources().getDisplayMetrics().density);
            }
        } else {
            if (mShadowDrawable != null) {
                mShadowDrawable = null;
                mShadowHeight = 0;
            }
        }
    }

    /*
     * Create shadow wrapper with a sticky view for a view at given position.
     */
    void createStickyShadow(int position) {

        // try to recycle shadow
        StickySection stickyShadow = mRecycleSection;
        mRecycleSection = null;

        // create new shadow, if needed
        if (stickyShadow == null) {
            stickyShadow = new StickySection();
        }
        // request new view using recycled view, if such
        View stickyView = getAdapter().getView(position, stickyShadow.view,
                StickySectionListView.this);

        // read layout parameters
        LayoutParams layoutParams = (LayoutParams) stickyView.getLayoutParams();
        if (layoutParams == null) { // create default layout params
            layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        }

        int heightMode = MeasureSpec.getMode(layoutParams.height);
        int heightSize = MeasureSpec.getSize(layoutParams.height);

        if (heightMode == MeasureSpec.UNSPECIFIED) {
            heightMode = MeasureSpec.EXACTLY;
        }

        int maxHeight = getHeight() - getListPaddingTop() - getListPaddingBottom();
        if (heightSize > maxHeight) {
            heightSize = maxHeight;
        }

        // measure & layout
        int ws = MeasureSpec.makeMeasureSpec(getWidth() - getListPaddingLeft()
                - getListPaddingRight(), MeasureSpec.EXACTLY);
        int hs = MeasureSpec.makeMeasureSpec(heightSize, heightMode);
        stickyView.measure(ws, hs);
        stickyView.layout(0, 0, stickyView.getMeasuredWidth(), stickyView.getMeasuredHeight());
        mTranslateY = 0;

        // initialize sticky shadow
        stickyShadow.view = stickyView;
        stickyShadow.position = position;
        stickyShadow.id = getAdapter().getItemId(position);

        // store sticky shadow
        mStickySection = stickyShadow;
    }

    /** Destroy shadow wrapper for currently sticky view. */
    void destroyStickyShadow() {
        if (mStickySection != null) {
            // keep shadow for being recycled later
            mRecycleSection = mStickySection;
            mStickySection = null;
        }
    }

    /** Makes sure we have an actual sticky shadow for given position. */
    void ensureShadowForPosition(int sectionPosition, int firstVisibleItem, int visibleItemCount) {
        if (visibleItemCount < 2) { // no need for creating shadow at all, we have a single visible
            // item
            destroyStickyShadow();
            return;
        }

        if (mStickySection != null
                && mStickySection.position != sectionPosition) { // invalidate shadow, if required
            destroyStickyShadow();
        }

        if (mStickySection == null) { // create shadow, if empty
            createStickyShadow(sectionPosition);
        }

        // align shadow according to next section position, if needed
        int nextPosition = sectionPosition + 1;
        if (nextPosition < getCount()) {
            int nextSectionPosition = findFirstVisibleSectionPosition(nextPosition,
                    visibleItemCount - (nextPosition - firstVisibleItem));
            if (nextSectionPosition > -1) {
                View nextSectionView = getChildAt(nextSectionPosition - firstVisibleItem);
                final int bottom = mStickySection.view.getBottom() + getPaddingTop();
                mSectionsDistanceY = nextSectionView.getTop() - bottom;
                if (mSectionsDistanceY < 0) {
                    // next section overlaps sticky shadow, move it up
                    mTranslateY = mSectionsDistanceY;
                } else {
                    // next section does not overlap with sticky, stick to top
                    mTranslateY = 0;
                }
            } else {
                // no other sections are visible, stick to top
                mTranslateY = 0;
                mSectionsDistanceY = Integer.MAX_VALUE;
            }
        }

    }

    int findFirstVisibleSectionPosition(int firstVisibleItem, int visibleItemCount) {
        ListAdapter adapter = getAdapter();
        for (int childIndex = 0; childIndex < visibleItemCount; childIndex++) {
            int position = firstVisibleItem + childIndex;
            int viewType = adapter.getItemViewType(position);
            if (isItemViewTypeSticky(adapter, viewType)) {
                return position;
            }
        }
        return -1;
    }

    int findCurrentSectionPosition(int fromPosition) {
        ListAdapter adapter = getAdapter();

        if (adapter instanceof SectionIndexer) {
            // try fast way by asking section indexer
            SectionIndexer indexer = (SectionIndexer) adapter;
            int sectionPosition = indexer.getSectionForPosition(fromPosition);
            int itemPosition = indexer.getPositionForSection(sectionPosition);
            int typeView = adapter.getItemViewType(itemPosition);
            if (isItemViewTypeSticky(adapter, typeView)) {
                return itemPosition;
            } // else, no luck
        }

        // try slow way by looking through to the next section item above
        for (int position = fromPosition; position >= 0; position--) {
            int viewType = adapter.getItemViewType(position);
            if (isItemViewTypeSticky(adapter, viewType)) {
                return position;
            }
        }
        return -1; // no candidate found
    }

    void recreateStickyShadow() {
        destroyStickyShadow();
        ListAdapter adapter = getAdapter();
        if (adapter != null && adapter.getCount() > 0) {
            int firstVisiblePosition = getFirstVisiblePosition();
            int sectionPosition = findCurrentSectionPosition(firstVisiblePosition);
            if (sectionPosition == -1) {
                return; // no sticky views, exit
            }
            ensureShadowForPosition(sectionPosition,
                    firstVisiblePosition, getLastVisiblePosition() - firstVisiblePosition);
        }
    }

    @Override
    public void setOnScrollListener(OnScrollListener listener) {
        if (listener == mOnScrollListener) {
            super.setOnScrollListener(listener);
        } else {
            mDelegateOnScrollListener = listener;
        }
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);
        post(new Runnable() {
            @Override
            public void run() { // restore sticky view after configuration change
                recreateStickyShadow();
            }
        });
    }

    @Override
    public void setAdapter(ListAdapter adapter) {

        // assert adapter in debug mode
        if (BuildConfig.DEBUG && adapter != null) {
            if (!(adapter instanceof StickySectionListAdapter)) {
                throw new IllegalArgumentException(
                        "Does your adapter implement StickySectionListAdapter?");
            }
            if (adapter.getViewTypeCount() < 2) {
                throw new IllegalArgumentException("Does your adapter handle at least two types"
                        + " of views in getViewTypeCount() method: items and sections?");
            }
        }

        // unregister observer at old adapter and register on new one
        ListAdapter oldAdapter = getAdapter();
        if (oldAdapter != null) {
            oldAdapter.unregisterDataSetObserver(mDataSetObserver);
        }
        if (adapter != null) {
            adapter.registerDataSetObserver(mDataSetObserver);
        }
        // destroy sticky shadow, if new adapter is not same as old one
        if (oldAdapter != adapter) {
            destroyStickyShadow();
        }

        super.setAdapter(adapter);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (mStickySection != null) {
            int parentWidth = r - l - getPaddingLeft() - getPaddingRight();
            int shadowWidth = mStickySection.view.getWidth();
            if (parentWidth != shadowWidth) {
                recreateStickyShadow();
            }
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        if (mStickySection != null) {

            // prepare variables
            int pLeft = getListPaddingLeft();
            int pTop = getListPaddingTop();
            View view = mStickySection.view;

            // draw child
            canvas.save();

            int clipHeight = view.getHeight()
                    + (mShadowDrawable == null ? 0 : Math.min(mShadowHeight, mSectionsDistanceY));
            canvas.clipRect(pLeft, pTop, pLeft + view.getWidth(), pTop + clipHeight);

            canvas.translate(pLeft, pTop + mTranslateY);
            drawChild(canvas, mStickySection.view, getDrawingTime());

            if (mShadowDrawable != null && mSectionsDistanceY > 0) {
                mShadowDrawable.setBounds(mStickySection.view.getLeft(),
                        mStickySection.view.getBottom(),
                        mStickySection.view.getRight(),
                        mStickySection.view.getBottom() + mShadowHeight);
                mShadowDrawable.draw(canvas);
            }

            canvas.restore();
        }
    }

    // -- touch handling methods

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {

        final float x = ev.getX();
        final float y = ev.getY();
        final int action = ev.getAction();

        if (action == MotionEvent.ACTION_DOWN
                && mTouchTarget == null
                && mStickySection != null
                && isStickyViewTouched(mStickySection.view, x, y)) { // create touch target

            // user touched sticky view
            mTouchTarget = mStickySection.view;
            mTouchPoint.x = x;
            mTouchPoint.y = y;

            // copy down event for eventually be used later
            mDownEvent = MotionEvent.obtain(ev);
        }

        if (mTouchTarget != null) {
            if (isStickyViewTouched(mTouchTarget, x, y)) { // forward event to sticky view
                mTouchTarget.dispatchTouchEvent(ev);
            }

            if (action == MotionEvent.ACTION_UP) { // perform onClick on sticky view
                super.dispatchTouchEvent(ev);
                performStickyItemClick();
                clearTouchTarget();

            } else if (action == MotionEvent.ACTION_CANCEL) { // cancel
                clearTouchTarget();

            } else if (action == MotionEvent.ACTION_MOVE) {
                if (Math.abs(y - mTouchPoint.y) > mTouchSlop) {

                    // cancel sequence on touch target
                    MotionEvent event = MotionEvent.obtain(ev);
                    event.setAction(MotionEvent.ACTION_CANCEL);
                    mTouchTarget.dispatchTouchEvent(event);
                    event.recycle();

                    // provide correct sequence to super class for further handling
                    super.dispatchTouchEvent(mDownEvent);
                    super.dispatchTouchEvent(ev);
                    clearTouchTarget();

                }
            }

            return true;
        }

        // call super if this was not our sticky view
        return super.dispatchTouchEvent(ev);
    }

    private boolean isStickyViewTouched(View view, float x, float y) {
        view.getHitRect(mTouchRect);

        // by taping top or bottom padding, the list performs on click on a border item.
        // we don't add top padding here to keep behavior consistent.
        mTouchRect.top += mTranslateY;

        mTouchRect.bottom += mTranslateY + getPaddingTop();
        mTouchRect.left += getPaddingLeft();
        mTouchRect.right -= getPaddingRight();
        return mTouchRect.contains((int) x, (int) y);
    }

    private void clearTouchTarget() {
        mTouchTarget = null;
        if (mDownEvent != null) {
            mDownEvent.recycle();
            mDownEvent = null;
        }
    }

    private boolean performStickyItemClick() {
        if (mStickySection == null) {
            return false;
        }

        OnItemClickListener listener = getOnItemClickListener();
        if (listener != null) {
            View view = mStickySection.view;
            playSoundEffect(SoundEffectConstants.CLICK);
            if (view != null) {
                view.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
            }
            listener.onItemClick(this, view, mStickySection.position, mStickySection.id);
            return true;
        }
        return false;
    }

    /**
     * Check if the viewType for an adapter is sticky.
     * 
     * @param adapter The adapter.
     * @param viewType The view type
     * @return return true, if is item view type sticky
     */
    public static boolean isItemViewTypeSticky(ListAdapter adapter, int viewType) {
        if (adapter instanceof HeaderViewListAdapter) {
            adapter = ((HeaderViewListAdapter) adapter).getWrappedAdapter();
        }
        return ((StickySectionListAdapter) adapter).isItemViewTypeSticky(viewType);
    }

}
