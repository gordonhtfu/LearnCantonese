package com.blackberry.email.preferences;




import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.blackberry.email.provider.contract.Account;
import com.blackberry.common.Logging;
import com.blackberry.common.utils.LogUtils;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Preferences {

    // Preferences file
    public static final String PREFERENCES_FILE = "AndroidMail.Main";

    // Preferences field names
    private static final String ACCOUNT_UUIDS = "accountUuids";
    private static final String ENABLE_DEBUG_LOGGING = "enableDebugLogging";
    private static final String ENABLE_EXCHANGE_LOGGING = "enableExchangeLogging";
    private static final String ENABLE_EXCHANGE_FILE_LOGGING = "enableExchangeFileLogging";
    private static final String INHIBIT_GRAPHICS_ACCELERATION = "inhibitGraphicsAcceleration";
    private static final String FORCE_ONE_MINUTE_REFRESH = "forceOneMinuteRefresh";
    private static final String ENABLE_STRICT_MODE = "enableStrictMode";
    private static final String DEVICE_UID = "deviceUID";
    private static final String ONE_TIME_INITIALIZATION_PROGRESS = "oneTimeInitializationProgress";
    private static final String AUTO_ADVANCE_DIRECTION = "autoAdvance";
    private static final String TEXT_ZOOM = "textZoom";
    private static final String BACKGROUND_ATTACHMENTS = "backgroundAttachments";
    private static final String TRUSTED_SENDERS = "trustedSenders";
    private static final String LAST_ACCOUNT_USED = "lastAccountUsed";
    private static final String REQUIRE_MANUAL_SYNC_DIALOG_SHOWN = "requireManualSyncDialogShown";
    private static final String CONFIRM_DELETE = "confirm_delete";
    private static final String CONFIRM_SEND = "confirm_send";
    @Deprecated
    private static final String SWIPE_DELETE = "swipe_delete";
    private static final String CONV_LIST_ICON = "conversation_list_icons";
    @Deprecated
    private static final String REPLY_ALL = "reply_all";

    public static final int AUTO_ADVANCE_NEWER = 0;
    public static final int AUTO_ADVANCE_OLDER = 1;
    public static final int AUTO_ADVANCE_MESSAGE_LIST = 2;
    // "move to older" was the behavior on older versions.
    private static final int AUTO_ADVANCE_DEFAULT = AUTO_ADVANCE_OLDER;
    private static final boolean CONFIRM_DELETE_DEFAULT = false;
    private static final boolean CONFIRM_SEND_DEFAULT = false;

    // The following constants are used as offsets into R.array.general_preference_text_zoom_size.
    public static final int TEXT_ZOOM_TINY = 0;
    public static final int TEXT_ZOOM_SMALL = 1;
    public static final int TEXT_ZOOM_NORMAL = 2;
    public static final int TEXT_ZOOM_LARGE = 3;
    public static final int TEXT_ZOOM_HUGE = 4;
    // "normal" will be the default
    public static final int TEXT_ZOOM_DEFAULT = TEXT_ZOOM_NORMAL;

    public static final String CONV_LIST_ICON_SENDER_IMAGE = "senderimage";
    public static final String CONV_LIST_ICON_NONE = "none";
    public static final String CONV_LIST_ICON_DEFAULT = CONV_LIST_ICON_SENDER_IMAGE;

    private static Preferences sPreferences;

    private final SharedPreferences mSharedPreferences;

    private Preferences(Context context) {
        mSharedPreferences = context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
    }

    public static synchronized Preferences getPreferences(Context context) {
        if (sPreferences == null) {
            sPreferences = new Preferences(context);
        }
        return sPreferences;
    }

    public static SharedPreferences getSharedPreferences(Context context) {
        return getPreferences(context).mSharedPreferences;
    }

    public static String getLegacyBackupPreference(Context context) {
        return getPreferences(context).mSharedPreferences.getString(ACCOUNT_UUIDS, null);
    }

    public static void clearLegacyBackupPreference(Context context) {
        getPreferences(context).mSharedPreferences.edit().remove(ACCOUNT_UUIDS).apply();
    }

    public void setEnableDebugLogging(boolean value) {
        mSharedPreferences.edit().putBoolean(ENABLE_DEBUG_LOGGING, value).apply();
    }

    public boolean getEnableDebugLogging() {
        return mSharedPreferences.getBoolean(ENABLE_DEBUG_LOGGING, false);
    }

    public void setEnableExchangeLogging(boolean value) {
        mSharedPreferences.edit().putBoolean(ENABLE_EXCHANGE_LOGGING, value).apply();
    }

    public boolean getEnableExchangeLogging() {
        return mSharedPreferences.getBoolean(ENABLE_EXCHANGE_LOGGING, false);
    }

    public void setEnableExchangeFileLogging(boolean value) {
        mSharedPreferences.edit().putBoolean(ENABLE_EXCHANGE_FILE_LOGGING, value).apply();
    }

    public boolean getEnableExchangeFileLogging() {
        return mSharedPreferences.getBoolean(ENABLE_EXCHANGE_FILE_LOGGING, false);
    }

    public void setInhibitGraphicsAcceleration(boolean value) {
        mSharedPreferences.edit().putBoolean(INHIBIT_GRAPHICS_ACCELERATION, value).apply();
    }

    public boolean getInhibitGraphicsAcceleration() {
        return mSharedPreferences.getBoolean(INHIBIT_GRAPHICS_ACCELERATION, false);
    }

    public void setForceOneMinuteRefresh(boolean value) {
        mSharedPreferences.edit().putBoolean(FORCE_ONE_MINUTE_REFRESH, value).apply();
    }

    public boolean getForceOneMinuteRefresh() {
        return mSharedPreferences.getBoolean(FORCE_ONE_MINUTE_REFRESH, false);
    }

    public void setEnableStrictMode(boolean value) {
        mSharedPreferences.edit().putBoolean(ENABLE_STRICT_MODE, value).apply();
    }

    public boolean getEnableStrictMode() {
        return mSharedPreferences.getBoolean(ENABLE_STRICT_MODE, false);
    }

    /**
     * Generate a new "device UID".  This is local to Email app only, to prevent possibility
     * of correlation with any other user activities in any other apps.
     * @return a persistent, unique ID
     */
    public synchronized String getDeviceUID() {
         String result = mSharedPreferences.getString(DEVICE_UID, null);
         if (result == null) {
             result = UUID.randomUUID().toString();
             mSharedPreferences.edit().putString(DEVICE_UID, result).apply();
         }
         return result;
    }

    public int getOneTimeInitializationProgress() {
        return mSharedPreferences.getInt(ONE_TIME_INITIALIZATION_PROGRESS, 0);
    }

    public void setOneTimeInitializationProgress(int progress) {
        mSharedPreferences.edit().putInt(ONE_TIME_INITIALIZATION_PROGRESS, progress).apply();
    }

    public int getAutoAdvanceDirection() {
        return mSharedPreferences.getInt(AUTO_ADVANCE_DIRECTION, AUTO_ADVANCE_DEFAULT);
    }

    public void setAutoAdvanceDirection(int direction) {
        mSharedPreferences.edit().putInt(AUTO_ADVANCE_DIRECTION, direction).apply();
    }

    /** @deprecated Only used for migration */
    @Deprecated
    public String getConversationListIcon() {
        return mSharedPreferences.getString(CONV_LIST_ICON, CONV_LIST_ICON_SENDER_IMAGE);
    }

    public boolean getConfirmDelete() {
        return mSharedPreferences.getBoolean(CONFIRM_DELETE, CONFIRM_DELETE_DEFAULT);
    }

    public void setConfirmDelete(boolean set) {
        mSharedPreferences.edit().putBoolean(CONFIRM_DELETE, set).apply();
    }

    public boolean getConfirmSend() {
        return mSharedPreferences.getBoolean(CONFIRM_SEND, CONFIRM_SEND_DEFAULT);
    }

    public void setConfirmSend(boolean set) {
        mSharedPreferences.edit().putBoolean(CONFIRM_SEND, set).apply();
    }

    /** @deprecated Only used for migration */
    @Deprecated
    public boolean hasSwipeDelete() {
        return mSharedPreferences.contains(SWIPE_DELETE);
    }

    /** @deprecated Only used for migration */
    @Deprecated
    public boolean getSwipeDelete() {
        return mSharedPreferences.getBoolean(SWIPE_DELETE, false);
    }

    /** @deprecated Only used for migration */
    @Deprecated
    public boolean hasReplyAll() {
        return mSharedPreferences.contains(REPLY_ALL);
    }

    /** @deprecated Only used for migration */
    @Deprecated
    public boolean getReplyAll() {
        return mSharedPreferences.getBoolean(REPLY_ALL, false);
    }

    public int getTextZoom() {
        return mSharedPreferences.getInt(TEXT_ZOOM, TEXT_ZOOM_DEFAULT);
    }

    public void setTextZoom(int zoom) {
        mSharedPreferences.edit().putInt(TEXT_ZOOM, zoom).apply();
    }

    public boolean getBackgroundAttachments() {
        return mSharedPreferences.getBoolean(BACKGROUND_ATTACHMENTS, false);
    }

    public void setBackgroundAttachments(boolean allowed) {
        mSharedPreferences.edit().putBoolean(BACKGROUND_ATTACHMENTS, allowed).apply();
    }

    /**
     * @deprecated This has been moved to {@link com.blackberry.email.preferences.MailPrefs}, and is only here for migration.
     */
    @Deprecated
    public Set<String> getWhitelistedSenderAddresses() {
        try {
            return parseEmailSet(mSharedPreferences.getString(TRUSTED_SENDERS, ""));
        } catch (JSONException e) {
            return Collections.emptySet();
        }
    }

    HashSet<String> parseEmailSet(String serialized) throws JSONException {
        HashSet<String> result = new HashSet<String>();
        if (!TextUtils.isEmpty(serialized)) {
            JSONArray arr = new JSONArray(serialized);
            for (int i = 0, len = arr.length(); i < len; i++) {
                result.add((String) arr.get(i));
            }
        }
        return result;
    }

    String packEmailSet(HashSet<String> set) {
        return new JSONArray(set).toString();
    }

    /**
     * Returns the last used account ID as set by {@link #setLastUsedAccountId}.
     * The system makes no attempt to automatically track what is considered a "use" - clients
     * are expected to call {@link #setLastUsedAccountId} manually.
     *
     * Note that the last used account may have been deleted in the background so there is also
     * no guarantee that the account exists.
     */
    public long getLastUsedAccountId() {
        return mSharedPreferences.getLong(LAST_ACCOUNT_USED, Account.NO_ACCOUNT);
    }

    /**
     * Sets the specified ID of the last account used. Treated as an opaque ID and does not
     * validate the value. Value is saved asynchronously.
     */
    public void setLastUsedAccountId(long accountId) {
        mSharedPreferences
                .edit()
                .putLong(LAST_ACCOUNT_USED, accountId)
                .apply();
    }

    /**
     * Gets whether the require manual sync dialog has been shown for the specified account.
     * It should only be shown once per account.
     */
    public boolean getHasShownRequireManualSync(Account account) {
        return getBoolean(account.getEmailAddress(), REQUIRE_MANUAL_SYNC_DIALOG_SHOWN, false);
    }

    /**
     * Sets whether the require manual sync dialog has been shown for the specified account.
     * It should only be shown once per account.
     */
    public void setHasShownRequireManualSync(Account account, boolean value) {
        setBoolean(account.getEmailAddress(), REQUIRE_MANUAL_SYNC_DIALOG_SHOWN, value);
    }


    /**
     * Get whether to show the manual sync dialog. This dialog is shown when the user is roaming,
     * connected to a mobile network, the administrator has set the RequireManualSyncWhenRoaming
     * flag to true, and the dialog has not been shown before for the supplied account.
     */
    public boolean shouldShowRequireManualSync(Context context, Account account) {
        return Account.isAutomaticSyncDisabledByRoaming(context, account.mId)
                && !getHasShownRequireManualSync(account);
    }

    public void clear() {
        mSharedPreferences.edit().clear().apply();
    }

    public void dump() {
        if (Logging.LOGD) {
            for (String key : mSharedPreferences.getAll().keySet()) {
                LogUtils.v(Logging.LOG_TAG, key + " = " + mSharedPreferences.getAll().get(key));
            }
        }
    }

    /**
     * Utility method for setting a boolean value on a per-account preference.
     */
    private void setBoolean(String account, String key, Boolean value) {
        mSharedPreferences.edit().putBoolean(makeKey(account, key), value).apply();
    }

    /**
     * Utility method for getting a boolean value from a per-account preference.
     */
    private boolean getBoolean(String account, String key, boolean def) {
        return mSharedPreferences.getBoolean(makeKey(account, key), def);
    }

    /**
     * Utility method for creating a per account preference key.
     */
    private static String makeKey(String account, String key) {
        return account != null ? account + "-" + key : key;
    }
}
