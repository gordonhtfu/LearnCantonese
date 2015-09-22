
package com.blackberry.widgets.tagview.contact.email;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.blackberry.widgets.R;
import com.blackberry.widgets.tagview.BaseTag.IDeletable;
import com.blackberry.widgets.tagview.contact.Contact.EmailAddress;
import com.blackberry.widgets.tagview.contact.ContactTag;

/**
 * The customized {@link ContactTag} for representing Email contacts.
 */
public class EmailTag extends ContactTag {
    /**
     * The resource Id for the email_external state
     */
    private static final int[] STATE_EMAIL_EXTERNAL = {
        R.attr.state_email_external
    };

    @Override
    protected boolean isValid() {
        Object o = getData();
        if (o instanceof String) {
            return android.util.Patterns.EMAIL_ADDRESS.matcher(o.toString())
                    .matches();
        }
        return super.isValid();
    }

    @Override
    public View getDetailsView(Context context) {
        Object o = getData();
        if (o instanceof EmailContact) {
            EmailContactDetailsArea details = new EmailContactDetailsArea(context, null, this);
            details.setContact((EmailContact) o);
            details.setReadOnly(isReadOnly());
            return details;
        }
        return null;
    }
}
