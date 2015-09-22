
package com.blackberry.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.LinearLayout;

/**
 * A {@link LinearLayout} which responds properly to maxWidth and maxHeight
 * properties.
 */
public class MaxLinearLayout extends LinearLayout {

    private int mMaxWidth = -1;
    private int mMaxHeight = -1;

    /**
     * @param context The context.
     * @param attrs The xml attributes.
     */
    public MaxLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

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

        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.MaxLinearLayout);

        try {
            int count = a.getIndexCount();
            for (int i = 0; i < count; i++) {
                int attr = a.getIndex(i);
                if (attr == R.styleable.MaxLinearLayout_android_maxWidth) {
                    setMaxWidth(a.getDimensionPixelSize(attr, -1));
                } else if (attr == R.styleable.MaxLinearLayout_android_maxHeight) {
                    setMaxHeight(a.getDimensionPixelSize(attr, -1));
                }
            }
        } finally {
            a.recycle();
        }
    }

    /**
     * @return The max height of this control.
     */
    public int getMaxHeight() {
        return mMaxHeight;
    }

    /**
     * @param maxHeight
     */
    public void setMaxHeight(int maxHeight) {
        mMaxHeight = maxHeight;
        requestLayout();
    }

    /**
     * @return The max width of this control.
     */
    public int getMaxWidth() {
        return mMaxWidth;
    }

    /**
     * @param maxWidth
     */
    public void setMaxWidth(int maxWidth) {
        mMaxWidth = maxWidth;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mMaxWidth > 0) {
            int measureMode = MeasureSpec.getMode(widthMeasureSpec);
            if (measureMode != MeasureSpec.EXACTLY) {
                widthMeasureSpec = MeasureSpec.makeMeasureSpec(mMaxWidth, MeasureSpec.AT_MOST);
            }
        }
        if (mMaxHeight > 0) {
            int measureMode = MeasureSpec.getMode(heightMeasureSpec);
            if (measureMode != MeasureSpec.EXACTLY) {
                heightMeasureSpec = MeasureSpec.makeMeasureSpec(mMaxHeight, MeasureSpec.AT_MOST);
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
