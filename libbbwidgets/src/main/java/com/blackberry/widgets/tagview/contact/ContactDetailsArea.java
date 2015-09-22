
package com.blackberry.widgets.tagview.contact;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import com.blackberry.widgets.tagview.BaseTag;
import com.blackberry.widgets.tagview.BaseTag.IDeletable;
import com.blackberry.widgets.tagview.BaseTag.OnTagDeleteClickListener;

public class ContactDetailsArea extends FrameLayout implements IDeletable {
    private OnTagDeleteClickListener mOnTagDeleteClickListener;
    private BaseTag mTag;

    /**
     * @param context The context.
     * @param attrs The xml attributes.
     * @param tag The tag these details represent.
     * @param layout The resource id of the layout file to inflate.
     */
    public ContactDetailsArea(Context context, AttributeSet attrs, BaseTag tag, int layout) {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(layout, this, true);

        mTag = tag;
    }

    /**
     * @return The on delete click listener, or null if not set.
     */
    public OnTagDeleteClickListener getOnDeleteClickListener() {
        return mOnTagDeleteClickListener;
    }

    protected void deleteClicked() {
        if (mOnTagDeleteClickListener != null) {
            mOnTagDeleteClickListener.onClick(mTag);
        }
    }

    @Override
    public void setOnDeleteClickListener(OnTagDeleteClickListener listener) {
        mOnTagDeleteClickListener = listener;
    }

}
