
package com.blackberry.email.ui.compose;

import android.database.DataSetObserver;
import android.text.TextWatcher;

public class RecipientTagWatcher extends DataSetObserver {
    private TextWatcher mListener;

    public RecipientTagWatcher(TextWatcher listener) {
        mListener = listener;
    }

    public void onChanged() {
        mListener.afterTextChanged(null);
    }

    public void onInvalidated() {
        mListener.afterTextChanged(null);
    }
}
