
package com.blackberry.provider;

import android.net.Uri;


/**
 * The contract between unified list provider and applications.
 */
public class UnifiedListContract extends ListItemContract {

    /**
     * Authority of the unified list provider.
     */
    public static final String AUTHORITY = "com.blackberry.unified.provider";

    /**
     * Notification authority of the unified list provider.
     */
    public static final String NOTIFY_AUTHORITY = "com.blackberry.unified.notifier";

    /**
     * URI at which list items for the device may be accessed.
     */
    public static final Uri LISTITEM_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/"
            + ListItemContract.URI_SUFFIX);

}
