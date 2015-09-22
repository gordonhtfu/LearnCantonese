
package com.blackberry.widgets.smartintentchooser;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;

/**
 * A {@link FrameLayout} which has its height and width locked to be the minimum
 * of the two values.
 */
public class SquareFrameLayout extends FrameLayout {

    /**
     * Constructor.
     * 
     * @param context The context.
     * @param attrs The xml attributes.
     */
    public SquareFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // Now that we have what size the layout *wants* to be, figure out which
        // one needs to be squared up and do it. Note that this does NOT handle
        // minwidth/minheight dimensions at all.
        int dimensionSpec = MeasureSpec.makeMeasureSpec(
                Math.min(getMeasuredWidth(), getMeasuredHeight()), MeasureSpec.EXACTLY);
        super.onMeasure(dimensionSpec, dimensionSpec);
    }
}
