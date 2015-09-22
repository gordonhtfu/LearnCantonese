package com.blackberry.widgets.tagview;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.blackberry.widgets.R;

/**
 * A custom Layout to wrap Views to a new row when they go past the right edge of the area.
 */
class FlowLayout extends ViewGroup {
    private int mHorizontalSpacing = 0;
    private int mVerticalSpacing = 0;

    /**
     * @param context The context
     */
    public FlowLayout(Context context) {
        super(context);
    }

    /**
     * @param context The context
     * @param attrs   The xml attributes
     */
    public FlowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FlowLayout);
        try {
            mHorizontalSpacing = a.getDimensionPixelSize(R.styleable
                    .FlowLayout_horizontalSpacing, 0);
            mVerticalSpacing = a.getDimensionPixelSize(R.styleable.FlowLayout_verticalSpacing, 0);
        } finally {
            a.recycle();
        }
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
        int currentTop = getPaddingTop();
        int currentRowHeight = 0;
        int currentHeight = currentTop;

        for (int i = 0; i < getChildCount(); ) {
            View child = getChildAt(i);

            if (child.getVisibility() == View.GONE) {
                i += 1;
                continue;
            }

            measureChildWithMargins(child, widthMeasureSpec, currentRowWidth, heightMeasureSpec,
                    currentHeight);
            currentLeft = currentRowWidth;
            currentRowWidth += getMeasuredWidthWithMargins(child);

            if (currentRowWidth >= widthLessRightPadding) {
                currentLeft = getPaddingLeft();
                currentRowWidth = currentLeft;
                currentTop += currentRowHeight + mVerticalSpacing;
                currentRowHeight = 0;
            } else {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                lp.x = currentLeft;
                lp.y = currentTop;
                currentRowHeight = Math.max(currentRowHeight, getMeasuredHeightWithMargins
                        (child));
                if (lp.mFillRemainingWidth) {
                    int oldWidth = lp.width;
                    // If anyone can tell me why it locks up without the " - 1" below be my guest.
                    // My math says it should be right without it:
                    // eg. 5px total width. currentLeft = 2px. child width should be 3px. 5 - 2 = 3.
                    // but this causes the app to not respond on startup. The " - 1" fixes that.
                    lp.width = widthLessRightPadding - currentLeft - 1;
                    measureChildWithMargins(child, MeasureSpec.makeMeasureSpec(lp.width,
                            MeasureSpec.EXACTLY), currentRowWidth - getMeasuredWidthWithMargins
                            (child), heightMeasureSpec, currentHeight);
                    lp.width = oldWidth;
                    currentLeft = getPaddingLeft();
                    currentRowWidth = currentLeft;
                    currentTop += currentRowHeight + mVerticalSpacing;
                    currentRowHeight = 0;
                } else {
                    currentRowWidth += mHorizontalSpacing;
                }
                i += 1;
            }
        }

        setMeasuredDimension(finalWidth + getPaddingRight(), currentTop + currentRowHeight +
                getPaddingBottom());
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        for (int i = 0; i < getChildCount(); i++) {
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
         * Whether or not to fill the remaining width of the current row
         */
        public boolean mFillRemainingWidth = false;

        /**
         * @param c     The context
         * @param attrs The xml attributes
         */
        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.FlowLayout_LayoutParams);
            try {
                mFillRemainingWidth = a.getBoolean(R.styleable
                        .FlowLayout_LayoutParams_layout_fill_remaining_width, false);
            } finally {
                a.recycle();
            }
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
        public LayoutParams(LayoutParams source) {
            super(source);
            mFillRemainingWidth = source.mFillRemainingWidth;
        }

        /**
         * @param source The layout params to copy from
         */
        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }
}
