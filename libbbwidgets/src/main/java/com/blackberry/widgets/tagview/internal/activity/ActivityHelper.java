package com.blackberry.widgets.tagview.internal.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.support.v4.content.LocalBroadcastManager;

import java.util.UUID;

/**
 * An internal class to do the heavy lifting of starting the Activity inside this library.
 * <p/>
 * Use (or add to) this class when you need an Activity to perform some action (like a Contact
 * Picker).
 * <p/>
 * Each Tags instance must use its own ActivityHelper. DO NOT share instances of this class.
 */
public class ActivityHelper {
    /**
     * The key to use for the broadcast action
     */
    public static final String BROADCAST_ACTION = "com.blackberry.widgets.tagview.internal.activity" +
            ".BROADCAST";

    /**
     * The key to use for the UUID field in the intent.
     */
    public static final String EXTRA_UUID = "com.blackberry.widgets.tagview.internal.activity.EXTRA_UUID";

    /**
     * The key to use for the Action field in the intent. Must be one of the EXTRA_ACTION_* int
     * constants below.
     */
    public static final String EXTRA_ACTION = "com.blackberry.widgets.tagview.internal.activity" +
            ".EXTRA_ACTION";
    /**
     * The Action ID for the Contact Picker action.
     */
    public static final int EXTRA_ACTION_CONTACT_PICKER = 0x0;
    /**
     * The Action ID for the Add Contact Activity action.
     */
    public static final int EXTRA_ACTION_ADD_CONTACT = 0x1;

    /**
     * The UUID
     */
    private UUID mUuid = UUID.randomUUID();
    /**
     * The broadcast receiver for the contact picker.
     */
    private BroadcastReceiver mContactPickerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!verifyReceivedIntent(intent, EXTRA_ACTION_CONTACT_PICKER)) {
                return;
            }
            LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
            if (mOnContactPicked != null) {
                Parcelable[] selectedUris;
                if (intent.getExtras().containsKey(Intent.EXTRA_STREAM) &&
                        ((selectedUris = intent.getExtras()
                                .getParcelableArray(Intent.EXTRA_STREAM))) != null) {
                    for (Parcelable uri : selectedUris) {
                        mOnContactPicked.onContactPicked((Uri) uri);
                    }
                } else if (intent.getData() != null) {
                    mOnContactPicked.onContactPicked(intent.getData());
                }
            }
        }
    };
    /**
     * The listener registered for when a contact is picked.
     *
     * @see #setOnContactPicked(com.blackberry.widgets.tagview.internal.activity.ActivityHelper
     * .OnContactPicked)
     * @see com.blackberry.widgets.tagview.internal.activity.ActivityHelper.OnContactPicked
     */
    private OnContactPicked mOnContactPicked;
    /**
     * The listener registered for when a contact is added.
     */
    private OnContactAdded mOnContactAdded;
    /**
     * The listener registered for when the contact has been added (or canceled).
     */
    private BroadcastReceiver mAddContactReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!verifyReceivedIntent(intent, EXTRA_ACTION_ADD_CONTACT)) {
                return;
            }
            LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
            if (mOnContactAdded != null) {
                mOnContactAdded.onContactAdded(intent.getData());
            }
        }
    };

    /**
     * Attempt to show the contact picker. If the Application does not have the proper activity in
     * their manifest this method will fail and throw an exception.
     *
     * @param context The context
     * @param allowMultiple Enables the ability to multi-select contacts
     */
    public void showContactPicker(Context context, boolean allowMultiple) {
        Intent intent = createIntent(context, EXTRA_ACTION_CONTACT_PICKER);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple);

        IntentFilter intentFilter = new IntentFilter(BROADCAST_ACTION);
        try {
            intentFilter.addDataType(ContactsContract.Contacts.CONTENT_TYPE);
        } catch (IntentFilter.MalformedMimeTypeException e) {
            e.printStackTrace();
        }
        LocalBroadcastManager.getInstance(context).registerReceiver(mContactPickerReceiver,
                intentFilter);
        context.startActivity(intent);
    }

    /**
     * Attempt to show the add contact activity. If the application does not have the proper
     * activity in their manifest this method will fail and throw an exception
     * <p/>
     * For the list of acceptable extra keys see {@link android.provider.ContactsContract.Intents
     * .Insert}
     *
     * @param context The context
     * @param extras  The extra key/value pairs to pass to the add contact activity
     * @see android.provider.ContactsContract.Intents.Insert
     */
    public void addContact(Context context, Bundle extras) {
        Intent intent = createIntent(context, EXTRA_ACTION_ADD_CONTACT);
        intent.putExtras(extras);

        IntentFilter intentFilter = new IntentFilter(BROADCAST_ACTION);
        try {
            intentFilter.addDataType(ContactsContract.Contacts.CONTENT_TYPE);
        } catch (IntentFilter.MalformedMimeTypeException e) {
            e.printStackTrace();
        }
        LocalBroadcastManager.getInstance(context).registerReceiver(mAddContactReceiver,
                intentFilter);
        context.startActivity(intent);
    }

    /**
     * Register a listener for when a contact has been picked
     *
     * @param onContactPicked The listener to register
     */
    public void setOnContactPicked(OnContactPicked onContactPicked) {
        mOnContactPicked = onContactPicked;
    }

    /**
     * Register a listener for when a contact has been added
     *
     * @param onContactAdded The listener to register
     */
    public void setOnContactAdded(OnContactAdded onContactAdded) {
        mOnContactAdded = onContactAdded;
    }

    /**
     * @param context  The context
     * @param actionId The action identifier
     * @return The intent created
     */
    private Intent createIntent(Context context, int actionId) {
        Intent intent = new Intent(context, TagsActivity.class);
        intent.putExtra(EXTRA_UUID, mUuid.toString());
        intent.putExtra(EXTRA_ACTION, actionId);
        checkForActivity(context, intent);
        return intent;
    }

    /**
     * Query {@link android.content.pm.PackageManager} to see if the Activity was properly
     * registered in the application's manifest file. If not throw an exception.
     *
     * @param context The context
     * @param intent  The Activity's intent to check for
     */
    private void checkForActivity(Context context, Intent intent) {
        if (context.getPackageManager().resolveActivity(intent, 0) == null) {
            // TODO: This should be a better exception with a better string.
            throw new IllegalArgumentException("App is Missing the tag intent from the Manifest");
        }
    }

    /**
     * @param intent The intent to verify
     * @param action The action id the received intent should be responding to
     * @return true if the intent matches or false otherwise. The intent only matches if the UUID
     * and action match.
     */
    private boolean verifyReceivedIntent(Intent intent, int action) {
        UUID uuid;
        try {
            uuid = UUID.fromString(intent.getStringExtra(EXTRA_UUID));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        int intentAction = intent.getIntExtra(EXTRA_ACTION, -1);
        return mUuid.equals(uuid) && (action == intentAction);
    }

    /**
     * An interface used for a callback when a contact has been picked.
     *
     * @see #setOnContactPicked(com.blackberry.widgets.tagview.internal.activity.ActivityHelper
     * .OnContactPicked)
     */
    public interface OnContactPicked {
        /**
         * Called when a contact is picked by the contact picker.
         *
         * @param contactUri The {@link Uri} of the contact
         */
        void onContactPicked(Uri contactUri);
    }

    /**
     * An interface used for a callback when a contact has been added.
     *
     * @see #setOnContactAdded(com.blackberry.widgets.tagview.internal.activity.ActivityHelper
     * .OnContactAdded)
     */
    public interface OnContactAdded {
        /**
         * Called when a contact has been added after a call to {@link #addContact(android
         * .content.Context, android.os.Bundle)}
         *
         * @param contactUri The {@link Uri} of the contact
         */
        void onContactAdded(Uri contactUri);
    }
}
