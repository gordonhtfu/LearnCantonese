
package com.blackberry.pimbase.provider.utilities;

import android.content.ContentProviderOperation;
import android.content.ContentValues;

import java.util.Map;

/**
 * Operation is our binder-safe ContentProviderOperation (CPO) construct; an
 * Operation can be created from a CPO, a CPO Builder, or a CPO Builder with a
 * "back reference" column name and offset (that might be used in
 * Builder.withValueBackReference). The CPO is not actually built until it is
 * ready to be executed (with applyBatch); this allows us to recalculate back
 * reference offsets if we are required to re-send a large batch in smaller
 * chunks. NOTE: A failed binder transaction is something of an emergency case,
 * and shouldn't happen with any frequency. When it does, and we are forced to
 * re-send the data to the content provider in smaller chunks, we DO lose the
 * sync-window atomicity, and thereby add another small risk to the data. Of
 * course, this is far, far better than dropping the data on the floor, as was
 * done before the framework implemented TransactionTooLargeException Also the
 * reason for the wrapper like class is because we cannot extend
 * ContentProviderOperation
 */
public class Operation {

    // this should only be set if there are no backReferences!
    public final ContentProviderOperation mOp;
    // this is required for anything using backReferences that want the correct
    // index set
    public final ContentProviderOperation.Builder mBuilder;
    // really the index in the CPO result[] used with withValueBackReference
    // this param should not include any increases due to an added separator, as
    // this will skew result[] count as
    // the separator is removed when executing the applyBatch call
    // container n-backReferences ie. collection of columnName/backRefIndex
    public final ContentValues mValuesBackReferences;
    // indicates Operation is a marker used to trigger a chunked applyBatch
    public final boolean mSeparator;

    public Operation(ContentProviderOperation.Builder builder, String columnName, int backRefIndex) {
        mOp = null;
        mBuilder = builder;
        mValuesBackReferences = new ContentValues();
        mValuesBackReferences.put(columnName, backRefIndex);
        mSeparator = false;
    }

    public Operation(ContentProviderOperation.Builder builder, ContentValues backReferences) {
        mOp = null;
        mBuilder = builder;
        mValuesBackReferences = backReferences;
        mSeparator = false;
    }

    public Operation(ContentProviderOperation.Builder builder) {
        mOp = null;
        mBuilder = builder;
        mValuesBackReferences = null;
        mSeparator = false;
    }

    public Operation(ContentProviderOperation op) {
        mOp = op;
        mBuilder = null;
        mValuesBackReferences = null;
        mSeparator = false;
    }

    public Operation(ContentProviderOperation op, boolean separator) {
        mOp = op;
        mSeparator = separator;
        mValuesBackReferences = null;
        mBuilder = null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Operation: ");
        ContentProviderOperation op = operationToContentProviderOperation(0);
        if (op.isReadOperation()) {
            sb.append("READ");
        } else {
            sb.append("WRITE");
        }
        sb.append(op.getUri().getPath());
        if (mValuesBackReferences != null) {
            for (Map.Entry<String, Object> entry : mValuesBackReferences.valueSet()) {
                String key = entry.getKey();
                Integer backRefIndex = mValuesBackReferences.getAsInteger(key);
                sb.append(" Back value of " + key + ":" + backRefIndex);

            }

        }
        return sb.toString();
    }

    /**
     * Convert the Operation to a CPO; if the Operation has a back reference,
     * apply it with the passed-in offset
     * 
     * @param offset will be used to ensure that the withValueBackReference() is
     *            adjusted to point to the correct index. Since the bulk
     *            operations really know nothing about the separators as they
     *            are
     */
    public ContentProviderOperation operationToContentProviderOperation(int offset) {
        if (mOp != null) {
            return mOp;
        } else if (mBuilder == null) {
            throw new IllegalArgumentException("Operation must have CPO.Builder");
        }

        if (mValuesBackReferences != null) {
            for (Map.Entry<String, Object> entry : mValuesBackReferences.valueSet()) {
                String key = entry.getKey();
                Integer backRefIndex = mValuesBackReferences.getAsInteger(key);
                if (backRefIndex == null) {
                    throw new IllegalArgumentException("values backref " + key
                            + " is not an integer");
                }
                // now adjust
                mBuilder.withValueBackReference(key, backRefIndex.intValue() - offset);
            }
        }
        return mBuilder.build();
    }
}
