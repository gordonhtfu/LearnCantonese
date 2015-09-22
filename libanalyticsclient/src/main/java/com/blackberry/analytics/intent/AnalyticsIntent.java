
package com.blackberry.analytics.intent;

/**
 * Intent constants for analytics.
 */
public final class AnalyticsIntent {

    /**
     * Services that wish to be invoked to collect and persist intermediate
     * analytics data from source domain and behavioral data must include this
     * category in their intent-filter.
     */
    public static final String CATEGORY_ANALYTICS_PROCESSOR =
            "com.blackberry.intent.category.ANALYTICS_PROCESSOR";

    /**
     * The action used for broadcast messages recording component use data.
     */
    public static final String RECORD_ACTION = "com.blackberry.intent.action.RECORD_COMPONENT_USE";

    /**
     * Fictional intent action used to identify something that happened because
     * a message was sent.
     */
    public static final String ACTION_MESSAGE_TO = "com.blackberry.analytics.action.MESSAGE_TO";

    /**
     * Fictional intent action used to identify something that happened because
     * a message was received.
     */
    public static final String ACTION_MESSAGE_FROM = "com.blackberry.analytics.action.MESSAGE_FROM";


   
    private AnalyticsIntent() {
        // private constructor, not meant to be instantiated
    }
}
