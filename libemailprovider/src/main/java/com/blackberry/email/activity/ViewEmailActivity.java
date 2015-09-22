
package com.blackberry.email.activity;

import com.blackberry.common.utils.LogUtils;
import com.blackberry.email.Account;
import com.blackberry.email.Conversation;
import com.blackberry.email.Folder;
import com.blackberry.email.FolderList;
import com.blackberry.email.ui.AbstractConversationViewFragment;
import com.blackberry.email.ui.AccountController;
import com.blackberry.email.ui.ControllableActivity;
import com.blackberry.email.ui.ConversationListCallbacks;
import com.blackberry.email.ui.ConversationUpdater;
import com.blackberry.email.ui.DestructiveAction;
import com.blackberry.email.ui.FragmentLauncher;
import com.blackberry.email.ui.SecureConversationViewFragment;
import com.blackberry.email.ui.ToastBarOperation;
import com.blackberry.email.ui.UpOrBackController;
import com.blackberry.email.ui.ViewMode;
import com.blackberry.email.ui.browse.ConversationCursor;
import com.blackberry.email.ui.browse.ConversationMessage;
import com.blackberry.email.ui.browse.ConversationPagerController;
import com.blackberry.email.utils.AccountUtils;
import com.blackberry.email.utils.VeiledAddressMatcher;
import com.blackberry.lib.emailprovider.R;
import com.blackberry.menu.MenuBuilder;
import com.blackberry.menu.MenuItemDetails;
import com.blackberry.menu.RequestedItem;
import com.blackberry.provider.ListItemContract;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class ViewEmailActivity extends Activity implements ControllableActivity, AccountController,
        ConversationUpdater {

    private static final String TAG = "ViewEmail";

    private ConversationPagerController mPagerController;
    private ConversationCursor mConvCursor;
    private Account mAccount = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_email);
        initActionBar();
        Intent viewConversationIntent = this.getIntent();

        if (viewConversationIntent != null) {
            Bundle extras = viewConversationIntent.getExtras();

            if (extras != null) {
                Long lAccountId = extras.getLong(ListItemContract.ListItemColumns.ACCOUNT_ID);
                String strMessageUri = viewConversationIntent.getDataString();
                Uri messageUri = Uri.parse(strMessageUri);

                if (lAccountId != null && strMessageUri != null) {
                    // load account data
                    mAccount = AccountUtils.getAccount(this, lAccountId.longValue());
                    Folder folder = null;
                    // This activity is currently only used for single messages. The conversation
                    // query that used to be here is being replaced with an explicitly created
                    // conversation object until we add proper conversation support.
                    mConvCursor = new ConversationCursor((Activity) this, messageUri, true,
                            "ViewEmail");
                    Conversation conv = new Conversation();
                    conv.id = Long.valueOf(messageUri.getPathSegments().get(1));
                    conv.messageListUri = messageUri;
                    if (viewConversationIntent.getType() != null &&
                        viewConversationIntent.getType().equals(getString(R.string.conversation_mimetype))) {
                        conv.messageListUri = conv.messageListUri.buildUpon()
                                .appendQueryParameter("message", "true").build();
                    }
                    conv.uri = messageUri;
                    conv.setRawFolders(FolderList.fromBlob(null));
                    if (conv != null) {
                        // need to get the data to populate the Views
                        mPagerController = new ConversationPagerController(this, null);
                        // this really just loads a fragment,
                        mPagerController.show(mAccount, folder, conv, true);
                    }
                } else {
                    LogUtils.e(TAG, "Missing Expected Intent Data");
                    this.finish();
                }
            }
        }
    }

    // this can be using instead of the pager, but currently the pager has a
    // nice controller that
    // will make the message/conversation as read - keeping this here for quick
    // ref
    protected AbstractConversationViewFragment getViewEmailDetailsFragment(Conversation c,
            Account account) {
        Bundle mCommonFragmentArgs = AbstractConversationViewFragment.makeBasicArgs(account);
        return SecureConversationViewFragment.newInstance(mCommonFragmentArgs, c);
    }

    @Override
    protected void onDestroy() {
        if (mConvCursor != null && !mConvCursor.isClosed()) {
            mConvCursor.close();
        }
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == android.R.id.home) {
            this.onBackPressed();
        } else {
            try {
                // TODO: fix this when menuservice is ready to launch service
                Set<String> categories = item.getIntent().getCategories();
                if (categories != null && categories.contains("activity")) {
                    this.startActivity(item.getIntent());
                } else {
                    this.startService(item.getIntent());
                }
            } catch (Exception e) {
                Log.e("ListItemCabDelegate", "mContext.startService/Activity failure.");
                e.printStackTrace();
            }
        }

        return true;
    }

    private void initActionBar() {
        ActionBar actionBar = getActionBar();
        if (actionBar == null) {
            return;
        }
        actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_HOME,
                ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_HOME);
        actionBar.setHomeButtonEnabled(true);
    }

    @Override
    public Context getActivityContext() {
        return getBaseContext();
    }

    @Override
    public void setPendingToastOperation(ToastBarOperation op) {
        // TODO Auto-generated method stub

    }

    @Override
    public ToastBarOperation getPendingToastOperation() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void onUndoAvailable(ToastBarOperation undoOp) {
        // TODO Auto-generated method stub

    }

    @Override
    public ViewMode getViewMode() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ConversationListCallbacks getListHandler() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Folder getHierarchyFolder() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ConversationUpdater getConversationUpdater() {
        // TODO Auto-generated method stub
        return this;
    }

    @Override
    public AccountController getAccountController() {
        // TODO Auto-generated method stub
        return this;
    }

    @Override
    public UpOrBackController getUpOrBackController() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isAccessibilityEnabled() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public FragmentLauncher getFragmentLauncher() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void registerAccountObserver(DataSetObserver observer) {
        // TODO Auto-generated method stub

    }

    @Override
    public void unregisterAccountObserver(DataSetObserver observer) {
        // TODO Auto-generated method stub

    }

    @Override
    public Account getAccount() {
        return mAccount;
    }

    @Override
    public void registerAllAccountObserver(DataSetObserver observer) {
        // TODO Auto-generated method stub

    }

    @Override
    public void unregisterAllAccountObserver(DataSetObserver observer) {
        // TODO Auto-generated method stub

    }

    @Override
    public Account[] getAllAccounts() {
        return AccountUtils.getAccounts(this);
    }

    @Override
    public VeiledAddressMatcher getVeiledAddressMatcher() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void changeAccount(Account account) {
        // TODO Auto-generated method stub

    }

    @Override
    public void switchToDefaultInboxOrChangeAccount(Account account) {
        // TODO Auto-generated method stub

    }

    @Override
    public int getFolderListViewChoiceMode() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void onConversationSelected(Conversation conversation, boolean inLoaderCallbacks) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onCabModeEntered() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onCabModeExited() {
        // TODO Auto-generated method stub

    }

    @Override
    public ConversationCursor getConversationListCursor() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Conversation getCurrentConversation() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setCurrentConversation(Conversation c) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isInitialConversationLoading() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void registerConversationLoadedObserver(DataSetObserver observer) {
        // TODO Auto-generated method stub

    }

    @Override
    public void unregisterConversationLoadedObserver(DataSetObserver observer) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onConversationSeen() {
        // TODO Auto-generated method stub

    }

    @Override
    public void registerConversationListObserver(DataSetObserver observer) {
        // TODO Auto-generated method stub

    }

    @Override
    public void unregisterConversationListObserver(DataSetObserver observer) {
        // TODO Auto-generated method stub

    }

    @Override
    public void commitDestructiveActions(boolean animate) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isAnimating() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setDetachedMode() {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateConversation(Collection<Conversation> target, ContentValues values) {
        this.mConvCursor.updateValues(target, values);
    }

    @Override
    public void updateConversation(Collection<Conversation> target, String columnName,
            boolean value) {
        mConvCursor.updateBoolean(target, columnName, value);
    }

    @Override
    public void updateConversation(Collection<Conversation> target, String columnName,
            int value) {
        mConvCursor.updateInt(target, columnName, value);
    }

    @Override
    public void updateConversation(Collection<Conversation> target, String columnName,
            String value) {
        mConvCursor.updateString(target, columnName, value);
    }

    @Override
    public void delete(int actionId, Collection<Conversation> target, DestructiveAction action,
            boolean isBatch) {
        // TODO Auto-generated method stub

    }

    @Override
    public void markConversationMessagesUnread(Conversation conv, Set<Uri> unreadMessageUris,
            byte[] originalConversationInfo) {
        // TODO Auto-generated method stub

    }

    @Override
    public void starMessage(ConversationMessage msg, boolean starred) {
        // TODO Auto-generated method stub

    }

    @Override
    public DestructiveAction getBatchAction(int action) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DestructiveAction getDeferredBatchAction(int action) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DestructiveAction getDeferredRemoveFolder(Collection<Conversation> target,
            Folder toRemove, boolean isDestructive, boolean isBatch, boolean showUndo) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void refreshConversationList() {
        // TODO Auto-generated method stub

    }

    @Override
    public void showNextConversation(Collection<Conversation> conversations) {
        // TODO Auto-generated method stub

    }

    @Override
    public void makeDialogListener(int action, boolean fromSelectedSet) {
        // TODO Auto-generated method stub

    }

    @Override
    public OnClickListener getListener() {
        // TODO Auto-generated method stub
        return null;
    }

}
