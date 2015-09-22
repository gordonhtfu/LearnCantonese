package com.blackberry.widgets.tagview;

/**
 * An interface specifying the object can be read-only or not
 */
public interface IReadOnly {
    /**
     * @return Whether or not the object is read-only
     */
    boolean isReadOnly();

    /**
     * @param readOnly The read-only state to set
     */
    void setReadOnly(boolean readOnly);
}
