/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.blackberry.email.utils;

import com.blackberry.common.Logging;
import com.blackberry.common.utils.LogUtils;
import com.blackberry.email.Account;
import com.blackberry.email.provider.EmailProvider;
import com.blackberry.email.provider.UIProvider;
import com.blackberry.email.service.EmailServiceUtils;
import com.blackberry.provider.MessageContract;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AccountUtils {
    /**
     * Merge two lists of accounts into one list of accounts without duplicates.
     * 
     * @param existingList List of accounts.
     * @param accounts Accounts to merge in.
     * @param prioritizeAccountList Boolean indicating whether this method
     *            should prioritize the list of Account objects when merging the
     *            lists
     * @return Merged list of accounts.
     */
    public static List<Account> mergeAccountLists(List<Account> inList, Account[] accounts,
            boolean prioritizeAccountList) {

        List<Account> newAccountList = new ArrayList<Account>();
        List<String> existingList = new ArrayList<String>();
        if (inList != null) {
            for (Account account : inList) {
                existingList.add(account.name);
            }
        }
        // Make sure the accounts are actually synchronized
        // (we won't be able to save/send for accounts that
        // have never been synchronized)
        for (int i = 0; i < accounts.length; i++) {
            final String accountName = accounts[i].name;
            // If the account is in the cached list or the caller requested
            // that we prioritize the list of Account objects, put it in the new
            // list
            if (prioritizeAccountList || existingList.contains(accountName)) {
                newAccountList.add(accounts[i]);
            }
        }
        return newAccountList;
    }

    /**
     * Synchronous method which returns registered accounts that are syncing.
     * 
     * @param context
     * @return
     */
    public static Account[] getSyncingAccounts(Context context) {
        final ContentResolver resolver = context.getContentResolver();
        Cursor accountsCursor = null;
        final List<Account> accounts = Lists.newArrayList();
        Account account;
        try {
            accountsCursor = resolver.query(EmailProvider.uiUri("uiaccts", -1),
                    UIProvider.ACCOUNTS_PROJECTION, null, null, null);
            if (accountsCursor != null) {
                while (accountsCursor.moveToNext()) {
                    account = new Account(accountsCursor);
                    if (!account.isAccountSyncRequired()) {
                        accounts.add(account);
                    }
                }
            }
        } finally {
            if (accountsCursor != null) {
                accountsCursor.close();
            }
        }
        return accounts.toArray(new Account[accounts.size()]);
    }

    /**
     * Synchronous method which returns registered accounts.
     * 
     * @param context
     * @return
     */
    public static Account[] getAccounts(Context context) {
        final ContentResolver resolver = context.getContentResolver();
        Cursor accountsCursor = null;
        final List<Account> accounts = Lists.newArrayList();
        try {

            accountsCursor = resolver.query(EmailProvider.uiUri("uiaccts", -1),
                    UIProvider.ACCOUNTS_PROJECTION, null, null, null);
            if (accountsCursor != null) {
                while (accountsCursor.moveToNext()) {
                    accounts.add(new Account(accountsCursor));
                }
            }
        } finally {
            if (accountsCursor != null) {
                accountsCursor.close();
            }
        }
        return accounts.toArray(new Account[accounts.size()]);
    }

    public static Account getAccount(Context context, long id) {
        final ContentResolver resolver = context.getContentResolver();
        Cursor accountsCursor = null;

        try {

            accountsCursor = resolver.query(EmailProvider.uiUri("uiaccts", -1),
                    UIProvider.ACCOUNTS_PROJECTION, null, null, null);
            if (accountsCursor != null) {
                while (accountsCursor.moveToNext()) {
                    return new Account(accountsCursor);
                }
            }
        } finally {
            if (accountsCursor != null) {
                accountsCursor.close();
            }
        }
        return null;
    }

    /**
     * Get a all {@link Account} objects from the {@link EmailProvider}.
     * @param context Our {@link Context}.
     * @return A list of all {@link Account}s from the {@link EmailProvider}.
     */
    private static List<com.blackberry.email.provider.contract.Account>
        getAllEmailProviderAccounts(final Context context) {
        final Cursor c = context.getContentResolver().query(
                com.blackberry.email.provider.contract.Account.CONTENT_URI,
                com.blackberry.email.provider.contract.Account.CONTENT_PROJECTION, null, null, null);
        if (c == null) {
            return Collections.emptyList();
        }

        final ImmutableList.Builder<com.blackberry.email.provider.contract.Account> builder = ImmutableList.builder();
        try {
            while (c.moveToNext()) {
                final com.blackberry.email.provider.contract.Account account = new com.blackberry.email.provider.contract.Account();
                account.restore(c);
                builder.add(account);
            }
        } finally {
            c.close();
        }
        return builder.build();
    }

    public static void syncAllEmailAccounts(final Context context) {
        try {
            List<com.blackberry.email.provider.contract.Account> accounts = getAllEmailProviderAccounts(context);
            for (com.blackberry.email.provider.contract.Account account: accounts) {
                // Validate that this account has automatic sync turned on and
                // that it is an email account
                // Kick off a sync for this account
                LogUtils.i(Logging.LOG_TAG, "Starting account %s", account.mDisplayName);
                Bundle extras = new Bundle();
                // Get the account manager type
                final EmailServiceUtils.EmailServiceInfo serviceInfo
                    = EmailServiceUtils.getServiceInfoForAccount(context, account.mId);
                ContentResolver.requestSync(account.getAccountManagerAccount(serviceInfo.accountType), MessageContract.AUTHORITY, extras);
            }
        } catch (Exception e) {
            LogUtils.e(LogUtils.TAG, "Unable to start a sync of all accounts due to exception: %s", e.getMessage());
            e.printStackTrace();
        }
    }
}
