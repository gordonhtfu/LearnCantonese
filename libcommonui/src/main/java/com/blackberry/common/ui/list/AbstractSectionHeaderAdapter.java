
package com.blackberry.common.ui.list;

import android.widget.BaseAdapter;

/**
 * Base class for an adapter for handling header type {@link SectionItem}s.
 */
public abstract class AbstractSectionHeaderAdapter extends BaseAdapter {

    @Override
    public abstract SectionItem getItem(int position);

    /**
     * Adds the specified {@link SectionItem} at the end of the array.
     * 
     * @param item The SectionItem to add at the end of the array.
     */
    public abstract void add(SectionItem item);

    /**
     * Remove all elements from the list.
     */
    public abstract void clear();

    /**
     * Returns a new array containing all elements contained in this Adapter. The array returned
     * does not reflect any changes of the Adapter. A new array is created even if the underlying
     * data structure is already an array.
     * 
     * @return an array of the elements from this Adapter.
     */
    public abstract Object[] toArray();
}
