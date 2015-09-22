
package com.blackberry.common.ui.list.templates;

import com.blackberry.account.registry.ListItemDecor;
import com.blackberry.account.registry.MimetypeRegistryContract;
import com.blackberry.account.registry.TextStyle;
import com.blackberry.common.ui.R;
import com.blackberry.common.ui.controller.InvokeData;
import com.blackberry.datagraph.provider.DataGraphContract;
import com.blackberry.provider.ListItemContract;
import com.blackberry.provider.MessageConversationListItemValue;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * List item populator for standard list item template.
 * 
 * @author blippeveld
 */
public class StandardListItemHandler extends BaseListItemHandler implements OnClickListener {

    private static final String TAG = "StandardListItemHandler";

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("hh:mm a",
            Locale.getDefault());

    private int mListItemLayout;
    private Context mContext;

    /**
     * Construct a new StandardListItemHandler.
     * 
     * @param context -- the context
     */
    public StandardListItemHandler(Context context) {
        mContext = context;
        mListItemLayout = R.layout.standard_list_item_object;
        // Will need to handle icons better
        //        this(context, 0, "Email", R.drawable.ic_email, "From", "Subject");
    }

    @Override
    public int getListItemLayout(Cursor c) {
        // This will return list or grid in the future, when it is decided
        // where to store this value. Should probably be at account registry
        return mListItemLayout;
    }

    @Override
    public Object createHolderForItemLayout(View v) {
        StandardListItemViewHolder vh = new StandardListItemViewHolder();
        vh.mPrimaryTextView = (TextView) v.findViewById(R.id.text_1);
        vh.mSecondaryTextView = (TextView) v.findViewById(R.id.text_2);
        vh.mTimestampTextView = (TextView) v.findViewById(R.id.timestamp);
        vh.mIconImageView = (ImageView) v.findViewById(R.id.iconImage);
        vh.mCounterContainer = (RelativeLayout) v.findViewById(R.id.counter_container);
        vh.mCounterTextView = (TextView) v.findViewById(R.id.counter_text);
        return vh;
    }

    @Override
    public void populateListItemView(Cursor c, Object holder) {
        try {
            if (holder instanceof StandardListItemViewHolder) {
                String displayOrderKey = mContext.getResources().getString(
                        R.string.pref_key_display_order);
                String displayOrderSN = mContext.getResources().getString(
                        R.string.pref_display_order_sn);

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
                String displayOrder = prefs.getString(displayOrderKey, displayOrderSN);

                StandardListItemViewHolder vh = (StandardListItemViewHolder) holder;

                String text1Column = (displayOrder.equals(displayOrderSN))
                        ? DataGraphContract.EntityColumns.SECONDARY_TEXT
                                : DataGraphContract.EntityColumns.PRIMARY_TEXT;
                String text2Column = (displayOrder.equals(displayOrderSN))
                        ? DataGraphContract.EntityColumns.PRIMARY_TEXT
                                : DataGraphContract.EntityColumns.SECONDARY_TEXT;

                String text1 = checkNull(c.getString(c.getColumnIndexOrThrow(text1Column)));
                String text2 = checkNull(c.getString(c.getColumnIndexOrThrow(text2Column)));
                String accountId = c.getString(c
                        .getColumnIndexOrThrow(DataGraphContract.EntityColumns.ACCOUNT_ID));
                String mimeType = c.getString(c
                        .getColumnIndexOrThrow(DataGraphContract.EntityColumns.MIME_TYPE));

                long state = c.getInt(c
                        .getColumnIndexOrThrow(DataGraphContract.EntityColumns.STATE));

                // TODO: we need a value object for list item, not just conversation specific?
                if (mimeType.equals("vnd.android.cursor.item/vnd.bb.email-conversation")) {
                    int totalCount = (int) MessageConversationListItemValue.getTotalCount(state);
                    int unreadCount = (int) MessageConversationListItemValue.getUnreadCount(state);
                    state = MessageConversationListItemValue.getState(state);
                    updateCounter(vh, unreadCount, totalCount);
                } else {
                    vh.mCounterContainer.setVisibility(View.GONE);
                }

                long timestamp = c.getLong(c
                        .getColumnIndexOrThrow(ListItemContract.ListItemColumns.TIMESTAMP));

                // search the decor
                // TODO: accountID is long or string???
                ListItemDecor.Result decor = searchListItemDecor(mContext,
                        Long.parseLong(accountId), mimeType,
                        MimetypeRegistryContract.TemplateMapping.StandardItem, (int)state);

                updateTextViews(vh, decor, text1, text2, timestamp);

                String uri = c.getString(c
                        .getColumnIndexOrThrow(ListItemContract.ListItemColumns.URI));
                InvokeData invokeDataIcon1 = new InvokeData(uri,
                        MimetypeRegistryContract.TemplateMapping.StandardItem, 1);
                vh.mIconImageView.setTag(invokeDataIcon1);
                vh.mIconImageView.setOnClickListener(this);
            }
        } catch (IllegalArgumentException ex) {
            Log.e(TAG, "IllegalArgumentException", ex);
        }
    }

    private void updateTextViews(StandardListItemViewHolder vh, ListItemDecor.Result decor,
            String text1, String text2, long timestamp) {
        // since in this view text_1 is the subject and my be blank we can
        // sub in the text_2 if that's not blank
        if (!text1.isEmpty() && !text2.isEmpty()) {
            vh.mPrimaryTextView.setText(text1);
            vh.mSecondaryTextView.setText(text2);
            vh.mSecondaryTextView.setVisibility(View.VISIBLE);
        } else if (!text1.isEmpty() && text2.isEmpty()) {
            vh.mPrimaryTextView.setText(text1);
            vh.mSecondaryTextView.setVisibility(View.GONE);
        } else if (text1.isEmpty() && !text2.isEmpty()) {
            vh.mPrimaryTextView.setText(text2);
            vh.mSecondaryTextView.setVisibility(View.GONE);
        }

        // update timestamp
        Date date = new Date(timestamp);
        vh.mTimestampTextView.setText(DATE_FORMAT.format(date));

        // style the primary text if needed
        TextStyle ts = getTextStyle(decor,
                ListItemDecor.StandardListItemTemplate.PrimaryText.toInt());
        if (ts != null && ts.hasStyle(TextStyle.BOLD)) {
            vh.mPrimaryTextView.setTypeface(Typeface.DEFAULT_BOLD);
        } else {
            vh.mPrimaryTextView.setTypeface(Typeface.DEFAULT);
        }

        // paint the icon
        Drawable primaryIcon = getDrawable(decor,
                ListItemDecor.StandardListItemTemplate.PrimaryIcon.toInt());

        if (primaryIcon == null) {
            vh.mIconImageView.setImageResource(R.drawable.ic_email);
        } else {
            vh.mIconImageView.setImageDrawable(primaryIcon);
        }
    }

    private void updateCounter(StandardListItemViewHolder vh, int unreadCount, int totalCount) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(unreadCount).append("/");
        buffer.append(totalCount);
        vh.mCounterContainer.setVisibility(View.VISIBLE);
        vh.mCounterTextView.setText(buffer.toString());
    }

    @Override
    public void onClick(View v) {
        super.onClick((InvokeData) v.getTag());
    }

    @Override
    public int getActionsMenu() {
        return R.menu.actions_context_base;
    }

    private String checkNull(String s) {
        return s == null ? "" : s;
    }

    private static class StandardListItemViewHolder {
        TextView mPrimaryTextView;
        TextView mSecondaryTextView;
        TextView mTimestampTextView;
        TextView mCounterTextView;
        RelativeLayout mCounterContainer;
        ImageView mIconImageView;
    }
}
