package com.blackberry.widgets.tagview.internal.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.content.LocalBroadcastManager;

public class TagsActivity extends Activity {
    /**
     * The ID to use for the callback.
     *
     * @see #onActivityResult(int, int, android.content.Intent)
     */
    private static final int CONTACT_PICKER_RESULT = 1001;
    /**
     * The ID to use for the callback
     *
     * @see #onActivityResult(int, int, android.content.Intent)
     */
    private static final int ADD_CONTACT_RESULT = 1002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        switch (getIntent().getIntExtra(ActivityHelper.EXTRA_ACTION, -1)) {
            case ActivityHelper.EXTRA_ACTION_CONTACT_PICKER:
                Intent contactPickerIntent = new Intent(Intent.ACTION_PICK,
                        ContactsContract.Contacts.CONTENT_URI);
                contactPickerIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,
                        getIntent().getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false));
                // TODO: Need to limit what type of contacts show (has email, has phone, both, etc).
                // has email or has phone is easy and built in to the standard contact picker. A
                // combination is not so we would need our own contact picker.
                startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT);
                break;
            case ActivityHelper.EXTRA_ACTION_ADD_CONTACT:
                Intent addContactIntent = new Intent(ContactsContract.Intents.Insert.ACTION);
                addContactIntent.setType(ContactsContract.Contacts.CONTENT_TYPE);
                addContactIntent.putExtras(getIntent());
                // Needed for Android 4.0+. Has no effect on <4.0
                addContactIntent.putExtra("finishActivityOnSaveCompleted", true);
                startActivityForResult(addContactIntent, ADD_CONTACT_RESULT);
                break;
            default:
                setResult(RESULT_CANCELED);
                finish();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case CONTACT_PICKER_RESULT:
                    sendBroadcastIntent(getIntent(), data, ContactsContract.Contacts.CONTENT_TYPE);
                    setResult(RESULT_OK);
                    finish();
                    break;
                case ADD_CONTACT_RESULT:
                    sendBroadcastIntent(getIntent(), data, ContactsContract.Contacts.CONTENT_TYPE);
                    setResult(RESULT_OK);
                    break;
            }
        } else {
            setResult(resultCode);
        }
        finish();
    }

    /**
     * @param launchedData The Intent used to launch this Activity
     * @param resultData   The Intent returned by the subsequently launched Activity
     * @param contentType  The content type to broadcast
     */
    private void sendBroadcastIntent(Intent launchedData, Intent resultData, String contentType) {
        Intent broadcastIntent = new Intent(ActivityHelper.BROADCAST_ACTION);
        broadcastIntent.setDataAndType(resultData.getData(), contentType);
        broadcastIntent.putExtra(Intent.EXTRA_STREAM,
                resultData.getParcelableArrayExtra(Intent.EXTRA_STREAM));
        broadcastIntent.putExtra(ActivityHelper.EXTRA_UUID,
                launchedData.getStringExtra(ActivityHelper.EXTRA_UUID));
        broadcastIntent.putExtra(ActivityHelper.EXTRA_ACTION,
                launchedData.getIntExtra(ActivityHelper.EXTRA_ACTION, -1));
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
    }
}
