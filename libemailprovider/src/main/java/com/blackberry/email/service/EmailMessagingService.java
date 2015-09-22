
package com.blackberry.email.service;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.blackberry.common.utils.LogTag;
import com.blackberry.common.utils.LogUtils;
import com.blackberry.email.utils.EmailMessageUtilities;
import com.blackberry.email.utils.FolderUtilities;
import com.blackberry.intent.PimIntent;
import com.blackberry.message.service.AbstractMessagingService;
import com.blackberry.message.service.IMessagingService;
import com.blackberry.message.service.MessageValue;
import com.blackberry.message.service.ServiceResult;
import com.blackberry.message.utilities.MessagingProviderUtilities;
import com.blackberry.provider.AccountContract;
import com.blackberry.provider.MessageContract;
import com.blackberry.provider.MessageContract.Folder;
import com.blackberry.provider.MessageContract.Message;

/**
 * Implementation of IMessagingService on top of EmailProvider
 * 
 * @author vrudenko
 */
public class EmailMessagingService extends IntentService {

    private static final String TAG = LogTag.getLogTag();

    public EmailMessagingService() {
        super(EmailMessagingService.class.getName());
    }

    // Intent service handler method
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();

        try {
            // this uri can be different based on the operation - for example it
            // can be
            // a message uri or a folder uri @TODO validate incoming data
            String strUriToActOn = intent.getDataString();

            if (TextUtils.equals(action, PimIntent.PIM_MESSAGE_ACTION_MARK_UNREAD)) {
                markReadUnread(strUriToActOn, false);
            } else if (TextUtils.equals(action, PimIntent.PIM_MESSAGE_ACTION_MARK_READ)) {
                markReadUnread(strUriToActOn, true);
            } else if (TextUtils.equals(action, PimIntent.PIM_ITEM_ACTION_DELETE)) {
                deleteEmail(strUriToActOn, null);
            } else if (TextUtils.equals(action, PimIntent.PIM_MESSAGE_ACTION_FLAG)) {
                setEmailMessageFlags(strUriToActOn, Message.State.FLAGGED, false);
            } else if (TextUtils.equals(action, PimIntent.PIM_MESSAGE_ACTION_CLEAR_FLAG)) {
                this.clearEmailMessageFlags(strUriToActOn, Message.State.FLAGGED);
            } else {
                Log.w(TAG, "Unrecognized command - " + action);
            }
        } catch (Exception ex) {
            Log.w(TAG, "Failed to process command " + action, ex);
        }
    }

    // Represents an active connection from a client
    private class ServiceConnection extends AbstractMessagingService {

        public ServiceConnection() {
        }

        @Override
        public String sendMessage(long accountId, MessageValue message, ServiceResult result)
                throws RemoteException {
            return sendOrSaveEmail(accountId, message, true);
        }

        @Override
        public String saveMessage(long accountId, MessageValue message, ServiceResult result)
                throws RemoteException {
            return sendOrSaveEmail(accountId, message, false);
        }

        @Override
        public void deleteMessage(long accountId, String messageUri, ServiceResult result)
                throws RemoteException {
            EmailMessagingService.this.deleteEmail(messageUri, result);
        }

        @Override
        public void setMessageFlags(long accountId, String messageUri, long flagsMask,
                boolean replace,
                ServiceResult result)
                throws RemoteException {
            setEmailMessageFlags(messageUri, flagsMask, replace);
        }

        @Override
        public void clearMessageFlags(long accountId, String messageUri, long flagsMask,
                ServiceResult result)
                throws RemoteException {
            clearEmailMessageFlags(messageUri, flagsMask);
        }

        @Override
        public String replyMessage(long accountId, String originalMessageUri, MessageValue message,
                ServiceResult result) throws RemoteException {
            return replyOrForward(accountId, originalMessageUri, message, true);
        }

        @Override
        public String forwardMessage(long accountId, String originalMessageUri,
                MessageValue message,
                ServiceResult result) throws RemoteException {
            return replyOrForward(accountId, originalMessageUri, message, false);
        }
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "Creating EmailMessagingService ...");
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind start");
        IMessagingService.Stub binder = null;
        // will keep the account data for now as it should ensure that there is
        // a valid
        // account in our system,
        long accountId = intent.getLongExtra(MessageContract.Message.ACCOUNT_ID, -1);
        if (accountId == -1) {
            throw new IllegalArgumentException("Missing account ID");
        }
        binder = new ServiceConnection();
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "onUnbind ");
        // TODO cleanup client temp resources such as Remote Search folders
        // can use the account id passed in the intent to ensure correct cleanup
        // is done
        return super.onUnbind(intent);
    }

    /**
     * the following method will set the message state flags.
     * 
     * @param messageUri message uri to change state on
     * @param flagsMask Incoming mask to be set
     * @param replace if true the incoming flagMask will replace the current
     *            values, if false then only the flags in the flagMask will be
     *            changed
     * @throws RemoteException
     */
    private void setEmailMessageFlags(String messageUri, long flagsMask, boolean replace)
            throws RemoteException {
        try {
            ContentResolver cr = getContentResolver();
            if (messageUri == null) {
                throw new Exception("Message ID is not specified");
            }

            long newFlags = flagsMask;
            long currentFlags = this.getMessageStateValue(cr, messageUri);
            // @TODO validate that the Flags can be applied to the message.
            // this would mean loading the message - checking current flags
            // against
            // new flags
            if (!replace) {
                newFlags = currentFlags | flagsMask;
            }

            // will only update if different
            if (newFlags != currentFlags) {
                Uri msgUri = Uri.parse(messageUri);

                ContentValues values = new ContentValues();
                values.put(MessageContract.Message.STATE, newFlags);
                int count = cr.update(msgUri, values, null, null);

                if (count > 0) {
                    triggerSyncWithMessageChanges(this, msgUri, count);
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            Log.w(TAG, "Failed to set flags on email message", ex);
            throw new RemoteException("Failed to set flags on email message");
        }
    }

    private void clearEmailMessageFlags(String messageUri, long flagsMask) throws RemoteException {
        try {
            ContentResolver cr = getContentResolver();

            if (messageUri == null) {
                throw new Exception("Message messageUri is not specified");
            }

            long currentFlags = getMessageStateValue(cr, messageUri);
            // make sure flags are ok
            if (currentFlags > -1) {
                long newFlagValues = currentFlags & (~flagsMask);

                // we have current an UNREAD an READ bits which are mutually
                // exclusive at this time
                // I will ensure bits are set properly so clients do not have to
                // --
                // May remove UNREAD state in future
                if ((flagsMask & MessageContract.Message.State.UNREAD) != 0) {
                    newFlagValues |= MessageContract.Message.State.READ;
                } else if ((flagsMask & MessageContract.Message.State.READ) != 0) {
                    newFlagValues |= MessageContract.Message.State.UNREAD;
                }

                // see if our new value is different before setting it
                if (newFlagValues != currentFlags) {
                    setEmailMessageFlags(messageUri, newFlagValues, true);
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            Log.w(TAG, "Failed to clear flags on email message", ex);
            throw new RemoteException("Failed to clear flags on email message");
        }
    }

    private void markReadUnread(String messageUri, boolean markRead) throws RemoteException {
        try {
            if (messageUri == null) {
                throw new Exception("Message messageUri is not specified");
            }
            // using imp of clear flags for read/unread
            long flagsToClear = markRead ? MessageContract.Message.State.UNREAD
                    : MessageContract.Message.State.READ;
            clearEmailMessageFlags(messageUri, flagsToClear);

        } catch (Exception ex) {
            ex.printStackTrace();
            Log.w(TAG, "Failed modify message", ex);
            throw new RemoteException("Failed to modify message");
        }
    }

    private String sendOrSaveEmail(long accountId, MessageValue message, boolean send)
            throws RemoteException {
        try {
            // @TODO validate incomming message

            String msgUriAsString = null;
            int folderType = Folder.Type.OUTBOX;

            if (!send) {
                //doing some validation here - so we need to see what the original state of the
                //message is if there is an id - we can only update an message if it is in a draft state
                //any other state should indicate to us that we need to save a new message draft
                //cannot trust incoming message
                if (message.mId != MessageValue.NOT_SAVED) {
                    long currentMsgState =
                            getMessageStateValue(this.getContentResolver(),
                                    ContentUris.withAppendedId(Message.CONTENT_URI, message.mId).toString());
                    // see if we need to clear id
                    if (currentMsgState == -1 || (currentMsgState & Message.State.DRAFT) == 0) {
                        message.mId = MessageValue.NOT_SAVED;
                        message.mEntityUri = null;

                    }
                }
                folderType = Folder.Type.DRAFT;
                message.mState = Message.State.DRAFT;
            } else {
                // will mark as OutGoing
                message.mState &= ~Message.State.DRAFT;
                message.mState |= Message.State.OUTGOING_MESSAGE;
            }
            // get folderId
            final long outBoxFolderId = MessagingProviderUtilities.findFolderOfType(this,
                    accountId, folderType);

            if (outBoxFolderId != -1) {
                message.mFolderId = outBoxFolderId;
                message.mTimeStamp = System.currentTimeMillis();
                message.mAccountId = accountId;

                // now for the creation/update of the message data
                Uri msgUri = EmailMessageUtilities.insertOrUpdateMessage(this, message);
                if (msgUri != null) {
                    msgUriAsString = msgUri.toString();
                    // kick SAM
                    startSync(this, accountId, message.mFolderId, 0);
                }
            }

            return msgUriAsString;

        } catch (Exception ex) {
            ex.printStackTrace();
            Log.w(TAG, "Failed email send/save", ex);
            throw new RemoteException("Can't send/save email");
        }
    }

    private void deleteEmail(String messageUri, ServiceResult result)
            throws RemoteException {
        ContentResolver cr = getContentResolver();
        Uri uri = Uri.parse(messageUri);
        int count = cr.delete(uri, null, null);

        if (count > 0) {
            triggerSyncWithMessageChanges(this, uri, count);
        } else {
            result.setResponseCode(ServiceResult.CODE_INVALID_ARGUMENT);
            result.setResponseMessage("No email found by ID");
        }

    }

    private String replyOrForward(long accountId, String originalMessageUri, MessageValue message,
            boolean isReply)
            throws RemoteException {
        String retValue = null;

        Uri orgMessageUri = Uri.parse(originalMessageUri);
        // 1. do an message save on the new message data
        // 2. update original message flags state (somewhat following what ECP
        // did for now)
        message.mSourceKey = ContentUris.parseId(orgMessageUri);
        message.mAccountId = accountId;
        message.mState = isReply ? MessageContract.Message.State.TYPE_REPLY
                : MessageContract.Message.State.TYPE_FORWARD;
        retValue = this.sendOrSaveEmail(accountId, message, true);
        // update org msg
        ContentValues values = new ContentValues();
        values.put(MessageContract.Message.STATE,
                isReply ? MessageContract.Message.State.REPLIED_TO
                        : MessageContract.Message.State.FORWARDED);
        getContentResolver().update(orgMessageUri, values, null, null);

        return retValue;
    }

    private long getMessageStateValue(ContentResolver cr, String messageUri) {
        Uri msgUri = Uri.parse(messageUri);

        // just getting the status data
        Cursor cursor = cr.query(msgUri,
                new String[] {
                    MessageContract.Message.STATE
                }, null, null, null);

        long currentFlags = -1;
        try {
            if (cursor != null && cursor.moveToFirst()) {
                currentFlags = cursor.getLong(0);// only one item in
                                                // projection
            }

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return currentFlags;
    }

    private void triggerSyncWithMessageChanges(Context context, Uri uri, int changeCount) {
        Cursor cursor = context.getContentResolver().query(uri,
                MessageContract.Message.DEFAULT_PROJECTION, null, null,
                null);

        try {
            if (cursor != null && cursor.moveToFirst()) {
                MessageValue message = new MessageValue(cursor);
                // kick SAM
                startSync(this, message.mAccountId, message.mFolderId, changeCount);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private static android.accounts.Account getAccountManagerAccount(Context context,
            final long accountId) {

        Cursor cursor = context.getContentResolver().query(AccountContract.Account.CONTENT_URI,
                new String[] {
                        AccountContract.Account.NAME, AccountContract.Account.TYPE
                }, AccountContract.Account._ID + "=" + accountId, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            return new android.accounts.Account(cursor.getString(0), cursor.getString(1));
        }

        return null;
    }

    private void startSync(Context context, final long accountId, final long folderId,
            final int changesCount) {

        final android.accounts.Account account =
                getAccountManagerAccount(context, accountId);

        final Bundle extras = FolderUtilities.createSyncBundle(folderId, changesCount);
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_DO_NOT_RETRY, true);
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        if (changesCount != 0) {
            extras.putInt(FolderUtilities.SYNC_EXTRA_DELTA_MESSAGE_COUNT, changesCount);
        }

        ContentResolver.requestSync(account, MessageContract.AUTHORITY, extras);
        LogUtils.i(TAG, "requestSync EmailProvider startSync %s, %s", account.toString(),
                extras.toString());
    }
}
