
package com.blackberry.email.ui.compose.views;

import java.util.ArrayList;
import java.util.List;

import android.app.ActionBar;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import com.blackberry.common.ui.editablewebview.EditableWebView;
import com.blackberry.email.Attachment;
import com.blackberry.email.ui.compose.controllers.ComposeActivity;
import com.blackberry.email.ui.compose.controllers.ComposeScreenController;
import com.blackberry.email.ui.compose.controllers.JSRepository;
import com.blackberry.email.ui.compose.views.AttachmentsView.AttachmentAddedOrDeletedListener;
import com.blackberry.email.ui.compose.views.FromAddressSpinner.OnAccountChangedListener;
import com.blackberry.lib.emailprovider.R;
import com.blackberry.message.service.MessageContactValue;

public class ComposeScreen implements EditableWebView.EventCallbacks,
        TextView.OnEditorActionListener,
        OnAccountChangedListener, ViewEditListener, AttachmentAddedOrDeletedListener,
        OnClickListener, RecipientsFragment.CallBack {

    public int mScreenType = ComposeActivity.COMPOSE;

    private static final int USER_ACTION_HANDLED = -1;
    protected static final String LOG_TAG = null;
    private Resources mContextResources;
    protected int mUserAction = USER_ACTION_HANDLED;
    protected boolean mIsDirty;
    protected ComposeActivity mActivityContext;
    protected ComposeScreenController mController;
    protected static final String COMPOSITION_DOCUMENT_TEMPLATE = new StringBuilder()
            .append("<html>")
            .append(JSRepository.RESPONSE_SECTION)
            .append("</html>").toString();

    // views this screen screen is hosting
    protected SubjectField mSubject;
    protected RecipientsFragment mRecipientsFrag;
    protected EditableWebView mBody;
    protected FromAddressSpinner mFromSpinner;
    protected View mFromStatic;
    protected TextView mFromStaticText;
    protected View mFromSpinnerWrapper;

    // attachment related
    protected AttachmentsView mAttachmentsView;
    protected View mPhotoAttachmentsButton;
    protected View mVideoAttachmentsButton;

    // screen states
    protected String mDocumentContents;
    
    private boolean mSetSignature = false;

	private boolean mCachedSendState = false;
   
    public enum FieldID {
        TO_FIELD,
        CC_FIELD,
        BCC_FIELD,
        SUBJECT,
        BODY;
    }

    public ComposeScreen(ComposeScreenController controller, ComposeActivity context,
            Bundle savedInstanceState, int screenType) {
        mActivityContext = context;
        mContextResources = mActivityContext.getResources();
        mScreenType = screenType;
        mController = controller;

        // Regardless of the type of screen we're building, we'll require
        // accounts
        // info, fire up the async query asap.
        mController.fetchAccounts();

        initialize();
    }

    public void initialize() {
        // find and initialize all views findViews()
        mActivityContext.setContentView(R.layout.compose);
        initializeActionBar(mActivityContext.getActionBar());
        cacheViews();
        mBody.registerForEventCallbacks(this);
        mBody.loadSecureData(COMPOSITION_DOCUMENT_TEMPLATE);
        mSubject.observeEditsOnView(this);
        mRecipientsFrag.observeEditsOnView(this);
        mRecipientsFrag.registerForRecipientChanges(this);
        mFromSpinner.setOnAccountChangedListener(this);
        mAttachmentsView.setAttachmentChangesListener(this);
        mAttachmentsView.observeEditsOnView(this);
        if (mPhotoAttachmentsButton != null) {
            mPhotoAttachmentsButton.setOnClickListener(this);
        }
        if (mVideoAttachmentsButton != null) {
            mVideoAttachmentsButton.setOnClickListener(this);
        }
        // asynchronously notify the main controller in case its waiting for UI
        // initialization
        // to complete.
        mBody.post(new Runnable() {
            @Override
            public void run() {
                mController.uisetupComplete();
            }
        });
    }

    protected void initializeActionBar(ActionBar actionBar) {
        if (actionBar == null)
            return;
        if (mScreenType == ComposeActivity.COMPOSE || mScreenType == ComposeActivity.EDIT_DRAFT) {
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
            actionBar.setTitle(R.string.compose);
        }
        actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_HOME,
                ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_HOME);
        actionBar.setHomeButtonEnabled(true);
    }

    private void cacheViews() {
        mRecipientsFrag = (RecipientsFragment) mActivityContext.getFragmentManager()
                .findFragmentById(R.id.recipients_fragment);
        mAttachmentsView = (AttachmentsView) mActivityContext.findViewById(R.id.attachments);
        mPhotoAttachmentsButton = mActivityContext.findViewById(R.id.add_photo_attachment);
        mVideoAttachmentsButton = mActivityContext.findViewById(R.id.add_video_attachment);
        mSubject = (SubjectField) mActivityContext.findViewById(R.id.subjectField);
        mSubject.setOnEditorActionListener(this);
        mBody = (EditableWebView) mActivityContext.findViewById(R.id.composition_area);
        mFromStatic = mActivityContext.findViewById(R.id.static_from_content);
        mFromStaticText = (TextView) mActivityContext.findViewById(R.id.from_account_name);
        mFromSpinnerWrapper = mActivityContext.findViewById(R.id.spinner_from_content);
        mFromSpinner = (FromAddressSpinner) mActivityContext.findViewById(R.id.from_picker);
    }

    public String getText(FieldID fieldId) {
        String value = "";
        if (fieldId.compareTo(FieldID.SUBJECT) == 0)
            value = mSubject.getText().toString();
        else if (fieldId.compareTo(FieldID.BODY) == 0)
            value = mDocumentContents;
        else if (fieldId.compareTo(FieldID.TO_FIELD) == 0)
            value = mRecipientsFrag.getFormattedRecipients(FieldID.TO_FIELD);
        else if (fieldId.compareTo(FieldID.CC_FIELD) == 0)
            value = mRecipientsFrag.getFormattedRecipients(FieldID.CC_FIELD);
        else if (fieldId.compareTo(FieldID.BCC_FIELD) == 0)
            value = mRecipientsFrag.getFormattedRecipients(FieldID.BCC_FIELD);

        return value;
    }

    public List<MessageContactValue> getRecipientAsContacts(FieldID fieldId) {
        return mRecipientsFrag.getRecipients(fieldId);
    }

    @Override
    public boolean onEditorAction(TextView view, int action, KeyEvent keyEvent) {
        if (action == EditorInfo.IME_ACTION_DONE) {
            mBody.requestFocus();
            return true;
        }
        return false;
    }

    public boolean isDirty() {
        return mIsDirty;
    }

    public void setDirty(boolean isDirty) {
        if (mIsDirty != isDirty) {
            mIsDirty = isDirty;
            MenuItem saveMenu = mActivityContext.getMenuItem(R.id.save);
            if (saveMenu != null) {
                saveMenu.setEnabled(isDirty());
            }
        }
    }

    public void setUserAction(int userAction) {
        mUserAction = userAction;
    }

    @Override
    public void onMicroFocusChanged() {
        // TODO sync RTF toolbar
    }

    @Override
    public void onLoadStarted(String url) {
        setUpDefaultFocus();
    }

    @Override
    public void onLoadSucceeded(String url) {
        mBody.evaluateJavascript(JSRepository.SETUP_SCRIPT_ENVIRONMENT);
        setSignatureAndStartObserving();
    }

    public void accountsLoaded() {
        setSignatureAndStartObserving();
    }

    private void setSignatureAndStartObserving() {
        // This is called in two cases: (1) when the html document
        // is loaded and (2) when the accounts are loaded. If
        // mSetSignature is true we set the signature, otherwise we
        // set it to true so when the next event happens, whichever it
        // may be, we will set the signature then
        if (mSetSignature) {
            setSignature();
            setDirty(false);
            mBody.observeContentChanges();
        } else {
            mSetSignature = true;
        }
    }

    private void setSignature() {
        if (mController.getCurrentAccountSettings() != null
                && mController.getCurrentAccountSettings().signature != null) {
            mBody.evaluateJavascript(String.format(JSRepository.INSERT_AUTO_SIGNATURE,
                    mController.getCurrentAccountSettings().signature));
        }
    }

    @Override
    public void processJSResult(int scriptID, String scriptResult) {
        // TODO Auto-generated method stub

    }

    @Override
    public void documentAvailable(String document, boolean isHtml) {
        mDocumentContents = document;
        if (mUserAction == R.id.save) {
            mController.save();
        }
        
        if (mUserAction == R.id.send) {
            mController.send();
        }
        mUserAction = USER_ACTION_HANDLED;
    }

    @Override
    public void onAccountChanged() {
        if (!mController.onAccountChanged()) {
            return;
        }
        setDirty(true);
        setSignature();
    }

    public void prepareSendOrSave() {
        if (mUserAction == R.id.save) {
        }
        if (mUserAction == R.id.send) {
            mBody.setEditable(false);
            cleanupDocument();
        }
        mBody.fetchDocumentContents(true);
    }

    protected void cleanupDocument() {
      // gives a chance to response screens to
      // perform custom cleanup on the document.
      mBody.evaluateJavascript(JSRepository.CLEANUP_BASED_ON_INTERNAL_RULES, null);
    }

    public void toggleBcc() {
        mRecipientsFrag.showHideBccView();
    }

    public void discardChanges() {
        setDirty(false);
    }

    public FromAddressSpinner getAccountSelector() {
        return mFromSpinner;
    }

    public void setupAccountSelectorState(boolean showStaticText, String[] accountDetails) {
        final String accDisplayName = (accountDetails[0] == null) ? "" :  accountDetails[0];
        mFromStaticText.setText(accDisplayName.isEmpty() ? accountDetails[1] : accDisplayName + " - " + accountDetails[1]);
        mFromStatic.setVisibility(showStaticText ? View.VISIBLE : View.GONE);
        mFromSpinnerWrapper.setVisibility(showStaticText ? View.GONE : View.VISIBLE);
    }

    private void setUpDefaultFocus() {

    }

    // TODO: Attachment related callbacks, move to its own controller which
    // handles events from AttachmentsView ---------------------
    @Override
    public void onClick(View v) {
        mController.handleMenuItemSelection(v.getId());
    }

    @Override
    public void onAttachmentDeleted() {
        setDirty(true);
    }

    @Override
    public void onAttachmentAdded() {
        mAttachmentsView.focusLastAttachment();
    }

    public ArrayList<Attachment> getAttachments() {
        return mAttachmentsView.getAttachments();
    }

    @Override
    public void recipientsChanged(boolean containsValidTags) {
        MenuItem sendMenu = mActivityContext.getMenuItem(R.id.send);
        if (sendMenu != null) {
            sendMenu.setEnabled(mCachedSendState ? mCachedSendState : containsValidTags);
            mCachedSendState = false;
        } else {
           mCachedSendState |= containsValidTags;
        }
    }
}
