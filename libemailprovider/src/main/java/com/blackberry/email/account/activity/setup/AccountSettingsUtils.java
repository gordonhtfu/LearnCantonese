/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.blackberry.email.account.activity.setup;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.net.Uri;
import android.text.Editable;
import android.text.TextUtils;
import android.widget.EditText;

import com.blackberry.account.registry.ListItemDecor;
import com.blackberry.account.registry.ListItemDecor.StandardListItemTemplate;
import com.blackberry.account.registry.MimetypeRegistryContract;
import com.blackberry.account.registry.TextStyle;
import com.blackberry.common.Logging;
import com.blackberry.common.utils.LogUtils;
import com.blackberry.email.VendorPolicyLoader;
import com.blackberry.email.VendorPolicyLoader.Provider;
import com.blackberry.email.provider.AccountBackupRestore;
import com.blackberry.email.provider.EmailMenuProvider;
import com.blackberry.email.provider.contract.Account;
import com.blackberry.email.provider.contract.EmailContent.AccountColumns;
import com.blackberry.email.provider.contract.QuickResponse;
import com.blackberry.email.service.EmailMessagingService;
import com.blackberry.email.service.EmailServiceUtils;
import com.blackberry.email.service.EmailServiceUtils.EmailServiceInfo;
import com.blackberry.email.utils.Utility;
import com.blackberry.lib.emailprovider.R;
import com.blackberry.provider.AccountContract;
import com.blackberry.provider.AccountContract.AccountAttribute;
import com.blackberry.provider.MessageContract;
import com.google.common.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.HashMap;

public class AccountSettingsUtils {

    /** Pattern to match any part of a domain */
    private final static String WILD_STRING = "*";
    /** Will match any, single character */
    private final static char WILD_CHARACTER = '?';
    private final static String DOMAIN_SEPARATOR = "\\.";

    /**
     * Commits the UI-related settings of an account to the provider. This is static so that it can
     * be used by the various account activities. If the account has never been saved, this method
     * saves it; otherwise, it just saves the settings.
     *
     * @param context the context of the caller
     * @param account the account whose settings will be committed
     */
    public static void commitSettings(Context context, Account account) {
        if (!account.isSaved()) {
            registerWithPimProviders(context, account);
            // Set up default quick responses here...
            String[] defaultQuickResponses =
                    context.getResources().getStringArray(R.array.default_quick_responses);
            ContentValues cv = new ContentValues();
            cv.put(QuickResponse.ACCOUNT_KEY, account.mId);
            ContentResolver resolver = context.getContentResolver();
            for (String quickResponse : defaultQuickResponses) {
                // Allow empty entries (some localizations may not want to have the maximum
                // number)
                if (!TextUtils.isEmpty(quickResponse)) {
                    cv.put(QuickResponse.TEXT, quickResponse);
                    resolver.insert(QuickResponse.CONTENT_URI, cv);
                }
            }
        } else {
            ContentValues cv = getAccountContentValues(account);
            account.update(context, cv);
        }

        // Update the backup (side copy) of the accounts
        AccountBackupRestore.backup(context);
    }

    /**
     * Returns a set of content values to commit account changes (not including the foreign keys for
     * the two host auth's and policy) to the database. Does not actually commit anything.
     */
    public static ContentValues getAccountContentValues(Account account) {
        ContentValues cv = new ContentValues();
        cv.put(AccountColumns.DISPLAY_NAME, account.getDisplayName());
        cv.put(AccountColumns.SENDER_NAME, account.getSenderName());
        cv.put(AccountColumns.SIGNATURE, account.getSignature());
        cv.put(AccountColumns.SYNC_INTERVAL, account.mSyncInterval);
        cv.put(AccountColumns.FLAGS, account.mFlags);
        cv.put(AccountColumns.SYNC_LOOKBACK, account.mSyncLookback);
        cv.put(AccountColumns.SECURITY_SYNC_KEY, account.mSecuritySyncKey);
        return cv;
    }

    private static void registerWithPimProviders(Context context, Account account) {
        // call Account Provider - begin pim account creation
        registerAccountWithPimAccountProvider(context, account);
        // now do local save
        account.save(context);
        // now for mimetype registry
        registerAccountWithMimetypeRegistry(context, account);
        // now for MenuProvider as we now have a valid account. Note we can register our menus
        // without an account if
        // we want to, but for now I will keep registration logic grouped as we hope to
        // collapse/reduce calls in future
        EmailMenuProvider.initialize(context);
    }

    private static long registerAccountWithPimAccountProvider(Context context, Account account) {
        long pimAccountId = -1;
        // I am just sticking this here for now.
        final EmailServiceInfo info = EmailServiceUtils.getServiceInfo(context,
                account.mHostAuthRecv.mProtocol);

        ContentValues values = new ContentValues();

        values.put(AccountContract.Account.APPLICATION_NAME, "Email Services");
        values.put(AccountContract.Account.DISPLAY_NAME, account.mDisplayName);
        values.put(AccountContract.Account.NAME, account.mEmailAddress);
        values.put(AccountContract.Account.CAPABILITIES, account.mCapabilities);
        values.put(AccountContract.Account.TYPE, info.accountType);
        values.put(AccountContract.Account.ACCOUNT_ICON, R.drawable.ic_account);

        // setting account status
        if ((account.mFlags & Account.FLAGS_SECURITY_HOLD) != 0
                || (account.mFlags & Account.FLAGS_INCOMPLETE) != 0) {
            values.put(AccountContract.Account.STATUS,
                    AccountContract.Account.FLAG_STATUS_PENDING_CREATION);
        } else {
            values.put(AccountContract.Account.STATUS, AccountContract.Account.FLAG_STATUS_ACTIVE);
        }

        ContentResolver resolver = context.getContentResolver();
        Uri uri = resolver.insert(AccountContract.Account.CONTENT_URI, values);

        if (uri != null) {
            String id = uri.getPathSegments().get(1);
            LogUtils.i(LogUtils.TAG, "New account id is %s", id);
            if (id != null) {
                pimAccountId = Long.parseLong(id);

                // register EmailProvider as MessagingService
                ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
                ops.add(ContentProviderOperation.newInsert(AccountContract.AccountAttribute.CONTENT_URI)
                        .withValue(AccountContract.AccountAttribute.ACCOUNT_KEY, pimAccountId)
                        .withValue(AccountContract.AccountAttribute.PIM_TYPE, AccountAttribute.PIM_TYPE_MESSAGE)
                        .withValue(AccountContract.AccountAttribute.ATTR_NAME, AccountAttribute.ATT_NAME_MESSAGING_SERVICE_PACKAGE)
                        .withValue(AccountContract.AccountAttribute.ATTR_VALUE, context.getPackageName()).build());
                ops.add(ContentProviderOperation.newInsert(AccountContract.AccountAttribute.CONTENT_URI)
                        .withValue(AccountContract.AccountAttribute.ACCOUNT_KEY, pimAccountId)
                        .withValue(AccountContract.AccountAttribute.PIM_TYPE, AccountAttribute.PIM_TYPE_MESSAGE)
                        .withValue(AccountContract.AccountAttribute.ATTR_NAME, AccountAttribute.ATT_NAME_MESSAGING_SERVICE_CLASS)
                        .withValue(AccountContract.AccountAttribute.ATTR_VALUE, EmailMessagingService.class.getName()).build());

                try {
                    resolver.applyBatch(AccountContract.AUTHORITY, ops);
                } catch (Exception e) {
                    LogUtils.w(Logging.LOG_TAG, e, "failed to register EmailMessagingService");
                }
            }
        } else {
            LogUtils.w(LogUtils.TAG, "Unable to insert the new account");
        }

        // update pim account id
        account.mPimAccountId = pimAccountId;

        return pimAccountId;
    }

    private static void registerAccountWithMimetypeRegistry(Context context, Account account) {
        ContentValues values = new ContentValues();

        values.put(MimetypeRegistryContract.TemplateMapping.ACCOUNT_KEY, account.mPimAccountId);
        values.put(MimetypeRegistryContract.TemplateMapping.MIME_TYPE,
                context.getString(R.string.message_mimetype));
        values.put(MimetypeRegistryContract.TemplateMapping.TEMPLATE_ID,
                MimetypeRegistryContract.TemplateMapping.StandardItem);

        context.getContentResolver().insert(MimetypeRegistryContract.TemplateMapping.CONTENT_URI,
                values);

        final String packageName = "com.blackberry.emailservices";
        final String mimeType = context.getString(R.string.message_mimetype);

        ListItemDecor.Registration reg = ListItemDecor.Registration.newInstance(context,
                account.mPimAccountId, packageName, mimeType);

        reg.addIcon()
        .setResourceId(R.drawable.ca_message_unread)
        .setItemState(MessageContract.Message.State.UNREAD)
        .setElementPosition(StandardListItemTemplate.PrimaryIcon.toInt())
        .setTemplateId(MimetypeRegistryContract.TemplateMapping.StandardItem);

        reg.addIcon()
        .setResourceId(R.drawable.ca_message_read)
        .setItemState(MessageContract.Message.State.READ)
        .setElementPosition(StandardListItemTemplate.PrimaryIcon.toInt())
        .setTemplateId(MimetypeRegistryContract.TemplateMapping.StandardItem);

        reg.addIcon()
        .setResourceId(R.drawable.ca_message_draft)
        .setItemState(MessageContract.Message.State.DRAFT)
        .setElementPosition(StandardListItemTemplate.PrimaryIcon.toInt())
        .setTemplateId(MimetypeRegistryContract.TemplateMapping.StandardItem);

        reg.addIcon()
        .setResourceId(R.drawable.ca_message_sent)
        .setItemState(MessageContract.Message.State.SENT)
        .setElementPosition(StandardListItemTemplate.PrimaryIcon.toInt())
        .setTemplateId(MimetypeRegistryContract.TemplateMapping.StandardItem);

        reg.addIcon()
        .setResourceId(R.drawable.ca_message_sending)
        .setItemState(MessageContract.Message.State.OUTGOING_MESSAGE)
        .setElementPosition(StandardListItemTemplate.PrimaryIcon.toInt())
        .setTemplateId(MimetypeRegistryContract.TemplateMapping.StandardItem);

        reg.addTextStyle()
        .setTemplateId(MimetypeRegistryContract.TemplateMapping.StandardItem)
        .setElementPosition(StandardListItemTemplate.PrimaryText.toInt())
        .setItemState(MessageContract.Message.State.UNREAD).addStyle(TextStyle.BOLD);

        reg.commit();

        // this will create a new registration for conversation mimetype
        registerConversationIcons(context, account, packageName);
    }

    private static void registerConversationIcons(Context context, Account account,
            String packageName) {
        String convMimeType = context.getString(R.string.conversation_mimetype);
        ListItemDecor.Registration reg = ListItemDecor.Registration.newInstance(context,
                account.mPimAccountId, packageName, convMimeType);

        HashMap<Integer, Long> iconMap = new HashMap<Integer, Long>(63);

        collectReadConv(iconMap);
        collectUnreadConv(iconMap);
        collectMixedConv(iconMap);
        collectMiscConv(iconMap);

        for (Integer key : iconMap.keySet()) {
            reg.addIcon()
            .setResourceId(key)
            .setItemState(iconMap.get(key))
            .setElementPosition(StandardListItemTemplate.PrimaryIcon.toInt())
            .setTemplateId(MimetypeRegistryContract.TemplateMapping.StandardItem);
        }

        reg.commit();
    }

    private static void collectMiscConv(HashMap<Integer, Long> iconMap) {
        iconMap.put(R.drawable.ca_conv_draft,
                MessageContract.Conversation.State.DRAFT);
        iconMap.put(R.drawable.ca_meeting_read,
                MessageContract.Conversation.State.MEETING_INVITE
                | MessageContract.Conversation.State.READ);
        iconMap.put(R.drawable.ca_meeting_unread,
                MessageContract.Conversation.State.MEETING_INVITE
                | MessageContract.Conversation.State.UNREAD);
    }

    private static void collectMixedConv(HashMap<Integer, Long> iconMap) {
        iconMap.put(R.drawable.ca_conv_mixed,
                MessageContract.Conversation.State.READ | MessageContract.Conversation.State.UNREAD);
        iconMap.put(R.drawable.ca_conv_mixed_error,
                MessageContract.Conversation.State.READ | MessageContract.Conversation.State.UNREAD
                | MessageContract.Conversation.State.ERROR);
        iconMap.put(R.drawable.ca_conv_mixed_filed,
                MessageContract.Conversation.State.READ | MessageContract.Conversation.State.UNREAD
                | MessageContract.Conversation.State.FILED);
        iconMap.put(R.drawable.ca_conv_mixed_filed_error,
                MessageContract.Conversation.State.READ | MessageContract.Conversation.State.UNREAD
                | MessageContract.Conversation.State.FILED
                | MessageContract.Conversation.State.ERROR);
        iconMap.put(R.drawable.ca_conv_mixed_filed_pending,
                MessageContract.Conversation.State.READ | MessageContract.Conversation.State.UNREAD
                | MessageContract.Conversation.State.FILED
                | MessageContract.Conversation.State.PENDING);
        iconMap.put(R.drawable.ca_conv_mixed_filed_sending,
                MessageContract.Conversation.State.READ | MessageContract.Conversation.State.UNREAD
                | MessageContract.Conversation.State.FILED
                | MessageContract.Conversation.State.SENDING);
        iconMap.put(R.drawable.ca_conv_mixed_filed_sent,
                MessageContract.Conversation.State.READ | MessageContract.Conversation.State.UNREAD
                | MessageContract.Conversation.State.FILED
                | MessageContract.Conversation.State.SENT);
        iconMap.put(R.drawable.ca_conv_mixed_pending,
                MessageContract.Conversation.State.READ | MessageContract.Conversation.State.UNREAD
                | MessageContract.Conversation.State.PENDING);
        iconMap.put(R.drawable.ca_conv_mixed_sending,
                MessageContract.Conversation.State.READ | MessageContract.Conversation.State.UNREAD
                | MessageContract.Conversation.State.SENDING);
        iconMap.put(R.drawable.ca_conv_mixed_sent,
                MessageContract.Conversation.State.READ | MessageContract.Conversation.State.UNREAD
                | MessageContract.Conversation.State.SENT);

    }

    private static void collectUnreadConv(HashMap<Integer, Long> iconMap) {
        iconMap.put(R.drawable.ca_conv_unread,
                MessageContract.Conversation.State.UNREAD);
        iconMap.put(R.drawable.ca_conv_unread_error,
                MessageContract.Conversation.State.UNREAD
                | MessageContract.Conversation.State.ERROR);
        iconMap.put(R.drawable.ca_conv_unread_filed,
                MessageContract.Conversation.State.UNREAD
                | MessageContract.Conversation.State.FILED);
        iconMap.put(R.drawable.ca_conv_unread_filed_error,
                MessageContract.Conversation.State.UNREAD
                | MessageContract.Conversation.State.FILED
                | MessageContract.Conversation.State.ERROR);
        iconMap.put(R.drawable.ca_conv_unread_filed_pending,
                MessageContract.Conversation.State.UNREAD
                | MessageContract.Conversation.State.FILED
                | MessageContract.Conversation.State.PENDING);
        iconMap.put(R.drawable.ca_conv_unread_filed_sending,
                MessageContract.Conversation.State.UNREAD
                | MessageContract.Conversation.State.FILED
                | MessageContract.Conversation.State.SENDING);
        iconMap.put(R.drawable.ca_conv_unread_pending,
                MessageContract.Conversation.State.UNREAD
                | MessageContract.Conversation.State.PENDING);
        iconMap.put(R.drawable.ca_conv_unread_sending,
                MessageContract.Conversation.State.UNREAD
                | MessageContract.Conversation.State.SENDING);

    }

    private static void collectReadConv(HashMap<Integer, Long> iconMap) {
        iconMap.put(R.drawable.ca_conv_read,
                MessageContract.Conversation.State.READ);
        iconMap.put(R.drawable.ca_conv_read_pending,
                MessageContract.Conversation.State.READ
                | MessageContract.Conversation.State.PENDING);
        iconMap.put(R.drawable.ca_conv_read_sending,
                MessageContract.Conversation.State.READ
                | MessageContract.Conversation.State.SENDING);
        iconMap.put(R.drawable.ca_conv_read_sent,
                MessageContract.Conversation.State.READ
                | MessageContract.Conversation.State.SENT);
        iconMap.put(R.drawable.ca_conv_read_error,
                MessageContract.Conversation.State.READ
                | MessageContract.Conversation.State.ERROR);
        iconMap.put(R.drawable.ca_conv_read_filed,
                MessageContract.Conversation.State.READ | MessageContract.Conversation.State.FILED);
        iconMap.put(R.drawable.ca_conv_read_filed_error,
                MessageContract.Conversation.State.READ | MessageContract.Conversation.State.FILED
                | MessageContract.Conversation.State.ERROR);
        iconMap.put(R.drawable.ca_conv_read_filed_pending,
                MessageContract.Conversation.State.READ | MessageContract.Conversation.State.FILED
                | MessageContract.Conversation.State.PENDING);
        iconMap.put(R.drawable.ca_conv_read_filed_sending,
                MessageContract.Conversation.State.READ | MessageContract.Conversation.State.FILED
                | MessageContract.Conversation.State.SENDING);
        iconMap.put(R.drawable.ca_conv_read_filed_sent,
                MessageContract.Conversation.State.READ | MessageContract.Conversation.State.FILED
                | MessageContract.Conversation.State.SENT);
    }

    /**
     * Search the list of known Email providers looking for one that matches the user's email
     * domain. We check for vendor supplied values first, then we look in providers_product.xml, and
     * finally by the entries in platform providers.xml. This provides a nominal override
     * capability. A match is defined as any provider entry for which the "domain" attribute
     * matches.
     *
     * @param domain The domain portion of the user's email address
     * @return suitable Provider definition, or null if no match found
     */
    public static Provider findProviderForDomain(Context context, String domain) {
        Provider p = VendorPolicyLoader.getInstance(context).findProviderForDomain(domain);
        if (p == null) {
            p = findProviderForDomain(context, domain, R.xml.providers_product);
        }
        if (p == null) {
            p = findProviderForDomain(context, domain, R.xml.providers);
        }
        return p;
    }

    /**
     * Search a single resource containing known Email provider definitions.
     *
     * @param domain The domain portion of the user's email address
     * @param resourceId Id of the provider resource to scan
     * @return suitable Provider definition, or null if no match found
     */
    /* package */static Provider findProviderForDomain(
            Context context, String domain, int resourceId) {
        try {
            XmlResourceParser xml = context.getResources().getXml(resourceId);
            int xmlEventType;
            Provider provider = null;
            while ((xmlEventType = xml.next()) != XmlResourceParser.END_DOCUMENT) {
                if (xmlEventType == XmlResourceParser.START_TAG
                        && "provider".equals(xml.getName())) {
                    String providerDomain = getXmlAttribute(context, xml, "domain");
                    try {
                        if (matchProvider(domain, providerDomain)) {
                            provider = new Provider();
                            provider.id = getXmlAttribute(context, xml, "id");
                            provider.label = getXmlAttribute(context, xml, "label");
                            provider.domain = domain.toLowerCase();
                            provider.note = getXmlAttribute(context, xml, "note");
                        }
                    } catch (IllegalArgumentException e) {
                        LogUtils.w(Logging.LOG_TAG, "providers line: " + xml.getLineNumber() +
                                "; Domain contains multiple globals");
                    }
                }
                else if (xmlEventType == XmlResourceParser.START_TAG
                        && "incoming".equals(xml.getName())
                        && provider != null) {
                    provider.incomingUriTemplate = getXmlAttribute(context, xml, "uri");
                    provider.incomingUsernameTemplate = getXmlAttribute(context, xml, "username");
                }
                else if (xmlEventType == XmlResourceParser.START_TAG
                        && "outgoing".equals(xml.getName())
                        && provider != null) {
                    provider.outgoingUriTemplate = getXmlAttribute(context, xml, "uri");
                    provider.outgoingUsernameTemplate = getXmlAttribute(context, xml, "username");
                }
                else if (xmlEventType == XmlResourceParser.END_TAG
                        && "provider".equals(xml.getName())
                        && provider != null) {
                    return provider;
                }
            }
        } catch (Exception e) {
            LogUtils.e(Logging.LOG_TAG, "Error while trying to load provider settings.", e);
        }
        return null;
    }

    /**
     * Returns true if the string <code>s1</code> matches the string <code>s2</code>. The string
     * <code>s2</code> may contain any number of wildcards -- a '?' character -- and/or asterisk
     * characters -- '*'. Wildcards match any single character, while the asterisk matches a domain
     * part (i.e. substring demarcated by a period, '.')
     */
    @VisibleForTesting
    static boolean matchProvider(String testDomain, String providerDomain) {
        String[] testParts = testDomain.split(DOMAIN_SEPARATOR);
        String[] providerParts = providerDomain.split(DOMAIN_SEPARATOR);
        if (testParts.length != providerParts.length) {
            return false;
        }
        for (int i = 0; i < testParts.length; i++) {
            String testPart = testParts[i].toLowerCase();
            String providerPart = providerParts[i].toLowerCase();
            if (!providerPart.equals(WILD_STRING) &&
                    !matchWithWildcards(testPart, providerPart)) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchWithWildcards(String testPart, String providerPart) {
        int providerLength = providerPart.length();
        if (testPart.length() != providerLength) {
            return false;
        }
        for (int i = 0; i < providerLength; i++) {
            char testChar = testPart.charAt(i);
            char providerChar = providerPart.charAt(i);
            if (testChar != providerChar && providerChar != WILD_CHARACTER) {
                return false;
            }
        }
        return true;
    }

    /**
     * Attempts to get the given attribute as a String resource first, and if it fails returns the
     * attribute as a simple String value.
     *
     * @param xml
     * @param name
     * @return the requested resource
     */
    private static String getXmlAttribute(Context context, XmlResourceParser xml, String name) {
        int resId = xml.getAttributeResourceValue(null, name, 0);
        if (resId == 0) {
            return xml.getAttributeValue(null, name);
        }
        else {
            return context.getString(resId);
        }
    }

    /**
     * Infer potential email server addresses from domain names Incoming: Prepend "imap" or "pop3"
     * to domain, unless "pop", "pop3", "imap", or "mail" are found. Outgoing: Prepend "smtp" if
     * domain starts with any in the host prefix array
     *
     * @param server name as we know it so far
     * @param incoming "pop3" or "imap" (or null)
     * @param outgoing "smtp" or null
     * @return the post-processed name for use in the UI
     */
    public static String inferServerName(Context context, String server, String incoming,
            String outgoing) {
        // Default values cause entire string to be kept, with prepended server string
        int keepFirstChar = 0;
        int firstDotIndex = server.indexOf('.');
        if (firstDotIndex != -1) {
            // look at first word and decide what to do
            String firstWord = server.substring(0, firstDotIndex).toLowerCase();
            String[] hostPrefixes =
                    context.getResources().getStringArray(R.array.smtp_host_prefixes);
            boolean canSubstituteSmtp = Utility.arrayContains(hostPrefixes, firstWord);
            boolean isMail = "mail".equals(firstWord);
            // Now decide what to do
            if (incoming != null) {
                // For incoming, we leave imap/pop/pop3/mail alone, or prepend incoming
                if (canSubstituteSmtp || isMail) {
                    return server;
                }
            } else {
                // For outgoing, replace imap/pop/pop3 with outgoing, leave mail alone, or
                // prepend outgoing
                if (canSubstituteSmtp) {
                    keepFirstChar = firstDotIndex + 1;
                } else if (isMail) {
                    return server;
                } else {
                    // prepend
                }
            }
        }
        return ((incoming != null) ? incoming : outgoing) + '.' + server.substring(keepFirstChar);
    }

    /**
     * Helper to set error status on password fields that have leading or trailing spaces
     */
    public static void checkPasswordSpaces(Context context, EditText passwordField) {
        Editable password = passwordField.getText();
        int length = password.length();
        if (length > 0) {
            if (password.charAt(0) == ' ' || password.charAt(length - 1) == ' ') {
                passwordField.setError(context.getString(R.string.account_password_spaces_error));
            }
        }
    }

}
