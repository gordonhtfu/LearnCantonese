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
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.blackberry.common.ui.R;

/**
 * Tree view, expandable multi-level.
 * 
 * <pre>
 * attr ref com.blackberry.ui.R.styleable#TreeViewList_collapsible
 * attr ref com.blackberry.ui.R.styleable#TreeViewList_src_expanded
 * attr ref com.blackberry.ui.R.styleable#TreeViewList_src_collapsed
 * attr ref com.blackberry.ui.R.styleable#TreeViewList_indent_width
 * attr ref com.blackberry.ui.R.styleable#TreeViewList_indicator_gravity
 * attr ref com.blackberry.ui.R.styleable#TreeViewList_indicator_background
 * attr ref com.blackberry.ui.R.styleable#TreeViewList_row_background
 * </pre>
 */
public class TreeViewList extends ListView {
    private static final int DEFAULT_COLLAPSED_RESOURCE = R.drawable.collapsed;
    private static final int DEFAULT_EXPANDED_RESOURCE = R.drawable.expanded;
    private static final int DEFAULT_INDENT = 0;
    private static final int DEFAULT_GRAVITY = Gravity.START | Gravity.CENTER_VERTICAL;

    private Drawable mExpandedDrawable;
    private Drawable mCollapsedDrawable;
    private Drawable mRowBackgroundDrawable;
    private Drawable mIndicatorBackgroundDrawable;
    private int mIndentWidth = 0;
    private int mIndicatorGravity = 0;
    private AbstractTreeViewAdapter<?> mTreeAdapter;
    private boolean mCollapsible;

    public TreeViewList(Context context) {
        this(context, null);
    }

    public TreeViewList(Context context, AttributeSet attrs) {
        this(context, attrs, R.style.TreeViewListStyle);
    }

    public TreeViewList(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
        parseAttributes(context, attrs);
    }

    private void parseAttributes(final Context context, final AttributeSet attrs) {
        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TreeViewList);
        mExpandedDrawable = a.getDrawable(R.styleable.TreeViewList_src_expanded);
        if (mExpandedDrawable == null) {
            mExpandedDrawable = context.getResources().getDrawable(DEFAULT_EXPANDED_RESOURCE);
        }
        mCollapsedDrawable = a.getDrawable(R.styleable.TreeViewList_src_collapsed);
        if (mCollapsedDrawable == null) {
            mCollapsedDrawable = context.getResources().getDrawable(DEFAULT_COLLAPSED_RESOURCE);
        }
        mIndentWidth = a.getDimensionPixelSize(R.styleable.TreeViewList_indent_width,
                DEFAULT_INDENT);
        mIndicatorGravity = a.getInteger(R.styleable.TreeViewList_indicator_gravity,
                DEFAULT_GRAVITY);
        mIndicatorBackgroundDrawable = a.getDrawable(R.styleable.TreeViewList_indicator_background);
        mRowBackgroundDrawable = a.getDrawable(R.styleable.TreeViewList_row_background);
        mCollapsible = a.getBoolean(R.styleable.TreeViewList_collapsible, true);
        a.recycle();
    }

    @Override
    public void setAdapter(final ListAdapter adapter) {
        if (!(adapter instanceof AbstractTreeViewAdapter)) {
            throw new TreeConfigurationException(
                    "The adapter is not of TreeViewAdapter type");
        }
        mTreeAdapter = (AbstractTreeViewAdapter<?>) adapter;
        syncAdapter();
        super.setAdapter(mTreeAdapter);
    }

    private void syncAdapter() {
        mTreeAdapter.setCollapsedDrawable(mCollapsedDrawable);
        mTreeAdapter.setExpandedDrawable(mExpandedDrawable);
        mTreeAdapter.setIndicatorGravity(mIndicatorGravity);
        mTreeAdapter.setIndentWidth(mIndentWidth);
        mTreeAdapter.setIndicatorBackgroundDrawable(mIndicatorBackgroundDrawable);
        mTreeAdapter.setRowBackgroundDrawable(mRowBackgroundDrawable);
        mTreeAdapter.setCollapsible(mCollapsible);
    }

    public void setExpandedDrawable(final Drawable expandedDrawable) {
        mExpandedDrawable = expandedDrawable;
        syncAdapter();
        mTreeAdapter.refresh();
    }

    public void setCollapsedDrawable(final Drawable collapsedDrawable) {
        mCollapsedDrawable = collapsedDrawable;
        syncAdapter();
        mTreeAdapter.refresh();
    }

    public void setRowBackgroundDrawable(final Drawable rowBackgroundDrawable) {
        mRowBackgroundDrawable = rowBackgroundDrawable;
        syncAdapter();
        mTreeAdapter.refresh();
    }

    public void setIndicatorBackgroundDrawable(
            final Drawable indicatorBackgroundDrawable) {
        mIndicatorBackgroundDrawable = indicatorBackgroundDrawable;
        syncAdapter();
        mTreeAdapter.refresh();
    }

    public void setIndentWidth(final int indentWidth) {
        this.mIndentWidth = indentWidth;
        syncAdapter();
        mTreeAdapter.refresh();
    }

    public void setIndicatorGravity(final int indicatorGravity) {
        mIndicatorGravity = indicatorGravity;
        syncAdapter();
        mTreeAdapter.refresh();
    }

    public void setCollapsible(final boolean collapsible) {
        mCollapsible = collapsible;
        syncAdapter();
        mTreeAdapter.refresh();
    }

    public Drawable getExpandedDrawable() {
        return mExpandedDrawable;
    }

    public Drawable getCollapsedDrawable() {
        return mCollapsedDrawable;
    }

    public Drawable getRowBackgroundDrawable() {
        return mRowBackgroundDrawable;
    }

    public Drawable getIndicatorBackgroundDrawable() {
        return mIndicatorBackgroundDrawable;
    }

    public int getIndentWidth() {
        return mIndentWidth;
    }

    public int getIndicatorGravity() {
        return mIndicatorGravity;
    }

    public boolean isCollapsible() {
        return mCollapsible;
    }
}
