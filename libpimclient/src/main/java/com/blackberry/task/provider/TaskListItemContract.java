package com.blackberry.task.provider;

import android.net.Uri;

import com.blackberry.provider.ListItemContract;

/**
 * A contract to access tasks list item.
 */
public final class TaskListItemContract {
    private TaskListItemContract() {
    }

    /**
     * URI at which list items for a tasks profile may be accessed.
     */
    public static final Uri CONTENT_URI = Uri.parse("content://"
            + TaskContract.AUTHORITY + "/" + ListItemContract.URI_SUFFIX);

    /**
     * Bit flags representing list item states.
     *
     * TODO: May need to change this once ListItemStates is integrated into master branch.
     */
    public final class TasksListItemStates {
        private TasksListItemStates() {
        }

        public static final int COMPLETE        = 1 << 0;
        public static final int REMINDER_SET    = 1 << 1;
        public static final int HIGH_IMPORTANCE = 1 << 2;
        public static final int LOW_IMPORTANCE  = 1 << 3;
        public static final int RECURRENCE_SET  = 1 << 4;
    }
}
