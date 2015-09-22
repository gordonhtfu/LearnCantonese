
package com.blackberry.common.ui.contenteditor;

import java.util.ArrayList;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.blackberry.common.utils.LogUtils;


/**
 * An abstract fragment that may be used to display an editor screen for an item loaded from a
 * ContentProvider.
 * <p>
 * This fragment facilitates loading and saving data for the item being displayed.
 * <p>
 * Subclasses must implement {@link #onCreateBindings(View view)} in order to bind data from the
 * data source to the UI. For example:
 * 
 * <pre>
 * public void onCreateBindings(View root) {
 *     addBinding(new TextViewContentBinding("text_key", root.findViewById(R.id.textView)));
 *     addBinding(new MyContentBinding("data_key", root.findViewById(R.id.customView)));
 *     ...
 * }
 * </pre>
 * 
 * To save the data back to the source ContentProvider, call {@link #save}. Depending on the desired
 * UX, this could be called from the containing Activity's {@link Activity#onPause()} for continuous
 * persistence, or after displaying a save/discard prompt when closing the activity, etc.
 * <p>
 * To edit a particular item, the item ID string should be passed to the Fragment in the fragment
 * arguments, using the {@link #ARG_ITEM_ID} key, e.g.:
 * 
 * <pre>
 * Bundle args = new Bundle();
 * args.putLong(ContentEditorFragment.ARG_ITEM_ID, itemId);
 * MyContentEditor fragment = new MyContentEditor();
 * fragment.setArguments(args);
 * 
 * </pre>
 * 
 * If no item id is set, the editor will create a new item when the editor is saved.
 */
public abstract class ContentEditorFragment extends Fragment implements
LoaderManager.LoaderCallbacks<Cursor>, View.OnClickListener {

    private static final String LOG_TAG = "ContentEditor";

    /**
     * The fragment argument representing the ID of the item to be displayed.
     * This ID will be appended to the ContentProvder URI provided at
     * construction.
     */
    public static final String ARG_ITEM_ID = "item_id";

    private static final int LOADER_ID = 0;

    private LinearLayout mRootLayout;
    private View mContentView;
    private TextView mNoContentView;
    private final int mLayoutId;

    private final int mNoContentId;
    private final String[] mProjection;

    private final Uri mAddUri;
    private final Uri mItemUri;

    private Long mItemId = null;

    /**
     * List of fields in this fragment that are based on properties in the
     * ContentProvider. These wrappers allow generic reading/ writing of data to
     * the Cursor/CP.
     */
    private final ArrayList<ContentBinding<?>> mFields = new ArrayList<ContentBinding<?>>();

    /**
     * @param layoutId Resource ID for the layout to inflate in this fragment.
     * @param noContentId Resource ID for the string to display if the specified
     *            item cannot be found in the ContentProvider.
     * @param addUri Content URI to use to insert a new item into a
     *            ContentProvider.
     * @param itemUri Content URI prefix to use to update an existing item in a
     *            ContentProvider.
     * @param projection The columns to use when loading data from the
     *            ContentProvider.
     */
    public ContentEditorFragment(int layoutId, int noContentId, Uri addUri, Uri itemUri,
            String[] projection) {
        super();
        mProjection = projection;
        mAddUri = addUri;
        mItemUri = itemUri;
        mLayoutId = layoutId;
        mNoContentId = noContentId;
    }

    /**
     * Create the bindings that will allow data to be loaded and saved using the
     * URI provided at construction.
     *
     * @param view The root view for this editor fragment, which contains the
     *            inflated layout. Individual views from the layout may be
     *            retrieved by ID from this root.
     */
    protected abstract void onCreateBindings(View root);

    /**
     * Add a binding to the list of bindings. The binding will be used to load
     * and save data to a view.
     *
     * @param binding The binding to add.
     */
    protected void addBinding(ContentBinding<?> binding) {
        mFields.add(binding);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mItemId != null) {
            outState.putLong(ARG_ITEM_ID, mItemId);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        LogUtils.d(LOG_TAG, "ContentEditorFragment.onCreate");

        super.onCreate(savedInstanceState);

        final Bundle args = getArguments();

        // Fetch the item id out of either the saved state, or the initial arguments
        if (savedInstanceState != null && savedInstanceState.containsKey(ARG_ITEM_ID)) {
            mItemId = savedInstanceState.getLong(ARG_ITEM_ID);
        } else if (args.containsKey(ARG_ITEM_ID)) {
            mItemId = args.getLong(ARG_ITEM_ID);
        }

        // If no item id has been set, then we are in 'create' mode.
        // Don't try to load any data.
        if (mItemId != null) {
            getLoaderManager().initLoader(LOADER_ID, args, this);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        final Activity activity = getActivity();

        mRootLayout = new LinearLayout(activity);
        mRootLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

        container.addView(mRootLayout);

        // This is the main content view, based on the layout provided at
        // construction
        mContentView = inflater.inflate(mLayoutId, mRootLayout, false);

        // The subclass must bind data to the view.
        onCreateBindings(mContentView);

        return mContentView;
    }

    @Override
    public CursorLoader onCreateLoader(int id, Bundle args) {
        LogUtils.d(LOG_TAG, "ContentEditorFragment.onCreateLoader");

        if (!args.containsKey(ARG_ITEM_ID)) {
            return null;
        }

        Uri uri = ContentUris.withAppendedId(mItemUri, args.getLong(ARG_ITEM_ID));
        return new CursorLoader(getActivity(), uri, mProjection, null, new String[] {}, null);
    }

    @Override
    public void onClick(View v) {
        LogUtils.d(LOG_TAG, "ContentEditorFragment.onClick");
        if (!mContentView.hasWindowFocus()) {
            // Don't do anything if the activity if paused. Since Activity
            // doesn't have a built in way to do this, we would have to
            // implement one ourselves and either cast our Activity to a
            // specialized activity base class or implement some generic
            // interface that tells us if an activity is paused.
            // hasWindowFocus() is close enough if not quite perfect.
            return;
        }
    }

    /**
     * Create and display a view to indicate that the selected ID is invalid,
     * for example: "The selected item does not exist". The resource id for the
     * message is provided by the subclass.
     */
    private void showNoContentView() {
        final Activity activity = getActivity();
        if (mNoContentView == null) {
            // This is the "no content" view, used when a bad URI or
            // catastrophic error prevents data from loading.
            mNoContentView = new TextView(activity);
            mNoContentView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT));
            mNoContentView.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
            mNoContentView.setText(getString(mNoContentId));
            mRootLayout.addView(mNoContentView);
        }

        mContentView.setVisibility(View.GONE);
        mNoContentView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor result) {
        LogUtils.d(LOG_TAG, "ContentEditorFragment.onLoadFinished");

        if (result == null || !result.moveToFirst()) {
            showNoContentView();
            return;
        }

        for (ContentBinding<?> f : mFields) {
            f.loadFrom(result);
        }

        mContentView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    /**
     * Update the current item in its ContentProvider with the current values from the UI.
     * 
     * @param id The item id.
     */
    private void updateItem(long id) {
        ContentValues updateValues = new ContentValues();

        for (ContentBinding<?> f : mFields) {
            f.saveDirtyData(updateValues);
        }

        if (updateValues.size() > 0) {
            Uri uri = ContentUris.withAppendedId(mItemUri, id);
            int rowsUpdated = getActivity().getContentResolver().update(uri, updateValues,
                    null, null);
            if (rowsUpdated != 1) {
                LogUtils.e(LOG_TAG, "update failed");
            }
        }
    }

    /**
     * Create a new item in the ContentProvider based on the current values in the UI.
     */
    private void createItem() {
        ContentValues updateValues = new ContentValues();

        // We have no valid id, this is an 'add'
        for (ContentBinding<?> f : mFields) {
            // Save all values, dirty or not, so that default values
            // populated in the UI are also persisted.
            f.saveData(updateValues);
        }

        if (updateValues.size() > 0) {
            Uri newItemUri = getActivity().getContentResolver().insert(mAddUri, updateValues);
            if (newItemUri == null) {
                LogUtils.e(LOG_TAG, "add failed");
            } else {
                try {
                    mItemId = Long.valueOf(newItemUri.getLastPathSegment());
                } catch (NumberFormatException e) {
                    LogUtils.e(LOG_TAG, "invalid insert item id:" + newItemUri);
                }
            }
        }

    }

    /**
     * Save current data in editor. If this is a new item, it will be inserted
     * into the ContentProvider. Otherwise, only the modified values will be
     * updated in the ContentProvider.
     */
    public void save() {

        if (mNoContentView != null) {
            // Don't try to save anything if we couldn't load the item.
            return;
        }

        if (mItemId != null) {
            // We have a valid id, this is an 'update'
            updateItem(mItemId);

        } else {
            // No valid id, this is a 'create'
            boolean dirty = false;
            for (ContentBinding<?> f : mFields) {
                if (f.isDirty()) {
                    dirty = true;
                    break;
                }
            }

            // If the user didn't modify anything, don't create the new task.
            if (!dirty) {
                return;
            }

            // Create a new item from the current values in the UI
            createItem();
        }
    }

}
