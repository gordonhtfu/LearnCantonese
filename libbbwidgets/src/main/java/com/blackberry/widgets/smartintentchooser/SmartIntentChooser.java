
package com.blackberry.widgets.smartintentchooser;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.blackberry.widgets.R;

/**
 * Shows a grouping of frequent contacts/accounts at the top followed by a list
 * of actions that can also be performed. Clicking on one performs that action.
 */
public class SmartIntentChooser extends FrameLayout {

    private ListView mAccountsListView;
    private ActionAdapter mAdapter;
    private OnIntentChosenListener mOnIntentChosenListener;
    private HubAccounts mHubAccounts;

    /**
     * @param context The context.
     * @param attrs The xml attributes.
     */
    public SmartIntentChooser(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.smart_intent_chooser, this, true);

        if (!isInEditMode()) {
            mAccountsListView = (ListView) findViewById(R.id.sicAccountsListView);
            mHubAccounts = new HubAccounts(context);
            mAccountsListView.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    startIntent(mAdapter.chooseIntent(position));
                }
            });

            // Remove this and force the user to pass one in or leave it as a
            // default?
            Intent intent = new Intent(android.content.Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(android.content.Intent.EXTRA_TEXT, "");
            setIntent(intent);
        }
    }

    private void startIntent(Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        getContext().startActivity(intent);
        if (mOnIntentChosenListener != null) {
            mOnIntentChosenListener.onIntentChosen(intent);
        }
    }

    /**
     * @param intent The intent used to generate the list of actions.
     */
    public void setIntent(Intent intent) {
        mAdapter = new ActionAdapter(getContext(), intent, mHubAccounts);
        mAccountsListView.setAdapter(mAdapter);
    }

    /**
     * Sets a filter to apply to the list of actions displayed in this control.
     * 
     * @param filter The filter to apply
     */
    public void setFilter(ActionFilter filter) {
        mAdapter.setFilter(filter);
    }

    /**
     * @return The listener registered for {@link Intent} launch notifications.
     */
    public OnIntentChosenListener getOnIntentChosenListener() {
        return mOnIntentChosenListener;
    }

    /**
     * Set the listener to be notified when an {@link Intent} has been launched.
     * 
     * @param onIntentChosenListener The listener.
     */
    public void setOnIntentChosenListener(OnIntentChosenListener onIntentChosenListener) {
        mOnIntentChosenListener = onIntentChosenListener;
    }

    /**
     * An interface used to notify the caller that an {@link Intent} has been
     * chosen and started. This is useful, for example, for the calling Activity
     * to call {@link Activity#finish()} to close itself.
     */
    public interface OnIntentChosenListener {
        /**
         * Called when an {@link Intent} has been chosen and launched.
         * 
         * @param intent The intent which was launched.
         */
        void onIntentChosen(Intent intent);
    }
}
