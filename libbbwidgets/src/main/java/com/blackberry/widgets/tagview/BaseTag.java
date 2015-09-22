
package com.blackberry.widgets.tagview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.View;
import android.widget.PopupWindow;

import com.blackberry.widgets.R;
import com.blackberry.widgets.tagview.internal.RoundedDrawable;

import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * The base class for all Tag Views.
 */
public class BaseTag implements IReadOnly {

    /**
     * The data represented by this Tag.
     */
    private Object mData;

    /**
     * Whether or not this control is read-only.
     *
     * @see #isReadOnly()
     * @see #setReadOnly(boolean)
     */
    private boolean mReadOnly = false;

    /**
     * Whether or not this tag is selected
     * 
     * @see #isSelected()
     * @see #setSelected(boolean)
     */
    private boolean mSelected = false;

    /**
     * Set the data to be presented in this View
     *
     * @param data The data object to present in this View
     * @see #getData()
     */
    public void setData(Object data) {
        mData = data;
    }

    /**
     * Get the data to be presented in this View
     *
     * @return The data to be presented in this View
     * @see #setData(Object)
     */
    public Object getData() {
        return mData;
    }

    private static float getTextYOffset(String text, TextPaint paint, int height) {
        Rect bounds = new Rect();
        // Yes I understand this is ridiculous. But it is how the Android Chips
        // control does it, and it is the only way I have found that works to
        // perfectly center the text in all cases (at least for English. i18n
        // may screw this up).
        paint.getTextBounds("a", 0, 1, bounds);
        int textHeight = bounds.bottom - bounds.top;
        return height - ((height - textHeight) / 2);
    }

    /**
     * @param context The context
     * @param availableWidth The maximum width the tag can be
     * @param tagDimensions The dimensions to use for the tag
     * @param paint The paint to use to draw with
     * @return The {@link Drawable} representation of this tag
     */
    public Drawable getDrawable(Context context, int availableWidth,
            BaseTagDimensions tagDimensions, TextPaint paint) {
        int roundedHeight = Math.round(tagDimensions.height);
        int iconWidth = roundedHeight;
        float[] widths = new float[1];
        paint.getTextWidths(" ", widths);
        CharSequence ellipsizedText = TextUtils.ellipsize(getLabel(), paint,
                availableWidth - iconWidth - widths[0] - tagDimensions.paddingLeft
                        - tagDimensions.paddingRight, TextUtils.TruncateAt.END);
        // Make sure there is a minimum chip width so the user can ALWAYS
        // tap a chip without difficulty.
        int width = Math.max(iconWidth * 2, (int) Math.floor(paint.measureText(ellipsizedText, 0,
                ellipsizedText.length()))
                + Math.round(tagDimensions.paddingLeft) + Math.round(tagDimensions.paddingRight)
                + iconWidth);

        Bitmap tmpBitmap = Bitmap.createBitmap(width, roundedHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(tmpBitmap);
        Drawable background = context.getResources().getDrawable(R.drawable.tag_background);

        background.setBounds(0, 0, width, roundedHeight);
        background.draw(canvas);

        Drawable result = new BitmapDrawable(context.getResources(), tmpBitmap);
        result.setBounds(0, 0, tmpBitmap.getWidth(), tmpBitmap.getHeight());

        int textLeft = Math.round(tagDimensions.paddingLeft);

        Uri photoUri = getImage(context);
        if (photoUri != null) {
            InputStream photoInputStream = null;
            try {
                photoInputStream = context.getContentResolver().openInputStream(getImage(context));

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            Bitmap photoBitmap = BitmapFactory.decodeStream(photoInputStream);
            if (photoBitmap != null) {
                Rect dst = new Rect(0, 0, iconWidth, roundedHeight);

                RoundedDrawable rd = new RoundedDrawable(photoBitmap, dst);
                rd.draw(canvas);

                textLeft += iconWidth;
            }
        }

        // Vertically center the text in the chip.
        canvas.drawText(ellipsizedText, 0, ellipsizedText.length(), textLeft,
                getTextYOffset((String) ellipsizedText, paint, roundedHeight), paint);

        return result;
    }

    /**
     * Indicates if this widget is read only or not
     *
     * @return Whether this widget is read only or not
     * @see #setReadOnly(boolean)
     */
    public boolean isReadOnly() {
        return mReadOnly;
    }

    /**
     * Set the read only status of the widget
     *
     * @param readOnly The new read only status
     * @see #isReadOnly()
     */
    public void setReadOnly(boolean readOnly) {
        mReadOnly = readOnly;
    }

    /**
     * The Label is the string which will be shown on the Tag. Subclasses can
     * override this to change the default behaviour.
     *
     * @return The string to use for the Tag
     */
    protected String getLabel() {
        Object data = getData();
        if (data == null) {
            return "";
        }
        return data.toString();
    }

    /**
     * A {@link Uri} which points to an image to show on the left side of the
     * tag. Subclasses can choose to override this to provide one.
     * 
     * @param context The context
     * @return The Uri to an image to show. May be null for no image.
     */
    protected Uri getImage(Context context) {
        return null;
    }

    /**
     * @return Whether or not this tag is selected
     */
    public boolean isSelected() {
        return mSelected;
    }

    /**
     * @param selected The selected state of the tag
     */
    public void setSelected(boolean selected) {
        mSelected = selected;
    }

    /**
     * The validity of the data contained in this Tag.
     *
     * @return Whether or not the data contained in this tag is valid or not
     */
    protected boolean isValid() {
        return getData() != null;
    }

    /**
     * Get the details View when required. Sub classes should implement this to
     * provide details to be shown in a {@link PopupWindow} when a BaseTag is
     * selected.
     * <p/>
     * If you return a View which implements {@link IDeletable} you can display
     * a delete mechanism to the user on the Details View.
     *
     * @param context The context
     * @return The {@link View} to use in the details area, or null for no
     *         details view.
     */
    public View getDetailsView(Context context) {
        return null;
    }

    static class BaseTagDimensions {
        float height;
        float paddingLeft;
        float paddingRight;
        float paddingTop;
        float paddingBottom;
        float textSize;
        float spacing;
    }

    /**
     * A listener to provide notifications when a delete action is triggered for
     * a tag.
     */
    public interface OnTagDeleteClickListener {
        /**
         * The delete action was triggered for a tag.
         * 
         * @param tag The tag whose delete action was triggered
         */
        void onClick(BaseTag tag);
    }

    /**
     * Interface providing a delete click listener.
     */
    public interface IDeletable {
        /**
         * @param listener The listener to register
         */
        void setOnDeleteClickListener(OnTagDeleteClickListener listener);
    }
}
