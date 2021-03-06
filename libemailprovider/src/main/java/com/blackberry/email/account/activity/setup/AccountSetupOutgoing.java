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
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.blackberry.email.account.activity.ActivityHelper;
import com.blackberry.email.utils.UiUtilities;
import com.blackberry.lib.emailprovider.R;

/**
 * Provides setup flow for SMTP (for IMAP/POP accounts).
 *
 * Uses AccountSetupOutgoingFragment for primary UI.  Uses AccountCheckSettingsFragment to validate
 * the settings as entered.  If the account is OK, proceeds to AccountSetupOptions.
 */
public class AccountSetupOutgoing extends AccountSetupActivity
        implements AccountSetupOutgoingFragment.Callback, OnClickListener {

    /* package */ AccountSetupOutgoingFragment mFragment;
    private Button mNextButton;
    /* package */ boolean mNextButtonEnabled;

    public static void actionOutgoingSettings(Activity fromActivity, SetupData setupData) {
        Intent intent = new Intent(fromActivity, AccountSetupOutgoing.class);
        intent.putExtra(SetupData.EXTRA_SETUP_DATA, setupData);
        fromActivity.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityHelper.debugSetWindowFlags(this);
        setContentView(R.layout.account_setup_outgoing);

        mFragment = (AccountSetupOutgoingFragment)
                getFragmentManager().findFragmentById(R.id.setup_fragment);

        // Configure fragment
        mFragment.setCallback(this);

        mNextButton = UiUtilities.getView(this, R.id.next);
        mNextButton.setOnClickListener(this);
        UiUtilities.getView(this, R.id.previous).setOnClickListener(this);
    }

    /**
     * Implements View.OnClickListener
     */
    @Override
	public void onClick(View view) {
		int vid = view.getId();
		if (vid == R.id.next) {
			mFragment.onNext();
		} else if (vid == R.id.previous) {
			onBackPressed();
		}

	}

    /**
     * Implements AccountSetupOugoingFragment.Callback
     *
     * Launches the account checker.  Positive results are reported to onCheckSettingsOk().
     */
    @Override
    public void onProceedNext(int checkMode, AccountServerBaseFragment target) {
        final AccountCheckSettingsFragment checkerFragment =
            AccountCheckSettingsFragment.newInstance(checkMode, target);
        final FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.add(checkerFragment, AccountCheckSettingsFragment.TAG);
        transaction.addToBackStack("back");
        transaction.commit();
    }

    /**
     * Implements AccountSetupOugoingFragment.Callback
     */
    @Override
    public void onEnableProceedButtons(boolean enable) {
        mNextButtonEnabled = enable;
        mNextButton.setEnabled(enable);
    }

    /**
     * Implements AccountServerBaseFragment.Callback
     *
     * If the checked settings are OK, proceed to options screen
     */
    @Override
    public void onCheckSettingsComplete(int result, SetupData setupData) {
        mSetupData = setupData;
        if (result == AccountCheckSettingsFragment.CHECK_SETTINGS_OK) {
            AccountSetupOptions.actionOptions(this, mSetupData);
            finish();
        }
    }
}
