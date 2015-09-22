package com.blackberry.qa.calendars;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Reminders;
import android.util.Log;

import com.blackberry.message.service.MessageValue;
import com.blackberry.provider.AccountContract.Account;

public class TestEvents {
    private final static String TAG = TestEvents.class.getSimpleName();

    /**
     * Account id of the email address passed in in the constructor
     */
    public Long mAccountId = null;
    /**
     * Context of the calling app/service/test
     */
    private final Context mContext;
    /**
     * Contents of the email to be found/sent
     */
    private MessageValue mEvents;

    /**
     * Initial setup for an calendar invite from the specified email address
     * @param context current context
     * @param emailAddr email address eg. example@blah.com
     * @return
     */
    public TestEvents(Context context, String emailAddr) {
        mContext = context;
        mAccountId = getAccountId(emailAddr);
    }

    /**
     * Gets the account id for the email provided
     * @param email email address eg. example@blah.com
     * @exception IllegalArgumentException if account does not exist.
     * @return account id
     */
    private long getAccountId(String email) {
        Log.d(TAG, "Getting Account ID for " + email);
        String[] accountProjection = { Account._ID, Account.TYPE, Account.NAME };
        String where = Account.NAME + "=?";
        String[] whereArgs = {email};
        Cursor accounts = null;
        Long accountId = null;
        accounts = mContext.getContentResolver().query(Account.CONTENT_URI, accountProjection,
                where, whereArgs, null);
        if (accounts != null && accounts.moveToNext()) {
            int idIdx = accounts.getColumnIndex(Account._ID);
            accountId = accounts.getLong(idIdx);
        }
        if (accounts != null)
            accounts.close();
        if (accountId == null) {
            throw new IllegalArgumentException("Email account not valid or does not exist");
        }
        Log.d(TAG, "Found account id for " + email + ": " + Long.toString(accountId));
        return accountId;
    }


    /**
     * Add a calendar event on device
     * 
     * @param context current context
     * @param calID calendar ID of the calendar account
     * @param startMillis the time the event starts in UTC millis
     * @param endMillis the time the event ends in UTC millis
     * @param title the title of event
     * @param description the description of the event
     * @return the id of the calendar event created
     *
     * TODO: This should be moved to the generic API class (test.lib.qa.pimcommon)
     */
    public static long insertEvent(Context context, long calID, long startMillis, long endMillis, String title, String description){
        // event insert
        ContentResolver cr = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(Events.DTSTART, startMillis);
        values.put(Events.DTEND, endMillis);
        values.put(Events.TITLE, title);
        values.put(Events.DESCRIPTION, description);
        values.put(Events.CALENDAR_ID, calID);
        values.put(Events.EVENT_TIMEZONE, "America/Los_Angeles");
        Uri uri = cr.insert(Events.CONTENT_URI, values);

        Log.d(TAG, "Uri of calendar created (Event)" + uri.toString());
        //assertTrue("Calendar account not added", ContentUris.parseId(uri)>0);
        return ContentUris.parseId(uri);
    }

    /**
     * Add invitees to calendar event
     * 
     * @param context current context
     * @param eventID the id of the event
     * @param attendee_name the name of attendee
     * @param attendee_email the email address of attendee
     * @return the id of the calendar attendee
     *
     * Precondition: an event is created
     * 
     * TODO: This should be moved to the generic API class (test.lib.qa.pimcommon)
     */
    public static long inviteAttendees(Context context, long eventID, String attendee_name, String attendee_email){
        ContentResolver cr = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(Attendees.ATTENDEE_NAME, attendee_name);
        values.put(Attendees.ATTENDEE_EMAIL, attendee_email);
        values.put(Attendees.ATTENDEE_RELATIONSHIP, Attendees.RELATIONSHIP_ATTENDEE);
        values.put(Attendees.ATTENDEE_TYPE, Attendees.TYPE_OPTIONAL);
        values.put(Attendees.ATTENDEE_STATUS, Attendees.ATTENDEE_STATUS_INVITED);
        values.put(Attendees.EVENT_ID, eventID);
        Uri uri = cr.insert(Attendees.CONTENT_URI, values);

        Log.d(TAG, "Uri of calendar created (Attendees): " + uri.toString());
        //assertTrue("Calendar account not added", ContentUris.parseId(uri)>0);
        return ContentUris.parseId(uri);
    }

    /**
     * Add reminder to calendar event
     *
     * @param context current context
     * @param eventID the id of the event
     * @param reminder_minutes the minutes prior to the event that the alarm would ring
     * @return the id of the calendar reminder
     *
     * Precondition: an event is created
     * 
     * TODO: This should be moved to the generic API class (test.lib.qa.pimcommon)
     */

    public static long sendReminder(Context context, long eventID, long reminder_minutes){
        ContentResolver cr = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(Reminders.MINUTES, reminder_minutes);
        values.put(Reminders.EVENT_ID, eventID);
        values.put(Reminders.METHOD, Reminders.METHOD_ALERT);
        Uri uri = cr.insert(Reminders.CONTENT_URI, values);
        Log.d(TAG, "Uri of calendar created (Reminders): " + uri.toString());
        //assertTrue("Calendar account not added", ContentUris.parseId(uri)>0);
        return ContentUris.parseId(uri);
    }

    /**
     * Return the calendar ID from on display name
     * 
     * @param context current context
     * @param display_name the display name of the calendar
     * @return the id of the calendar account
     *
     */
    public static long getCalID(Context context, String display_name){
        String calID = null;
        String[] mProjection = {
                Calendars._ID
        };
        String mSelectionClause = Calendars.CALENDAR_DISPLAY_NAME + " = ?";
        String[] mSelectionArgs = {display_name};

        Cursor c = context.getContentResolver().query(Calendars.CONTENT_URI, mProjection, mSelectionClause, mSelectionArgs, null, null);
        if(c.getCount()==1){
            if(c.moveToNext()) {
                calID = c.getString(c.getColumnIndex(Calendars._ID));
            }
        }
        //assertTrue("No calendar is found", Integer.valueOf(calID)>0);
        Log.d(TAG, "Cal ID of " + display_name + " is " + calID);
        if(calID!=null){
            return Long.valueOf(calID);
        }
        return 0;
    }
}