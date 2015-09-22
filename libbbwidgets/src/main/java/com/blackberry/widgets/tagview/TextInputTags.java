package com.blackberry.widgets.tagview;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.Adapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

/**
 * A Tags View which has an EditText at the end for user input
 */
public abstract class TextInputTags<T> extends BaseTags<T> {
    private EditText mEditText;
    /**
     * If the View is in the input state or not
     *
     * @see #setIsInInputState(boolean)
     */
    private boolean mIsInInputState = false;

    /**
     * @param context The context
     * @param attrs   The xml attributes
     * @param layout  The layout resource ID to use.
     * @see com.blackberry.widgets.tagview.BaseTags#BaseTags(android.content.Context,
     * android.util.AttributeSet, int)
     */
    protected TextInputTags(Context context, AttributeSet attrs, int layout, Class<T> dataClass) {
        super(context, attrs, layout, BaseTag.class, dataClass);

        init();
    }

    /**
     * Initialize the View.
     */
    private void init() {
//        getTagListView().setOnInputStateChangedListener(new WrapTagListView
//                .OnInputStateChangedListener() {
//            @Override
//            public void onInputStateChanged(boolean isInInputState) {
//                setIsInInputState(isInInputState);
//            }
//        });
    }

    /**
     * Call this to update the input state and ensure the View appears correctly.
     *
     * @param isInInputState Whether or not this View is in the input state or not
     */
    private void setIsInInputState(boolean isInInputState) {
        mIsInInputState = isInInputState;
        updateChildrensVisibility();
    }

    /**
     * Clear the input text
     */
    public void clearText() {
//        if (mEditText != null) {
//            mEditText.setText("");
//        }
    }

    @Override
    protected void updateChildrensVisibility() {
        super.updateChildrensVisibility();
    }

    /**
     * This is called when the EditText is created by the {@link com.blackberry.widgets.tagview
     * .AppendEditTextAdapter}. Subclasses can override this to customize any properties of the
     * EditText.
     * <p/>
     * <strong>WARNING! If overriding this method you MUST call the super implementation or the
     * control will not function properly!</strong>
     *
     * @param editText The EditText that was created.
     */
    protected void onEditTextCreated(EditText editText) {
        mEditText = editText;
        editText.setImeOptions(EditorInfo.IME_ACTION_GO);
        editText.setOnEditorActionListener(new OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId,
                                          KeyEvent event) {
                if ((actionId == EditorInfo.IME_ACTION_GO)
                        // This part is for hardware keyboard in the simulator
                        || ((event.getAction() == KeyEvent.ACTION_DOWN)
                        && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER))) {
//                    createTag();
                    // without this we lose focus
                    v.requestFocus();
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState ss = new SavedState(super.onSaveInstanceState());
        ss.mEditTextSavedState = mEditText.onSaveInstanceState();
        ss.mIsInInputState = mIsInInputState;
        return ss;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        mEditText.onRestoreInstanceState(ss.mEditTextSavedState);
        setIsInInputState(ss.mIsInInputState);
    }

    /**
     * Used for saving state
     */
    static class SavedState extends BaseSavedState {
        Parcelable mEditTextSavedState = null;
        boolean mIsInInputState = false;

        /**
         * @param superState The saved-state Parcelable from the super class.
         */
        SavedState(Parcelable superState) {
            super(superState);
        }

        /**
         * @param in The parcel to unpack.
         */
        SavedState(Parcel in) {
            super(in);
            mEditTextSavedState = in.readParcelable(null);
            mIsInInputState = in.readByte() != 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeParcelable(mEditTextSavedState, 0);
            dest.writeByte((byte) (mIsInInputState ? 1 : 0));
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}
