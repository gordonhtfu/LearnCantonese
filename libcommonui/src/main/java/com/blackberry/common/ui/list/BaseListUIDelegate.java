
package com.blackberry.common.ui.list;

import android.app.LoaderManager;

/**
 * Common base class with common implementation for an {@link ListUIDelegate}.
 */
public abstract class BaseListUIDelegate implements ListUIDelegate {

    @Override
    public ListLayoutType getListLayoutType() {
        return ListLayoutType.LIST;
    }

    @Override
    public void listItemClick(Object obj) {

    }

    @Override
    public void listItemSwipe(Object obj) {

    }

    @Override
    public void refreshData(LoaderManager loaderManager, int mUniqueId) {

    }
}
