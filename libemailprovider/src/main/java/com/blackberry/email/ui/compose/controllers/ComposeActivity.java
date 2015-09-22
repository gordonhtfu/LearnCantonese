/**
 * Copyright (c) 2011, Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.blackberry.email.ui.compose.controllers;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.util.Rfc822Token;
import android.text.util.Rfc822Tokenizer;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.widget.EditText;
import android.widget.Toast;

import com.android.ex.chips.RecipientEditTextView;
import com.blackberry.common.analytics.Analytics;
import com.blackberry.common.utils.LogTag;
import com.blackberry.common.utils.LogUtils;
import com.blackberry.datagraph.provider.DataGraphContract;
import com.blackberry.email.Account;
import com.blackberry.email.Address;
import com.blackberry.email.Attachment;
import com.blackberry.email.Folder;
import com.blackberry.email.Message;
import com.blackberry.email.ReplyFromAccount;
import com.blackberry.email.provider.UIProvider;
import com.blackberry.email.ui.AttachmentTile.AttachmentPreview;
import com.blackberry.email.ui.WaitFragment;
import com.blackberry.email.ui.compose.views.AttachmentsView;
import com.blackberry.email.ui.compose.views.ComposeScreen;
import com.blackberry.email.ui.compose.views.AttachmentsView.AttachmentFailureException;
import com.blackberry.email.ui.compose.views.SendConfirmDialogFragment;
import com.blackberry.email.utils.AttachmentUtilities;
import com.blackberry.email.utils.UtilsEx;
import com.blackberry.intent.PimIntent;
import com.blackberry.lib.emailprovider.R;
import com.blackberry.message.service.MessageValue;
import com.blackberry.provider.ListItemContract;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;


public class ComposeActivity extends Activity
{
    private static final String KEY_INNER_SAVED_STATE = "compose_state";
    static {
        LogTag.setLogTag("ComposeActivity");
    }

    // Identifiers for which type of composition this is
    public static final int COMPOSE = -1;
    public static final int REPLY = 0;
    public static final int REPLY_ALL = 1;
    public static final int FORWARD = 2;
    public static final int EDIT_DRAFT = 3;
    public static final int SHARE = 4;

    public static final String MESSAGE_IDENTIFIER = DataGraphContract.EntityColumns.URI;

    // Integer extra holding one of the above compose action
    protected static final String EXTRA_ACTION = "action";

    private static final String EXTRA_SHOW_CC = "showCc";
    private static final String EXTRA_SHOW_BCC = "showBcc";
    private static final String EXTRA_RESPONDED_INLINE = "respondedInline";
    private static final String EXTRA_SAVE_ENABLED = "saveEnabled";

    private static final String UTF8_ENCODING_NAME = "UTF-8";

    private static final String MAIL_TO = "mailto";

    private static final String EXTRA_SUBJECT = "subject";

    private static final String EXTRA_BODY = "body";

//    private static SendConfirmDialogFragment mSendConfirmDialogFragment = new SendConfirmDialogFragment();
    /**
     * Expected to be html formatted text.
     */
    private static final String EXTRA_QUOTED_TEXT = "quotedText";

    protected static final String EXTRA_FROM_ACCOUNT_STRING = "fromAccountString";

    private static final String EXTRA_ATTACHMENT_PREVIEWS = "attachmentPreviews";

    // Extra that we can get passed from other activities
    @VisibleForTesting
    protected static final String EXTRA_TO = "to";
    private static final String EXTRA_CC = "cc";
    private static final String EXTRA_BCC = "bcc";

    /**
     * An optional extra containing a {@link ContentValues} of values to be added to
     * {@link SendOrSaveMessage#mValues}.
     */
    public static final String EXTRA_VALUES = "extra-values";

    // List of all the fields
    static final String[] ALL_EXTRAS = { EXTRA_SUBJECT, EXTRA_BODY, EXTRA_TO, EXTRA_CC, EXTRA_BCC,
        EXTRA_QUOTED_TEXT };

    /**
     * Notifies the {@code Activity} that the caller is an Email
     * {@code Activity}, so that the back behavior may be modified accordingly.
     *
     * @see #onAppUpPressed
     */
    public static final String EXTRA_FROM_EMAIL_TASK = "fromemail";

    public static final String EXTRA_ATTACHMENTS = "attachments";

    /** If set, we will clear notifications for this folder. */
    public static final String EXTRA_NOTIFICATION_FOLDER = "extra-notification-folder";

    //  If this is a reply/forward then this extra will hold the original message
    private static final String EXTRA_IN_REFERENCE_TO_MESSAGE = "in-reference-to-message";
    // If this is a reply/forward then this extra will hold a uri we must query
    // to get the original message.
    protected static final String EXTRA_IN_REFERENCE_TO_MESSAGE_URI = "in-reference-to-message-uri";
    // If this is an action to edit an existing draft message, this extra will hold the
    // draft message
    private static final String ORIGINAL_DRAFT_MESSAGE = "original-draft-message";
    private static final String END_TOKEN = ", ";
    private static final String LOG_TAG = LogTag.getLogTag();
    // TODO(mindyp) set mime-type for auto send?
    public static final String AUTO_SEND_ACTION = "com.blackberry.mail.action.AUTO_SEND";

    private static final String EXTRA_SELECTED_REPLY_FROM_ACCOUNT = "replyFromAccount";
    private static final String EXTRA_REQUEST_ID = "requestId";
    private static final String EXTRA_FOCUS_SELECTION_START = "focusSelectionStart";
    private static final String EXTRA_FOCUS_SELECTION_END = "focusSelectionEnd";
    private static final String EXTRA_MESSAGE = "extraMessage";
    private static final String EXTRA_SELECTED_ACCOUNT = "selectedAccount";
    private static final String TAG_WAIT = "wait-fragment";
    private static final String LOCAL_ACCOUNT_ID = "local_acct_id";
    private static final long NOT_FOUND = -1;

    // TODO: Move to ComposeScreen
    private AttachmentsView mAttachmentsView;
    protected Account mAccount;
    protected ReplyFromAccount mReplyFromAccount;

    protected int mComposeMode = -1;
    private long mDraftId = UIProvider.INVALID_MESSAGE_ID;
    private Object mDraftLock = new Object();

    private Uri mRefMessageUri;
    protected Bundle mInnerSavedState;
    private ContentValues mExtraValues = null;

    private int mRequestId;
    private Account[] mAccounts;
    private boolean mPerformedSendOrDiscard = false;

    private MenuItem mSave;
    private MenuItem mSend;
    private ComposeScreenController mController;

    // Can be called from a non-UI thread.
    public static void editDraft(Context launcher, Account account, Message message) {
        launch(launcher, account, message, EDIT_DRAFT, null, null, null, null,
                null /* extraValues */);
    }

    // Can be called from a non-UI thread.
    public static void compose(Context launcher, Account account) {
        launch(launcher, account, null, COMPOSE, null, null, null, null, null /* extraValues */);
    }

    // Can be called from a non-UI thread.
    public static void composeToAddress(Context launcher, Account account, String toAddress) {
        launch(launcher, account, null, COMPOSE, toAddress, null, null, null,
                null /* extraValues */);
    }

    // Can be called from a non-UI thread.
    public static void composeWithQuotedText(Context launcher, Account account,
            String quotedText, String subject, final ContentValues extraValues) {
        launch(launcher, account, null, COMPOSE, null, null, quotedText, subject, extraValues);
    }

    // Can be called from a non-UI thread.
    public static void composeWithExtraValues(Context launcher, Account account,
            String subject, final ContentValues extraValues) {
        launch(launcher, account, null, COMPOSE, null, null, null, subject, extraValues);
    }

    // Can be called from a non-UI thread.
    public static Intent createReplyIntent(final Context launcher, final Account account,
            final Uri messageUri, final boolean isReplyAll) {
        return createActionIntent(launcher, account, messageUri, isReplyAll ? REPLY_ALL : REPLY);
    }

    // Can be called from a non-UI thread.
    public static Intent createForwardIntent(final Context launcher, final Account account,
            final Uri messageUri) {
        return createActionIntent(launcher, account, messageUri, FORWARD);
    }

    private static Intent createActionIntent(final Context launcher, final Account account,
            final Uri messageUri, final int action) {
        final Intent intent = new Intent(launcher, ComposeActivity.class);

        updateActionIntent(account, messageUri, action, intent);

        return intent;
    }

    @VisibleForTesting
    static Intent updateActionIntent(Account account, Uri messageUri, int action, Intent intent) {
        intent.putExtra(EXTRA_FROM_EMAIL_TASK, true);
        intent.putExtra(EXTRA_ACTION, action);
        intent.putExtra(UtilsEx.EXTRA_ACCOUNT, account);
        intent.putExtra(EXTRA_IN_REFERENCE_TO_MESSAGE_URI, messageUri);

        return intent;
    }

    /**
     * Can be called from a non-UI thread.
     */
    public static void reply(Context launcher, Account account, Message message) {
        launch(launcher, account, message, REPLY, null, null, null, null, null /* extraValues */);
    }

    /**
     * Can be called from a non-UI thread.
     */
    public static void replyAll(Context launcher, Account account, Message message) {
        launch(launcher, account, message, REPLY_ALL, null, null, null, null,
                null /* extraValues */);
    }

    /**
     * Can be called from a non-UI thread.
     */
    public static void forward(Context launcher, Account account, Message message) {
        launch(launcher, account, message, FORWARD, null, null, null, null, null /* extraValues */);
    }

    public static void reportRenderingFeedback(Context launcher, Account account, Message message,
            String body) {
//        launch(launcher, account, message, FORWARD,
//                "android-gmail-readability@google.com", body, null, null, null /* extraValues */);
    }

    // Package a full blown context (launcher, message, account objects, other details) 
    // in an intent and launch ourselves.
    private static void launch(Context launcher, Account account, Message message, int action,
            String toAddress, String body, String quotedText, String subject,
            final ContentValues extraValues) {
        Intent intent = new Intent(launcher, ComposeActivity.class);
        intent.putExtra(EXTRA_FROM_EMAIL_TASK, true);
        intent.putExtra(EXTRA_ACTION, action);
        intent.putExtra(UtilsEx.EXTRA_ACCOUNT, account);
        if (action == EDIT_DRAFT) {
            intent.putExtra(ORIGINAL_DRAFT_MESSAGE, message);
        } else {
            intent.putExtra(EXTRA_IN_REFERENCE_TO_MESSAGE, message);
        }
        if (toAddress != null) {
            intent.putExtra(EXTRA_TO, toAddress);
        }
        if (body != null) {
            intent.putExtra(EXTRA_BODY, body);
        }
        if (quotedText != null) {
            intent.putExtra(EXTRA_QUOTED_TEXT, quotedText);
        }
        if (subject != null) {
            intent.putExtra(EXTRA_SUBJECT, subject);
        }
        if (extraValues != null) {
            LogUtils.d(LOG_TAG, "Launching with extraValues: %s", extraValues.toString());
            intent.putExtra(EXTRA_VALUES, extraValues);
        }
        launcher.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mController == null) {
            mController = new ComposeScreenController(this, savedInstanceState);
        }
        mInnerSavedState = (savedInstanceState != null) ? savedInstanceState.getBundle(KEY_INNER_SAVED_STATE) : null;
        initializeFromBundleORIntent();
    }

    // TODO: Parse the saved bundle as well!
    private void initializeFromBundleORIntent() {
        Intent intent = getIntent();

        if (containsValidResponseAction()) {
            Uri messageUri = Uri.EMPTY;
            Bundle bundle = intent.getExtras();
            if (intent.getData() != null) {
                messageUri = intent.getData();
            } else if (bundle.containsKey(MESSAGE_IDENTIFIER)) {
               messageUri = Uri.parse(bundle.getString(MESSAGE_IDENTIFIER));
            }

            if (!messageUri.equals(Uri.EMPTY)) {
                // Reply/ReplyAll/Forward/Draft screens
                mController.createScreen(messageUri, getResponseTypeFromAction(intent.getAction()));
                return;
            }
        } else {
            // TODO: Combine this block with unparcel case.
            long localAccountID = NOT_FOUND;
            localAccountID = intent.getLongExtra(LOCAL_ACCOUNT_ID, NOT_FOUND);
            if (localAccountID != NOT_FOUND) {
                mController.createScreen(localAccountID);
            } else {
                mController.createScreen(intent);
            }
        }

        // Build from a parceled message object.
        int responseType = intent.getIntExtra(EXTRA_ACTION, COMPOSE);
        if (responseType != COMPOSE) {
            MessageValue message = (MessageValue) intent
                    .getParcelableExtra(EXTRA_IN_REFERENCE_TO_MESSAGE);
            if (message != null) {
                mController.createScreen(message, responseType);
            }
        }
    }

    // Only after we've determined that we're working with a response screen
    private int getResponseTypeFromAction(String pimAction) {
        int responseType = COMPOSE;
        if (pimAction != null) {
            if (pimAction.equals(PimIntent.PIM_MESSAGE_ACTION_REPLY))
                responseType = REPLY;
            if (pimAction.equals(PimIntent.PIM_MESSAGE_ACTION_REPLY_ALL))
                responseType = REPLY_ALL;
            if (pimAction.equals(PimIntent.PIM_MESSAGE_ACTION_FORWARD))
                responseType = FORWARD;
            if (pimAction.equals("com.blackberry.email.COMPOSE"))
                responseType = EDIT_DRAFT;
        }
        return responseType;
    }

    private boolean containsValidResponseAction() {
        Intent intent = getIntent();
        // Assumption: this valid URI is a email msg URI
        // do we need to make sure this is a email URI?
        return (intent.getData() != null);
    }

    private void finishCreate() {
        final Bundle savedState = mInnerSavedState;
        // findViews();
        Intent intent = getIntent();
        Message message;
        ArrayList<AttachmentPreview> previews;
        int action;
        // Check for any of the possibly supplied accounts.;
        Account account = null;
            if (hadSavedInstanceStateMessage(savedState)) {
                action = savedState.getInt(EXTRA_ACTION, COMPOSE);
                account = savedState.getParcelable(UtilsEx.EXTRA_ACCOUNT);
                message = (Message)savedState.getParcelable(EXTRA_MESSAGE);

                previews = savedState.getParcelableArrayList(EXTRA_ATTACHMENT_PREVIEWS);
                // mRefMessage = (Message)savedState.getParcelable(EXTRA_IN_REFERENCE_TO_MESSAGE);
                // quotedText = savedState.getCharSequence(EXTRA_QUOTED_TEXT);

                mExtraValues = savedState.getParcelable(EXTRA_VALUES);
            } else {
                account = obtainAccount(intent);
                action = intent.getIntExtra(EXTRA_ACTION, COMPOSE);
                // Initialize the message from the message in the intent
                message = (Message)intent.getParcelableExtra(ORIGINAL_DRAFT_MESSAGE);
                previews = intent.getParcelableArrayListExtra(EXTRA_ATTACHMENT_PREVIEWS);
                // mRefMessage = (Message)intent.getParcelableExtra(EXTRA_IN_REFERENCE_TO_MESSAGE);
                mRefMessageUri = (Uri)intent.getParcelableExtra(EXTRA_IN_REFERENCE_TO_MESSAGE_URI);

                if (Analytics.isLoggable()) {
                    if (intent.getBooleanExtra(UtilsEx.EXTRA_FROM_NOTIFICATION, false)) {
                        Analytics.getInstance().sendEvent("notification_action", "compose",
                                getActionString(action), 0);
                    }
                }
            }
            mAttachmentsView.setAttachmentPreviews(previews);

            // setAccount(account);
            // if (mAccount == null) {
            //    return;
            // }
            // mRecipents.initRecipients(this, mAccount);

            // Clear the notification and mark the conversation as seen, if
            // necessary
            final Folder notificationFolder = intent.getParcelableExtra(EXTRA_NOTIFICATION_FOLDER);
            if (notificationFolder != null) {
                // final Intent clearNotifIntent =
                // new
                // Intent(MailIntentService.ACTION_CLEAR_NEW_MAIL_NOTIFICATIONS);
                // clearNotifIntent.setPackage(getPackageName());
                // clearNotifIntent.putExtra(UtilsEx.EXTRA_ACCOUNT, account);
                // clearNotifIntent.putExtra(UtilsEx.EXTRA_FOLDER,
                // notificationFolder);
                //
                // startService(clearNotifIntent);
            }

            if (intent.getBooleanExtra(EXTRA_FROM_EMAIL_TASK, false)) {
                // mLaunchedFromEmail = true;
            } else if (Intent.ACTION_SEND.equals(intent.getAction())) {
                final Uri dataUri = intent.getData();
                if (dataUri != null) {
                    final String dataScheme = intent.getData().getScheme();
                    final String accountScheme = mAccount.composeIntentUri.getScheme();
                    // mLaunchedFromEmail = TextUtils.equals(dataScheme, accountScheme);
                }
            }

            if (mRefMessageUri != null) {
                // mShowQuotedText = true;
                mComposeMode = action;
                return;
            } else if (message != null && action != EDIT_DRAFT) {
                // initFromDraftMessage(message);
                // initQuotedTextFromRefMessage(mRefMessage, action);
                showCcBcc(savedState);
                // mShowQuotedText = message.appendRefMessageContent;
            } else if (action == EDIT_DRAFT) {
                // initFromDraftMessage(message);
                // mRecipents.initializeFromMessage(message);
                // Update the action to the draft type of the previous draft
                switch (message.draftType) {
                    case UIProvider.DraftType.REPLY:
                        action = REPLY;
                        break;
                    case UIProvider.DraftType.REPLY_ALL:
                        action = REPLY_ALL;
                        break;
                    case UIProvider.DraftType.FORWARD:
                        action = FORWARD;
                        break;
                    case UIProvider.DraftType.COMPOSE:
                    default:
                        action = COMPOSE;
                        break;
                }
                LogUtils.d(LOG_TAG, "Previous draft had action type: %d", action);

                // mShowQuotedText = message.appendRefMessageContent;
                if (message.refMessageUri != null) {
                    // If we're editing an existing draft that was in reference
                    // to an existing message,
                    // still need to load that original message since we might
                    // need to refer to the
                    // original sender and recipients if user switches
                    // "reply <-> reply-all".
                    mRefMessageUri = message.refMessageUri;
                    mComposeMode = action;
                    // getLoaderManager().initLoader(REFERENCE_MESSAGE_LOADER, null, this);
                    return;
                }
            } else if ((action == REPLY || action == REPLY_ALL || action == FORWARD)) {
                // if (mRefMessage != null) {
                //    initFromRefMessage(action);
                //    mShowQuotedText = true;
                // }
            } else {
                if (initFromExtras(intent)) {
                    return;
                }
            }

            mComposeMode = action;
            // finishSetup(action, intent, savedState);
    }

    private Account obtainAccount(Intent intent) {
        Account account = null;
        Object accountExtra = null;
        if (intent != null && intent.getExtras() != null) {
            accountExtra = intent.getExtras().get(UtilsEx.EXTRA_ACCOUNT);
            if (accountExtra instanceof Account) {
                return (Account) accountExtra;
            } else if (accountExtra instanceof String) {
                // This is the Account attached to the widget compose intent.
                account = Account.newinstance((String)accountExtra);
                if (account != null) {
                    return account;
                }
            }
            accountExtra = intent.hasExtra(UtilsEx.EXTRA_ACCOUNT) ?
                    intent.getStringExtra(UtilsEx.EXTRA_ACCOUNT) :
                        intent.getStringExtra(EXTRA_SELECTED_ACCOUNT);
        }
        if (account == null) {
            //            MailAppProvider provider = MailAppProvider.getInstance();
            //            String lastAccountUri = provider.getLastSentFromAccount();
            //            if (TextUtils.isEmpty(lastAccountUri)) {
            //                lastAccountUri = provider.getLastViewedAccount();
            //            }
            //            if (!TextUtils.isEmpty(lastAccountUri)) {
            //                accountExtra = Uri.parse(lastAccountUri);
            //            }
        }
        if (mAccounts != null && mAccounts.length > 0) {
            if (accountExtra instanceof String && !TextUtils.isEmpty((String) accountExtra)) {
                // For backwards compatibility, we need to check account
                // names.
                for (Account a : mAccounts) {
                    if (a.getEmailAddress().equals(accountExtra)) {
                        account = a;
                    }
                }
            } else if (accountExtra instanceof Uri) {
                // The uri of the last viewed account is what is stored in
                // the current code base.
                for (Account a : mAccounts) {
                    if (a.uri.equals(accountExtra)) {
                        account = a;
                    }
                }
            }
            if (account == null) {
                account = mAccounts[0];
            }
        }
        return account;
    }

    private static boolean hadSavedInstanceStateMessage(final Bundle savedInstanceState) {
        return savedInstanceState != null && savedInstanceState.containsKey(EXTRA_MESSAGE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Analytics.getInstance().activityStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Analytics.getInstance().activityStop(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update the from spinner as other accounts
        // may now be available.
        // if (mFromSpinner != null && mAccount != null) {
        //    mFromSpinner.initialize(mComposeMode, mAccount, mAccounts, mRefMessage);
        // }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // When the user exits the compose view, see if this draft needs saving.
        // Don't save unnecessary drafts if we are only changing the orientation.
        if (!isChangingConfigurations()) {
            // saveIfNeeded();

            if (isFinishing() && !mPerformedSendOrDiscard /*&& !isBlank()*/) {
                // log saving upon backing out of activity. (we avoid logging every sendOrSave()
                // because that method can be invoked many times in a single compose session.)
                logSendOrSave(true /* save */);
            }
        }
    }

    @Override
    protected final void onActivityResult(int request, int result, Intent data) {
         mController.handleResultFromSubActivity(request, result, data);
    }

    @Override
    protected final void onRestoreInstanceState(Bundle savedInstanceState) {
        final boolean hasAccounts = mAccounts != null && mAccounts.length > 0;
        if (hasAccounts) {
            // clearChangeListeners();
        }
        super.onRestoreInstanceState(savedInstanceState);
        if (mInnerSavedState != null) {
            if (mInnerSavedState.containsKey(EXTRA_FOCUS_SELECTION_START)) {
                int selectionStart = mInnerSavedState.getInt(EXTRA_FOCUS_SELECTION_START);
                int selectionEnd = mInnerSavedState.getInt(EXTRA_FOCUS_SELECTION_END);
                // There should be a focus and it should be an EditText since we
                // only save these extras if these conditions are true.
                EditText focusEditText = (EditText) getCurrentFocus();
                final int length = focusEditText.getText().length();
                if (selectionStart < length && selectionEnd < length) {
                    focusEditText.setSelection(selectionStart, selectionEnd);
                }
            }
        }
        if (hasAccounts) {
            // initChangeListeners();
        }
    }

    @Override
    protected final void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        final Bundle inner = new Bundle();
        saveState(inner);
        state.putBundle(KEY_INNER_SAVED_STATE, inner);
    }

    private void saveState(Bundle state) {
        // We have no accounts so there is nothing to compose, and therefore, nothing to save.
        if (mAccounts == null || mAccounts.length == 0) {
            return;
        }
        // The framework is happy to save and restore the selection but only if it also saves and
        // restores the contents of the edit text. That's a lot of text to put in a bundle so we do
        // this manually.
        View focus = getCurrentFocus();
        if (focus != null && focus instanceof EditText) {
            EditText focusEditText = (EditText) focus;
            state.putInt(EXTRA_FOCUS_SELECTION_START, focusEditText.getSelectionStart());
            state.putInt(EXTRA_FOCUS_SELECTION_END, focusEditText.getSelectionEnd());
        }

        final List<ReplyFromAccount> replyFromAccounts = null; //mFromSpinner.getReplyFromAccounts();
        final int selectedPos = 0; // mFromSpinner.getSelectedItemPosition();
        final ReplyFromAccount selectedReplyFromAccount = (replyFromAccounts != null
                && replyFromAccounts.size() > 0 && replyFromAccounts.size() > selectedPos) ?
                        replyFromAccounts.get(selectedPos) : null;
                        if (selectedReplyFromAccount != null) {
                            state.putString(EXTRA_SELECTED_REPLY_FROM_ACCOUNT, selectedReplyFromAccount.serialize()
                                    .toString());
                            state.putParcelable(UtilsEx.EXTRA_ACCOUNT, selectedReplyFromAccount.account);
                        } else {
                            // state.putParcelable(UtilsEx.EXTRA_ACCOUNT, mAccount);
                        }

                        if (mDraftId == UIProvider.INVALID_MESSAGE_ID && mRequestId !=0) {
                            // We don't have a draft id, and we have a request id,
                            // save the request id.
                            state.putInt(EXTRA_REQUEST_ID, mRequestId);
                        }

                        // We want to restore the current mode after a pause
                        // or rotation.
                        int mode = getMode();
                        state.putInt(EXTRA_ACTION, mode);

                        final Message message = null; //createMessage(selectedReplyFromAccount, mode);
                        // if (mDraft != null) {
                        //    message.id = mDraft.id;
                        //    message.serverId = mDraft.serverId;
                        //    message.uri = mDraft.uri;
                        //}
                        state.putParcelable(EXTRA_MESSAGE, message);

                        // if (mRefMessage != null) {
                        //    state.putParcelable(EXTRA_IN_REFERENCE_TO_MESSAGE, mRefMessage);
                        // } else if (message.appendRefMessageContent) {
                            // If we have no ref message but should be appending
                            // ref message content, we have orphaned quoted text. Save it.
//                      //      state.putCharSequence(EXTRA_QUOTED_TEXT, mQuotedTextView.getQuotedTextIfIncluded());
                        // }
                       
//                        state.putBoolean(EXTRA_SHOW_CC, true);
//                        state.putBoolean(EXTRA_SHOW_BCC, mRecipents.isBccVisible());
//                        state.putBoolean(EXTRA_RESPONDED_INLINE, mRespondedInline);
//                        state.putBoolean(EXTRA_SAVE_ENABLED, mSave != null && mSave.isEnabled());
//                        state.putParcelableArrayList(
//                                EXTRA_ATTACHMENT_PREVIEWS, mAttachmentsView.getAttachmentPreviews());

                        state.putParcelable(EXTRA_VALUES, mExtraValues);
    }

    private int getMode() {
        int mode = ComposeActivity.COMPOSE;
        ActionBar actionBar = getActionBar();
        if (actionBar != null
                && actionBar.getNavigationMode() == ActionBar.NAVIGATION_MODE_LIST) {
            mode = actionBar.getSelectedNavigationIndex();
        }
        return mode;
    }


    private ReplyFromAccount getReplyFromAccountForReply(Account account, Message refMessage) {
        if (refMessage.accountUri != null) {
            // This must be from combined inbox.
            List<ReplyFromAccount> replyFromAccounts = null; //mFromSpinner.getReplyFromAccounts();
            for (ReplyFromAccount from : replyFromAccounts) {
                if (from.account.uri.equals(refMessage.accountUri)) {
                    return from;
                }
            }
            return null;
        } else {
            return getReplyFromAccount(account, refMessage);
        }
    }

    /**
     * Given an account and the message we're replying to,
     * return who the message should be sent from.
     * @param account Account in which the message arrived.
     * @param refMessage Message to analyze for account selection
     * @return the address from which to reply.
     */
    public ReplyFromAccount getReplyFromAccount(Account account, Message refMessage) {
        // First see if we are supposed to use the default address or
        // the address it was sentTo.
        if (false/*mCachedSettings.forceReplyFromDefault*/) {
            return getDefaultReplyFromAccount(account);
        } else {
            // If we aren't explicitly told which account to look for, look at
            // all the message recipients and find one that matches
            // a custom from or account.
            List<String> allRecipients = new ArrayList<String>();
            allRecipients.addAll(Arrays.asList(refMessage.getToAddressesUnescaped()));
            allRecipients.addAll(Arrays.asList(refMessage.getCcAddressesUnescaped()));
            return getMatchingRecipient(account, allRecipients);
        }
    }

    /**
     * Compare all the recipients of an email to the current account and all
     * custom addresses associated with that account. Return the match if there
     * is one, or the default account if there isn't.
     */
    protected ReplyFromAccount getMatchingRecipient(Account account, List<String> sentTo) {
        // Tokenize the list and place in a hashmap.
        ReplyFromAccount matchingReplyFrom = null;
        Rfc822Token[] tokens;
        HashSet<String> recipientsMap = new HashSet<String>();
        for (String address : sentTo) {
            tokens = Rfc822Tokenizer.tokenize(address);
            for (int i = 0; i < tokens.length; i++) {
                recipientsMap.add(tokens[i].getAddress());
            }
        }

        int matchingAddressCount = 0;
        List<ReplyFromAccount> customFroms;
        customFroms = account.getReplyFroms();
        if (customFroms != null) {
            for (ReplyFromAccount entry : customFroms) {
                if (recipientsMap.contains(entry.address)) {
                    matchingReplyFrom = entry;
                    matchingAddressCount++;
                }
            }
        }
        if (matchingAddressCount > 1) {
            matchingReplyFrom = getDefaultReplyFromAccount(account);
        }
        return matchingReplyFrom;
    }

    private static ReplyFromAccount getDefaultReplyFromAccount(final Account account) {
        for (final ReplyFromAccount from : account.getReplyFroms()) {
            if (from.isDefault) {
                return from;
            }
        }
        return new ReplyFromAccount(account, account.uri, account.getEmailAddress(), account.name,
                account.getEmailAddress(), true, false);
    }

    private ReplyFromAccount getReplyFromAccountFromDraft(Account account, Message msg) {
        String sender = msg.getFrom();
        ReplyFromAccount replyFromAccount = null;
        List<ReplyFromAccount> replyFromAccounts = null; //mFromSpinner.getReplyFromAccounts();
        if (TextUtils.equals(account.getEmailAddress(), sender)) {
            // replyFromAccount = new ReplyFromAccount(mAccount, mAccount.uri,
            //        mAccount.getEmailAddress(), mAccount.name, mAccount.getEmailAddress(),
            //        true, false);
        } else {
            for (ReplyFromAccount fromAccount : replyFromAccounts) {
                if (TextUtils.equals(fromAccount.address, sender)) {
                    replyFromAccount = fromAccount;
                    break;
                }
            }
        }
        return replyFromAccount;
    }

    @VisibleForTesting
    public Account getFromAccount() {
        return null;
        // mReplyFromAccount != null && mReplyFromAccount.account != null ?
        //        mReplyFromAccount.account : mAccount;
    }


    /**
     * Fill all the widgets with the content found in the Intent Extra, if any.
     * Also apply the same style to all widgets. Note: if initFromExtras is
     * called as a result of switching between reply, reply all, and forward per
     * the latest revision of Gmail, and the user has already made changes to
     * attachments on a previous incarnation of the message (as a reply, reply
     * all, or forward), the original attachments from the message will not be
     * re-instantiated. The user's changes will be respected. This follows the
     * web gmail interaction.
     * @return {@code true} if the activity should not call {@link #finishSetup}.
     */
    public boolean initFromExtras(Intent intent) {
        // If we were invoked with a SENDTO intent, the value
        // should take precedence
//        final Uri dataUri = intent.getData();
//        if (dataUri != null) {
//            if (MAIL_TO.equals(dataUri.getScheme())) {
//                initFromMailTo(dataUri.toString());
//            } else {
//                if (!mAccount.composeIntentUri.equals(dataUri)) {
//                    String toText = dataUri.getSchemeSpecificPart();
//                    if (toText != null) {
//                        mRecipents.reset();
//                        mRecipents.addToAddresses(Arrays.asList(TextUtils.split(toText, ",")));
//                    }
//                }
//            }
//        }
//
//        String[] extraStrings = intent.getStringArrayExtra(Intent.EXTRA_EMAIL);
//        if (extraStrings != null) {
//            mRecipents.addToAddresses(Arrays.asList(extraStrings));
//        }
//        extraStrings = intent.getStringArrayExtra(Intent.EXTRA_CC);
//        if (extraStrings != null) {
//            mRecipents.addCcAddresses(Arrays.asList(extraStrings));
//        }
//        extraStrings = intent.getStringArrayExtra(Intent.EXTRA_BCC);
//        if (extraStrings != null) {
//            mRecipents.addBccAddresses(Arrays.asList(extraStrings));
//        }
//
//        String extraString = intent.getStringExtra(Intent.EXTRA_SUBJECT);
//        if (extraString != null) {
//            mSubject.setText(extraString);
//        }
//
//        for (String extra : ALL_EXTRAS) {
//            if (intent.hasExtra(extra)) {
//                String value = intent.getStringExtra(extra);
//                if (EXTRA_TO.equals(extra)) {
//                    mRecipents.addToAddresses(Arrays.asList(TextUtils.split(value, ",")));
//                } else if (EXTRA_CC.equals(extra)) {
//                    mRecipents.addCcAddresses(Arrays.asList(TextUtils.split(value, ",")));
//                } else if (EXTRA_BCC.equals(extra)) {
//                    mRecipents.addBccAddresses(Arrays.asList(TextUtils.split(value, ",")));
//                } else if (EXTRA_SUBJECT.equals(extra)) {
//                    mSubject.setText(value);
//                } else if (EXTRA_BODY.equals(extra)) {
//                    setBody(value, true /* with signature */);
//                } else if (EXTRA_QUOTED_TEXT.equals(extra)) {
//                    // initQuotedText(value, true /* shouldQuoteText */);
//                }
//            }
//        }

        Bundle extras = intent.getExtras();
        if (extras != null) {
            CharSequence text = extras.getCharSequence(Intent.EXTRA_TEXT);
            if (text != null) {
//                setBody(text, true /* with signature */);
            }

            // TODO - support EXTRA_HTML_TEXT
        }

        mExtraValues = intent.getParcelableExtra(EXTRA_VALUES);
//        if (mExtraValues != null) {
//            LogUtils.d(LOG_TAG, "Launched with extra values: %s", mExtraValues.toString());
//            initExtraValues(mExtraValues);
//            return true;
//        }

        return false;
    }

    @VisibleForTesting
    protected String decodeEmailInUri(String s) throws UnsupportedEncodingException {
        // TODO: handle the case where there are spaces in the display name as
        // well as the email such as "Guy with spaces <guy+with+spaces@gmail.com>"
        // as they could be encoded ambiguously.
        // Since URLDecode.decode changes + into ' ', and + is a valid
        // email character, we need to find/ replace these ourselves before
        // decoding.
        try {
            return URLDecoder.decode(replacePlus(s), UTF8_ENCODING_NAME);
        } catch (IllegalArgumentException e) {
            if (LogUtils.isLoggable(LOG_TAG, LogUtils.VERBOSE)) {
                LogUtils.e(LOG_TAG, "%s while decoding '%s'", e.getMessage(), s);
            } else {
                LogUtils.e(LOG_TAG, e, "Exception  while decoding mailto address");
            }
            return null;
        }
    }

    /**
     * Replaces all occurrences of '+' with "%2B", to prevent URLDecode.decode from
     * changing '+' into ' '
     *
     * @param toReplace Input string
     * @return The string with all "+" characters replaced with "%2B"
     */
    private static String replacePlus(String toReplace) {
        return toReplace.replace("+", "%2B");
    }

    /**
     * Initialize the compose view from a String representing a mailTo uri.
     * @param mailToString The uri as a string.
     */
    public void initFromMailTo(String mailToString) {
        // We need to disguise this string as a URI in order to parse it
        // TODO:  Remove this hack when http://b/issue?id=1445295 gets fixed
//        Uri uri = Uri.parse("foo://" + mailToString);
//        int index = mailToString.indexOf("?");
//        int length = "mailto".length() + 1;
//        String to;
//        try {
//            // Extract the recipient after mailto:
//            if (index == -1) {
//                to = decodeEmailInUri(mailToString.substring(length));
//            } else {
//                to = decodeEmailInUri(mailToString.substring(length, index));
//            }
//            if (!TextUtils.isEmpty(to)) {
//                mRecipents.addToAddresses(Arrays.asList(TextUtils.split(to, ",")));
//            }
//        } catch (UnsupportedEncodingException e) {
//            if (LogUtils.isLoggable(LOG_TAG, LogUtils.VERBOSE)) {
//                LogUtils.e(LOG_TAG, "%s while decoding '%s'", e.getMessage(), mailToString);
//            } else {
//                LogUtils.e(LOG_TAG, e, "Exception  while decoding mailto address");
//            }
//        }
//
//        mRecipents.initFromMailTo(uri);
//
//        List<String> subject = uri.getQueryParameters("subject");
//        if (subject.size() > 0) {
//            try {
//                mSubject.setText(URLDecoder.decode(replacePlus(subject.get(0)),
//                        UTF8_ENCODING_NAME));
//            } catch (UnsupportedEncodingException e) {
//                LogUtils.e(LOG_TAG, "%s while decoding subject '%s'",
//                        e.getMessage(), subject);
//            }
//        }
//
//        List<String> body = uri.getQueryParameters("body");
//        if (body.size() > 0) {
//            try {
//                setBody(URLDecoder.decode(replacePlus(body.get(0)), UTF8_ENCODING_NAME),
//                        true /* with signature */);
//            } catch (UnsupportedEncodingException e) {
//                LogUtils.e(LOG_TAG, "%s while decoding body '%s'", e.getMessage(), body);
//            }
//        }
    }

    @VisibleForTesting
    protected void initAttachments(Message refMessage) {
        addAttachments(refMessage.getAttachments());
    }

    public long addAttachments(List<Attachment> attachments) {
        long size = 0;
        AttachmentFailureException error = null;
        for (Attachment a : attachments) {
            try {
                 size += mAttachmentsView.addAttachment(mAccount, a);
            } catch (AttachmentFailureException e) {
                error = e;
            }
        }
        if (error != null) {
            LogUtils.e(LOG_TAG, error, "Error adding attachment");
            if (attachments.size() > 1) {
                showAttachmentTooBigToast(R.string.too_large_to_attach_multiple);
            } else {
                showAttachmentTooBigToast(error.getErrorRes());
            }
        }
        return size;
    }

    /**
     * When an attachment is too large to be added to a message, show a toast.
     * This method also updates the position of the toast so that it is shown
     * clearly above they keyboard if it happens to be open.
     */
    private void showAttachmentTooBigToast(int errorRes) {
        // String maxSize = AttachmentUtilities.convertToHumanReadableSize(
        //        getApplicationContext(), mAccount.settings.getMaxAttachmentSize());
        // showErrorToast(getString(errorRes, maxSize));
    }

    private void showErrorToast(String message) {
        Toast t = Toast.makeText(this, message, Toast.LENGTH_LONG);
        t.setText(message);
        t.setGravity(Gravity.CENTER_HORIZONTAL, 0,
                getResources().getDimensionPixelSize(R.dimen.attachment_toast_yoffset));
        t.show();
    }

    private void initAttachmentsFromIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras == null) {
            extras = Bundle.EMPTY;
        }
        final String action = intent.getAction();
        if (false/*!mAttachmentsChanged*/) {
            long totalSize = 0;
            if (extras.containsKey(EXTRA_ATTACHMENTS)) {
                String[] uris = (String[]) extras.getSerializable(EXTRA_ATTACHMENTS);
                for (String uriString : uris) {
                    final Uri uri = Uri.parse(uriString);
                    long size = 0;
                    try {
                        final Attachment a = mAttachmentsView.generateLocalAttachment(uri);
                        size = 0; //mAttachmentsView.addAttachment(mAccount, a);

                        Analytics.getInstance().sendEvent("send_intent_attachment",
                                UtilsEx.normalizeMimeType(a.getContentType()), null, size);

                    } catch (AttachmentFailureException e) {
                        LogUtils.e(LOG_TAG, e, "Error adding attachment");
                        showAttachmentTooBigToast(e.getErrorRes());
                    }
                    totalSize += size;
                }
            }
            if (extras.containsKey(Intent.EXTRA_STREAM)) {
                if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
                    ArrayList<Parcelable> uris = extras
                            .getParcelableArrayList(Intent.EXTRA_STREAM);
                    ArrayList<Attachment> attachments = new ArrayList<Attachment>();
                    for (Parcelable uri : uris) {
                        try {
                            final Attachment a = mAttachmentsView.generateLocalAttachment((Uri) uri);
                            attachments.add(a);

                            Analytics.getInstance().sendEvent("send_intent_attachment",
                                    UtilsEx.normalizeMimeType(a.getContentType()), null, a.size);

                        } catch (AttachmentFailureException e) {
                            LogUtils.e(LOG_TAG, e, "Error adding attachment");
                            String maxSize = AttachmentUtilities.convertToHumanReadableSize(
                                    getApplicationContext(),
                                    0/*mAccount.settings.getMaxAttachmentSize()*/);
                            showErrorToast(getString
                                    (R.string.generic_attachment_problem, maxSize));
                        }
                    }
                    totalSize += addAttachments(attachments);
                } else {
                    final Uri uri = (Uri) extras.getParcelable(Intent.EXTRA_STREAM);
                    long size = 0;
                    try {
                        final Attachment a = mAttachmentsView.generateLocalAttachment(uri);
                        size = mAttachmentsView.addAttachment(mAccount, a);

                        Analytics.getInstance().sendEvent("send_intent_attachment",
                                UtilsEx.normalizeMimeType(a.getContentType()), null, size);

                    } catch (AttachmentFailureException e) {
                        LogUtils.e(LOG_TAG, e, "Error adding attachment");
                        showAttachmentTooBigToast(e.getErrorRes());
                    }
                    totalSize += size;
                }
            }

            if (totalSize > 0) {
                // mAttachmentsChanged = true;
                updateSaveUi();

                Analytics.getInstance().sendEvent("send_intent_with_attachments",
                        Integer.toString(getAttachments().size()), null, totalSize);
            }
        }
    }

    private void showCcBcc(Bundle state) {
        if (state != null && state.containsKey(EXTRA_SHOW_CC)) {
            boolean showCc = state.getBoolean(EXTRA_SHOW_CC);
            boolean showBcc = state.getBoolean(EXTRA_SHOW_BCC);
            // if (showCc || showBcc) {
            //    mRecipents.show(false, showCc, showBcc);
            //}
        }
    }

    /**
     * Add attachment and update the compose area appropriately.
     * @param data
     */
    public void addAttachmentAndUpdateView(Intent data) {
        addAttachmentAndUpdateView(data != null ? data.getData() : (Uri) null);
    }

    public void addAttachmentAndUpdateView(Uri contentUri) {
        if (contentUri == null) {
            return;
        }
        try {
            addAttachmentAndUpdateView(mAttachmentsView.generateLocalAttachment(contentUri));
        } catch (AttachmentFailureException e) {
            LogUtils.e(LOG_TAG, e, "Error adding attachment");
            showErrorToast(getResources().getString(
                    e.getErrorRes(),
                    AttachmentUtilities.convertToHumanReadableSize(
                            getApplicationContext(), 0/*mAccount.settings.getMaxAttachmentSize()*/)));
        }
    }

    public void addAttachmentAndUpdateView(Attachment attachment) {
        try {
            long size = mAttachmentsView.addAttachment(mAccount, attachment);
            if (size > 0) {
                // mAttachmentsChanged = true;
                updateSaveUi();
            }
        } catch (AttachmentFailureException e) {
            LogUtils.e(LOG_TAG, e, "Error adding attachment");
            showAttachmentTooBigToast(e.getErrorRes());
        }
    }

    @VisibleForTesting
    protected void addCcAddressesToList(List<Rfc822Token[]> addresses,
            List<Rfc822Token[]> compareToList, RecipientEditTextView list) {
        String address;

        if (compareToList == null) {
            for (Rfc822Token[] tokens : addresses) {
                for (int i = 0; i < tokens.length; i++) {
                    address = tokens[i].toString();
                    list.append(address + END_TOKEN);
                }
            }
        } else {
            HashSet<String> compareTo = convertToHashSet(compareToList);
            for (Rfc822Token[] tokens : addresses) {
                for (int i = 0; i < tokens.length; i++) {
                    address = tokens[i].toString();
                    // Check if this is a duplicate:
                    if (!compareTo.contains(tokens[i].getAddress())) {
                        // Get the address here
                        list.append(address + END_TOKEN);
                    }
                }
            }
        }
    }

    private static HashSet<String> convertToHashSet(final List<Rfc822Token[]> list) {
        final HashSet<String> hash = new HashSet<String>();
        for (final Rfc822Token[] tokens : list) {
            for (int i = 0; i < tokens.length; i++) {
                hash.add(tokens[i].getAddress());
            }
        }
        return hash;
    }

    protected List<Rfc822Token[]> tokenizeAddressList(Collection<String> addresses) {
        @VisibleForTesting
        List<Rfc822Token[]> tokenized = new ArrayList<Rfc822Token[]>();

        for (String address: addresses) {
            tokenized.add(Rfc822Tokenizer.tokenize(address));
        }
        return tokenized;
    }

    @VisibleForTesting
    void addAddressesToList(Collection<String> addresses, RecipientEditTextView list) {
        for (String address : addresses) {
            addAddressToList(address, list);
        }
    }

    private static void addAddressToList(final String address, final RecipientEditTextView list) {
        if (address == null || list == null)
            return;

        final Rfc822Token[] tokens = Rfc822Tokenizer.tokenize(address);

        for (int i = 0; i < tokens.length; i++) {
            list.append(tokens[i] + END_TOKEN);
        }
    }

    @VisibleForTesting
    protected Collection<String> initToRecipients(final String fullSenderAddress,
            final String replyToAddress, final String[] inToAddresses) {
        // The To recipient is the reply-to address specified in the original
        // message, unless it is:
        // the current user OR a custom from of the current user, in which case
        // it's the To recipient list of the original message.
        // OR missing, in which case use the sender of the original message
        Set<String> toAddresses = Sets.newHashSet();
        if (!TextUtils.isEmpty(replyToAddress) && !recipientMatchesThisAccount(replyToAddress)) {
            toAddresses.add(replyToAddress);
        } else {
            // In this case, the user is replying to a message in which their
            // current account or one of their custom from addresses is the only
            // recipient and they sent the original message.
            if (inToAddresses.length == 1 && recipientMatchesThisAccount(fullSenderAddress)
                    && recipientMatchesThisAccount(inToAddresses[0])) {
                toAddresses.add(inToAddresses[0]);
                return toAddresses;
            }
            // This happens if the user replies to a message they originally
            // wrote. In this case, "reply" really means "re-send," so we
            // target the original recipients. This works as expected even
            // if the user sent the original message to themselves.
            for (String address : inToAddresses) {
                if (!recipientMatchesThisAccount(address)) {
                    toAddresses.add(address);
                }
            }
        }
        return toAddresses;
    }

    /**
     * A recipient matches this account if it has the same address as the
     * currently selected account OR one of the custom from addresses associated
     * with the currently selected account.
     * @param recipientAddress address we are comparing with the currently selected account
     * @return
     */
    protected boolean recipientMatchesThisAccount(String recipientAddress) {
        return ReplyFromAccount.matchesAccountOrCustomFrom(mAccount, recipientAddress,
                mAccount.getReplyFroms());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final boolean superCreated = super.onCreateOptionsMenu(menu);
        // Don't render any menu items when there are no accounts.
        if (!mController.hasAccounts()) {
            return superCreated;
        }
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.compose_menu, menu);
        mSend = menu.findItem(R.id.send);
        mSave = menu.findItem(R.id.save);
        mSend.setEnabled(false);
        mSave.setEnabled(false);

        /*
         * Start save in the correct enabled state.
         * 1) If a user launches compose from within gmail, save is disabled
         * until they add something, at which point, save is enabled, auto save
         * on exit; if the user empties everything, save is disabled, exiting does not
         * auto-save
         * 2) if a user replies/ reply all/ forwards from within gmail, save is
         * disabled until they change something, at which point, save is
         * enabled, auto save on exit; if the user empties everything, save is
         * disabled, exiting does not auto-save.
         * 3) If a user launches compose from another application and something
         * gets populated (attachments, recipients, body, subject, etc), save is
         * enabled, auto save on exit; if the user empties everything, save is
         * disabled, exiting does not auto-save
         */
//        String action = getIntent() != null ? getIntent().getAction() : null;
//        enableSave(mInnerSavedState != null ?
//                mInnerSavedState.getBoolean(EXTRA_SAVE_ENABLED)
//                : (Intent.ACTION_SEND.equals(action)
//                        || Intent.ACTION_SEND_MULTIPLE.equals(action)
//                        || Intent.ACTION_SENDTO.equals(action)
//                        || shouldSave()));
//
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();
        Analytics.getInstance().sendMenuItemEvent(Analytics.EVENT_CATEGORY_MENU_ITEM, id, null, 0);
        boolean handled = mController.handleMenuItemSelection(id);
        if (id == R.id.discard) {
            doDiscard();
        } else if (id == android.R.id.home) {
            onAppUpPressed();
        } else {
            handled = false;
        }

        return !handled ? super.onOptionsItemSelected(item) : handled;
    }

    @Override
    public void onBackPressed() {
        mController.handleSendOrSave(R.id.save);
        mController.closeScreen();
    }

    /**
     * Carries out the "up" action in the action bar.
     */
    private void onAppUpPressed() {
        onBackPressed();
    }

    public MenuItem getMenuItem(int menuItemName) {
       if (menuItemName == R.id.send)
          return mSend;
       else if (menuItemName == R.id.save)
          return mSave;
       return null;
    }

    private void finishRecipientErrorDialog() {
        // after the user dismisses the recipient error
        // dialog we want to make sure to refocus the
        // recipient to field so they can fix the issue
        // easily
        // if (mRecipents != null) {
        //    mRecipents.requestFocus();
        // }
    }

    /**
     * Update the state of the UI based on whether or not the current draft
     * needs to be saved and the message is not empty.
     */
    public void updateSaveUi() {
        // if (mSave != null) {
        //     mSave.setEnabled((shouldSave() /*&& !isBlank()*/));
        // }
    }

    /**
     * @param save
     * @param showToast
     * @return Whether the send or save succeeded.
     */
    protected boolean sendOrSaveWithSanityChecks(final boolean save, final boolean showToast,
            final boolean orientationChanged, final boolean autoSend) {
        if (mAccounts == null || mAccount == null) {
            Toast.makeText(this, R.string.send_failed, Toast.LENGTH_SHORT).show();
            if (autoSend) {
                finish();
            }
            return false;
        }

        if(true/*mRecipents.validateRecipents(save, orientationChanged)*/) {
            // Show a warning before sending only if there are no attachments.
            // showSendConfirmDialog(R.string.confirm_send_message_with_no_subject, save,showToast);
            // showSendConfirmDialog(R.string.confirm_send_message_with_no_subject, save,showToast);
            // showSendConfirmDialog(R.string.confirm_send_message, save, showToast);
        }

        // sendOrSave(save, showToast);
        return true;
    }

    /**
     * Returns a boolean indicating whether warnings should be shown for empty
     * subject and body fields
     *
     * @return True if a warning should be shown for empty text fields
     */
    protected boolean showEmptyTextWarnings() {
        return mAttachmentsView.getAttachments().size() == 0;
    }

    /**
     * Returns a boolean indicating whether the user should confirm each send
     *
     * @return True if a warning should be on each send
     */
    protected boolean showSendConfirmation() {
        return false; //mCachedSettings != null ? mCachedSettings.confirmSend : false;
    }



    private void finishSendConfirmDialog(final boolean save, final boolean showToast) {
        // sendOrSave(save, showToast);
    }

    private void showSendConfirmDialog(final int messageId, final boolean save,
            final boolean showToast) {
        final DialogFragment frag = SendConfirmDialogFragment.newInstance(messageId, save,
                showToast);
        frag.show(getFragmentManager(), "send confirm");
    }

    /**
     * Removes any composing spans from the specified string.  This will create a new
     * SpannableString instance, as to not modify the behavior of the EditText view.
     */
    private static SpannableString removeComposingSpans(Spanned body) {
        final SpannableString messageBody = new SpannableString(body);
        BaseInputConnection.removeComposingSpans(messageBody);
        return messageBody;
    }

//    private void sendOrSave(final boolean save, final boolean showToast) {}

    /**
     * Save the state of the request messageid map. This allows for the Gmail
     * process to be killed, but and still allow for ComposeActivity instances
     * to be recreated correctly.
     */
    private void saveRequestMap() {
        // TODO: store the request map in user preferences.
    }


    private static String getActionString(int action) {
        final String msgType;
        switch (action) {
            case COMPOSE:
                msgType = "new_message";
                break;
            case REPLY:
                msgType = "reply";
                break;
            case REPLY_ALL:
                msgType = "reply_all";
                break;
            case FORWARD:
                msgType = "forward";
                break;
            default:
                msgType = "unknown";
                break;
        }
        return msgType;
    }

    private void logSendOrSave(boolean save) {
        if (!Analytics.isLoggable() || mAttachmentsView == null) {
            return;
        }

        final String category = (save) ? "message_save" : "message_send";
        final int attachmentCount = getAttachments().size();
        final String msgType = getActionString(mComposeMode);
        final String label;
        final long value;
        if (mComposeMode == COMPOSE) {
            label = Integer.toString(attachmentCount);
            value = attachmentCount;
        } else {
            label = null;
            value = 0;
        }
        Analytics.getInstance().sendEvent(category, msgType, label, value);
    }


    // @Override
    // Impl for AttachmentAddedOrDeletedListener
    public void onRespondInline(String text) {
//        appendToBody(text, false);
//        mQuotedTextView.setUpperDividerVisible(false);
//        mRespondedInline = true;
//        if (!mBodyView.hasFocus()) {
//            mBodyView.requestFocus();
//        }
    }


    public static class DiscardConfirmDialogFragment extends DialogFragment {
        // Public no-args constructor needed for fragment re-instantiation
        public DiscardConfirmDialogFragment() {}

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
            .setMessage(R.string.confirm_discard_text)
            .setPositiveButton(R.string.discard,
                    new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ((ComposeActivity)getActivity()).doDiscardWithoutConfirmation();
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .create();
        }
    }

    private void doDiscard() {
        final DialogFragment frag = new DiscardConfirmDialogFragment();
        frag.show(getFragmentManager(), "discard confirm");
    }
    /**
     * Effectively discard the current message.
     *
     * This method is either invoked from the menu or from the dialog
     * once the user has confirmed that they want to discard the message.
     */
    private void doDiscardWithoutConfirmation() {
        synchronized (mDraftLock) {
            if (mDraftId != UIProvider.INVALID_MESSAGE_ID) {
                ContentValues values = new ContentValues();
                values.put(BaseColumns._ID, mDraftId);
                if (!mAccount.expungeMessageUri.equals(Uri.EMPTY)) {
                    getContentResolver().update(mAccount.expungeMessageUri, values, null, null);
                } else {
                    getContentResolver().delete(Uri.EMPTY/*mDraft.uri*/, null, null);
                }
                // This is not strictly necessary (since we should not try to
                // save the draft after calling this) but it ensures that if we
                // do save again for some reason we make a new draft rather than
                // trying to resave an expunged draft.
                mDraftId = UIProvider.INVALID_MESSAGE_ID;
            }
        }

        // Display a toast to let the user know
        Toast.makeText(this, R.string.message_discarded, Toast.LENGTH_SHORT).show();

        // This prevents the draft from being saved in onPause().
        // discardChanges();
        mPerformedSendOrDiscard = true;
        mController.closeScreen();
    }

    @VisibleForTesting
    protected ArrayList<Attachment> getAttachments() {
        return mAttachmentsView.getAttachments();
    }

    public void showWaitFragment(Account account) {
        WaitFragment fragment = getWaitFragment();
        if (fragment != null) {
            fragment.updateAccount(account);
        } else {
            findViewById(R.id.wait).setVisibility(View.VISIBLE);
            replaceFragment(WaitFragment.newInstance(account, true),
                    FragmentTransaction.TRANSIT_FRAGMENT_OPEN, TAG_WAIT);
        }
    }

    private WaitFragment getWaitFragment() {
        return (WaitFragment) getFragmentManager().findFragmentByTag(TAG_WAIT);
    }

    private int replaceFragment(Fragment fragment, int transition, String tag) {
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.setTransition(transition);
        fragmentTransaction.replace(R.id.wait, fragment, tag);
        final int transactionId = fragmentTransaction.commitAllowingStateLoss();
        return transactionId;
    }

}
