package com.blackberry.widgets.tagview;

import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;

import com.blackberry.widgets.WrappedAdapter;

/**
 * An Adapter which will soft-focus the first item in the wrapped Adapter if it is soft focusable.
 */
public class SoftFocusAdapter extends WrappedAdapter {
    /**
     * Constructor
     */
    public SoftFocusAdapter() {
        super();
    }

    /**
     * @param wrappedAdapter The Adapter to wrap
     */
    public SoftFocusAdapter(Adapter wrappedAdapter) {
        super(wrappedAdapter);
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        View v = super.getView(i, view, viewGroup);
        if (v instanceof ISoftFocusable) {
            // From the looks of playing with the BB10 TokenEntry index 0 always has soft focus.
            // This is *contrary* to the original specifications
            ((ISoftFocusable) v).setSoftFocus(i == 0);
        }
        return v;
    }
}
