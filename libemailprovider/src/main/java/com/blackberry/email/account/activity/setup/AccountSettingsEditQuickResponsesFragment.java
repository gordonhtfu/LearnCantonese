/*
 * Copyright (C) 2011 The Android Open Source Project
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
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.blackberry.email.utils.UiUtilities;
import com.blackberry.lib.emailprovider.R;
import com.blackberry.email.Account;
import com.blackberry.email.provider.UIProvider;
import com.blackberry.email.utils.Utility;

/**
 * Lists quick responses associated with the specified email account. Allows users to create,
 * edit, and delete quick responses. Owning activity must:
 * <ul>
 *   <li>Launch this fragment using startPreferencePanel().</li>
 *   <li>Provide an Account as an argument named "account". This account's quick responses
 *   will be read and potentially modified.</li>
 * </ul>
 *
 * <p>This fragment is run as a preference panel from AccountSettings.</p>
 */
public class AccountSettingsEditQuickResponsesFragment extends Fragment {
    private Account mAccount;

    private static final String BUNDLE_KEY_ACTIVITY_TITLE
            = "AccountSettingsEditQuickResponsesFragment.title";

    // Public no-args constructor needed for fragment re-instantiation
    public AccountSettingsEditQuickResponsesFragment() {}

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // startPreferencePanel launches this fragment with the right title initially, but
        // if the device is rotated we must set the title ourselves
        if (savedInstanceState != null) {
            getActivity().setTitle(savedInstanceState.getString(BUNDLE_KEY_ACTIVITY_TITLE));
        }

        final SimpleCursorAdapter adapter = new SimpleCursorAdapter(getActivity(),
                R.layout.quick_response_item, null,
                new String[] {UIProvider.QuickResponseColumns.TEXT},
                new int[] {R.id.quick_response_text}, 0);

        final ListView listView = UiUtilities.getView(getView(),
                R.id.account_settings_quick_responses_list);
        listView.setAdapter(adapter);

        getLoaderManager().initLoader(0, null, new LoaderManager.LoaderCallbacks<Cursor>() {
            @Override
            public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                return new CursorLoader(getActivity(), mAccount.quickResponseUri,
                        UIProvider.QUICK_RESPONSE_PROJECTION, null, null, null);
            }

            @Override
            public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
                adapter.swapCursor(data);
            }

            @Override
            public void onLoaderReset(Loader<Cursor> loader) {
                adapter.swapCursor(null);
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(BUNDLE_KEY_ACTIVITY_TITLE, (String) getActivity().getTitle());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        mAccount = args.getParcelable("account");

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.account_settings_edit_quick_responses_fragment,
                container, false);

        final ListView listView = UiUtilities.getView(view,
                R.id.account_settings_quick_responses_list);
        final TextView emptyView =
                UiUtilities.getView((ViewGroup) listView.getParent(), R.id.empty_view);
        listView.setEmptyView(emptyView);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final Cursor c = (Cursor) listView.getItemAtPosition(position);
                final String quickResponseText =
                        c.getString(c.getColumnIndex(UIProvider.QuickResponseColumns.TEXT));
                final Uri uri = Utility.getValidUri(
                        c.getString(c.getColumnIndex(UIProvider.QuickResponseColumns.URI)));
                EditQuickResponseDialog.newInstance(quickResponseText, uri, false)
                        .show(getFragmentManager(), null);
            }
        });
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.quick_response_prefs_fragment_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.create_new) {
            EditQuickResponseDialog.newInstance(null, mAccount.quickResponseUri, true)
                    .show(getFragmentManager(), null);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}