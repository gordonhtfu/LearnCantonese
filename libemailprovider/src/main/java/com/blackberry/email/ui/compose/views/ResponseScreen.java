
package com.blackberry.email.ui.compose.views;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannedString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.blackberry.email.ui.compose.ComposeUtils;
import com.blackberry.email.ui.compose.controllers.ComposeActivity;
import com.blackberry.email.ui.compose.controllers.ComposeScreenController;
import com.blackberry.email.ui.compose.controllers.JSRepository;
import com.blackberry.lib.emailprovider.R;
import com.blackberry.message.service.MessageBodyValue;
import com.blackberry.message.service.MessageValue;
import com.blackberry.provider.MessageContract;

public class ResponseScreen extends ComposeScreen implements OnNavigationListener {

    public ResponseScreen(ComposeScreenController controller, ComposeActivity context,
            Bundle savedInstanceState, int screenType) {
        super(controller, context, savedInstanceState, screenType);
    }

    protected void initializeActionBar(ActionBar actionBar) {
        if ((actionBar != null) && mScreenType != ComposeActivity.EDIT_DRAFT) {
            actionBar.setTitle(null);
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
            actionBar.setListNavigationCallbacks(new TitleAdapter(mActivityContext), this);
            actionBar.setSelectedNavigationItem(mScreenType);
        }
        super.initializeActionBar(actionBar);
    }
    
    public void initializeFromMessage(MessageValue refMessage) {
        mRecipientsFrag.initializeFromMessage(mFromSpinner.getCurrentAccount().account.getEmailAddress(), refMessage, mScreenType);
        mSubject.setText(ComposeUtils.buildFormattedSubject(mActivityContext.getResources(), refMessage.mSubject, mScreenType));

        String originalBodyData = COMPOSITION_DOCUMENT_TEMPLATE;
        final List<MessageBodyValue> bodies = refMessage.getBodies();
        if (!bodies.isEmpty()) {
            MessageBodyValue body = bodies.get(0);
            if (body != null) {
                final String bodyData = body.getBytesAsString(); 
                if (body.mType == MessageContract.MessageBody.Type.HTML)
                    originalBodyData = bodyData;
                else if (body.mType == MessageContract.MessageBody.Type.TEXT)
                    originalBodyData =  Html.toHtml(new SpannedString(bodyData));
            }
        }

        StringBuilder responseDocument = new StringBuilder();
        if (mScreenType != ComposeActivity.EDIT_DRAFT) {
            DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT);
            responseDocument.append(JSRepository.RESPONSE_SECTION);
            responseDocument.append(JSRepository.ORIGINAL_MSG_SEPARATOR_BEGIN);

            // build original message header
            responseDocument.append(String.format(JSRepository.LTR_HEADER_FIELD_TEMPLATE, "from", 
             "From: ", refMessage.getAddresses(MessageContract.MessageContact.FieldType.FROM)[0]));
            responseDocument.append(String.format(JSRepository.LTR_HEADER_FIELD_TEMPLATE, "sent",
                                  "Sent: ", dateFormat.format(new Date(refMessage.mTimeStamp))));
            responseDocument.append(String.format(JSRepository.LTR_HEADER_FIELD_TEMPLATE, "to",
             "To: ", getFormattedAddressList(refMessage.getAddresses(MessageContract.MessageContact.FieldType.TO))));
            if (refMessage.getAddresses(MessageContract.MessageContact.FieldType.REPLY_TO).length != 0) {
                responseDocument.append(String.format(JSRepository.LTR_HEADER_FIELD_TEMPLATE, "reply_to",
                "Reply To: ", getFormattedAddressList(refMessage.getAddresses(MessageContract.MessageContact.FieldType.REPLY_TO))));
            }
            if (refMessage.getAddresses(MessageContract.MessageContact.FieldType.CC).length != 0) {
                responseDocument.append(String.format(JSRepository.LTR_HEADER_FIELD_TEMPLATE, "cc",
                "Cc: ", getFormattedAddressList(refMessage.getAddresses(MessageContract.MessageContact.FieldType.CC))));
            }
            responseDocument.append(String.format(JSRepository.LTR_HEADER_FIELD_TEMPLATE, "subject", "Subject: ", refMessage.mSubject));

            responseDocument.append(JSRepository.ORIGINAL_MSG_SEPARATOR_END);
            responseDocument.append(originalBodyData);
            responseDocument.append(JSRepository.ORIGINAL_MSG_CONTENT_END);
        } else {
            responseDocument.append(originalBodyData);
        }
        mBody.loadSecureData(responseDocument.toString());

        String[] selectedAccountDetails = {
                mController.getSelectedAccount().getSenderName(),
                mController.getSelectedAccount().getEmailAddress()
        };
        super.setupAccountSelectorState(mScreenType != ComposeActivity.EDIT_DRAFT,
                selectedAccountDetails);
    }

    protected void cleanupDocument() {
        super.cleanupDocument();
    }

    @Override
    public void onLoadSucceeded(String url) {
        mBody.evaluateJavascript(JSRepository.SETUP_SCRIPT_ENVIRONMENT);
        if (mScreenType != ComposeActivity.EDIT_DRAFT) {
            mBody.evaluateJavascript(JSRepository.RESTRICT_DOCUMENT_WIDE_STYLES_TO_ORIGINAL_CONTENT);
        }
        mBody.evaluateJavascript(JSRepository.ATTACH_SHOW_MSG_DETAILS_TOGGLE);
        super.onLoadSucceeded(url);
    }

    private String getFormattedAddressList(String[] addressList) {
        StringBuilder returnValue = new StringBuilder();
        for (String recipient : addressList) {
            if (returnValue.length() != 0) {
                returnValue.append("; ");
            }
            returnValue.append(recipient.replace("<", "&lt;").replace(">", "&gt;")
                    .replace("\"", ""));
        }
        return returnValue.toString();
    }

    private class TitleAdapter extends ArrayAdapter<String> {

        private LayoutInflater mInflater;

        public TitleAdapter(Context context) {
            super(context, R.layout.compose_title_item, R.id.mode, mActivityContext.getResources()
                    .getStringArray(R.array.compose_modes));
        }

        private LayoutInflater getInflater() {
            if (mInflater == null) {
                mInflater = LayoutInflater.from(mActivityContext);
            }
            return mInflater;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getInflater().inflate(R.layout.compose_title_display_item, null);
            }
            ((TextView) convertView.findViewById(R.id.mode)).setText(getItem(position));
            return super.getView(position, convertView, parent);
        }
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        int cachedResponseType = mScreenType;
        mScreenType = itemPosition;
        // Rebuild contents of the screen, but leave user changes as is.
        if (cachedResponseType != mScreenType) {
            boolean notDirtyBeforeReset = !isDirty();
            mRecipientsFrag.reset();
            mSubject.setText("");
            MessageValue referenceMessage = mController.getReferenceMessage();
            mRecipientsFrag.initializeFromMessage(mFromSpinner.getCurrentAccount().account.getEmailAddress(), referenceMessage, mScreenType);
            mSubject.setText(ComposeUtils.buildFormattedSubject(mActivityContext.getResources(),
                    referenceMessage.mSubject, mScreenType));
            // Switching between reply contexts should not mark the screen as dirty.
            if (notDirtyBeforeReset)
                setDirty(false);
        }
        // TODO: Handle attachments
        return true;
    }

}
