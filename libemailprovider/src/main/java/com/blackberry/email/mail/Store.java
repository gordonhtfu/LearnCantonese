/*
 * Copyright (C) 2008 The Android Open Source P-roject
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.blackberry.email.mail;

import java.lang.reflect.Method;
import java.util.HashMap;

import android.content.Context;
import android.os.Bundle;

import com.blackberry.email.mail.store.ServiceStore;
import com.blackberry.email.provider.contract.Account;
import com.blackberry.email.provider.contract.EmailContent;
import com.blackberry.email.provider.contract.HostAuth;
import com.blackberry.email.provider.contract.Mailbox;
import com.blackberry.email.utils.FolderUtilities;
import com.blackberry.lib.emailprovider.R;
import com.blackberry.message.service.FolderValue;
import com.blackberry.common.Logging;
import com.blackberry.common.utils.LogUtils;
import com.google.common.annotations.VisibleForTesting;

/**
 * Store is the legacy equivalent of the Account class
 */
public abstract class Store {
    /**
     * A global suggestion to Store implementors on how much of the body
     * should be returned on FetchProfile.Item.BODY_SANE requests. We'll use 125k now.
     */
    public static final int FETCH_BODY_SANE_SUGGESTED_SIZE = (125 * 1024);

    @VisibleForTesting
    static final HashMap<HostAuth, Store> sStores = new HashMap<HostAuth, Store>();
    protected Context mContext;
    protected Account mAccount;
    protected String mUsername;
    protected String mPassword;

    static final HashMap<String, Class<? extends Store>> sStoreClasses =
        new HashMap<String, Class<? extends Store>>();

    /**
     * Static named constructor.  It should be overrode by extending class.
     * Because this method will be called through reflection, it can not be protected.
     */
    static Store newInstance(Account account) throws MessagingException {
        throw new MessagingException("Store#newInstance: Unknown scheme in "
                + account.mDisplayName);
    }

    /**
     * Get an instance of a mail store for the given account. The account must be valid (i.e. has
     * at least an incoming server name).
     *
     * NOTE: The internal algorithm used to find a cached store depends upon the account's
     * HostAuth row. If this ever changes (e.g. such as the user updating the
     * host name or port), we will leak entries. This should not be typical, so, it is not
     * a critical problem. However, it is something we should consider fixing.
     *
     * @param account The account of the store.
     * @return an initialized store of the appropriate class
     * @throws MessagingException If the store cannot be obtained or if the account is invalid.
     */
    public synchronized static Store getInstance(Account account, Context context)
            throws MessagingException {
        if (sStores.isEmpty()) {
        	try{
        		
        	//JUST HACKING THIS IN UNTIL IMAP/POP SUPPORT ACTION INTENTS LIKE EAS DOES **MFL
            sStoreClasses.put(context.getString(R.string.protocol_pop3), 
            		Class.forName("com.blackberry.email.mail.store.Pop3Store").asSubclass(Store.class));
            sStoreClasses.put(context.getString(R.string.protocol_imap), 
            		Class.forName("com.blackberry.email.mail.store.ImapStore").asSubclass(Store.class));
        	}catch(ClassNotFoundException cnf){
        		 LogUtils.e(Logging.LOG_TAG, "ClassNotFoundException::" + cnf.getMessage());
        	}
        }
        HostAuth hostAuth = account.getOrCreateHostAuthRecv(context);
        // An existing account might have been deleted
        if (hostAuth == null) return null;
        Store store = sStores.get(hostAuth);
        if (store == null) {
            Context appContext = context.getApplicationContext();
            Class<? extends Store> klass = sStoreClasses.get(hostAuth.mProtocol);
            if (klass == null) {
                klass = ServiceStore.class;
            }
            try {
                // invoke "newInstance" class method
                Method m = klass.getMethod("newInstance", Account.class, Context.class);
                store = (Store)m.invoke(null, account, appContext);
            } catch (Exception e) {
                LogUtils.d(Logging.LOG_TAG, String.format(
                        "exception %s invoking method %s#newInstance(Account, Context) for %s",
                        e.toString(), klass.getName(), account.mDisplayName));
                throw new MessagingException("Can't instantiate Store for " + account.mDisplayName);
            }
            // Don't cache this unless it's we've got a saved HostAuth
            if (hostAuth.mId != EmailContent.NOT_SAVED) {
                sStores.put(hostAuth, store);
            }
        }
        return store;
    }

    /**
     * Delete the mail store associated with the given account. The account must be valid (i.e. has
     * at least an incoming server name).
     *
     * The store should have been notified already by calling delete(), and the caller should
     * also take responsibility for deleting the matching LocalStore, etc.
     *
     * @throws MessagingException If the store cannot be removed or if the account is invalid.
     */
    public synchronized static Store removeInstance(Account account, Context context)
            throws MessagingException {
        return sStores.remove(HostAuth.restoreHostAuthWithId(context, account.mHostAuthKeyRecv));
    }

    /**
     * Some protocols require that a sent message be copied (uploaded) into the Sent folder
     * while others can take care of it automatically (ideally, on the server).  This function
     * allows a given store to indicate which mode(s) it supports.
     * @return true if the store requires an upload into "sent", false if this happens automatically
     * for any sent message.
     */
    public boolean requireCopyMessageToSentFolder() {
        return true;
    }

    public Folder getFolder(String name) throws MessagingException {
        return null;
    }

    /**
     * Updates the local list of mailboxes according to what is located on the remote server.
     * <em>Note: This does not perform folder synchronization and it will not remove mailboxes
     * that are stored locally but not remotely.</em>
     * @return The set of remote folders
     * @throws MessagingException If there was a problem connecting to the remote server
     */
    public Folder[] updateFolders() throws MessagingException {
        return null;
    }

    public abstract Bundle checkSettings() throws MessagingException;

    /**
     * Handle discovery of account settings using only the user's email address and password
     * @param context the context of the caller
     * @param emailAddress the email address of the exchange user
     * @param password the password of the exchange user
     * @return a Bundle containing an error code and a HostAuth (if successful)
     * @throws MessagingException
     */
    public Bundle autoDiscover(Context context, String emailAddress, String password)
            throws MessagingException {
        return null;
    }

    /**
     * Updates the fields within the given folder. Only the fields that are important to
     * non-EAS accounts are modified.
     */
    protected static void updateFolder(FolderValue folderValue, long accountId, String folderPath,
            char delimiter, boolean selectable, int type) {
        folderValue.mAccountId = accountId;
        folderValue.mSyncData3 = delimiter + ""; 
        String displayPath = folderPath;
        int pathIndex = folderPath.lastIndexOf(delimiter);
        if (pathIndex > 0) {
            displayPath = folderPath.substring(pathIndex + 1);
        }
        folderValue.mDisplayName = displayPath;
        if (selectable) {
            folderValue.mFlags = FolderUtilities.FLAG_HOLDS_MAIL | FolderUtilities.FLAG_ACCEPTS_MOVED_MAIL;
        }
        folderValue.mRemoteId = folderPath;
        folderValue.mType = type;
    }
}
