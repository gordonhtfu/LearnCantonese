package com.blackberry.widgets.tagview;

import android.content.Context;

/**
 * A specialized token used when collapsing a large list down to a smaller, more manageable number.
 */
public class MoreTag extends BaseTag {
    /**
     * The number of tags that were collapsed out of view
     */
    int mHowManyMore = 0;

    /**
     * @return The number of tags that were collapsed out of view
     */
    public int getHowManyMore() {
        return mHowManyMore;
    }

    /**
     * @param howManyMore The number of tags that were collapsed out of view
     */
    public void setHowManyMore(int howManyMore) {
        if (this.mHowManyMore != howManyMore) {
            this.mHowManyMore = howManyMore;
//            updateLabel();
        }
    }

    @Override
    protected String getLabel() {
        // TODO: i18n somehow...
        return mHowManyMore + " more...";
    }
}
