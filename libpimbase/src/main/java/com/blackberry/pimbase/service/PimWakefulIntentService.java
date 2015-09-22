
package com.blackberry.pimbase.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

/**
 * PimWakefulIntentService This class will ensure that the executing service
 * will remain wakeful while executing. It is only to be used if the service
 * needs to ensure it has CPU when the device is going into sleep mode aka needs
 * to run when device is in sleep mode. It is also only to be used if the
 * service is not started using a WakefulBroadcastReceiver
 */
public abstract class PimWakefulIntentService extends IntentService {
    private static final String TAG = PimWakefulIntentService.class.getSimpleName();

    private static volatile WakeLock sWakeLock = null;

    synchronized private static WakeLock getLock(Context context) {
        if (sWakeLock == null) {
            PowerManager mgr = (PowerManager) context
                    .getSystemService(Context.POWER_SERVICE);
            sWakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
            sWakeLock.setReferenceCounted(true);
        }
        return sWakeLock;
    }

    public PimWakefulIntentService(String name) {
        super(name);
    }

    /**
     * This method is invoked on the worker thread with a request to process.
     * This method will hold a wakelock for its duration, and will release it
     * when done. Only one Intent is processed at a time, but the processing
     * happens on a worker thread that runs independently from other application
     * logic. So, if this code takes a long time, it will hold up other requests
     * to the same IntentService, but it will not hold up anything else. When
     * all requests have been handled, the IntentService stops itself, so you
     * should not call {@link #stopSelf}.
     * 
     * @param intent The value passed to
     *            {@link android.content.Context#startService(Intent)}.
     * @return true if execution was successful; false if not - note the return
     *         value is used more for unit-testing than anything else
     */
    protected abstract boolean onWakefulHandleIntent(Intent intent);

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        WakeLock lock = getLock(this);
        if (!lock.isHeld() || (flags & START_FLAG_REDELIVERY) != 0) {
            lock.acquire();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            onWakefulHandleIntent(intent);
        } finally {
            // make sure we release the wake lock
            PowerManager.WakeLock lock = getLock(this);
            if (lock.isHeld()) {
                lock.release();
            }
        }

    }

}
