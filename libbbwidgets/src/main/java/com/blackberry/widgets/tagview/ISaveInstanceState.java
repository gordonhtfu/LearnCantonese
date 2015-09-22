package com.blackberry.widgets.tagview;

import android.os.Parcelable;

/**
 * An interface which specifies an object can save its state
 */
public interface ISaveInstanceState {
    /**
     * Called when the object should save its state
     *
     * @return A parcelable containing its save state
     */
    Parcelable onSaveInstanceState();

    /**
     * Called when the object should restore its state.
     *
     * @param state The state to restore
     */
    void onRestoreInstanceState(Parcelable state);
}
