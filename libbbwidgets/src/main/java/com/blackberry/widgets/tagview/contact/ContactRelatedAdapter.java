package com.blackberry.widgets.tagview.contact;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author tallen
 *         <p/>
 *         The standard Adapter used by ContactTags for generating related items. Users can subclass
 *         this and override {@link #getSearchResults()} to add custom items, alternative list
 *         sorting, etc. Users who do this will also need to: <ol> <li> override {@link
 *         #getView(int, View, ViewGroup)} to provide the view to use for displaying any custom
 *         items, falling back to the super's {@link #getView(int, View, ViewGroup)} for non-custom
 *         items.</li> <li> override {@link #getItemViewType(int)} returning a unique integer >=
 *         super's {@link #getViewTypeCount()} for each type of custom item, falling back to the
 *         super's {@link #getItemViewType(int)} for non-custom items.</li> </ol>
 * @see ContactTags
 */
public class ContactRelatedAdapter extends BaseAdapter {
    /**
     * List of Objects so that we (or subclasses) can add custom items
     */
    private List<Object> mRelatedItems = new ArrayList<Object>(0);

    @Override
    public int getCount() {
        return mRelatedItems.size();
    }

    @Override
    public Object getItem(int position) {
        return mRelatedItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
//        RelatedTag tag;
//        if (convertView instanceof RelatedTag) {
//            tag = (RelatedTag) convertView;
//        } else {
//            tag = new RelatedTag(parent.getContext());
//        }
//        tag.setData(getItem(position));
//        return tag;
        return null;
    }

    /**
     * Subclasses can override this method to add extra items to the list. Subclasses can choose to
     * not call the super's implementation if they wish to override the default search behaviour.
     *
     * @return A list of contacts that are related to the list of items in {@link #getTagAdapter()}
     */
    protected List<Object> getSearchResults() {
        return new ArrayList<Object>(0);
    }
}
