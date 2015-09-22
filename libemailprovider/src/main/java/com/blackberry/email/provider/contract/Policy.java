/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.blackberry.email.provider.contract;
import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.blackberry.common.utils.TextUtilities;
import com.blackberry.email.utils.Utility;
import com.blackberry.lib.emailprovider.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

/**
 * The Policy class represents a set of security requirements that are associated with an Account.
 * The requirements may be either device-specific (e.g. password) or application-specific (e.g.
 * a limit on the sync window for the Account)
 */
public final class Policy extends EmailContent implements EmailContent.PolicyColumns, Parcelable {
    public static final boolean DEBUG_POLICY = false;  // DO NOT SUBMIT WITH THIS SET TO TRUE
    public static final String TAG = "Email/Policy";

    public static final String TABLE_NAME = "Policy";
    public static Uri CONTENT_URI;

    public static void initPolicy() {
        CONTENT_URI = Uri.parse(EmailContent.CONTENT_URI + "/policy");
    }

    /* Convert days to mSec (used for password expiration) */
    private static final long DAYS_TO_MSEC = 24 * 60 * 60 * 1000;
    /* Small offset (2 minutes) added to policy expiration to make user testing easier. */
    private static final long EXPIRATION_OFFSET_MSEC = 2 * 60 * 1000;

    public static final int PASSWORD_MODE_NONE = 0;
    public static final int PASSWORD_MODE_SIMPLE = 1;
    public static final int PASSWORD_MODE_STRONG = 2;

    // Defines for unsupported polices
    public static final int REQUIRE_DEVICE_ENCRYPTION            = 100;
    public static final int DEVICE_ENCRYPTION_ENABLED            = 101;
    public static final int UNAPPROVED_IN_ROM_APPLICATION_LIST   = 102;
    public static final int APPROVED_APPLICATION_LIST            = 103;
    public static final int ALLOW_HTML_EMAIL                     = 104;
    public static final int ALLOW_STORAGE_CARD                   = 105;
    public static final int ALLOW_UNSIGNED_APPLICATIONS          = 106;
    public static final int ALLOW_UNSIGNED_INSTALLATION_PACKAGES = 107;
    public static final int ALLOW_WIFI                           = 108;
    public static final int MAX_EMAIL_HTML_BODY_TRUNCATION_SIZE  = 109;
    public static final int ALLOW_TEXT_MESSAGING                 = 110;
    public static final int ALLOW_POP_IMAP_EMAIL                 = 111;
    public static final int ALLOW_IRDA                           = 112;
    public static final int ALLOW_BROWSER                        = 113;
    public static final int ALLOW_CONSUMER_EMAIL                 = 114;
    public static final int ALLOW_INTERNET_SHARING               = 115;
    public static final int ALLOW_BLUETOOTH                      = 116;
    public static final int REQUIRE_SIGNED_SMIME_MESSAGES        = 117;
    public static final int REQUIRE_ENCRYPTED_SMIME_MESSAGES     = 118;
    public static final int REQUIRE_SIGNED_SMIME_ALGORITHM       = 119;
    public static final int REQUIRE_ENCRYPTION_SMIME_ALGORITHM   = 120;
    public static final int MAX_EMAIL_BODY_TRUNCATION_SIZE       = 121;

    private static final HashSet<Integer> mUnsupportedPolicies = new HashSet<Integer>(Arrays.asList(
            REQUIRE_DEVICE_ENCRYPTION,
            DEVICE_ENCRYPTION_ENABLED,
            UNAPPROVED_IN_ROM_APPLICATION_LIST,
            APPROVED_APPLICATION_LIST,
            ALLOW_HTML_EMAIL,
            ALLOW_STORAGE_CARD,
            ALLOW_UNSIGNED_APPLICATIONS,
            ALLOW_UNSIGNED_INSTALLATION_PACKAGES,
            ALLOW_WIFI,
            ALLOW_TEXT_MESSAGING,
            ALLOW_POP_IMAP_EMAIL,
            ALLOW_IRDA,
            ALLOW_BROWSER,
            ALLOW_CONSUMER_EMAIL,
            ALLOW_INTERNET_SHARING,
            ALLOW_BLUETOOTH,
            REQUIRE_SIGNED_SMIME_MESSAGES,
            REQUIRE_ENCRYPTED_SMIME_MESSAGES,
            REQUIRE_SIGNED_SMIME_ALGORITHM,
            REQUIRE_ENCRYPTION_SMIME_ALGORITHM,
            MAX_EMAIL_BODY_TRUNCATION_SIZE,
            MAX_EMAIL_HTML_BODY_TRUNCATION_SIZE
            ));

    public static final int ALLOW_SIMPLE_DEVICE_PASSWORD         = 200;
    public static final int ALLOW_DESKTOP_SYNC                   = 201;
    public static final int ALLOW_SMIME_ENCRYPTION_NEGOTIATION   = 202;
    public static final int ALLOW_SMIME_SOFT_CERTS               = 203;
    public static final int ALLOW_REMOTE_DESKTOP                 = 204;
    private HashSet<Integer> mIgnoredPolicies = new HashSet<Integer>(Arrays.asList(ALLOW_DESKTOP_SYNC,ALLOW_SMIME_ENCRYPTION_NEGOTIATION,
            ALLOW_SMIME_SOFT_CERTS,ALLOW_REMOTE_DESKTOP, ALLOW_SIMPLE_DEVICE_PASSWORD));

    public static final char POLICY_STRING_DELIMITER = '\1';
    public static final String POLICY_IS_ACTIVE = "__security_policy_activated__";

    public boolean mRequireRemoteWipe;
    public boolean mRequireEncryption;
    public boolean mRequireEncryptionExternal;
    public boolean mRequireManualSyncWhenRoaming;
    public boolean mDontAllowCamera;
    public boolean mDontAllowAttachments;
    public boolean mDontAllowHtml;
    public int mMaxAttachmentSize;
    public int mMaxTextTruncationSize;
    public int mMaxHtmlTruncationSize;
    public int mMaxEmailLookback;
    public int mMaxCalendarLookback;
    public boolean mPasswordRecoveryEnabled;
    public String mProtocolPoliciesEnforced;
    public ArrayList<Integer> mUnsupportedList;
    public ArrayList<Integer> mIgnoredList;

    // The following items need to be private due to the fact
    // that there are relationships between the fields and that logic
    // should be handled in a single spot (in this class)
    private int mPasswordComplexChars;
    private boolean mPasswordEnabled;
    private int mPasswordMode;
    private boolean mAlphaNumericPassword;
    private int mPasswordMinLength;
    private int mPasswordMaxFails;
    private int mPasswordExpirationDays;
    private int mPasswordHistory;
    private int mMaxScreenLockTime;

    private boolean mPasswordModeSetExplicitly = false;

    public static final int CONTENT_ID_COLUMN = 0;
    public static final int CONTENT_PASSWORD_MODE_COLUMN = 1;
    public static final int CONTENT_PASSWORD_MIN_LENGTH_COLUMN = 2;
    public static final int CONTENT_PASSWORD_EXPIRATION_DAYS_COLUMN = 3;
    public static final int CONTENT_PASSWORD_HISTORY_COLUMN = 4;
    public static final int CONTENT_PASSWORD_COMPLEX_CHARS_COLUMN = 5;
    public static final int CONTENT_PASSWORD_MAX_FAILS_COLUMN = 6;
    public static final int CONTENT_MAX_SCREEN_LOCK_TIME_COLUMN = 7;
    public static final int CONTENT_REQUIRE_REMOTE_WIPE_COLUMN = 8;
    public static final int CONTENT_REQUIRE_ENCRYPTION_COLUMN = 9;
    public static final int CONTENT_REQUIRE_ENCRYPTION_EXTERNAL_COLUMN = 10;
    public static final int CONTENT_REQUIRE_MANUAL_SYNC_WHEN_ROAMING = 11;
    public static final int CONTENT_DONT_ALLOW_CAMERA_COLUMN = 12;
    public static final int CONTENT_DONT_ALLOW_ATTACHMENTS_COLUMN = 13;
    public static final int CONTENT_DONT_ALLOW_HTML_COLUMN = 14;
    public static final int CONTENT_MAX_ATTACHMENT_SIZE_COLUMN = 15;
    public static final int CONTENT_MAX_TEXT_TRUNCATION_SIZE_COLUMN = 16;
    public static final int CONTENT_MAX_HTML_TRUNCATION_SIZE_COLUMN = 17;
    public static final int CONTENT_MAX_EMAIL_LOOKBACK_COLUMN = 18;
    public static final int CONTENT_MAX_CALENDAR_LOOKBACK_COLUMN = 19;
    public static final int CONTENT_PASSWORD_RECOVERY_ENABLED_COLUMN = 20;
    public static final int CONTENT_PROTOCOL_POLICIES_ENFORCED_COLUMN = 21;
    public static final int CONTENT_PROTOCOL_POLICIES_UNSUPPORTED_COLUMN = 22;

    public static final String[] CONTENT_PROJECTION = new String[] {RECORD_ID,
        PolicyColumns.PASSWORD_MODE, PolicyColumns.PASSWORD_MIN_LENGTH,
        PolicyColumns.PASSWORD_EXPIRATION_DAYS, PolicyColumns.PASSWORD_HISTORY,
        PolicyColumns.PASSWORD_COMPLEX_CHARS, PolicyColumns.PASSWORD_MAX_FAILS,
        PolicyColumns.MAX_SCREEN_LOCK_TIME, PolicyColumns.REQUIRE_REMOTE_WIPE,
        PolicyColumns.REQUIRE_ENCRYPTION, PolicyColumns.REQUIRE_ENCRYPTION_EXTERNAL,
        PolicyColumns.REQUIRE_MANUAL_SYNC_WHEN_ROAMING, PolicyColumns.DONT_ALLOW_CAMERA,
        PolicyColumns.DONT_ALLOW_ATTACHMENTS, PolicyColumns.DONT_ALLOW_HTML,
        PolicyColumns.MAX_ATTACHMENT_SIZE, PolicyColumns.MAX_TEXT_TRUNCATION_SIZE,
        PolicyColumns.MAX_HTML_TRUNCATION_SIZE, PolicyColumns.MAX_EMAIL_LOOKBACK,
        PolicyColumns.MAX_CALENDAR_LOOKBACK, PolicyColumns.PASSWORD_RECOVERY_ENABLED,
        PolicyColumns.PROTOCOL_POLICIES_ENFORCED, PolicyColumns.PROTOCOL_POLICIES_UNSUPPORTED
    };

    public static final Policy NO_POLICY = new Policy();

    private static final String[] ATTACHMENT_RESET_PROJECTION =
        new String[] {EmailContent.RECORD_ID, AttachmentColumns.SIZE, AttachmentColumns.FLAGS};
    private static final int ATTACHMENT_RESET_PROJECTION_ID = 0;
    private static final int ATTACHMENT_RESET_PROJECTION_SIZE = 1;
    private static final int ATTACHMENT_RESET_PROJECTION_FLAGS = 2;

    public Policy() {
        mBaseUri = CONTENT_URI;
        // By default, the password mode is not set - if someone tries to get
        // the password mode then we will try and determine the mode then
        mPasswordMode = Policy.PASSWORD_MODE_NONE;
        // All server policies require the ability to wipe the device
        mRequireRemoteWipe = true;
        mUnsupportedList = new ArrayList<Integer>();
        mIgnoredList = new ArrayList<Integer>();

        mPasswordExpirationDays = Integer.MAX_VALUE;
        mMaxScreenLockTime = Integer.MAX_VALUE;
        mPasswordMaxFails = Integer.MAX_VALUE;
    }

    public static Policy restorePolicyWithId(Context context, long id) {
        return EmailContent.restoreContentWithId(context, Policy.class, Policy.CONTENT_URI,
                Policy.CONTENT_PROJECTION, id);
    }

    public static long getAccountIdWithPolicyKey(Context context, long id) {
        return Utility.getFirstRowLong(context, Account.CONTENT_URI, Account.ID_PROJECTION,
                AccountColumns.POLICY_KEY + "=?", new String[] {Long.toString(id)}, null,
                Account.ID_PROJECTION_COLUMN, Account.NO_ACCOUNT);
    }

    public static ArrayList<String> addPolicyStringToList(String policyString,
            ArrayList<String> policyList) {
        if (policyString != null) {
            int start = 0;
            int len = policyString.length();
            while(start < len) {
                int end = policyString.indexOf(POLICY_STRING_DELIMITER, start);
                if (end > start) {
                    policyList.add(policyString.substring(start, end));
                    start = end + 1;
                } else {
                    break;
                }
            }
        }
        return policyList;
    }

    public int getMinimumNonLetterChars() {
        // NOTE - ActiveSync does not really support this concept so just return 0.
        // For ActiveSync, we will rely on the use of the call to getDPManagerPasswordQuality
        // to generate the correct password rules for Android
        return 0;
    }
    public void setMinimumNonLetterChars(int numChars) {
        // NOTE - ActiveSync does not really support this concept so just return 0.
        // For ActiveSync, we will rely on the use of the call to getDPManagerPasswordQuality
        // to generate the correct password rules for Android
    }

    // NOTE - here are the getters and setters for the various password settings

    // The complex chars value in ActiveSync actually refers to how many password groups
    // are required.  See MinDevicePasswordComplexCharacters in the MS-ASPROV documentation
    public int getNumPasswordGroups() {
        return mPasswordComplexChars;
    }
    public boolean getPasswordEnabled() {
        return mPasswordEnabled;
    }
    public int getMaxScreenLockTime() {
        if (mPasswordEnabled && mMaxScreenLockTime != Integer.MAX_VALUE) {
            return mMaxScreenLockTime;
        } else {
            return 0;
        }
    }
    public void setMaxScreenLockTime(int value) {
        mMaxScreenLockTime = value;
    }
    public int getPasswordExpirationDays() {
        if (mPasswordEnabled && mPasswordExpirationDays != Integer.MAX_VALUE) {
            return mPasswordExpirationDays;
        } else {
            return 0;
        }
    }
    public void setPasswordExpirationDays(int value) {
        mPasswordExpirationDays = value;
    }
    public int getPasswordMaxFails() {
        if (mPasswordEnabled && mPasswordMaxFails != Integer.MAX_VALUE) {
            return mPasswordMaxFails;
        } else {
            return 0;
        }
    }
    public void setPasswordMaxFails(int value) {
        mPasswordMaxFails = value;
    }
    public int getPasswordMinLength() {
        if (mPasswordEnabled) {
            return mPasswordMinLength;
        } else {
            return 0;
        }
    }
    public void setPasswordMinLength(int value) {
        mPasswordMinLength = value;
    }
    public int getPasswordHistory() {
        if (mPasswordEnabled) {
            return mPasswordHistory;
        } else {
            return 0;
        }
    }
    public void setPasswordHistory(int value) {
        mPasswordHistory = value;
    }

    // Now the tricky stuff - password mode is the old way ActiveSync used
    // to communicate the password complexity, the newer policy does not have this
    // concept but the current Policy database still does - hence the following
    // gymnastics to get the correct mode
    public int getPasswordMode() throws IllegalStateException {
        if (!mPasswordModeSetExplicitly) {
            // Throw an exception since this policy is invalid
            throw new IllegalStateException("Policies have not been set");
        }
        return mPasswordMode;
    }

    // This method is only for old stype MS-WAP-Provisioning-xml policies - in that
    // case there was only simple and complex modes
    public void setPasswordMode(int mode) {
        mPasswordMode = mode;
        mPasswordModeSetExplicitly = true;
        if (mPasswordMode > Policy.PASSWORD_MODE_NONE) {
            mPasswordEnabled = true;
        }
    }
    // Use this method for MS-EAS-Provisioning-WBXML policies
    public void setPasswordMode(boolean passwordEnabled, boolean alphaNumericPassword, int numPasswordGroups) {
        mPasswordEnabled = passwordEnabled;
        mPasswordModeSetExplicitly = true;
        if (mPasswordEnabled) {
            mAlphaNumericPassword = alphaNumericPassword;
            mPasswordComplexChars = numPasswordGroups;
            setPasswordMode();
        } else {
            // Set all the password values to be the default
            mPasswordMinLength = 0;
            mPasswordComplexChars = 0;
            mPasswordHistory = 0;
            mAlphaNumericPassword = false;
            mPasswordExpirationDays = Integer.MAX_VALUE;
            mMaxScreenLockTime = Integer.MAX_VALUE;
            mPasswordMaxFails = Integer.MAX_VALUE;

        }
    }
    private void setPasswordMode() {
        if (mPasswordEnabled) {
            if (mAlphaNumericPassword || this.mPasswordComplexChars > 2) {
                mPasswordMode = Policy.PASSWORD_MODE_STRONG;
            } else {
                mPasswordMode = Policy.PASSWORD_MODE_SIMPLE;
            }
        } else {
            mPasswordMode = Policy.PASSWORD_MODE_NONE;
        }
    }

    public boolean getAlphaNumericPassword() {
        return mAlphaNumericPassword;
    }

    // We override this method to insure that we never write invalid policy data to the provider
    @Override
    public Uri save(Context context) {
        normalize(context.getResources());
        return super.save(context);
    }

    /**
     * Review all attachment records for this account, and reset the "don't allow download" flag
     * as required by the account's new security policies
     * @param context the caller's context
     * @param account the account whose attachments need to be reviewed
     * @param policy the new policy for this account
     */
    public static void setAttachmentFlagsForNewPolicy(Context context, Account account,
            Policy policy) {
        // A nasty bit of work; start with all attachments for a given account
        ContentResolver resolver = context.getContentResolver();
        Cursor c = resolver.query(Attachment.CONTENT_URI, ATTACHMENT_RESET_PROJECTION,
                AttachmentColumns.ACCOUNT_KEY + "=?", new String[] {Long.toString(account.mId)},
                null);
        ContentValues cv = new ContentValues();
        try {
            // Get maximum allowed size (0 if we don't allow attachments at all)
            int policyMax = policy.mDontAllowAttachments ? 0 : (policy.mMaxAttachmentSize > 0) ?
                    policy.mMaxAttachmentSize : Integer.MAX_VALUE;
            while (c.moveToNext()) {
                int flags = c.getInt(ATTACHMENT_RESET_PROJECTION_FLAGS);
                int size = c.getInt(ATTACHMENT_RESET_PROJECTION_SIZE);
                boolean wasRestricted = (flags & Attachment.FLAG_POLICY_DISALLOWS_DOWNLOAD) != 0;
                boolean isRestricted = size > policyMax;
                if (isRestricted != wasRestricted) {
                    if (isRestricted) {
                        flags |= Attachment.FLAG_POLICY_DISALLOWS_DOWNLOAD;
                    } else {
                        flags &= ~Attachment.FLAG_POLICY_DISALLOWS_DOWNLOAD;
                    }
                    long id = c.getLong(ATTACHMENT_RESET_PROJECTION_ID);
                    cv.put(AttachmentColumns.FLAGS, flags);
                    resolver.update(ContentUris.withAppendedId(Attachment.CONTENT_URI, id),
                            cv, null, null);
                }
            }
        } finally {
            c.close();
        }
    }

    public void addToUnsupportedList(int policyTag) {
        if (mIgnoredPolicies.contains(policyTag)) {
            mIgnoredList.add(policyTag);
        } else if (mUnsupportedPolicies.contains(policyTag)){
            mUnsupportedList.add(policyTag);
        }
    }

    public String[] getUnsupportedPolicyStrings(Resources resources) {
        ArrayList<String> unsupportedList = new ArrayList<String>();
        for (int policy: mUnsupportedList) {
            int resDefine = getPolicyResourceDefine(policy);
            if (resDefine > 0) {
                unsupportedList.add(resources.getString(resDefine));
            }
        }
        return unsupportedList.toArray(new String[unsupportedList.size()]);
    }
    private void addPolicyString(Resources resources, StringBuilder sb, int res) {
        sb.append(resources.getString(res));
        sb.append(Policy.POLICY_STRING_DELIMITER);
    }
    /**
     * Normalize the Policy.  If the password mode is "none", zero out all password-related fields;
     * zero out complex characters for simple passwords.
     */
    public void normalize(Resources resources) {
        if (mPasswordMode == PASSWORD_MODE_NONE) {
            mPasswordMaxFails = 0;
            mMaxScreenLockTime = 0;
            mPasswordMinLength = 0;
            mPasswordComplexChars = 0;
            mPasswordHistory = 0;
            mPasswordExpirationDays = 0;
        } else {
            if ((mPasswordMode != PASSWORD_MODE_SIMPLE) &&
                    (mPasswordMode != PASSWORD_MODE_STRONG)) {
                throw new IllegalArgumentException("password mode");
            }
            // If we're only requiring a simple password, set complex chars to zero; note
            // that EAS can erroneously send non-zero values in this case
            if (mPasswordMode == PASSWORD_MODE_SIMPLE) {
                mPasswordComplexChars = 0;
            }
        }

        if (resources != null) {
            StringBuilder sb = new StringBuilder();
            if (mDontAllowAttachments) {
                addPolicyString(resources, sb, R.string.policy_dont_allow_attachments);
            }
            if (mRequireManualSyncWhenRoaming) {
                addPolicyString(resources, sb, R.string.policy_require_manual_sync_roaming);
            }
            mProtocolPoliciesEnforced = sb.toString();
        }
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Policy)) return false;
        return (comparePolicy((Policy)other) == 0);
    }

    public int comparePolicy(Policy otherPolicy) {

        if (mRequireEncryption != otherPolicy.mRequireEncryption) return 1;
        if (mRequireEncryptionExternal != otherPolicy.mRequireEncryptionExternal) return 2;
        if (mRequireRemoteWipe != otherPolicy.mRequireRemoteWipe) return 3;
        if (mMaxScreenLockTime != otherPolicy.mMaxScreenLockTime) return 4;
        if (mPasswordComplexChars != otherPolicy.mPasswordComplexChars) return 5;
        if (mPasswordExpirationDays != otherPolicy.mPasswordExpirationDays) return 6;
        if (mPasswordHistory != otherPolicy.mPasswordHistory) return 7;
        if (mPasswordMaxFails != otherPolicy.mPasswordMaxFails) return 8;
        if (mPasswordMinLength != otherPolicy.mPasswordMinLength) return 9;
        if (mPasswordMode != otherPolicy.mPasswordMode) return 10;
        if (mDontAllowCamera != otherPolicy.mDontAllowCamera) return 11;

        // Policies here are enforced by the Exchange sync manager
        // They should eventually be removed from Policy and replaced with some opaque data
        if (mRequireManualSyncWhenRoaming != otherPolicy.mRequireManualSyncWhenRoaming) {
            return 12;
        }
        if (mDontAllowAttachments != otherPolicy.mDontAllowAttachments) return 13;
        if (mDontAllowHtml != otherPolicy.mDontAllowHtml) return 14;
        if (mMaxAttachmentSize != otherPolicy.mMaxAttachmentSize) return 15;
        if (mMaxTextTruncationSize != otherPolicy.mMaxTextTruncationSize) return 16;
        if (mMaxHtmlTruncationSize != otherPolicy.mMaxHtmlTruncationSize) return 17;
        if (mMaxEmailLookback != otherPolicy.mMaxEmailLookback) return 18;
        if (mMaxCalendarLookback != otherPolicy.mMaxCalendarLookback) return 19;
        if (mPasswordRecoveryEnabled != otherPolicy.mPasswordRecoveryEnabled) return 20;

        if (!TextUtilities.stringOrNullEquals(mProtocolPoliciesEnforced,
                otherPolicy.mProtocolPoliciesEnforced)) {
            return 21;
        }
        if (!mUnsupportedList.equals(otherPolicy.mUnsupportedList)) {
            return 22;
        }
        if (!mIgnoredList.equals(otherPolicy.mIgnoredList)) {
            return 23;
        }
        if (mPasswordEnabled != otherPolicy.mPasswordEnabled) return 24;
        if (mAlphaNumericPassword != otherPolicy.mAlphaNumericPassword) return 25;
        return 0;
    }

    @Override
    public int hashCode() {
        int code = mRequireEncryption ? 1 : 0;
        code += (mRequireEncryptionExternal ? 1 : 0) << 1;
        code += (mRequireRemoteWipe ? 1 : 0) << 2;
        code += (mMaxScreenLockTime << 3);
        code += (mPasswordComplexChars << 6);
        code += (mPasswordExpirationDays << 12);
        code += (mPasswordHistory << 15);
        code += (mPasswordMaxFails << 18);
        code += (mPasswordMinLength << 22);
        code += (mPasswordMode << 26);
        // Don't need to include the other fields
        return code;
    }

    @Override
    public void restore(Cursor cursor) {
        mBaseUri = CONTENT_URI;
        mId = cursor.getLong(CONTENT_ID_COLUMN);
        setPasswordMode(cursor.getInt(CONTENT_PASSWORD_MODE_COLUMN));
        if (mPasswordMode > Policy.PASSWORD_MODE_NONE) {
            mPasswordEnabled = true;
        }
        mPasswordMinLength = cursor.getInt(CONTENT_PASSWORD_MIN_LENGTH_COLUMN);
        mPasswordMaxFails = cursor.getInt(CONTENT_PASSWORD_MAX_FAILS_COLUMN);
        if (mPasswordMaxFails == 0) {
            // Password max fails default is MAX, not MIN
            mPasswordMaxFails = Integer.MAX_VALUE;
        }
        mPasswordHistory = cursor.getInt(CONTENT_PASSWORD_HISTORY_COLUMN);
        mPasswordExpirationDays = cursor.getInt(CONTENT_PASSWORD_EXPIRATION_DAYS_COLUMN);
        if (mPasswordExpirationDays == 0) {
            // Expiration days default is MAX, not MIN
            mPasswordExpirationDays = Integer.MAX_VALUE;
        }
        mPasswordComplexChars = cursor.getInt(CONTENT_PASSWORD_COMPLEX_CHARS_COLUMN);
        mMaxScreenLockTime = cursor.getInt(CONTENT_MAX_SCREEN_LOCK_TIME_COLUMN);
        if (mMaxScreenLockTime == 0) {
            // Screen lock time default is MAX, not MIN
            mMaxScreenLockTime = Integer.MAX_VALUE;
        }
        mRequireRemoteWipe = cursor.getInt(CONTENT_REQUIRE_REMOTE_WIPE_COLUMN) == 1;
        mRequireEncryption = cursor.getInt(CONTENT_REQUIRE_ENCRYPTION_COLUMN) == 1;
        mRequireEncryptionExternal =
            cursor.getInt(CONTENT_REQUIRE_ENCRYPTION_EXTERNAL_COLUMN) == 1;
        mRequireManualSyncWhenRoaming =
            cursor.getInt(CONTENT_REQUIRE_MANUAL_SYNC_WHEN_ROAMING) == 1;
        mDontAllowCamera = cursor.getInt(CONTENT_DONT_ALLOW_CAMERA_COLUMN) == 1;
        mDontAllowAttachments = cursor.getInt(CONTENT_DONT_ALLOW_ATTACHMENTS_COLUMN) == 1;
        mDontAllowHtml = cursor.getInt(CONTENT_DONT_ALLOW_HTML_COLUMN) == 1;
        mMaxAttachmentSize = cursor.getInt(CONTENT_MAX_ATTACHMENT_SIZE_COLUMN);
        mMaxTextTruncationSize = cursor.getInt(CONTENT_MAX_TEXT_TRUNCATION_SIZE_COLUMN);
        mMaxHtmlTruncationSize = cursor.getInt(CONTENT_MAX_HTML_TRUNCATION_SIZE_COLUMN);
        mMaxEmailLookback = cursor.getInt(CONTENT_MAX_EMAIL_LOOKBACK_COLUMN);
        mMaxCalendarLookback = cursor.getInt(CONTENT_MAX_CALENDAR_LOOKBACK_COLUMN);
        mPasswordRecoveryEnabled = cursor.getInt(CONTENT_PASSWORD_RECOVERY_ENABLED_COLUMN) == 1;
        mProtocolPoliciesEnforced = cursor.getString(CONTENT_PROTOCOL_POLICIES_ENFORCED_COLUMN);
        // TODO - in the ActiveSync case we are no longer creating strings for unsupported policies,
        // instead we are using the mUnsupportedList to pass around defines of policies.  Perhaps
        // mUnsupportedList and mIgnoredList need to be saved to the DB, not sure about that.
        // At any rate, there is NO need for the ActiveSync code to be converting AS policy codes to strings
        // from the resource file - that creates a dependency that seems improper.
    }

    @Override
    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put(PolicyColumns.PASSWORD_MODE, mPasswordMode);
        values.put(PolicyColumns.PASSWORD_MIN_LENGTH, mPasswordMinLength);
        if (mPasswordMaxFails == Integer.MAX_VALUE) {
            values.put(PolicyColumns.PASSWORD_MAX_FAILS, 0);
        } else {
            values.put(PolicyColumns.PASSWORD_MAX_FAILS, mPasswordMaxFails);
        }
        values.put(PolicyColumns.PASSWORD_HISTORY, mPasswordHistory);
        // Save 0 if not set
        if (mPasswordExpirationDays == Integer.MAX_VALUE) {
            values.put(PolicyColumns.PASSWORD_EXPIRATION_DAYS, 0);
        } else {
            values.put(PolicyColumns.PASSWORD_EXPIRATION_DAYS, mPasswordExpirationDays);
        }
        values.put(PolicyColumns.PASSWORD_COMPLEX_CHARS, mPasswordComplexChars);
        if (mMaxScreenLockTime == Integer.MAX_VALUE) {
            values.put(PolicyColumns.MAX_SCREEN_LOCK_TIME, 0);
        } else {
            values.put(PolicyColumns.MAX_SCREEN_LOCK_TIME, mMaxScreenLockTime);
        }
        values.put(PolicyColumns.REQUIRE_REMOTE_WIPE, mRequireRemoteWipe);
        values.put(PolicyColumns.REQUIRE_ENCRYPTION, mRequireEncryption);
        values.put(PolicyColumns.REQUIRE_ENCRYPTION_EXTERNAL, mRequireEncryptionExternal);
        values.put(PolicyColumns.REQUIRE_MANUAL_SYNC_WHEN_ROAMING, mRequireManualSyncWhenRoaming);
        values.put(PolicyColumns.DONT_ALLOW_CAMERA, mDontAllowCamera);
        values.put(PolicyColumns.DONT_ALLOW_ATTACHMENTS, mDontAllowAttachments);
        values.put(PolicyColumns.DONT_ALLOW_HTML, mDontAllowHtml);
        values.put(PolicyColumns.MAX_ATTACHMENT_SIZE, mMaxAttachmentSize);
        values.put(PolicyColumns.MAX_TEXT_TRUNCATION_SIZE, mMaxTextTruncationSize);
        values.put(PolicyColumns.MAX_HTML_TRUNCATION_SIZE, mMaxHtmlTruncationSize);
        values.put(PolicyColumns.MAX_EMAIL_LOOKBACK, mMaxEmailLookback);
        values.put(PolicyColumns.MAX_CALENDAR_LOOKBACK, mMaxCalendarLookback);
        values.put(PolicyColumns.PASSWORD_RECOVERY_ENABLED, mPasswordRecoveryEnabled);
        values.put(PolicyColumns.PROTOCOL_POLICIES_ENFORCED, mProtocolPoliciesEnforced);
        return values;
    }

    /**
     * Helper to map our internal encoding to DevicePolicyManager password modes.
     */
    public int getDPManagerPasswordQuality() {
        switch (mPasswordMode) {
            case PASSWORD_MODE_SIMPLE:
                return DevicePolicyManager.PASSWORD_QUALITY_NUMERIC;
            case PASSWORD_MODE_STRONG:
                switch (mPasswordComplexChars) {
                case 1:
                    // According to the ActiveSync docs, this requires only a single "group" of values.
                    // So it could be all lower case, all upper case, all numbers, etc.
                    return DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC;
                case 2:
                    // This requires at least 2 groups - not necessarily alpha numeric but I guess
                    // that will have to do for now
                    return DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC;
                case 3:
                case 4:
                    // When the server asks for 3 or 4 groups then we need a complex password
                    return DevicePolicyManager.PASSWORD_QUALITY_COMPLEX;
                default:
                    return DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED;
                }
            default:
                return DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED;
        }
    }

    /**
     * Helper to map expiration times to the millisecond values used by DevicePolicyManager.
     */
    public long getDPManagerPasswordExpirationTimeout() {
        if (mPasswordExpirationDays == Integer.MAX_VALUE || mPasswordExpirationDays == 0) {
            return 0;
        } else {
            long result = mPasswordExpirationDays * DAYS_TO_MSEC;
            // Add a small offset to the password expiration.  This makes it easier to test
            // by changing (for example) 1 day to 1 day + 5 minutes.  If you set an expiration
            // that is within the warning period, you should get a warning fairly quickly.
            if (result > 0) {
                result += EXPIRATION_OFFSET_MSEC;
            }
            return result;
        }
    }

    private static void appendPolicy(StringBuilder sb, String code, int value) {
        sb.append(code);
        sb.append(":");
        sb.append(value);
        sb.append(" ");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        if (equals(NO_POLICY)) {
            sb.append("No policies]");
        } else {
            if (mPasswordMode == PASSWORD_MODE_NONE) {
                sb.append("Pwd none ");
            } else {
                appendPolicy(sb, "Pwd strong", mPasswordMode == PASSWORD_MODE_STRONG ? 1 : 0);
                appendPolicy(sb, "len", mPasswordMinLength);
                appendPolicy(sb, "cmpx", mPasswordComplexChars);
                appendPolicy(sb, "expy", mPasswordExpirationDays);
                appendPolicy(sb, "hist", mPasswordHistory);
                appendPolicy(sb, "fail", mPasswordMaxFails);
                appendPolicy(sb, "idle", mMaxScreenLockTime);
            }
            if (mRequireEncryption) {
                sb.append("encrypt ");
            }
            if (mRequireEncryptionExternal) {
                sb.append("encryptsd ");
            }
            if (mDontAllowCamera) {
                sb.append("nocamera ");
            }
            if (mDontAllowAttachments) {
                sb.append("noatts ");
            }
            if (mRequireManualSyncWhenRoaming) {
                sb.append("nopushroam ");
            }
            if (mMaxAttachmentSize > 0) {
                appendPolicy(sb, "attmax", mMaxAttachmentSize);
            }
            sb.append("]");
        }
        return sb.toString();
    }

    /**
     * Supports Parcelable
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Supports Parcelable
     */
    public static final Parcelable.Creator<Policy> CREATOR = new Parcelable.Creator<Policy>() {
        @Override
        public Policy createFromParcel(Parcel in) {
            return new Policy(in);
        }

        @Override
        public Policy[] newArray(int size) {
            return new Policy[size];
        }
    };

    /**
     * Supports Parcelable
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // mBaseUri is not parceled
        dest.writeLong(mId);
        dest.writeInt(mPasswordMode);
        dest.writeInt(mPasswordMinLength);
        dest.writeInt(mPasswordMaxFails);
        dest.writeInt(mPasswordHistory);
        dest.writeInt(mPasswordExpirationDays);
        dest.writeInt(mPasswordComplexChars);
        dest.writeInt(mMaxScreenLockTime);
        dest.writeInt(mRequireRemoteWipe ? 1 : 0);
        dest.writeInt(mRequireEncryption ? 1 : 0);
        dest.writeInt(mRequireEncryptionExternal ? 1 : 0);
        dest.writeInt(mRequireManualSyncWhenRoaming ? 1 : 0);
        dest.writeInt(mDontAllowCamera ? 1 : 0);
        dest.writeInt(mDontAllowAttachments ? 1 : 0);
        dest.writeInt(mDontAllowHtml ? 1 : 0);
        dest.writeInt(mMaxAttachmentSize);
        dest.writeInt(mMaxTextTruncationSize);
        dest.writeInt(mMaxHtmlTruncationSize);
        dest.writeInt(mMaxEmailLookback);
        dest.writeInt(mMaxCalendarLookback);
        dest.writeInt(mPasswordRecoveryEnabled ? 1 : 0);
        dest.writeString(mProtocolPoliciesEnforced);
        dest.writeSerializable(mUnsupportedList);
        dest.writeSerializable(mIgnoredList);
    }

    /**
     * Supports Parcelable
     */
    public Policy(Parcel in) {
        mBaseUri = CONTENT_URI;
        mId = in.readLong();
        setPasswordMode(in.readInt());
        if (mPasswordMode > Policy.PASSWORD_MODE_NONE) {
            mPasswordEnabled = true;
        }
        mPasswordMinLength = in.readInt();
        mPasswordMaxFails = in.readInt();
        mPasswordHistory = in.readInt();
        mPasswordExpirationDays = in.readInt();
        mPasswordComplexChars = in.readInt();
        mMaxScreenLockTime = in.readInt();
        mRequireRemoteWipe = in.readInt() == 1;
        mRequireEncryption = in.readInt() == 1;
        mRequireEncryptionExternal = in.readInt() == 1;
        mRequireManualSyncWhenRoaming = in.readInt() == 1;
        mDontAllowCamera = in.readInt() == 1;
        mDontAllowAttachments = in.readInt() == 1;
        mDontAllowHtml = in.readInt() == 1;
        mMaxAttachmentSize = in.readInt();
        mMaxTextTruncationSize = in.readInt();
        mMaxHtmlTruncationSize = in.readInt();
        mMaxEmailLookback = in.readInt();
        mMaxCalendarLookback = in.readInt();
        mPasswordRecoveryEnabled = in.readInt() == 1;
        mProtocolPoliciesEnforced = in.readString();
        mUnsupportedList = (ArrayList<Integer>) in.readSerializable();
        mIgnoredList = (ArrayList<Integer>) in.readSerializable();
    }

    private int getPolicyResourceDefine(int tag) {
        switch (tag) {
        case ALLOW_STORAGE_CARD:
            return R.string.policy_dont_allow_storage_cards;
        case ALLOW_UNSIGNED_APPLICATIONS:
            return R.string.policy_dont_allow_unsigned_apps;
        case ALLOW_UNSIGNED_INSTALLATION_PACKAGES:
            return R.string.policy_dont_allow_unsigned_installers;
        case ALLOW_WIFI:
            return R.string.policy_dont_allow_wifi;
        case ALLOW_TEXT_MESSAGING:
            return R.string.policy_dont_allow_text_messaging;
        case ALLOW_POP_IMAP_EMAIL:
            return R.string.policy_dont_allow_pop_imap;
        case ALLOW_IRDA:
            return R.string.policy_dont_allow_irda;
        case ALLOW_HTML_EMAIL:
            return R.string.policy_dont_allow_html;
        case ALLOW_BROWSER:
            return R.string.policy_dont_allow_browser;
        case ALLOW_CONSUMER_EMAIL:
            return R.string.policy_dont_allow_consumer_email;
        case ALLOW_INTERNET_SHARING:
            return R.string.policy_dont_allow_internet_sharing;
        case ALLOW_BLUETOOTH:
            return R.string.policy_bluetooth_restricted;
        case REQUIRE_DEVICE_ENCRYPTION:
            return R.string.policy_require_encryption;
        case DEVICE_ENCRYPTION_ENABLED:
            return R.string.policy_require_sd_encryption;
        case REQUIRE_SIGNED_SMIME_MESSAGES:
        case REQUIRE_ENCRYPTED_SMIME_MESSAGES:
        case REQUIRE_SIGNED_SMIME_ALGORITHM:
        case REQUIRE_ENCRYPTION_SMIME_ALGORITHM:
            return R.string.policy_require_smime;
        case UNAPPROVED_IN_ROM_APPLICATION_LIST:
            return R.string.policy_app_blacklist;
        case APPROVED_APPLICATION_LIST:
            return R.string.policy_app_whitelist;
        case MAX_EMAIL_BODY_TRUNCATION_SIZE:
            return R.string.policy_text_truncation;
        case MAX_EMAIL_HTML_BODY_TRUNCATION_SIZE:
            return R.string.policy_html_truncation;
        default:
            return 0;
        }
    }

    public void addPolicy(Policy inPolicy) {
        // NOTE - when adding policies in, we always have to take the most restrictive value
        setPasswordMode(Math.max(mPasswordMode, inPolicy.mPasswordMode));
        if (mPasswordMode > Policy.PASSWORD_MODE_NONE) {
            mPasswordEnabled = true;
        }
        mPasswordMinLength = Math.max(mPasswordMinLength, inPolicy.mPasswordMinLength);
        mPasswordComplexChars = Math.max(mPasswordComplexChars, inPolicy.mPasswordComplexChars);
        mPasswordHistory = Math.max(mPasswordHistory, inPolicy.mPasswordHistory);

        // The following are set to MAX_VALUE as the default - ignore them if not set by the policy
        // NOTE: while MAX_VALUE is the default the value save to the database is 0
        // so we need to check for both.  As well when the Android Policy Manager is asking for this value
        // we give them a value of 0 if not set (which to the APM means not set).
        if (inPolicy.mPasswordExpirationDays != Integer.MAX_VALUE && inPolicy.mPasswordExpirationDays != 0) {
            mPasswordExpirationDays = Math.min(mPasswordExpirationDays, inPolicy.mPasswordExpirationDays);
        }
        if (inPolicy.mPasswordMaxFails != Integer.MAX_VALUE && inPolicy.mPasswordMaxFails != 0) {
            mPasswordMaxFails = Math.min(mPasswordMaxFails, inPolicy.mPasswordMaxFails);
        }
        if (inPolicy.mMaxScreenLockTime != Integer.MAX_VALUE && inPolicy.mMaxScreenLockTime != 0) {
            mMaxScreenLockTime = Math.min(mMaxScreenLockTime, inPolicy.mMaxScreenLockTime);
        }

        mRequireRemoteWipe |= inPolicy.mRequireRemoteWipe;
        mRequireEncryption |= inPolicy.mRequireEncryption;
        mDontAllowCamera |= inPolicy.mDontAllowCamera;
    }
}