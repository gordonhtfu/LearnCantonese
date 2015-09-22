package com.blackberry.widgets.tagview;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.blackberry.widgets.R;

/**
 * @author tallen
 *         <p/>
 *         The standard tag class with the majority of the functionality
 */
public class StandardTag extends BaseTag {
    /**
     * The resource ID of the invalid state
     */
    private static final int[] STATE_INVALID = {R.attr.state_invalid};
    /**
     * The TextView showing the Label
     */
    private TextView mLabelTextView;
    /**
     * The TextView showing the Delete ('x'). This will probably be replaced by an image
     * eventually.
     */
    private TextView mDeleteTextView;
    /**
     * The listener registered to be called when the Delete View is touched.
     */
    private OnDeleteClickListener mDeleteListener = null;
    /**
     * The listener registered to be called when the udnerlying data object changes, if it supports
     * the data change notification.
     */
    private StandardTagObjectChangedListener mListener = new StandardTagObjectChangedListener();

    /**
     * @param context The context
     */
    public StandardTag(Context context) {
        this(context, null);
    }

    /**
     * @param context The context
     * @param attrs   The xml attributes
     */
    public StandardTag(Context context, AttributeSet attrs) {
//        super(context, attrs, R.layout.standard_tag);
//
//        setClickable(true);
//
//        setBackgroundResource(R.drawable.tag_background);
//
//        mLabelTextView = (TextView) findViewById(R.id.stLabelTextView);
//        mDeleteTextView = (TextView) findViewById(R.id.stDeleteTextView);
//        final BaseTag thisStandardTag = this;
//        mDeleteTextView.setOnClickListener(new OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//                if (mDeleteListener != null) {
//                    mDeleteListener.onDeleteClick(thisStandardTag);
//                }
//            }
//        });
//        updateDeleteVisibility();
    }

    /**
     * @return The registered listener to use when the delete action is triggered.
     * @see #setOnDeleteClickListener(com.blackberry.widgets.tagview.StandardTag.OnDeleteClickListener)
     */
    public OnDeleteClickListener getOnDeleteClickListener() {
        return mDeleteListener;
    }

    /**
     * Set the delete action listener
     *
     * @param listener The listener to use when the delete action is triggered
     * @see #getOnDeleteClickListener()
     */
    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        mDeleteListener = listener;
    }

//    @Override
    public void setData(Object data) {
        Object oldData = getData();
        if (oldData != null) {
            if (oldData instanceof OnObjectChanged) {
                OnObjectChanged ooc = (OnObjectChanged) oldData;
                // deregister our on changed listener
                ooc.setOnObjectChangedListener(null);
            }
        }
        super.setData(data);
        updateLabel();
        if (data instanceof OnObjectChanged) {
            // supports listening for changes
            OnObjectChanged ooc = (OnObjectChanged) data;
            ooc.setOnObjectChangedListener(mListener);
        }
    }

    /**
     * This is called automatically when we need to update the label. Subclasses may call this to
     * force a label update.
     */
    protected void updateLabel() {
        mLabelTextView.setText(getLabel());
    }

    /**
     * This is called automatically when the underlying data changes, if the underlying data extends
     * {@link com.blackberry.widgets.tagview.StandardTag.OnObjectChanged}
     */
    protected void dataChanged() {
        updateLabel();
    }



//    @Override
    public boolean onTouchEvent(MotionEvent event) {
//        boolean superResult = super.onTouchEvent(event);
//        if (event.getAction() == MotionEvent.ACTION_DOWN) {
//            // Do this otherwise View has a standard delay before entering the
//            // pressed state. This is noticeable to the user because the
//            // translucent overlay when in pressed mode will not show up
//            // immediately after touching the tag.
//            setPressed(true);
//        }
//        return superResult;
        return true;
    }

    /**
     * Sets the text color
     *
     * @param colors The colors to set
     */
    public void setTextColor(ColorStateList colors) {
        mLabelTextView.setTextColor(colors);
    }

    /**
     * Sets the text color
     *
     * @param color The color to set
     */
    public void setTextColor(int color) {
        mLabelTextView.setTextColor(color);
    }

//    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
//        if (!isValid()) {
//            final int[] drawableState = super
//                    .onCreateDrawableState(extraSpace + 1);
//            mergeDrawableStates(drawableState, STATE_INVALID);
//            return drawableState;
//        }
//        return super.onCreateDrawableState(extraSpace);
        return new int[] { };
    }

    /**
     * Call this to update the visibility of the Delete View based on the current state.
     */
    void updateDeleteVisibility() {
        boolean showWidget = isSelected() && !isReadOnly();
        mDeleteTextView.setVisibility(showWidget ? View.VISIBLE : View.GONE);
    }

    /**
     * @author tallen
     *         <p/>
     *         A Listener to extend to listen for object changes. This is useful for the Data object
     *         on a Tag to automatically-update the Label
     */
    public abstract static interface OnObjectChangedListener {
        /**
         * Called when the object has changed
         */
        void onObjectChanged();
    }

    /**
     * @author tallen
     *         <p/>
     *         A Listener to extend to listen for delete clicks
     */
    public abstract static interface OnDeleteClickListener {
        /**
         * Called when a delete has been requested by the StandardTag
         *
         * @param sender The StandardTag which is being deleted
         */
        void onDeleteClick(BaseTag sender);
    }

    /**
     * @author tallen
     *         <p/>
     *         A class which allows an Object to notify a registered listener of changes to its
     *         underlying data.
     */
    public abstract static class OnObjectChanged {
        private OnObjectChangedListener mListener;

        /**
         * Set the object changed listener
         *
         * @param listener The new listener
         */
        public void setOnObjectChangedListener(OnObjectChangedListener listener) {
            mListener = listener;
        }

        /**
         * Called by subclasses to notify that this object has changed.
         */
        protected void notifyOnObjectChangedListener() {
            if (mListener != null) {
                mListener.onObjectChanged();
            }
        }
    }

    /**
     * @author tallen
     *         <p/>
     *         Used to listen for changes to objects which extend OnObjectChanged.
     */
    private class StandardTagObjectChangedListener implements
            OnObjectChangedListener {

        @Override
        public void onObjectChanged() {
            dataChanged();
        }

    }
}
