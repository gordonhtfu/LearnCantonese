
package com.blackberry.common.ui.list.templates;

import com.blackberry.account.registry.IconData;
import com.blackberry.account.registry.ListItemDecor;
import com.blackberry.account.registry.TextStyle;
import com.blackberry.common.ui.controller.InvokeData;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.view.View;

public abstract class BaseListItemHandler {

    private static final String TAG = "BaseListItemHandler";

    public abstract int getListItemLayout(Cursor c);

    public abstract Object createHolderForItemLayout(View v);

    public abstract void populateListItemView(Cursor c, Object holder);

    public abstract int getActionsMenu();

    public void onClick(InvokeData invokeData) {
        String uri = invokeData.mUri;
        int templateId = invokeData.mTemplateId;
        int viewId = invokeData.mViewId;

        // This will be replaced by firing an invoke with uri, templateId, and viewId
        // To be handled by the respective services
    }

    public ListItemDecor.Result searchListItemDecor(Context context, long accountId,
            String mimeType, int templateId, int state) {
        return ListItemDecor.Search.newInstance(context, accountId, templateId,
                mimeType, state).execute();
    }

    public Drawable getDrawable(ListItemDecor.Result result, int position) {
        Drawable drawable = null;

        if (result.contains(position, ListItemDecor.Type.Icon)) {
            IconData icon = result.getIconAt(position);
            drawable = icon.drawable();
        }

        return drawable;
    }

    public TextStyle getTextStyle(ListItemDecor.Result result, int position) {
        TextStyle ts = null;

        if (result.contains(position, ListItemDecor.Type.TextStyle)) {
            ts = result.getTextStyleAt(position);
        }

        return ts;
    }
}
