package com.blackberry.widgets.tagview;

/**
 * An interface which determines the object can be soft focusable
 */
public interface ISoftFocusable {
    /**
     * @return Whether or not the item is soft focusable
     */
    boolean isSoftFocused();

    /**
     * @param isSoftFocused The soft focusable state to set
     */
    void setSoftFocus(boolean isSoftFocused);
}
