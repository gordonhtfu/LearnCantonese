
package com.blackberry.email.ui.compose;

import java.util.HashMap;
import java.util.Map.Entry;

import com.android.ex.chips.RecipientEditTextView;

import android.text.Editable;
import android.text.TextWatcher;
import android.text.util.Rfc822Token;
import android.text.util.Rfc822Tokenizer;

// There is a big difference between the text associated with an address changing
// to add the display name or to format properly and a recipient being added or deleted.
// Make sure we only notify of changes when a recipient has been added or deleted.
public class RecipientTextWatcher implements TextWatcher {
    private HashMap<String, Integer> mContent = new HashMap<String, Integer>();

    private RecipientEditTextView mView;

    private TextWatcher mListener;

    public RecipientTextWatcher(RecipientEditTextView view, TextWatcher listener) {
        mView = view;
        mListener = listener;
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (hasChanged()) {
            mListener.afterTextChanged(s);
        }
    }

    private boolean hasChanged() {
        String[] currRecips = tokenizeRecips(getAddressesFromList(mView));
        int totalCount = currRecips.length;
        int totalPrevCount = 0;
        for (Entry<String, Integer> entry : mContent.entrySet()) {
            totalPrevCount += entry.getValue();
        }
        if (totalCount != totalPrevCount) {
            return true;
        }

        for (String recip : currRecips) {
            if (!mContent.containsKey(recip)) {
                return true;
            } else {
                int count = mContent.get(recip) - 1;
                if (count < 0) {
                    return true;
                } else {
                    mContent.put(recip, count);
                }
            }
        }
        return false;
    }

    private String[] tokenizeRecips(String[] recips) {
        // Tokenize them all and put them in the list.
        String[] recipAddresses = new String[recips.length];
        for (int i = 0; i < recips.length; i++) {
            recipAddresses[i] = Rfc822Tokenizer.tokenize(recips[i])[0].getAddress();
        }
        return recipAddresses;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        String[] recips = tokenizeRecips(getAddressesFromList(mView));
        for (String recip : recips) {
            if (!mContent.containsKey(recip)) {
                mContent.put(recip, 1);
            } else {
                mContent.put(recip, (mContent.get(recip)) + 1);
            }
        }
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // Do nothing.
    }

    private String[] getAddressesFromList(RecipientEditTextView list) {
        if (list == null) {
            return new String[0];
        }
        Rfc822Token[] tokens = Rfc822Tokenizer.tokenize(list.getText());
        int count = tokens.length;
        String[] result = new String[count];
        for (int i = 0; i < count; i++) {
            result[i] = tokens[i].toString();
        }
        return result;
    }
}
