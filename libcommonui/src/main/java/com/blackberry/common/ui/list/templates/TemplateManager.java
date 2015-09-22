
package com.blackberry.common.ui.list.templates;

import com.blackberry.account.registry.MimetypeRegistryContract;
import com.blackberry.datagraph.provider.DataGraphContract;
import com.blackberry.provider.ListItemContract;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.util.SparseArray;

import java.util.HashMap;

public class TemplateManager {

    private static final String TAG = "TemplateManager";

    private SparseArray<BaseListItemHandler> mItemHandlers;
    private HashMap<String, Integer> mTemplateMappingCache;

    private Context mContext;

    public TemplateManager(Context context) {
        mContext = context;
        mItemHandlers = new SparseArray<BaseListItemHandler>();
        mTemplateMappingCache = new HashMap<String, Integer>();

        BaseListItemHandler itemHandler = new StandardListItemHandler(context);
        mItemHandlers.put(MimetypeRegistryContract.TemplateMapping.StandardItem, itemHandler);

        itemHandler = new ExpandableListItemHandler(context);
        mItemHandlers.put(MimetypeRegistryContract.TemplateMapping.ExpandableItem, itemHandler);

        itemHandler = new TwoIconListItemHandler(context);
        mItemHandlers.put(MimetypeRegistryContract.TemplateMapping.TwoIconItem, itemHandler);
    }

    public BaseListItemHandler getItemHandler(Cursor c) {
        Integer templateId = 0;
        String mimeType = "";
        String accountId = "";
        try {
            mimeType = c.getString(c.getColumnIndex(DataGraphContract.EntityColumns.MIME_TYPE));
            accountId = c.getString(c.getColumnIndex(DataGraphContract.EntityColumns.ACCOUNT_ID));
            String lookupKey = getTemplateLookupKey(accountId, mimeType);
            templateId = mTemplateMappingCache.get(lookupKey);
            if (templateId == null) {
                // First lookup for this mapping, so fetch from Content Provider and then store in
                // Cache
                String selection = MimetypeRegistryContract.TemplateMapping.ACCOUNT_KEY
                        + " = ? AND " + MimetypeRegistryContract.TemplateMapping.MIME_TYPE
                        + " = ? ";
                String[] selectionArgs = new String[] {
                        accountId, mimeType
                };

                Cursor templateMappingCursor = mContext.getContentResolver().query(
                        MimetypeRegistryContract.TemplateMapping.CONTENT_URI,
                        MimetypeRegistryContract.TemplateMapping.DEFAULT_PROJECTION, selection,
                        selectionArgs, null);
                if (templateMappingCursor != null && templateMappingCursor.moveToFirst()) {
                    templateId = templateMappingCursor
                            .getInt(MimetypeRegistryContract.
                                    TemplateMapping.DEFAULT_PROJECTION_TEMPLATE_ID_COLUMN);
                    mTemplateMappingCache.put(lookupKey, templateId);
                } else {
                    templateId = MimetypeRegistryContract.TemplateMapping.StandardItem;
                }
            }
        } catch (IllegalArgumentException ex) {
            Log.e(TAG, "IllegalArgumentException. templateId: " + templateId + " mimeType: "
                    + mimeType + " accountId: " + accountId, ex);
        }

        return mItemHandlers.get(templateId);
    }

    public int getViewTypeCount() {
        return mItemHandlers.size();
    }

    private String getTemplateLookupKey(String accountId, String mimeType) {
        return accountId + ":" + mimeType;
    }
}
