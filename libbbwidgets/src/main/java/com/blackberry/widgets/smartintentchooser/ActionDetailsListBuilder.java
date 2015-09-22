
package com.blackberry.widgets.smartintentchooser;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;

import com.blackberry.widgets.smartintentchooser.HubAccounts.HubAccount;

import java.util.ArrayList;
import java.util.List;

class ActionDetailsListBuilder {
    private HubAccounts mHubAccounts;

    private List<ActionDetails> mResult;
    
    /**
     * I *REALLY* hope this is temporary as man is it fragile and ugly!
     */
    private static final String HUB_EMAIL_PACKAGE = "com.blackberry.emailservices";
    private static final String HUB_EMAIL_ACTIVITY = "com.blackberry.email.ui.compose.controllers.ComposeActivity";

    public ActionDetailsListBuilder(HubAccounts hubAccounts) {
        mHubAccounts = hubAccounts;
        mResult = new ArrayList<ActionDetails>();
    }

    public void createActionDetails(ResolveInfo resolveInfo) {
        if (HUB_EMAIL_ACTIVITY.equals(resolveInfo.activityInfo.name)
                && HUB_EMAIL_PACKAGE.equals(resolveInfo.activityInfo.packageName)) {
            for (HubAccount hubAccount : mHubAccounts.getAccounts()) {
                if (hubAccount.translateAccountType() == HubAccount.ACCOUNT_TYPE_EMAIL) {
                    mResult.add(new HubEmailActionDetails(resolveInfo, hubAccount));
                }
            }
        } else {
            mResult.add(new ResolveInfoActionDetails(resolveInfo));
        }
    }
    
    public List<ActionDetails> getList() {
        return mResult;
    }

    /**
     * An extension to ResolveInfoActionDetails which deals with adding Account
     * information to a standard HUB Email Compose Intent.
     */
    private static class HubEmailActionDetails extends ResolveInfoActionDetails {
        private HubAccount mAccount;

        public HubEmailActionDetails(ResolveInfo resolveInfo, HubAccount account) {
            super(resolveInfo);
            mAccount = account;
        }

        @Override
        public Intent getIntent(Intent originalIntent) {
            Intent result = super.getIntent(originalIntent);
            // TODO: convince them to put "fromAccountString" in lib.pimclient
            // so we can access it with a static constant
            result.putExtra("fromAccountString", mAccount.name);
            return result;
        }

        @Override
        public CharSequence getSubtitle(Context context) {
            return mAccount.displayName;
        }
    }
}
