package com.blackberry.email.utils;

import java.util.Timer;
import java.util.TimerTask;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import com.blackberry.email.provider.EmailProvider;
import com.blackberry.email.provider.contract.Mailbox;
import com.blackberry.email.provider.contract.EmailContent.Message;
import com.blackberry.email.provider.contract.EmailContent.MessageColumns;
import com.blackberry.common.utils.LogUtils;

public class MessageConsumerTimeManager {
    private Timer mTimer = new Timer();
    private Timer mOutgoingTimer = new Timer();
    private static Object mMutex = new Object();
    private static final long TIMEOUT_INTERVAL = 10000;

    private static MessageConsumerTimeManager sInstance;

    public static MessageConsumerTimeManager getInstance() {
        synchronized (mMutex) {
            if (sInstance == null) {
                sInstance = new MessageConsumerTimeManager();
            }

            return sInstance;
        }

    }
    
    public void addIncomingTimeoutMessageTask(Uri messageUri, Context context){
        synchronized (mMutex){
            LogUtils.w("TIMER", " adding TimerTask");
            TimeoutMessageTask task = new TimeoutMessageTask(messageUri, context);
            mTimer.schedule(task, TIMEOUT_INTERVAL);
        }
    }
    
    public void addOutgoingTimeoutMessageTask(Message msg, Mailbox mailbox, EmailProvider emailProvider, Context context){
        synchronized (mMutex){
            LogUtils.w("TIMER", " adding TimerTask");
            OutgoingTimeoutMessageTask task = new OutgoingTimeoutMessageTask(msg, mailbox, emailProvider, context);
            mOutgoingTimer.schedule(task, TIMEOUT_INTERVAL);
        }
    }

    class OutgoingTimeoutMessageTask extends TimerTask {
        Message mMsg = null;
        Context mContext = null;
        Mailbox mMailBox = null;
        EmailProvider mEmailProvider = null;

        public OutgoingTimeoutMessageTask(Message msg, Mailbox mailBox, EmailProvider emailProvider, Context context){
            mMsg = msg;
            mMailBox = mailBox;
            mContext = context;
            mEmailProvider = emailProvider;
        }
        @Override
        public void run() {
            LogUtils.w("TIMER", " **********OutgoingTimeoutMessageTask starting...");
            //for now I am not going to do any error handling, will revisit if approach is sound
            ContentValues values = new ContentValues();
            values.put(MessageColumns.FLAG_INCLUDED, 1);
            mEmailProvider.update(mMsg.getUri(), values, null, null);
            mEmailProvider.startSync(mMailBox, 0);
            
            LogUtils.w("TIMER", "**********OutgoingTimeoutMessageTask completed...");
        }
   };

    class TimeoutMessageTask extends TimerTask {

        Uri mUri = null;
        Context mContext = null;
        boolean mOutGoingMessage = false;
        
        public TimeoutMessageTask(Uri messageUri, Context context){
            mUri = messageUri;
            mContext = context;
        }
        @Override
        public void run() {
            LogUtils.w("TIMER", " **********TimeoutMessageTask starting...");
            //for now I am not going to do any error handling, will revisit if approach is sound
            ContentValues values = new ContentValues();
            values.put(MessageColumns.FLAG_INCLUDED, 1);
            mContext.getContentResolver().update(mUri, values, null, null);
            LogUtils.w("TIMER", "**********TimeoutMessageTask completed...");
        }
   };

}
