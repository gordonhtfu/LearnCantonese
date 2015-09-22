package com.blackberry.widgets.tagview.contact.textmessage;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.blackberry.widgets.tagview.contact.ContactTag;
import com.blackberry.widgets.tagview.contact.email.EmailContact;
import com.blackberry.widgets.tagview.contact.email.EmailContactDetailsArea;

/**
 * The customized {@link ContactTag} for representing Text Message contacts.
 */
public class TextMessageTag extends ContactTag {
    @Override
    protected boolean isValid() {
        Object o = getData();
        if (o instanceof String) {
            // TODO: phone number or email address validity
            return true;
        }
        return super.isValid();
    }
    
    @Override
    public View getDetailsView(Context context) {
        Object o = getData();
        if (o instanceof TextMessageContact) {
            TextMessageContactDetailsArea details = new TextMessageContactDetailsArea(context, null, this);
            details.setContact((TextMessageContact) o);
            details.setReadOnly(isReadOnly());
            return details;
        }
        return null;
    }
}
