
package com.blackberry.widgets.smartintentchooser;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

import com.blackberry.provider.AccountContract;

import java.util.ArrayList;
import java.util.List;

/**
 * Used to expose a list of HUB accounts (if any) registered on the system.
 */
class HubAccounts {
    private Context mContext;
    private List<HubAccount> mAccounts;

    public HubAccounts(Context context) {
        mContext = context;

        fetchAccounts();
    }

    /**
     * @return The list of accounts which may be empty.
     */
    public List<HubAccount> getAccounts() {
        return mAccounts;
    }

    private void fetchAccounts() {
        String[] projection = new String[] {
                AccountContract.Account.DISPLAY_NAME,
                AccountContract.Account.NAME,
                AccountContract.Account.PACKAGE_NAME,
                AccountContract.Account.TYPE
        };
        Cursor cur = mContext.getContentResolver().query(AccountContract.Account.CONTENT_URI,
                projection, null, null, null);
        if ((cur == null) || (cur.getCount() == 0)) {
            mAccounts = new ArrayList<HubAccount>(0);
            return;
        }
        int displayNameIndex = cur.getColumnIndex(AccountContract.Account.DISPLAY_NAME);
        int nameIndex = cur.getColumnIndex(AccountContract.Account.NAME);
        int packageIndex = cur.getColumnIndex(AccountContract.Account.PACKAGE_NAME);
        int typeIndex = cur.getColumnIndex(AccountContract.Account.TYPE);
        List<HubAccount> result = new ArrayList<HubAccount>(cur.getCount());
        while (cur.moveToNext()) {
            result.add(new HubAccount(cur.getString(displayNameIndex), cur.getString(nameIndex),
                    cur.getString(packageIndex), cur.getString(typeIndex)));
        }
        mAccounts = result;
    }

    /**
     * Represents a HUB account.
     */
    public static class HubAccount {
        public String displayName;
        public String name;
        public String packageName;
        public String type;

        public static final int ACCOUNT_TYPE_UNKNOWN = 0;
        public static final int ACCOUNT_TYPE_EMAIL = 1;
        public static final int ACCOUNT_TYPE_CALLS = 2;
        public static final int ACCOUNT_TYPE_SMS = 3;

        private static final String CALLS_TYPE_STRING = "vnd.android.cursor.item/calls";
        private static final String SMS_TYPE_STRING = "vnd.android.cursor.item/mms-sms";
        private static final String EMAIL_TYPE_STRING_PREFIX = "com.blackberry.email";
        private static final String EMAIL_EAS_TYPE_STRING = "com.blackberry.eas";

        public HubAccount(String displayName, String name, String packageName, String type) {
            this.displayName = displayName;
            this.name = name;
            this.packageName = packageName;
            this.type = type;
        }

        /**
         * @return One of {@link #ACCOUNT_TYPE_CALLS},
         *         {@link #ACCOUNT_TYPE_EMAIL}, {@link #ACCOUNT_TYPE_SMS} or
         *         {@link #ACCOUNT_TYPE_UNKNOWN}.
         */
        public int translateAccountType() {
            if (!TextUtils.isEmpty(type)) {
                if (type.equals(CALLS_TYPE_STRING)) {
                    return ACCOUNT_TYPE_CALLS;
                }
                if (type.equals(SMS_TYPE_STRING)) {
                    return ACCOUNT_TYPE_SMS;
                }
                if (type.equals(EMAIL_EAS_TYPE_STRING) || type.startsWith(EMAIL_TYPE_STRING_PREFIX)) {
                    return ACCOUNT_TYPE_EMAIL;
                }
            }
            return ACCOUNT_TYPE_UNKNOWN;
        }

        @Override
        public String toString() {
            return "HubAccount: \"" + displayName + "\" <" + name + "> @ " + packageName + " ["
                    + type + "]";
        }
    }
}
