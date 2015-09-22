
package com.blackberry.common.ui.list;

import com.blackberry.datagraph.provider.DataGraphContract;
import com.blackberry.menu.MenuBuilder;
import com.blackberry.menu.MenuItemDetails;
import com.blackberry.menu.RequestedItem;
import com.blackberry.provider.ListItemContract;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Concrete {@link ListUICabDelegate} that uses Menu Service to create menu items.
 * <p>
 * It expects a ListItem cursor.
 */
public class ListItemCabDelegate implements ListUICabDelegate {

    private Context mContext;
    private Cursor mCursor;

    /**
     * Constructor.
     * 
     * @param context The context
     */
    public ListItemCabDelegate(Context context) {
        mContext = context;
    }

    @Override
    public void setSelectedItem(Object obj) {
        if (obj instanceof Cursor) {
            mCursor = (Cursor) obj;
        }
    }

    @Override
    public boolean populateMenu(Menu menu) {
        MenuBuilder menuBuilder = null;
        List<MenuItemDetails> menuItems = new ArrayList<MenuItemDetails>();
        ArrayList<RequestedItem> list = new ArrayList<RequestedItem>();

        String mimeType = mCursor.getString(mCursor
                .getColumnIndex(DataGraphContract.EntityColumns.MIME_TYPE));
        String sourceId = mCursor.getString(mCursor
                .getColumnIndex(DataGraphContract.EntityColumns.URI));
        long state = mCursor
                .getLong(mCursor.getColumnIndex(DataGraphContract.EntityColumns.STATE));

        list.add(new RequestedItem(Uri.parse(sourceId), mimeType, state));
        menuBuilder = new MenuBuilder(mContext, list);
        menuItems = menuBuilder.getMenuItems(mContext);

        MenuBuilder.populateMenu(mContext, menu, menuItems);
        return true;
    }

    @Override
    public boolean onMenuItemClicked(MenuItem item) {
        try {
            // TODO: fix this when menuservice is ready to launch service
            Set<String> categories = item.getIntent().getCategories();
            if (categories != null && categories.contains("activity")) {
                mContext.startActivity(item.getIntent());
            } else {
                mContext.startService(item.getIntent());
            }
        } catch (Exception e) {
            Log.e("ListItemCabDelegate", "mContext.startService failure.");
            e.printStackTrace();
        }

        return true;
    }
}
