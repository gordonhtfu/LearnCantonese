package com.blackberry.common.ui.list;

/**
 * Classes implementing this interface will be told when CAB is enabled or disabled when
 * user long press on a list item.  This will give a chance to the listener to react
 * and set specific behavior to the component based on CAB state.
 * 
 * @author dsutedja
 */
public interface ActionModeStateListener {

    /**
     * CAB has been enabled.
     */
    public void actionModeStarted();

    /**
     * CAB has been disabled.
     */
    public void actionModeEnded();
}
