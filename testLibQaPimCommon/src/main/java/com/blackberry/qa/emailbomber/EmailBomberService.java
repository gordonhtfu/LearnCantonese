package com.blackberry.qa.emailbomber;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.blackberry.email.provider.UIProvider;

/**
 * Class for sending messages every time the alarm manager sends out the bomb event.
 * @author kwright
 *
 */
public class EmailBomberService extends IntentService {
    private static String TAG = EmailBomberService.class.getSimpleName();

    /**
     * Constructor.
     */
    public EmailBomberService() {
        super(EmailBomberService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "Handling Bomber event");
        ContentResolver resolver = getContentResolver();
        Uri accountUri = intent.getParcelableExtra("account.uri");
        resolver.call(accountUri, UIProvider.AccountCallMethods.SEND_MESSAGE,
                accountUri.toString(), intent.getExtras());
        EmailBomberWakefulReceiver.completeWakefulIntent(intent);
    }
}