
package com.blackberry.widgets.smartintentchooser;

import android.content.Intent;

import java.util.List;

/**
 * An interface used to provide a way to augment a list of {@link ActionDetail}
 * objects by adding, removing, re-ordering, or leaving be the input list.
 */
public interface ActionFilter {
    /**
     * @param input The list of actions to filter.
     * @param originalIntent The original intent which created the list of
     *            actions to filter.
     * @return A list of {@link ActionDetails} to be displayed. This list may
     *         contain more items, less items, new items, or a reordered list of
     *         items. There are no constraints other than do not return null.
     */
    List<ActionDetails> filterActions(List<ActionDetails> input, Intent originalIntent);
}
