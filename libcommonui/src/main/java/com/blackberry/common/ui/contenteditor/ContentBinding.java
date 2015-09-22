package com.blackberry.common.ui.contenteditor;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;


/**
 * This class is used to bind data that is retrieved from a Cursor to a View. It supports loading
 * data from the Cursor, saving data from the View to a ContentValues object, and tracking the
 * 'dirty' status of the data to ensure that the minimal amount of data is saved.
 * 
 * @param <T> The type of the View that the binding implementation supports.
 * @see ContentEditorFragment
 */
public abstract class ContentBinding<T extends View> {

    private final T mField;
    private final String mKey;
    private boolean mIsDirty = false;

    /**
     * @param key The key for the data in the Cursor and ContentValues that will
     *            be bound to the given view.
     * @param view The view to bind data to.
     */
    public ContentBinding(String key, T view) {
        mKey = key;
        mField = view;
    }

    /**
     * Load data from this Cursor into the field.
     * 
     * @param c The Cursor to load data from.
     * @throws IllegalArgumentException If the key for this binding cannot be found in the given
     *             Cursor.
     */
    protected abstract void loadData(Cursor c);

    /**
     * Save the data in this field to the values.
     *
     * @param values The values to add the data to.
     */
    public abstract void saveData(ContentValues values);

    /**
     * Get the key to use for retrieving data from the Cursor, and for saving to ContentValues.
     * 
     * @return The string key for the data.
     */
    protected String getKey() {
        return mKey;
    }

    /**
     * Check whether the current data in the view is dirty, and requires saving. Note that
     * subclasses may override this method to provide custom dirty tracking.
     * 
     * @return True if the data is dirty and needs to be saved, else false.
     */
    protected boolean isDirty() {
        return mIsDirty;
    }

    /**
     * Get the view for this binding.
     * 
     * @return The view for this binding.
     */
    protected T getField() {
        return mField;
    }

    /**
     * Set the dirty status.
     * 
     * @param isDirty
     */
    protected void setDirty(boolean isDirty) {
        mIsDirty = isDirty;
    }

    /**
     * Save the data in this field to the values, only if the data is dirty.
     * Calling this method will mark the data as 'clean'.
     *
     * @param values The values to add the data to.
     */
    public void saveDirtyData(ContentValues values) {
        if (isDirty()) {
            saveData(values);
            mIsDirty = false;
        }
    }

    /**
     * Load data from the given cursor and set it on the view.
     * 
     * @param c The cursor to load data from.
     */
    public void loadFrom(Cursor c) {
        loadData(c);
        mIsDirty = false;
    }


    /**
     * ContentBinding for a CheckBox view.
     */
    public static class CheckBoxContentBinding extends ContentBinding<CheckBox> {

        private boolean mCachedValue;

        /**
         * @param key The key for the data.
         * @param field The CheckBox to bind to.
         */
        public CheckBoxContentBinding(String key, CheckBox field) {
            super(key, field);
        }

        @Override
        public boolean isDirty() {
            return mCachedValue != getField().isChecked();
        }

        @Override
        public void loadData(Cursor c) {
            mCachedValue = c.getInt(c.getColumnIndexOrThrow(getKey())) == 1;
            getField().setChecked(mCachedValue);
        }

        @Override
        public void saveData(ContentValues values) {
            mCachedValue = getField().isChecked();
            values.put(getKey(), mCachedValue ? 1 : 0);
        }
    }
    
    /**
     * ContentBinding for a Spinner.
     */
    public static class SpinnerContentBinding extends ContentBinding<Spinner> {

        private int mCachedValue;

        /**
         * @param key The key for the data.
         * @param field The Spinner to bind to.
         */
        public SpinnerContentBinding(String key, Spinner field) {
            super(key, field);
        }

        @Override
        public boolean isDirty() {
            return mCachedValue != getField().getSelectedItemPosition();
        }

        @Override
        public void loadData(Cursor c) {
            mCachedValue = c.getInt(c.getColumnIndexOrThrow(getKey()));
            getField().setSelection(mCachedValue);
        }

        @Override
        public void saveData(ContentValues values) {
            mCachedValue = getField().getSelectedItemPosition();
            values.put(getKey(), mCachedValue);
        }
    }

    /**
     * ContentBinding for a TextView.
     */
    public static class TextViewContentBinding extends ContentBinding<TextView> {

        /**
         * @param key The key for the data.
         * @param field The TextView to bind to.
         */
        public TextViewContentBinding(String key, TextView field) {
            super(key, field);
            field.addTextChangedListener(new TextWatcher() {

                @Override
                public void afterTextChanged(Editable s) {
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    setDirty(true);
                }

            });
        }

        @Override
        public void loadData(Cursor c) {
            getField().setText(c.getString(c.getColumnIndexOrThrow(getKey())));
        }

        @Override
        public void saveData(ContentValues values) {
            values.put(getKey(), getField().getText().toString());
        }
    }
}