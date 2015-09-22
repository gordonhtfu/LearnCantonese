
package com.blackberry.common.ui.list;

import android.app.LoaderManager.LoaderCallbacks;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.CursorAdapter;

import com.google.android.mail.common.base.Preconditions;

/**
 * A class that implements {@link LoaderCallbacks} for Cursor. When the onLoadFinished is called the
 * new data will be passed to the CursorAdapter. onLoaderReset will pass null to the CursorAdapter.
 * Uses the CursorLoaderFactory to create loaders.
 */
public class CursorLoaderCallbacks implements LoaderCallbacks<Cursor> {
    private CursorAdapter mAdapter;
    private CursorLoaderFactory mFactory;

    /**
     * Constructor.
     * 
     * @param adapter The CursorAdapter
     * @param factory The CursorLoaderFactory
     */
    public CursorLoaderCallbacks(CursorAdapter adapter, CursorLoaderFactory factory) {
        Preconditions.checkNotNull(adapter, "CursorAdapter is null");
        Preconditions.checkNotNull(factory, "CursorLoaderFactory is null");
        mAdapter = adapter;
        mFactory = factory;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return mFactory.createLoader(id, args);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }
}
