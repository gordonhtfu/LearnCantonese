
package com.blackberry.widgets.tagview;

/**
 * An interface used to notify registered listeners of changes to the tag list.
 * 
 * @param <T> The type of tag to notify
 */
public interface OnTagListChanged<T> {
    /**
     * A tag was added.
     * 
     * @param tag The tag that was added
     */
    void tagAdded(T tag);

    /**
     * A tag was removed.
     * 
     * @param tag The tag that was removed
     */
    void tagRemoved(T tag);

    /**
     * [NOT IMPLEMENTED YET] A tag was changed. For instance if the user edited
     * the tag or an alternate address was selected for a contact tag.
     * 
     * @param tag The tag that was changed.
     */
    void tagChanged(T tag);
}
