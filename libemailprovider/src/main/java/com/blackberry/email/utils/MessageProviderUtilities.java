
package com.blackberry.email.utils;

import com.blackberry.common.utils.LogUtils;
import com.blackberry.email.mail.Address;
import com.blackberry.email.provider.EmailProvider;
import com.blackberry.email.provider.UIProvider;
import com.blackberry.email.provider.contract.EmailContent;
import com.blackberry.email.provider.contract.EmailContent.Message;
import com.blackberry.email.provider.contract.EmailContent.MessageColumns;
import com.blackberry.email.provider.contract.Mailbox;
import com.blackberry.provider.MessageContract;

import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentProviderOperation.Builder;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;

/**
 * DomainMsgProviderUtilities This class will map the data from EmailProvider
 * into the DomainMessageProvider projections and then executes the specific
 * provider CRUD call
 */
public class MessageProviderUtilities {
    private static String TAG = "DomainInt";

    public static final String MESSAGE_ID_TYPE = "uimessage";
    public static final String FOLDER_ID_TYPE = "uifolder";
    private static final String WHERE_MSG_ID = MessageContract.Message.ENTITY_URI + "=?";

    public static String buildFolderId(long mailboxId) {
        return EmailProvider.uiUriString(FOLDER_ID_TYPE, mailboxId);
    }

    /**
     * insertMessage converts and EmailProvider Msg obj and inserts it into the
     * DomainMessageProvider
     * 
     * @param context
     * @param emailMsgUri - EmailProvider's Msg Uri
     * @param emailMessageValues EmailProviders Msg values
     * @param mimeType EmailProviders Msg Mimetype
     * @return valid Uri with appended id if success, null if not
     */
    public static Uri insertMessage(Context context, Uri emailMsgUri,
            ContentValues emailMessageValues, String mimeType) {
        // convert
        ContentValues domainMsgValues = new ContentValues();

        // account id
        Long lAccountId = emailMessageValues
                .getAsLong(EmailContent.MessageColumns.ACCOUNT_KEY);
        if (lAccountId != null) {
            domainMsgValues.put(MessageContract.Message.ACCOUNT_ID,
                    lAccountId.longValue());
        }
        // mimetype msg id - note using the uimessage for now - in future may
        // change
        String msgId = EmailProvider.uiUriString(MESSAGE_ID_TYPE,
                Long.parseLong(emailMsgUri.getPathSegments().get(1)));
        domainMsgValues.put(MessageContract.Message.ENTITY_URI, msgId);

        // add the mailbox id - in uri form
        Long lMailboxId = emailMessageValues.getAsLong(EmailContent.Message.MAILBOX_KEY);
        if (lMailboxId != null) {
            domainMsgValues.put(
                    MessageContract.Message.FOLDER_ID,
                    getFolderId(EmailProvider.uiUriString(FOLDER_ID_TYPE, lMailboxId.longValue()),
                            context));
        }
        // subject
        domainMsgValues.put(MessageContract.Message.SUBJECT,
                emailMessageValues.getAsString(EmailContent.Message.SUBJECT));
        // set mimetype
        domainMsgValues.put(MessageContract.Message.MIME_TYPE,
                mimeType);
        // sender
        domainMsgValues.put(MessageContract.Message.SENDER,
                emailMessageValues.getAsString(EmailContent.Message.DISPLAY_NAME));
        // timestamp
        Long lTimeStamp = emailMessageValues.getAsLong(EmailContent.Message.TIMESTAMP);
        if (lTimeStamp != null) {
            domainMsgValues.put(MessageContract.Message.TIMESTAMP,
                    lTimeStamp.longValue());
        }

        // @TODO need to figure this out - may occur on an update
        // domainMsgValues.put(MessageContract.Message.ATTACHMENT_COUNT,
        // emailMessageValues.getAsString(EmailContent.Message.));

        // @TODO figure out all the State mappings
        Boolean bRead = emailMessageValues.getAsBoolean(EmailContent.Message.FLAG_READ);
        long state = MessageContract.Message.State.NO_STATE;
        if (bRead != null) {
            state |= bRead.booleanValue() == false ? MessageContract.Message.State.UNREAD :
                MessageContract.Message.State.READ;

            domainMsgValues.put(MessageContract.Message.STATE, state);
        }

        Integer flagValue = emailMessageValues
                .getAsInteger(MessageColumns.FLAGS);
        int mFlags = (flagValue != null) ? flagValue.intValue() : 0;

        if ((mFlags & Message.FLAG_TYPE_DRAFT) != 0) {
            domainMsgValues.put(MessageContract.Message.STATE,
                    (state | MessageContract.Message.State.DRAFT));
        }

        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        // create insert msg op
        Builder insertMessageOp = ContentProviderOperation
                .newInsert(MessageContract.Message.CONTENT_URI);
        insertMessageOp.withValues(domainMsgValues);
        ops.add(insertMessageOp.build());
        // contacts insert ops
        processAddressList(emailMessageValues.getAsString(EmailContent.Message.TO_LIST),
                MessageContract.MessageContact.FieldType.TO, ops);
        processAddressList(emailMessageValues.getAsString(EmailContent.Message.CC_LIST),
                MessageContract.MessageContact.FieldType.CC, ops);
        processAddressList(emailMessageValues.getAsString(EmailContent.Message.BCC_LIST),
                MessageContract.MessageContact.FieldType.BCC, ops);
        processAddressList(emailMessageValues.getAsString(EmailContent.Message.FROM_LIST),
                MessageContract.MessageContact.FieldType.FROM, ops);
        processAddressList(emailMessageValues.getAsString(EmailContent.Message.REPLY_TO_LIST),
                MessageContract.MessageContact.FieldType.REPLY_TO, ops);

        ContentProviderResult[] results = applyBatchIntoDomainMsgProvider(context, ops);

        // will return first operation uri, which will be the messages uri in
        // domain provider
        Uri returnUri = null;
        if (results != null && results.length > 0) {
            returnUri = results[0].uri;
        }

        return returnUri;
    }

    private static String WHERE_ENTITY_IS = MessageContract.Folder.ENTITY_URI + "= ?";

    private static long getFolderId(String folderEntityUri, Context context) {
        Cursor cursor = context.getContentResolver().query(MessageContract.Folder.CONTENT_URI,
                new String[] {
                    MessageContract.Folder._ID
                }, WHERE_ENTITY_IS, new String[] {
                    folderEntityUri
                }, null);

        long id = -1;
        if (cursor != null && cursor.moveToFirst()) {
            id = cursor.getLong(0);
            cursor.close();
        }

        return id;
    }

    private static long getMessageId(String msgEntityUri, Context context) {
        Cursor cursor = context.getContentResolver().query(MessageContract.Message.CONTENT_URI,
                new String[] {
                    MessageContract.Message._ID
                }, WHERE_ENTITY_IS, new String[] {
                    msgEntityUri
                }, null);

        long id = -1;
        if (cursor != null && cursor.moveToFirst()) {
            id = cursor.getLong(0);
            cursor.close();
        }

        return id;
    }

    private static void processAddressList(String addressList, int fieldType,
            ArrayList<ContentProviderOperation> ops) {
        if (addressList != null && addressList.length() > 0) {
            Address[] addresses = Address.unpack(addressList);
            for (int x = 0; x < addresses.length; x++) {
                // note that Domain requires a non-null name so
                String name = addresses[x].getPersonal();
                ops.add(createInsertMessageContactOperation(name != null ? name : "",
                        addresses[x].getAddress(),
                        fieldType));
            }
        }
    }

    private static ContentProviderOperation createInsertMessageContactOperation(String name,
            String address, int fieldType) {
        Builder cpo = ContentProviderOperation
                .newInsert(MessageContract.MessageContact.CONTENT_URI);

        ContentValues values = new ContentValues();
        values.put(MessageContract.MessageContact.NAME, name);
        values.put(MessageContract.MessageContact.ADDRESS, address);
        values.put(MessageContract.MessageContact.ADDRESS_TYPE,
                MessageContract.MessageContact.AddrType.EMAIL);
        values.put(MessageContract.MessageContact.FIELD_TYPE, fieldType);
        cpo.withValues(values);
        cpo.withValueBackReference(MessageContract.MessageContact.MESSAGE_ID, 0);
        return cpo.build();
    }

    public static Uri insertUpdateFolder(Context context, Uri mailBoxUri,
            ContentValues emailFolderValues, boolean insert) {
        // convert
        ContentValues domainFolderValues = new ContentValues();
        // name of folder
        String folderName = emailFolderValues.getAsString(EmailContent.MailboxColumns.DISPLAY_NAME);
        if (folderName != null) {
            domainFolderValues.put(MessageContract.Folder.NAME, folderName);
        }
        // description
        domainFolderValues.put(MessageContract.Folder.DESCRIPTION, "");
        // folder id for the mimetype
        String folderId = buildFolderId(Long.parseLong(mailBoxUri.getPathSegments().get(1)));
        domainFolderValues.put(MessageContract.Folder.ENTITY_URI, folderId);
        // account id
        Long lAccountId = emailFolderValues
                .getAsLong(EmailContent.MailboxColumns.ACCOUNT_KEY);
        if (lAccountId != null) {
            domainFolderValues.put(MessageContract.Folder.ACCOUNT_ID,
                    lAccountId.longValue());
        }
        // set folder type
        Integer folderType = emailFolderValues.getAsInteger(EmailContent.MailboxColumns.TYPE);

        if (folderType != null) {
            domainFolderValues.put(MessageContract.Folder.TYPE,
                mapMailboxTypeToDomainFolderType(
                    folderType));
        }
        // set folder state
        int state = 0;
        Integer uiSyncStatus = emailFolderValues
                .getAsInteger(EmailContent.MailboxColumns.UI_SYNC_STATUS);
        if (uiSyncStatus != null && uiSyncStatus == UIProvider.SyncStatus.LIVE_QUERY) {
            state |= MessageContract.Folder.State.SYNCING;
        }
        domainFolderValues.put(MessageContract.Folder.SYNC_STATE, state);

        if(insert) {
            return insertIntoDomainMsgProvider(context,
                    MessageContract.Folder.CONTENT_URI, domainFolderValues);
        } else {
            updateFolderInDomainMsgProvider(context, MessageContract.Folder.CONTENT_URI,
                    folderId, domainFolderValues);
            return mailBoxUri;
        }
    }

    public static Uri insertBody(Context context, Uri uri, ContentValues emailBodyValues) {
        ContentValues domainBodyValues = new ContentValues();

        Long lMsgId = emailBodyValues.getAsLong(EmailContent.Body.MESSAGE_KEY);
        if (lMsgId != null) {
            // may need to build uri type id
            domainBodyValues.put(
                    MessageContract.MessageBody.MESSAGE_ID,
                    getMessageId(EmailProvider.uiUriString(MESSAGE_ID_TYPE, lMsgId.longValue()),context));
        }

        String bodyData = emailBodyValues.getAsString(EmailContent.Body.TEXT_CONTENT);
        int type = -1;
        if (bodyData != null) {
            type = MessageContract.MessageBody.Type.TEXT;
        } else {
            bodyData = emailBodyValues.getAsString(EmailContent.Body.HTML_CONTENT);
            type = MessageContract.MessageBody.Type.HTML;
        }

        if (bodyData != null) {
            domainBodyValues.put(MessageContract.MessageBody.DATA, bodyData);
            domainBodyValues.put(MessageContract.MessageBody.TYPE, type);
            domainBodyValues.put(MessageContract.MessageBody.STATE,
                    MessageContract.MessageBody.State.DOWNLOADED);
        }

        return insertIntoDomainMsgProvider(context,
                MessageContract.MessageBody.CONTENT_URI, domainBodyValues);
    }

    public static Uri insertAttachment(Context context, Uri uri, ContentValues attachmentValues) {
        // convert
        ContentValues domainMsgAttachmentValues = new ContentValues();

        // account id
        Long lAccountId = attachmentValues.getAsLong(EmailContent.AttachmentColumns.ACCOUNT_KEY);
        if (lAccountId != null) {
            domainMsgAttachmentValues.put(MessageContract.MessageAttachment.ACCOUNT_ID,
                    lAccountId.longValue());
        }

        Long lMsgId = attachmentValues.getAsLong(EmailContent.AttachmentColumns.MESSAGE_KEY);
        if (lMsgId != null) {
            // may need to build uri type id
            domainMsgAttachmentValues.put(
                    MessageContract.Message.ENTITY_URI,
                    EmailProvider.uiUriString(MESSAGE_ID_TYPE, lMsgId.longValue()));
        }
        // attachment name
        domainMsgAttachmentValues.put(MessageContract.MessageAttachment.NAME,
                attachmentValues.getAsString(EmailContent.AttachmentColumns.FILENAME));
        // add size of attachment
        Long lSize = attachmentValues.getAsLong(EmailContent.AttachmentColumns.SIZE);
        if (lSize != null) {
            domainMsgAttachmentValues.put(MessageContract.MessageAttachment.SIZE,
                    lSize.longValue());
        }
        // attachment mime-type
        domainMsgAttachmentValues.put(MessageContract.MessageAttachment.MIME_TYPE,
                attachmentValues.getAsString(EmailContent.AttachmentColumns.MIME_TYPE));

        domainMsgAttachmentValues.put(MessageContract.MessageAttachment.MIME_TYPE,
                attachmentValues.getAsString(EmailContent.AttachmentColumns.MIME_TYPE));

        domainMsgAttachmentValues.put(MessageContract.MessageAttachment.URI,
                attachmentValues.getAsString(EmailContent.AttachmentColumns.CONTENT_URI));

        // @TODO finish state mappings
        // domainMsgValues.put(MessageContract.MessageAttachment.STATE,
        // attachmentValues.getAsString(EmailContent.AttachmentColumns.STATE));

        return insertIntoDomainMsgProvider(context,
                MessageContract.MessageAttachment.CONTENT_URI, domainMsgAttachmentValues);
    }

    /**
     * Deletes a Single Msg from the DomainMessageProvider
     * 
     * @param context
     * @param msgId
     * @return
     */
    public static int deleteMessage(Context context, String msgId) {
        String[] selectionArgs = {
                EmailProvider.uiUriString(MESSAGE_ID_TYPE, Long.parseLong(msgId))
        };
        return deleteFromDomainMsgProvider(context, MessageContract.Message.CONTENT_URI,
                WHERE_MSG_ID, selectionArgs);
    }

    /**
     * updateMessage will converts EmailProviders Msg updates to the
     * MessageDomainProvider.
     * 
     * @param context
     * @param emailMsgUri - EmailProvider's Msg Uri that has been updated
     * @param emailMsgValues - EmailProviders Msg updated values - only updated
     *            values will be included
     * @param selection
     * @param selectionArgs
     * @return
     */
    public static int updateMessage(Context context, String msgId, ContentValues emailMsgValues) {
        ContentValues domainMsgValues = new ContentValues();
        // @TODO figure out the state mappings, EmailProvider is very limited
        // may need to add more to align
        // with DomainMessageProviders Message State
        // for now read state is know
        Boolean bRead = emailMsgValues.getAsBoolean(EmailContent.Message.FLAG_READ);
        if (bRead != null) {
            long state = bRead.booleanValue() == false ? MessageContract.Message.State.UNREAD :
                MessageContract.Message.State.READ;

            domainMsgValues.put(MessageContract.Message.STATE, state);
        }

        // see if folder changed
        Long lMailboxId = emailMsgValues.getAsLong(EmailContent.Message.MAILBOX_KEY);
        if (lMailboxId != null) {
            domainMsgValues.put(MessageContract.Message.FOLDER_ID,
                    getFolderId(EmailProvider.uiUriString(FOLDER_ID_TYPE, lMailboxId.longValue()),context));
        }

        String[] selectionArgs = {
                EmailProvider.uiUriString(MESSAGE_ID_TYPE, Long.parseLong(msgId))
        };
        // @TODO update other data that changed
        if (domainMsgValues.size() != 0) {
            return updateIntoDomainMsgProvider(context, MessageContract.Message.CONTENT_URI,
                    domainMsgValues, WHERE_MSG_ID, selectionArgs);
        } else {
            Log.d(TAG, "No mapped content to update, return 0");
            return 0;
        }
    }

    /**
     * mapMailboxTypeToDomainFolderType
     * 
     * @param mailboxType
     * @return
     */
    private static int mapMailboxTypeToDomainFolderType(Integer mailboxType) {

        int type = -1;
        if (mailboxType == null) {
            return MessageContract.Folder.Type.OTHER_PROVIDER_FOLDER;
        }

        switch (mailboxType.intValue()) {
            case Mailbox.TYPE_INBOX: {
                type = MessageContract.Folder.Type.INBOX;
                break;
            }
            case Mailbox.TYPE_DRAFTS: {
                type = MessageContract.Folder.Type.DRAFT;
                break;
            }
            case Mailbox.TYPE_OUTBOX: {
                type = MessageContract.Folder.Type.OUTBOX;
                break;
            }
            case Mailbox.TYPE_SENT: {
                type = MessageContract.Folder.Type.SENT;
                break;
            }
            case Mailbox.TYPE_TRASH: {
                type = MessageContract.Folder.Type.TRASH;
                break;
            }
            case Mailbox.TYPE_STARRED: {
                type = MessageContract.Folder.Type.STARRED;
                break;
            }
            default: {
                type = MessageContract.Folder.Type.OTHER_PROVIDER_FOLDER;
            }
        }

        return type;
    }

    /**
     * insertIntoDomainMsgProvider
     * 
     * @param context
     * @param uri
     * @param values
     * @return
     * @throws RemoteException
     */
    private static Uri insertIntoDomainMsgProvider(Context context, Uri uri, ContentValues values) {
        Uri returnUri = null;
        ContentResolver resolver = context.getContentResolver();
        if (resolver != null) {
            ContentProviderClient cpClient = resolver.acquireContentProviderClient(uri);

            if (cpClient != null) {
                try {
                    returnUri = cpClient.insert(uri, values);
                } catch (RemoteException re) {
                    Log.e(TAG, re.getMessage());
                }
                cpClient.release();
            }
        } else {
            LogUtils.e(TAG, "No CP found for uri");
        }

        return returnUri;
    }

    private static int updateIntoDomainMsgProvider(Context context, Uri uri, ContentValues values,
            String selection,
            String[] selectionArgs) {
        int result = 0;
        ContentResolver resolver = context.getContentResolver();
        if (resolver != null) {
            ContentProviderClient cpClient = resolver.acquireContentProviderClient(uri);

            if (cpClient != null) {
                try {
                    result = cpClient.update(uri, values, selection, selectionArgs);
                } catch (RemoteException re) {
                    Log.e(TAG, re.getMessage());
                }
                cpClient.release();
            }
        } else {
            LogUtils.e(TAG, "No CP found for uri");
        }

        return result;
    }

    private static int deleteFromDomainMsgProvider(Context context, Uri uri, String selection,
            String[] selectionArgs) {
        int result = 0;
        ContentResolver resolver = context.getContentResolver();
        if (resolver != null) {
            ContentProviderClient cpClient = resolver.acquireContentProviderClient(uri);

            if (cpClient != null) {
                try {
                    result = cpClient.delete(uri, selection, selectionArgs);
                } catch (RemoteException re) {
                    Log.e(TAG, re.getMessage());
                }
                cpClient.release();
            }
        } else {
            LogUtils.e(TAG, "No CP found for uri");
        }

        return result;
    }

    private static void updateFolderInDomainMsgProvider(Context context, Uri uri, String folderId,
            ContentValues values) {
        ContentResolver resolver = context.getContentResolver();
        if (resolver != null) {
            ContentProviderClient cpClient = resolver.acquireContentProviderClient(uri);

            if (cpClient != null) {
                try {
                    cpClient.update(uri, values, MessageContract.Folder.ENTITY_URI + "=?",
                            new String[] {
                                folderId.toString()
                            });
                } catch (RemoteException re) {
                    Log.e(TAG, re.getMessage());
                }
                cpClient.release();
            }
        } else {
            LogUtils.e(TAG, "No CP found for uri");
        }
    }

    private static ContentProviderResult[] applyBatchIntoDomainMsgProvider(Context context,
            ArrayList<ContentProviderOperation> ops) {
        ContentProviderResult[] results = null;
        ContentResolver resolver = context.getContentResolver();
        if (resolver != null) {
            try {
                results = resolver.applyBatch(MessageContract.AUTHORITY, ops);
            } catch (OperationApplicationException e) {
                LogUtils.e(TAG, e.getMessage());

            } catch (RemoteException rec) {
                LogUtils.e(TAG, rec.getMessage());
            } catch (IllegalArgumentException iae) {
                LogUtils.e(TAG, iae.getMessage());
            }
        } else {
            LogUtils.e(TAG, "No CP found for uri");
        }

        return results;
    }
}
