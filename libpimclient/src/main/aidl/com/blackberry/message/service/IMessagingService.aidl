package com.blackberry.message.service;

import com.blackberry.message.service.MessageValue;
import com.blackberry.message.service.FolderValue;
import com.blackberry.message.service.MessageFilter;
import com.blackberry.message.service.ServiceResult;

import android.os.Bundle;

interface IMessagingService {

    int getApiLevel();

    // methods for API level 1

    // account actions
    void syncAccount(in long accountId, out ServiceResult result);
    void setOutOfOffice(in long accountId, in String label, out ServiceResult result);

    // folder actions
    String createFolder(in long accountId, in FolderValue folder, out ServiceResult result);
    void renameFolder(in long accountId, in String folderId, String newName, out ServiceResult result);
    void deleteFolder(in long accountId, in String folderId, out ServiceResult result);
    void setFolderSyncEnabled(in long accountId, in String folderId, in boolean syncEnabled, out ServiceResult result);
    void fetchMore(in long accountId, in String folderId, out ServiceResult result);

	// message actions
    String sendMessage(in long accountId, in MessageValue message, out ServiceResult result);
    String saveMessage(in long accountId, in MessageValue message, out ServiceResult result);
    String replyMessage(in long accountId, in String originalMessageId, in MessageValue message, out ServiceResult result);
    String forwardMessage(in long accountId, in String originalMessageId, in MessageValue message, out ServiceResult result);
    void fileMessage(in long accountId, in String messageId, in String destFolderId, out ServiceResult result);
    void fileMessages(in long accountId, in String[] messageId, in String destFolderId, out ServiceResult result);
    void downloadMessage(in long accountId, in String messageId, out ServiceResult result);
    void deleteMessage(in long accountId, in String messageId, out ServiceResult result);
    void prefetchMessage(in long accountId, in String messageId, out ServiceResult result);
    void setMessageFlags(in long accountId, in String messageId, in long flagsMask, in boolean replace, out ServiceResult result);
    void clearMessageFlags(in long accountId, in String messageId, in long flagsMask, out ServiceResult result);
    void setMessagePriority(in long accountId, in String messageId, in int priority, out ServiceResult result);

    // attachment actions
    void downloadAttachment(in long accountId, in String attachmentId, out ServiceResult result);

	// auxiliary actions
    void bulkActionMessage(in long accountId, in MessageFilter filter, int action, out ServiceResult result);
    String startRemoteSearch(in long accountId, in MessageFilter filter, out ServiceResult result);
}
