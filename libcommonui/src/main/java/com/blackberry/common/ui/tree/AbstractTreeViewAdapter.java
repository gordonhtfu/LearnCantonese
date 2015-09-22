/*
 * Copyright (c) 2011, Polidea
 * 
All rights reserved. Redistribution and use in source and binary forms, with or 
without modification,  are permitted provided that the following conditions are 
met: Redistributions of source code must retain the above copyright notice, this 
list of conditions and the following disclaimer. Redistributions in binary form 
must reproduce the above copyright notice, this list of conditions and the 
following disclaimer in the documentation and/or other materials provided with 
the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, 
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
POSSIBILITY OF SUCH DAMAGE.

https://github.com/Polidea/tree-view-list-android/blob/master/LICENCE.txt
 */

package com.blackberry.common.ui.tree;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ListAdapter;

import com.blackberry.common.ui.R;

/**
 * Adapter used to feed the table view.
 * 
 * @param <NODE_ID> type of the identifier used by the tree
 */
public abstract class AbstractTreeViewAdapter<NODE_ID> extends BaseAdapter implements
        ListAdapter {
    private static final String TAG = AbstractTreeViewAdapter.class.getSimpleName();
    private final TreeStateManager<NODE_ID> mTreeStateManager;
    private final Context mContext;
    private final LayoutInflater mLayoutInflater;
    private final DataSetObserver mDataSetObserver = new MyDataSetObserver();

    private int mIndentWidth = 0;
    private int mIndicatorGravity = 0;
    private Drawable mCollapsedDrawable;
    private Drawable mExpandedDrawable;
    private Drawable mIndicatorBackgroundDrawable;
    private Drawable mRowBackgroundDrawable;

    private final OnClickListener mIndicatorClickListener = new OnClickListener() {
        @Override
        public void onClick(final View v) {
            @SuppressWarnings("unchecked")
            final NODE_ID id = (NODE_ID) v.getTag();
            expandCollapse(id);
        }
    };

    private boolean mCollapsible;

    protected TreeStateManager<NODE_ID> getManager() {
        return mTreeStateManager;
    }

    protected void expandCollapse(final NODE_ID id) {
        final TreeNodeInfo<NODE_ID> info = mTreeStateManager.getNodeInfo(id);
        if (!info.isWithChildren()) {
            // ignore - no default action
            return;
        }
        if (info.isExpanded()) {
            mTreeStateManager.collapseChildren(id);
        } else {
            mTreeStateManager.expandDirectChildren(id);
        }
    }

    private void calculateIndentWidth() {
        if (mExpandedDrawable != null) {
            mIndentWidth = Math.max(getIndentWidth(), mExpandedDrawable.getIntrinsicWidth());
        }
        if (mCollapsedDrawable != null) {
            mIndentWidth = Math.max(getIndentWidth(), mCollapsedDrawable.getIntrinsicWidth());
        }
    }

    public AbstractTreeViewAdapter(final Context context,
            final LayoutInflater layoutInflater,
            final TreeStateManager<NODE_ID> treeStateManager) {
        mContext = context;
        mTreeStateManager = treeStateManager;
        mTreeStateManager.registerDataSetObserver(mDataSetObserver);
        mLayoutInflater = layoutInflater;
        mCollapsedDrawable = null;
        mExpandedDrawable = null;
        mRowBackgroundDrawable = null;
        mIndicatorBackgroundDrawable = null;
    }

    @Override
    public int getCount() {
        return mTreeStateManager.getVisibleCount();
    }

    @Override
    public Object getItem(final int position) {
        return getTreeId(position);
    }

    public NODE_ID getTreeId(final int position) {
        return mTreeStateManager.getVisibleList().get(position);
    }

    public TreeNodeInfo<NODE_ID> getTreeNodeInfo(final int position) {
        return mTreeStateManager.getNodeInfo(getTreeId(position));
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    protected int getTreeListItemWrapperId() {
        return R.layout.tree_list_item_wrapper;
    }

    // TODO: can we use view holder?
    @Override
    public final View getView(final int position, final View convertView, final ViewGroup parent) {
        Log.d(TAG, "Creating a view based on " + convertView + " with position " + position);
        final TreeNodeInfo<NODE_ID> nodeInfo = getTreeNodeInfo(position);
        LinearLayout linear;
        View childView;
        boolean isNewView = false;
        if (convertView == null) {
            Log.d(TAG, "Creating the view a new");
            linear = (LinearLayout) mLayoutInflater.inflate(getTreeListItemWrapperId(), null);
            childView = newChildView(mContext, nodeInfo, linear);
            isNewView = true;
        } else {
            Log.d(TAG, "Reusing the view");
            linear = (LinearLayout) convertView;
            final FrameLayout frameLayout = (FrameLayout) linear
                    .findViewById(R.id.treeview_list_item_frame);
            childView = frameLayout.getChildAt(0);
        }
        bindView(childView, mContext, nodeInfo);
        return populateTreeItem(linear, childView, nodeInfo, isNewView);
    }

    /**
     * Makes a new view to hold the data pointed to by cursor.
     * 
     * @param context Interface to application's global information
     * @param treeNodeInfo The TreeNodeInfo from which to get the data.
     * @param parent The parent to which the new view is attached to
     * @return the newly created view.
     */
    public abstract View newChildView(Context context, TreeNodeInfo<NODE_ID> treeNodeInfo,
            final ViewGroup parent);

    /**
     * Bind an existing view to the data pointed to by TreeNodeInfo
     * 
     * @param view Existing view, returned earlier by newView
     * @param context Interface to application's global information
     * @param treeNodeInfo The TreeNodeInfo from which to get the data.
     */

    public abstract void bindView(View view, Context context, TreeNodeInfo<NODE_ID> treeNodeInfo);

    /**
     * Retrieves background drawable for the node.
     * 
     * @param treeNodeInfo node info
     * @return drawable returned as background for the whole row. Might be null, then default
     *         background is used
     */
    public Drawable getBackgroundDrawable(final TreeNodeInfo<NODE_ID> treeNodeInfo) { // NOPMD
        return null;
    }

    private Drawable getDrawableOrDefaultBackground(final Drawable r) {
        if (r == null) {
            return mContext.getResources()
                    .getDrawable(R.drawable.list_selector_background).mutate();
        } else {
            return r;
        }
    }

    public final LinearLayout populateTreeItem(final LinearLayout layout,
            final View childView,
            final TreeNodeInfo<NODE_ID> nodeInfo,
            final boolean newChildView) {

        final Drawable individualRowDrawable = getBackgroundDrawable(nodeInfo);
        final Drawable defaultRowDrawable = getDrawableOrDefaultBackground(mRowBackgroundDrawable);
        layout.setBackground(
                individualRowDrawable == null ? defaultRowDrawable : individualRowDrawable);
        final LinearLayout.LayoutParams indicatorLayoutParams = new LinearLayout.LayoutParams(
                calculateIndentation(nodeInfo), LayoutParams.MATCH_PARENT);
        final LinearLayout indicatorLayout = (LinearLayout) layout
                .findViewById(R.id.treeview_list_item_image_layout);
        indicatorLayout.setGravity(mIndicatorGravity);
        indicatorLayout.setLayoutParams(indicatorLayoutParams);

        final ImageView image = (ImageView) layout.findViewById(R.id.treeview_list_item_image);
        if (nodeInfo.isWithChildren() && mCollapsible) {
            image.setImageDrawable(getDrawable(nodeInfo));
            image.setBackground(getDrawableOrDefaultBackground(mIndicatorBackgroundDrawable));
            image.setScaleType(ScaleType.CENTER);
            image.setTag(nodeInfo.getId());
            image.setOnClickListener(mIndicatorClickListener);
            image.setVisibility(View.VISIBLE);
        } else {
            // TODO: GONE or INVISIBLE?
            image.setVisibility(View.INVISIBLE);
            image.setOnClickListener(null);
        }

        layout.setTag(nodeInfo.getId());
        final FrameLayout frameLayout = (FrameLayout) layout
                .findViewById(R.id.treeview_list_item_frame);
        // TODO: do we need to set this in code?
        final FrameLayout.LayoutParams childParams = new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        if (newChildView) {
            frameLayout.addView(childView, childParams);
        }
        frameLayout.setTag(nodeInfo.getId());
        return layout;
    }

    protected int calculateIndentation(final TreeNodeInfo<NODE_ID> nodeInfo) {
        return getIndentWidth() * (nodeInfo.getLevel() + (mCollapsible ? 1 : 0));
    }

    protected Drawable getDrawable(final TreeNodeInfo<NODE_ID> nodeInfo) {
        if (!nodeInfo.isWithChildren() || !mCollapsible) {
            return getDrawableOrDefaultBackground(mIndicatorBackgroundDrawable);
        }
        if (nodeInfo.isExpanded()) {
            return mExpandedDrawable;
        } else {
            return mCollapsedDrawable;
        }
    }

    // FIXME: don't use actual gravity. Use an enum for Start, END
    public void setIndicatorGravity(final int indicatorGravity) {
        this.mIndicatorGravity = indicatorGravity;
    }

    public void setRowBackgroundDrawable(final Drawable rowBackgroundDrawable) {
        this.mRowBackgroundDrawable = rowBackgroundDrawable;
    }

    public void setIndicatorBackgroundDrawable(
            final Drawable indicatorBackgroundDrawable) {
        this.mIndicatorBackgroundDrawable = indicatorBackgroundDrawable;
    }

    public void setCollapsedDrawable(final Drawable collapsedDrawable) {
        this.mCollapsedDrawable = collapsedDrawable;
        calculateIndentWidth();
    }

    public void setExpandedDrawable(final Drawable expandedDrawable) {
        this.mExpandedDrawable = expandedDrawable;
        calculateIndentWidth();
    }

    public void setIndentWidth(final int indentWidth) {
        this.mIndentWidth = indentWidth;
        calculateIndentWidth();
    }

    public void setCollapsible(final boolean collapsible) {
        this.mCollapsible = collapsible;
    }

    public void refresh() {
        mTreeStateManager.refresh();
    }

    private int getIndentWidth() {
        return mIndentWidth;
    }

    private class MyDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            notifyDataSetChanged();
        }

        @Override
        public void onInvalidated() {
            notifyDataSetInvalidated();
        }
    }
}
