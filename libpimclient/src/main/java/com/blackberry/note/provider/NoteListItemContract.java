package com.blackberry.note.provider;

import android.net.Uri;

import com.blackberry.provider.ListItemContract;

/**
 * A contract to access notes list item.
 */
public final class NoteListItemContract {
    private NoteListItemContract() {
    }

    /**
     * URI at which list items for a notes profile may be accessed.
     */
    public static final Uri CONTENT_URI = Uri.parse("content://"
            + NoteContract.AUTHORITY + "/" + ListItemContract.URI_SUFFIX);

}
