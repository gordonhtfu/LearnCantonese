
package com.blackberry.common.ui.list;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * A concrete {@link AbstractSectionHeaderAdapter} that is backed by an array of {@link SectionItem}
 * objects. By default this class expects that the provided resource id references a single
 * {@link TextView}. If you want to use a more complex layout, use the constructors that also takes
 * a field id. That field id should reference a {@link TextView} in the larger layout resource. The
 * {@link TextView} will be filled with the text of each {@link SectionItem} in the array.
 */

public class SimpleSectionHeaderAdapter extends AbstractSectionHeaderAdapter {

    static final String TAG = SimpleSectionHeaderAdapter.class.getSimpleName();

    private ArrayList<SectionItem> mSectionItems;

    private int mResource;
    private int mTextViewResourceId;
    private LayoutInflater mInflater;

    /**
     * Constructor.
     * 
     * @param context The current context.
     * @param resource The resource ID for a layout file containing a TextView to use when
     *            instantiating views.
     */
    public SimpleSectionHeaderAdapter(Context context, int resource) {
        initialize(context, resource, 0);
    }

    /**
     * Constructor.
     * 
     * @param context The current context.
     * @param resource The resource ID for a layout file containing a layout to use when
     *            instantiating views.
     * @param textViewResourceId The id of the TextView within the layout resource to be populated
     */
    public SimpleSectionHeaderAdapter(Context context, int resource, int textViewResourceId) {
        initialize(context, resource, textViewResourceId);
    }

    private void initialize(Context context, int resource, int textViewResourceId) {
        // mInflater = LayoutInflater.from(context);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mResource = resource;
        mTextViewResourceId = textViewResourceId;
        mSectionItems = new ArrayList<SectionItem>();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        TextView text;
        if (convertView == null) {
            view = mInflater.inflate(mResource, parent, false);
        } else {
            view = convertView;
        }
        try {
            if (mTextViewResourceId == 0) {
                // If no custom field is assigned, assume the whole resource is a TextView
                text = (TextView) view;
            } else {
                // Otherwise, find the TextView field within the layout
                text = (TextView) view.findViewById(mTextViewResourceId);
            }
        } catch (ClassCastException e) {
            Log.e(TAG, "You must supply a resource ID for a TextView");
            throw new IllegalStateException(
                    "SimpleSectionHeaderAdapter requires the resource ID to be a TextView", e);
        }

        SectionItem item = getItem(position);
        text.setText(item.toString());

        return view;
    }

    @Override
    public int getCount() {
        return mSectionItems.size();
    }

    @Override
    public SectionItem getItem(int position) {
        return mSectionItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public void add(SectionItem title) {
        mSectionItems.add(title);
        notifyDataSetChanged();
    }

    @Override
    public Object[] toArray() {
        return mSectionItems.toArray();
    }

    @Override
    public void clear() {
        mSectionItems.clear();
    }
}
