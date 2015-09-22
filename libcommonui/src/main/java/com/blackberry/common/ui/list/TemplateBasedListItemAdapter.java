
package com.blackberry.common.ui.list;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;

import com.blackberry.common.ui.list.templates.BaseListItemHandler;
import com.blackberry.common.ui.list.templates.TemplateManager;
import com.google.android.mail.common.base.Preconditions;

/**
 * An extension of {@link CursorAdapter} that uses {@link TemplateManager} to get a template for the
 * layout.
 */
public class TemplateBasedListItemAdapter extends CursorAdapter {

    private TemplateManager mTemplateManager;
    private LayoutInflater mInflater;

    /**
     * Constructor.
     * 
     * @param context The context
     * @param templateManager The template manager.
     */
    public TemplateBasedListItemAdapter(Context context, TemplateManager templateManager) {
        super(context, null, 0);
        Preconditions.checkNotNull(templateManager, "TemplateManager cannot be null");
        mTemplateManager = templateManager;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        BaseListItemHandler itemHandler = getHandlerFromCursor(cursor);
        itemHandler.populateListItemView(cursor, view.getTag());
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        BaseListItemHandler itemHandler = getHandlerFromCursor(cursor);
        int layout = itemHandler.getListItemLayout(cursor);
        View v = mInflater.inflate(layout, parent, false);
        v.setTag(itemHandler.createHolderForItemLayout(v));
        return v;
    }

    private BaseListItemHandler getHandlerFromCursor(Cursor c) {
        return mTemplateManager.getItemHandler(c);
    }
}
