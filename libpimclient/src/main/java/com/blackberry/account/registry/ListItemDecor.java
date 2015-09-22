package com.blackberry.account.registry;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import android.util.Log;
import android.util.LruCache;
import android.util.SparseArray;

import java.util.ArrayList;

/**
 * 
 * @author dsutedja
 * @version 1.0
 * @since 1.0
 */
public final class ListItemDecor {

    /**
     * Enum describing the position in the standard list item template.
     * 
     * @since 1.0
     */
    public static enum StandardListItemTemplate {
        PrimaryIcon,
        PrimaryText,
        SecondaryText,
        Timestamp;

        /**
         * Convert the enum to int.
         * 
         * @return the ordinal of the enum
         */
        public int toInt() {
            return ordinal();
        }
    }

    /**
     * Enum describing the type of the decor.  Could be: Icon or TextStyle
     * 
     * @since 1.0
     */
    public static enum Type {
        Icon,
        TextStyle;

        /**
         * Convert the enum to int.
         * 
         * @return the ordinal of the enum
         */
        public int toInt() {
            return ordinal();
        }
    }

    /**
     * Use this class to create a registration for list item decoration.
     * Once you have finished adding icons and text style, make sure to call commit(), which
     * will persist the changes to the registry.
     * 
     * Note that deleting an account will also clean up all registered resources for list item
     * 
     * @since 1.0
     */
    public static final class Registration {
        public static final String LOG_TAG = "LID_Reg";
        private Context mContext;
        private long mAccountId;
        private String mPackageName;
        private String mMimeType;

        private ArrayList<IconData> mIcons;
        private ArrayList<TextStyle> mTextStyles;

        Registration(Context context, long accountId, String packageName, String mimeType) {
            mContext = context;
            mAccountId = accountId;
            mPackageName = packageName;
            mMimeType = mimeType;

            mIcons = new ArrayList<IconData>();
            mTextStyles = new ArrayList<TextStyle>();
        }

        /**
         * Create a new registration instance for a list item decoration.
         * 
         * @param context -- the context
         * @param accountId -- the account id of the data
         * @param packageName -- the package where icon is hosted
         * @param mimeType -- mime type of the data
         * 
         * @return a new registration object.
         */
        public static Registration newInstance(Context context, long accountId, String packageName,
                String mimeType) {
            return new Registration(context, accountId, packageName, mimeType);
        }

        /**
         * Add a new icon to the registration.
         * 
         * @return an instance of IconData. Set all appropriate values on the returned instance.
         */
        public IconData addIcon() {
            IconData icon = new IconData(mContext, mAccountId, mPackageName, mMimeType);
            mIcons.add(icon);
            return icon;
        }

        /**
         * Add a new text style definition to the registration.
         * 
         * @return the new instance.  Set all appropriate values on the returned instance.
         */
        public TextStyle addTextStyle() {
            TextStyle style = new TextStyle(mContext, mAccountId, mMimeType);
            mTextStyles.add(style);
            return style;
        }

        /**
         * Once you have finished added icons and text style, call this method to complete
         * the registration process.
         * 
         * @return the commit result array.
         */
        public ContentProviderResult[] commit() {
            ContentProviderResult[] results = null;
            ArrayList<ContentProviderOperation> ops =
                    new ArrayList<ContentProviderOperation>(mIcons.size() + mTextStyles.size());
            for (IconData icon : mIcons) {
                ContentProviderOperation op = icon.toContentProviderOperation();
                ops.add(op);
            }
            for (TextStyle style : mTextStyles) {
                ContentProviderOperation op = style.toContentProviderOperation();
                ops.add(op);
            }
            try {
                results = mContext.getContentResolver()
                        .applyBatch(MimetypeRegistryContract.AUTHORITY, ops);
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "applyBatch for icon and text styles failed: " + e.toString());
                e.printStackTrace();
            } catch (OperationApplicationException e) {
                Log.e(LOG_TAG, "applyBatch for icon and text styles failed: " + e.toString());
                e.printStackTrace();
            }
            return results;
        }
    }

    /**
     * Use this class to create a search query for registered list item decoration.
     * 
     * Call execute() to run the query, and it will return an instance of Result object.
     * 
     * @since 1.0
     */
    public static final class Search {
        private Context mContext;
        private long mAccountId;
        private int mTemplateId;
        private String mMimeType;
        private int mItemState;
        private String mCacheKey;

        private static final String LOG_TAG = "LID_S";
        private static final String KEY_SEP = "::";
        private static final int MAX_CACHE_SIZE = 50;
        private static final LruCache<String, Result> CACHE =
                new LruCache<String, Result>(MAX_CACHE_SIZE);

        Search(Context context, long accountId, int templateId, String mimeType, int state) {
            mContext = context;
            mAccountId = accountId;
            mTemplateId = templateId;
            mMimeType = mimeType;
            mItemState = state;

            mCacheKey = new StringBuffer().append(mAccountId).append(KEY_SEP)
                    .append(mMimeType).append(KEY_SEP)
                    .append(mItemState).append(KEY_SEP)
                    .append(mTemplateId).toString();
        }

        /**
         * Run the search.
         * 
         * @return the Result set.
         */
        public Result execute() {
            Result result = null;

            if ((result = CACHE.get(mCacheKey)) == null) {
                //                Log.d(LOG_TAG, "++Cannot find item in cache");
                String selection = MimetypeRegistryContract.DecorMapping.ACCOUNT_KEY + " = ? AND "
                        + MimetypeRegistryContract.DecorMapping.MIME_TYPE + " = ? AND "
                        + MimetypeRegistryContract.DecorMapping.TEMPLATE_ID + " = ? AND (? & "
                        + MimetypeRegistryContract.DecorMapping.ITEM_STATE + " = " +
                        MimetypeRegistryContract.DecorMapping.ITEM_STATE + ")";

                String[] selectionArgs = new String[] {
                        String.valueOf(mAccountId), mMimeType, String.valueOf(mTemplateId),
                        String.valueOf(mItemState)
                };

                Cursor cursor = mContext.getContentResolver().query(
                        MimetypeRegistryContract.DecorMapping.CONTENT_URI,
                        MimetypeRegistryContract.DecorMapping.DEFAULT_PROJECTION,
                        selection, selectionArgs, null);

                result = new Result();
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        int elementType = cursor.getInt(cursor.getColumnIndexOrThrow(
                                MimetypeRegistryContract.DecorMapping.ELEMENT_TYPE));
                        if (elementType == ListItemDecor.Type.Icon.toInt()) {
                            result.addIcon(createIconFromCursor(cursor));
                        } else if (elementType == ListItemDecor.Type.TextStyle.toInt()) {
                            result.addTextStyle(createTextStyleFromCursor(cursor));
                        }
                    }
                    cursor.close();
                    //                    Log.d(LOG_TAG, "++putting item in cache");
                    CACHE.put(mCacheKey, result);
                }
            }
            //            } else {
            //                Log.d(LOG_TAG, "--Found the item in cache!!");
            //            }
            return result;
        }

        private IconData createIconFromCursor(Cursor cursor) {
            String packageName = cursor.getString(cursor.getColumnIndexOrThrow(
                    MimetypeRegistryContract.DecorMapping.PACKAGE_NAME));
            int resourceId = cursor.getInt(cursor.getColumnIndexOrThrow(
                    MimetypeRegistryContract.DecorMapping.ELEMENT_RESOURCE_ID));
            int position = cursor.getInt(cursor.getColumnIndexOrThrow(
                    MimetypeRegistryContract.DecorColumns.ELEMENT_POSITION));

            IconData icon = new IconData(mContext, mAccountId, packageName, mMimeType);
            icon.setResourceId(resourceId).setTemplateId(mTemplateId).setItemState(mItemState)
            .setPackageName(packageName).setElementPosition(position);

            return icon;
        }

        private TextStyle createTextStyleFromCursor(Cursor cursor) {
            int position = cursor.getInt(cursor.getColumnIndexOrThrow(
                    MimetypeRegistryContract.DecorColumns.ELEMENT_POSITION));
            int style = cursor.getInt(cursor.getColumnIndexOrThrow(
                    MimetypeRegistryContract.DecorColumns.ELEMENT_STYLE));

            TextStyle ts = new TextStyle(mContext, mAccountId, mMimeType);
            ts.setTemplateId(mTemplateId).setElementPosition(position).setRawStyle(style);
            return ts;
        }

        /**
         * Create a new instance of Search.
         * 
         * @param context -- the context
         * @param accountId -- account that owns the data
         * @param templateId -- the template this data is going to be painted on
         * @param mimeType -- the data mimetype
         * @param itemState -- the data itemState
         * 
         * @return the new instance of Search.
         */
        public static Search newInstance(Context context, long accountId, int templateId,
                String mimeType, int itemState) {
            //            Log.d(LOG_TAG, "** Cache size is now? " + CACHE.size());
            return new Search(context, accountId, templateId, mimeType, itemState);
        }
    }

    /**
     * Result of a search query.  From this object, you can obtain the list of icons and text
     * styles registered.
     * 
     * @since 1.0
     */
    public static final class Result {
        private SparseArray<IconData> mIcons;
        private SparseArray<TextStyle> mStyles;

        Result() {
            mIcons = new SparseArray<IconData>();
            mStyles = new SparseArray<TextStyle>();
        }

        void addIcon(IconData icon) {
            int position = icon.elementPosition();
            mIcons.put(Integer.valueOf(position), icon);
        }

        void addTextStyle(TextStyle textStyle) {
            int position = textStyle.elementPosition();
            mStyles.put(Integer.valueOf(position), textStyle);
        }

        /**
         * Check if there's a [type] decor defined in the given position.
         * 
         * @param position -- what position?
         * @param type -- the decor type
         * 
         * @return true if exists
         */
        public boolean contains(int position, ListItemDecor.Type type) {
            boolean retVal = false;
            switch (type) {
                case Icon:
                    retVal = mIcons.indexOfKey(position) >= 0;
                    break;
                case TextStyle:
                    retVal = mStyles.indexOfKey(position) >= 0;
                    break;
                default:
                    break;
            }
            return retVal;
        }

        /**
         * Get the icon at the given position.
         * 
         * @param position -- the position we're checking.
         * @return valid item or null if doesn't exist.
         */
        public IconData getIconAt(int position) {
            return mIcons.get(position);
        }

        /**
         * Get the text style at the given position.
         * 
         * @param position -- the position we're checking.
         * @return valid item or null if doesn't exist.
         */
        public TextStyle getTextStyleAt(int position) {
            return mStyles.get(position);
        }
    }
}
