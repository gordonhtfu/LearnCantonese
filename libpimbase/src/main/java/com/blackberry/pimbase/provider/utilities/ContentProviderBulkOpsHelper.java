
package com.blackberry.pimbase.provider.utilities;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.OperationApplicationException;
import android.os.RemoteException;
import android.os.TransactionTooLargeException;

import com.blackberry.common.utils.LogUtils;
import com.google.common.annotations.VisibleForTesting;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class ContentProviderBulkOpsHelper {
    private static final String TAG = "CPBulkOpsHelper";

    /**
     * commit() this method will attempt to apply a batch of operations against
     * a CP. If the operations fails due to TransactionTooLargeException retry
     * logic will then be executed where the operations will be broken into
     * chunks based on a separator operation and smaller apply batch call will
     * be made. Note separator logic must be setup when creating the operation
     * in order to take advantage of this logic. Also this only has a single
     * retry
     * 
     * @param resovler
     * @param authority
     * @param operations
     * @return
     * @throws IOException
     */
    public static ContentProviderResult[] commit(ContentResolver resovler, String authority,
            ArrayList<Operation> operations) throws IOException {
        if (operations.isEmpty()) {
            return new ContentProviderResult[0];
        }

        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>(
                operations.size());
        for (Operation op : operations) {
            if (!op.mSeparator) { // Don't need to commit separators
                ops.add(op.operationToContentProviderOperation(0));
            }
        }

        try {
            return resovler.applyBatch(authority, ops);
        } catch (TransactionTooLargeException e) {
            LogUtils.w(TAG,
                    "TransactionTooLargeException applying batch, attempting to commit in chunks");
            return commitBySeparators(resovler, authority, operations);
        } catch (final RemoteException e) {
            LogUtils.e(TAG, "RemoteException in commit");
            throw new IOException("RemoteException in commit");
        } catch (final OperationApplicationException e) {
            LogUtils.e(TAG, "OperationApplicationException in commit");
            throw new IOException("OperationApplicationException in commit");
        }
    }

    @VisibleForTesting
    static ContentProviderResult[] commitBySeparators(ContentResolver resolver,
            String authority, ArrayList<Operation> ops) throws IOException {
        ArrayList<Operation> mini = new ArrayList<Operation>();
        // using ArrayList as I do not know how many separators are in the ops
        // collection
        // and I intend on only returning actual results that are executed
        // against a CP
        ArrayList<ContentProviderResult> arResults = new ArrayList<ContentProviderResult>();

        int count = 0;
        int offset = 0;

        for (Operation op : ops) {
            // because the separator is not part of the offset adjustment, we
            // will
            // just count real CPO's and adjust based on them
            if (op.mSeparator) {
                commitMini(resolver, authority, mini, arResults, offset);
                mini.clear();
                offset = count;
            } else {
                mini.add(op);
                count++;
            }
        }

        // Check out what's left, if it's more than just a separator, apply the
        // batch
        int miniSize = mini.size();
        if ((miniSize > 0) && !(miniSize == 1 && mini.get(0).mSeparator)) {
            commitMini(resolver, authority, mini, arResults, offset);
        }

        // result set only has real results - separator ops are not included!
        return arResults.toArray(new ContentProviderResult[arResults.size()]);
    }

    @VisibleForTesting
    static void commitMini(ContentResolver resolver, final String authority,
            final ArrayList<Operation> mini,
            ArrayList<ContentProviderResult> results, int offset) throws IOException {
        if (mini.isEmpty()) {
            return;// Empty lists are okay, we just ignore them
        }

        try {
            ArrayList<ContentProviderOperation> cpos = new ArrayList<ContentProviderOperation>();
            for (Operation op : mini) {
                cpos.add(op.operationToContentProviderOperation(offset));
            }

            ContentProviderResult[] miniResult = resolver.applyBatch(authority, cpos);
            results.addAll(Arrays.asList(miniResult));
            // System.arraycopy(miniResult, 0, result, offset,
            // miniResult.length);
        } catch (final RemoteException e) {
            LogUtils.e(TAG, "RemoteException in commitMini");
            throw new IOException("RemoteException in commitMini");
        } catch (final OperationApplicationException e) {
            // Not possible since we're building the ops ourselves
        }
    }
}
