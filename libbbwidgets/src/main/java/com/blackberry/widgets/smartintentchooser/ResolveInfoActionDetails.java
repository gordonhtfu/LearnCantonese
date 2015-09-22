
package com.blackberry.widgets.smartintentchooser;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

/**
 * An implementation of {@link ActionDetails} which wraps around a
 * {@link ResolveInfo} object.
 */
public class ResolveInfoActionDetails implements ActionDetails {
    private ResolveInfo mResolveInfo;
    private Intent mIntent = null;

    /**
     * @param resolveInfo The {@link ResolveInfo} to use for this action.
     */
    public ResolveInfoActionDetails(ResolveInfo resolveInfo) {
        mResolveInfo = resolveInfo;
    }

    /**
     * @param other The {@link ResolveInfoActionDetails} to copy.
     */
    public ResolveInfoActionDetails(ResolveInfoActionDetails other) {
        mResolveInfo = other.mResolveInfo;
        mIntent = null;
    }

    @Override
    public Intent getIntent(Intent originalIntent) {
        if (mIntent == null) {
            ComponentName chosenName = new ComponentName(
                    mResolveInfo.activityInfo.packageName,
                    mResolveInfo.activityInfo.name);

            mIntent = new Intent(originalIntent);
            mIntent.setComponent(chosenName);
        }
        return mIntent;
    }

    /**
     * @return The ResolveInfo this action is backed by.
     */
    public ResolveInfo getResolveInfo() {
        return mResolveInfo;
    }

    @Override
    public Drawable getIcon(Context context) {
        return mResolveInfo.loadIcon(context.getPackageManager());
    }

    @Override
    public Drawable getEmblem(Context context) {
        return null;
    }

    @Override
    public CharSequence getTitle(Context context) {
        return mResolveInfo.loadLabel(context.getPackageManager());
    }

    @Override
    public CharSequence getSubtitle(Context context) {
        return "";
    }
}
