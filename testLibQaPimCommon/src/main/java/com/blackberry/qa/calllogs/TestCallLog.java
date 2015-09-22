package com.blackberry.qa.calllogs;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.provider.CallLog;
import android.util.Log;

public class TestCallLog {
    private static String TAG = TestCallLog.class.getSimpleName();
    private Context mContext;
    private int mPresentation = -1;
    private int mType = -1;
    private String mPhoneNumber = null;
    private long mDate = -1;
    private int mIsNew = -1;
    private long mDuration = -1;

    /**
     * create an uninitialized call log.  Useful if you just want a random call log or are going to
     * query to fill in the blanks
     * @param context
     */
    public TestCallLog(Context context) {
        mContext = context;
    }

    /**
     * Create a fully initialized call log
     * @param context current context
     * @param presentation call log presentation
     * @param type call log type
     * @param number phone number
     * @param date date of call
     * @param isNew if the call log has been acknowledged or not
     * @param duration how long the call log was
     */
    public TestCallLog(Context context, int presentation, int type, String number, long date,
            boolean isNew, long duration) {
        mContext = context;
        setPresentation(presentation);
        setType(type);
        setNumber(number);
        setDate(date);
        setNewState(isNew);
        setDuration(duration);
    }

    /**
     * Set the call log presentation (eg. payphone, restricted, etc).  See CallLogs.Calls for
     * constants.
     * @param presentation the presentation to set
     */
    public void setPresentation(int presentation) {
        this.mPresentation = presentation;
    }

    /**
     * Set the call log type (eg. missed).  See CallLogs.Calls for constants.
     * @param type the type to set
     */
    public void setType(int type) {
        this.mType = type;
    }

    /**
     * Set the phone number
     * @param number the phone number to set
     */
    public void setNumber(String number) {
        this.mPhoneNumber = number;
    }

    /**
     * Set the date on the log
     * @param date the date of the call log in ms since the epoch
     */
    public void setDate(long date) {
        this.mDate = date;
    }

    /**
     * Set if the call log is new
     * @param isNew if the number is new
     */
    public void setNewState(boolean isNew) {
        this.mIsNew = isNew ? 1 : 0;
    }

    /**
     * Set the call duration
     * @param duration duration of the call in seconds
     */
    public void setDuration(long seconds) {
        this.mDuration = seconds;
    }

    /**
     * Insert call logs into the device
     */
    public void insertCallLog() {
        ContentValues values = new ContentValues();
        values.put(CallLog.Calls.NUMBER, mPhoneNumber);
        values.put(CallLog.Calls.DATE, mDate);
        values.put(CallLog.Calls.DURATION, mDuration);
        values.put(CallLog.Calls.TYPE, mType);
        values.put(CallLog.Calls.NEW, mIsNew);
        values.put(CallLog.Calls.CACHED_NAME, "");
        values.put(CallLog.Calls.CACHED_NUMBER_TYPE, 0);
        values.put(CallLog.Calls.CACHED_NUMBER_LABEL, "");
        values.put(CallLog.Calls.NUMBER_PRESENTATION, mPresentation);
        Log.d(TAG, "Inserting call log placeholder for " + '\"' + mPhoneNumber + '\"');
        mContext.getContentResolver().insert(CallLog.Calls.CONTENT_URI, values);
    }

    /**
     * Sets all the instance variables to random values.
     * Date is within the last year
     * Duration is less than 2 hours
     * Type and Presentation is random
     * All are treated as new call logs.
     */
    public void randomizeCallLog() {
        Random rand = new Random();
        mPhoneNumber = String.valueOf((long) (rand.nextDouble() * 9999999999L));
        while (mPhoneNumber.length() < 10)
            mPhoneNumber = "0" + mPhoneNumber;
        long range = 1000 * 60 * 60 * 24 * 365;
        long currentTime = new Date().getTime();
        mDate = (currentTime - range) + (long)(rand.nextDouble() * (range));
        mDuration = (long) (rand.nextDouble() * 7200 + 1);
        mType = rand.nextInt(3) + 1;
        mPresentation = rand.nextInt(4) + 1;
        mIsNew = 1;
    }

    /**
     * Deletes all call logs that match the current TestCallLog.
     * @return number of call logs deleted
     */
    public int deleteCallLogs() {
        HashMap<String, String> map = getParamMap();
        String where = "";
        ArrayList<String> whereArgs = new ArrayList<String>();
        for (String param : map.keySet()) {
            if (where != "")
                where = where + " AND ";
            where = where + param + "=?";
            whereArgs.add(map.get(param));
        }
        return mContext.getContentResolver().delete(CallLog.Calls.CONTENT_URI, where,
                whereArgs.toArray(new String[map.size()]));
    }

    /**
     * Returns true if the call log already exists in the provider.
     * @return
     */
    public boolean isCallLogInProvider() {
        HashMap<String, String> map = getParamMap();
        String where = "";
        ArrayList<String> whereArgs = new ArrayList<String>();
        for (String param : map.keySet()) {
            if (where != "")
                where = where + " AND ";
            where = where + param + "=?";
            whereArgs.add(map.get(param));
        }
        Cursor c = mContext.getContentResolver().query(CallLog.Calls.CONTENT_URI, getProjection(),
                where, whereArgs.toArray(new String[map.size()]), null);
        return (c.moveToFirst());
    }

    /**
     * Builds a map out of the parameters that have been set for the test call log.
     * @return
     */
    private HashMap<String,String> getParamMap() {
        HashMap<String, String> map = new HashMap<String, String>();
        if (mPresentation != -1) {
            map.put(CallLog.Calls.NUMBER_PRESENTATION, Integer.toString(mPresentation));
        }
        if (mType != -1) {
            map.put(CallLog.Calls.TYPE, Integer.toString(mType));
        }
        if (mPhoneNumber != null) {
            map.put(CallLog.Calls.NUMBER, mPhoneNumber);
        }
        if (mDate != -1) {
            map.put(CallLog.Calls.DATE, Long.toString(mDate));
        }
        if (mIsNew != -1) {
            map.put(CallLog.Calls.NEW, Integer.toString(mIsNew));
        }
        if (mDuration != -1) {
            map.put(CallLog.Calls.DURATION, Long.toString(mDuration));
        }
        return map;
    }

    /**
     * Get the projection we use by default to access the Call Log CP.
     * @return the projection
     */
    private String[] getProjection() {
        String[] projection = {
                CallLog.Calls.NUMBER,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION,
                CallLog.Calls.TYPE,
                CallLog.Calls.NEW,
                CallLog.Calls.NUMBER_PRESENTATION
        };
        return projection;
    }

    /**
     * generate a given number of random call logs
     * @param context context
     * @param number how many call logs to generate.
     */
    public static void generateRandomCallLogs(Context context, int number) {
        TestCallLog callLog;
        for (int i = 0; i < number; i++) {
            callLog = new TestCallLog(context);
            callLog.randomizeCallLog();
            callLog.insertCallLog();
        }
    }
}
