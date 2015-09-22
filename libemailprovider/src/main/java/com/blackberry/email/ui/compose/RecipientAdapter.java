/**
 * Copyright (c) 2007, Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.blackberry.email.ui.compose;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Filter;

import com.android.ex.chips.BaseRecipientAdapter;
import com.android.ex.chips.RecipientEntry;
import com.blackberry.analytics.recent.RecentContactContract.RecentContact;
import com.blackberry.email.Account;

import java.util.ArrayList;
import java.util.List;

public class RecipientAdapter extends BaseRecipientAdapter {

    private static final String TAG = "BaseRecipientAdapter";

    private static final boolean DEBUG = false;

    public RecipientAdapter(Context context, Account account) {
        super(context);
        setAccount(account.getAccountManagerAccount());
    }

    @Override
    public Filter getFilter() {
        return new RecentContactFilter();
    }

    @Override
    protected void clearTempEntries() {
        super.clearTempEntries();
    }

    @Override
    protected void updateEntries(List<RecipientEntry> newEntries) {
        super.updateEntries(newEntries);
    }

    protected static class TemporaryEntry {
        public final String displayName;
        public final String destination;
        public final int destinationType;
        public final String destinationLabel;
        public final long contactId;
        public final long dataId;
        public final String thumbnailUriString;
        public final int displayNameSource;
        public final boolean isGalContact;

        public TemporaryEntry(Cursor cursor, boolean isGalContact) {
            this.displayName = cursor.getString(cursor
                    .getColumnIndex(RecentContact.DISPLAY_NAME));
            this.destination = cursor.getString(cursor
                    .getColumnIndex(RecentContact.ADDRESS));
            this.destinationType = cursor.getInt(cursor
                    .getColumnIndex(RecentContact.ADDRESS_TYPE));
            this.destinationLabel = cursor.getString(cursor.getColumnIndex(RecentContact.ADDRESS_TYPE_LABEL));
            this.contactId = cursor.getLong(cursor.getColumnIndex(RecentContact.CONTACT_ID));
            this.dataId = cursor.getLong(cursor.getColumnIndex(RecentContact.ADDRESS_ID));
            this.thumbnailUriString = cursor.getString(cursor
                    .getColumnIndex(RecentContact.PHOTO_THUMBNAIL_URI));
            this.displayNameSource = cursor.getInt(cursor
                    .getColumnIndex(RecentContact.DISPLAY_NAME_SOURCE));
            this.isGalContact = isGalContact;
        }
    }

    private final class RecentContactFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            if (DEBUG) {
                Log.d(TAG, "start filtering. constraint: " + constraint + ", thread:"
                        + Thread.currentThread());
            }

            final FilterResults results = new FilterResults();

            if (TextUtils.isEmpty(constraint)) {
                clearTempEntries();
                // Return empty results.
                return results;
            }

            Cursor cur = null;
            try {
                ContentResolver cr = getContext().getContentResolver();
                String[] PROJECTION = new String[] {
                        RecentContact.DISPLAY_NAME,
                        RecentContact.DISPLAY_NAME_SOURCE,
                        RecentContact.ADDRESS,
                        RecentContact.ADDRESS_CATEGORY,
                        RecentContact.ADDRESS_TYPE,
                        RecentContact.ADDRESS_TYPE_LABEL,
                        RecentContact.PHOTO_THUMBNAIL_URI,
                        RecentContact.CONTACT_LOOKUP_KEY,
                        RecentContact.CONTACT_ID,
                        RecentContact.ADDRESS_ID
                };
                Uri filterUri = Uri.withAppendedPath( RecentContact.RECENT_EMAIL_FILTER_URI, constraint.toString());
                cur = cr.query(filterUri, PROJECTION, null, null, null);


                if (cur == null || cur.getCount() < 0) {
                    if (DEBUG) {
                        Log.w(TAG, "null cursor returned for default Email filter query.");
                    }
                } else {
                    final List<RecipientEntry> entryList = new ArrayList<RecipientEntry>();
                    while (cur.moveToNext()) {
                        TemporaryEntry entry = new TemporaryEntry(cur, false);

                        if (entry.contactId < 1) {
                            // this entry doesn't resolve to a real contact
                            entryList.add(RecipientEntry.constructGeneratedEntry(entry.displayName,
                                    entry.destination, true));

                        } else {
                            entryList.add(RecipientEntry.constructTopLevelEntry(
                                    entry.displayName,
                                    entry.displayNameSource,
                                    entry.destination,
                                    entry.destinationType,
                                    entry.destinationLabel,
                                    entry.contactId,
                                    entry.dataId,
                                    entry.thumbnailUriString,
                                    true,
                                    entry.isGalContact));
                        }

                    }
                    results.values = entryList;
                    results.count = 1;
                }
            }catch (SecurityException ex) {
            } finally {
                if (cur != null) {
                    cur.close();
                }
            }

            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(final CharSequence constraint, FilterResults results) {
            // If a user types a string very quickly and database is slow, "constraint" refers to
            // an older text which shows inconsistent results for users obsolete (b/4998713).
            // TODO: Fix it.
            setCurrentConstraint(constraint);
            clearTempEntries();

            if (results.values != null) {

                List<RecipientEntry> entries = (List<RecipientEntry>) results.values;
                updateEntries(entries);
            }

        }
    }
}
