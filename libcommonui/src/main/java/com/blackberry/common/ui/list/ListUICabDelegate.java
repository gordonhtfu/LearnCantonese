
package com.blackberry.common.ui.list;

import android.view.Menu;
import android.view.MenuItem;

/**
 * Interface used by the {@link ListFragment} for handling item long press menus.
 */
public interface ListUICabDelegate {

    /**
     * Set the selected object.
     * 
     * @param obj The selected object
     */
    // TODO: What about multi-select?
    void setSelectedItem(Object obj);

    /**
     * Populate the given menu with menu items applicable to the selected item.
     * 
     * @param menu
     * @return false if failed
     */
    boolean populateMenu(Menu menu);

    /**
     * Perform action associated to the given menu item.
     * 
     * @param item
     * @return false if failed
     */
    boolean onMenuItemClicked(MenuItem item);

}
