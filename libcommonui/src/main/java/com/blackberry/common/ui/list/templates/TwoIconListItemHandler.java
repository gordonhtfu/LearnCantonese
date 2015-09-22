
package com.blackberry.common.ui.list.templates;

import com.blackberry.account.registry.MimetypeRegistryContract;
import com.blackberry.common.ui.R;
import com.blackberry.common.ui.controller.InvokeData;
import com.blackberry.datagraph.provider.DataGraphContract;
import com.blackberry.provider.ListItemContract;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TwoIconListItemHandler extends BaseListItemHandler implements OnClickListener {

    private static final String TAG = "TwoIconListItemHandler";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm",
            Locale.getDefault());

    private int mIcon1;
    private int mIcon2;
    private int mListItemLayout;
    private int mGridItemLayout;
    Context mContext;

    public TwoIconListItemHandler(Context context) {
        // Will need to handle icons better
        this(context, 0, "Email", R.drawable.ic_email, R.drawable.ic_email, "From", "Subject");
    }

    public TwoIconListItemHandler(Context context, int id, String name, int icon1, int icon2,
            String primaryLabel,
            String secondaryLabel) {
        mContext = context;
        mIcon1 = icon1;
        mIcon2 = icon2;
        mListItemLayout = R.layout.two_icon_list_item_object;
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
        TwoIconListItemViewHolder vh = new TwoIconListItemViewHolder();
        vh.mPrimaryTextView = (TextView) v.findViewById(R.id.primary_text);
        vh.mSecondaryTextView = (TextView) v.findViewById(R.id.secondary_text);
        vh.mTimestampTextView = (TextView) v.findViewById(R.id.timestamp);
        vh.mIcon1ImageView = (ImageView) v.findViewById(R.id.icon1Image);
        vh.mIcon2ImageView = (ImageView) v.findViewById(R.id.icon2Image);
        return vh;
    }

    @Override
    public void populateListItemView(Cursor c, Object holder) {
        try {
            if (holder instanceof TwoIconListItemViewHolder) {
                TwoIconListItemViewHolder vh = (TwoIconListItemViewHolder) holder;

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
                        MimetypeRegistryContract.TemplateMapping.TwoIconItem, 1);
                vh.mIcon1ImageView.setTag(invokeDataIcon1);
                vh.mIcon1ImageView.setOnClickListener(this);

                vh.mIcon2ImageView.setImageResource(mIcon2);
                InvokeData invokeDataIcon2 = new InvokeData(uri,
                        MimetypeRegistryContract.TemplateMapping.TwoIconItem, 2);
                vh.mIcon2ImageView.setTag(invokeDataIcon2);
                vh.mIcon2ImageView.setOnClickListener(this);
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

    private static class TwoIconListItemViewHolder {
        TextView mPrimaryTextView;
        TextView mSecondaryTextView;
        TextView mTimestampTextView;
        ImageView mIcon1ImageView;
        ImageView mIcon2ImageView;
    }
}
