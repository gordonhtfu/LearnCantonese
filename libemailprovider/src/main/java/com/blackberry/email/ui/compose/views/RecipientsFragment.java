package com.blackberry.email.ui.compose.views;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.blackberry.common.Rfc822Validator;
import com.blackberry.common.utils.LogTag;
import com.blackberry.email.Account;
import com.blackberry.email.ui.compose.controllers.ComposeActivity;
import com.blackberry.email.ui.compose.controllers.RecipientErrorDialogFragment;
import com.blackberry.email.ui.compose.views.ComposeScreen.FieldID;
import com.blackberry.lib.emailprovider.R;
import com.blackberry.message.service.MessageContactValue;
import com.blackberry.message.service.MessageValue;
import com.blackberry.provider.MessageContract;
import com.blackberry.widgets.tagview.OnTagListChanged;
import com.blackberry.widgets.tagview.contact.email.EmailContact;
import com.blackberry.widgets.tagview.contact.email.EmailTags;

public class RecipientsFragment extends Fragment implements
        OnTagListChanged<EmailContact> {

    private static final String LOG_TAG = LogTag.getLogTag();
    private static final String END_TOKEN = ", ";
    private Rfc822Validator mValidator;

    private EmailTags mTo;
    private EmailTags mCc;
    private EmailTags mBcc;
    private Button mBccButton;
    private BccView mBccView;
    private ViewEditListener mChangeListener;
    private RecipientsFragment.CallBack mCallback;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.compose_recipients,
                container, false);

        mBccView = (BccView) rootView.findViewById(R.id.cc_bcc_wrapper);
        mBccButton = (Button) rootView.findViewById(R.id.add_bcc);
        if (mBccButton != null) {
            mBccButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showHideBccView();
                }
            });
        }

        mTo = (EmailTags) rootView.findViewById(R.id.ToEmailTag);
        mCc = (EmailTags) rootView.findViewById(R.id.CcEmailTag);
        mBcc = (EmailTags) rootView.findViewById(R.id.BccEmailTag);

        setHasOptionsMenu(true);
        initChangeListeners();
        return rootView;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem bccMenuItem = menu.findItem(R.id.add_bcc);
        if (bccMenuItem != null) {
            bccMenuItem.setTitle(isBccVisible() ? R.string.remove_bcc_label
                    : R.string.add_bcc_label);
        }
        super.onPrepareOptionsMenu(menu);
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();
        if (id == R.id.add_bcc) {
            mBcc.clear();
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        // Menu items like send need to be updated based on recipient
        // states, but we need to wait for menu initialization first.
        notifyObserversOfRecipientChanges();
    }

    public void showHideBccView() {
        boolean show = !isBccVisible();
        mBccView.show(true, show);
        if (!show) {
            mBcc.clear();
        }
        if (mBccButton != null) {
            mBccButton.setVisibility(View.INVISIBLE);
        }
        mBcc.setOnTagListChanged(show ? this : null);
        notifyObserversOfRecipientChanges();
    }

    public void initializeFromMessage(String senderAccount, MessageValue message, int forScreen) {

        final List<String> toAddresses = Arrays.asList(message
                .getAddresses(MessageContract.MessageContact.FieldType.TO));
        final List<String> ccAddresses = Arrays.asList(message
                .getAddresses(MessageContract.MessageContact.FieldType.CC));
        final String[] replyToAddresses = message
                .getAddresses(MessageContract.MessageContact.FieldType.REPLY_TO);
        final String replytoAddress = replyToAddresses.length > 0 ? replyToAddresses[0]
                : null;
        final String sender = (replytoAddress == null)
                              ? message.getAddresses(MessageContract.MessageContact.FieldType.FROM)[0]
                              : replytoAddress;

        setValidator(senderAccount);

        switch (forScreen) {
        case ComposeActivity.REPLY:
            final Collection<String> recipients = new HashSet<String>();
            recipients.add(sender);
            addAddresses(recipients, mTo);
            break;

        case ComposeActivity.REPLY_ALL:
            Collection<String> uniqueToAddresses = new HashSet<String>(toAddresses);
            uniqueToAddresses.add(sender);
            uniqueToAddresses.remove(senderAccount);
            addAddresses(uniqueToAddresses, mTo);
            addAddresses(ccAddresses, mCc);
            break;

        case ComposeActivity.EDIT_DRAFT:
            addAddresses(toAddresses, mTo);
            addAddresses(ccAddresses, mCc);
            final List<String> bccAddresses = Arrays.asList(message
                            .getAddresses(MessageContract.MessageContact.FieldType.BCC));
            addAddresses(bccAddresses, mBcc);
            mBccView.show(false, !bccAddresses.isEmpty());
            mBcc.setOnTagListChanged(!bccAddresses.isEmpty() ? this : null);
            if (!bccAddresses.isEmpty()) {
              // Workaround due to BCC field: the field will not report a valid value
              // for getTagValues() because it has not been painted on the screen yet.
              mCallback.recipientsChanged(true);
            }
            break;
        }
    }

    public void initFromMailTo(Uri uri) {
        List<String> cc = uri.getQueryParameters("cc");
        addAddresses(Arrays.asList(cc.toArray(new String[cc.size()])), mCc);

        List<String> otherTo = uri.getQueryParameters("to");
        addAddresses(Arrays
                .asList(otherTo.toArray(new String[otherTo.size()])), mTo);

        List<String> bcc = uri.getQueryParameters("bcc");
        addAddresses(Arrays.asList(bcc.toArray(new String[bcc.size()])), mBcc);
    }

    /**
     * A tag was added.
     * 
     * @param tag
     *            The tag that was added
     */
    public void tagAdded(EmailContact tag) {
        if (mChangeListener != null) {
            mChangeListener.setDirty(true);
        }
        notifyObserversOfRecipientChanges();
    }

    /**
     * A tag was removed.
     * 
     * @param tag
     *            The tag that was removed
     */
    public void tagRemoved(EmailContact tag) {
        if (mChangeListener != null) {
            mChangeListener.setDirty(true);
        }
        notifyObserversOfRecipientChanges();
    }

	/**
     * [NOT IMPLEMENTED YET] A tag was changed. For instance if the user edited
     * the tag or an alternate address was selected for a contact tag.
     * 
     * @param tag
     *            The tag that was changed.
     */
    public void tagChanged(EmailContact tag) {
        if (mChangeListener != null) {
            mChangeListener.setDirty(true);
        }
        notifyObserversOfRecipientChanges();
    }

    private void notifyObserversOfRecipientChanges() {
        if (mCallback != null) {
            boolean hasValidRecipients = false;
            hasValidRecipients = !mTo.getTagValues(EmailTags.TAG_VALID).isEmpty()
                               || !mCc.getTagValues(EmailTags.TAG_VALID).isEmpty()
                               || !mBcc.getTagValues(EmailTags.TAG_VALID).isEmpty();
            mCallback.recipientsChanged(hasValidRecipients);
        }
    }

    public void initChangeListeners() {
        mTo.setOnTagListChanged(this);
        mCc.setOnTagListChanged(this);
        mBcc.setOnTagListChanged(isBccVisible() ? this : null);
    }

    public void clearChangeListeners() {
        mTo.setOnTagListChanged(null);
        mCc.setOnTagListChanged(null);
        if (isBccVisible()) {
            mBcc.setOnTagListChanged(null);
        }
    }

    public void reset() {
        mTo.clear();
        mCc.clear();
        mBcc.clear();
    }

    public boolean isBccVisible() {
        return mBccView.isBccVisible();
    }

    public List<MessageContactValue> getRecipients(FieldID field) {
        List<MessageContactValue> contacts = null;
        if (field.compareTo(FieldID.TO_FIELD) == 0)
            contacts = convertToMessageContact(
                    mTo.getTagValues(EmailTags.TAG_VALID),
                    MessageContract.MessageContact.FieldType.TO);
        else if (field.compareTo(FieldID.CC_FIELD) == 0)
            contacts = convertToMessageContact(
                    mCc.getTagValues(EmailTags.TAG_VALID),
                    MessageContract.MessageContact.FieldType.CC);
        else if (field.compareTo(FieldID.BCC_FIELD) == 0)
            contacts = convertToMessageContact(
                    mBcc.getTagValues(EmailTags.TAG_VALID),
                    MessageContract.MessageContact.FieldType.BCC);
        return contacts;
    }

    public String getFormattedRecipients(FieldID field) {
        String formattedRecipients = "";
        if (field.compareTo(FieldID.TO_FIELD) >= 0
                || field.compareTo(FieldID.BCC_FIELD) <= 0) {
            if (field.compareTo(FieldID.TO_FIELD) == 0)
                formattedRecipients = convertContactListToString(mTo
                        .getTagValues(EmailTags.TAG_VALID));
            if (field.compareTo(FieldID.CC_FIELD) == 0)
                formattedRecipients = convertContactListToString(mCc
                        .getTagValues(EmailTags.TAG_VALID));
            if (field.compareTo(FieldID.BCC_FIELD) == 0)
                formattedRecipients = convertContactListToString(mBcc
                        .getTagValues(EmailTags.TAG_VALID));
        }
        return formattedRecipients;
    }

    private static String[] getEmailAddresses(
            final List<EmailContact> emailContacts) {
        final int size = (emailContacts == null) ? 0 : emailContacts.size();
        String[] addresses = new String[size];

        for (int i = 0; i < size; i++) {
            // TODO: check if there is a active display string
            EmailContact contact = emailContacts.get(i);

            // TODO: Remove this when AVEN-4050 is resolved
            if (contact.getActiveEmailAddressIndex() == -1) {
                contact.setActiveEmailAddressIndex(0);
            }
            addresses[i] = contact.getActiveDisplayString();
        }

        return addresses;
    }

    private String convertContactListToString(
            final List<EmailContact> emailContacts) {
        StringBuilder strBldr = new StringBuilder("");

        for (EmailContact contact : emailContacts) {
            // TODO: Remove this when AVEN-4050 is resolved
            if (!contact.isValid()) {
                contact.setActiveEmailAddressIndex(0);
            }

            strBldr.append(contact.getName());
            strBldr.append(":");
            strBldr.append(contact.getActiveEmailAddress().getValue());
            strBldr.append(",");
        }

        String formattedRecipients = strBldr.toString();
        if (!TextUtils.isEmpty(formattedRecipients)
                && formattedRecipients.charAt(formattedRecipients.length() - 1) == ',')
            formattedRecipients = formattedRecipients.substring(0,
                    formattedRecipients.length() - 1);

        return formattedRecipients;
    }

    private List<MessageContactValue> convertToMessageContact(
            final List<EmailContact> emailContacts, int contactType) {
        List<MessageContactValue> contactList = new ArrayList<MessageContactValue>();

        for (EmailContact emailContact : emailContacts) {
            // TODO: Remove this when AVEN-4050 is resolved
            if (!emailContact.isValid()) {
                emailContact.setActiveEmailAddressIndex(0);
            }

            MessageContactValue contact = new MessageContactValue();
            contact.mName = emailContact.getName();
            contact.mAddress = emailContact.getActiveEmailAddress().getValue();
            contact.mFieldType = contactType;
            contact.mAddressType = MessageContract.MessageContact.AddrType.EMAIL;

            contactList.add(contact);
        }

        return contactList;
    }

    // We need a wrapper over {@link EmailTags#addEmailAddresses(String)} to
    // broadcast add signals to the editor, as EmailTags do not notify us of
    // programmatic additions.
    private void addAddresses(Collection<String> addresses, EmailTags tag) {
        if (addresses.size() > 0) {
            tag.addAllEmailAddresses(addresses);
            notifyObserversOfRecipientChanges();
        }
    }

    private void setValidator(String accountName) {
        if (mValidator == null) {
            int offset = accountName.indexOf("@") + 1;
            String accountStr = accountName;
            if (offset > 0) {
                accountStr = accountStr.substring(offset);
            }
            mValidator = new Rfc822Validator(accountStr);
        }
    }

    public void checkInvalidEmails(final String[] to,
            final List<String> wrongEmailsOut) {
        if (mValidator == null) {
            return;
        }
        for (final String email : to) {
            if (!mValidator.isValid(email)) {
                wrongEmailsOut.add(email);
            }
        }
    }

    public boolean validateRecipents(final boolean save,
            final boolean orientationChanged) {
        final String[] to, cc, bcc;
        if (orientationChanged) {
            to = cc = bcc = new String[0];
        } else {
            to = getToAddresses();
            cc = getCcAddresses();
            bcc = getBccAddresses();
        }

        // Don't let the user send to nobody (but it's okay to save a message
        // with no recipients)
        if (!save && (to.length == 0 && cc.length == 0 && bcc.length == 0)) {
            showRecipientErrorDialog(getString(R.string.recipient_needed));
            return false;
        }

        List<String> wrongEmails = new ArrayList<String>();
        if (!save) {
            checkInvalidEmails(to, wrongEmails);
            checkInvalidEmails(cc, wrongEmails);
            checkInvalidEmails(bcc, wrongEmails);
        }

        // Don't let the user send an email with invalid recipients
        if (wrongEmails.size() > 0) {
            String errorText = String.format(
                    getString(R.string.invalid_recipient), wrongEmails.get(0));
            showRecipientErrorDialog(errorText);
            return false;
        }
        return true;
    }

    /**
     * Get the to recipients.
     */
    public String[] getToAddresses() {
        return getEmailAddresses(mTo.getTagValues());
    }

    /**
     * Get the cc recipients.
     */
    public String[] getCcAddresses() {
        return getEmailAddresses(mCc.getTagValues());
    }

    /**
     * Get the bcc recipients.
     */
    public String[] getBccAddresses() {
        return getEmailAddresses(mBcc.getTagValues());
    }

    private void showRecipientErrorDialog(final String message) {
        final DialogFragment frag = RecipientErrorDialogFragment
                .newInstance(message);
        frag.show(getFragmentManager(), "recipient error");
    }

    public void requestFocus() {
        mTo.requestFocus();
    }

    public void observeEditsOnView(ViewEditListener listener) {
        mChangeListener = listener;
    }

    public void registerForRecipientChanges(RecipientsFragment.CallBack callback) {
        if (callback != null) {
            mCallback = callback;
        }
    }

    interface CallBack {
       public void recipientsChanged(boolean containsValidTags);
    }
}
