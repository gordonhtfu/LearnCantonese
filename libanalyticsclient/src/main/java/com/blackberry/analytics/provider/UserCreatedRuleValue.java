
package com.blackberry.analytics.provider;

import java.util.List;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.blackberry.message.service.MessageContactValue;
import com.blackberry.message.service.MessageValue;
import com.blackberry.provider.MessageContract.Message;
import com.blackberry.provider.MessageContract.MessageContact;

import com.blackberry.analytics.provider.AnalyticsContract.UserCreatedRule;

/**
 * Convenience class for working with UserCreatedRules.
 */
public class UserCreatedRuleValue implements Parcelable {

    /**
     * the id of this rule.
     */
    public long mId = -1;

    /**
     * The name of the rule as set by the User.
     */
    public String mName;

    /**
     * Flag indicating whether rule is enabled.
     */
    public boolean mEnabled;

    /**
     * Unique identifier for the account the rule applies too.
     */
    public long mAccountId;

    /**
     * User readable name for the account the rule applies too.
     */
    public String mAccountName;

    /**
     * Flag indicating whether the rule is visible.
     */
    public boolean mVisible;

    /**
     * Flag indicating whether the rule is a Level 1 rule.
     */
    public boolean mIsLevel1;

    /**
     * Address of the sender the rule applies to.
     */
    public String mSender;

    /**
     * Address of the recipient the rule applies to.
     */
    public String mRecipient;

    /**
     * String to match against the subject of the message.
     */
    public String mSubject;

    /**
     * Flag representing the importance the rule will match against.
     */
    public int mImportance = -1;

    /**
     * Flag indicating whether rule applies to messages sent directly to the
     * recipient.
     */
    public boolean mSentDirectlyToMe;

    /**
     * Flag indicating whether rule applies to messages where user was cc'd.
     */
    public boolean mCcToMe;

    /**
     * Flag indicating whether rule applies to enterprise messages.
     */
    public boolean mEnterprise;
    /**
     * supports parcelable.
     */
    public static final Parcelable.Creator<UserCreatedRuleValue> CREATOR =
            new Parcelable.Creator<UserCreatedRuleValue>() {
                @Override
                public UserCreatedRuleValue createFromParcel(Parcel in) {
                    return new UserCreatedRuleValue(in);
                }

                @Override
                public UserCreatedRuleValue[] newArray(int size) {
                    return new UserCreatedRuleValue[size];
                }
            };

    /**
     * creates empty instance.
     */
    public UserCreatedRuleValue() {
    }

    /**
     * instantiate from db.
     */
    public UserCreatedRuleValue(Cursor cursor) {
        setValues(cursor);
    }

    /**
     * Supports Parcelable.
     * 
     * @param in the parcel to create the contact from
     */
    public UserCreatedRuleValue(Parcel in) {
        setValues(ContentValues.CREATOR.createFromParcel(in));
    }

    /**
     * Create a ContentValues representation of this rule.
     * 
     * @return the content values
     */
    public ContentValues toContentValues(boolean excludeId) {
        ContentValues values = new ContentValues();
        if (!excludeId) {
            values.put(UserCreatedRule._ID, mId);
        }
        values.put(UserCreatedRule.NAME, mName);
        values.put(UserCreatedRule.ACCOUNT_ID, mAccountId);
        values.put(UserCreatedRule.ACCOUNT_NAME, mAccountName);
        values.put(UserCreatedRule.ENABLED, mEnabled);
        values.put(UserCreatedRule.VISIBLE, mVisible);
        values.put(UserCreatedRule.IS_LEVEL_1, mIsLevel1);
        values.put(UserCreatedRule.SENDER, mSender != null ? mSender : "");
        values.put(UserCreatedRule.RECIPIENT, mRecipient != null ? mRecipient : "");
        values.put(UserCreatedRule.SUBJECT, mSubject != null ? mSubject : "");
        values.put(UserCreatedRule.IMPORTANCE, mImportance);
        values.put(UserCreatedRule.SENT_DIRECTLY_TO_ME, mSentDirectlyToMe);
        values.put(UserCreatedRule.CC_TO_ME, mCcToMe);
        values.put(UserCreatedRule.ENTERPRISE, mEnterprise);

        return values;
    }

    /**
     * set values in class using ContentValues.
     */
    public void setValues(ContentValues values) {
        if (values.containsKey(UserCreatedRule._ID)) {
            mId = values.getAsLong(UserCreatedRule._ID);
        }
        mName = values.getAsString(UserCreatedRule.NAME);
        mAccountId = values.getAsLong(UserCreatedRule.ACCOUNT_ID);
        mAccountName = values.getAsString(UserCreatedRule.ACCOUNT_NAME);
        if (values.containsKey(UserCreatedRule.ENABLED)) {
            mEnabled = values.getAsBoolean(UserCreatedRule.ENABLED);
        }
        if (values.containsKey(UserCreatedRule.VISIBLE)) {
            mVisible = values.getAsBoolean(UserCreatedRule.VISIBLE);
        }
        if (values.containsKey(UserCreatedRule.IS_LEVEL_1)) {
            mIsLevel1 = values.getAsBoolean(UserCreatedRule.IS_LEVEL_1);
        }
        if (values.containsKey(UserCreatedRule.SENDER)) {
            mSender = values.getAsString(UserCreatedRule.SENDER);
        }
        if (values.containsKey(UserCreatedRule.RECIPIENT)) {
            mRecipient = values.getAsString(UserCreatedRule.RECIPIENT);
        }
        if (values.containsKey(UserCreatedRule.SUBJECT)) {
            mSubject = values.getAsString(UserCreatedRule.SUBJECT);
        }
        if (values.containsKey(UserCreatedRule.IMPORTANCE)) {
            mImportance = values.getAsInteger(UserCreatedRule.IMPORTANCE);
        }
        if (values.containsKey(UserCreatedRule.SENT_DIRECTLY_TO_ME)) {
            mSentDirectlyToMe = values.getAsBoolean(UserCreatedRule.SENT_DIRECTLY_TO_ME);
        }
        if (values.containsKey(UserCreatedRule.CC_TO_ME)) {
            mCcToMe = values.getAsBoolean(UserCreatedRule.CC_TO_ME);
        }
        if (values.containsKey(UserCreatedRule.ENTERPRISE)) {
            mEnterprise = values.getAsBoolean(UserCreatedRule.ENTERPRISE);
        }
    }

    public void setValues(Cursor cursor) {
        ContentValues values = new ContentValues();

        DatabaseUtils.cursorLongToContentValuesIfPresent(cursor, values, UserCreatedRule._ID);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values, UserCreatedRule.NAME);
        DatabaseUtils.cursorLongToContentValuesIfPresent(cursor, values, UserCreatedRule.ACCOUNT_ID);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values, UserCreatedRule.ACCOUNT_NAME);
        DatabaseUtils.cursorIntToContentValuesIfPresent(cursor, values, UserCreatedRule.ENABLED);
        DatabaseUtils.cursorIntToContentValuesIfPresent(cursor, values, UserCreatedRule.VISIBLE);
        DatabaseUtils.cursorIntToContentValuesIfPresent(cursor, values, UserCreatedRule.IS_LEVEL_1);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values, UserCreatedRule.SENDER);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values, UserCreatedRule.RECIPIENT);
        DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values, UserCreatedRule.SUBJECT);
        DatabaseUtils.cursorIntToContentValuesIfPresent(cursor, values, UserCreatedRule.IMPORTANCE);
        DatabaseUtils.cursorIntToContentValuesIfPresent(cursor, values, UserCreatedRule.SENT_DIRECTLY_TO_ME);
        DatabaseUtils.cursorIntToContentValuesIfPresent(cursor, values, UserCreatedRule.CC_TO_ME);
        DatabaseUtils.cursorIntToContentValuesIfPresent(cursor, values, UserCreatedRule.ENTERPRISE);

        setValues(values);

    }

    /**
     * Inserts a UserCreatedRule for the specified sender.
     * 
     * @param context
     * @param name rule name
     * @param accountId account id to apply rule against
     * @param sender address of sender
     */
    public static Uri insertUserCreatedSenderRule(ContentProvider contentProvider, String name, Long accountId,
            String accountName, String sender) {

        int columnCount = 3;
        ContentValues values = new ContentValues(columnCount);
        values.put(UserCreatedRule.NAME, name);
        values.put(UserCreatedRule.ACCOUNT_ID, accountId);
        values.put(UserCreatedRule.ACCOUNT_NAME, accountName);
        values.put(UserCreatedRule.SENDER, sender);

        return contentProvider.insert(UserCreatedRule.CONTENT_URI, values);
    }

    /**
     * Tests if this rule should be applied to the supplied message. Message
     * must be from the same account and be enabled for it to apply.
     *
     * @param msg message to check if the rule is applicable
     * @return true if the rule should be checked, false otherwise
     */
    public boolean shouldApply(MessageValue msg) {
        return msg.mAccountId == mAccountId && ((msg.mState & Message.State.DRAFT) == 0)
                && ((msg.mState & Message.State.SENT) == 0)
                && ((msg.mState & Message.State.OUTGOING_MESSAGE) == 0);
    }

    /**
     * Checks if this message matches this rule.
     *
     * @param msg the message to check
     * @return true if message is Priority 1.
     */
    public boolean matches(MessageValue msg) {
        boolean matches = false;
        if (mSubject != null && !mSubject.isEmpty()) {
            matches = msg.mSubject.toLowerCase().contains(mSubject.toLowerCase());
            if (!matches) {
                return matches;
            }
        }
        if (processMessageContacts()) {
            
            List<MessageContactValue> contacts = msg.getContacts();
            for (MessageContactValue contact : contacts) {
                if (mSender != null && !mSender.isEmpty()) {
                    MessageContactValue sender = null;
                    if (contact.mFieldType == MessageContact.FieldType.FROM) {
                        sender = contact;
                    }
                    String address = sender != null ? sender.mAddress : "";
                    matches = address.equals(mSender);
                    if (!matches) {
                        continue;
                    }
                }
                if (mRecipient != null && !mRecipient.isEmpty()) {
                    MessageContactValue recipient = null;
                    if (contact.mFieldType == MessageContact.FieldType.TO) {
                        recipient = contact;
                    }
                    String address = recipient != null ? recipient.mAddress : "";
                    matches = address.equals(mRecipient);
                    if (!matches) {
                        continue;
                    }
                }
                if (mSentDirectlyToMe) {
                    if (contact.mFieldType == MessageContact.FieldType.TO) {
                        matches = contact.mAddress.equalsIgnoreCase(mAccountName);
                        if (!matches) {
                            continue;
                        }
                    }
                }
                if (mCcToMe) {
                    if (contact.mFieldType == MessageContact.FieldType.CC
                            || contact.mFieldType == MessageContact.FieldType.BCC) {
                        matches = contact.mAddress.equalsIgnoreCase(mAccountName);
                        if (!matches) {
                            continue;
                        }
                    }
                }
                if (matches) {
                    break;
                }
            }
            if (!matches) {
                return matches;
            }
        }
        //  matching for remaining attributes will go here
        
        return matches;
    }

    private boolean processMessageContacts() {
        return ((mSender != null && !mSender.isEmpty())
                || (mSubject != null && !mSubject.isEmpty())
                || (mRecipient != null && !mRecipient.isEmpty())
                || mSentDirectlyToMe || mCcToMe);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        toContentValues(false).writeToParcel(dest, flags);
    }

}
