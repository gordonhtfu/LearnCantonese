
package com.blackberry.pimbase.provider.utilities;

import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.net.Uri;

import java.util.ArrayList;

public class Operations extends ArrayList<Operation> {
    private static final long serialVersionUID = 1L;
    private static final long SEPARATOR_ID = Long.MAX_VALUE;

    public int mCount = 0;

    ArrayList<Operation> mOpsWithSep = new ArrayList<Operation>();

    @Override
    public final boolean add(Operation op) {
        if (!op.mSeparator) {
            super.add(op);
            mCount++;
        }
        // container tracking ops and separators
        mOpsWithSep.add(op);
        return true;
    }

    @Override
    public final Operation remove(int index) {
        Operation retObj = super.remove(index);
        mOpsWithSep.remove(retObj);

        if (!retObj.mSeparator) {
            mCount--;
        }

        return retObj;
    }

    @Override
    public final Operation set(int index, Operation op) {
        Operation old = super.set(index, op);
        mOpsWithSep.set(mOpsWithSep.indexOf(old), op);
        return old;
    }

    public ArrayList<Operation> getOpsIncludingSeparators() {
        return mOpsWithSep;
    }

    public void addSeparatorOperation(Uri uri) {
        Operation op = new Operation(getSeparatorOperation(uri), true);
        this.add(op);
    }

    protected ContentProviderOperation getSeparatorOperation(Uri uri) {
        return ContentProviderOperation.newDelete(ContentUris.withAppendedId(uri, SEPARATOR_ID))
                .build();
    }
}
