package com.blackberry.email.provider;

import android.os.Bundle;

public class RequestSyncParameters {

    // NOTE: These keys MUST be unique
    public final static String FETCH_MESSAGE = "__FETCH_MESSAGE_BODY__";
    public final static String MAILBOX_SYNC_ID = "__MAILBOX_SYNC_ID__";
    public final static String MAILBOX_SYNC_KEY = "__MAILBOX_SYNC_KEY__";
    public final static String MESSAGE_SYNC_ID = "__MESSAGE_SYNC_ID__";
    public final static String MESSAGE_ID = "__MESSAGE_ID__";


    public static Bundle createDefaultExtras() {
        return new Bundle();
    }

    public static boolean isBodyFetchRequest(Bundle extras) {
        return extras.getBoolean(FETCH_MESSAGE, false);
    }

}
