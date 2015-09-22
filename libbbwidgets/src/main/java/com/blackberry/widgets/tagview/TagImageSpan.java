
package com.blackberry.widgets.tagview;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ImageSpan;

import com.blackberry.widgets.tagview.BaseTag.BaseTagDimensions;

/**
 * An {@link ImageSpan} wrapped around a {@link BaseTag} object using this
 * {@link BaseTag} for the {@link Drawable}.
 */
class TagImageSpan extends ImageSpan {
    /**
     * The tag this Span represents
     */
    private BaseTag mTag;

    /**
     * @param context The context
     * @param availableWidth The maximum width the tag can be
     * @param tagDimensions The dimensions to use for the tag
     * @param paint The {@link TextPaint} used to draw the text
     * @param tag The tag this Span represents. Must not be null.
     */
    public TagImageSpan(Context context, int availableWidth, BaseTagDimensions tagDimensions,
            TextPaint paint,
            BaseTag tag) {
        super(tag.getDrawable(context, availableWidth, tagDimensions, paint),
                DynamicDrawableSpan.ALIGN_BOTTOM);
        mTag = tag;
    }

    /**
     * @return The tag this Span represents
     */
    public BaseTag getTag() {
        return mTag;
    }
}
