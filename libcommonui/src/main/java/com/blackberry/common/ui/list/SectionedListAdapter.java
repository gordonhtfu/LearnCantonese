
package com.blackberry.common.ui.list;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.SectionIndexer;

import com.blackberry.common.ui.list.StickySectionListView.StickySectionListAdapter;

import java.util.ArrayList;
import java.util.LinkedHashSet;

/**
 * Adapter that generates and merges section headers and items into a single list. Section headers
 * are generated using the provided {@link Sectionizer}.
 * <p>
 * Adapter implements {@link StickySectionListAdapter} interface so it can work a
 * {@link StickySectionList}. Adapter implements {@link SectionIndexer} interface so it can handle
 * fast scrolling.
 * <p>
 * <b> NOTE: The adapter assumes that the data source of the decorated list adapter is sorted.</b>
 */

public class SectionedListAdapter extends BaseAdapter
        implements StickySectionListAdapter, SectionIndexer {

    static final String TAG = SectionedListAdapter.class.getSimpleName();

    private ArrayList<SectionItem> mAllItems;
    private BaseAdapter mListAdapter;
    private Sectionizer mSectionizer;
    private AbstractSectionHeaderAdapter mSectionHeaderAdapter;

    /**
     * Constructor.
     * 
     * @param listAdapter The wrapped adapter for items.
     * @param sectionHeaderAdapter The adapter for section headers.
     * @param sectionizer The sectionizer used to create headers for items in listAdapter.
     */
    public SectionedListAdapter(BaseAdapter listAdapter,
            AbstractSectionHeaderAdapter sectionHeaderAdapter, Sectionizer sectionizer) {

        initialize(listAdapter,
                sectionHeaderAdapter,
                sectionizer);
    }

    /**
     * Constructor.
     * 
     * @param context The current context.
     * @param layoutResourceId The resource ID for a layout file containing a TextView to use when
     *            instantiating header views.
     * @param listAdapter The wrapped adapter for items.
     * @param sectionizer The sectionizer used to create headers for items in listAdapter.
     */

    public SectionedListAdapter(Context context, int layoutResourceId,
            BaseAdapter listAdapter, Sectionizer sectionizer) {

        if (context == null) {
            throw new IllegalArgumentException("context cannot be null.");
        }

        initialize(listAdapter,
                new SimpleSectionHeaderAdapter(context, layoutResourceId),
                sectionizer);
    }

    /**
     * Constructor.
     * 
     * @param context The current context.
     * @param layoutResourceId The resource ID for a layout file containing a layout to use when
     *            instantiating header views.
     * @param textViewResourceId The id of the TextView within the layout resource to be populated
     * @param listAdapter The wrapped adapter for items.
     * @param sectionizer The sectionizer used to create headers for items in listAdapter.
     */
    public SectionedListAdapter(Context context, int layoutResourceId, int textViewResourceId,
            BaseAdapter listAdapter, Sectionizer sectionizer) {

        if (context == null) {
            throw new IllegalArgumentException("context cannot be null.");
        }

        initialize(listAdapter,
                new SimpleSectionHeaderAdapter(context, layoutResourceId, textViewResourceId),
                sectionizer);
    }

    private void initialize(BaseAdapter listAdapter,
            AbstractSectionHeaderAdapter sectionHeaderAdapter,
            Sectionizer sectionizer) {

        if (listAdapter == null) {
            throw new IllegalArgumentException("listAdapter cannot be null.");
        } else if (sectionizer == null) {
            throw new IllegalArgumentException("sectionizer cannot be null.");
        }

        mListAdapter = listAdapter;
        mSectionHeaderAdapter = sectionHeaderAdapter;
        mSectionizer = sectionizer;
        mAllItems = new ArrayList<SectionItem>();

        this.mListAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                notifyDataSetChanged();
            }

            @Override
            public void onInvalidated() {
                notifyDataSetInvalidated();
            }
        });

        buildSections();
    }

    // TODO: if this function becomes too slow. Look into having the sections supplied up front, and
    // then matching items to sections. For date separators it will require a second CP query.
    private void buildSections() {

        clearData();

        LinkedHashSet<CharSequence> sectionTitles = new LinkedHashSet<CharSequence>();

        int listPosition = 0;
        int sectionPosition = -1;
        for (int i = 0; i < mListAdapter.getCount(); i++) {
            Object element = mListAdapter.getItem(i);
            CharSequence sectionName = mSectionizer.getSectionTitle(element);

            if (sectionTitles.contains(sectionName)) {
                // Handle existing section
                SectionItem item = SectionItem.createItem(sectionPosition, listPosition, i);
                listPosition++;
                add(item);
            } else {
                // Handle new section
                sectionPosition++;
                sectionTitles.add(sectionName);

                SectionItem section = SectionItem.createHeader(sectionName, sectionPosition,
                        listPosition, sectionPosition);
                listPosition++;
                add(section);

                SectionItem item = SectionItem.createItem(sectionPosition, listPosition, i);
                listPosition++;
                add(item);
            }
        }
    }

    @Override
    public void notifyDataSetChanged() {
        buildSections();
        super.notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetInvalidated() {
        clearData();
        super.notifyDataSetInvalidated();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        SectionItem item = getSectionItem(position);
        if (item.isHeader()) {
            view = mSectionHeaderAdapter.getView(item.sourcePosition(), view, parent);
        } else {
            view = mListAdapter.getView(item.sourcePosition(), convertView, parent);
        }
        return view;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return getSectionItem(position).itemType();
    }

    private void add(SectionItem item) {
        if (item.isHeader()) {
            mSectionHeaderAdapter.add(item);
        }
        mAllItems.add(item);
    }

    private void clearData() {
        mAllItems.clear();
        mSectionHeaderAdapter.clear();
    }

    /*
     * StickySectionListAdapter interface
     */
    @Override
    public boolean isItemViewTypeSticky(int viewType) {
        return viewType == SectionItem.HEADER;
    }

    /*
     * BaseAdapter interface
     */
    @Override
    public int getCount() {
        return mAllItems.size();
    }

    @Override
    public Object getItem(int position) {
        SectionItem item = getSectionItem(position);
        if (item.isHeader()) {
            return item;
        } else {
            return mListAdapter.getItem(item.sourcePosition());
        }
    }

    private SectionItem getSectionItem(int position) {
        return mAllItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    /*
     * SectionIndexer interface
     */
    @Override
    public Object[] getSections() {
        return mSectionHeaderAdapter.toArray();
    }

    @Override
    public int getPositionForSection(int section) {
        if (section >= mSectionHeaderAdapter.getCount()) {
            section = mSectionHeaderAdapter.getCount() - 1;
            if (section < 0) {
                section = 0;
            }
            Log.w(TAG, "This shouldn't happen! Something went wrong in buildSections");
        }
        return mSectionHeaderAdapter.getItem(section).listPosition();
    }

    @Override
    public int getSectionForPosition(int position) {
        if (position >= getCount()) {
            position = getCount() - 1;
            if (position < 0) {
                position = 0;
            }
            Log.w(TAG, "This shouldn't happen! Something went wrong in buildSections");
        }
        return getSectionItem(position).sectionPosition();
    }

}
