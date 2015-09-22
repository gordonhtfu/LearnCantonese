package com.blackberry.email.provider.contract;

public class SyncRequestMessage {

    private final String mAuthority;
    private final android.accounts.Account mAccount;
    private final long mMailboxId;

    public SyncRequestMessage(final String authority, final android.accounts.Account account, final long mailboxId) {
        mAuthority = authority;
        mAccount = account;
        mMailboxId = mailboxId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SyncRequestMessage that = (SyncRequestMessage) o;

        return mAccount.equals(that.mAccount)
                && mMailboxId == that.mMailboxId
                && mAuthority.equals(that.mAuthority);
    }

    @Override
    public int hashCode() {
        int result = mAuthority.hashCode();
        result = 31 * result + mAccount.hashCode();
        result = 31 * result + (int) (mMailboxId ^ (mMailboxId >>> 32));
        return result;
    }
}