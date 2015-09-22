package com.blackberry.pimbase.service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import com.blackberry.common.utils.LogUtils;
import com.blackberry.pimbase.provider.PIMContentProviderBase;

public class CPMaintenanceService extends PimWakefulIntentService {

    // Action used for BroadcastReceiver entry point
    public static final String ACTION_DB_MAINT_START_BROADCAST = "db_maint_start_broadcast_receiver";
    public static final String ACTION_DB_MAINT_STOP_BROADCAST = "db_maint_stop_broadcast_receiver";
    public static final String ACTION_PACKAGE_REPLACED_BROADCAST = "package_replaced_broadcast_receiver";
    public static final String ACTION_LOCALE_CHANGED_BROADCAST = "locale_changed_broadcast_receiver";
    public static final String ACTION_NORMAL_START = "normal_start";
    // Action used for Alarm to start service from alarmManager
    public static final String ACTION_DB_MAINT_START = "com.blackberry.ACTION_DB_MAINT_START";

    private static final String CPMAINT_STATE_PREFERENCE = "pimcpm_state_pref";
    private static final String CURRENT_TASK = "current_cpmaint_task";
    protected static final String CP_LIST = "cpmaint_cp_list";

    public static final int UNKNOWN_ACTION = 0;
    public static final int DB_MAINT_ACTION = 1;
    public static final int DB_UPGRADE_ACTION = 2;
    public static final int DB_LOCALE_ACTION = 3;
    public static final int START_ACTION = 4;

    // A "friend" class that is the only one allowed to lock the Base Content Provider
    public static class CPLock {
        private CPLock() {
        }
    }
    // Make this a package variable so we can unit test
    CPLock cpLock = new CPLock();

    public CPMaintenanceService() {
        super(CPMaintenanceService.class.getName());
    }

    /**
     * processPackageReplacedBroadcast should be called when the MY_PACKAGE_REPLACED broadcast
     * is received by this application
     *
     * @param context
     * @param broadcastIntext - the raw broadcast
     * @param cps             - a list of ContentProvider Authorities to upgrade
     * @return
     */
    public static void processPackageReplacedBroadcast(Context context, Intent broadcastIntent, String[] cps) {
        LogUtils.i(LogUtils.TAG, "CPMaintenanceService - package replaced");
        Intent i = new Intent(context, CPMaintenanceService.class);
        i.setAction(ACTION_PACKAGE_REPLACED_BROADCAST);
        i.putExtra(Intent.EXTRA_INTENT, broadcastIntent);
        i.putExtra(CP_LIST, cps);
        context.startService(i);
    }

    /**
     * processStartupTask should be called when the very first ContentProvider is created
     * is received by this application
     *
     * @param context
     * @param broadcastIntext - the raw broadcast
     * @param cps             - a list of ContentProvider Authorities to upgrade
     * @return
     */
    public static void processStartupTask(Context context) {
        LogUtils.i(LogUtils.TAG, "CPMaintenanceService - startup of first ContentProvider");
        Intent i = new Intent(context, CPMaintenanceService.class);
        i.setAction(ACTION_NORMAL_START);
        context.startService(i);
    }

    /**
     * onWakefulHandleIntent called by the WakefullIntent service any time service is called
     *
     * @param intext - the intent we need to handle
     * @return boolean
     */
    @Override
    protected boolean onWakefulHandleIntent(Intent intent) {
        LogUtils.i(LogUtils.TAG, "CPMaintenanceService - onWakefulHandleIntent for %s", intent.getAction());
        boolean success = false;
        int action = getAction(intent.getAction());
        switch (action) {
        case DB_MAINT_ACTION:
            break;
        case DB_UPGRADE_ACTION:
            success = doDbUpgrade(intent);
            break;
        case DB_LOCALE_ACTION:
            break;
        case START_ACTION:
            success = doStartupAction(readActionState());
            break;
        }
        if (success) {
            // Overwrite the preference object to indicate we are not in the middle of a task
            writeActionState(UNKNOWN_ACTION, null);
        }
        LogUtils.i(LogUtils.TAG, "Current Maintenance state value is %d", readActionState());
        return success;
    }

    /**
     * doStartupAction - during startup, check if a previous task was not finished
     *
     * @param action - last action performed
     * @return always returns true
     */
    protected boolean doStartupAction(int lastAction) {
        LogUtils.i(LogUtils.TAG, "Last Maintenance state value is %d", lastAction);
        if (lastAction != UNKNOWN_ACTION) {
            queueUnfinishedTask(lastAction);
        }
        return true;
    }

    /**
     * queueUnfinishedTask - If the last action did not complete the queue up again
     *
     * @param lastAction - the last action saved to the prefs file
     * @return N/A
     */
    protected void queueUnfinishedTask(int lastAction) {
        switch(lastAction) {
        case DB_UPGRADE_ACTION:
            // Read in the list of authorities
            String[] cps = getCPListFromSharedPrefs();
            // Schedule the upgrade task
            if (cps != null) {
                LogUtils.w(LogUtils.TAG, "Previous Database upgrade task failed:");
                for (String cp: cps) {
                    LogUtils.i(LogUtils.TAG, "   upgrading CP: %s", cp);
                }
                processPackageReplacedBroadcast(getApplicationContext(), null, cps);
            } else {
                LogUtils.i(LogUtils.TAG, "Last task was a db upgrade but could not find CP list");
            }
            break;
        }
    }

    /**
     * getAction - convert the String Intent to an integer action
     *
     * @param action - a String action Intent
     * @return associated integer representation of the action
     */
    public static int getAction(final String action) {
        int result = UNKNOWN_ACTION;
        if (action.equals(ACTION_DB_MAINT_START_BROADCAST) || action.equals(ACTION_DB_MAINT_START)) {
            result = DB_MAINT_ACTION;
        } else if (action.equals(ACTION_PACKAGE_REPLACED_BROADCAST)) {
            result = DB_UPGRADE_ACTION;
        } else if (action.equals(ACTION_LOCALE_CHANGED_BROADCAST)) {
            result = DB_LOCALE_ACTION;
        } else if (action.equals(ACTION_NORMAL_START)) {
            result = START_ACTION;
        }
        return result;
    }

    /**
     * doDbUpgrade is triggered by someone calling the static method
     * "processPackageReplacedBroadcast"
     *
     * @param cps - a String array containing all the ContentProvider authorities to upgrade
     * @return
     */
    protected boolean doDbUpgrade(final Intent intent) {
        boolean success = false;
        try {
            // First set the global lock on the Base ContentProvider
            LogUtils.i(LogUtils.TAG, "Package has been replaced, perform database upgrades...");
            //PIMContentProviderBase.setMaintenanceLock(cpLock, true);

            // Get the list of content authorities from the Intent extras
            Bundle extras = intent.getExtras();
            String[] cps = extras.getStringArray(CP_LIST);
            if (cps == null) {
                // Nothing to upgrade?
                return success;
            }
            // Write out our current task information to a preference file
            writeActionState(DB_UPGRADE_ACTION, cps);

            // Lock all providers, then upgrade, then unlock
            lockProviders(cps);
            success = upgradeProviders(cps);
            unlockProviders(cps);

        } catch (Exception e) {
            LogUtils.e(LogUtils.TAG, "Database upgrade exception: %s", e.getMessage());
        } finally {
            // Unlock the Base Content Provider
            //PIMContentProviderBase.setMaintenanceLock(cpLock, false);
        }
        LogUtils.i(LogUtils.TAG, "Package has been replaced, perform database upgrades...done");
        return success;
    }

    /**
     * lockProviders - wrap the call to PIMContentProviderBase to allow override
     *
     * @param cps - a String array containing the list of CPs to contact
     * @return void
     */
    protected void lockProviders(final String[] cps) {
        PIMContentProviderBase.lockProviders(cpLock, cps, getContentResolver());
    }
    /**
     * unlockProviders - wrap the call to PIMContentProviderBase to allow override
     *
     * @param cps - a String array containing the list of CPs to contact
     * @return void
     */
    protected void unlockProviders(final String[] cps) {
        PIMContentProviderBase.unlockProviders(cpLock, cps, getContentResolver());
    }
    /**
     * upgradeProviders - wrap the call to PIMContentProviderBase to allow override
     *
     * @param cps - a String array containing the list of CPs to contact
     * @return boolean - success/failure
     */
    protected boolean upgradeProviders(final String[] cps) {
        return PIMContentProviderBase.upgradeProviders(cpLock, cps, getContentResolver());
    }

//    /**
//     * sendDBUpgradeRequest - extract Android call to enable unit testing of other methods
//     *
//     * @param cp - a String array containing a single ContentProvider authority
//     * @return
//     */
//    protected Bundle sendDBUpgradeRequest(final String cpAuthority) {
//        return getContentResolver().call(Uri.parse("content://" + cpAuthority),
//                PIMContentProviderBase.PIMBCP_DB_UPGRADE, null, null);
//    }

    // **** The following methods read/write state to a shared preference file.  Currently there is
    // **** only one task that writes state (DB_UPGRADE_ACTION)
    // NOTE: since the following 3 methods are quite simple and involve Android system code
    //       it is impractical (and unnecessary) to create unit tests.
    /**
     * readActionState - read in state from preference file
     *
     * @return current action from the preference file
     */
    protected int readActionState() {
        int currentAction = UNKNOWN_ACTION;
        try {
            SharedPreferences prefs = getApplicationContext().
                    getSharedPreferences(CPMAINT_STATE_PREFERENCE, MODE_PRIVATE);
            if (prefs != null) {
                currentAction= prefs.getInt(CURRENT_TASK, UNKNOWN_ACTION);
            }
        } catch (Exception e) {
            LogUtils.e(LogUtils.TAG, "Unable to get CP Maint action: %s", e.getMessage());
        }
        return currentAction;
    }

    /**
     * getActionState - write out some state to a preference file
     *
     * @return current action from the preference file
     */
    protected String[] getCPListFromSharedPrefs() {
        try {
            SharedPreferences prefs = getApplicationContext().
                    getSharedPreferences(CPMAINT_STATE_PREFERENCE, MODE_PRIVATE);
            if (prefs != null) {
                Set<String> cps = prefs.getStringSet(CP_LIST, null);
                if (cps != null) {
                    // Convert the set to an array of String objects
                    return cps.toArray(new String[cps.size()]);
                }
            }
        } catch (Exception e) {
            LogUtils.e(LogUtils.TAG, "Cannot get ContentProvider list from preferences: %s",
                    e.getMessage());
        }
        return null;
    }

    /**
     * writeActionState - write out some state to a preference file
     *
     * @param action - integer
     * @return N/A
     */
    protected void writeActionState(int action, String[] cps) {
        try {
            SharedPreferences prefs = getApplicationContext().
                    getSharedPreferences(CPMAINT_STATE_PREFERENCE, MODE_PRIVATE);
            SharedPreferences.Editor editPrefs = prefs.edit();
            editPrefs.putInt(CURRENT_TASK, action);
            if (cps != null) {
                // Save the list of ContentProvider authority strings to the shared pref
                Set<String> mySet = new HashSet<String>(Arrays.asList(cps));
                editPrefs.putStringSet(CP_LIST, mySet);
            }
            editPrefs.commit();
        } catch (Exception e) {
            LogUtils.e(LogUtils.TAG, "Unable to save CP Maint action: %s", e.getMessage());
        }
    }
}
