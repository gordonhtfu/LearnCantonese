
package com.blackberry.pimbase.provider;

import android.annotation.TargetApi;
import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.pm.ProviderInfo;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.util.Log;

import com.blackberry.common.utils.LogUtils;
import com.blackberry.pimbase.BuildConfig;
import com.blackberry.pimbase.service.CPMaintenanceService;
import com.blackberry.pimbase.service.DatabaseMaintenanceService;

import java.util.ArrayList;

/**
 * Android content Provider base class by which all PIM Content Providers should
 * be derived from. Implements functionality to support upgrade, backup/
 * restore, sqlite database optimizations, and other common sqlite based Content
 * Provider functionality. Derived classes must implement the pimXXX() methods
 * defined as abstract below. Note it was desired to do a containment design
 * approach versus this inheritance approach, but each CP must have a unique
 * class definition / registration in Android, and Java templates do not have
 * runtime uniqueness ( only compile time ).So inheritance design pattern is
 * used. As the above features are implemented this base class impl will become
 * larger, possibly requiring additional utility classes to be implemented. The
 * goal is to keep this base class small, and adding some features as optional
 * use by derived classes. Please see the test.lib.pimbase project for the unit
 * test of this class.
 * 
 * @author fjudge
 */
public abstract class PIMContentProviderBase extends ContentProvider {
    private static final String TAG = "PIMBCP";

    public static final String NOTIFCATION_QUERY_PARAM_OP_KEY = "operation";
    /** Query value for the notification URI for delete operations */
    public static final String NOTIFICATION_OP_DELETE = "delete";
    /** Query value for the notification URI for insert operations */
    public static final String NOTIFICATION_OP_INSERT = "insert";
    /** Query value for the notification URI for update operations */
    public static final String NOTIFICATION_OP_UPDATE = "update";

    // DB Maintenance methods
    public static final String PIMBCP_DB_UPGRADE = "pimbcp_db_upgrade";
    protected static final String PIMBCP_DB_LOCK = "pimbcp_db_lock";
    protected static final String PIMBCP_DB_UNLOCK = "pimbcp_db_unlock";

    /***
     * Private data, particularly locked. Note it is assumed this value is set
     * by querying a central service or due to central services notifying us to
     * be locked. Similarly set to false, unlocked, in same manner.
     */
    private static boolean sLock;
    private ThreadLocal<ArrayList<Uri>> mBatchNotifications =
        new ThreadLocal<ArrayList<Uri>>();

    protected enum OpenMode { READ, WRITE }

    // *** ContentProvider ABSTRACT METHODS ***
    /***
     * The first group of abstract methods are those that coming from the ContentProvider
     */
    protected abstract void pimShutdown();
    protected abstract boolean pimOnCreate();
    protected abstract int pimDelete(Uri uri, String sel, String[] sArgs);
    protected abstract Uri pimInsert(Uri uri, ContentValues vals);
    protected abstract Cursor pimQuery(Uri uri, String[] proj, String sel, String[] selArgs,
                                       String ord);
    protected abstract int pimUpdate(Uri uri, ContentValues cv, String sel, String[] sArgs);
    protected abstract Bundle pimCall(String method, String arg, Bundle extras);

    // *** Additional ABSTRACT METHODS ***
    /**
     * getDatabaseHelpers - get an array of helpers from the sub-class
     *
     * @param includeAttachedDbs - boolean, in very specific cases the returned list should
     *        include helpers that hold attached databases.  Currently the only case where
     *        this is true is during some database maintenance.  During normal operation
     *        sub-classes should NOT return helpers for attached databases.
     * @return array of SQLiteOpenHelper
     */
    protected abstract SQLiteOpenHelper[] getDatabaseHelpers(boolean includeAttachedDbs);

    /**
     * initializeDatabaseHelpers - get the implementer to create any DBHelpers
     * <p>
     * This method should ONLY be called by the base provider.  Implementers should NOT
     * need to nor should then call this method within the implementation class.
     * @return void
     */
    protected abstract void initializeDatabaseHelpers();

    /**
     * closeAllDatabases - abstract method to indicate that all databases should be closed
     * <p>
     * The implementer should close every database it controls and reset any cached
     * database objects to null.
     * @return N/A
     */
    protected abstract void closeAllDatabases();

    /**
     * getWritableDatabase/getReadableDatabase
     * <p>
     * The implementer can use a cached database, an attached database, and do whatever
     * while getting the database.  The implementer should NEVER open the database in the
     * pimOnCreate method.
     * @return N/A
     */
    protected abstract SQLiteDatabase getWritableDatabase();
    protected abstract SQLiteDatabase getReadableDatabase();

    /**
     * getDatabase - the one and only method implementers should call to get the database
     * <p>
     * Depending on the open mode the call is passed to abstract methods getReadableDatabase or
     * getWritableDatabase.
     * @return N/A
     */
    protected SQLiteDatabase getDatabase(OpenMode mode) {
        if (mode == OpenMode.READ) {
            return getReadableDatabase();
        } else if (mode == OpenMode.WRITE) {
            return getWritableDatabase();
        } else {
            return null;
        }
    }
    // Note derived class must still implement CP getType() method

    @Override
    public boolean onCreate() {
        // The onCreate method is not locked.  Provider implementations are NEVER
        // supposed to actually open the database
        boolean result = pimOnCreate();
        initializeDatabaseHelpers();
        return result;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        Cursor retval = null;

        if (!sLock) {
            if (BuildConfig.DEBUG) {
                retval = pimQuery(uri, projection, selection, selectionArgs, sortOrder);
            } else {
                try {
                    retval = pimQuery(uri, projection, selection, selectionArgs, sortOrder);
                } catch (Exception e) {
                    retval = null;
                    Log.e(TAG, e.getMessage());
                }
            }
        }
        return retval;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Uri retval = null;

        if (!sLock) {
            if (BuildConfig.DEBUG) {
                retval = pimInsert(uri, values);
            } else {
                try {
                    retval = pimInsert(uri, values);
                } catch (Exception e) {
                    retval = null;
                    Log.e(TAG, e.getMessage());
                }
            }
        }
        return retval;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int retval = 0;

        if (!sLock) {
            if (BuildConfig.DEBUG) {
                retval = pimDelete(uri, selection, selectionArgs);
            } else {
                try {
                    retval = pimDelete(uri, selection, selectionArgs);
                } catch (Exception e) {
                    retval = 0;
                    Log.e(TAG, e.getMessage());
                }
            }
        }
        return retval;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        int retval = 0;

        if (!sLock) {
            if (BuildConfig.DEBUG) {
                retval = pimUpdate(uri, values, selection, selectionArgs);
            } else {
                try {
                    retval = pimUpdate(uri, values, selection, selectionArgs);
                } catch (Exception e) {
                    retval = 0;
                    Log.e(TAG, e.getMessage());
                }
            }
        }
        return retval;
    }

    @Override
    public void attachInfo(Context context, ProviderInfo info) {
        super.attachInfo(context, info);

        if (isDbMaintenanceEnabled()) {
            ensureDbMaintenanceScheduled(info.authority);
        }

    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        Bundle bundleResult = null;
        boolean success;

        if (sLock) {
            LogUtils.i(LogUtils.TAG, "Base CP is currently locked: command=%s, thisCP=%s", method, getClass().getName());
            // There are only a limited number of methods that are allowed when locked
            bundleResult = new Bundle();
            if (method.equals(PIMContentProviderBase.PIMBCP_DB_UPGRADE)) {
                // The package has been replaced and we are currently in a locked state
                // Inform this particular CP to do a schema upgrade
                success = upgradeDatabases(getDatabaseHelpers(true));
                bundleResult.putBoolean(Intent.EXTRA_RETURN_RESULT, success);
            } else if (method.equals(PIMContentProviderBase.PIMBCP_DB_UNLOCK)) {
                // We are being told to unlock the provider
                // TODO - Carl - how do we stop anyone from locking our provider?
                LogUtils.i(LogUtils.TAG, "Unlocking the base provider");
                PIMContentProviderBase.sLock = false;
            } else {
                // Just in case someone tries to do a call during while we are locked, return false
                bundleResult.putBoolean(DatabaseMaintenanceService.EXTRA_RESULT_VALUE, false);
            }
        } else {
            if (method.equals(PIMContentProviderBase.PIMBCP_DB_LOCK)) {
                // We are being told to lock the provider
                // TODO - Carl - how do we stop anyone from locking our provider?
                LogUtils.i(LogUtils.TAG, "Locking the base provider, thisCP=%s", getClass().getName());
                PIMContentProviderBase.sLock = true;
            } else if (method.equals(DatabaseMaintenanceService.ACTION_DB_MAINT_START)
                    && isDbMaintenanceEnabled()) {
                bundleResult = new Bundle();
                success = handleStartDbMaintenance(getDatabaseHelpers(true));
                bundleResult.putBoolean(DatabaseMaintenanceService.EXTRA_RESULT_VALUE, success);
            } else {
                bundleResult = pimCall(method, arg, extras);
            }
        }
        return bundleResult;
    }

    /**
     * Wrap batch operations inside a database transaction. Issue all notifications only if the
     * transaction completes successfully.
     *
     * The notifications are collected during the calls to notifyUI() by way of the update(), insert()
     * and delete() calls made by the derived class.
     */

    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {

        SQLiteOpenHelper[] dbHelpers = getDatabaseHelpers(false);

        mBatchNotifications.set(new ArrayList<Uri>());

        // Get a list of the writable databases used by the derived class. We'll use this list to
        // begin/mark/end transactions.

        ArrayList<SQLiteDatabase> dbs = new ArrayList<SQLiteDatabase>();

        for (int i = 0; i < dbHelpers.length; i++) {
            SQLiteDatabase db = dbHelpers[i].getWritableDatabase();
            if (db != null) {
                dbs.add(db);
            }
        }

        try {
            // Start a transaction on all the writable databases.

            for (SQLiteDatabase db: dbs) {
                if (db.isWriteAheadLoggingEnabled()) {
                    db.beginTransactionNonExclusive();
                } else {
                    db.beginTransaction();
                }
            }

            ContentProviderResult[] results = super.applyBatch(operations);

            for (SQLiteDatabase db: dbs) {
                db.setTransactionSuccessful();
            }

            // End the transactions in reverse order.

            for (int i = dbs.size() - 1; i >= 0; i--) {
                dbs.get(i).endTransaction();
            }

            // Send all the notifications now that the transaction has completed successfully. In
            // future we might want to send a subset of the notifications to improve
            // performance. For now we preserve the number and the order (helps with testing too!).

            final ArrayList<Uri> notifications = mBatchNotifications.get();
            mBatchNotifications.remove();

            for (final Uri uri : notifications) {
                getContext().getContentResolver().notifyChange(uri, null);
            }

            return results;

        } finally {
            mBatchNotifications.remove();
        }
    }

    /**
     * shutdown - base override, calls pimShutdown
     */
    @Override
    public void shutdown() {
        pimShutdown();
    }

    /**
     * isDbMaintenanceEnabled indicates if the CP wants to have maintenance
     * executed on its' databases. Database Maintenance will be on by default
     * and if any CP wants to opt-out they will need to override this method
     * 
     * @return true if maintenance it to be run; otherwise false.
     */
    protected boolean isDbMaintenanceEnabled() {
        return true;
    }

    /**
     * ensureDbMaintenanceScheduled will ensure that a database maintenance task
     * is scheduled for execution
     */
    private void ensureDbMaintenanceScheduled(String providerAuth) {
        if (providerAuth != null &&
                !DatabaseMaintenanceService.hasScheduledDBMaintenanceTask(this.getContext(),
                        this.getClass())) {

            DatabaseMaintenanceService.scheduleDbMaintenanceTask(this.getContext(),
                    this.getClass(), providerAuth);
        }
    }

    protected boolean handleStartDbMaintenance(SQLiteOpenHelper[] dbHelpers) {
        boolean retValue = true;

        if (dbHelpers != null) {
            for (int x = 0; x < dbHelpers.length; x++) {
                SQLiteDatabase db = null;
                try {
                    db = dbHelpers[x].getWritableDatabase();
                    // just doing it like this for now
                    db.execSQL("ANALYZE");
                    db.execSQL("VACUUM");
                    Log.i(TAG, "DB Maint Done On::" + db.getPath());
                } catch (SQLException ex) {
                    Log.e(TAG, ex.getMessage());
                    retValue = false;
                }
            }
        }

        return retValue;
    }

    /**
     * Trigger the opening of a database so that the schema will get upgraded
     */
    protected boolean upgradeDatabases(SQLiteOpenHelper[] dbHelpers) {
        // Close all of the databases associated with this CP
        closeAllDatabases();
        // Now "touch" then all - basically opening then up with the helper
        boolean retValue = false;
        if (dbHelpers != null) {
            for (int x = 0; x < dbHelpers.length; x++) {
                SQLiteOpenHelper helper = dbHelpers[x];
                if (helper == null) {
                    retValue = false;
                    continue;
                } else {
                    retValue = touchDatabase(helper);
                    if (!retValue) {
                        LogUtils.e(LogUtils.TAG,
                                "Unable to get a writable database for upgrade");
                    }
                }
            }
        }
        // Now close them all again
        closeAllDatabases();
        return retValue;
    }

    // The touch method's purpose is to simply open the database which will call
    // the various "helper" methods if needed (such as onUpgrade)
    private boolean touchDatabase(SQLiteOpenHelper helper) {
        if (helper != null) {
            try {
                // Getting a writable database will cause it to be opened which will also cause
                // an onUpgrade call if necessary - this is the desired result.
                SQLiteDatabase db = helper.getWritableDatabase();
                if (db != null) {
                    LogUtils.i(LogUtils.TAG, "DB upgrade complete on %s", db.getPath());
                    return true;
                } else {
                    return false;
                }
            } catch (Exception e) {
                LogUtils.e(LogUtils.TAG, "Exception getting writable database: %s", e.getMessage());
                return false;
            }
        }
        // If we get here then we were unable to open open the database
        return false;
    }
    /**
     * Sends a change notification to any cursors observers of the given base
     * URI. The final notification URI is dynamically built to contain the
     * specified information. It will be of the format
     * <<baseURI>>/<<id>>?operation=<<op>>; where <<op>> and <<id>> are optional
     * depending upon the given values. 
     * 
     * NOTE: If <<op>> is specified,
     * notifications for <<baseURI>>/<<id>> will NOT be invoked. If this is
     * necessary, it can be added. However, due to the implementation of
     * {@link ContentObserver}, observers of <<baseURI>> will receive multiple
     * notifications.
     * 
     * @param baseUri The base URI to send notifications to. Must be able to
     *            take appended IDs.
     * @param op Optional operation to be appended to the URI
     *            (insert/update/delete for now)
     * @param id If a positive value, the ID to append to the base URI.
     *            Otherwise, no ID will be appended to the base URI.
     */
    protected Uri sendNotifierChange(Uri baseUri, String operationValue, String id) {
        if (baseUri == null) {
            return null;
        }

        if (id != null) {
            try {
                long longId = Long.valueOf(id);

                if (longId > 0) {
                    baseUri = ContentUris.withAppendedId(baseUri, longId);
                }
            } catch (NumberFormatException ignore) {
            }
        }

        return sendNotifierChange(baseUri, operationValue);
    }

    /**
     * Sends a change notification to any cursors observers he final
     * notification URI is dynamically built to contain the specified
     * information. It will be of the format <<uri>>?operation=<<op>>
     * 
     * @param uri
     * @param op
     */
    protected Uri sendNotifierChange(Uri uri, String operationValue) {

        if (uri == null) {
            return null;
        }

        if (operationValue != null) {
            uri = uri.buildUpon()
                    .appendQueryParameter(NOTIFCATION_QUERY_PARAM_OP_KEY, operationValue).build();
        }

        notifyChange(uri, null);

        return uri;
    }

    /**
     * Intercept the notifications so they can be deferred if we're currently in a batch operation.
     */

    protected void notifyChange (Uri uri, ContentObserver observer) {
        final ArrayList<Uri> notifications = mBatchNotifications.get();
        if (notifications != null) {
            notifications.add(uri);
        } else {
            getContext().getContentResolver().notifyChange(uri, null);
        }
    }

    /**
     * Combine id with user-provided selection
     *
     * @param id
     * @param selection user-provided selection, may be null
     * @return a single selection string
     */
    protected static String whereWithId(String id, String selection) {
        StringBuilder sb = new StringBuilder(256);
        sb.append(BaseColumns._ID);
        sb.append("=");
        sb.append(id);
        if (selection != null) {
            sb.append(" AND (");
            sb.append(selection);
            sb.append(')');
        }
        return sb.toString();
    }

    /**
     * Combine a locally-generated selection with a user-provided selection This
     * introduces risk that the local selection might insert incorrect chars
     * into the SQL, so use caution.
     * 
     * @param where locally-generated selection, must not be null
     * @param selection user-provided selection, may be null
     * @return a single selection string
     */
    protected static String whereWith(String where, String selection) {
        if (selection == null) {
            return where;
        }
        StringBuilder sb = new StringBuilder(where);
        sb.append(" AND (");
        sb.append(selection);
        sb.append(')');

        return sb.toString();
    }

    /**
     * Return the package name of the caller that initiated the request being
     * processed on the current thread. The returned package will have been
     * verified to belong to the calling UID.
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public String getCallingPackageName() {
        String retPackageName = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            retPackageName = this.getCallingPackage();
        } else {
            int uid = Binder.getCallingUid();

            String[] packages = getContext().getPackageManager().getPackagesForUid(uid);

            if (packages != null && packages.length > 0) {
                retPackageName = packages[0];
            }
        }

        return retPackageName;
    }

    /**
     * upgradeProviders - send a PIMBCP_DB_UPGRADE to all providers in the array
     * <p>
     * (simulated "friend" method) that can only be called by a class
     * that can create a CPMaintenanceService::CPLock class.  Currently that is limited
     * to the CPMaintenanceService class.
     *
     * @param l    - CPMaintenanceService.CPLock object
     * @param cps  - list of ContentProvides to lock
     * @param cr   - ContentResolver to use (since this is a static method)
     * @return N/A
     */
    public static boolean upgradeProviders(CPMaintenanceService.CPLock l, final String[] cps, ContentResolver cr) {
        if (l != null && l instanceof CPMaintenanceService.CPLock && cps != null) {
            boolean success = true;
            // Go through our list of content providers 1 by 1 and do the upgrade call
            for (String cp: cps) {
                LogUtils.i(LogUtils.TAG, "Send DB UPGRADE request to %s", cp);
                try {
                    Bundle result = cr.call(Uri.parse("content://" + cp),
                            PIMContentProviderBase.PIMBCP_DB_UPGRADE, null, null);
                    if (result == null || result.getBoolean(Intent.EXTRA_RETURN_RESULT, true) == false) {
                        success = false;
                    }
                } catch (Exception e) {
                    LogUtils.w(LogUtils.TAG, "Exception upgrading CP via call command: %s, %s", cp, e.getMessage());
                }
            }
            return success;
        } else {
            LogUtils.w(LogUtils.TAG, "setMaintenanceLock invalid CPLock object");
            return false;
        }
    }

    /**
     * lockProviders - send a PIMBCP_DB_LOCK to all providers in the array
     * <p>
     * (simulated "friend" method) that can only be called by a class
     * that can create a CPMaintenanceService::CPLock class.  Currently that is limited
     * to the CPMaintenanceService class.
     *
     * @param l    - CPMaintenanceService.CPLock object
     * @param cps  - list of ContentProvides to lock
     * @param cr   - ContentResolver to use (since this is a static method)
     * @return N/A
     */
    public static void lockProviders(CPMaintenanceService.CPLock l, final String[] cps, ContentResolver cr) {
        if (l != null && l instanceof CPMaintenanceService.CPLock && cps != null) {
            setProviderLockState(true, cps, cr);
        } else {
            LogUtils.w(LogUtils.TAG, "setMaintenanceLock invalid CPLock object");
        }
    }
    /**
     * unlockProviders - send a PIMBCP_DB_UNLOCK to all providers in the array.
     * <p>
     * (simulated "friend" method) that can only be called by a class
     * that can create a CPMaintenanceService::CPLock class.  Currently that is limited
     * to the CPMaintenanceService class.
     *
     * @param l    - CPMaintenanceService.CPLock object
     * @param cps  - list of ContentProvides to lock
     * @param cr   - ContentResolver to use (since this is a static method)
     * @return N/A
     */
    public static void unlockProviders(CPMaintenanceService.CPLock l, final String[] cps, ContentResolver cr) {
        if (l != null && l instanceof CPMaintenanceService.CPLock && cps != null) {
            setProviderLockState(false, cps, cr);
        } else {
            LogUtils.w(LogUtils.TAG, "setMaintenanceLock invalid CPLock object");
        }
    }
    private static void setProviderLockState(boolean lockState, final String[] cps, ContentResolver cr) {
        String lockCommand = null;
        if (lockState) {
            // Set the global lock - only affects CPs in the current process
            PIMContentProviderBase.sLock = true;
            lockCommand = PIMBCP_DB_LOCK;
        } else {
            // Clear the global lock - only affects CPs in the current process
            PIMContentProviderBase.sLock = false;
            lockCommand = PIMBCP_DB_UNLOCK;
        }
        for (String cp: cps) {
            // Send the specified command to any CPs not in our process
            LogUtils.i(LogUtils.TAG, "Sending %s command to CP %s", lockCommand, cp);
            try {
                cr.call(Uri.parse("content://" + cp), lockCommand, null, null);
            } catch (Exception e) {
                LogUtils.e(LogUtils.TAG, "Exception running call command on %s, %s", cp, e.getMessage());
            }
        }
    }
    /**
     * setMaintenanceLock (simulated "friend" method) that can only be called by a class
     * that can create a CPMaintenanceService::CPLock class.  Currently that is limited
     * to the CPMaintenanceService class.
     *
     * @param l    - CPMaintenanceService.CPLock object
     * @param lock - boolean value for the lock
     * @return N/A
     */
    public static void setMaintenanceLock(CPMaintenanceService.CPLock l, boolean lock) {
        if (l != null && l instanceof CPMaintenanceService.CPLock) {
            PIMContentProviderBase.sLock = lock;
        } else {
            LogUtils.w(LogUtils.TAG, "setMaintenanceLock invalid CPLock object");
        }
    }
    /**
     * getMaintenanceLock - get the current value of the maintenance lock
     *
     * @return boolean - value of _lock
     */
    public static boolean getMaintenanceLock() {
        return PIMContentProviderBase.sLock;
    }
}
