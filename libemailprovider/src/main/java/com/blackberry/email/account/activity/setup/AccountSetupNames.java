/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.blackberry.email.account.activity.setup;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract.Profile;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.TextKeyListener;
import android.text.method.TextKeyListener.Capitalize;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.blackberry.email.account.activity.ActivityHelper;
import com.blackberry.email.utils.UiUtilities;
import com.blackberry.email.provider.AccountBackupRestore;
import com.blackberry.email.service.EmailServiceUtils;
import com.blackberry.email.service.EmailServiceUtils.EmailServiceInfo;
import com.blackberry.email.provider.contract.Account;
import com.blackberry.email.provider.contract.EmailContent.AccountColumns;
import com.blackberry.email.utils.EmailAsyncTask;
import com.blackberry.email.utils.Utility;
import com.blackberry.lib.emailprovider.R;

/**
 * Final screen of setup process.  Collect account nickname and/or username.
 */
public class AccountSetupNames extends AccountSetupActivity {
    private static final int REQUEST_SECURITY = 0;

    private static final Uri PROFILE_URI = Profile.CONTENT_URI;

    private EditText mDescription;
    private EditText mName;
    private Button mNextButton;
    private boolean mRequiresName = true;
    private boolean mIsCompleting = false;

    public static void actionSetNames(Activity fromActivity, SetupData setupData) {
        ForwardingIntent intent = new ForwardingIntent(fromActivity, AccountSetupNames.class);
        intent.putExtra(SetupData.EXTRA_SETUP_DATA, setupData);
        fromActivity.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityHelper.debugSetWindowFlags(this);
        setContentView(R.layout.account_setup_names);
        mDescription = UiUtilities.getView(this, R.id.account_description);
        mName = UiUtilities.getView(this, R.id.account_name);
        final View accountNameLabel = UiUtilities.getView(this, R.id.account_name_label);
        mNextButton = UiUtilities.getView(this, R.id.next);
        mNextButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onNext();
            }
        });

        final TextWatcher validationTextWatcher = new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                validateFields();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        };
        mName.addTextChangedListener(validationTextWatcher);
        mName.setKeyListener(TextKeyListener.getInstance(false, Capitalize.WORDS));

        final Account account = mSetupData.getAccount();
        if (account == null) {
            throw new IllegalStateException("unexpected null account");
        }
        if (account.mHostAuthRecv == null) {
            throw new IllegalStateException("unexpected null hostauth");
        }

        final int flowMode = mSetupData.getFlowMode();

        if (flowMode != SetupData.FLOW_MODE_FORCE_CREATE
                && flowMode != SetupData.FLOW_MODE_EDIT) {
            final String accountEmail = account.mEmailAddress;
            mDescription.setText(accountEmail);

            // Move cursor to the end so it's easier to erase in case the user doesn't like it.
            mDescription.setSelection(accountEmail.length());
        }

        // Remember whether we're an EAS account, since it doesn't require the user name field
        final EmailServiceInfo info =
                EmailServiceUtils.getServiceInfo(this, account.mHostAuthRecv.mProtocol);
        if (!info.usesSmtp) {
            mRequiresName = false;
            mName.setVisibility(View.GONE);
            accountNameLabel.setVisibility(View.GONE);
        } else {
            if (account.getSenderName() != null) {
                mName.setText(account.getSenderName());
            } else if (flowMode != SetupData.FLOW_MODE_FORCE_CREATE
                    && flowMode != SetupData.FLOW_MODE_EDIT) {
                // Attempt to prefill the name field from the profile if we don't have it,
                prefillNameFromProfile();
            }
        }

        // Make sure the "done" button is in the proper state
        validateFields();

        // Proceed immediately if in account creation mode
        if (flowMode == SetupData.FLOW_MODE_FORCE_CREATE) {
            onNext();
        }
    }

    private void prefillNameFromProfile() {
        new EmailAsyncTask<Void, Void, String>(null) {
            @Override
            protected String doInBackground(Void... params) {
                final String[] projection = new String[] { Profile.DISPLAY_NAME };
                return Utility.getFirstRowString(
                        AccountSetupNames.this, PROFILE_URI, projection, null, null, null, 0);
            }

            @Override
            public void onSuccess(String result) {
                // Views can only be modified on the main thread.
                mName.setText(result);
            }
        }.executeParallel((Void[]) null);
    }

    /**
     * Check input fields for legal values and enable/disable next button
     */
    private void validateFields() {
        boolean enableNextButton = true;
        // Validation is based only on the "user name" field, not shown for EAS accounts
        if (mRequiresName) {
            final String userName = mName.getText().toString().trim();
            if (TextUtils.isEmpty(userName)) {
                enableNextButton = false;
                mName.setError(getString(R.string.account_setup_names_user_name_empty_error));
            } else {
                mName.setError(null);
            }
        }
        mNextButton.setEnabled(enableNextButton);
    }

    /**
     * Block the back key if we are currently processing the "next" key"
     */
    @Override
    public void onBackPressed() {
        if (!mIsCompleting) {
            finishActivity();
        }
    }

    private void finishActivity() {
        if (mSetupData.getFlowMode() == SetupData.FLOW_MODE_NO_ACCOUNTS) {
            AccountSetupBasics.actionAccountCreateFinishedWithResult(this);
        } else if (mSetupData.getFlowMode() != SetupData.FLOW_MODE_NORMAL) {
            AccountSetupBasics.actionAccountCreateFinishedAccountFlow(this);
        } else {
            final Account account = mSetupData.getAccount();
            if (account != null) {
                AccountSetupBasics.actionAccountCreateFinished(this, account);
            }
        }
        finish();
    }

    /**
     * After clicking the next button, we'll start an async task to commit the data
     * and other steps to finish the creation of the account.
     */
    private void onNext() {
        mNextButton.setEnabled(false); // Protect against double-tap.
        mIsCompleting = true;

        // Update account object from UI
        final Account account = mSetupData.getAccount();
        final String description = mDescription.getText().toString().trim();
        if (!TextUtils.isEmpty(description)) {
            account.setDisplayName(description);
        }
        account.setSenderName(mName.getText().toString().trim());

        // Launch async task for final commit work
        // Sicne it's a write task, use the serial executor so even if we ran the task twice
        // with different values the result would be consistent.
        new FinalSetupTask(account).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
    }

    /**
     * Final account setup work is handled in this AsyncTask:
     *   Commit final values to provider
     *   Trigger account backup
     *   Check for security hold
     *
     * When this completes, we return to UI thread for the following steps:
     *   If security hold, dispatch to AccountSecurity activity
     *   Otherwise, return to AccountSetupBasics for conclusion.
     *
     * TODO: If there was *any* indication that security might be required, we could at least
     * force the DeviceAdmin activation step, without waiting for the initial sync/handshake
     * to fail.
     * TODO: If the user doesn't update the security, don't go to the MessageList.
     */
    private class FinalSetupTask extends AsyncTask<Void, Void, Boolean> {

        private final Account mAccount;
        private final Context mContext;

        public FinalSetupTask(Account account) {
            mAccount = account;
            mContext = AccountSetupNames.this;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // Update the account in the database
            final ContentValues cv = new ContentValues();
            cv.put(AccountColumns.DISPLAY_NAME, mAccount.getDisplayName());
            cv.put(AccountColumns.SENDER_NAME, mAccount.getSenderName());
            mAccount.update(mContext, cv);

            // Update the backup (side copy) of the accounts
            AccountBackupRestore.backup(AccountSetupNames.this);

            return Account.isSecurityHold(mContext, mAccount.mId);
        }

        @Override
        protected void onPostExecute(Boolean isSecurityHold) {
            if (!isCancelled()) {
                if (isSecurityHold) {
                    final Intent i = AccountSecurity.actionUpdateSecurityIntent(
                            AccountSetupNames.this, mAccount.mId, false);
                    startActivityForResult(i, REQUEST_SECURITY);
                } else {
                    finishActivity();
                }
            }
        }
    }

    /**
     * Handle the eventual result from the security update activity
     *
     * TODO: If the user doesn't update the security, don't go to the MessageList.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_SECURITY:
                finishActivity();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}
