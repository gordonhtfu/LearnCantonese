package com.blackberry.account.registry;

import android.content.ContentValues;
import android.content.Context;

/**
 * Text style definition.
 * It can describe font style, and font color.
 * 
 * @author dsutedja
 */
public final class TextStyle extends AbstractDecorData {
    public static final int PLAIN = 1 << 0;
    public static final int ITALIC = 1 << 1;
    public static final int BOLD = 1 << 2;

    private static final int RGB_MASK = 0xFFFFFF << 8;

    private int mStyle;



    TextStyle(Context context, long accountId, String mimeType) {
        super(context, accountId, mimeType, ListItemDecor.Type.TextStyle);
        mItemState = -1;
        mTemplateId = -1;
        mElementPosition = -1;
        mStyle = -1;
    }

    /**
     * Set the color for this text style.
     * 
     * @param rgb -- RGB value
     * @return this
     */
    public TextStyle setColor(int rgb) {
        if (mStyle == -1) {
            mStyle = 0; // just to note that client did try to set a style
        }
        if (rgb >= 0 && rgb <= 0xFFFFFF) {
            mStyle |= (rgb << 8);
        }
        return this;
    }

    /**
     * Get the assigned color.
     * 
     * @return the color
     */
    public int color() {
        return mStyle & RGB_MASK;
    }

    /**
     * Add a font style.
     * 
     * @param style -- the style flag (must be PLAIN, BOLD, or ITALIC).
     * @return this
     */
    public TextStyle addStyle(int style) {
        if (mStyle == -1) {
            mStyle = 0; // just to note that client did try to set a style
        }
        validateStyle(style);
        mStyle |= ((mStyle & ~RGB_MASK) | style);
        return this;
    }

    /**
     * Is the passed in style added to this definition?
     * 
     * @param style -- the style flag, must be BOLD, ITALIC, or PLAIN
     * @return set?
     */
    public boolean hasStyle(int style) {
        validateStyle(style);
        return ((mStyle & ~RGB_MASK) & style) > 0;
    }

    /**
     * Replaced the defined style with the passed in one.
     * This is only used, when TextStyle is populated from db cursor.
     * 
     * @param style -- the raw style
     */
    public void setRawStyle(int style) {
        mStyle = style;
    }

    @Override
    protected void insertExtraData(ContentValues values) {
        if (mStyle == -1) {
            throw new IllegalArgumentException("invalid style");
        }
        values.put(MimetypeRegistryContract.DecorMapping.ELEMENT_STYLE, mStyle);
    }


    private void validateStyle(int style) {
        if (style != PLAIN && style != BOLD && style != ITALIC) {
            throw new IllegalArgumentException("Wrong value");
        }
    }

    // overrides so the fluent interface still works

    @Override
    public TextStyle setAccountId(int id) {
        super.setAccountId(id);
        return this;
    }

    @Override
    public TextStyle setItemState(long state) {
        super.setItemState(state);
        return this;
    }

    @Override
    public TextStyle setTemplateId(int id) {
        super.setTemplateId(id);
        return this;
    }

    @Override
    public TextStyle setElementPosition(int position) {
        super.setElementPosition(position);
        return this;
    }
}
