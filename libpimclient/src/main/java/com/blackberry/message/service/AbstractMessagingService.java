package com.blackberry.message.service;

import android.os.RemoteException;

/**
 * Base implementation of IMessagingService for message providers. This class is
 * to be extended with actual implementation. By default, all the methods set
 * CODE_OPERATION_NOT_SUPPORTED result value
 * 
 * @author vrudenko
 * 
 */
public class AbstractMessagingService extends IMessagingService.Stub {

    @Override
    public int getApiLevel() throws RemoteException {
        return 1;
    }

    private static void abstractFail(ServiceResult result) {
        if (result != null) {
            result.setResponseCode(ServiceResult.CODE_OPERATION_NOT_SUPPORTED);
        }
    }

    @Override
    public void syncAccount(long accountId, ServiceResult result) throws RemoteException {
        abstractFail(result);
    }

    @Override
    public void setOutOfOffice(long accountId, String label, ServiceResult result) throws RemoteException {
        abstractFail(result);
    }

    @Override
    public String createFolder(long accountId, FolderValue folder, ServiceResult result) throws RemoteException {
        abstractFail(result);
        return null;
    }

    @Override
    public void renameFolder(long accountId, String folderId, String newName, ServiceResult result) throws RemoteException {
        abstractFail(result);
    }

    @Override
    public void deleteFolder(long accountId, String folderId, ServiceResult result) throws RemoteException {
        abstractFail(result);
    }

    @Override
    public void setFolderSyncEnabled(long accountId, String folderId, boolean syncEnabled, ServiceResult result) throws RemoteException {
        abstractFail(result);
    }

    @Override
    public String sendMessage(long accountId, MessageValue message, ServiceResult result) throws RemoteException {
        abstractFail(result);
        return null;
    }

    @Override
    public String saveMessage(long accountId, MessageValue message, ServiceResult result) throws RemoteException {
        abstractFail(result);
        return null;
    }

    @Override
    public String replyMessage(long accountId, String originalMessageId, MessageValue message, ServiceResult result) throws RemoteException {
        abstractFail(result);
        return null;
    }

    @Override
    public String forwardMessage(long accountId, String originalMessageId, MessageValue message, ServiceResult result) throws RemoteException {
        abstractFail(result);
        return null;
    }

    @Override
    public void fileMessage(long accountId, String messageId, String destFolderId, ServiceResult result) throws RemoteException {
        abstractFail(result);
    }

    @Override
    public void fileMessages(long accountId, String[] messageId, String destFolderId, ServiceResult result) throws RemoteException {
        abstractFail(result);
    }

    @Override
    public void downloadMessage(long accountId, String messageId, ServiceResult result) throws RemoteException {
        abstractFail(result);
    }

    @Override
    public void deleteMessage(long accountId, String messageId, ServiceResult result) throws RemoteException {
        abstractFail(result);
    }

    @Override
    public void prefetchMessage(long accountId, String messageId, ServiceResult result) throws RemoteException {
        abstractFail(result);
    }

    @Override
    public void downloadAttachment(long accountId, String attachmentId, ServiceResult result) throws RemoteException {
        abstractFail(result);
    }

    @Override
    public void bulkActionMessage(long accountId, MessageFilter filter, int action, ServiceResult result) throws RemoteException {
        abstractFail(result);
    }

    @Override
    public String startRemoteSearch(long accountId,MessageFilter filter, ServiceResult result) throws RemoteException {
        abstractFail(result);
        return null;
    }

    @Override
    public void setMessageFlags(long accountId, String messageId, long flagsMask, boolean replace, ServiceResult result) throws RemoteException {
        abstractFail(result);
    }

    @Override
    public void clearMessageFlags(long accountId, String messageId, long flagsMask, ServiceResult result) throws RemoteException {
        abstractFail(result);
    }

    @Override
    public void setMessagePriority(long accountId, String messageId, int priority, ServiceResult result) throws RemoteException {
        abstractFail(result);
    }

    @Override
    public void fetchMore(long accountId, String folderId, ServiceResult result) throws RemoteException {
        abstractFail(result);
    }

}
