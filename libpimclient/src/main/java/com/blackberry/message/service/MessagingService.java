package com.blackberry.message.service;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.blackberry.provider.AccountContract;
import com.blackberry.provider.AccountContract.AccountAttribute;
import com.blackberry.provider.MessageContract;

/**
 * A wrapper for IMessagingService with some helper functions. Unless specified
 * otherwise
 * 
 * @author vrudenko
 * 
 */
public class MessagingService {

    private IMessagingService mService = null;
    private Context mContext = null;
    private ServiceConnection mConnection;
    private boolean mConnectionBound = false;

    private static final String TAG = MessagingService.class.getSimpleName();

    /**
     * Creates messaging service based on account ID and context. This method
     * initializes service connection and should be performed in the onCreate()
     * method.
     * 
     * @param accountId
     *            account ID from the Accounts Content Provider
     * @param context
     *            context to be used for connection callbacks, i.e. current
     *            Activity
     */
    public MessagingService(long accountId, Context context) {
        mContext = context;
        mConnection = new MessagingServiceConnection();
        Intent intent = new Intent();

        String packageName = null, className = null;
        ContentResolver resolver = context.getContentResolver();
        Cursor accountAttrs = resolver.query(AccountContract.AccountAttribute.CONTENT_URI, new String[] {
                AccountContract.AccountAttribute.ATTR_NAME,
                AccountContract.AccountAttribute.ATTR_VALUE }, AccountContract.AccountAttribute.ACCOUNT_KEY + "=?",
                new String[] { String.valueOf(accountId) }, null);
        try {
            int nameIdx = accountAttrs.getColumnIndex(AccountContract.AccountAttribute.ATTR_NAME);
            int valueIdx = accountAttrs.getColumnIndex(AccountContract.AccountAttribute.ATTR_VALUE);
            if (nameIdx < 0 || valueIdx < 0) {
                throw new MessageServiceException("Can't discover account properties, field indexes wrong: " + nameIdx + ", " + valueIdx);
            }
            while (accountAttrs.moveToNext()) {
                String name = accountAttrs.getString(nameIdx);
                if (TextUtils.equals(name, AccountAttribute.ATT_NAME_MESSAGING_SERVICE_PACKAGE)) {
                    packageName = accountAttrs.getString(valueIdx);
                } else if (TextUtils.equals(name, AccountAttribute.ATT_NAME_MESSAGING_SERVICE_CLASS)) {
                    className = accountAttrs.getString(valueIdx);
                }
            }
            if (packageName == null || className == null) {
                throw new MessageServiceException("Can't discover account properties, service class or package name is null - "
                        + packageName + ", " + className);
            }
            ComponentName componentName = new ComponentName(packageName, className);
            intent.setComponent(componentName);
            // during connection we specify account ID
            intent.putExtra(MessageContract.Message.ACCOUNT_ID, accountId);
            mConnectionBound = context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

            if (mConnectionBound) {
                Log.i(TAG, "Connected to messaging service successfully");
            } else {
                Log.w(TAG, "Failed to connect to messaging service");
            }
        } finally {
            accountAttrs.close();
        }
    }

    /**
     * Sets new flags in the message state
     * 
     * @param messageId
     *            message ID retrieved from DomainProvider
     * @param flagsMask
     *            a bit mask comprising from
     *            {@link MessageContract.Message.State} values
     */
    public void setMessageFlags(long accountId, String messageId, long flagsMask, boolean replace) {
        checkConnected();
        ServiceResult result = new ServiceResult();
        try {
            mService.setMessageFlags(accountId, messageId, flagsMask, replace, result);
        } catch (RemoteException e) {
            Log.i(TAG, "Failed to set message flags", e);
            throw new MessageServiceException("Can't communicate with remote service");
        }
        checkServiceResult(result);
    }

    /**
     * Clears specified flags from the message state
     * 
     * @param messageId
     *            message ID retrieved from DomainProvider
     * @param flagsMask
     *            a bit mask comprising from
     *            {@link MessageContract.Message.State} values
     */
    public void clearMessageFlags(long accountId, String messageId, long flagsMask) {
        checkConnected();
        ServiceResult result = new ServiceResult();
        try {
            mService.clearMessageFlags(accountId, messageId, flagsMask, result);
        } catch (RemoteException e) {
            Log.i(TAG, "Failed to clear message flags", e);
            throw new MessageServiceException("Can't communicate with remote service");
        }
        checkServiceResult(result);
    }

    /**
     * Deletes a message
     * 
     * @param messageId
     *            message ID retrieved from DomainProvider
     */
    public void deleteMessage(long accountId, String messageId) {
        checkConnected();
        ServiceResult result = new ServiceResult();
        try {
            mService.deleteMessage(accountId, messageId, result);
        } catch (RemoteException e) {
            Log.i(TAG, "Failed to delete message", e);
            throw new MessageServiceException("Can't communicate with remote service");
        }
        checkServiceResult(result);
    }

    /**
     * Send a new message
     * 
     * @param message
     *            message to send
     * @return message ID of the sent message
     */
    public String sendMessage(long accountId, MessageValue message) {
        checkConnected();
        ServiceResult result = new ServiceResult();
        String messageId = null;
        try {
            messageId = mService.sendMessage(accountId, message, result);
        } catch (RemoteException e) {
            Log.i(TAG, "Failed to send message", e);
            throw new MessageServiceException("Can't communicate with remote service");
        }
        checkServiceResult(result);
        return messageId;
    }

    /**
     * Saves a message - draft folder for example
     * @param message
     * @return
     */
    public String saveMessage(long accountId, MessageValue message){
        checkConnected();
        ServiceResult result = new ServiceResult();
        String messageUri = null;
        try {
            messageUri = mService.saveMessage(accountId, message, result);
        } catch (RemoteException e) {
            Log.i(TAG, "Failed to send message", e);
            throw new MessageServiceException("Can't communicate with remote service");
        }
        checkServiceResult(result);
        return messageUri;
    }

    /**
     * reply to a message
     * @param originalMessageUri
     * @param message
     * @return
     */
    public String replyMessage(long accountId, String originalMessageUri, MessageValue message) {
        checkConnected();
        ServiceResult result = new ServiceResult();
        String messageUri = null;
        try {
            messageUri = mService.replyMessage(accountId, originalMessageUri, message, result);
        } catch (RemoteException e) {
            Log.i(TAG, "Failed to replyMessage message", e);
            throw new MessageServiceException("Can't communicate with remote service");
        }
        checkServiceResult(result);
        return messageUri;
    }

    /**
     * forward a message
     * @param originalMessageUri
     * @param message
     * @return
     */
    public String forwardMessage(long accountId, String originalMessageUri, MessageValue message) {
        checkConnected();
        ServiceResult result = new ServiceResult();
        String messageUri = null;
        try {
            messageUri = mService.replyMessage(accountId, originalMessageUri, message, result);
        } catch (RemoteException e) {
            Log.i(TAG, "Failed to forwardMessage message", e);
            throw new MessageServiceException("Can't communicate with remote service");
        }
        checkServiceResult(result);
        return messageUri;
    }

    /**
     * Starts an asynchronous remote message search operation. The messages will
     * be populated in the domain provider in a dedicated folder which can be
     * referenced by the returned folder ID
     * 
     * @param filter
     *            filter for message search
     * @return folder ID to reference messages in DomainProvider
     */
    public String startRemoteSearch(long accountId, MessageFilter filter) {
        checkConnected();
        ServiceResult result = new ServiceResult();
        String folderId = null;
        try {
            folderId = mService.startRemoteSearch(accountId, filter, result);
        } catch (RemoteException e) {
            Log.i(TAG, "Failed to start remote search", e);
            throw new MessageServiceException("Can't communicate with remote service");
        }
        checkServiceResult(result);
        return folderId;
    }

    /**
     * Requests service to fetch more messages to the specified folder. This can
     * be used with folders containing messages of a remote search operation
     * 
     * @param folderId
     *            folder ID returned by
     *            {@link #startRemoteSearch(MessageFilter)} method
     */
    public void fetchMore(long accountId, String folderId) {
        checkConnected();
        ServiceResult result = new ServiceResult();
        try {
            mService.fetchMore(accountId, folderId, result);
        } catch (RemoteException e) {
            Log.i(TAG, "Failed to fetch more messages", e);
            throw new MessageServiceException("Can't communicate with remote service");
        }
        checkServiceResult(result);
    }

    /**
     * Returns true if service is connected
     * 
     * @return true if connected, false otherwise
     */
    public synchronized boolean isConnected() {
        return mService != null;
    }

    /**
     * Waits for service connection for given number of milliseconds. Don't call
     * this method on UI thread. This method returns when connection is
     * established or specified
     * 
     * @param millis
     *            time to wait, 0 = indefinitely
     */
    public void waitForConnection(long millis) {
        if (isConnected())
            return;
        if (Looper.myLooper() == Looper.getMainLooper())
            throw new IllegalStateException("Can't wait on the main UI thread");
        long deadline = System.currentTimeMillis() + millis;
        try {
            synchronized (this) {
                while (!isConnected()) {
                    if (millis == 0) {
                        this.wait();
                    } else {
                        long toWait = deadline - System.currentTimeMillis();
                        if (toWait <= 0)
                            return;
                        this.wait(toWait);
                    }
                }
            }
        } catch (InterruptedException ex) {
            Log.w(TAG, "Interrupted wait", ex);
        }
    }

    /**
     * Closes connection to messaging service and cleans up temporary resources
     */
    public void close() {
        if (mConnectionBound) {
            try {
                mContext.unbindService(mConnection);
            } catch (Exception ex) {
                Log.w(TAG, ex.getMessage());
            }

        }
    }

    private void checkConnected() {
        if (!isConnected())
            throw new IllegalStateException("Messaging service is not yet connected");
    }

    private static void checkServiceResult(ServiceResult result) throws IllegalArgumentException, UnsupportedOperationException,
            MessageServiceException {
        switch (result.getResponseCode()) {
        case ServiceResult.CODE_SUCCESS:
            return;
        case ServiceResult.CODE_INVALID_ARGUMENT:
            throw new IllegalArgumentException(result.getResponseMessage());
        case ServiceResult.CODE_OPERATION_NOT_SUPPORTED:
            throw new UnsupportedOperationException(result.getResponseMessage());
        case ServiceResult.CODE_SERVICE_ERROR:
        default:
            throw new MessageServiceException(result.getResponseMessage());
        }
    }

    private class MessagingServiceConnection implements ServiceConnection {
        public void onServiceConnected(ComponentName className, IBinder service) {
            synchronized (MessagingService.this) {
                mService = IMessagingService.Stub.asInterface(service);
                MessagingService.this.notifyAll();
                Log.d(TAG, "OnServiceConnected");
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
            Log.d(TAG, "onServiceDisconnected");
        }
    };

    public static class MessageServiceException extends RuntimeException {

        private static final long serialVersionUID = 2099661879510362410L;

        /**
         * Constructs a new {@code RemoteServiceException} that includes the
         * current stack trace.
         */
        public MessageServiceException() {
        }

        /**
         * Constructs a new {@code RemoteServiceException} with the current
         * stack trace and the specified detail message.
         * 
         * @param detailMessage
         *            the detail message for this exception.
         */
        public MessageServiceException(String detailMessage) {
            super(detailMessage);
        }

    }

}
