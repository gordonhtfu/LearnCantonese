package com.blackberry.account.registry;


import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;

/**
 * Registered icon data.
 * 
 * Note: calling drawable() method will reach out to the assigned app (through package manager),
 * and retrieve the icons.
 * 
 * @author dsutedja
 * @version 1.0
 */
public class IconData extends AbstractDecorData {

    private int mIconResId;
    private String mPackageName;
    private Drawable mDrawable;

    IconData(Context context, long accountId, String packageName, String mimeType) {
        super(context, accountId, mimeType, ListItemDecor.Type.Icon);
        mPackageName = packageName;
        mIconResId = -1;
        mTemplateId = -1;
        mItemState = -1;
    }

    /**
     * Set the resource ID for this icon.
     * 
     * @param id -- the drawable resource ID (should be accessible from the defined package name)
     * @return this
     */
    public IconData setResourceId(int id) {
        mIconResId = id;
        return this;
    }

    /**
     * Get the resource ID for this icon.
     * 
     * @return the resource ID
     */
    public int resourceId() {
        return mIconResId;
    }

    /**
     * Set the package name where this icon drawable resides in.
     * 
     * @param packageName -- the packageName
     * @return this
     */
    public IconData setPackageName(String packageName) {
        mPackageName = packageName;
        return this;
    }

    /**
     * Get the packagename where this icon drawable resides in.
     * 
     * @return the packageName.
     */
    public String packageName() {
        return mPackageName;
    }

    /**
     * Get the real drawable defined by this icon meta data.
     * If haven't processed, we will use the PackageManager and obtain the reference to the
     * drawable.  Calling this method multiple times, will not go to the package manager
     * multiple times.
     * 
     * @return the drawable.
     */
    public Drawable drawable() {
        if (!mPackageName.isEmpty() && mIconResId != -1 && mDrawable == null) {
            try {
                final PackageManager pm = mContext.getPackageManager();
                ApplicationInfo applicationInfo = pm.getApplicationInfo(mPackageName,
                        PackageManager.GET_META_DATA);
                mDrawable = pm.getDrawable(mPackageName, mIconResId, applicationInfo);
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        return mDrawable;
    }

    @Override
    protected void insertExtraData(ContentValues values) {
        if (mPackageName == null || mPackageName.isEmpty()) {
            throw new IllegalArgumentException("Package name cannot be null");
        }
        if (mIconResId == -1) {
            throw new IllegalArgumentException("Invalid resource ID");
        }
        values.put(MimetypeRegistryContract.DecorMapping.PACKAGE_NAME, mPackageName);
        values.put(MimetypeRegistryContract.DecorMapping.ELEMENT_RESOURCE_ID, mIconResId);
    }

    // overrides so the fluent interface still works

    @Override
    public IconData setAccountId(int id) {
        super.setAccountId(id);
        return this;
    }

    @Override
    public IconData setItemState(long state) {
        super.setItemState(state);
        return this;
    }

    @Override
    public IconData setTemplateId(int id) {
        super.setTemplateId(id);
        return this;
    }

    @Override
    public IconData setElementPosition(int position) {
        super.setElementPosition(position);
        return this;
    }

}
