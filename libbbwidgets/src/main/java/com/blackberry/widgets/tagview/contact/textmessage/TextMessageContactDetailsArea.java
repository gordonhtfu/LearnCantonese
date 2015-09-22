
package com.blackberry.widgets.tagview.contact.textmessage;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.blackberry.widgets.R;
import com.blackberry.widgets.tagview.BaseTag;
import com.blackberry.widgets.tagview.contact.ContactDetailsArea;

public class TextMessageContactDetailsArea extends ContactDetailsArea {

    private TextMessageContactExpandedArea mExpandedArea;

    public TextMessageContactDetailsArea(Context context, AttributeSet attrs, BaseTag tag) {
        super(context, attrs, tag, R.layout.textmessage_contact_details_area);

        mExpandedArea = (TextMessageContactExpandedArea) findViewById(R.id.textmessageContactExpandedArea);
        mExpandedArea.setOnDeleteClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                deleteClicked();
            }
        });
    }

    void setContact(TextMessageContact contact) {
        mExpandedArea.setContact(contact);
    }

    void setReadOnly(boolean readOnly) {
        mExpandedArea.setReadOnly(readOnly);
    }
}
