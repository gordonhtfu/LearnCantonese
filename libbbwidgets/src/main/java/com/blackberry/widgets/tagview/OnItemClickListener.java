package com.blackberry.widgets.tagview;

import android.view.View;

/**
 * @author tallen
 *         <p/>
 *         An interface to extend to listen for item clicks
 */
public interface OnItemClickListener {
    /**
     * Called when an item is clicked
     *
     * @param view  The view which was clicked
     * @param event The details of the item click event
     */
    void onItemClick(View view, ItemClickEvent event);

    /**
     * The class containing the parameters for the item click event.
     */
    public static class ItemClickEvent {
        /**
         * The position in the adapter for the clicked item
         *
         * @see #getPosition()
         */
        private int mPosition;
        /**
         * The data object that was clicked on
         */
        private Object mData;
        /**
         * Whether or not to select the clicked item
         *
         * @see #getSelectItem()
         * @see #setSelectItem(boolean)
         */
        private boolean mSelectItem = true;

        /**
         * @param position The position in the adapter for the clicked item
         * @param data     The data which the clicked item represents
         */
        public ItemClickEvent(int position, Object data) {
            mPosition = position;
            mData = data;
        }

        /**
         * @return The position in the adapter for the clicked item
         */
        public int getPosition() {
            return mPosition;
        }

        /**
         * @return The data which the clicked item represents. This is analagous to using {@link
         * #getPosition()} as the index into the adapter to retrieve the data item.
         */
        public Object getData() {
            return mData;
        }

        /**
         * @return Whether or not to select the clicked item.
         */
        public boolean getSelectItem() {
            return mSelectItem;
        }

        /**
         * Set this to false if the item should not be selected. This is useful to perform custom
         * actions on an item while blocking the default action (select the item).
         *
         * @param selectItem Whether or not to select the clicked item
         */
        public void setSelectItem(boolean selectItem) {
            mSelectItem = selectItem;
        }
    }
}