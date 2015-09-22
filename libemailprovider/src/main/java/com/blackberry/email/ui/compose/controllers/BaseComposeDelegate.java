package com.blackberry.email.ui.compose.controllers;

import android.net.Uri;
import android.util.Log;

import com.blackberry.email.ui.compose.views.ComposeScreen;
import com.blackberry.message.service.MessageValue;
import com.blackberry.message.service.MessagingService;
import com.blackberry.provider.MessageContract;

public class BaseComposeDelegate {

    private ComposeActivity mActivityContext;
    private Object mDraftLock = new Object();
    private ComposeScreen mEditor = null;
    private MessagingService mMessagingService;
    private long mConnectedAccountID = -1;

    private static final String LOG_TAG = BaseComposeDelegate.class
            .getSimpleName();

    public BaseComposeDelegate(ComposeActivity activityContext,
            ComposeScreen editor) {
        mActivityContext = activityContext;
        mEditor = editor;
    }

    public String save(MessageValue message) {
        if (mMessagingService != null && mMessagingService.isConnected()) {
            return mMessagingService.saveMessage(mConnectedAccountID, message);
        } else {
            Log.e(LOG_TAG, "Messaging Service not connected, could not save!");
        }
        return null;
    }

    public String send(MessageValue message, Uri originalMessageUri) {
        String msgId = null;
        executePreSendRules();
        if (mMessagingService != null && mMessagingService.isConnected()) {
            switch (mEditor.mScreenType) {
                case ComposeActivity.COMPOSE:
                case ComposeActivity.EDIT_DRAFT:
                    msgId = mMessagingService.sendMessage(mConnectedAccountID, message);
                    break;

                case ComposeActivity.REPLY:
                case ComposeActivity.REPLY_ALL:
                    msgId = mMessagingService.replyMessage(mConnectedAccountID,
                            originalMessageUri.toString(), message);
                    break;

                case ComposeActivity.FORWARD:
                    msgId = mMessagingService.forwardMessage(mConnectedAccountID,
                            originalMessageUri.toString(), message);
                    break;

                default:
                    Log.e(LOG_TAG, "Unable to handle send for this message");
                    break;
            }
        } else {
            Log.e(LOG_TAG, "Messaging Service not connected, could not send!");
        }

        return msgId;
    }
    
    public void markMessageRead(MessageValue msg) {
        if (msg != null && (msg.mState & MessageContract.Message.State.UNREAD) != 0) {   
            if (mMessagingService != null && mMessagingService.isConnected()) {
                mMessagingService.clearMessageFlags(mConnectedAccountID,
                        msg.mEntityUri.toString(), MessageContract.Message.State.UNREAD);
            } else {
                Log.e(LOG_TAG, "Messaging Service not connected, could not mark the message as read!");
            }
        }
    }

    private void executePreSendRules() {
        // TODO Auto-generated method stub
    }

    public MessagingService getValidService(long selectedAccountID) {
        if (mConnectedAccountID != selectedAccountID) {
            mConnectedAccountID = selectedAccountID;

            if (mMessagingService != null) {
                mMessagingService.close();
                mMessagingService = null;
            }
            mMessagingService = new MessagingService(selectedAccountID,
                    mActivityContext);
        }

        return mMessagingService;

    }

}
