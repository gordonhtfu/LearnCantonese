
package com.blackberry.common.ui.list;

import android.view.Menu;
import android.view.MenuItem;

/**
 * A concrete implementation of {@link ListUICabDelegate} that does nothing.
 */
public class DefaultListUICabDelegate implements ListUICabDelegate {

    @Override
    public void setSelectedItem(Object obj) {
        // no op
    }

    @Override
    public boolean populateMenu(Menu menu) {
        // no op
        return false;
    }

    @Override
    public boolean onMenuItemClicked(MenuItem item) {
        // no op
        return false;
    }

}
