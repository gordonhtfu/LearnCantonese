
package com.blackberry.widgets.smartintentchooser;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.blackberry.widgets.R;

import java.util.ArrayList;
import java.util.List;

/**
 * An adapter which provides a list of actions based on a given {@link Intent}.
 * It expands out any known HUB {@link Intent} items to registered accounts.
 */
public class ActionAdapter extends BaseAdapter {

    private List<ActionDetails> mData;
    private Intent mIntent;
    private Context mContext;
    private ActionFilter mFilter;
    private HubAccounts mHubAccounts;

    private static class ViewHolder {
        public TextView titleTextView;
        public TextView subtitleTextView;
        public ImageView icon;
    }

    /**
     * @param context The context.
     * @param intent The intent to be used to create the list of actions.
     * @param hubAccounts The list of HUB accounts on the system.
     */
    public ActionAdapter(Context context, Intent intent, HubAccounts hubAccounts) {
        mContext = context;

        // don't call setIntent as we don't want to trigger pulling data in case
        // the user wants to set a filter
        mIntent = intent;
        mHubAccounts = hubAccounts;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            LayoutInflater inflater =
                    (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.intent_list_item, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.titleTextView = (TextView) convertView.findViewById(android.R.id.text1);
            viewHolder.subtitleTextView = (TextView) convertView.findViewById(android.R.id.text2);
            viewHolder.icon = (ImageView) convertView.findViewById(android.R.id.icon);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        ActionDetails actionDetails = (ActionDetails) getItem(position);
        viewHolder.titleTextView.setText(actionDetails.getTitle(mContext));
        viewHolder.subtitleTextView.setText(actionDetails.getSubtitle(mContext));
        viewHolder.icon.setImageDrawable(actionDetails.getIcon(mContext));

        if (TextUtils.isEmpty(viewHolder.titleTextView.getText())) {
            viewHolder.titleTextView.setVisibility(View.GONE);
        } else {
            viewHolder.titleTextView.setVisibility(View.VISIBLE);
        }
        if (TextUtils.isEmpty(viewHolder.subtitleTextView.getText())) {
            viewHolder.subtitleTextView.setVisibility(View.GONE);
        } else {
            viewHolder.subtitleTextView.setVisibility(View.VISIBLE);
        }

        return convertView;
    }

    @Override
    public int getCount() {
        checkForData();
        return mData.size();
    }

    @Override
    public Object getItem(int position) {
        checkForData();
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * @param intent The intent to use to create the list.
     */
    public void setIntent(Intent intent) {
        mIntent = intent;
        updateData();
    }

    private void checkForData() {
        if (mData == null) {
            updateData();
        }
    }

    private void updateData() {
        List<ResolveInfo> packageList =
                mContext.getPackageManager().queryIntentActivities(mIntent, 0);
        ActionDetailsListBuilder actionDetailsFactory = new ActionDetailsListBuilder(mHubAccounts);

        for (ResolveInfo resolveInfo : packageList) {
            actionDetailsFactory.createActionDetails(resolveInfo);
        }
        mData = actionDetailsFactory.getList();

        if (mFilter != null) {
            mData = mFilter.filterActions(mData, mIntent);
            // in case the filter does something stupid
            if (mData == null) {
                mData = new ArrayList<ActionDetails>(0);
            }
        }
        notifyDataSetChanged();
    }

    /**
     * Choose the item at the selected position, returning an Intent.
     * 
     * @param position The position in this list for the item which was
     *            selected.
     * @return The Intent for the chosen item.
     */
    public Intent chooseIntent(int position) {
        ActionDetails actionDetails = (ActionDetails) getItem(position);
        return actionDetails.getIntent(mIntent);
    }

    /**
     * Set a filter to augment the list before displaying.
     * 
     * @param filter The filter to use on the list.
     */
    public void setFilter(ActionFilter filter) {
        mFilter = filter;
        if (mData != null) {
            updateData();
        }
    }
}
