
package com.blackberry.common.ui.list;

import android.database.Cursor;

import com.blackberry.datagraph.provider.DataGraphContract;
import com.blackberry.provider.ListItemContract;

import java.text.SimpleDateFormat;

/**
 * Sectionizer to generate sections headers based on the {@Link ListItemContract}
 * .ListItemColumns.TIMESTAMP. Expects a cursor to be passed in.
 */
public class DateSectionizer implements Sectionizer {

    private SimpleDateFormat mDateFormat;

    /**
     * Constructor.
     * 
     * @param format The date format of the section header string.
     */
    public DateSectionizer(SimpleDateFormat format) {
        mDateFormat = format;
    }

    @Override
    public CharSequence getSectionTitle(Object item) {
        Cursor cursor = null;
        if (item != null && item instanceof Cursor) {
            cursor = (Cursor) item;
        } else {
            if (item == null) {
                throw new IllegalArgumentException("Argument 'item' is null");
            } else {
                throw new IllegalArgumentException(
                        "DateSectionizer expected a Cursor, but was passed: "
                                + item.getClass().getSimpleName());
            }
        }
        int dateIdx = cursor.getColumnIndex(DataGraphContract.EntityColumns.TIMESTAMP);
        long thisDate = cursor.getLong(dateIdx);
        return mDateFormat.format(thisDate);
    }
}
