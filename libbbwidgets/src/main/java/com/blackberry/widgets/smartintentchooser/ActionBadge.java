
package com.blackberry.widgets.smartintentchooser;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.blackberry.widgets.R;

/**
 * A class which visually represents an Action. It consists of:
 * <ul>
 * <li>A background image</li>
 * <li>An optional emblem in one of the 4 corners</li>
 * <li>An optional label along the bottom</li>
 * <li>An optional border around the outside edge</li>
 * </ul>
 */
public class ActionBadge extends FrameLayout {

    private View mRootLayout;
    private ImageView mImageView;
    private ImageView mEmblemImageView;
    private TextView mLabel;

    /**
     * Locate the emblem in the top left corner.
     * <p/>
     * Use with {@link #setEmblemLocation(int)}.
     */
    public static final int LOCATION_TOP_LEFT = 0x1;
    /**
     * Locate the emblem in the top right corner.
     * <p/>
     * Use with {@link #setEmblemLocation(int)}.
     */
    public static final int LOCATION_TOP_RIGHT = 0x2;
    /**
     * Locate the emblem in the bottom left corner.
     * <p/>
     * Use with {@link #setEmblemLocation(int)}.
     */
    public static final int LOCATION_BOTTOM_LEFT = 0x3;
    /**
     * Locate the emblem in the bottom right corner.
     * <p/>
     * Use with {@link #setEmblemLocation(int)}.
     */
    public static final int LOCATION_BOTTOM_RIGHT = 0x4;

    public ActionBadge(Context context) {
        this(context, null);
    }

    /**
     * Constructor
     * 
     * @param context
     * @param attrs
     */
    public ActionBadge(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater inflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.action_badge, this, true);

        mRootLayout = findViewById(R.id.abRootLayout);
        mImageView = (ImageView) findViewById(R.id.abBackgroundImage);
        mEmblemImageView = (ImageView) findViewById(R.id.abEmblemImage);
        mLabel = (TextView) findViewById(R.id.abLabel);
        setLabel(""); // hide the label until they set one
    }

    /**
     * Sets a Bitmap as the content of the background.
     * 
     * @param bm The bitmap to set
     */
    public void setImageBitmap(Bitmap bm) {
        mImageView.setImageBitmap(bm);
    }

    /**
     * Sets a drawable as the content of the background.
     * 
     * @param drawable The drawable to set
     */
    public void setImageDrawable(Drawable drawable) {
        mImageView.setImageDrawable(drawable);
    }

    /**
     * Sets a drawable as the content of the background.
     * 
     * @param resId The resource identifier of the drawable
     */
    public void setImageResource(int resId) {
        mImageView.setImageResource(resId);
    }

    /**
     * Sets the content of the background to the specified Uri.
     * 
     * @param uri The Uri of an image
     */
    public void setImageURI(Uri uri) {
        mImageView.setImageURI(uri);
    }

    /**
     * Sets a Bitmap as the content of the emblem.
     * 
     * @param bm The bitmap to set
     */
    public void setEmblemImageBitmap(Bitmap bm) {
        mEmblemImageView.setImageBitmap(bm);
    }

    /**
     * Sets a drawable as the content of the emblem.
     * 
     * @param drawable The drawable to set
     */
    public void setEmblemImageDrawable(Drawable drawable) {
        mEmblemImageView.setImageDrawable(drawable);
    }

    /**
     * Sets a drawable as the content of the emblem.
     * 
     * @param resId The resource identifier of the drawable
     */
    public void setEmblemImageResource(int resId) {
        mEmblemImageView.setImageResource(resId);
    }

    /**
     * Sets the content of the emblem to the specified Uri.
     * 
     * @param uri The Uri of an image
     */
    public void setEmblemImageURI(Uri uri) {
        mEmblemImageView.setImageURI(uri);
    }

    /**
     * Sets the location of the emblem.
     * 
     * @param location The location to set. One of {@link #LOCATION_TOP_LEFT},
     *            {@link #LOCATION_TOP_RIGHT}, {@link #LOCATION_BOTTOM_LEFT},
     *            {@link #LOCATION_BOTTOM_RIGHT}
     */
    public void setEmblemLocation(int location) {
        boolean alignTop = false;
        boolean alignLeft = false;
        switch (location) {
            case LOCATION_TOP_RIGHT:
                alignTop = true;
                break;
            case LOCATION_BOTTOM_LEFT:
                alignLeft = true;
                break;
            case LOCATION_BOTTOM_RIGHT:
                break;
            default:
                alignTop = true;
                alignLeft = true;
                break;
        }

        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mEmblemImageView
                .getLayoutParams();
        if (alignTop) {
            lp.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        } else {
            lp.removeRule(RelativeLayout.ALIGN_PARENT_TOP);
            lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        }
        if (alignLeft) {
            lp.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        } else {
            lp.removeRule(RelativeLayout.ALIGN_PARENT_LEFT);
            lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        }
        mEmblemImageView.setLayoutParams(lp);
    }

    /**
     * Sets the label to show at the bottom of this View.
     * 
     * @param label The label to show. If null or empty the bottom bar will not
     *            be shown.
     */
    public void setLabel(String label) {
        mLabel.setText(label);
        if (TextUtils.isEmpty(label)) {
            mLabel.setVisibility(GONE);
        } else {
            mLabel.setVisibility(VISIBLE);
        }
    }

    /**
     * Sets the visibility of the border around this View.
     * 
     * @param visible True to show the border or false otherwise.
     */
    public void setBorderVisible(boolean visible) {
        if (visible) {
            mRootLayout.setBackgroundResource(R.drawable.action_badge_border);
        } else {
            mRootLayout.setBackground(null);
        }
    }
}
