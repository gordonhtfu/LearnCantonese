
package com.blackberry.widgets.tagview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.blackberry.widgets.R;

/**
 * ListItem is the base View for showing data in a ListView in a very structured
 * way.
 * <p/>
 * There is both a left- and right-aligned image with three TextViews in
 * between. The 2 images are optional and collapse when not in use. These 3
 * TextViews are organized into two rows. The top row has two TextViews (Title
 * and Status). The Status is right-aligned while Title fills the remaining
 * space. The second row has the remaining TextView (Description).
 */
public class ListItem extends LinearLayout implements ISoftFocusable {
    /**
     * The resource ID of the soft_focus state.
     */
    private static final int[] STATE_SOFT_FOCUS = {
            R.attr.state_soft_focus
    };

    /**
     * The ImageView for the left-aligned image
     *
     * @see #getLeftImageView()
     */
    private ImageView mLeftImageView;
    /**
     * The TextView for the Title
     */
    private TextView mTitleTextView;
    /**
     * The TextView for the Status
     */
    private TextView mStatusTextView;
    /**
     * The TextView for the Description
     */
    private TextView mDescriptionTextView;
    /**
     * The ImageView for the right-aligned image
     *
     * @see #getRightImageView()
     */
    private ImageView mRightImageView;
    /**
     * Whether or not the control has soft focus
     *
     * @see #isSoftFocused()
     * @see #setSoftFocus(boolean)
     */
    private boolean mHasSoftFocus;

    /**
     * The ImageView for the delete image
     * 
     * @see #getDeleteImageView()
     */
    private ImageView mDeleteImageView;

    /**
     * @param context The context
     */
    public ListItem(Context context) {
        this(context, null);
    }

    /**
     * @param context The context
     * @param attrs The xml attributes
     */
    public ListItem(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context
                .LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.list_item, this, true);
        mLeftImageView = (ImageView) findViewById(R.id.liLeftImage);
        mTitleTextView = (TextView) findViewById(R.id.liTitleTextView);
        mStatusTextView = (TextView) findViewById(R.id.liStatusTextView);
        mDescriptionTextView = (TextView) findViewById(R.id.liDescriptionTextView);
        mRightImageView = (ImageView) findViewById(R.id.liRightImage);
        mDeleteImageView = (ImageView) findViewById(R.id.liDeleteImage);
    }

    /**
     * @return The ImageView used for the left-aligned image
     */
    public ImageView getLeftImageView() {
        return mLeftImageView;
    }

    /**
     * @return The Title to show
     */
    public String getTitle() {
        return mTitleTextView.getText().toString();
    }

    /**
     * @param title The Title to show
     */
    public void setTitle(String title) {
        mTitleTextView.setText(title);
    }

    /**
     * @param visibility The new visibility of the title
     */
    public void setTitleVisibility(int visibility) {
        mTitleTextView.setVisibility(visibility);
    }

    /**
     * @return The Status to show
     */
    public String getStatus() {
        return mStatusTextView.getText().toString();
    }

    /**
     * @param status The Status to show
     */
    public void setStatus(String status) {
        mStatusTextView.setText(status);
    }

    /**
     * @return The Description to show
     */
    public String getDescription() {
        return mDescriptionTextView.getText().toString();
    }

    /**
     * @param description The Description to show
     */
    public void setDescription(String description) {
        mDescriptionTextView.setText(description);
    }

    /**
     * @return The ImageView used for the right-aligned image
     */
    public ImageView getRightImageView() {
        return mRightImageView;
    }

    @Override
    public boolean isSoftFocused() {
        return mHasSoftFocus;
    }

    @Override
    public void setSoftFocus(boolean isSoftFocused) {
        this.mHasSoftFocus = isSoftFocused;
        refreshDrawableState();
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        if (mHasSoftFocus) {
            int[] result = super.onCreateDrawableState(extraSpace + 1);
            mergeDrawableStates(result, STATE_SOFT_FOCUS);
            return result;
        }
        return super.onCreateDrawableState(extraSpace);
    }

    /**
     * @return The ImageView used for the delete image.
     */
    public ImageView getDeleteImageView() {
        return mDeleteImageView;
    }
}
