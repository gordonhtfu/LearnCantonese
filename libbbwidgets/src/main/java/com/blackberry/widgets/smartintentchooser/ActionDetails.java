
package com.blackberry.widgets.smartintentchooser;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;

/**
 * An interface used to specify the information required for an action.
 */
public interface ActionDetails {

    /**
     * Get the {@link Intent} which is to be launched for this Action. The
     * originalIntent is used to provide the majority of the {@link Intent}
     * information.
     * 
     * @param originalIntent The original {@link Intent} asked to be launched.
     * @return A new {@link Intent} to be launched.
     */
    Intent getIntent(Intent originalIntent);

    /**
     * @param context The context.
     * @return The background icon to use for this action. Can be null.
     */
    Drawable getIcon(Context context);

    /**
     * @param context The context.
     * @return The emblem to use for this action. Can be null.
     */
    Drawable getEmblem(Context context);

    /**
     * @param context The context.
     * @return The title to show for this action.
     */
    CharSequence getTitle(Context context);

    /**
     * @param context The context.
     * @return The subtitle to show for this action.
     */
    CharSequence getSubtitle(Context context);
}
