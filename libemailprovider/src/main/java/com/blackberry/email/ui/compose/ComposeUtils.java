package com.blackberry.email.ui.compose;

import android.content.res.Resources;
import android.text.TextUtils;

import com.blackberry.email.Account;
import com.blackberry.email.ui.compose.controllers.ComposeActivity;
import com.blackberry.lib.emailprovider.R;

public class ComposeUtils {

    /**
     * Returns a formatted subject string with the appropriate prefix for the action type.
     * E.g., "FWD: " is prepended if action is {@link ComposeActivity#FORWARD}.
     */
    public static String buildFormattedSubject(Resources res, String subject, int action) {
        String prefix;
        String correctedSubject = null;

        if (action == ComposeActivity.EDIT_DRAFT) {
            return subject;
        }

        if (subject == null){
            subject = "";
        }

        if (action == ComposeActivity.COMPOSE) {
            prefix = "";
        } else if (action == ComposeActivity.FORWARD) {
            prefix = res.getString(R.string.forward_subject_label);
        } else {
            prefix = res.getString(R.string.reply_subject_label);
        }

        // Don't duplicate the prefix
        if (!TextUtils.isEmpty(subject)
                && subject.toLowerCase().startsWith(prefix.toLowerCase())) {
            correctedSubject = subject;
        } else {
            correctedSubject = String.format(
                    res.getString(R.string.formatted_subject), prefix, subject);
        }

        return correctedSubject;
    }

    public static long getAccountID(Account account) {
    	final String accURIString = account.uri.toString();
        return Long.parseLong(accURIString.substring(accURIString.lastIndexOf("/") + 1,
                              accURIString.length()));
    }
}
