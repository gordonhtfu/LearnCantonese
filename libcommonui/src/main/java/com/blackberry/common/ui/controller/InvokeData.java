
package com.blackberry.common.ui.controller;

public class InvokeData {
    public final String mUri;
    public final int mTemplateId;
    public final int mViewId;

    public InvokeData(String uri, int templateId, int viewId) {
        // Going forward when ListItemContract is solidified, the URI will be constructed
        // before firing off the invoke with the proper accountId, mime-type, sourceId, and itemId
        mUri = uri;
        mTemplateId = templateId;
        mViewId = viewId;
    }
}
