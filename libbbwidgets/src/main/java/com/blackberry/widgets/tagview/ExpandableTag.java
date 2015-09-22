package com.blackberry.widgets.tagview;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * @author tallen
 *         <p/>
 *         A special Tag which can be expanded when selected.
 */
public abstract class ExpandableTag extends BaseTag {

    /**
     * The listener registered for expansion requests
     *
     * @see #getOnExpandRequestListener()
     * @see #setOnExpandRequestListener(OnExpandRequestListener)
     */
    private OnExpandRequestListener mOnExpandRequestListener;

    /**
     * @param context The context
     */
    public ExpandableTag(Context context) {
        this(context, null);
    }

    /**
     * @param context The context
     * @param attrs   The xml attributes
     */
    public ExpandableTag(Context context, AttributeSet attrs) {
//        super(context, attrs);
    }

    @Override
    public void setSelected(boolean selected) {
        if (selected != isSelected()) {
            super.setSelected(selected);
            if (mOnExpandRequestListener != null) {
                mOnExpandRequestListener.expand(this, selected);
            }
        }
    }

    /**
     * Get the {@link OnExpandRequestListener}
     *
     * @return The {@link OnExpandRequestListener} which may be null.
     */
    public OnExpandRequestListener getOnExpandRequestListener() {
        return mOnExpandRequestListener;
    }

    /**
     * Set the {@link OnExpandRequestListener}
     *
     * @param listener The {@link OnExpandRequestListener} to set
     */
    public void setOnExpandRequestListener(OnExpandRequestListener listener) {
        mOnExpandRequestListener = listener;
    }

    /**
     * Get the expanded area View when required. Sub classes must implement this.
     *
     * @param parent The parent the expanded area will be added to. This can be used for inflation
     *               purposes, if necessary.
     * @return The View to use in the expanded area, or null for no expanded View.
     */
    public abstract View getExpandedAreaView(ViewGroup parent);

    /**
     * @author tallen
     *         <p/>
     *         A class used to listen for expansion/collapse requests from {@link ExpandableTag}
     *         objects.
     */
    public static interface OnExpandRequestListener {
        /**
         * Called when an {@link ExpandableTag} requests expansion/collapse.
         *
         * @param tag    The {@link ExpandableTag} requesting expansion/collapse
         * @param expand True to expand or false to collapse
         */
        void expand(ExpandableTag tag, boolean expand);
    }
}
