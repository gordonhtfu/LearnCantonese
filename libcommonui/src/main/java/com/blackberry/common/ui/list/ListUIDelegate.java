
package com.blackberry.common.ui.list;

import android.app.LoaderManager;
import android.widget.BaseAdapter;

/**
 * Interface used by {@link ListFragment} to configure {@code ListView} and handle actions on items.
 */
public interface ListUIDelegate {

    /**
     * The types of list layouts ListFragment can use.
     */
    public enum ListLayoutType {
        LIST, GRID, STICKY_LIST, TREE,
    }

    /**
     * Get the list layout type.
     * 
     * @return ListLayoutType
     */
    ListLayoutType getListLayoutType();

    /**
     * Handle an item click event.
     * 
     * @param obj The Object that was clicked.
     */
    void listItemClick(Object obj);

    /**
     * Handle an item swipe event.
     * 
     * @param obj The Object that was swiped.
     */
    void listItemSwipe(Object obj);

    /**
     * Get the {@link BaseAdapter} that will be used by {@link ListFragment}.
     * 
     * @return BaseAdapter
     */
    BaseAdapter getAdapter();

    /**
     * Get the {@link ListUICabDelegate} that {@link ListFragment} will use when an item is long
     * pressed.
     * 
     * @return If there should be no menu on long press use {@link DefaultListUICabDelegate}.
     */
    ListUICabDelegate getCabDelegate();

    void refreshData(LoaderManager loaderManager, int mUniqueId);
}
