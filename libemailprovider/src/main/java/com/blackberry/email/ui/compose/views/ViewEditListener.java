package com.blackberry.email.ui.compose.views;

/**
 * A generic callback interface to listen to any and all changes on
 * a given view. This is used to tie all 'view dirty' changes to one observer. 
 * @author rratan
 *
 */
public interface ViewEditListener {
    public void setDirty(boolean isDirty);
}
