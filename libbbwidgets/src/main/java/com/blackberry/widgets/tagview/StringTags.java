package com.blackberry.widgets.tagview;

import android.content.Context;
import android.util.AttributeSet;

import java.util.List;

/**
 * @author tallen
 *         <p/>
 *         A simple class which handles string input. No completions and no related tags. If the
 *         user wants these they can subclass and implement them.
 */
public class StringTags extends BaseTags<String> {

    /**
     * @param context The context
     */
    public StringTags(Context context) {
        this(context, null);
    }

    /**
     * @param context The context
     * @param attrs   The xml attributes
     */
    public StringTags(Context context, AttributeSet attrs) {
        super(context, attrs, BaseTag.class, String.class);
    }

    @Override
    protected String createTagDataItem(CharSequence inputText) {
        return inputText.toString();
    }
}
