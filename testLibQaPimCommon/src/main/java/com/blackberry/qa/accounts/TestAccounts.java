package com.blackberry.qa.accounts;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.blackberry.provider.AccountContract.Account;


public class TestAccounts {

    private final static String TAG = TestAccounts.class.getSimpleName();

    /**
     * Account id of the email address passed in in the constructor
     */
    public Long mAccountId = null;
    /**
     * Context of the calling app/service/test
     */
    private final Context mContext;

    /**
     * Initial setup for an email from the specified email address
     * @param context current context
     * @param emailAddr email address eg. example@blah.com
     */
    public TestAccounts(Context context, String emailAddress) {
        mContext = context;
        mAccountId = getAccountId(emailAddress);
    }

    /**
     * Gets the account id for the email provided
     * @param email email address eg. example@blah.com
     * @exception IllegalArgumentException if account does not exist.
     * @return account id
     */
    private long getAccountId(String emailAddress) {
        Log.d(TAG, "Getting Account ID for " + emailAddress);
        String[] accountProjection = { Account._ID, Account.TYPE, Account.NAME };
        String where = Account.NAME + "=?";
        String[] whereArgs = {emailAddress};
        Cursor accounts = null;
        Long accountId = null;
        accounts = mContext.getContentResolver().query(Account.CONTENT_URI, accountProjection,
                where, whereArgs, null);
        if (accounts != null && accounts.moveToNext()) {
            int idIdx = accounts.getColumnIndex(Account._ID);
            accountId = accounts.getLong(idIdx);
        }
        if (accounts != null)
            accounts.close();
        if (accountId == null) {
            throw new IllegalArgumentException("Email account not valid or does not exist");
        }
        Log.d(TAG, "Found account id for " + emailAddress + ": " + Long.toString(accountId));
        return accountId;
    }

    /**
     * Get the number of accounts currently on device
     * 
     * @param context current context
     * @return number of accounts on the device
     *
     * @throws Exception
     * 
     */

    public static int getNumberOfAccounts(Context context) throws Exception{
        String[] proj = {
                Account.NAME
        };
        Cursor c = context.getContentResolver().query(Account.CONTENT_URI, proj, null, null, null, null);
        if (c == null) {
            throw new Exception("blah");
        }
        return c.getCount();
    }
}