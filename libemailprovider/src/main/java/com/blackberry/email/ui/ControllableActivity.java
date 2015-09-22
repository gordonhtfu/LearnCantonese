/*******************************************************************************
 *      Copyright (C) 2012 Google Inc.
 *      Licensed to The Android Open Source Project.
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 *******************************************************************************/

package com.blackberry.email.ui;

import com.blackberry.email.Folder;
import com.blackberry.email.ui.AccountController;
import com.blackberry.email.ui.ConversationListCallbacks;
import com.blackberry.email.ui.FragmentLauncher;
import com.blackberry.email.ui.RestrictedActivity;
import com.blackberry.email.ui.UndoListener;
import com.blackberry.email.ui.UpOrBackController;
import com.blackberry.email.ui.ViewMode;

/**
 * A controllable activity is an Activity that has a Controller attached. This activity must be
 * able to attach the various view fragments and delegate the method calls between them.
 */
public interface ControllableActivity extends RestrictedActivity,
        UndoListener
        {
    /**
     * Returns the ViewMode the activity is updating.
     * @see com.blackberry.email.ui.ViewMode
     * @return ViewMode.
     */
    ViewMode getViewMode();

    /**
     * Returns the object that handles {@link ConversationListCallbacks} that is associated with
     * this activity.
     * @return
     */
    ConversationListCallbacks getListHandler();

    /**
     * Get the folder currently being accessed by the activity.
     */
    Folder getHierarchyFolder();

    /**
     * Returns an object that can update conversation state. Holding a reference to the
     * ConversationUpdater is safe since the ConversationUpdater is guaranteed to persist across
     * changes to the conversation cursor.
     * @return
     */
    ConversationUpdater getConversationUpdater();
 
    /**
     * Returns the {@link AccountController} object associated with this activity, if any.
     * @return
     */
    AccountController getAccountController();

    UpOrBackController getUpOrBackController();

    boolean isAccessibilityEnabled();

     /**
     * Returns the {@link FragmentLauncher} object associated with this activity, if any.
     */
    FragmentLauncher getFragmentLauncher();
}
