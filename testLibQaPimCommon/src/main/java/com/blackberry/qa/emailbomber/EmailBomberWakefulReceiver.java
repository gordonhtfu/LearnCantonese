package com.blackberry.qa.emailbomber;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

/**
 *
 * @author kwright
 * Receives requests to send out an email bomb.
 *
 * Intents sent to this class should contain extras that define key/values for:
 *      UIProvider.MessageColumns.SUBJECT
        UIProvider.MessageColumns.BCC
        UIProvider.MessageColumns.CC
        UIProvider.MessageColumns.ATTACHMENTS
        UIProvider.MessageColumns.DRAFT_TYPE
        UIProvider.MessageColumns.TO
        UIProvider.MessageColumns.BODY_HTML
        UIProvider.MessageColumns.BODY_TEXT
        "account.uri"
 */

public class EmailBomberWakefulReceiver extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent service = new Intent(context, EmailBomberService.class);
        service.replaceExtras(intent.getExtras());
        startWakefulService(context, service);
    }
}