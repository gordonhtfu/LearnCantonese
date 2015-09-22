package com.blackberry.qa.emails;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.blackberry.message.service.MessageBodyValue;
import com.blackberry.message.service.MessageContactValue;
import com.blackberry.message.service.MessageValue;
import com.blackberry.message.service.MessagingService;
import com.blackberry.provider.AccountContract.Account;
import com.blackberry.provider.MessageContract;
import com.blackberry.provider.MessageContract.Message;

/**
 * Test class to support initialization of MessagingService and preparing
 * data for MessagingService actions.
 */
public class TestEmail {
    private final static String TAG = TestEmail.class.getSimpleName();

    /**
     * Account id of the email address passed in in the constructor
     */
    public Long mAccountId = null;
    /**
     * Context of the calling app/service/test
     */
    private Context mContext;
    /**
     * Contents of the email to be found/sent
     */
    private MessageValue mEmail;

    /**
     * Initial setup for an email from the specified email address
     * @param context current context
     * @param emailAddr email address eg. example@blah.com
     */
    public TestEmail(Context context, String emailAddr) {
        mContext = context;
        mAccountId = getAccountId(emailAddr);
        mEmail = new MessageValue();
        mEmail.mAccountId = mAccountId;
        MessageContactValue contact = new MessageContactValue();
        contact.mAddress = emailAddr;
        contact.mFieldType = MessageContract.MessageContact.FieldType.FROM;
        contact.mAddressType = MessageContract.MessageContact.AddrType.EMAIL;
        mEmail.add(contact);
    }

    /**
     * Gets the account id for the email provided
     * @param email email address eg. example@blah.com
     * @exception IllegalArgumentException if account does not exist.
     * @return account id
     */
    private long getAccountId(String email) {
        Log.d(TAG, "Getting Account ID for " + email);
        String[] accountProjection = { Account._ID, Account.TYPE, Account.NAME };
        String where = Account.NAME + "=?";
        String[] whereArgs = {email};
        Cursor accounts = null;
        Long accountId = null;
        accounts = mContext.getContentResolver().query(Account.CONTENT_URI, accountProjection,
                where, whereArgs, null);
        if (accounts != null && accounts.moveToNext()) {
            int idIdx = accounts.getColumnIndex(Account._ID);
            accountId = accounts.getLong(idIdx);
        }
        if (accounts != null)
            accounts.close();
        if (accountId == null) {
            throw new IllegalArgumentException("Email account not valid or does not exist");
        }
        Log.d(TAG, "Found account id for " + email + ": " + Long.toString(accountId));
        return accountId;
    }

    /**
     * Sets the message values (email contents) to an entire other set of values
     * @param mv already built set of message values
     */
    public void setMessageValue(MessageValue mv) {
        mEmail = mv;
    }

    /**
     * Adds a to section to the emailAddr
     * @param emailAddr email address to send to
     */
    public void addTo(String emailAddr) {
        MessageContactValue contact = new MessageContactValue();
        contact.mAddress = emailAddr;
        contact.mFieldType = MessageContract.MessageContact.FieldType.TO;
        contact.mAddressType = MessageContract.MessageContact.AddrType.EMAIL;
        mEmail.add(contact);
    }
    /**
     * Sets the message subject
     * @param subject message subject
     */
    public void setSubject(String subject) {
        mEmail.mSubject = subject;
    }

    /**
     * Sets to the message body to the specified body
     * @param body body of the message
     */
    public void setBody(String body) {
        MessageBodyValue msgBody = new MessageBodyValue();
        msgBody.mContentBytes = body.getBytes();
        msgBody.mType = MessageContract.MessageBody.Type.TEXT;
        mEmail.setSingleBody(msgBody);
    }
    /**
     * Sends a message with the parameters that has been built by this object
     */
    public void sendMessage() {
        String messageId;
        Log.d(TAG, "Setting up messaging service for account: " + Long.toString(mAccountId));
        MessagingService service = new MessagingService(mAccountId, mContext);
        service.waitForConnection(10000);
        Log.d(TAG, "Sending message from account " + Long.toString(mAccountId));
        messageId = service.sendMessage(mEmail);
        service.close();
        if (messageId == null) {
            throw new IllegalArgumentException("Could not send message.  Check that it was" +
                    " properly constructed.");
        }
    }
    /**
     * Searches the content provider for a message that matches the message currently created in
     * this object and deletes it if it exists
     * @throws Exception Thrown if message does not exist to delete
     */
    public void deleteMessage() throws Exception {
        Cursor emailCursor = getMessageCursor();
        if (emailCursor.getCount() == 0) {
            throw new Exception("Message did not exist to delete");
        }
        int uriIndex = emailCursor.getColumnIndex(Message.ENTITY_URI);
        String messageUri = emailCursor.getString(uriIndex);
        Log.d(TAG, "Deleting message for account " + mAccountId + ": " + messageUri);
        MessagingService service = new MessagingService(mAccountId, mContext);
        service.waitForConnection(10000);
        service.deleteMessage(messageUri);
        service.close();
    }

    /**
     * Gets a cursor from the messaging content provider that matches the message built within this
     * object
     * @return cursor of all matching records
     */
    private Cursor getMessageCursor() {
        //TODO: expand to check all fields, not just subject
        //String[] to = mEmail.getAddresses(MessageContract.MessageContact.FieldType.TO);
        //String[] from = mEmail.getAddresses(MessageContract.MessageContact.FieldType.FROM);
        //String[] cc = mEmail.getAddresses(MessageContract.MessageContact.FieldType.CC);
        //String[] bcc = mEmail.getAddresses(MessageContract.MessageContact.FieldType.BCC);
        //String[] replyTo = mEmail.getAddresses(MessageContract.MessageContact.FieldType.REPLY_TO);
        //mEmail.getAttachments()
        //List<MessageBodyValue> bodies = mEmail.getBodies();
        String subject = mEmail.mSubject;
        String mProjection[] = Message.DEFAULT_PROJECTION;    // Contract class constant for the _ID column name
        String mSelectionClause = Message.SUBJECT + "= ?";
        String[] mSelectionArgs = {subject};
        Cursor cursor = mContext.getContentResolver().query(Message.CONTENT_URI,  mProjection,
                mSelectionClause, mSelectionArgs, null);
        cursor.moveToFirst();
        return cursor;
    }

    /**
     * True if the email constructed is already in the content provider
     * @return
     */
    public boolean isEmailInProvider() {
        Cursor emailCursor = getMessageCursor();
        return (emailCursor.getCount() > 0);
    }

    /**
     * Gets the email most recently added to the messaging content provider for a given email
     * account.  Note: this may not be the most recent email in the list if something old is
     * suddenly synced to the device
     * @param context current context
     * @param account email account (eg. blah@blah.com)
     * @return TestEmail object populated with the properties of the most recent email
     * @throws Exception Thrown if no emails are present in the given account
     */
    public static TestEmail getMostRecentEmail(Context context, String account) throws Exception {
        TestEmail te = new TestEmail(context, account);
        String accountIdStr = te.mAccountId.toString();
        Cursor cursor = context.getContentResolver().query(Message.CONTENT_URI,
                Message.DEFAULT_PROJECTION, Message.ACCOUNT_ID + "=?", new String[] {accountIdStr},
                null);
        if (!cursor.moveToLast()) {
            throw new Exception("No emails present in account");
        }
        te.setMessageValue(new MessageValue(cursor));
        return te;
    }
}
