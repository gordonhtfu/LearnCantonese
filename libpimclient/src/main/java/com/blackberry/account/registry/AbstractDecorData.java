package com.blackberry.account.registry;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;

/**
 * Base class for any decoration data object.
 * 
 * @author dsutedja
 */
public abstract class AbstractDecorData {
    protected Context mContext;
    protected long mAccountId;
    protected long mItemState;
    protected String mMimeType;
    protected int mTemplateId;
    protected int mElementPosition;
    protected int mElementType;

    protected AbstractDecorData(Context context, long accountId, String mimeType,
            ListItemDecor.Type templateType) {
        mContext = context;
        mAccountId = accountId;
        mMimeType = mimeType;
        mElementType = templateType.toInt();
    }


    /**
     * Set the position (in the defined template) where this icon will be shown.
     * 
     * @param position -- the position in the template
     * @return this
     */
    public AbstractDecorData setElementPosition(int position) {
        mElementPosition = position;
        return this;
    }

    /**
     * Get the template position for this icon.
     * 
     * @return the template position
     */
    public int elementPosition() {
        return mElementPosition;
    }

    /**
     * Set the item state that this icon is mapped to.
     * 
     * @param state -- the item state defined in ListItemStates. Bit combo is allowed.
     * @return this
     */
    public AbstractDecorData setItemState(long state) {
        mItemState = state;
        return this;
    }

    /**
     * Get the item state that this icon is mapped to.
     * 
     * @return the item state
     */
    public long itemState() {
        return mItemState;
    }

    /**
     * Set the template ID where this icon will be shown in.
     * 
     * @param id -- the template ID
     * @return this
     */
    public AbstractDecorData setTemplateId(int id) {
        mTemplateId = id;
        return this;
    }

    /**
     * Get the template ID where this icon will be shown in.
     * 
     * @return the template ID
     */
    public int templateId() {
        return mTemplateId;
    }

    /**
     * Set the account ID that owns the data that this icon is registered with.
     * 
     * @param id -- the account ID
     * @return this
     */
    public AbstractDecorData setAccountId(int id) {
        mAccountId = id;
        return this;
    }

    /**
     * Get the account ID that owns the data that this icon is registered with.
     * 
     * @return the account ID
     */
    public long accountId() {
        return mAccountId;
    }

    /**
     * Convert this text style definition into a ContentProviderOperation insertion command.
     * 
     * @return instance of ContentProviderOperation.
     */
    public ContentProviderOperation toContentProviderOperation() {
        return ContentProviderOperation.newInsert(MimetypeRegistryContract.DecorMapping.CONTENT_URI)
                .withValues(createPackage()).build();
    }

    private ContentValues createPackage() {
        ContentValues values = new ContentValues();

        // the following data cannot be null, let's validate
        validateBaseData();
        values.put(MimetypeRegistryContract.DecorMapping.ACCOUNT_KEY, mAccountId);
        values.put(MimetypeRegistryContract.DecorMapping.MIME_TYPE, mMimeType);
        values.put(MimetypeRegistryContract.DecorMapping.TEMPLATE_ID, mTemplateId);
        values.put(MimetypeRegistryContract.DecorMapping.ELEMENT_TYPE,
                String.valueOf(mElementType));
        values.put(MimetypeRegistryContract.DecorMapping.ITEM_STATE, mItemState);
        values.put(MimetypeRegistryContract.DecorMapping.ELEMENT_POSITION,
                mElementPosition);

        insertExtraData(values);

        return values;
    }

    private void validateBaseData() {
        if (mAccountId == -1) {
            throw new IllegalArgumentException("Invalid account ID");
        }
        if (mMimeType == null || mMimeType.isEmpty()) {
            throw new IllegalArgumentException("Invalid mime type");
        }
        if (mTemplateId == -1) {
            throw new IllegalArgumentException("Invalid template ID");
        }
        if (mElementType == -1) {
            throw new IllegalArgumentException("Invalid template type");
        }
        if (mItemState == -1) {
            throw new IllegalArgumentException("Invalid item state");
        }
        if (mElementPosition == -1) {
            throw new IllegalArgumentException("Invalid template position");
        }
    }

    protected abstract void insertExtraData(ContentValues values);
}
