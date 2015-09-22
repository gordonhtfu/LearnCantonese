package com.blackberry.qa.emailbomber;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.blackberry.email.Account;
import com.blackberry.email.provider.UIProvider;

/**
 * Class for scheduling a email to be sent repeatedly.
 * @author kwright
 *
 */
public class EmailBomb {
    private static String TAG = EmailBomb.class.getSimpleName();

    private String mSubject;
    private String mBCC;
    private String mCC;
    private String mTo;
    private String mBody;
    private Account mAccount;
    private long mInterval;
    private int mBombId;
    private Intent mIntent = null;
    private boolean mScheduled = false;

    /**
     *
     * @param subject Email subject
     * @param to Destination email
     * @param cc CC email
     * @param bcc BCC email
     * @param body Plain Text body
     * @param account Account to be sent from
     * @param interval Interval the sends will recurr on in ms
     * @param bombId Arbitrary id used to distinguish different email bombs
     */
    public EmailBomb(String subject, String to, String cc, String bcc, String body,
            Account account, long interval, int bombId) {
        mSubject = subject;
        mTo = to;
        mCC = cc;
        mBCC = bcc;
        mBody = body;
        mAccount = account;
        mInterval = interval;
        mBombId = bombId;
    }

    /**
     * Schedule this email bomb with the alarm manager.
     * @param context Calling context
     */
    public void schedule(Context context) {
        if (!mScheduled) {
            Intent intent = getBombIntent(context);
            PendingIntent alarmIntent;
            alarmIntent = PendingIntent.getBroadcast(context, mBombId, intent,
                    PendingIntent.FLAG_CANCEL_CURRENT);
            AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmMgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, mInterval, mInterval,
                    alarmIntent);
            Log.d(TAG, "Scheduled bomb for " + Long.toString(mBombId));
            mScheduled = true;
        } else {
            throw new IllegalStateException("Attempted to schedule the same bomb more than once");
        }
    }

    /**
     * Cancel this email bomb.  Must be scheduled first.
     * @param context Calling context
     */
    public void cancel(Context context) {
        if (mScheduled) {
            Intent intent = new Intent(context, EmailBomberWakefulReceiver.class);
            //TODO fix long to int cast
            AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmMgr.cancel(PendingIntent.getBroadcast(
                    context, mBombId, intent, PendingIntent.FLAG_CANCEL_CURRENT));
            Log.d(TAG, "Cancelled bomb for " + Long.toString(mBombId));
        } else {
            throw new IllegalStateException("Attempted to cancel bomb that was never scheduled");
        }
    }

    /**
     * Get back a bundle containing the contents of the email bomb.
     * @param context Calling context
     * @return Extras bundle that will be used for email bomb intent
     */
    public Bundle getBombBundle(Context context) {
        return getBombIntent(context).getExtras();
    }

    private Intent getBombIntent(Context context) {
        if (mIntent == null) {
            Intent intent = new Intent(context, EmailBomberWakefulReceiver.class);
            intent.putExtra(UIProvider.MessageColumns.SUBJECT, mSubject);
            intent.putExtra(UIProvider.MessageColumns.BCC, mBCC);
            intent.putExtra(UIProvider.MessageColumns.CC, mCC);
            intent.putExtra(UIProvider.MessageColumns.ATTACHMENTS, "[]");
            intent.putExtra(UIProvider.MessageColumns.DRAFT_TYPE, 1);
            intent.putExtra(UIProvider.MessageColumns.TO, mTo);
            intent.putExtra(UIProvider.MessageColumns.BODY_HTML,
                    "<p dir=\"ltr\">" + mBody + "</p>\n");
            intent.putExtra(UIProvider.MessageColumns.BODY_TEXT, mBody);
            intent.putExtra("account.uri", mAccount.uri);
            mIntent = intent;
        }
        return mIntent;
    }

    /**
     *
     * @param id Id that will be sent to the alarm manager.
     */
    public void setBombId(int id) {
        mBombId = id;
    }

    /**
     * Get the currently specified bomb id.
     * @return Currently set bomb id
     */
    public int getBombId() {
        return mBombId;
    }
}
