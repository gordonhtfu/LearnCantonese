
package com.blackberry.pimbase.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import com.google.common.annotations.VisibleForTesting;
import java.util.Calendar;

/**
 * The following class will perform all database maintenance processing logic.
 */
public class DatabaseMaintenanceService extends PimWakefulIntentService {
    private static final String TAG = DatabaseMaintenanceService.class.getSimpleName();

    public static final long NEXT_TASK_TIME_INTERVAL_MILLIS =   24 * 60 * 60 * 1000; //1 day
    // Action used for BroadcastReceiver entry point
    public static final String ACTION_DB_MAINT_START_BROADCAST = "db_maint_start_broadcast_receiver";
    public static final String ACTION_DB_MAINT_STOP_BROADCAST = "db_maint_stop_broadcast_receiver";
    // Action used for Alarm to start service from alarmManager
    public static final String ACTION_DB_MAINT_START = "com.blackberry.ACTION_DB_MAINT_START";
    
    // Extras
    public static final String EXTRA_FORCED = "forced";
    public static final String EXTRA_PROVIDER_AUTH = "provider_auth";
    public static final String EXTRA_RESULT_VALUE = "result_value";

    // battery min values
    private static final int MIN_BATTERY_LEVEL_CHARGING = 20; // percent
    private static final int MIN_BATTERY_LEVEL_NOT_CHARGING = 80; // percent

    //will be used to ensure our handler thread ends asap when service is stopped
    boolean mIsCanceled;

    public DatabaseMaintenanceService() {
        super(DatabaseMaintenanceService.class.getName());
    }

    /**
     * Entry point for {@link BroadcastReceiver}.
     */
    public static void processStartDbMaintBroadcastIntent(Context context, Intent broadcastIntent) {
        Intent i = new Intent(context, DatabaseMaintenanceService.class);
        i.setAction(ACTION_DB_MAINT_START_BROADCAST);
        i.putExtra(Intent.EXTRA_INTENT, broadcastIntent);
        context.startService(i);
    }

    public static void processStopDbMaintBroadcastIntent(Context context,
            Intent broadcastIntent) {
        Intent i = new Intent(context, DatabaseMaintenanceService.class);
        context.stopService(i);
    }

    /**
     * scheduleDbMaintenanceTask will schedule a PendingIntent to execute at
     * NEXT_TASK_TIME_INTERVAL_MILLIS time period If there is currently a
     * PendingIntent it will be updated accordingly.
     * 
     * @param context
     * @param callingClass
     */
    public static void scheduleDbMaintenanceTask(Context context, Class<?> callingClass,
            String auth) {
        Intent intent = new Intent(ACTION_DB_MAINT_START);
        // setting up a unique mimetype for our pendingIntents
        intent.setType(context.getPackageName() + "/" + callingClass.getName());
        intent.putExtra(EXTRA_PROVIDER_AUTH, auth);
        scheduleDbMaintenanceTask(context, intent);
    }

    private static void scheduleDbMaintenanceTask(Context context, Intent intent) {
        intent.setClassName(context.getPackageName(), DatabaseMaintenanceService.class.getName());
        // Now create PendingIntent and create the Alarm
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime()
                + NEXT_TASK_TIME_INTERVAL_MILLIS,
                pendingIntent);
    }

    /**
     * hasScheduledDBMaintenanceTask call will indicate if there is currently a
     * PendingIntent scheduled
     * 
     * @param context
     * @param intent
     * @return
     */
    public static boolean hasScheduledDBMaintenanceTask(Context context, Class<?> callingClass) {
        Intent intent = new Intent(ACTION_DB_MAINT_START);
        // setting up a unique mimetype for our pendingIntents
        intent.setType(context.getPackageName() + "/" + callingClass.getName());
        intent.setClassName(context.getPackageName(), DatabaseMaintenanceService.class.getName());
        return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_NO_CREATE) != null ? true
                : false;
    }

    /**
     * cancelScheduledDBMaintenanceTask will cancel any current PendingIntents
     * scheduled for Alarm
     * 
     * @param context
     * @param callingClass
     */
    public static void cancelScheduledDBMaintenanceTask(Context context, Class<?> callingClass) {
        Intent intent = new Intent(ACTION_DB_MAINT_START);
        // setting up a unique mimetype for our pendingIntents
        intent.setType(context.getPackageName() + "/" + callingClass.getName());
        intent.setClassName(context.getPackageName(), DatabaseMaintenanceService.class.getName());

        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        pendingIntent.cancel();
        alarmManager.cancel(pendingIntent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        setCancelled(true);
    }

    protected synchronized boolean isCancelled() {
        return mIsCanceled;
    }

    private synchronized void setCancelled(boolean value) {
        mIsCanceled = value;
    }

    @Override
    protected boolean onWakefulHandleIntent(Intent intent) {
        boolean bSuccess = false;
        final String action = intent.getAction();
        Log.i(TAG, "ACTION " + action);

        if (action.equals(ACTION_DB_MAINT_START_BROADCAST) || action.equals(ACTION_DB_MAINT_START)) {
            Intent broadcastIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);

            if (broadcastIntent == null) {
                broadcastIntent = intent;
            }

            Bundle extras = broadcastIntent.getExtras();
            if (extras != null) {
                // get provider and see if it is a forced operation
                String providerAth = extras.getString(EXTRA_PROVIDER_AUTH);

                if (providerAth != null) {// required

                    if (extras.getBoolean(EXTRA_FORCED) || isMaintenanceConditionsSatified()) {
                        // do real work
                        if (providerAth != null) {
                            bSuccess = startDbMaintenance(providerAth);
                        } 
                        // re-schedule event if not successful for now,
                        // resetting the forced flag in-case this
                        // is a forced call
                        //NOTE MAY MOVE THIS INTO CP
                        broadcastIntent.putExtra(EXTRA_FORCED, false);
                        scheduleDbMaintenanceTask(this, broadcastIntent);
                    } else {
                        // conditions not met so start listening for those
                        // changes
                        new BatteryAndIdleStateReceiver(this.getApplicationContext(), broadcastIntent);
                        // will also reschedule an alarm with a force=true,
                        // as the receiver will not survive if our process is
                        // killed
                        broadcastIntent.putExtra(EXTRA_FORCED, true);
                        scheduleDbMaintenanceTask(this, broadcastIntent);
                    }
                }else{
                    Log.e(TAG, "NO AUTH_STRING");
                }
            }else{
                Log.e(TAG,"NO EXTRA_DATA");
            }
        } else {
            Log.e(TAG, "INVALID ACTION");
        }

        return bSuccess;
    }

    /**
     * Method will evaluate system conditions such as Power Level , IsCharging,
     * Memory, Idle state.
     * 
     * @return true if all our conditions are met ; false if not
     */
    @VisibleForTesting
    protected boolean isMaintenanceConditionsSatified() {
        // check if screen is Off (idle condition)
        boolean isScreenOk = isScreenConditionSatified();
        // ensure batter is good
        boolean isBatteryOk = isBatteryConditionSatified();
        return (isScreenOk && isBatteryOk);
    }

    @VisibleForTesting
    protected boolean isScreenConditionSatified() {
        PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        return pm.isScreenOn() ? false : true;
    }

    /**
     * isBatteryConditionSatified() ensures that the battery level conditions
     * are met
     * 
     * @return true if conditions to process are met; false if not
     */
    @VisibleForTesting
    protected boolean isBatteryConditionSatified() {
        // lets get battery data
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = this.registerReceiver(null, ifilter);

        // see if device is charging
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;

        // see what the current BatteryLevle is
        int currentBatteryValue = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);

        final int minBatteryLevel = isCharging
                ? MIN_BATTERY_LEVEL_CHARGING
                : MIN_BATTERY_LEVEL_NOT_CHARGING;

        return currentBatteryValue > minBatteryLevel;
    }

    /**
     * startDbMaintenance will call back into the CP using call method where the CP
     * will then process db maintenance 
     * 
     * @param providerAuth
     * @return
     */
    @VisibleForTesting
    protected boolean startDbMaintenance(String providerAuth) {
        boolean bRetValue = false;
        Log.i(TAG,"startDbMaintenance CP AUTH::" + providerAuth);
        try {
            Bundle bundle = this.getContentResolver().call(getContentUri(providerAuth), ACTION_DB_MAINT_START, null, null);
            if (bundle != null) {
                bRetValue = bundle.getBoolean(EXTRA_RESULT_VALUE);
            }
        } catch (IllegalArgumentException iae) {
            Log.e(TAG, iae.getMessage());
        }

        return bRetValue;
    }

    private Uri getContentUri(String providerAuth) {
        // keep in mind the providerAuth can contain n auths delimited by ;
        // because they target the same provider class, we only need to fire a
        // single
        // call to anyone of the valid auths
        String singleAuthString = null;
        int index = providerAuth.indexOf(';');
        if (index > 0) {
            singleAuthString = providerAuth.substring(0, index);
        } else {
            singleAuthString = providerAuth;
        }

        return Uri.parse("content://" + singleAuthString);
    }

    /**
     * In order to ensure that we at least try to execute a maintenance if one
     * of the device conditions were not met when the alarm executed, the
     * BatteryAndIdleStateReceiver will listen for these conditions and once
     * met, will start our service. It should be noted that this Receiver will
     * not survive if the process is killed
     * 
     * @TODO implement possible cancel logic when a condition that was good is
     *       no longer this will require this always running
     */
    protected class BatteryAndIdleStateReceiver extends BroadcastReceiver {
        private final Context mContext;
        private final Handler mHandler;
        private final Intent mIntent;

        public BatteryAndIdleStateReceiver(Context context, Intent intent) {
            Log.i(TAG, "BatteryAndIdleStateReceiver contr" + intent.getType());
            mContext = context;
            mIntent = intent;
            mHandler = new Handler(mContext.getMainLooper());

            register(mHandler);
        }

        protected void register(Handler handler) {
            IntentFilter intentFilter = new IntentFilter();

            if (!DatabaseMaintenanceService.this.isBatteryConditionSatified()) {
                intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
            }

            if (!DatabaseMaintenanceService.this.isScreenConditionSatified()) {
                intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
            }

            if (intentFilter.countActions() > 0) {
                mContext.registerReceiver(this, intentFilter, null, mHandler);
            } else {
                // looks like the conditions are met so just start the service
                invokeStartService(mContext, mIntent);
            }
        }

        protected void unregister() {
            mContext.unregisterReceiver(this);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, intent.getAction());
            handleOnReceive(context, intent);
        }

        @VisibleForTesting
        protected boolean handleOnReceive(Context context, Intent intent) {
            boolean bRetVal = false;
            String action = intent.getAction();

            if (areConditionsSatified(action)) {
                // start service
                Log.d(TAG, "Conditions are good " + mIntent.getType());
                bRetVal = invokeStartService(context, mIntent);
            }

            return bRetVal;
        }

        @VisibleForTesting
        protected boolean areConditionsSatified(String action) {
            boolean conditionsSatified = false;

            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                // check both conditions
                conditionsSatified = DatabaseMaintenanceService.this
                        .isMaintenanceConditionsSatified();

            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                // check battery conditions as we know the screen is off
                conditionsSatified = DatabaseMaintenanceService.this.isBatteryConditionSatified();
            }

            return conditionsSatified;
        }

        /**
         * invokeStartService
         * 
         * @param context
         * @param intent
         * @return true - used mainly for testing
         */
        @VisibleForTesting
        protected boolean invokeStartService(Context context, Intent intent) {
            DatabaseMaintenanceService.processStartDbMaintBroadcastIntent(context, intent);
            // this receiver is no longer required
            unregister();
            return true;
        }
    }
}
