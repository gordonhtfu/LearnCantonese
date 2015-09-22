
package com.blackberry.email.ui.compose.controllers;

import java.util.ArrayList;
import java.util.List;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.blackberry.datagraph.provider.DataGraphContract;
import com.blackberry.email.Account;
import com.blackberry.email.ReplyFromAccount;
import com.blackberry.email.Settings;
import com.blackberry.email.provider.EmailProvider;
import com.blackberry.email.provider.UIProvider;
import com.blackberry.email.ui.compose.ComposeUtils;
import com.blackberry.email.ui.compose.views.ComposeScreen;
import com.blackberry.email.ui.compose.views.ComposeScreen.FieldID;
import com.blackberry.email.ui.compose.views.ResponseScreen;
import com.blackberry.lib.emailprovider.R;
import com.blackberry.message.service.MessageBodyValue;
import com.blackberry.message.service.MessageContactValue;
import com.blackberry.message.service.MessageValue;
import com.blackberry.message.service.MessagingService;
import com.blackberry.provider.MessageContract;
import com.blackberry.provider.MessageContract.Message;
import com.blackberry.provider.MessageContract.MessageContact;

/**
 * @author rratan
 */
public class ComposeScreenController implements LoaderManager.LoaderCallbacks<Cursor> {

    private ComposeScreen mEditor = null;
    private ComposeActivity mActivityContext;
    private BaseComposeDelegate mDelegate;
    private Bundle mInnerSavedState;
    private static final String KEY_INNER_SAVED_STATE = "compose_state";

    private static final String LOG_TAG = "ComposeScreenController";
    private static final String MIME_TYPE_PHOTO = "image/*";
    private static final String MIME_TYPE_VIDEO = "video/*";

    // Loader Ids
    private static final int REFERENCE_MESSAGE_LOADER = 0;
    private static final int LOADER_ACCOUNT_CURSOR = 1;

    // Request numbers for activities we start
    private static final int RESULT_PICK_ATTACHMENT = 1;
    private static final int RESULT_CREATE_ACCOUNT = 2;

    // model data
    private Account[] mAccounts;
    private Uri mRefMessageUri;
    private MessageValue mRefMessage;
    private String mAccountName;
    MessagingService mEmailMsgService;

    // states
    private Account mSelectedAccount;
    private long mSelectedAccountID;
    private Settings mAccountSettings;
    private boolean mAddingAttachment;
    
    private static final String EXTRA_FROM_ACCOUNT_STRING = "fromAccountString";
    
    public ComposeScreenController(ComposeActivity context, Bundle savedInstanceState) {
        mActivityContext = context;
        initialize(savedInstanceState);
    }

    // Create or restore screen and controller states.
    private void initialize(Bundle savedInstanceState) {
        mInnerSavedState = (savedInstanceState != null) ? savedInstanceState
                .getBundle(KEY_INNER_SAVED_STATE) : null;
    }

    public void createScreen(long accountId) {
        mSelectedAccountID = accountId;
        mEditor = new ComposeScreen(this, mActivityContext, mInnerSavedState,
                ComposeActivity.COMPOSE);
        initializeDelegate();
    }

    /**
     * Used to build screen based on a message.
     * 
     * @param message Message object to base the screen contents on.
     * @param responseType Type of compose screen to build.
     */
    // Handle screen restore from parceled MessageValue object
    public void createScreen(MessageValue message, int responseType) {
        mRefMessage = message;
        mEditor = new ResponseScreen(this, mActivityContext, mInnerSavedState, responseType);
        initializeDelegate();
    }

    public void createScreen(Uri messageUri, int responseType) {
        if (messageUri != null) {
            mRefMessageUri = messageUri;
            // kick off message fetch asap, if we know that this is a response
            // screen.
            mActivityContext.getLoaderManager().initLoader(REFERENCE_MESSAGE_LOADER, null, this);
            mEditor = new ResponseScreen(this, mActivityContext, mInnerSavedState, responseType);
            initializeDelegate();
        } else {
            Log.e(LOG_TAG, "Could not create a response screen for Uri: " + messageUri);
        }
    }

    /**
     * Builds an email compose screen based on the data contained in
     * incomingIntent. Applies to screen restores from saved bundles, and all
     * types of compose screens which do not have a message object.
     * 
     * @param incomingIntent Intent object as received from invoker.
     */
    // TODO: this should take a Bundle instead of an intent
    public void createScreen(Intent incomingIntent) {
        Object extras = incomingIntent.getExtras();
        if (extras instanceof Bundle) {
            Bundle bundle = (Bundle) extras;
            if (bundle.containsKey(EXTRA_FROM_ACCOUNT_STRING)) {
                mAccountName = bundle.getString(EXTRA_FROM_ACCOUNT_STRING);
                mEditor = new ComposeScreen(this, mActivityContext, mInnerSavedState,
                        ComposeActivity.COMPOSE);
                initializeDelegate();
            } else {
                Log.e(LOG_TAG, "Could not create a compose screen from bundle: " + bundle.toString());
            }
        }
    }

    private void initializeDelegate() {
        // Create respective handlers based on message type
        mDelegate = new BaseComposeDelegate(mActivityContext, mEditor);
    }

    private void validateServiceDelegate() {
       if (mDelegate != null) {
           mEmailMsgService = mDelegate.getValidService(mSelectedAccountID);
       }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case REFERENCE_MESSAGE_LOADER:
                return new CursorLoader(mActivityContext, mRefMessageUri, MessageContract.Message.DEFAULT_PROJECTION,
                         null, null, null);
            case LOADER_ACCOUNT_CURSOR:
                return new CursorLoader(mActivityContext, EmailProvider.uiUri("uiaccts", -1),
                        UIProvider.ACCOUNTS_PROJECTION, null, null, null);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Guaranteed to be called after UI is done setting up.
        switch (loader.getId()) {
            case LOADER_ACCOUNT_CURSOR:
                cacheAccounts(data);
                populateAccountSelector();
                if (!isResponseScreen()) {
                    mEditor.accountsLoaded();
                }
                break;
            case REFERENCE_MESSAGE_LOADER:
                if (data != null && data.moveToFirst()) {
                    mRefMessage = new MessageValue(data);
                    mRefMessage.restoreMessageContacts(mActivityContext);
                    mRefMessage.restoreMessageBodies(mActivityContext);
                    mActivityContext.getLoaderManager().destroyLoader(REFERENCE_MESSAGE_LOADER);
                }
                break;
        }
        // Wait for both loaders to finish before populating the screen.
        if (mAccounts != null && mRefMessage != null) {
            if (mEditor instanceof ResponseScreen) {
                populateEditorFromMessage();
                mEditor.accountsLoaded();
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        // We've cached all of our loader results already, and
        // we do not need to listen to changes to email accounts or message.
    }

    public void send() {
        MessageValue message = buildMessageFromEditor(R.id.send);
        final String sentMsgID = mDelegate.send(message , mRefMessageUri);
        Log.i(LOG_TAG, "Resulting msgId from send attempt: " + sentMsgID);
        Toast.makeText(mActivityContext, R.string.sending_message, Toast.LENGTH_LONG).show();
        closeScreen();
    }

    public void save() {
        if (!mEditor.isDirty())
            return;
        MessageValue message = buildMessageFromEditor(R.id.save);
        final String savedMsgID = mDelegate.save(message);
        Log.i(LOG_TAG, "Resulting msgId from save attempt: " + savedMsgID);
        Toast.makeText(mActivityContext, R.string.save_draft, Toast.LENGTH_LONG).show();
         closeScreen();
    }

    public void handleResultFromSubActivity(int request, int result, Intent data) {
        if (request == RESULT_PICK_ATTACHMENT && result == ComposeActivity.RESULT_OK) {
            // addAttachmentAndUpdateView(data);
            mAddingAttachment = false;
        } else if (request == RESULT_CREATE_ACCOUNT) {
            // We were waiting for the user to create an account
            if (result != ComposeActivity.RESULT_OK) {
                mActivityContext.finish();
            } else {
                // Wait for user to add accounts.
                fetchAccounts();
                mActivityContext.showWaitFragment(null);
            }
        }
    }

    // sync controller states with UI
    public boolean onAccountChanged() {
        boolean handledAccountChange = false;
        ReplyFromAccount changedAccount = mEditor.getAccountSelector().getCurrentAccount();
        if (changedAccount != null && !mSelectedAccount.uri.equals(changedAccount.account.uri)) {
            handledAccountChange = true;
            setSelectedAccount(changedAccount.account);
        }
        if (!handledAccountChange)
            Log.e(LOG_TAG, "Out of sync with UI");

        return handledAccountChange;
    }

    public boolean handleMenuItemSelection(int menuID) {
        boolean handled = true;
        if (menuID == R.id.add_photo_attachment) {
            launchAttachmentPicker(MIME_TYPE_PHOTO);
        } else if (menuID == R.id.add_video_attachment) {
            launchAttachmentPicker(MIME_TYPE_VIDEO);
        } else if (menuID == R.id.add_bcc) {
            mEditor.toggleBcc();
        } else if (menuID == R.id.save || menuID == R.id.send) {
            handleSendOrSave(menuID);
        } else {
            handled = false;
        }
        return handled;
    }

    public void handleSendOrSave(int actionId) {
        if (!mEditor.isDirty() && actionId == R.id.save)
            return;
        mEditor.setUserAction(actionId);
        mEditor.prepareSendOrSave();
    }

    public MessageValue getReferenceMessage() {
        return mRefMessage;
    }

    public void uisetupComplete() {

    }

    public Account getSelectedAccount() {
        return mSelectedAccount;
    }

    public boolean hasAccounts() {
        return !(mAccounts == null || mAccounts.length == 0);
    }

    public void fetchAccounts() {
        mActivityContext.getLoaderManager().initLoader(LOADER_ACCOUNT_CURSOR, null, this);
    }

    public Settings getCurrentAccountSettings() {
        return mAccountSettings;
    }

    public boolean isResponseScreen() {
        return mEditor instanceof ResponseScreen;
    }

    private void setSelectedAccount(Account selectedAccount) {
        if (mSelectedAccount != null  && mSelectedAccount.uri.equals(selectedAccount.uri))
            return;
        mSelectedAccount = selectedAccount;
        mEditor.getAccountSelector().setCurrentAccount(new ReplyFromAccount(mSelectedAccount,
                mSelectedAccount.uri, mSelectedAccount.getEmailAddress(), mSelectedAccount.name,
                mSelectedAccount.getEmailAddress(), true, false));
        mAccountSettings = mSelectedAccount.settings;
        //TODO: Account value object should have a getID(), this is hacky!
        mSelectedAccountID = ComposeUtils.getAccountID(mSelectedAccount);
        validateServiceDelegate();
    }

    private void setComposeAccount() {
        Account composeAccount = mAccounts[0];

        if (mSelectedAccountID != 0) {
            for (Account account : mAccounts) {
                if (mSelectedAccountID == ComposeUtils.getAccountID(account)) {
                    composeAccount = account;
                    break;
                }
            }
        } else if (mAccountName != null) {
            for (Account account : mAccounts) {
                // TODO: A better way would be to pass the account URI instead
                // of the account name or email address
                String accName = account.name;
                String emailAddress = account.getEmailAddress();
                if ((emailAddress != null && emailAddress.equalsIgnoreCase(mAccountName))
                        || (accName != null && accName.equalsIgnoreCase(mAccountName))) {
                    composeAccount = account;
                    break;
                }
            }
        }

        setSelectedAccount(composeAccount);
    }

    private void populateAccountSelector() {
        if (mAccounts == null || mAccounts.length == 0) {
            return;
        }

        setComposeAccount(); 
        mEditor.getAccountSelector().initialize(mEditor.mScreenType, mSelectedAccount, mAccounts, mRefMessage);
        if (mSelectedAccount != null && mSelectedAccount.getEmailAddress() != null) {
            String senderName = (mSelectedAccount.getSenderName() != null) ? mSelectedAccount.getSenderName()
                                                                         : mSelectedAccount.getEmailAddress();
            String[] accountDetails = {senderName, mSelectedAccount.getEmailAddress()};
            mEditor.setupAccountSelectorState((mAccounts.length == 1), accountDetails);
        } else {
            Log.e(LOG_TAG, "Could not select any account. Account cache empty or preferred account not found.");
        }
    }

    private void cacheAccounts(Cursor data) {
        if (data != null && data.moveToFirst()) {
            // there are accounts now!
            Account account;
            final ArrayList<Account> accounts = new ArrayList<Account>();
            final ArrayList<Account> initializedAccounts = new ArrayList<Account>();
            do {
                account = new Account(data);
                if (account.isAccountReady()) {
                    initializedAccounts.add(account);
                }
                accounts.add(account);
            } while (data.moveToNext());
            if (initializedAccounts.size() > 0) {
                mActivityContext.findViewById(R.id.wait).setVisibility(View.GONE);
                mActivityContext.getLoaderManager().destroyLoader(LOADER_ACCOUNT_CURSOR);
                mActivityContext.findViewById(R.id.composeForm).setVisibility(View.VISIBLE);
                mAccounts = initializedAccounts.toArray(
                        new Account[initializedAccounts.size()]);

                // cc/Bcc needs to be changed for responseScreens
                mActivityContext.invalidateOptionsMenu();

            } else {
                // Show "waiting"
                account = hasAccounts() ? accounts.get(0) : null;
                // mActivityContext.showWaitFragment(account);
            }
            mActivityContext.getLoaderManager().destroyLoader(LOADER_ACCOUNT_CURSOR);
        }
    }

    private void populateEditorFromMessage() {
        if (!(mEditor instanceof ResponseScreen)) {
            return;
        }
        // set current account selection to match the account to which
        // refMessage belongs
        for (Account account : mAccounts) {
            if (ComposeUtils.getAccountID(account) == mRefMessage.mAccountId) {
                setSelectedAccount(account);
            }
        }
        if (mEditor instanceof ResponseScreen)
            ((ResponseScreen) mEditor).initializeFromMessage(mRefMessage);
    }

    private void launchAttachmentPicker(String type) {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        i.setType(type);
        mAddingAttachment = true;
        mActivityContext.startActivityForResult(
                Intent.createChooser(i, mActivityContext.getText(R.string.select_attachment_type)),
                RESULT_PICK_ATTACHMENT);
    }

    private MessageValue buildMessageFromEditor(int userAction) {

        MessageValue message = new MessageValue();
        message.mSubject = mEditor.getText(FieldID.SUBJECT);
        message.mAccountId = mSelectedAccountID;
        message.mSender = mSelectedAccount.getEmailAddress();

        //need from contact as well
        MessageContactValue from = new MessageContactValue();
        from.mAddress =  mSelectedAccount.getEmailAddress();
        from.mFieldType = MessageContact.FieldType.FROM;
        from.mName = mSelectedAccount.getSenderName();
        message.add(from);

        message.addMessageContacts(mEditor.getRecipientAsContacts(FieldID.TO_FIELD));
        List<MessageContactValue> contactsCC = mEditor.getRecipientAsContacts(FieldID.CC_FIELD);
        if (!contactsCC.isEmpty())
            message.addMessageContacts(contactsCC);
        List<MessageContactValue> contactsBCC = mEditor.getRecipientAsContacts(FieldID.BCC_FIELD);
        if (!contactsBCC.isEmpty())
            message.addMessageContacts(contactsBCC);

        MessageBodyValue body = new MessageBodyValue();
        body.mContentBytes = mEditor.getText(FieldID.BODY).getBytes();
        //TODO: Conditionally populate message.bodyText with plaintext impl.
        body.mType = MessageContract.MessageBody.Type.HTML;
        message.add(body);

        if (mRefMessage != null) {
            message.mConversationId = mRefMessage.mConversationId;
        }

        //so 2 UC for this 1. re-save a draft or when a draft is about to be 
        //sent - service needs id of message or a new one will be create and 
        //and will no idea about the ref message data
        if(mRefMessage != null && (userAction == R.id.save || 
                (userAction == R.id.send && (mRefMessage.mState & Message.State.DRAFT) != 0 ))){

            message.mId = mRefMessage.mId;
            message.mMimeType = mRefMessage.mMimeType;
            message.mEntityUri = mRefMessage.mEntityUri;
        }

        return message;
    }

    public void closeScreen() {
        if (mEmailMsgService != null) {
            if (isResponseScreen() && mRefMessage != null) {
                mDelegate.markMessageRead(mRefMessage);
            }
            mEmailMsgService.close();
            mEmailMsgService = null;
            Log.d(LOG_TAG, "Closing active connection to Message service");
        }
        mActivityContext.finish();
    }

}
