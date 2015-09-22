
package com.blackberry.task.provider;

import android.net.Uri;
import android.provider.BaseColumns;

import com.blackberry.provider.SyncContract;

/**
 * A contract to access tasks content provider.
 */
public final class TaskContract {
    private TaskContract() {
    }

    /**
     * This authority is used for writing to or querying from the task provider.
     * Note: This is set at first run and cannot be changed without breaking
     * apps that access the provider.
     */
    public static final String AUTHORITY = "com.blackberry.task.provider";

    /**
     * The path segment that follows the AUTHORITY, and precedes a task item ID.
     */
    public static final String URI_SUFFIX = "task";

    /**
     * The content:// style URL for the top-level task authority.
     */
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    /**
     * The content:// style URL prefix for task items. The task ID should be
     * appended to this URL to identify a particular task.
     */
    public static final Uri CONTENT_URI_WITH_SUFFIX = Uri.parse("content://"
            + TaskContract.AUTHORITY + "/" + URI_SUFFIX);

    public static final String CREATE_TASK_ACTION = "com.blackberry.task.CREATE_TASK";
    public static final String EDIT_TASK_ACTION = "com.blackberry.task.EDIT_TASK";
    public static final String REMOVE_TASK_ACTION = "com.blackberry.task.REMOVE_TASK";

    /**
     * Task projection for the ID column only.
     */
    public static final String[] PROJECTION_ID = new String[] {
            TaskColumns._ID
    };

    /**
     * Task projection of all columns.
     */
    public static final String[] PROJECTION_ALL = new String[] {
            TaskColumns._ID, // 0
            TaskColumns.SERVER_ID, // 1
            TaskColumns.SERVER_TIMESTAMP, // 2
            TaskColumns.DIRTY, // 3
            TaskColumns.DELETED, // 4
            TaskColumns.ACCOUNT_KEY, // 5
            TaskColumns.MAILBOX_KEY, // 6
            TaskColumns.SUBJECT, // 7
            TaskColumns.IMPORTANCE, // 8
            TaskColumns.UTC_START_DATE, // 9
            TaskColumns.START_DATE, // 10
            TaskColumns.UTC_DUE_DATE, // 11
            TaskColumns.DUE_DATE, // 12
            TaskColumns.COMPLETE, // 13
            TaskColumns.COMPLETED_DATE, // 14
            TaskColumns.SENSITIVITY, // 15
            TaskColumns.REMINDER_SET, // 16
            TaskColumns.REMINDER_DATE, // 17
            TaskColumns.BODY
    // 18
    };

    /**
     * Inner static class which contains the task priority constants.
     */
    public static final class Priority {
        public static final int LOW = 0;
        public static final int NORMAL = 1;
        public static final int HIGH = 2;
    }

    public interface TaskColumns extends BaseColumns, SyncContract.SyncColumns {
        // Foreign key to the Account holding this task
        public static final String ACCOUNT_KEY = "accountKey";
        // Foreign key to the Mailbox holding this task
        public static final String MAILBOX_KEY = "mailboxKey";
        // The Subject of the task
        public static final String SUBJECT = "subject";
        // The Importance for the task
        public static final String IMPORTANCE = "importance";
        // The UtcStartDate of the task
        public static final String UTC_START_DATE = "utc_start_date";
        // The StartDate of the task
        public static final String START_DATE = "start_date";
        // The UtcDueDate of the task
        public static final String UTC_DUE_DATE = "utc_due_date";
        // The DueDate of the task
        public static final String DUE_DATE = "due_date";
        // The Complete state of the task
        public static final String COMPLETE = "complete";
        // The CompletedDate of the task
        public static final String COMPLETED_DATE = "completed_date";
        // The Sensitivity state of the task
        public static final String SENSITIVITY = "sensitivity";
        // The Reminder state of the task
        public static final String REMINDER_SET = "reminder_set";
        // The Reminder date of the task
        public static final String REMINDER_DATE = "reminder_date";
        // The Body of the task
        public static final String BODY = "body";
    }
}
