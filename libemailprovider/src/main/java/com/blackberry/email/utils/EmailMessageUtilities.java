
package com.blackberry.email.utils;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.blackberry.common.Logging;
import com.blackberry.common.utils.LogUtils;
import com.blackberry.email.LegacyConversions;
import com.blackberry.email.internet.MimeUtility;
import com.blackberry.email.mail.Message;
import com.blackberry.email.mail.MessagingException;
import com.blackberry.email.mail.Part;
import com.blackberry.email.provider.contract.Account;
import com.blackberry.message.service.FolderValue;
import com.blackberry.message.service.MessageAttachmentValue;
import com.blackberry.message.service.MessageBodyValue;
import com.blackberry.message.service.MessageContactValue;
import com.blackberry.message.service.MessageValue;
import com.blackberry.message.utilities.MessagingProviderUtilities;
import com.blackberry.pimbase.provider.utilities.ContentProviderBulkOpsHelper;
import com.blackberry.pimbase.provider.utilities.Operation;
import com.blackberry.provider.MessageContract;
import com.blackberry.provider.MessageContract.MessageBody;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EmailMessageUtilities {

    private static final String TAG = EmailMessageUtilities.class.getSimpleName();
    // Values used in mFlagLoaded
    public static final int FLAG_LOADED_UNLOADED = 0;
    public static final int FLAG_LOADED_COMPLETE = 1;
    public static final int FLAG_LOADED_PARTIAL = 2;
    public static final int FLAG_LOADED_DELETED = 3;
    public static final int FLAG_LOADED_UNKNOWN = 4;

    /** a pseudo ID for "no message". */
    public static final long NO_MESSAGE = -1L;

    /**
     * The following method will either insert/update a Message and its parts
     * (Body, Contacts, Attachments) Note that when updating the parts - really
     * a replace is occurring on its parts this means that any old values not
     * contained in the MessageValue object will be removed and the new ones
     * will be created. If you need to just add a few new
     * MessageContacts/Attachments recommend using another api or ensure the
     * MessageValue has all parts/values
     * 
     * @param context
     * @param msg
     * @param ops
     */
    public static Uri insertOrUpdateMessage(Context context, MessageValue msg) {
        ArrayList<Operation> ops = new ArrayList<Operation>();

        addMessageOperations(context, msg, ops);
        // apply the batch
        Uri returnUri = null;
        try {
            ContentProviderResult[] results = ContentProviderBulkOpsHelper.commit(
                    context.getContentResolver(),
                    MessageContract.AUTHORITY, ops);

            if (results != null && results.length > 0) {
                // first one is the message but if an update occured
                // then all we get is a count - if insert then we get the uri
                ContentProviderResult msgPartResult = results[0];
                returnUri = msgPartResult.uri;
                if (returnUri == null && msgPartResult.count == 1 && msg.mId != -1) {
                    // update occured
                    returnUri =
                            ContentUris
                                    .withAppendedId(MessageContract.Message.CONTENT_URI, msg.mId);
                }
            }

        } catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
        }

        return returnUri;
    }

    /**
     * Builds Operations from a Message object which will be used in an
     * applyBatch operation
     * 
     * @param context
     * @param msg
     * @param ops
     * @return
     */
    public static void addMessageOperations(Context context, MessageValue msg,
            ArrayList<Operation> ops) {

        boolean isNewMessage = msg.isNew();
        ContentProviderOperation.Builder b;
        // First, save/update the message
        if (isNewMessage) {
            b = ContentProviderOperation.newInsert(MessageContract.Message.CONTENT_URI);
            msg.mMimeType = context
                    .getString(com.blackberry.lib.emailprovider.R.string.message_mimetype);

        } else {
            b = ContentProviderOperation.newUpdate(MessageContract.Message.CONTENT_URI)
                    .withSelection(MessageContract.Message._ID + "=?", new String[] {
                            Long.toString(msg.mId)
                    });
        }
        // Generate the snippet here, before we create the CPO for Message
        // @TODO see if we want this in MCP
        // if (msg.mText != null) {
        // msg.mSnippet = TextUtilities.makeSnippetFromPlainText(msg.mText);
        // } else if (msg.mHtml != null) {
        // msg.mSnippet = TextUtilities.makeSnippetFromHtmlText(msg.mHtml);
        // }

        // add to main container
        Operation msgOp = new Operation(b.withValues(msg.toContentValues(true)).build());
        ops.add(msgOp);

        // Create and save the body
        // will see if body member has data first
        List<MessageBodyValue> bodies = msg.getBodies();
        MessageBodyValue body = null;
        if (bodies != null && bodies.size() > 0) {
            // for now will just support one body - in future will process all
            body = bodies.get(0);
        } else {
            // @NOTE will change logic in future this is old logic used by AS
            byte[] bodyBytes = null;
            int bodyType = -1;
            if (msg.mText != null) {
                bodyBytes = msg.mText.getBytes();
                bodyType = MessageContract.MessageBody.Type.TEXT;
            }
            if (msg.mHtml != null) {
                bodyBytes = msg.mHtml.getBytes();
                bodyType = MessageContract.MessageBody.Type.HTML;
            }

            if (bodyBytes != null) {
                body = new MessageBodyValue();
                body.mContentBytes = bodyBytes;
                body.mType = bodyType;
            }
        }

        if (msg.mSourceKey != 0) {
            body.mSyncData1 = Long.toString(msg.mSourceKey);
        }
        if (msg.mQuotedTextStartPos != 0) {
            body.mSyncData2 = Integer.toString(msg.mQuotedTextStartPos);
        }

        // // We'll need this if we have a new message
        int messageBackValue = ops.size() - 1;
        // Only create a body if we've got some data
        if (body != null) {

            // Put our message id in the Body
            if (!isNewMessage) {
                body.mMessageId = msg.mId;
                // condition can occur if we are updating the body - but caller
                // did not set the body id (which means
                // it could look like a new body to us) - I am trying not do do
                // a blind delete for now so
                // we need to see if we have a body - as we really only support
                // a single body at this time
                if (body.isNew()) {
                    body.mId = MessageBodyValue.getBodyIdFromMessageId(context, msg.mId);
                }
            }

            if (body.mId < 1) {
                b = ContentProviderOperation.newInsert(MessageContract.MessageBody.CONTENT_URI);
            } else {
                b = ContentProviderOperation.newUpdate(
                        ContentUris.withAppendedId(MessageContract.MessageBody.CONTENT_URI,
                                body.mId));
            }
            b.withValues(body.toContentValues(true));
            // If we're new, create a back value entry
            Operation bodyOps = null;
            if (isNewMessage) {
                ContentValues backValues = new ContentValues();
                backValues.put(MessageContract.MessageBody.MESSAGE_ID, messageBackValue);
                b.withValueBackReferences(backValues);

                bodyOps = new Operation(b, MessageContract.MessageBody.MESSAGE_ID, messageBackValue);
            } else {
                bodyOps = new Operation(b.build());
            }
            // And add the Body operation, note will add builder type here
            // as if the applyBatch call fails do to size, we will need to
            // reset the withValueBackReferences
            ops.add(bodyOps);
        }

        // before we do any inserts/updates of Contact/Attachments we
        if (!isNewMessage) {
            // we are in an update, so we need to remove all previous
            // Contacts/Attachments as
            // the ones contained in the MessageValue will replace old ones.
            b = ContentProviderOperation.newDelete(MessageContract.MessageContact.CONTENT_URI)
                    .withSelection(MessageContract.MessageContact.MESSAGE_ID + "=?", new String[] {
                            Long.toString(msg.mId)
                    });

            Operation removeOps = new Operation(b.build());
            ops.add(removeOps);
            // now for attachments
            b = ContentProviderOperation.newDelete(MessageContract.MessageAttachment.CONTENT_URI)
                    .withSelection(MessageContract.MessageAttachment.MESSAGE_ID + "=?",
                            new String[] {
                                Long.toString(msg.mId)
                            });
            removeOps = new Operation(b.build());
            ops.add(removeOps);

        }

        // create MessageContacts
        if (!msg.getContacts().isEmpty()) {
            for (MessageContactValue contact : msg.getContacts()) {
                if (!isNewMessage) {
                    contact.mMessageId = msg.mId;
                }

                b = ContentProviderOperation.newInsert(MessageContract.MessageContact.CONTENT_URI)
                        .withValues(contact.toContentValues(true));

                Operation attOps = null;
                if (isNewMessage) {
                    b.withValueBackReference(MessageContract.MessageContact.MESSAGE_ID,
                            messageBackValue);
                    attOps = new Operation(b, MessageContract.MessageContact.MESSAGE_ID,
                            messageBackValue);
                } else {
                    attOps = new Operation(b.build());
                }

                ops.add(attOps);
            }
        }

        // Create the attachments, if any
        if (!msg.getAttachments().isEmpty()) {
            for (MessageAttachmentValue att : msg.getAttachments()) {
                if (!isNewMessage) {
                    att.mMessageId = msg.mId;
                }
                b = ContentProviderOperation.newInsert(
                        MessageContract.MessageAttachment.CONTENT_URI)
                        .withValues(att.toContentValues(true));

                Operation attOps = null;
                if (isNewMessage) {
                    b.withValueBackReference(MessageContract.MessageAttachment.MESSAGE_ID,
                            messageBackValue);
                    attOps = new Operation(b, MessageContract.MessageBody.MESSAGE_ID,
                            messageBackValue);
                } else {
                    attOps = new Operation(b.build());
                }

                ops.add(attOps);
            }
        }

    }

    /**
     * Copy one downloaded message (which may have partially-loaded sections)
     * into a newly created EmailProvider Message, given the account and mailbox
     * 
     * @param message the remote message we've just downloaded
     * @param account the account it will be stored into
     * @param folder the mailbox it will be stored into
     * @param loadStatus when complete, the message will be marked with this
     *            status (e.g. EmailContent.Message.LOADED)
     */
    public static void copyOneMessageToProvider(Context context, Message message, Account account,
            FolderValue folder, int loadStatus) {
        MessageValue localMessage = null;
        Cursor c = null;
        try {
            c = context.getContentResolver().query(
                    MessageContract.Message.CONTENT_URI,
                    MessageContract.Message.DEFAULT_PROJECTION,
                    MessageContract.Message.ACCOUNT_ID + "=?" +
                            " AND " + MessageContract.Message.FOLDER_ID + "=?" +
                            " AND " + MessageContract.Message.REMOTE_ID + "=?",
                    new String[] {
                            String.valueOf(account.mId),
                            String.valueOf(folder.mId),
                            String.valueOf(message.getUid())
                    },
                    null);
            if (c == null) {
                return;
            } else if (c.moveToNext()) {
                localMessage = new MessageValue(c);
            } else {
                localMessage = new MessageValue();
            }
            localMessage.mFolderId = folder.mId;
            localMessage.mAccountId = account.mId;
            copyOneMessageToProvider(context, message, localMessage, loadStatus);
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    /**
     * Copy one downloaded message (which may have partially-loaded sections)
     * into an already-created EmailProvider Message
     * 
     * @param message the remote message we've just downloaded
     * @param localMessage the EmailProvider Message, already created
     * @param loadStatus when complete, the message will be marked with this
     *            status (e.g. EmailContent.Message.LOADED)
     * @param context the context to be used for EmailProvider
     */
    public static void copyOneMessageToProvider(Context context, Message message,
            MessageValue localMessage, int loadStatus) {
        try {

            MessageBodyValue body = null;
            if (localMessage.mId != NO_MESSAGE) {
                body = MessageBodyValue.restoreBodyWithMessageId(context, localMessage.mId);
            }
            if (body == null) {
                body = new MessageBodyValue();
            }
            try {
                // Copy the fields that are available into the message object
                LegacyConversions.updateMessageFields(localMessage, message,
                        localMessage.mAccountId, localMessage.mFolderId);

                // Now process body parts & attachments
                ArrayList<Part> viewables = new ArrayList<Part>();
                ArrayList<Part> attachments = new ArrayList<Part>();
                MimeUtility.collectParts(message, viewables, attachments);

                final ConversionUtilities.BodyFieldData data =
                        ConversionUtilities.parseBodyFields(viewables);

                // set body and local message values
                // localMessage.setFlags(data.isQuotedReply,
                // data.isQuotedForward);
                // localMessage.mSnippet = data.snippet;
                if (data.textContent != null) {
                    body.mContentBytes = data.textContent.getBytes();
                    body.mType = MessageContract.MessageBody.Type.TEXT;
                } else if (data.htmlContent != null) {
                    body.mContentBytes = data.htmlContent.getBytes();
                    body.mType = MessageContract.MessageBody.Type.HTML;
                }

                // will only support one body for now and
                localMessage.setSingleBody(body);
                insertOrUpdate(localMessage, context);

                // process (and save) attachments
                if (loadStatus != FLAG_LOADED_PARTIAL
                        && loadStatus != FLAG_LOADED_UNKNOWN) {
                    // TODO(pwestbro): What should happen with unknown status?
                    LegacyConversions.updateAttachments(context, localMessage, attachments);
                } else {
                    MessageAttachmentValue att = new MessageAttachmentValue();
                    // Since we haven't actually loaded the attachment, we're
                    // just putting
                    // a dummy placeholder here. When the user taps on it, we'll
                    // load the attachment
                    // for real.
                    // TODO: This is not really a great way to model this....
                    // could we at least get
                    // the file names and mime types from the attachments? Then
                    // we could display
                    // something sensible at least. This may be impossible in
                    // POP, but maybe
                    // we could put a flag on the message indicating that there
                    // are undownloaded
                    // attachments, rather than this dummy placeholder, which
                    // causes a lot of
                    // special case handling in a lot of places.
                    att.mFileName = "";
                    att.mSize = message.getSize();
                    att.mMimeType = "text/plain";
                    att.mMessageId = localMessage.mId;
                    att.mAccountId = localMessage.mAccountId;
                    // att.mFlags = Attachment.FLAG_DUMMY_ATTACHMENT;
                    // att.save(context);
                    localMessage.mFlagAttachment = true;
                }

                // One last update of message with two updated flags
                localMessage.mSyncData1 = Integer.toString(loadStatus);

                ContentValues cv = new ContentValues();
                // cv.put(EmailContent.MessageColumns.FLAG_ATTACHMENT,
                // localMessage.mFlagAttachment);

                // putting the old MessageLoadStatus in SYNC_DATA1
                cv.put(MessageContract.Message.SYNC_DATA1, localMessage.mSyncData1);
                Uri uri = MessagingProviderUtilities.buildUri(MessageContract.Message.CONTENT_URI,
                        localMessage.mId, true);
                context.getContentResolver().update(uri, cv, null, null);

            } catch (MessagingException me) {
                LogUtils.e(Logging.LOG_TAG, "Error while copying downloaded message." + me);
            }

        } catch (RuntimeException rte) {
            LogUtils.e(Logging.LOG_TAG, "Error while storing downloaded message." + rte.toString());
        } catch (IOException ioe) {
            LogUtils.e(Logging.LOG_TAG, "Error while storing attachment." + ioe.toString());
        }
    }

    private static void insertOrUpdate(MessageBodyValue body, Context context) {

        if (body.mId > 0) {
            Uri updateUri = MessagingProviderUtilities.buildUri(
                    MessageContract.MessageBody.CONTENT_URI, body.mId, true);
            context.getContentResolver().update(updateUri, body.toContentValues(true), null, null);
        } else {
            // FOR NOW DOING THIS - SHOULD USE UTIL LIKE AS DOES
            context.getContentResolver().insert(MessageContract.MessageBody.CONTENT_URI,
                    body.toContentValues(true));
        }
    }

    public static void insertOrUpdate(MessageValue message, Context context) throws IOException {
        // @TODO BUILD BETTER getMsgOperations - which handles updates of
        // contacts and bodies
        if (!message.isNew()) {
            Uri updateUri = MessagingProviderUtilities.buildUri(
                    MessageContract.Message.CONTENT_URI, message.mId, true);
            context.getContentResolver().update(updateUri, message.toContentValues(true), null,
                    null);
            // only doing one
            if (message.getBodies().size() > 0) {
                insertOrUpdate(message.getBodies().get(0), context);
            }

        } else {
            message.mMimeType = context
                    .getString(com.blackberry.lib.emailprovider.R.string.message_mimetype);

            ArrayList<Operation> ops = new ArrayList<Operation>();
            EmailMessageUtilities.addMessageOperations(context, message, ops);
            ContentProviderResult[] results = ContentProviderBulkOpsHelper.commit(
                    context.getContentResolver(),
                    MessageContract.AUTHORITY, ops);

            if (results != null && results.length > 0) {
                // firt one will be messsage uri
                message.mId = ContentUris.parseId(results[0].uri);
            }
        }
    }

    public static int getMessageLoadState(MessageValue msg) {
        int status = -1;

        if (msg.mSyncData1 != null) {
            try {
                status = Integer.parseInt(msg.mSyncData1);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        }

        return status;
    }
}
