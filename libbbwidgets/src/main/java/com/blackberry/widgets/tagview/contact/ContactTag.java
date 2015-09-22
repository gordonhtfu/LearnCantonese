
package com.blackberry.widgets.tagview.contact;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.blackberry.widgets.tagview.BaseTag;

/**
 * A Tag representing a {@link Contact} object
 */
public class ContactTag extends BaseTag {
    @Override
    protected boolean isValid() {
        Object o = getData();
        return o instanceof Contact && ((Contact) o).isValid();
    }

    @Override
    protected String getLabel() {
        Object o = getData();
        if (o instanceof Contact) {
            return ((Contact) o).getLabel();
        }
        return super.getLabel();
    }
    
    @Override
    protected Uri getImage(Context context) {
        Object o = getData();
        if (o instanceof Contact) {
            return ((Contact) o).getPhotoUriOrDefault(context);
        }
        return null;
    }
}
