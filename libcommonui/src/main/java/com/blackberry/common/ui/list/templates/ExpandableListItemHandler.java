
package com.blackberry.common.ui.list.templates;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.blackberry.account.registry.MimetypeRegistryContract;
import com.blackberry.common.ui.R;
import com.blackberry.common.ui.controller.InvokeData;
import com.blackberry.datagraph.provider.DataGraphContract;
import com.blackberry.provider.ListItemContract;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/*
 * Will be used to support items that have a collapsible icon to reveal another row of data.
 * 
 * <b> NOTE: Currently not implemented or used </b>
 */
public class ExpandableListItemHandler extends BaseListItemHandler implements OnClickListener {

    private static final String TAG = "ExpandableListItemHandler";

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm",
            Locale.getDefault());

    private int mIcon1;
    private int mListItemLayout;
    private int mGridItemLayout;
    private Context mContext;

    public ExpandableListItemHandler(Context context) {
        // TODO: Will need to handle icons better
        this(context, 0, "Email", R.drawable.ic_email, "From", "Subject");
    }

    public ExpandableListItemHandler(Context context, int id, String name, int icon1, String primaryLabel,
            String secondaryLabel) {
        mContext = context;
        mIcon1 = icon1;
        mListItemLayout = R.layout.standard_list_item_object;
        mGridItemLayout = 0;
    }

    @Override
    public int getListItemLayout(Cursor c) {
        // This will return list or grid in the future, when it is decided
        // where to store this value. Should probably be at account registry
        return mListItemLayout;
    }

    @Override
    public Object createHolderForItemLayout(View v) {
        ExpandableListItemViewHolder vh = new ExpandableListItemViewHolder();
        vh.mPrimaryTextView = (TextView) v.findViewById(R.id.primary_text);
        vh.mSecondaryTextView = (TextView) v.findViewById(R.id.secondary_text);
        vh.mTimestampTextView = (TextView) v.findViewById(R.id.timestamp);
        vh.mIcon1ImageView = (ImageView) v.findViewById(R.id.iconImage);
        return vh;
    }

    @Override
    public void populateListItemView(Cursor c, Object holder) {
        try {
            if (holder instanceof ExpandableListItemViewHolder) {
                ExpandableListItemViewHolder vh = (ExpandableListItemViewHolder) holder;

                String pt = c.getString(c
                        .getColumnIndexOrThrow(DataGraphContract.EntityColumns.PRIMARY_TEXT));
                String st = c.getString(c
                        .getColumnIndexOrThrow(DataGraphContract.EntityColumns.SECONDARY_TEXT));

                vh.mPrimaryTextView.setText(pt);
                if (st != null && !st.isEmpty()) {
                    vh.mSecondaryTextView.setText(st);
                    vh.mSecondaryTextView.setVisibility(View.VISIBLE);
                } else {
                    vh.mSecondaryTextView.setVisibility(View.GONE);
                }

                long timestamp = c.getLong(c
                        .getColumnIndexOrThrow(DataGraphContract.EntityColumns.TIMESTAMP));
                Date date = new Date(timestamp);
                vh.mTimestampTextView.setText(DATE_FORMAT.format(date));

                String uri = c.getString(c
                        .getColumnIndexOrThrow(DataGraphContract.EntityColumns.URI));

                vh.mIcon1ImageView.setImageResource(mIcon1);
                InvokeData invokeDataIcon1 = new InvokeData(uri,
                        MimetypeRegistryContract.TemplateMapping.ExpandableItem, 1);
                vh.mIcon1ImageView.setTag(invokeDataIcon1);
                vh.mIcon1ImageView.setOnClickListener(this);
            }
        } catch (IllegalArgumentException ex) {
            Log.e(TAG, "IllegalArgumentException", ex);
        }
    }

    @Override
    public void onClick(View v) {
        super.onClick((InvokeData) v.getTag());
    }

    @Override
    public int getActionsMenu() {
        return R.menu.actions_context_base;
    }

    private static class ExpandableListItemViewHolder {
        TextView mPrimaryTextView;
        TextView mSecondaryTextView;
        TextView mTimestampTextView;
        ImageView mIcon1ImageView;
    }
}
