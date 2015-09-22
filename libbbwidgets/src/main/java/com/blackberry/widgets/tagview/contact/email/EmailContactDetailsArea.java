
package com.blackberry.widgets.tagview.contact.email;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.blackberry.widgets.R;
import com.blackberry.widgets.tagview.BaseTag;
import com.blackberry.widgets.tagview.contact.ContactDetailsArea;

public class EmailContactDetailsArea extends ContactDetailsArea {

    private EmailContactExpandedArea mExpandedArea;

    public EmailContactDetailsArea(Context context, AttributeSet attrs, BaseTag tag) {
        super(context, attrs, tag, R.layout.email_contact_details_area);

        mExpandedArea = (EmailContactExpandedArea) findViewById(R.id.emailContactExpandedArea);
        mExpandedArea.setOnDeleteClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                deleteClicked();
            }
        });
    }

    void setContact(EmailContact contact) {
        mExpandedArea.setContact(contact);
    }
    
    void setReadOnly(boolean readOnly) {
        mExpandedArea.setReadOnly(readOnly);
    }
}
