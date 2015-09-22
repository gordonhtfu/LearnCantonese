
package com.blackberry.widgets.tagview.contact;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.blackberry.widgets.R;
import com.blackberry.widgets.tagview.ListItem;
import com.blackberry.widgets.tagview.internal.RoundedDrawable;

import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * A list item for {@link Contact} objects
 */
public class ContactListItem extends ListItem {
    /**
     * The {@link Contact} object this ListItem represents
     *
     * @see #getContact()
     * @see #setContact(Contact)
     */
    private Contact mContact;
    /**
     * Whether or not to allow showing the add contact button
     *
     * @see #allowAddContactButton()
     * @see #setAllowAddContactButton(boolean)
     */
    private boolean mAllowAddContactButton = false;
    /**
     * Swap the ImageViews or not.
     * 
     * @see #setSwapImageViews(boolean)
     */
    private boolean mSwapImageViews = false;

    /**
     * @param context The context
     */
    public ContactListItem(Context context) {
        this(context, null);
    }

    /**
     * @param context The context
     * @param attrs The xml attributes
     */
    public ContactListItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * @return The contact
     * @see #setContact(Contact)
     */
    public Contact getContact() {
        return mContact;
    }

    /**
     * Set the contact
     *
     * @param contact The contact
     * @see #getContact()
     */
    public void setContact(Contact contact) {
        mContact = contact;
        update();
    }

    /**
     * @return Whether or not to allow showing the add contact button
     */
    public boolean allowAddContactButton() {
        return mAllowAddContactButton;
    }

    /**
     * @param allowAddContactButton Whether or not to allow showing the add
     *            contact button
     */
    public void setAllowAddContactButton(boolean allowAddContactButton) {
        mAllowAddContactButton = allowAddContactButton;
    }

    private Drawable getRoundedImage(int rid) {
        RoundedDrawable result = new RoundedDrawable(getResources(), R.drawable.ic_contact_picture);
        result.setDrawBorder(true);
        result.setBorderWidth(getResources().getDimension(R.dimen.contactlistitem_border_width));
        return result;
    }
    
    private Drawable getRoundedImage(Bitmap bitmap) {
        RoundedDrawable result = new RoundedDrawable(bitmap);
        result.setDrawBorder(true);
        result.setBorderWidth(getResources().getDimension(R.dimen.contactlistitem_border_width));
        return result;
    }
    
    private void defaultContactImage() {
        // default contact image, not gone
        getRightImageView().setImageDrawable(getRoundedImage(R.drawable.ic_contact_picture));
        getRightImageView().setVisibility(VISIBLE);
    }

    /**
     * Update this View based on its current state. Call this each time the
     * state changes.
     */
    private void update() {
        getRightImageView().setVisibility(GONE);
        getLeftImageView().setVisibility(GONE);
        setTitle(createTitle());
        setDescription(createDescription());
        setStatus(createStatus());
        Uri photoUri = createPhotoUri();
        if (photoUri != null) {
            InputStream photoInputStream = null;
            try {
                photoInputStream = getContext().getContentResolver().openInputStream(photoUri);
                if (photoInputStream == null) {
                    defaultContactImage();
                } else {
                    getRightImageView()
                            .setImageDrawable(
                                    getRoundedImage(BitmapFactory
                                            .decodeStream(photoInputStream)));
                    getRightImageView().setVisibility(VISIBLE);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            defaultContactImage();
        }
        if (TextUtils.isEmpty(mContact.getLookupKey())) {
            defaultContactImage();
            if (allowAddContactButton()) {
                getLeftImageView().setImageResource(R.drawable.add_to_contact);
                getLeftImageView().setVisibility(VISIBLE);
            }
        }
    }

    /**
     * Subclasses can override this method to provide their own Title
     *
     * @return The title to use
     */
    protected String createTitle() {
        return mContact.getName();
    }

    /**
     * Subclasses can override this method to provide their own description
     *
     * @return The description to use
     */
    protected String createDescription() {
        return "";
    }

    /**
     * Subclasses can override this method to provide their own status
     *
     * @return The status to use
     */
    protected String createStatus() {
        return "";
    }

    /**
     * Subclasses can override this method to provide their own photo Uri
     *
     * @return The photo Uri to use
     */
    protected Uri createPhotoUri() {
        return mContact.getPhotoUri();
    }

    /**
     * Swap the location of the ImageViews. When true things that were normally
     * on the right are now on the left and vice-versa.
     * 
     * @param swapImageViews Whether to swap the ImageViews
     */
    public void setSwapImageViews(boolean swapImageViews) {
        mSwapImageViews = swapImageViews;
    }

    @Override
    public ImageView getLeftImageView() {
        if (mSwapImageViews) {
            return super.getRightImageView();
        }
        return super.getLeftImageView();
    }

    @Override
    public ImageView getRightImageView() {
        if (mSwapImageViews) {
            return super.getLeftImageView();
        }
        return super.getRightImageView();
    }
}
