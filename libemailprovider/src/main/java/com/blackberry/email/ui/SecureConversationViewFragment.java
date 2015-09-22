/*
 * Copyright (C) 2012 Google Inc.
 * Licensed to The Android Open Source Project.
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

package com.blackberry.email.ui;

import com.blackberry.common.content.ObjectCursor;
import com.blackberry.common.utils.LogTag;
import com.blackberry.common.utils.LogUtils;
import com.blackberry.email.Account;
import com.blackberry.email.Address;
import com.blackberry.email.Conversation;
import com.blackberry.email.ui.browse.ConversationAccountController;
import com.blackberry.email.ui.browse.ConversationMessage;
import com.blackberry.email.ui.browse.ConversationViewHeader;
import com.blackberry.email.ui.browse.MessageCursor;
import com.blackberry.email.ui.browse.MessageHeaderView;
import com.blackberry.menu.MenuBuilder;
import com.blackberry.menu.MenuItemDetails;
import com.blackberry.menu.RequestedItem;
import com.blackberry.provider.MessageContract;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

import android.app.Fragment;
import android.content.Loader;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SecureConversationViewFragment extends AbstractConversationViewFragment
        implements SecureConversationViewControllerCallbacks {
    private static final String LOG_TAG = LogTag.getLogTag();
    public static final String LAYOUT_TAG = "ConvLayout";

    private SecureConversationViewController mViewController;
    private Menu mMenu;

    private class SecureConversationWebViewClient extends AbstractConversationWebViewClient {
        public SecureConversationWebViewClient(Account account) {
            super(account);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            // Ignore unsafe calls made after a fragment is detached from an activity.
            // This method needs to, for example, get at the loader manager, which needs
            // the fragment to be added.
            if (!isAdded()) {
                LogUtils.d(LOG_TAG, "ignoring SCVF.onPageFinished, url=%s fragment=%s", url,
                        SecureConversationViewFragment.this);
                return;
            }

            if (isUserVisible()) {
                onConversationSeen();
            }

            mViewController.dismissLoadingStatus();

            final Set<String> emailAddresses = Sets.newHashSet();
            final List<Address> cacheCopy;
            synchronized (mAddressCache) {
                cacheCopy = ImmutableList.copyOf(mAddressCache.values());
            }
            for (Address addr : cacheCopy) {
                emailAddresses.add(addr.getAddress());
            }
            final ContactLoaderCallbacks callbacks = getContactInfoSource();
            callbacks.setSenders(emailAddresses);
            getLoaderManager().restartLoader(CONTACT_LOADER, Bundle.EMPTY, callbacks);
        }
    }

    /**
     * Creates a new instance of {@link ConversationViewFragment}, initialized
     * to display a conversation with other parameters inherited/copied from an
     * existing bundle, typically one created using {@link #makeBasicArgs}.
     */
    public static SecureConversationViewFragment newInstance(Bundle existingArgs,
            Conversation conversation) {
        SecureConversationViewFragment f = new SecureConversationViewFragment();
        Bundle args = new Bundle(existingArgs);
        args.putParcelable(ARG_CONVERSATION, conversation);
        f.setArguments(args);
        return f;
    }

    /**
     * Constructor needs to be public to handle orientation changes and activity
     * lifecycle events.
     */
    public SecureConversationViewFragment() {}

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        mWebViewClient = new SecureConversationWebViewClient(mAccount);
        mViewController = new SecureConversationViewController(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return mViewController.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewController.onActivityCreated(savedInstanceState);
    }

    // Start implementations of SecureConversationViewControllerCallbacks

    @Override
    public Fragment getFragment() {
        return this;
    }

    @Override
    public AbstractConversationWebViewClient getWebViewClient() {
        return mWebViewClient;
    }

    @Override
    public void setupConversationHeaderView(ConversationViewHeader headerView) {
        headerView.setCallbacks(this, this);
        headerView.setSubject(mConversation.subject);
    }

    @Override
    public boolean isViewVisibleToUser() {
        return isUserVisible();
    }

    @Override
    public ConversationAccountController getConversationAccountController() {
        return this;
    }

    @Override
    public Map<String, Address> getAddressCache() {
        return mAddressCache;
    }

    @Override
    public void setupMessageHeaderVeiledMatcher(MessageHeaderView messageHeaderView) {
        messageHeaderView.setVeiledMatcher(
                ((ControllableActivity) getActivity()).getAccountController()
                        .getVeiledAddressMatcher());
    }

    @Override
    public void startMessageLoader() {
        getLoaderManager().initLoader(MESSAGE_LOADER, null, getMessageLoaderCallbacks());
    }

    @Override
    public String getBaseUri() {
        return mBaseUri;
    }

    @Override
    public boolean isViewOnlyMode() {
        return false;
    }

    @Override
    public Uri getAccountUri() {
        return mAccount != null ? mAccount.uri: null;
    }

    // End implementations of SecureConversationViewControllerCallbacks

    @Override
    public void onAccountChanged(Account newAccount, Account oldAccount) {
        renderMessage(getMessageFromCursor(getMessageCursor()));
    }

    @Override
    public void onConversationViewHeaderHeightChange(int newHeight) {
        // Do nothing.
    }

    @Override
    public void onUserVisibleHintChanged() {
        if (mActivity == null) {
            return;
        }
        if (isUserVisible()) {
            onConversationSeen();
        }
    }

    @Override
    protected void onMessageCursorLoadFinished(Loader<ObjectCursor<ConversationMessage>> loader,
            MessageCursor newCursor, MessageCursor oldCursor) {
        ConversationMessage message = getMessageFromCursor(newCursor);
        renderMessage(message);
        updateMenuForMessage(message);
    }

    private ConversationMessage getMessageFromCursor(MessageCursor cursor) {
        // ignore cursors that are still loading results
        if (cursor == null || !cursor.isLoaded()) {
            LogUtils.i(LOG_TAG, "CONV RENDER: existing cursor is null, rendering from scratch");
            return null;
        }
        if (mActivity == null || mActivity.isFinishing()) {
            // Activity is finishing, just bail.
            return null;
        }
        if (!cursor.moveToFirst()) {
            LogUtils.e(LOG_TAG, "unable to open message cursor");
            return null;
        }
        return cursor.getMessage();
    }

    private void renderMessage(ConversationMessage message) {
        if (message == null) {
            return;
        }
        message.restoreMessageBodies(getContext());
        message.restoreMessageContacts(getContext());

        mConversation.subject = message.mSubject;
        onConversationUpdated(mConversation);

        mViewController.renderMessage(message);
    }

    private void updateMenuForMessage(ConversationMessage message) {
        if (mMenu != null && message != null) {
            mMenu.clear();
            List<MenuItemDetails> menuItems = new ArrayList<MenuItemDetails>();
            ArrayList<RequestedItem> list = new ArrayList<RequestedItem>();
            RequestedItem item = new RequestedItem(message.mEntityUri, message.mMimeType, message.mState);
            list.add(item);
            MenuBuilder menuBuilder = new MenuBuilder(getContext(), list);
            menuItems = menuBuilder.getMenuItems(getContext());
            MenuBuilder.populateMenu(getContext(),  mMenu,  menuItems);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Hold this to populate later
        mMenu = menu;
    }

    @Override
    public void onConversationUpdated(Conversation conv) {
        final ConversationViewHeader headerView = mViewController.getConversationHeaderView();
        if (headerView != null) {
            headerView.onConversationUpdated(conv);
            headerView.setSubject(conv.subject);
        }
    }

    // Need this stub here
    @Override
    public boolean supportsMessageTransforms() {
        return false;
    }

    @Override
    protected void markMessagesRead() {
        mSvc.clearMessageFlags(mViewController.getMessage().mAccountId,
                mViewController.getMessage().mEntityUri.toString(),
                MessageContract.Message.State.UNREAD);
    }
}
